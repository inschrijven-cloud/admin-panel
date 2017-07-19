import be.thomastoye.speelsysteem.models.Tenant
import com.ibm.couchdb.Res
import controllers.TenantsController

import scala.concurrent.{ExecutionContext, Future}
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import play.api.inject.bind
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.concurrent.ScalaFutures
import play.api.{Application, Configuration, Mode}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.test.WithApplication
import services._
import mockws.MockWS
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.FileMimeTypes
import play.api.i18n.{Langs, MessagesApi}
import play.api.libs.json.Json

class TenantsControllerTest extends PlaySpec with GuiceOneServerPerSuite with Results with MockitoSugar with ScalaFutures {
  val conf = Seq(
  "couchdb.host" -> "localhost",
  "couchdb.port" -> 80,
  "couchdb.https" -> false,
  "couchdb.remote.user" -> "name",
  "couchdb.remote.host" -> "remote",
  "couchdb.remote.port" -> 2222,
  "couchdb.remote.pass" -> "secret")

  val tenantsService: TenantsService = mock[TenantsService]

  when(tenantsService.all) thenReturn Future.successful(
    Seq(Tenant.create("some-tenant").get, Tenant.create("another-tenant").get)
  )

  when(tenantsService.create(any[Tenant])) thenReturn Future.successful(new Res.Ok)


  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(bind[TenantsService].toInstance(tenantsService))
    .configure(conf:_*)
    .build()

  "The tenants controller" should {
    "display all databases on the listing page" in {
      val controller = app.injector.instanceOf[TenantsController]
      val result: Future[Result] = controller.list().apply(FakeRequest())
      val bodyText: String = contentAsString(result)

      bodyText must include("some-tenant")
      bodyText must include("another-tenant")
      bodyText must include("All tenants (2)")
    }

    "create a tenant when given a valid normalized tenant name" in {
      val controller = app.injector.instanceOf[TenantsController]

      val resultFut = controller.createNewPost.apply(FakeRequest().withFormUrlEncodedBody("tenantNormalizedName" -> "test-tenant-name"))

      whenReady(resultFut) { res =>
        res.header.status mustBe 303
        res.header.headers mustBe Map("Location" -> "/tenants")
        verify(tenantsService, times(1)).create(any[Tenant])
      }

    }

    "fail to create a tenant when given a valid normalized tenant name" in {
      val controller = app.injector.instanceOf[TenantsController]
      reset(tenantsService)

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
