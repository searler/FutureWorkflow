/* Copyright (c) 2011 Richard Searle
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
 * Tests, based on a telecom problem domain
 *
 * Asynchronous services provide the following lookups
 * id -> Phone numbers
 * Phone number -> account number
 * Account number -> balance (either credit or prepaid)
 *
 * Flows then wire these lookups together to represent
 * business logic
 *
 *
 * @author Richard Searle
 */
package cognitiveentity.workflow
import akka.dispatch.Future
import akka.dispatch.Futures

trait SpecialBalLookup extends Function1[Acct, Future[Bal]]
trait BalLookup extends Function1[Acct, Future[Bal]]

/**
 * A no-op flow that simply returns the Num, expensively
 */
object NoOp {
  def apply(pn: Num) = Future(pn)
}

/**
 * A no-op flow that simply returns the  Num
 */
object NoOpOptimized {
  def apply(pn: Num) = Ret(pn)
}

object Balance {
  def apply(acct: Acct)(implicit balLook: Lookup[Acct, Bal]): Future[Bal] = balLook(acct)
}

object Discount {
  def apply(acct: Acct)(implicit balLook: Lookup[Acct, Bal], specialLook: Lookup[Acct, Boolean]): Future[Bal] = {
    val balance = balLook(acct)
    val special = specialLook(acct)
    for {
      val bal <- balance
      val spec <- special
    } yield if (spec) bal * 0.9F else bal
  }
}

object DiscountByPhone {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal], specialLook: Lookup[Acct, Boolean]): Future[Bal] =
    acctLook(pn) flatMap { Discount(_) }
}

object DiscountById {
  def apply(id: Id)(implicit numLook: Lookup[Id, List[Num]], acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal], specialLook: Lookup[Acct, Boolean]): Future[Bal] =
    numLook(id) flatMap { Future.traverse(_)(DiscountByPhone(_)) } map { _.reduce(_ + _) }
}
/**
 *
 */
object SpecialLineBalance {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal], special: SpecialBalLookup): Future[Bal] = {
    acctLook(pn) flatMap { special(_) }
  }
}

object OtherLineBalance {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: BalLookup): Future[Bal] = {
    acctLook(pn) flatMap { balLook(_) }
  }
}

object SingleLineBalance {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal]): Future[Bal] = {
    acctLook(pn) flatMap { balLook(_) }
  }
}

object SingleLineBalanceNoArgs {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal]): Future[Bal] = {
    acctLook(pn) flatMap { balLook }
  }
}
object SingleLineBalanceDoubled {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal]): Future[Bal] = {
    acctLook(pn) flatMap { balLook } map { _ * 2 }
  }
}

object SplitLineBalanceFiltered {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal], special: SpecialBalLookup): Future[Any] = {
    val std = acctLook(pn) flatMap { balLook(_) }
    val spec = acctLook(pn) flatMap { special(_) }
    for {
      b1 <- std if b1 == Bal(124.5F)
      b2 <- spec
    } yield b2 + b1.asInstanceOf[Bal] //need cast due to Any type on filter. Fixed in 2.0
  }
}

object SplitLineBalanceFirst {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal], special: SpecialBalLookup): Future[Bal] = {
    val std = acctLook(pn) flatMap { balLook(_) }
    val spec = acctLook(pn) flatMap { special(_) }
    Futures.firstCompletedOf(List(std, spec))
  }
}

object SplitLineBalanceSerial {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal], special: SpecialBalLookup): Future[Bal] = {
    acctLook(pn) flatMap { acct =>
      balLook(acct) flatMap { b =>
        special(acct) map {
          _ + b
        }
      }
    }
  }
}

object SplitLineBalanceCommon {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal], special: SpecialBalLookup): Future[Bal] = {
    val acct = acctLook(pn)
    val std = acct flatMap { balLook(_) }
    val spec = acct flatMap { special(_) }
    for {
      val b1 <- std
      val b2 <- spec
    } yield b1 + b2
  }
}

object SplitLineBalanceCommonMap {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal], special: SpecialBalLookup): Future[Bal] = {
    val acct = acctLook(pn)
    val std = acct flatMap { balLook(_) }
    val spec = acct flatMap { special(_) }
    std flatMap { v => spec map { u => v + u } }
  }
}

object SplitLineBalanceList {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal], special: SpecialBalLookup): Future[List[Bal]] = {
    val acct = acctLook(pn)
    val std = acct flatMap { balLook(_) }
    val spec = acct flatMap { special(_) }
    Future.sequence(List(std, spec))
  }
}

object SplitLineBalanceTuple {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal], special: SpecialBalLookup): Future[(Num, Acct, Bal, Bal)] = {
    val acct = acctLook(pn)
    val std = acct flatMap { balLook(_) }
    val spec = acct flatMap { special(_) }
    for {
      a <- acct
      b1 <- std
      b2 <- spec
    } yield (pn, a, b1, b2)
  }
}

object SplitLineBalance {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal], special: SpecialBalLookup): Future[Bal] = {
    val std = acctLook(pn) flatMap { balLook(_) }
    val spec = acctLook(pn) flatMap { special(_) }
    for {
      val b1 <- std
      val b2 <- spec
    } yield b1 + b2
  }
}

object SplitLineBalanceTuple2 {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal], special: SpecialBalLookup): Future[(Bal, Bal)] = {
    val std = acctLook(pn) flatMap { balLook(_) }
    val spec = acctLook(pn) flatMap { special(_) }
    Flow.tuple(std, spec)
  }
}

object SplitLineBalanceTuple2Id {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal], special: SpecialBalLookup): Future[(Num, Bal, Bal)] = {
    val std = acctLook(pn) flatMap { balLook(_) }
    val spec = acctLook(pn) flatMap { special(_) }
    Flow.tuple(pn, std, spec)
  }
}

object RecoverNumBalance {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal]): Future[Bal] = {
    acctLook(pn) flatMap { balLook(_) } recover { case _ => Bal(0F) } map { _ + Bal(66F) }
  }
}

object IfNumBalance {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal]): Future[Bal] = {
    if (pn == null)
      Ret(Bal(0F))
    else
      acctLook(pn) flatMap { a => if (a == null) Ret(Bal(55F)) else balLook(a) }
  }
}

object Phones {
  def apply(id: Id)(implicit numLook: Lookup[Id, List[Num]]): Future[List[Num]] = numLook(id)
}

object AccountsByFor {
  def apply(id: Id)(implicit numLook: Lookup[Id, List[Num]], acctLook: Lookup[Num, Acct]): Future[List[Acct]] = {
    val nums = numLook(id)
    for {
      ns <- nums
      a <- Future.traverse(ns) { acctLook(_) }
    } yield a
  }
}

object AccountsByTraverse {
  def apply(id: Id)(implicit numLook: Lookup[Id, List[Num]], acctLook: Lookup[Num, Acct]): Future[List[Acct]] = {
    numLook(id) flatMap { Future.traverse(_)(acctLook) }
  }
}

object BalancesByMap {
  def apply(id: Id)(implicit numLook: Lookup[Id, List[Num]], acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal]): Future[List[Bal]] =
    numLook(id) flatMap { Future.traverse(_) { acctLook(_) flatMap { balLook } } }
}

object BalancesByFor {
  def apply(id: Id)(implicit numLook: Lookup[Id, List[Num]], acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal]): Future[List[Bal]] =
    {
      val nums: Future[List[Num]] = numLook(id)
      for {
        ns <- nums
        a <- Future.traverse(ns) { acctLook(_) flatMap { balLook(_) } }
      } yield a
    }
}

object BalanceByMap {
  def apply(id: Id)(implicit numLook: Lookup[Id, List[Num]], acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal]): Future[Bal] =
    BalancesByMap(id) map { _.reduce(_ + _) }
}

object BalanceParallel {
  def apply(id: Id)(implicit numLook: Lookup[Id, List[Num]], acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal]): Future[Bal] =
    numLook(id) flatMap { ns => Futures.reduce(ns map { acctLook(_) flatMap { balLook } })(_ + _) }
}

