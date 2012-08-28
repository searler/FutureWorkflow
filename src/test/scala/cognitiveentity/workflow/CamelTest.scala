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
 * Simulate a real world deployment where services are remoted and accessed via
 * Camel (e.g. over JMS)
 *
 * Both Request/Response and fire-and-forget(oneway) interactions are tested.
 * In the latter case, the result is delivered to a fixed end point (e.g. a JMS
 * queue)
 *
 * @author Richard Searle
 */
package cognitiveentity.workflow

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import akka.actor._
import scala.concurrent.Future
import akka.camel._
 import java.util.concurrent.TimeUnit
import akka.util._

/**
 * Simulates external services that provides
 *  id             -> corresponding phone numbers
 *  phone number   -> account number
 *  account number -> balance
 *
 */
case class Responder[K, V](map: Map[K, V]) {
  def apply(a: Any) = map(a.asInstanceOf[K])
}

case class CamelService[K, V](aname: String)(implicit system:ActorSystem) extends Lookup[K, V] {
  import akka.actor.Actor
 import akka.pattern.ask
 import system.dispatcher

implicit val timeout = Timeout(100, TimeUnit.MILLISECONDS)

  val act = system.actorOf(Props(new SActor(aname)),name=aname)

  def apply(arg: K): Future[V] = (act ? arg).map {
    _ match {
      case CamelMessage(a: V, _) => a
    }
  }

}

 private class SActor(name:String) extends Actor with Producer {
    def endpointUri = "seda:" + name
  }

case class RRFlow[A, R](aname: String, flow: A => Future[R])(implicit system:ActorSystem) {

  val act = system.actorOf(Props (new FActor(aname,flow)),name=aname)

 
}

 private   class FActor[A,R](name:String,flow: A => Future[R]) extends  Consumer {
    def endpointUri = "seda:" + name
    import context._
    def receive = {
      case CamelMessage(a: A, _) => sender ! (Future(a).flatMap(flow))
    }
  }


case class OWFlow[A, R](in: String, out: String, flow: A => Future[R])(implicit system:ActorSystem) {

  val outActor = system.actorOf(Props(new OutActor(out)))
  val inActor = system.actorOf(Props(new InActor(in,flow,outActor)))
  

 
}

 private  class OutActor(out:String) extends Actor with  Producer with Oneway {
    val endpointUri = "seda:" + out
  }

  private  class InActor[A,R](in:String,flow: A => Future[R],outActor:ActorRef) extends Consumer {
    val endpointUri = "seda:" + in
    import context._
    def receive = {
      case CamelMessage(a: A, _) => (Future(a).flatMap(flow)).onComplete { outActor ! _ }
    }
  }

/**
 * Mechanism to asynchronously capture the results of a one-way
 * interaction.
 */
private object Gather {
  import java.util.concurrent._
  import java.util.concurrent.atomic._
  import scala.collection.mutable._
  val awaiter = new AtomicReference[CountDownLatch]
  val values = new ListBuffer[Any]

  //Reset the state and indicate the number of expected results
  def prep(expected: Int) {
    synchronized {
      values.clear
      awaiter.set(new CountDownLatch(expected))
    }
  }
  //wait for expected number of results to be received
  def await { awaiter.get.await(2, java.util.concurrent.TimeUnit.SECONDS) }

  //record the result
  def apply[A](arg: A) {
    synchronized {
      values += arg
      awaiter.get.countDown
    }
  }

  def get = synchronized { values toList }
}

@RunWith(classOf[JUnitRunner])
class CamelTest extends org.specs2.mutable.SpecificationWithJUnit {

import Getter._

  sequential

 implicit val system = ActorSystem("some-system")

  implicit val acctLook: Lookup[Num, Acct] = CamelService("acct")
  implicit val balLook: Lookup[Acct, Bal] = CamelService("bal")
  implicit val numLook: Lookup[Id, List[Num]] = CamelService("num")

  
   val camel = CamelExtension(system)
   val context = camel.context
   val template = camel.template
   implicit val ec = system.dispatcher

  step {
    import org.apache.camel.builder._

    context.addRoutes(new RouteBuilder(){
         def configure {
           from("seda:acct").bean(Responder(ValueMaps.acctMap))
         }
       })

 context.addRoutes(new RouteBuilder(){
         def configure {
           from("seda:num").bean(Responder(ValueMaps.numMap))
         }
       })

 context.addRoutes(new RouteBuilder(){
         def configure {
           from("seda:bal").bean(Responder(ValueMaps.balMap))
         }
       })

context.addRoutes(new RouteBuilder(){
         def configure {
           from("seda:gather").bean(Gather)
         }
       })


    // R-R flows
    val slb = new RRFlow("slb", SingleLineBalance.apply)
    val bbm = new RRFlow("bbm", BalanceByMap.apply)
    //oneway flows
    val slbOW = new OWFlow("slbIn", "gather", SingleLineBalance.apply)
    val noop = new OWFlow("noop", "gather", NoOp.apply) 


  }

  "seda" in {
    val cnt = 1

    for (i <- 1 to cnt)
      template.requestBody("seda:acct", Num("124-555-1234")) must beEqualTo(Acct("alpha"))

    success
  }

  "Noop" in {
  
    val cnt = 1
    Gather.prep(cnt)
    for (i <- 1 to cnt)
      template.sendBody("seda:noop", Num("124-555-1234"))

    Gather.await
    Gather.values.size must beEqualTo(cnt)
  }

  "check slb one way using camel many" in {
  
    val cnt = 2
    Gather.prep(cnt)
    for (i <- 1 to cnt)
      template.sendBody("seda:slbIn", Num("124-555-1234"))

    Gather.await
    Gather.values.size must beEqualTo(cnt)

  }
  "check slb request using camel" in {
   
    val fs = for (i <- 0 to 1) yield (template.requestBody("seda:slb", Num("124-555-1234"))).asInstanceOf[Future[Bal]]

    fs.map(f => f.get must beEqualTo(Bal(124.5F)))
  }

  "check bbm request using camel" in {
  
    val fs = for (i <- 0 to 1) yield (template.requestBody("seda:bbm", Id(123))).asInstanceOf[Future[Bal]]

    fs.map(f => f.get must beEqualTo(Bal(125.5F)))
  }

  "check responder request using camel" in {
  
    template.requestBody("seda:bal", Acct("alpha")) must beEqualTo(Bal(124.5F))
    template.requestBody("seda:bal", Acct("beta")) must beEqualTo(Bal(1F))
  } 

  "placeholder" in {
    success
  }

  /**
   * Shutdown
   */
  step {

   system shutdown
  }

}
