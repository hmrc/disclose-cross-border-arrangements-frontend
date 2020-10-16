/*
 * Copyright 2020 HM Revenue & Customs
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

package helpers

import play.api.libs.json.{JsObject, JsString, Json}

object JsonFixtures {

  def jsonPayloadForDisplaySubscription(firstName: JsString,
                                        lastName:JsString,
                                        organisationName: JsString,
                                        primaryEmail: JsString,
                                        secondaryEmail: JsString,
                                        phone: JsString): String = {
    s"""
      |{
      |  "displaySubscriptionForDACResponse": {
      |    "responseCommon": {
      |      "status": "OK",
      |      "processingDate": "2020-08-09T11:23:45Z"
      |    },
      |    "responseDetail": {
      |      "subscriptionID": "XE0001234567890",
      |      "tradingName": "Trading Name",
      |      "isGBUser": true,
      |      "primaryContact": [
      |        {
      |          "email": $primaryEmail,
      |          "phone": $phone,
      |          "mobile": $phone,
      |          "individual": {
      |            "lastName": $lastName,
      |            "firstName": $firstName
      |          }
      |        }
      |      ],
      |      "secondaryContact": [
      |        {
      |          "email": $secondaryEmail,
      |          "organisation": {
      |            "organisationName": $organisationName
      |          }
      |        }
      |      ]
      |    }
      |  }
      |}""".stripMargin
  }

  val jsonPayloadForDisplaySubscriptionRequest: String =
    s"""
       |{
       |  "displaySubscriptionForDACRequest": {
       |    "requestCommon": {
       |      "regime": "DAC",
       |      "conversationID": "bffaa447-b500-49e0-9c73-bfd81db9242f",
       |      "receiptDate": "2020-09-23T16:12:11Z",
       |      "acknowledgementReference": "Abc12345",
       |      "originatingSystem": "MDTP"
       |    },
       |    "requestDetail": {
       |      "IDType": "DAC",
       |      "IDNumber": "1234567890"
       |    }
       |  }
       |}""".stripMargin

  val jsonForDisplaySubscriptionRequest: JsObject = Json.obj(
    "displaySubscriptionForDACRequest" -> Json.obj(
      "requestCommon" -> Json.obj(
        "regime" -> "DAC",
        "conversationID" -> "bffaa447-b500-49e0-9c73-bfd81db9242f",
        "receiptDate" -> "2020-09-23T16:12:11Z",
        "acknowledgementReference" -> "Abc12345",
        "originatingSystem" -> "MDTP"
      ),
      "requestDetail" -> Json.obj(
        "IDType" -> "DAC",
        "IDNumber" -> "1234567890"
      )
    )
  )

}
