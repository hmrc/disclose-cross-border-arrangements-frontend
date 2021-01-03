/*
 * Copyright 2021 HM Revenue & Customs
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

package models

import play.api.libs.json.{Reads, Writes, __}

case class GeneratedIDs(arrangementID: Option[String], disclosureID: Option[String])

object GeneratedIDs {
  import play.api.libs.functional.syntax._

  implicit val reads: Reads[GeneratedIDs] = {
    ((__ \ "arrangementID").readNullable[String] and
      (__ \ "disclosureID").readNullable[String])(GeneratedIDs.apply _)
  }

  implicit lazy val writes: Writes[GeneratedIDs] =
    (
      (__ \ "arrangementID").writeNullable[String] and
        (__ \ "disclosureID").writeNullable[String]
      )(unlift(GeneratedIDs.unapply))
}
