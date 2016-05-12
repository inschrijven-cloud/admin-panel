package controllers

import play.api.mvc.Action
import javax.inject._

import play.api._
import play.api.mvc._
import services.{DatabaseService, TenantsService}
import play.api.libs.concurrent.Execution.Implicits._

class DatabaseController @Inject() (databaseService: DatabaseService) extends Controller {
  def list = Action.async {
    databaseService.all map { dbs =>
      Ok(views.html.database.list(dbs))
    }
  }
}
