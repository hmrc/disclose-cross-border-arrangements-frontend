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

import base.SpecBase
import generators.Generators
import models.subscription._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsObject, Json}

import java.time.LocalDateTime

class CreateSubscriptionForDACRequestSpec extends SpecBase with Generators with ScalaCheckPropertyChecks {

  val requestParameter = Seq(RequestParameter("Name", "Value"))

  val requestCommon: RequestCommonForUpdate = RequestCommonForUpdate(
    regime = "DAC",
    receiptDate = "2020-09-23T16:12:11Z",
    acknowledgementReference = "AB123c",
    originatingSystem = "MDTP",
    requestParameters = Some(requestParameter)
  )

  val primaryContactForInd: PrimaryContact = PrimaryContact(
    Seq(ContactInformationForIndividual(IndividualDetails("FirstName", "LastName", None), "email@email.com", None, None))
  )

  val lastUpdated: LocalDateTime = LocalDateTime.parse("2021-06-20T12:12:12")

  val requestDetailForUpdate: RequestDetailForUpdate = RequestDetailForUpdate(
    IDType = "DAC",
    IDNumber = "IDNumber",
    tradingName = None,
    isGBUser = false,
    primaryContact = primaryContactForInd,
    secondaryContact = None
  )

  val updateRequest: CreateSubscriptionForDACRequest = CreateSubscriptionForDACRequest(
    UpdateSubscriptionDetails(
      requestCommon = requestCommon,
      requestDetail = requestDetailForUpdate
    ),
    "XADAC0000111111",
    lastUpdated
  )

  "CreateSubscriptionForDACRequest" - {

    "must deserialise CreateSubscriptionForDACRequest" in {
      val jsonPayload =
        s"""{
          |  "createSubscriptionForDACRequest": {
          |    "requestCommon": {
          |      "regime": "DAC",
          |      "receiptDate": "2020-09-23T16:12:11Z",
          |      "acknowledgementReference": "AB123c",
          |      "originatingSystem": "MDTP",
          |      "requestParameters": [
          |        {
          |          "paramName": "Name",
          |          "paramValue": "Value"
          |        }
          |      ]
          |    },
          |    "requestDetail": {
          |      "IDType": "DAC",
          |      "IDNumber": "IDNumber",
          |      "isGBUser": false,
          |      "primaryContact": [
          |        {
          |          "individual": {
          |            "firstName": "FirstName",
          |            "lastName": "LastName"
          |          },
          |          "email": "email@email.com"
          |        }
          |      ]
          |    }
          |  },
          |  "subscriptionID": "XADAC0000111111",
          |  "lastUpdated": "$lastUpdated"
          |}""".stripMargin

      Json.parse(jsonPayload).validate[CreateSubscriptionForDACRequest].get mustBe updateRequest
    }

    "must serialise CreateSubscriptionForDACRequest" in {
      val json: JsObject =
        Json.obj(
          "createSubscriptionForDACRequest" -> Json.obj(
            "requestCommon" -> Json.obj(
              "regime"                   -> "DAC",
              "receiptDate"              -> "2020-09-23T16:12:11Z",
              "acknowledgementReference" -> "AB123c",
              "originatingSystem"        -> "MDTP",
              "requestParameters" -> Json.arr(
                Json.obj(
                  "paramName"  -> "Name",
                  "paramValue" -> "Value"
                )
              )
            ),
            "requestDetail" -> Json.obj(
              "IDType"   -> "DAC",
              "IDNumber" -> "IDNumber",
              "isGBUser" -> false,
              "primaryContact" -> Json.arr(
                Json.obj(
                  "individual" -> Json.obj(
                    "firstName" -> "FirstName",
                    "lastName"  -> "LastName"
                  ),
                  "email" -> "email@email.com"
                )
              )
            )
          ),
          "subscriptionID" -> "XADAC0000111111",
          "lastUpdated"    -> s"$lastUpdated"
        )

      Json.toJson(updateRequest) mustBe json
    }
  }

}
