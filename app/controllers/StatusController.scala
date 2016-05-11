package controllers

import play.api._
import play.api.libs.json.Json
import play.api.mvc._

class StatusController extends Controller {
  def ping = Action { Ok(Json.obj("response" -> "pong")) }
}
