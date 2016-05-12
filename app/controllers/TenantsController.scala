package controllers

import javax.inject._

import play.api._
import play.api.mvc._
import services.TenantsService
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

@Singleton
class TenantsController @Inject() (tenantsService: TenantsService) extends Controller {
  def list = Action.async {
    tenantsService.all map { tenants =>
      Ok(views.html.tenants.list(tenants))
    }
  }

  def details(name: String) = TODO

  def createNew = TODO
}
