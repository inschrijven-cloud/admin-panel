package services

import javax.inject.{Inject, Singleton}

import be.thomastoye.speelsysteem.models.Tenant
import com.ibm.couchdb.Res.{DocOk, Ok}
import com.ibm.couchdb._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

trait TenantsService {
  def all: Future[Seq[Tenant]]
  def create(tenant: Tenant): Future[Res.Ok]
  def details(tenant: Tenant): Future[Unit] // does nothing yet
  def initializeDatabase(tenant: Tenant): Future[Seq[DocOk]]
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

    val designDocs: Map[String, Map[String, CouchView]] = Map(
      "children" -> Map("all" -> viewAll("type/child/v1")),
      "crew" -> Map("all" -> viewAll("type/crew/v1")),
      "childattendance" -> Map("all" -> viewAll("type/childattendance/v1")),
      "days" -> Map("all" -> viewAll("type/day/v1")),
      "contactpeople" -> Map("all" -> viewAll("type/contactperson/v1"))
    )

    case class Revs(childRev: String, crewRev: String)

    Future.sequence(tenant.dataDatabases
      .flatMap { dbName =>
        designDocs.find(x => dbName.value.endsWith("-" + x._1)).map(value => (dbName, value._2))
      }
      .map { case (dbName, designDoc) =>
        Logger.info(s"Initializing database ${dbName.value} for tenant ${tenant.normalizedName}")

        val name = dbName.value.split("-").last
        val exists = databaseService.designDocExists(dbName, name)
        val design = designDocs.get(dbName.value)

        exists.flatMap { rev =>
          databaseService.createDesignDoc(dbName, CouchDesign(name, _rev = rev.getOrElse(""), views = designDoc))
        }
      })
  }
}
