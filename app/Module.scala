import com.google.inject.AbstractModule
import services.{TenantsService, CloudantTenantsService}

class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[TenantsService]).to(classOf[CloudantTenantsService])
  }

}
