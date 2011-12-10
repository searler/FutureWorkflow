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
import akka.dispatch.Future
 import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner

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


 @RunWith(classOf[JUnitRunner])
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
    IfNumBalance.apply(Num("999-555-1234")).get must beEqualTo(Bal(55F))
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
  
   "slb loop" in {
     val cnt = 50
    Future.traverse( ( 0 until cnt).toList){ _ =>SingleLineBalance.apply(Num("124-555-1234"))}.get.size must beEqualTo(cnt)
   
    }

}