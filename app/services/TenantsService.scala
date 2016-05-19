package services

import javax.inject.{Inject, Singleton}

import be.thomastoye.speelsysteem.models.Tenant
import com.ibm.couchdb.Res.Ok
import com.ibm.couchdb._
import models.DbName
import play.api.libs.concurrent.Execution.Implicits._
import services.TenantsService.TenantInfo
import util.TaskExtensionOps

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}
import scala.concurrent.{Future, Promise}
import scalaz.concurrent.Task

trait TenantsService {
  def all: Future[Seq[Tenant]]
  def create(tenant: Tenant): Future[Res.Ok]
}

object TenantsService {
  case class TenantInfo(tenant: Tenant, databases: Seq[String])
}

@Singleton
class CloudantTenantsService @Inject() (databaseService: DatabaseService) extends TenantsService
{
  override def all = {
    databaseService.all map { dbs =>
      dbs
        .map(_.value)
        .filter(name => {
          name.startsWith("tenant-data-") || name.startsWith("tenant-meta-")
        })
        .map(_.drop("tenant-xxxx-".length))
        .map(_.split('.').head)
        .distinct
        .flatMap(Tenant.create)
    }
  }

  override def create(tenant: Tenant): Future[Ok] = {
    databaseService.create(tenant.dataDatabaseName) flatMap { ok =>
      databaseService.create(tenant.metadataDatabaseName)
    }
  }
}
