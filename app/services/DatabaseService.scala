package services

import javax.inject.{Inject, Singleton}

import be.thomastoye.speelsysteem.ConfigurationException
import models.DbName
import play.api.Configuration
import com.ibm.couchdb._
import play.api.libs.ws.ahc.WSClientProvider
import play.api.libs.concurrent.Execution.Implicits._
import util.TaskExtensionOps

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

trait DatabaseService {
  def all: Future[Seq[DbName]]
  def create(db: DbName): Future[Res.Ok]
  def details(db: DbName): Future[Res.DbInfo]
  def drop(db: DbName): Future[Res.Ok]
}

case class CloudantConfig @Inject() (configuration: Configuration) {
  lazy val host = configuration.getString("cloudant.host").getOrElse(throw new ConfigurationException("cloudant.host"))
  lazy val port = configuration.getInt("cloudant.port").getOrElse(throw new ConfigurationException("cloudant.port"))
  lazy val user = configuration.getString("cloudant.user").getOrElse(throw new ConfigurationException("cloudant.user"))
  lazy val pass = configuration.getString("cloudant.pass").getOrElse(throw new ConfigurationException("cloudant.pass"))
}

@Singleton
class CloudantDatabaseService @Inject() (wsClientProvider: WSClientProvider, cloudantConfig: CloudantConfig)
  extends DatabaseService
{
  val client = CouchDb(
    cloudantConfig.host,
    cloudantConfig.port,
    https = true,
    cloudantConfig.user,
    cloudantConfig.pass
  )

  override def all = {
    new TaskExtensionOps(client.dbs.getAll).runFuture().map(_.map(DbName.create(_).get))
  }

  override def create(db: DbName) = {
    new TaskExtensionOps(client.dbs.create(db.value)).runFuture()
  }

  override def details(db: DbName) = {
    new TaskExtensionOps(client.dbs.get(db.value)).runFuture()
  }

  override def drop(db: DbName) = {
    new TaskExtensionOps(client.dbs.delete(db.value)).runFuture()
  }
}
