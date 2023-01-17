/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}

case class EmailRequest(to: List[String], templateId: String, parameters: Map[String, String])

object EmailRequest {
  implicit val format: OFormat[EmailRequest] = Json.format[EmailRequest]

  def sendConfirmation(email: String,
                       importInstruction: String,
                       arrangementID: String,
                       disclosureID: String,
                       dateSubmitted: String,
                       messageRefID: String,
                       name: Option[String]
  ): EmailRequest = {

    val templateID = importInstruction match {
      case "DAC6NEW" => "dac6_new_disclosure_confirmation"
      case "DAC6ADD" => "dac6_additional_disclosure_confirmation"
      case "DAC6REP" => "dac6_replace_disclosure_confirmation"
      case "DAC6DEL" => "dac6_delete_disclosure_confirmation"
    }

    EmailRequest(
      List(email),
      templateID,
      name
        .map(
          n => "name" -> n
        )
        .toMap ++
        Map(
          "arrangementID" -> arrangementID,
          "disclosureID"  -> disclosureID,
          "dateSubmitted" -> dateSubmitted,
          "messageRefID"  -> messageRefID
        )
    )
  }

}
