package controllers

import javax.inject._

import play.api.Logger
import be.thomastoye.speelsysteem.models.Tenant
import services.{AutoRemoteCouchDBConfig, TenantsService}
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

@Singleton
class TenantsController @Inject()(tenantsService: TenantsService, remoteCouchDB: AutoRemoteCouchDBConfig, val messagesApi: MessagesApi) extends Controller with I18nSupport {
  val tenantNormalizedNameConstraint = Constraint[String] { input: String =>
    input match {
      case name if name.matches("""^([a-z]|[0-9]|_|\$|\(|\)|\+|\-|\/)*$""") => Valid
      case _ => Invalid("May only contain lowercase letters, numbers, underscores (_), dollar signs ($), parentheses, plus and minus signs, and slashes.")
    }
  }

  val createTenantForm = Form(
    mapping("tenantNormalizedName" -> nonEmptyText.verifying(tenantNormalizedNameConstraint))
    (Tenant.create(_).get)
    (tenant => Some(tenant.normalizedName))
  )

  def list = Action.async {
    tenantsService.all map { tenants =>
      Ok(views.html.tenants.list(tenants))
    }
  }

  def details(name: String) = Action.async {
    Tenant.create(name).fold(
      Future.successful(BadRequest(""))
    )(tenant =>
      tenantsService.details(tenant) map { details =>
        Ok(views.html.tenants.details(tenant, remoteCouchDB))
      }
    )
  }

  def syncTo(tenantName: String) = Action.async {
    Tenant.create(tenantName).fold(Future.successful(BadRequest("")))( tenant =>
      tenantsService.syncTo(tenant, remoteCouchDB).map(res => Ok(views.html.tenants.replicationresults(tenant, res)))
    )
  }

  def syncFrom(tenantName: String) = Action.async {
    Tenant.create(tenantName).fold(Future.successful(BadRequest("")))( tenant =>
      tenantsService.syncFrom(tenant, remoteCouchDB).map(res => Ok(views.html.tenants.replicationresults(tenant, res)))
    )
  }

  def createNew = Action {
    Ok(views.html.tenants.createnew(createTenantForm))
  }

  def createNewPost = Action.async { implicit req =>
    Logger.debug("entering create new post")
    createTenantForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.tenants.createnew(formWithErrors)))
      },
      createTenant => {
        tenantsService.create(createTenant) map { status =>
          Logger.info(s"Successfully created new tenant with normalized name ${createTenant.normalizedName}")
          Redirect(routes.TenantsController.list)
        }
      }
    )
  }

  def initializeDatabase(tenantName: String) = Action.async {
    Tenant.create(tenantName).fold(Future.successful(BadRequest("Could not parse tenant name"))) { tenant =>
      tenantsService.initializeDatabase(tenant).map { res =>
        Ok(views.html.tenants.initializeResult(tenant, res))
      }
    }
  }
}
