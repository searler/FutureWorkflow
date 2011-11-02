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

import _root_.akka.actor.Actor
import _root_.akka.dispatch.Future
import _root_.akka.camel._

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

import _root_.akka.camel.CamelContextManager
import _root_.akka.camel.CamelServiceManager._

case class CamelService[K, V](name: String) extends Lookup[K, V] {
  import akka.actor.Actor
  import akka.dispatch.Future

  val act = Actor.actorOf(new SActor).start

  def apply(arg: K): Future[V] = (act ? arg).map {
    _ match {
      case akka.camel.Message(a: V, _) => a
    }
  }

  private class SActor extends Actor with Producer {
    def endpointUri = "seda:" + name
  }

}

case class RRFlow[A, R](name: String, flow: A => Future[R]) {
  import akka.actor.Actor
  import akka.dispatch.Future

  val act = Actor.actorOf(new FActor).start

  private class FActor extends Actor with Consumer {
    def endpointUri = "seda:" + name

    def receive = {
      case akka.camel.Message(a: A, _) => self.reply(Future(a).flatMap(flow))
    }
  }

}

case class OWFlow[A, R](in: String, out: String, flow: A => Future[R]) {
  import akka.actor.Actor
  import akka.dispatch.Future

  val inActor = Actor.actorOf(new InActor).start
  val outActor = Actor.actorOf(new OutActor).start

  private class OutActor extends Actor with Producer with Oneway {
    val endpointUri = "seda:" + out
  }

  private class InActor extends Actor with Consumer   {
    val endpointUri = "seda:" + in

    def receive = {
      case akka.camel.Message(a: A, _) => (Future(a).flatMap(flow)).onComplete{outActor ! _}
    }
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

object CamelTest extends org.specs2.mutable.SpecificationWithJUnit {

  sequential

  implicit val acctLook: Lookup[Num, Acct] = CamelService("acct")
  implicit val balLook: Lookup[Acct, Bal] = CamelService("bal")
  implicit val numLook: Lookup[Id, List[Num]] = CamelService("num")

  step {
    CamelContextManager.init
    val context = CamelContextManager.mandatoryContext

    import org.apache.camel.scala.dsl.builder.RouteBuilder;
    //wire end points for services to a single bean
    context.addRoutes(new RouteBuilder { "seda:num".bean(Responder(ValueMaps.numMap)) })
    context.addRoutes(new RouteBuilder { "seda:acct".bean(Responder(ValueMaps.acctMap)) })
    context.addRoutes(new RouteBuilder { "seda:bal".bean(Responder(ValueMaps.balMap)) })
    context.addRoutes(new RouteBuilder { "seda:pp".bean(Responder(ValueMaps.prepaidMap)) })
    //wire end point for result of f-a-f call 
    context.addRoutes(new RouteBuilder { "seda:gather".bean(Gather) })
    // R-R flows
    val slb = new RRFlow("slb", SingleLineBalance.apply)
    val bbm = new RRFlow("bbm", BalanceByMap.apply)
    //oneway flows
    val slbOW = new OWFlow("slbIn", "gather", SingleLineBalance.apply)

    startCamelService

    success
  }

  "check slb one way using camel many" in {
    val producer = CamelContextManager.mandatoryContext.createProducerTemplate
    val cnt = 16600
    Gather.prep(cnt)
    for (i <- 1 to cnt)
      producer.sendBody("seda:slbIn", Num("124-555-1234"))

    val consumer = CamelContextManager.mandatoryContext.createConsumerTemplate
    Gather.await
    Gather.values.size must beEqualTo(cnt)

  }

  "check slb request using camel" in {
    val template = CamelContextManager.mandatoryContext.createProducerTemplate
    val fs = for (i <- 0 to 14) yield (template.requestBody("seda:slb", Num("124-555-1234"))).asInstanceOf[Future[Bal]]

    fs.map(f => f.get must beEqualTo(Bal(124.5F)))

  }

  "check bbm request using camel" in {
    val template = CamelContextManager.mandatoryContext.createProducerTemplate
    val fs = for (i <- 0 to 10) yield (template.requestBody("seda:bbm", Id(123))).asInstanceOf[Future[Bal]]

    fs.map(f => f.get must beEqualTo(Bal(125.5F)))

  }

  "check responder request using camel" in {
    val template = CamelContextManager.mandatoryContext.createProducerTemplate
    template.requestBody("seda:bal", Acct("alpha")) must beEqualTo(Bal(124.5F))
    template.requestBody("seda:bal", Acct("beta")) must beEqualTo(Bal(1F))
  }

  "check responder request using lookup" in {
    balLook(Acct("alpha")).get must beEqualTo(Bal(124.5F))
    val future = balLook(Acct("alpha"))
    future.await.result must beEqualTo(Some(Bal(124.5F)))
  }

  "slb" in {
    SingleLineBalance.apply(Num("124-555-1234")).get must beEqualTo(Bal(124.5F))
  }

  "bbm" in {
    BalanceByMap.apply(Id(123)).get must beEqualTo(Bal(125.5F))
  }

  /**
   * Shutdown
   */
  step {
    stopCamelService
    success
  }

}
