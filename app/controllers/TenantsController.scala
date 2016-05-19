package controllers

import javax.inject._

import play.api.Logger
import be.thomastoye.speelsysteem.models.Tenant
import services.TenantsService
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

@Singleton
class TenantsController @Inject()(tenantsService: TenantsService, val messagesApi: MessagesApi) extends Controller with I18nSupport {
  val tenantNormalizedNameConstraint = Constraint[String] { input: String =>
    input match {
      case name if name.matches("""^([a-z]|[0-9]|_|\$|\(|\)|\+|\-|\/)*$""") => Valid
      case _ => Invalid("May only contain lowercase letters, numbers, underscores (_), dollar signs ($), parentheses, plus and minus signs, and slashes.")
    }
  }

  val createTenantForm = Form(
    mapping("tenantNormalizedName" -> nonEmptyText.verifying(tenantNormalizedNameConstraint))
    (Tenant.create(_).get)
    (tenant => Some(tenant.nomalizedName))
  )

  def list = Action.async {
    tenantsService.all map { tenants =>
      Ok(views.html.tenants.list(tenants))
    }
  }

  def details(name: String) = TODO

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
          Logger.info(s"Successfully created new tenant with normalized name ${createTenant.nomalizedName}")
          Redirect(routes.TenantsController.list)
        }
      }
    )
  }
}
