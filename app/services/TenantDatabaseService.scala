package services

import javax.inject.{Inject, Singleton}

import be.thomastoye.speelsysteem.ConfigurationException
import com.ibm.couchdb.Res.DocOk
import models.DbName
import play.api.{Configuration, Logger}
import com.ibm.couchdb._
import play.api.libs.ws.ahc.WSClientProvider
import play.api.libs.concurrent.Execution.Implicits._
import util.TaskExtensionOps

import scala.concurrent.Future

trait TenantDatabaseService {
  def all: Future[Seq[DbName]]

  def create(db: DbName): Future[Res.Ok]

  def details(db: DbName): Future[Res.DbInfo]

  def drop(db: DbName): Future[Res.Ok]
}

case class CouchDBConfig @Inject()(configuration: Configuration) {
  lazy val host = configuration.getString("couchdb.host").getOrElse(throw new ConfigurationException("couchdb.host"))
  lazy val port = configuration.getInt("couchdb.port").getOrElse(throw new ConfigurationException("couchdb.port"))
  lazy val https = configuration.getBoolean("couchdb.https").getOrElse(true)
  lazy val user = configuration.getString("couchdb.user")
  lazy val pass = configuration.getString("couchdb.pass")
}

@Singleton
class CloudantTenantDatabaseService @Inject()(wsClientProvider: WSClientProvider, couchdbConfig: CouchDBConfig)
  extends TenantDatabaseService {

  val client = (for {
    user <- couchdbConfig.user
    pass <- couchdbConfig.pass
  } yield {
    CouchDb(
      couchdbConfig.host,
      couchdbConfig.port,
      https = couchdbConfig.https,
      user,
      pass
    )
  }) getOrElse CouchDb(
    couchdbConfig.host,
    couchdbConfig.port,
    https = couchdbConfig.https
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
