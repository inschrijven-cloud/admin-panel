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
import com.netaporter.uri.dsl._
import com.netaporter.uri.Uri
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json.{Format, JsValue, Json}

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

  def startReplicationToRemote(db: DbName, target: CouchDBConfig): Future[JsValue]
  def startReplicationFromRemote(db: CouchDBConfig, target: DbName): Future[JsValue]
}

trait CouchDBConfig {
  val host: String
  val port: Int
  val https: Boolean
  val user: Option[String]
  val pass: Option[String]

  def uri: Uri = {
    val uri = Uri.empty
      .withScheme(if(https) "https" else "http")
      .withHost(host)
      .withPort(port)

    (for {
      u <- user
      p <- pass
    } yield uri.withUser(u).withPassword(p)).getOrElse(uri)
  }
}

case class AutoCouchDBConfig @Inject()(configuration: Configuration) extends CouchDBConfig{
  lazy val host: String = configuration.getString("couchdb.host").getOrElse(throw new ConfigurationException("couchdb.host"))
  lazy val port: Int = configuration.getInt("couchdb.port").getOrElse(throw new ConfigurationException("couchdb.port"))
  lazy val https: Boolean = configuration.getBoolean("couchdb.https").getOrElse(true)
  lazy val user: Option[String] = configuration.getString("couchdb.user")
  lazy val pass: Option[String] = configuration.getString("couchdb.pass")
}

case class AutoRemoteCouchDBConfig @Inject()(configuration: Configuration) extends CouchDBConfig{
  lazy val host: String = configuration.getString("couchdb.remote.host").getOrElse(throw new ConfigurationException("couchdb.remote.host"))
  lazy val port: Int = configuration.getInt("couchdb.remote.port").getOrElse(throw new ConfigurationException("couchdb.remote.port"))
  lazy val https: Boolean = configuration.getBoolean("couchdb.remote.https").getOrElse(true)
  lazy val user: Option[String] = configuration.getString("couchdb.remote.user")
  lazy val pass: Option[String] = configuration.getString("couchdb.remote.pass")
}

case class ReplicationDocument(source: String, target: String)

@Singleton
class CouchdbTenantDatabaseService @Inject()(wsClientProvider: WSClientProvider, couchdbConfig: CouchDBConfig)
  extends TenantDatabaseService with StrictLogging {

  implicit val replicationDocumentForm: Format[ReplicationDocument] = Json.format[ReplicationDocument]

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

  override def startReplicationToRemote(db: DbName, target: CouchDBConfig): Future[JsValue] = {
    val url = target.uri / db.value
    val document = Json.toJson(ReplicationDocument(db.value, url))

    logger.info("Starting replication local --> remote. Replication document to post: " + document)

    wsClientProvider.get().url(couchdbConfig.uri / "_replicate").post(document).map(_.json)
  }

  override def startReplicationFromRemote(db: CouchDBConfig, target: DbName): Future[JsValue] = {
    val url = db.uri / target.value
    val document = Json.toJson(ReplicationDocument(url, target.value))

    logger.info("Starting replication remote --> local. Replication document to post: " + document)

    wsClientProvider.get().url(couchdbConfig.uri / "_replicate").post(document).map(_.json)
  }
}
