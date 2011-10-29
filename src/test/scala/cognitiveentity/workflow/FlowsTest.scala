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

  "special" in {
    SpecialLineBalance.apply(Num("124-555-1234")).get must beEqualTo(Bal(1000F))
  }

  "other" in {
    OtherLineBalance.apply(Num("124-555-1234")).get must beEqualTo(Bal(13F))

  }

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

  case class Service[K, V](values: Map[K, V]) extends MapService(values) with Lookup[K, V]

  abstract class MapService[K, V](map: Map[K, V]) {
    self: Function1[K, Future[V]] =>
    def apply(a: K) = Future(map(a))
  }

  case object StdBalService extends MapService(ValueMaps.balMap) with Lookup[Acct, Bal]

  case object BalService extends MapService(Map(Acct("alpha") -> Bal(13F))) with BalLookup

  case object SpecialService extends MapService(Map(Acct("alpha") -> Bal(1000F))) with SpecialBalLookup

}