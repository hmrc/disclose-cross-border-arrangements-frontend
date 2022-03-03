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

package connectors

import config.FrontendAppConfig
import models.EmailRequest
import play.api.Logging
import uk.gov.hmrc.http.HttpReads.Implicits._
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailConnector @Inject() (val config: FrontendAppConfig, http: HttpClient)(implicit ex: ExecutionContext) extends Logging {

  def sendEmail(emailRequest: EmailRequest)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    http.POST[EmailRequest, HttpResponse](s"${config.sendEmailUrl}/hmrc/email", emailRequest) recoverWith {
      case e: Exception =>
        logger.warn(s"${config.emailFailureAlertMessage}")
        logger.warn(s"${e.getMessage}")
        Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, config.emailFailureAlertMessage))
    }

}
