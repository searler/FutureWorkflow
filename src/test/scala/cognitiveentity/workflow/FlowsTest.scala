
/* Copyright (c) 2010 Richard Searle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Richard Searle
 */
package cognitiveentity.workflow

import org.specs2.mutable._
import akka.dispatch.Future

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
object FlowsTest extends org.specs2.mutable.SpecificationWithJUnit {

  private implicit val acctLook: Lookup[Num, Acct] = Service(ValueMaps.acctMap)
  private implicit val balLook: BalLookup = BalService
  private implicit val stdBalLook: Lookup[Acct, Bal] = StdBalService
  private implicit val special = SpecialService
  private implicit val numLook = Service(ValueMaps.numMap)
  private implicit val discountLook = Service(ValueMaps.discountMap)

  "phones" in {
    Phones(Id(123)).get must beEqualTo(List(Num("124-555-1234"), Num("333-555-1234")))
    Phones(Id(-1)).await.exception.get.getMessage() must beEqualTo("key not found: Id(-1)")
  }

  "balance" in {
    Balance(Acct("alpha")).get must beEqualTo(Bal(124.5F))
  }

  "discount" in {
    Discount(Acct("alpha")).get must beEqualTo(Bal(124.5F * 0.9F))
    Discount(Acct("beta")).get must beEqualTo(Bal(1.0F))
  }

  "discount by Phone number" in {
    DiscountByPhone(Num("124-555-1234")).get must beEqualTo(Bal(124.5F * 0.9F))
    DiscountByPhone(Num("333-555-1234")).get must beEqualTo(Bal(1.0F))
  }

  "discount by id" in {
    DiscountById(Id(123)).get must beEqualTo(Bal(124.5F * 0.9F + 1.0F))
  }

  "special" in {
    SpecialLineBalance(Num("124-555-1234")).get must beEqualTo(Bal(1000F))
  }

  "other" in {
    OtherLineBalance(Num("124-555-1234")).get must beEqualTo(Bal(13F))
  }

  "noop" in {
    import akka.dispatch.Future
    import akka.dispatch.Futures
    NoOp(Num("124-555-1234")).get must beEqualTo(Num("124-555-1234"))
  }

  "noop optimized" in {
    NoOpOptimized(Num("124-555-1234")).get must beEqualTo(Num("124-555-1234"))
  }

  "splitFiltered" in {
    SplitLineBalanceFiltered(Num("124-555-1234")).get must beEqualTo(Bal(1124.5F))
    SplitLineBalanceFiltered(Num("333-555-1234")).result must beEqualTo(None)
  }

  "split first" in {
    val result = SplitLineBalanceFirst(Num("124-555-1234")).get
    List(Bal(124.5F), Bal(1000F)).contains(result) must beTrue
  }

  "split common" in {
    SplitLineBalanceCommon(Num("124-555-1234")).get must beEqualTo(Bal(1124.5F))
  }

  "split serial" in {
    SplitLineBalanceSerial(Num("124-555-1234")).get must beEqualTo(Bal(1124.5F))
  }

  "split common map" in {
    SplitLineBalanceCommonMap(Num("124-555-1234")).get must beEqualTo(Bal(1124.5F))
  }

  "split list" in {
    SplitLineBalanceList(Num("124-555-1234")).get must beEqualTo(List(Bal(124.5F), Bal(1000.0F)))
  }

  "split tuple" in {
    SplitLineBalanceTuple(Num("124-555-1234")).get must beEqualTo((Num("124-555-1234"), Acct("alpha"), Bal(124.5F), Bal(1000.0F)))
  }

  "split tuple2" in {
    SplitLineBalanceTuple2(Num("124-555-1234")).get must beEqualTo((Bal(124.5F), Bal(1000.0F)))
  }

  "split tuple2 id" in {
    SplitLineBalanceTuple2Id(Num("124-555-1234")).get must beEqualTo((Num("124-555-1234"), Bal(124.5F), Bal(1000.0F)))
  }

  "split" in {
    SplitLineBalance(Num("124-555-1234")).get must beEqualTo(Bal(1124.5F))
    SplitLineBalance(Num("124-555-1234")).await.result must beEqualTo(Some(Bal(1124.5F)))
    val noResult = SplitLineBalance(Num("xxx")).await
    noResult.result must beEqualTo(None)
    noResult.exception.get.getMessage() must beEqualTo("key not found: Num(xxx)")
  }

  "slbna" in {
    SingleLineBalanceNoArgs(Num("124-555-1234")).get must beEqualTo(Bal(124.5F))
  }

  "slb doubled" in {
    SingleLineBalanceDoubled(Num("124-555-1234")).get must beEqualTo(Bal(249F))
  }

  "slb" in {
    SingleLineBalance(Num("124-555-1234")).get must beEqualTo(Bal(124.5F))
    SingleLineBalance(Num("124-555-1234")).await.result must beEqualTo(Some(Bal(124.5F)))
    val noResult = SingleLineBalance(Num("xxx")).await
    noResult.result must beEqualTo(None)
    noResult.exception.get.getMessage() must beEqualTo("key not found: Num(xxx)")
  }

  "rnb" in {
    RecoverNumBalance(Num("124-555-1234")).get must beEqualTo(Bal(190.5F))
    RecoverNumBalance(null).get must beEqualTo(Bal(66F))
    RecoverNumBalance(Num("xxxx")).get must beEqualTo(Bal(66F))
  }

  "fnbIf" in {
    IfNumBalance(Num("124-555-1234")).get must beEqualTo(Bal(124.5F))
    IfNumBalance(Num("999-555-1234")).get must beEqualTo(Bal(55F))
    IfNumBalance(null).get must beEqualTo(Bal(0F))
  }

  "bbm" in {
    BalanceByMap(Id(123)).get must beEqualTo(Bal(125.5F))
  }

  "balance parallel" in {
    BalanceParallel(Id(123)).get must beEqualTo(Bal(125.5F))
  }

  "bsbf" in {
    BalancesByFor(Id(123)).get must beEqualTo(List(Bal(124.5F), Bal(1.0F)))
  }

  "bsbm" in {
    BalancesByMap(Id(123)).get must beEqualTo(List(Bal(124.5F), Bal(1.0F)))
  }

  "accounts by for" in {
    AccountsByFor(Id(123)).get must beEqualTo(List(Acct("alpha"), Acct("beta")))
  }

  "accounts by traverse" in {
    AccountsByTraverse(Id(123)).get must beEqualTo(List(Acct("alpha"), Acct("beta")))
  }

  private abstract class MapService[K, V](map: Map[K, V]) {
    def apply(a: K) = Future(map(a))
  }

  private case class Service[K, V](values: Map[K, V]) extends MapService(values) with Lookup[K, V]

  private case object StdBalService extends MapService(ValueMaps.balMap) with Lookup[Acct, Bal]

  private case object BalService extends MapService(Map(Acct("alpha") -> Bal(13F))) with BalLookup

  private case object SpecialService extends MapService(Map(Acct("alpha") -> Bal(1000F),
    Acct("beta") -> Bal(1234F))) with SpecialBalLookup

}