import controllers.DatabaseController
import models.DbName

import scala.concurrent.Future
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import play.api.test.Helpers._
import services.TenantDatabaseService

class DatabaseControllerTest extends PlaySpec with Results with MockitoSugar {

  "The database controller" should {
    "display all databases on the listing page" in {
      val databaseService: TenantDatabaseService = mock[TenantDatabaseService]
      when(databaseService.all) thenReturn Future.successful(Seq(DbName.create("test").get, DbName.create("sometestdb").get))
      val controller = new DatabaseController(databaseService)
      val result: Future[Result] = controller.list().apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      bodyText must include("test")
      bodyText must include("sometestdb")
    }
  }
}
