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

package connectors

import config.FrontendAppConfig
import javax.inject.Inject
import models.UserAnswers
import models.subscription._
import org.slf4j.LoggerFactory
import play.api.http.Status.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionConnector @Inject()(val config: FrontendAppConfig, val http: HttpClient) {

  private val logger = LoggerFactory.getLogger(getClass)

  def displaySubscriptionDetails(enrolmentID: String)
                                (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DisplaySubscriptionForDACResponse] = {

    val submissionUrl = s"${config.crossBorderArrangementsUrl}/disclose-cross-border-arrangements/subscription/display-subscription"

    http.POST[DisplaySubscriptionForDACRequest, HttpResponse](
      submissionUrl,
      DisplaySubscriptionForDACRequest(DisplaySubscriptionDetails.createRequest(enrolmentID))
    ).map {
      response =>
        response.status match {
          case OK => response.json.as[DisplaySubscriptionForDACResponse]
          case errorStatus: Int =>
            logger.warn(s"Status $errorStatus has been thrown when display subscription was called")
            throw new Exception(s"Status $errorStatus has been thrown when display subscription was called")
        }
    }
  }

  def updateSubscription(subscriptionDetails: SubscriptionForDACResponse, userAnswers: UserAnswers)
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpdateSubscriptionForDACResponse] = {

    val submissionUrl = s"${config.crossBorderArrangementsUrl}/disclose-cross-border-arrangements/subscription/update-subscription"

    http.POST[UpdateSubscriptionForDACRequest, HttpResponse](
      submissionUrl,
      UpdateSubscriptionForDACRequest(UpdateSubscriptionDetails.updateSubscription(subscriptionDetails, userAnswers))
    ).map {
      response =>
        response.status match {
          case OK => response.json.as[UpdateSubscriptionForDACResponse]
          case errorStatus: Int =>
            logger.warn(s"Status $errorStatus has been thrown when update subscription was called")
            throw new Exception(s"Status $errorStatus has been thrown when update subscription was called")
        }
    }
  }

}
