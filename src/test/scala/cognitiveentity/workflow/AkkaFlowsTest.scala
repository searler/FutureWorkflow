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
import scala.concurrent.Future
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import akka.actor.Props
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import scala.concurrent.Promise

@RunWith(classOf[JUnitRunner]) class AkkaFlowsTest extends org.specs2.mutable.SpecificationWithJUnit {

  implicit val system = ActorSystem("MySystem")
  implicit val ec = system.dispatcher

  implicit val acctLook: Lookup[Num, Acct] = ActorService(ValueMaps.acctMap)
  implicit val balLook: Lookup[Acct, Bal] = ActorService(ValueMaps.balMap)
  implicit val numLook = ActorService(ValueMaps.numMap)

  import Getter._

  "slb" in {
    SingleLineBalance.apply(Num("124-555-1234")).get must beEqualTo(Bal(124.5F))
    SingleLineBalance.apply(Num("124-555-1234")).option must beEqualTo(Some(Bal(124.5F)))
    SingleLineBalance.apply(Num("xxx")).exception must beEqualTo("key not found: Num(xxx)")
    SingleLineBalance.apply(Num("xxx")).option must beEqualTo(None)
  }

  "rnb" in {
    RecoverNumBalance.apply(Num("124-555-1234")).get must beEqualTo(Bal(190.5F))
    RecoverNumBalance.apply(Num("xxxx")).get must beEqualTo(Bal(66F))
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
    Future.traverse((0 until cnt).toList) { _ => SingleLineBalance.apply(Num("124-555-1234")) }.get.size must beEqualTo(cnt)

  }

  case class ActorService[K, V](values: Map[K, V])(implicit m: Manifest[V], system: ActorSystem) extends Lookup[K, V] {
    import akka.actor.Actor

    val act = system.actorOf(Props(new SActor))
    implicit val ec = system.dispatcher

    import akka.pattern.ask

    implicit val timeout = Timeout(100, TimeUnit.MILLISECONDS)

    //mapTo only needed because Map can contain null
    def apply(arg: K): Future[V] = (act ? arg).mapTo[Option[V]].collect {
      case Some(v) => v
      case None => throw new NoSuchElementException("key not found: " + arg)
    }

    private class SActor extends Actor {
      def receive = {
        case a => sender ! values.get(a.asInstanceOf[K])
      }
    }

  }

}

