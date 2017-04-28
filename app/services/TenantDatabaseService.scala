package services

import javax.inject.{Inject, Singleton}

import be.thomastoye.speelsysteem.ConfigurationException
import com.ibm.couchdb.Res.DocOk
import models.DbName
import play.api.Configuration
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

  /**
    * Check if a design doc exists
    * @return Maybe the rev of the existing design doc
    */
  def designDocExists(db: DbName, designName: String): Future[Option[String]]

  def createDesignDoc(db: DbName, couchDesign: CouchDesign): Future[DocOk]
}

case class CouchDBConfig @Inject()(configuration: Configuration) {
  lazy val host: String = configuration.getString("couchdb.host").getOrElse(throw new ConfigurationException("couchdb.host"))
  lazy val port: Int = configuration.getInt("couchdb.port").getOrElse(throw new ConfigurationException("couchdb.port"))
  lazy val https: Boolean = configuration.getBoolean("couchdb.https").getOrElse(true)
  lazy val user: Option[String] = configuration.getString("couchdb.user")
  lazy val pass: Option[String] = configuration.getString("couchdb.pass")
}

@Singleton
class CouchdbTenantDatabaseService @Inject()(wsClientProvider: WSClientProvider, couchdbConfig: CouchDBConfig)
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

  override def designDocExists(dbName: DbName, designName: String): Future[Option[String]] = {
    new TaskExtensionOps(client.db(dbName.value, TypeMapping.empty).design.get(designName)).runFuture().map(t => Some(t._rev)).recover {
      case e: CouchException[_] => None
    }
  }

  override def createDesignDoc(db: DbName, couchDesign: CouchDesign): Future[DocOk] = {
    new TaskExtensionOps(client.db(db.value, TypeMapping.empty).design.create(couchDesign)).runFuture()
  }
}
