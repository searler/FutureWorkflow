package cognitiveentity.workflow

import org.specs2.mutable._
import akka.dispatch.Future

object FlowsTest extends org.specs2.mutable.SpecificationWithJUnit {

  sequential

  implicit val acctLook: Lookup[Num, Acct] = Service(ValueMaps.acctMap)
  implicit val balLook: Lookup[Acct, Bal] = Service(ValueMaps.balMap)
  implicit val numLook = Service(ValueMaps.numMap)

  "slb" in {
    SingleLineBalance.apply(Num("124-555-1234")).get must beEqualTo(Bal(124.5F))
    SingleLineBalance.apply(Num("124-555-1234")).await.result must beEqualTo(Some(Bal(124.5F)))
    SingleLineBalance.apply(Num("xxx")).await.result must beEqualTo(None)
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

  case class Service[K, V](values: Map[K, V]) extends Lookup[K, V] {
    def apply(arg: K): Future[V] = Future(values(arg))
//       def apply(arg: K): Future[V] = new AlreadyCompletedFuture(new Right((values(arg))))

  }

}