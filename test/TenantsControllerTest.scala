import be.thomastoye.speelsysteem.models.Tenant
import com.ibm.couchdb.Res
import controllers.TenantsController

import scala.concurrent.Future
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.concurrent.ScalaFutures
import play.api.{Configuration, Environment}
import play.api.i18n.{DefaultLangs, DefaultMessagesApi, MessagesApi}
import play.api.test.Helpers._
import play.test.WithApplication
import services.TenantsService

class TenantsControllerTest extends PlaySpec with Results with MockitoSugar with ScalaFutures {
  "The tenants controller" should {
    "display all databases on the listing page" in {
      val tenantsService: TenantsService = mock[TenantsService]
      when(tenantsService.all) thenReturn Future.successful(
        Seq(Tenant.create("some-tenant").get, Tenant.create("another-tenant").get)
      )
      val controller = new TenantsController(tenantsService, null)
      val result: Future[Result] = controller.list().apply(FakeRequest())
      val bodyText: String = contentAsString(result)

      bodyText must include("some-tenant")
      bodyText must include("another-tenant")
      bodyText must include("All tenants (2)")
    }

    "create a tenant when given a valid normalized tenant name" in new WithApplication { app =>
      val tenantsService = mock[TenantsService]
      when(tenantsService.create(any[Tenant])) thenReturn Future.successful(new Res.Ok)

      val controller = new TenantsController(tenantsService, null)

      val resultFut = controller.createNewPost.apply(FakeRequest().withFormUrlEncodedBody("tenantNormalizedName" -> "test-tenant-name"))

      whenReady(resultFut) { res =>
        res.header.status mustBe 303
        res.header.headers mustBe Map("Location" -> "/tenants")
        verify(tenantsService, times(1)).create(any[Tenant])
      }

    }

    "fail to create a tenant when given a valid normalized tenant name" in new WithApplication { app =>
      val tenantsService = mock[TenantsService]
      when(tenantsService.create(any[Tenant])) thenReturn Future.successful(new Res.Ok)

      val messagesApi = new DefaultMessagesApi(
        Environment.simple(),
        Configuration.load(Environment.simple()),
        new DefaultLangs(Configuration.load(Environment.simple()))
      )

      val controller = new TenantsController(tenantsService, messagesApi)

      val resultFut = controller.createNewPost.apply(FakeRequest().withFormUrlEncodedBody("tenantNormalizedName" -> "some-tenant-}{)(*&^%$#@!"))

      whenReady(resultFut) { res =>
        res.header.status mustBe 400
        verify(tenantsService, times(0)).create(any[Tenant])
      }
      
      contentAsString(resultFut) must include("""May only contain lowercase letters, numbers, underscores (_), dollar signs ($), parentheses, plus and minus signs, and slashes.""")
      contentAsString(resultFut) must include("""value="some-tenant-}{)(*&amp;^%$#@!"""")

    }
  }
}
