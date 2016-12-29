package services

import be.thomastoye.speelsysteem.models.Tenant
import models.DbName

import scala.concurrent.Future
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers._

class TenantsServiceTest extends PlaySpec with Results with MockitoSugar with ScalaFutures {
  "The tenants service" should {
    "correcly get all tenants from the database service" in {
      val databaseService: TenantDatabaseService = mock[TenantDatabaseService]
      when(databaseService.all) thenReturn Future.successful(
        Seq(
          DbName.create("test").get,
          DbName.create("sometestdb").get,
          DbName.create("tenant-something-lrcg").get,
          DbName.create("tenant-data-aoeu-test").get,
          DbName.create("tenant-data-snth-children").get,
          DbName.create("tenant-data-snth-days").get,
          DbName.create("tenant-data-snth-something").get,
          DbName.create("tenant-meta-snth").get
        )
      )

      val tenantsService = new CouchdbTenantsService(databaseService)
      whenReady(tenantsService.all) { tenants =>
        tenants must have size 2
        tenants must contain(Tenant.create("snth").get)
        tenants must contain(Tenant.create("aoeu").get)
      }

    }
  }
}