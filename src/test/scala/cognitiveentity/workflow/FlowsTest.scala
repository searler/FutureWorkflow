package cognitiveentity.workflow

import org.specs2.mutable._
import akka.dispatch.Future

object FlowsTest extends org.specs2.mutable.SpecificationWithJUnit {

  sequential

  implicit val acctLook: Lookup[Num, Acct] = Service(ValueMaps.acctMap)
  implicit val balLook: BalLookup = BalService
  implicit val stdBalLook: Lookup[Acct, Bal] = StdBalService
  implicit val special = SpecialService
  implicit val numLook = Service(ValueMaps.numMap)
  
  "phones" in {
    Phones(Id(123)).get must beEqualTo(List(Num("124-555-1234"), Num("333-555-1234")))
    Phones(Id(-1)).await.exception.get.getMessage() must beEqualTo("key not found: Id(-1)")
  }

  "special" in {
    SpecialLineBalance(Num("124-555-1234")).get must beEqualTo(Bal(1000F))
  }

  "other" in {
    OtherLineBalance(Num("124-555-1234")).get must beEqualTo(Bal(13F))

  }
  
   "split" in {
    SplitLineBalance(Num("124-555-1234")).get must beEqualTo(Bal(1124.5F))
    SplitLineBalance(Num("124-555-1234")).await.result must beEqualTo(Some(Bal(1124.5F)))
    val noResult = SplitLineBalance(Num("xxx")).await
    noResult.result must beEqualTo(None)
    noResult.exception.get.getMessage() must beEqualTo("key not found: Num(xxx)")
  }

  "slb" in {
    SingleLineBalance(Num("124-555-1234")).get must beEqualTo(Bal(124.5F))
    SingleLineBalance(Num("124-555-1234")).await.result must beEqualTo(Some(Bal(124.5F)))
    val noResult = SingleLineBalance(Num("xxx")).await
    noResult.result must beEqualTo(None)
    noResult.exception.get.getMessage() must beEqualTo("key not found: Num(xxx)")
  }

  "rnb" in {
    RecoverNumBalance(Num("124-555-1234")).get must beEqualTo(Bal(124.5F))
    RecoverNumBalance(null).get must beEqualTo(Bal(0F))
    RecoverNumBalance(Num("xxxx")).get must beEqualTo(Bal(0.0F))
  }

  "fnbIf" in {
    IfNumBalance(Num("124-555-1234")).get must beEqualTo(Bal(124.5F))
    IfNumBalance(Num("999-555-1234")).get must beEqualTo(Bal(55F))
    IfNumBalance(null).get must beEqualTo(Bal(0F))
  }

  "bbm" in {
    BalanceByMap(Id(123)).get must beEqualTo(Bal(125.5F))
  }

  "bsbf" in {
    BalancesByFor(Id(123)).get must beEqualTo(List(Bal(124.5F), Bal(1.0F)))
  }

  "bsbm" in {
    BalancesByMap(Id(123)).get must beEqualTo(List(Bal(124.5F), Bal(1.0F)))
  }

  case class Service[K, V](values: Map[K, V]) extends MapService(values) with Lookup[K, V]

  abstract class MapService[K, V](map: Map[K, V]) {
    self: Function1[K, Future[V]] =>
    def apply(a: K) = Future(map(a))
  }

  case object StdBalService extends MapService(ValueMaps.balMap) with Lookup[Acct, Bal]

  case object BalService extends MapService(Map(Acct("alpha") -> Bal(13F))) with BalLookup

  case object SpecialService extends MapService(Map(Acct("alpha") -> Bal(1000F))) with SpecialBalLookup

}