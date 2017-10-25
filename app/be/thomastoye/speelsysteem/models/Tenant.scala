package be.thomastoye.speelsysteem.models

import models.DbName

object Tenant {

  private case class TenantImpl(normalizedName: String) extends Tenant {
    override def databaseName: DbName = DbName.create("ic" + normalizedName).get
  }

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

  def databaseName: DbName
}
