import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.test._

class StatusControllerTest extends PlaySpec with OneServerPerSuite with Results with FutureAwaits with DefaultAwaitTimeout {
  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "couchdb.user" -> "test",
      "couchdb.host" -> "localhost",
      "couchdb.port" -> 1111,
      "couchdb.pass" -> "***",
      "couchdb.remote.user" -> "test",
      "couchdb.remote.host" -> "localhost",
      "couchdb.remote.port" -> 1111,
      "couchdb.remote.pass" -> "***"
    )
    .build()

  "The status controller" should {
    "return pong when requesting /ping" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val address =  s"http://localhost:$port/ping"
      val response = await(wsClient.url(address).get())

      response.status mustBe 200
      response.body mustBe """{"response":"pong"}"""
    }
  }
}
