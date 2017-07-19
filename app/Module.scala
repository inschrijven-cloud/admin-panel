import com.google.inject.AbstractModule
import services._

class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[TenantsService]).to(classOf[CouchdbTenantsService])
    bind(classOf[TenantDatabaseService]).to(classOf[CouchdbTenantDatabaseService])
    bind(classOf[CouchDBConfig]).to(classOf[AutoCouchDBConfig])
  }

}
