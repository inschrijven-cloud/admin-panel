package services

import javax.inject.{Inject, Singleton}

import be.thomastoye.speelsysteem.ConfigurationException
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

case class CloudantConfig @Inject() (configuration: Configuration) {
  lazy val host = configuration.getString("cloudant.host").getOrElse(throw new ConfigurationException("cloudant.host"))
  lazy val port = configuration.getInt("cloudant.port").getOrElse(throw new ConfigurationException("cloudant.port"))
  lazy val user = configuration.getString("cloudant.user").getOrElse(throw new ConfigurationException("cloudant.user"))
  lazy val pass = configuration.getString("cloudant.pass").getOrElse(throw new ConfigurationException("cloudant.pass"))
}

@Singleton
class CloudantTenantsService @Inject() (wsClientProvider: WSClientProvider, cloudantConfig: CloudantConfig)
  extends TenantsService
{
  val client = CouchDb(
    cloudantConfig.host,
    cloudantConfig.port,
    https = true,
    cloudantConfig.user,
    cloudantConfig.pass
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
