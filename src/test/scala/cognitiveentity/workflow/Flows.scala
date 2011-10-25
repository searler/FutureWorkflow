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
import akka.dispatch.AlreadyCompletedFuture

object SingleLineBalance {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal]): Future[Bal] = {
    acctLook(pn) flatMap { balLook(_) }
  }
}

object RecoverNumBalance {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal]): Future[Bal] = {
    acctLook(pn) flatMap { balLook(_) } recover { case _ => Bal(0F) }
  }
}

object IfNumBalance {
  def apply(pn: Num)(implicit acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal]): Future[Bal] = {
    if (pn == null)
      new AlreadyCompletedFuture(new Right(Bal(0F)))
    else
      acctLook(pn) flatMap { a => if (a == null) new AlreadyCompletedFuture(new Right(Bal(0F))) else balLook(a) }
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

object BalancesByMap {
  def apply(id: Id)(implicit numLook: Lookup[Id, List[Num]], acctLook: Lookup[Num, Acct], balLook: Lookup[Acct, Bal]): Future[List[Bal]] =
    numLook(id) flatMap { Future.traverse(_) { acctLook(_) flatMap { balLook(_) } } }
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
    BalancesByFor(id) map { _.reduce(_ + _) }
 
}

