package controllers

import play.api.mvc.Action
import javax.inject._

import play.api._
import play.api.mvc._
import services.TenantDatabaseService

import scala.concurrent.ExecutionContext.Implicits.global

class DatabaseController @Inject() (databaseService: TenantDatabaseService) extends InjectedController {
  def list = Action.async {
    databaseService.all map { dbs =>
      Ok(views.html.database.list(dbs.sortBy(_.value)))
    }
  }
}
