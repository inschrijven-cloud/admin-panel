package services

import javax.inject.{Inject, Singleton}

import be.thomastoye.speelsysteem.models.Tenant
import com.ibm.couchdb.Res.{DocOk, Ok}
import com.ibm.couchdb._
import play.api.Logger
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait TenantsService {
  def all: Future[Seq[Tenant]]
  def create(tenant: Tenant): Future[Res.Ok]
  def details(tenant: Tenant): Future[Unit] // does nothing yet
  def initializeDatabase(tenant: Tenant): Future[Seq[DocOk]]
  def syncTo(tenant: Tenant, remote: CouchDBConfig): Future[JsValue]
  def syncFrom(tenant: Tenant, remote: CouchDBConfig): Future[JsValue]
}

object TenantsService {
  case class TenantInfo(tenant: Tenant, databases: Seq[String])
}

@Singleton
class CouchdbTenantsService @Inject()(databaseService: TenantDatabaseService) extends TenantsService
{
  override def all: Future[Seq[Tenant]] = {
    databaseService.all map { dbs =>
      dbs
        .map(_.value)
        .filter(name => {
          name.startsWith("ic-")
        })
        .map(_.drop("ic-".length)) // remove prefix
        .flatMap(Tenant.create)
    }
  }

  override def create(tenant: Tenant): Future[Ok] = {
    databaseService.create(tenant.databaseName)
  }

  override def details(tenant: Tenant): Future[Unit] = Future.successful(())

  override def initializeDatabase(tenant: Tenant): Future[Seq[DocOk]] = {
    def viewAll(kind: String): CouchView = {
      CouchView(map =
        s"""
           |function(doc) {
           |  if(doc.kind === '$kind') {
           |    emit([doc.kind, doc._id], doc._id);
           |  }
           |}
        """.stripMargin)
    }

    val designDocs: Map[String, CouchView] = Map(
      "all-children" -> viewAll("type/child/v1"),
      "all-crew" -> viewAll("type/crew/v1"),
      "all-child-attendances" -> viewAll("type/childattendance/v2"),
      "all-days" -> viewAll("type/day/v1"),
      "all-contactperson" -> viewAll("type/contactperson/v1")
    )

    case class Revs(childRev: String, crewRev: String)

    Future.sequence(
      designDocs.map { designDoc =>
        Logger.info(s"Initializing database ${tenant.databaseName.value} for tenant ${tenant.normalizedName}")

        val name = tenant.databaseName.value.split("-").last
        val exists = databaseService.designDocExists(tenant.databaseName, name)
        val design = designDocs.get(tenant.databaseName.value)

        exists.flatMap { rev =>
          databaseService.createDesignDoc(tenant.databaseName, CouchDesign(name, _rev = rev.getOrElse(""), views = Map(designDoc)))
        }
      }.toSeq)
  }

  override def syncTo(tenant: Tenant, remote: CouchDBConfig): Future[JsValue] = {
      databaseService.startReplicationToRemote(tenant.databaseName, remote)
    }

  override def syncFrom(tenant: Tenant, remote: CouchDBConfig): Future[JsValue] = {
    databaseService.startReplicationFromRemote(remote, tenant.databaseName)
  }
}
