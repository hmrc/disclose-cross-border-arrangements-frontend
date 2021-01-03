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

package helpers

import play.api.libs.json.{JsObject, JsString, Json}

object JsonFixtures {

  def displaySubscriptionPayload(subscriptionID: JsString,
                                 firstName: JsString,
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
      |      "subscriptionID": $subscriptionID,
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

  def displaySubscriptionPayloadNoSecondary(subscriptionID: JsString,
                                            firstName:JsString,
                                            lastName:JsString,
                                            primaryEmail: JsString,
                                            phone: JsString): String = {
    s"""
       |{
       |  "displaySubscriptionForDACResponse": {
       |    "responseCommon": {
       |      "status": "OK",
       |      "processingDate": "2020-08-09T11:23:45Z"
       |    },
       |    "responseDetail": {
       |      "subscriptionID": $subscriptionID,
       |      "tradingName": "Trading Name",
       |      "isGBUser": true,
       |      "primaryContact": [
       |        {
       |          "email": $primaryEmail,
       |          "phone": $phone,
       |          "individual": {
       |            "lastName": $lastName,
       |            "firstName": $firstName
       |          }
       |        }
       |      ]
       |    }
       |  }
       |}""".stripMargin
  }

  def jsonForDisplaySubscription(safeID: String,
                                 firstName: String,
                                 lastName: String,
                                 organisationName: String,
                                 primaryEmail: String,
                                 secondaryEmail: String,
                                 phone: String): JsObject = {
    Json.obj(
      "displaySubscriptionForDACResponse" -> Json.obj(
        "responseCommon" -> Json.obj(
          "status" -> "OK",
          "processingDate" -> "2020-08-09T11:23:45Z"
        ),
        "responseDetail" -> Json.obj(
          "subscriptionID" -> safeID,
          "tradingName" -> "Trading Name",
          "isGBUser" -> true,
          "primaryContact" -> Json.obj(
            "email" -> primaryEmail,
            "phone" -> phone,
            "mobile" -> phone,
            "individual" -> Json.obj(
              "lastName" -> lastName,
              "firstName" -> firstName
            )
          ),
          "secondaryContact" -> Json.obj(
            "email" -> secondaryEmail,
            "organisation" -> Json.obj(
              "organisationName" -> organisationName
            )
          )
        )
      )
    )
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

  def updateDetailsPayloadNoSecondContact(firstName: JsString,
                                          lastName: JsString,
                                          primaryEmail: JsString): String = {
    s"""
       |{
       |  "updateSubscriptionForDACRequest": {
       |    "requestCommon": {
       |      "regime": "DAC",
       |      "receiptDate": "2020-09-23T16:12:11Z",
       |      "acknowledgementReference": "AB123c",
       |      "originatingSystem": "MDTP",
       |      "requestParameters": [{
       |        "paramName":"Name",
       |        "paramValue":"Value"
       |      }]
       |    },
       |    "requestDetail": {
       |      "IDType": "SAFE",
       |      "IDNumber": "IDNumber",
       |      "isGBUser": true,
       |      "primaryContact": [{
       |        "individual": {
       |          "firstName": $firstName,
       |          "lastName": $lastName
       |        },
       |        "email": $primaryEmail
       |      }]
       |    }
       |  }
       |}
       |""".stripMargin
  }

  def updateDetailsPayload(firstName: JsString,
                           lastName: JsString,
                           email: JsString,
                           organisationName: JsString,
                           secondaryEmail: JsString,
                           phone: JsString): String = {
    s"""
       |{
       |  "updateSubscriptionForDACRequest": {
       |    "requestCommon": {
       |      "regime": "DAC",
       |      "receiptDate": "2020-09-23T16:12:11Z",
       |      "acknowledgementReference": "AB123c",
       |      "originatingSystem": "MDTP",
       |      "requestParameters": [{
       |        "paramName":"Name",
       |        "paramValue":"Value"
       |      }]
       |    },
       |    "requestDetail": {
       |      "IDType": "SAFE",
       |      "IDNumber": "IDNumber",
       |      "isGBUser": false,
       |      "primaryContact": [{
       |        "individual": {
       |          "firstName": $firstName,
       |          "lastName": $lastName
       |        },
       |        "email": $email
       |      }],
       |      "secondaryContact": [{
       |        "organisation": {
       |          "organisationName": $organisationName
       |        },
       |        "email": $secondaryEmail,
       |        "phone": $phone
       |      }]
       |    }
       |  }
       |}
       |""".stripMargin
  }

  def updateDetailsJsonNoSecondContact(firstName: String,
                                       lastName: String,
                                       primaryEmail: String): JsObject = {
    Json.obj(
      "updateSubscriptionForDACRequest" -> Json.obj(
        "requestCommon" -> Json.obj(
          "regime" -> "DAC",
          "receiptDate" -> "2020-09-23T16:12:11Z",
          "acknowledgementReference" -> "AB123c",
          "originatingSystem" -> "MDTP",
          "requestParameters" -> Json.arr(
            Json.obj(
              "paramName" -> "Name",
              "paramValue" -> "Value"
            )
          )
        ),
        "requestDetail" -> Json.obj(
          "IDType" -> "SAFE",
          "IDNumber" -> "IDNumber",
          "isGBUser" -> true,
          "primaryContact" -> Json.arr(Json.obj(
            "individual" -> Json.obj(
              "firstName" -> firstName,
              "lastName" -> lastName
            ),
            "email" -> primaryEmail
          ))

        )
      )
    )
  }

  def updateDetailsJson(firstName: String,
                        lastName: String,
                        email: String,
                        organisationName: String,
                        secondaryEmail: String,
                        phone: String): JsObject = {
    Json.obj(
      "updateSubscriptionForDACRequest" -> Json.obj(
        "requestCommon" -> Json.obj(
          "regime" -> "DAC",
          "receiptDate" -> "2020-09-23T16:12:11Z",
          "acknowledgementReference" -> "AB123c",
          "originatingSystem" -> "MDTP",
          "requestParameters" -> Json.arr(
            Json.obj(
              "paramName" -> "Name",
              "paramValue" -> "Value"
            )
          )
        ),
        "requestDetail" -> Json.obj(
          "IDType" -> "SAFE",
          "IDNumber" -> "IDNumber",
          "isGBUser" -> false,
          "primaryContact" -> Json.arr(Json.obj(
            "individual" -> Json.obj(
              "firstName" -> firstName,
              "lastName" -> lastName
            ),
            "email" -> email
          )),
          "secondaryContact" -> Json.arr(Json.obj(
            "organisation" -> Json.obj(
              "organisationName" -> organisationName
            ),
            "email" -> secondaryEmail,
            "phone" -> phone
          ))

        )
      )
    )
  }

  def updateSubscriptionResponsePayload(safeID: JsString): String = {
    s"""
      |{
      |  "updateSubscriptionForDACResponse": {
      |    "responseCommon": {
      |      "status": "OK",
      |      "processingDate": "2020-09-23T16:12:11Z",
      |      "returnParameters": [{
      |        "paramName":"Name",
      |        "paramValue":"Value"
      |      }]
      |    },
      |    "responseDetail": {
      |      "subscriptionID": $safeID
      |    }
      |  }
      |}
      |""".stripMargin
  }

  def updateSubscriptionResponseJson(safeID: String): JsObject = Json.obj(
    "updateSubscriptionForDACResponse" -> Json.obj(
      "responseCommon" -> Json.obj(
        "status" -> "OK",
        "processingDate" -> "2020-09-23T16:12:11Z",
        "returnParameters" -> Json.arr(
          Json.obj(
            "paramName" -> "Name",
            "paramValue" -> "Value"
          )
        )
      ),
      "responseDetail" -> Json.obj(
        "subscriptionID" -> safeID
      )
    )
  )

}
