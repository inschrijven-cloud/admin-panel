package models

sealed trait DbName { val value: String }

object DbName {
  private case class DbNameImpl(value: String) extends DbName
  def create(value: String): Option[DbName] = {
    if(value.matches("""^([a-z]|[0-9]|_|\$|\(|\)|\+|\-|\/)*$""")){
      Some(DbNameImpl(value))
    } else {
      None
    }
  }
}
