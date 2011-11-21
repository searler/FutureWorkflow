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

object ValueMaps {

  /**
   * Standard values for test purposes
   *
   * Id->Num
   * Num->Acct
   * Acct->Bal
   *
   */
  val numMap = Map(Id(123) -> List(Num("124-555-1234"), Num("333-555-1234")))
  val acctMap = Map(Num("124-555-1234") -> Acct("alpha"),
    Num("333-555-1234") -> Acct("beta"),
    Num("999-555-1234") -> null,
    Num("111-555-1234") -> Acct("zulu"))
  val balMap = Map(Acct("alpha") -> Bal(124.5F),
    Acct("beta") -> Bal(1.0F),
    Acct("gamma") -> Bal(11.0F))

}