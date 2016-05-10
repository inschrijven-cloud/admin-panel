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
    tenantsService.allDbs.map(_.mkString(", ")) map { dbs =>
      Ok(views.html.index(dbs))
    }
  }
}
