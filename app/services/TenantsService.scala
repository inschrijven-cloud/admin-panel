package services

import javax.inject.{Inject, Singleton}

import models.DbName
import play.api.Configuration
import com.ibm.couchdb._
import play.api.libs.ws.WSAuthScheme
import play.api.libs.ws.ahc.WSClientProvider
import play.api.libs.concurrent.Execution.Implicits._
import util.TaskExtensionOps

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}
import scala.concurrent.{Future, Promise}
import scalaz.concurrent.Task

trait TenantsService {
  def allDbs: Future[Seq[DbName]]
  def createDb(db: DbName): Future[Res.Ok]
  def dbDetails(db: DbName): Future[Res.DbInfo]
}

@Singleton
class CloudantTenantsService @Inject() (wsClientProvider: WSClientProvider, configuration: Configuration)
  extends TenantsService
{
  val client = CouchDb(
    configuration.getString("cloudant.host").get,
    configuration.getInt("cloudant.port").get,
    https = true, configuration.getString("cloudant.user").get,
    configuration.getString("cloudant.pass").get
  )

  override def allDbs = {
    new TaskExtensionOps(client.dbs.getAll).runFuture().map(_.map(DbName.create(_).get))
  }

  override def createDb(db: DbName) = {
    new TaskExtensionOps(client.dbs.create(db.value)).runFuture()
  }

  override def dbDetails(db: DbName) = {
    new TaskExtensionOps(client.dbs.get(db.value)).runFuture()
  }
}
