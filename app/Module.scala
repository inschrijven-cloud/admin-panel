import com.google.inject.AbstractModule
import services.{CloudantTenantDatabaseService, CloudantTenantsService, TenantDatabaseService, TenantsService}

class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[TenantsService]).to(classOf[CloudantTenantsService])
    bind(classOf[TenantDatabaseService]).to(classOf[CloudantTenantDatabaseService])
  }

}
