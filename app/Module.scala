import com.google.inject.AbstractModule
import services.{CloudantDatabaseService, CloudantTenantsService, DatabaseService, TenantsService}

class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[TenantsService]).to(classOf[CloudantTenantsService])
    bind(classOf[DatabaseService]).to(classOf[CloudantDatabaseService])
  }

}
