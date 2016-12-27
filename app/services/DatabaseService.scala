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

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

trait DatabaseService {
  def all: Future[Seq[DbName]]

  def create(db: DbName): Future[Res.Ok]

  def details(db: DbName): Future[Res.DbInfo]

  def drop(db: DbName): Future[Res.Ok]

  def createViews(db: DbName): Future[Seq[Res.DocOk]]
}

case class CloudantConfig @Inject()(configuration: Configuration) {
  lazy val host = configuration.getString("cloudant.host").getOrElse(throw new ConfigurationException("cloudant.host"))
  lazy val port = configuration.getInt("cloudant.port").getOrElse(throw new ConfigurationException("cloudant.port"))
  lazy val https = configuration.getBoolean("cloudant.https").getOrElse(true)
  lazy val user = configuration.getString("cloudant.user")
  lazy val pass = configuration.getString("cloudant.pass")
}

@Singleton
class CloudantDatabaseService @Inject()(wsClientProvider: WSClientProvider, cloudantConfig: CloudantConfig)
  extends DatabaseService {

  val client = (for {
    user <- cloudantConfig.user
    pass <- cloudantConfig.pass
  } yield {
    CouchDb(
      cloudantConfig.host,
      cloudantConfig.port,
      https = cloudantConfig.https,
      user,
      pass
    )
  }) getOrElse CouchDb(
    cloudantConfig.host,
    cloudantConfig.port,
    https = cloudantConfig.https
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

  override def createViews(db: DbName): Future[Seq[DocOk]] = {
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

    case class Revs(childRev: String, crewRev: String)

    val childRevFuture = new TaskExtensionOps(client.db(db.value, TypeMapping.empty).design.get("child")).runFuture().map(t => Some(t._rev)).recover {
      case e: CouchException[_] => None
    }

    val crewRevFuture = new TaskExtensionOps(client.db(db.value, TypeMapping.empty).design.get("crew")).runFuture().map(t => Some(t._rev)).recover {
      case e: CouchException[_] => None
    }

    Future.sequence(Seq(childRevFuture, crewRevFuture))
      .map(s => Revs(s(0).getOrElse(""), s(1).getOrElse("")))
      .flatMap { revs =>

        val childDesign = CouchDesign("child", _rev = revs.childRev, views = Map(
          "all" -> viewAll("type/child/v1")
        ))

        val crewDesign = CouchDesign("crew", _rev = revs.crewRev, views = Map(
          "all" -> viewAll("type/crew/v1")
        ))

        Logger.debug("Got design docs current revs: " + revs)

        Future.sequence(Seq(
          new TaskExtensionOps(client.db(db.value, TypeMapping.empty).design.create(childDesign)).runFuture(),
          new TaskExtensionOps(client.db(db.value, TypeMapping.empty).design.create(crewDesign)).runFuture()
        ))
      }
  }
}
