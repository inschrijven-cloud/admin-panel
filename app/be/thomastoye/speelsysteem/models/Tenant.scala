package be.thomastoye.speelsysteem.models

import models.DbName

object Tenant {

  private case class TenantImpl(nomalizedName: String) extends Tenant {
    def dataDatabaseName: DbName = DbName.create(TenantDataDatabasePrefix + nomalizedName).get
    def metadataDatabaseName: DbName = DbName.create(TenantMetadataDatabasePrefix + nomalizedName).get
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
  val nomalizedName: String

  def dataDatabaseName: DbName
  def metadataDatabaseName: DbName
}
