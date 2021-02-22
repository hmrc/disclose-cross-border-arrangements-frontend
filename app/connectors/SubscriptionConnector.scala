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

package connectors

import config.FrontendAppConfig
import javax.inject.Inject
import models.UserAnswers
import models.subscription._
import org.slf4j.LoggerFactory
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionConnector @Inject()(val config: FrontendAppConfig, val http: HttpClient) {

  private val logger = LoggerFactory.getLogger(getClass)

  def displaySubscriptionDetails(enrolmentID: String)
                                (implicit hc: HeaderCarrier,
                                 ec: ExecutionContext): Future[DisplaySubscriptionDetailsAndStatus] = {

    val submissionUrl = s"${config.crossBorderArrangementsUrl}/disclose-cross-border-arrangements/subscription/retrieve-subscription"

    http.POST[DisplaySubscriptionForDACRequest, HttpResponse](
      submissionUrl,
      DisplaySubscriptionForDACRequest(DisplaySubscriptionDetails.createRequest(enrolmentID))
    ).map {
      response =>
        val errorMessage = "Create/Amend request is in progress"

        response.status match {
          case OK => response.json.validate[DisplaySubscriptionForDACResponse] match {
            case JsSuccess(response, _) => DisplaySubscriptionDetailsAndStatus(Some(response))
            case JsError(errors) =>
              logger.warn("Validation of display subscription payload failed", errors)
              DisplaySubscriptionDetailsAndStatus(None)
          }
          case errorStatus: Int => response.json.validate[DisplaySubscriptionErrorResponse] match {
            case JsSuccess(errorResponse, _) if errorResponse.errorDetail.errorMessage == errorMessage =>
              DisplaySubscriptionDetailsAndStatus(None, isLocked = true)
            case JsSuccess(errorResponse, _) =>
              logger.warn(s"Status $errorStatus has been thrown when display subscription was called")
              DisplaySubscriptionDetailsAndStatus(None)
            case JsError(errors) =>
              logger.warn("Validation of display subscription error response payload failed", errors)
              DisplaySubscriptionDetailsAndStatus(None)
          }
        }
    }
  }

  def updateSubscription(subscriptionDetails: SubscriptionForDACResponse, userAnswers: UserAnswers)
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UpdateSubscriptionForDACResponse]] = {

    val submissionUrl = s"${config.crossBorderArrangementsUrl}/disclose-cross-border-arrangements/subscription/update-subscription"

    http.POST[UpdateSubscriptionForDACRequest, HttpResponse](
      submissionUrl,
      UpdateSubscriptionForDACRequest(UpdateSubscriptionDetails.updateSubscription(subscriptionDetails, userAnswers))
    ).map {
      response =>
        response.status match {
          case OK => response.json.validate[UpdateSubscriptionForDACResponse] match {
            case JsSuccess(response, _) => Some(response)
            case JsError(errors) =>
              logger.warn("Validation of update subscription response failed", errors)
              None
          }
          case errorStatus: Int =>
            logger.warn(s"Status $errorStatus has been thrown when update subscription was called")
            None
        }
    }
  }

}
