import models.DbName
import org.scalatestplus.play._

import scala.collection.mutable

class DbNameTest extends PlaySpec {

  "DbName#create" must {
    "create DbNames for valid input" in {
      DbName.create("aoeu") mustBe defined
      DbName.create("ao55e57u8") mustBe defined
      DbName.create("_____") mustBe defined
      DbName.create("_$()+-/") mustBe defined
    }

    "return None for invalid input" in {
      DbName.create("aoeU") must not be defined
      DbName.create("aoeu{)(*&^%$#") must not be defined
      DbName.create("ao eu") must not be defined
    }
  }
}