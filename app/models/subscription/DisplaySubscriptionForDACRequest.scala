/*
 * Copyright 2022 HM Revenue & Customs
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

package models.subscription

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import play.api.libs.json.{Json, OFormat}

case class RequestParameter(paramName: String, paramValue: String)

object RequestParameter {
  implicit val format: OFormat[RequestParameter] = Json.format[RequestParameter]
}

case class RequestCommon(regime: String,
                         conversationID: Option[String],
                         receiptDate: String,
                         acknowledgementReference: String,
                         originatingSystem: String,
                         requestParameters: Option[Seq[RequestParameter]]
)

object RequestCommon {
  implicit val format: OFormat[RequestCommon] = Json.format[RequestCommon]

  def createRequestCommon: RequestCommon = {
    //Format: ISO 8601 YYYY-MM-DDTHH:mm:ssZ e.g. 2020-09-23T16:12:11Z
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

    //Generate a 32 chars UUID without hyphens
    val acknowledgementReference = UUID.randomUUID().toString.replace("-", "")
    val conversationID           = UUID.randomUUID().toString

    RequestCommon(
      regime = "DAC",
      conversationID = Some(conversationID),
      receiptDate = ZonedDateTime.now().format(formatter),
      acknowledgementReference = acknowledgementReference,
      originatingSystem = "MDTP",
      requestParameters = None
    )
  }
}

case class RequestDetail(IDType: String, IDNumber: String)

object RequestDetail {
  implicit val format: OFormat[RequestDetail] = Json.format[RequestDetail]
}

case class DisplaySubscriptionDetails(requestCommon: RequestCommon, requestDetail: RequestDetail)

object DisplaySubscriptionDetails {
  implicit val format: OFormat[DisplaySubscriptionDetails] = Json.format[DisplaySubscriptionDetails]

  def createRequest(enrolmentID: String): DisplaySubscriptionDetails =
    DisplaySubscriptionDetails(requestCommon = RequestCommon.createRequestCommon, requestDetail = createRequestDetail(enrolmentID))

  private def createRequestDetail(enrolmentID: String): RequestDetail =
    RequestDetail(IDType = "DAC", IDNumber = enrolmentID)
}

case class DisplaySubscriptionForDACRequest(displaySubscriptionForDACRequest: DisplaySubscriptionDetails)

object DisplaySubscriptionForDACRequest {
  implicit val format: OFormat[DisplaySubscriptionForDACRequest] = Json.format[DisplaySubscriptionForDACRequest]
}
