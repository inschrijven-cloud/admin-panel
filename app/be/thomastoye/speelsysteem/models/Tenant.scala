package be.thomastoye.speelsysteem.models

import models.DbName

object Tenant {

  private case class TenantImpl(normalizedName: String) extends Tenant {
    private val databases = Seq("children", "crew", "days")

    override def dataDatabases: Seq[DbName] = databases.map(name => DbName.create(TenantDataDatabasePrefix + normalizedName + "-" + name).get)
    override def metadataDatabaseName: DbName = DbName.create(TenantMetadataDatabasePrefix + normalizedName).get
  }

  final val TenantMetadataDatabasePrefix = "tenant-meta-"
  final val TenantDataDatabasePrefix = "tenant-data-"

  def create(normalizedName: String): Option[Tenant] = {
    if (normalizedName.matches("""^([a-z]|[0-9]|_|\$|\(|\)|\+|\-|\/)*$""")) {
      Some(TenantImpl(normalizedName))
    } else {
      None
    }
  }
}

sealed trait Tenant {
  val normalizedName: String

  def dataDatabases: Seq[DbName]
  def metadataDatabaseName: DbName
}
