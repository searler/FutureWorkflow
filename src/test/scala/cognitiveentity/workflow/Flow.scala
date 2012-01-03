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

/**
 * The Lookup trait represents the generic form of an async call to
 * an external service.
 */
trait Lookup[A, R] extends Function1[A, Future[R]] {
  def apply(arg: A): Future[R]
}

object Flow {
  def tuple[A, B](af: Future[A], bf: Future[B]) = 
    for {
       a <- af
       b <- bf
    } yield (a, b)
    
     def tuple[I,A, B](id:I,af: Future[A], bf: Future[B]) = 
    for {
       a <- af
       b <- bf
    } yield (id,a, b)

}

    

