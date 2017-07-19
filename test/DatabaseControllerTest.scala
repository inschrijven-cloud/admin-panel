import controllers.DatabaseController
import models.DbName

import scala.concurrent.Future
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.TenantDatabaseService

class DatabaseControllerTest extends PlaySpec with GuiceOneAppPerSuite with Results with MockitoSugar {
  val databaseService: TenantDatabaseService = mock[TenantDatabaseService]
  when(databaseService.all) thenReturn Future.successful(Seq(DbName.create("test").get, DbName.create("sometestdb").get))

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
    .overrides(bind[TenantDatabaseService].toInstance(databaseService))
    .build()

  "The database controller" should {
    "display all databases on the listing page" in {
      val controller = app.injector.instanceOf[DatabaseController]
      val result: Future[Result] = controller.list().apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      bodyText must include("test")
      bodyText must include("sometestdb")
    }
  }
}
