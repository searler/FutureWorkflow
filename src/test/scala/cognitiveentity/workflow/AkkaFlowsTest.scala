package cognitiveentity.workflow

case class ActorService[K, V](values: Map[K, V]) extends Lookup[K, V] {
  import akka.actor.Actor
  import akka.dispatch.Future

  val act = Actor.actorOf(new SActor).start

  def apply(arg: K): Future[V] = (act ? arg).map { _.asInstanceOf[V] }

  private class SActor extends Actor {
    def receive = {
      case a => self.reply(values(a.asInstanceOf[K]))
    }
  }

}

object AkkaFlowsTest extends org.specs2.mutable.SpecificationWithJUnit {

  implicit val acctLook: Lookup[Num, Acct] = ActorService(ValueMaps.acctMap)
  implicit val balLook: Lookup[Acct, Bal] = ActorService(ValueMaps.balMap)
  implicit val numLook = ActorService(ValueMaps.numMap)

  "slb" in {
    SingleLineBalance.apply(Num("124-555-1234")).get must beEqualTo(Bal(124.5F))
    SingleLineBalance.apply(Num("124-555-1234")).await.result must beEqualTo(Some(Bal(124.5F)))
    val noResult = SingleLineBalance.apply(Num("xxx")).await
    noResult.result must beEqualTo(None)
    noResult.exception.get.getMessage() must beEqualTo("key not found: Num(xxx)")
  }

  "rnb" in {
    RecoverNumBalance.apply(Num("124-555-1234")).get must beEqualTo(Bal(124.5F))
    RecoverNumBalance.apply(null).get must beEqualTo(Bal(0F))
    RecoverNumBalance.apply(Num("xxxx")).get must beEqualTo(Bal(0.0F))
  }

  "fnbIf" in {
    IfNumBalance.apply(Num("124-555-1234")).get must beEqualTo(Bal(124.5F))
    IfNumBalance.apply(Num("999-555-1234")).get must beEqualTo(Bal(0F))
    IfNumBalance.apply(null).get must beEqualTo(Bal(0F))
  }

  "bbm" in {
    BalanceByMap.apply(Id(123)).get must beEqualTo(Bal(125.5F))
  }

  "bsbf" in {
    BalancesByFor.apply(Id(123)).get must beEqualTo(List(Bal(124.5F), Bal(1.0F)))
  }

  "bsbm" in {
    BalancesByMap.apply(Id(123)).get must beEqualTo(List(Bal(124.5F), Bal(1.0F)))
  }

}