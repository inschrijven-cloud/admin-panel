package controllers

import play.api.mvc.Action
import javax.inject._

import play.api._
import play.api.mvc._
import services.{TenantDatabaseService, TenantsService}
import play.api.libs.concurrent.Execution.Implicits._

class DatabaseController @Inject() (databaseService: TenantDatabaseService) extends Controller {
  def list = Action.async {
    databaseService.all map { dbs =>
      Ok(views.html.database.list(dbs.sortBy(_.value)))
    }
  }
}
