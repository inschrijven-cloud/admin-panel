import be.thomastoye.speelsysteem.models.Tenant
import controllers.TenantsController
import models.DbName

import scala.concurrent.Future
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers._
import services.TenantsService

class TenantsControllerTest extends PlaySpec with Results with MockitoSugar with ScalaFutures {
  "The tenants controller" should {
    "display all databases on the listing page" in {
      val tenantsService: TenantsService = mock[TenantsService]
      when(tenantsService.all) thenReturn Future.successful(Seq(Tenant("some-tenant"), Tenant("another-tenant")))
      val controller = new TenantsController(tenantsService)
      val result: Future[Result] = controller.list().apply(FakeRequest())
      val bodyText: String = contentAsString(result)

      bodyText must include("some-tenant")
      bodyText must include("another-tenant")
      bodyText must include("All tenants (2)")
    }
  }
}