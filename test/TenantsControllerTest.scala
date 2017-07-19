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
import play.api.{Application, Configuration, Environment}
import play.api.i18n.{DefaultLangs, DefaultMessagesApi, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.test.WithApplication
import services._
import mockws.MockWS
import play.api.libs.json.Json
import play.api.libs.ws.{WSAPI, WSClient, WSRequest}
import play.api.libs.ws.ahc.WSClientProvider

class TenantsControllerTest extends PlaySpec with OneServerPerSuite with Results with MockitoSugar with ScalaFutures {
  val conf = Seq(
  "couchdb.host" -> "localhost",
  "couchdb.port" -> 80,
  "couchdb.https" -> false,
  "couchdb.remote.user" -> "name",
  "couchdb.remote.host" -> "remote",
  "couchdb.remote.port" -> 2222,
  "couchdb.remote.pass" -> "secret")

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(conf:_*)
    .build()

  "The tenants controller" should {
    "display all databases on the listing page" in {
      val tenantsService: TenantsService = mock[TenantsService]
      when(tenantsService.all) thenReturn Future.successful(
        Seq(Tenant.create("some-tenant").get, Tenant.create("another-tenant").get)
      )
      val controller = new TenantsController(tenantsService, null, null)
      val result: Future[Result] = controller.list().apply(FakeRequest())
      val bodyText: String = contentAsString(result)

      bodyText must include("some-tenant")
      bodyText must include("another-tenant")
      bodyText must include("All tenants (2)")
    }

    "create a tenant when given a valid normalized tenant name" in new WithApplication { app =>
      val tenantsService = mock[TenantsService]
      when(tenantsService.create(any[Tenant])) thenReturn Future.successful(new Res.Ok)

      val controller = new TenantsController(tenantsService, null, null)

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

      val controller = new TenantsController(tenantsService, null, messagesApi)

      val resultFut = controller.createNewPost.apply(FakeRequest().withFormUrlEncodedBody("tenantNormalizedName" -> "some-tenant-}{)(*&^%$#@!"))

      whenReady(resultFut) { res =>
        res.header.status mustBe 400
        verify(tenantsService, times(0)).create(any[Tenant])
      }
      
      contentAsString(resultFut) must include("""May only contain lowercase letters, numbers, underscores (_), dollar signs ($), parentheses, plus and minus signs, and slashes.""")
      contentAsString(resultFut) must include("""value="some-tenant-}{)(*&amp;^%$#@!"""")

    }
  }

  "Syncing to a remote database" should {
    "post a correct replication document when syncing to the remote" in {
      implicit val format = Json.format[ReplicationDocument]

      val mockWs = MockWS {
        case (POST, "http://localhost:80/_replicate") => Action { req =>
          req.body.asJson
            .flatMap(_.validate[ReplicationDocument].asOpt)
            .filter(doc => doc.source.startsWith("tenant-data-test-tenant-name-") && doc.target.startsWith("https://name:secret@remote:2222/tenant-data-test-tenant-name"))
            .map(doc => Ok(Json.obj("test" -> true)))
            .getOrElse(throw new Exception("Replication document was not of the correct format: " + req.body))
        }
      }

      val provider = new WSClientProvider(new WSAPI {
        override def url(url: String): WSRequest = ???

        override def client: WSClient = mockWs
      })

      val databaseService = new CouchdbTenantDatabaseService(provider, AutoCouchDBConfig(Configuration(conf:_*)))
      val tenantsService = new CouchdbTenantsService(databaseService)

      val controller = new TenantsController(tenantsService, AutoRemoteCouchDBConfig(Configuration(conf:_*)), null)

      val resultFut = controller.syncTo("test-tenant-name").apply(FakeRequest())

      whenReady(resultFut) { res =>
        res.header.status mustBe 200
      }
    }

    "post a correct replication document when syncing from the remote" in {
      implicit val format = Json.format[ReplicationDocument]

      val mockWs = MockWS {
        case (POST, "http://localhost:80/_replicate") => Action { req =>
          req.body.asJson
            .flatMap(_.validate[ReplicationDocument].asOpt)
            .filter(doc => doc.target.startsWith("tenant-data-test-tenant-name-") && doc.source.startsWith("https://name:secret@remote:2222/tenant-data-test-tenant-name"))
            .map(doc => Ok(Json.obj("test" -> true)))
            .getOrElse(throw new Exception("Replication document was not of the correct format: " + req.body))
        }
      }

      val provider = new WSClientProvider(new WSAPI {
        override def url(url: String): WSRequest = ???

        override def client: WSClient = mockWs
      })

      val databaseService = new CouchdbTenantDatabaseService(provider, AutoCouchDBConfig(Configuration(conf:_*)))
      val tenantsService = new CouchdbTenantsService(databaseService)

      val controller = new TenantsController(tenantsService, AutoRemoteCouchDBConfig(Configuration(conf:_*)), null)

      val resultFut = controller.syncFrom("test-tenant-name").apply(FakeRequest())

      whenReady(resultFut) { res =>
        res.header.status mustBe 200
      }
    }
  }
}
