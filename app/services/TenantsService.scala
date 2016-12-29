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
  def details(tenant: Tenant): Future[Unit] // does nothing yet
  def initializeDatabase(tenant: Tenant): Future[Seq[Res.DocOk]]
}

object TenantsService {
  case class TenantInfo(tenant: Tenant, databases: Seq[String])
}

@Singleton
class CouchdbTenantsService @Inject()(databaseService: TenantDatabaseService) extends TenantsService
{
  override def all = {
    databaseService.all map { dbs =>
      dbs
        .map(_.value)
        .filter(name => {
          name.startsWith("tenant-data-") || name.startsWith("tenant-meta-")
        })
        .map(db => if(db.startsWith("tenant-data")) db.split("-").dropRight(1).mkString("-") else db) // data databases
        .map(_.drop("tenant-xxxx-".length)) // remove prefix
        .map(_.split('.').head)
        .distinct
        .flatMap(Tenant.create)
    }
  }

  override def create(tenant: Tenant): Future[Ok] = {
    Future.sequence(tenant.dataDatabases.map(databaseService.create)) flatMap { ok =>
      databaseService.create(tenant.metadataDatabaseName)
    }
  }

  override def details(tenant: Tenant): Future[Unit] = Future.successful(())

  override def initializeDatabase(tenant: Tenant): Future[Seq[Res.DocOk]] = {
    Future.successful(Seq())
  }
}
