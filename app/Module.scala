import com.google.inject.AbstractModule
import services.{CouchdbTenantDatabaseService, CouchdbTenantsService, TenantDatabaseService, TenantsService}

class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[TenantsService]).to(classOf[CouchdbTenantsService])
    bind(classOf[TenantDatabaseService]).to(classOf[CouchdbTenantDatabaseService])
  }

}
