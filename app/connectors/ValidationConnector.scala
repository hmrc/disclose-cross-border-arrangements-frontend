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
import controllers.exceptions.UpscanTimeoutException
import models.{Dac6MetaData, UploadSubmissionValidationFailure, UploadSubmissionValidationResult, UploadSubmissionValidationSuccess, ValidationErrors}
import org.slf4j.LoggerFactory
import play.api.http.Status.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidationConnector @Inject() (http: HttpClient, config: FrontendAppConfig) {

  private val logger = LoggerFactory.getLogger(getClass)

  val url = s"${config.crossBorderArrangementsUrl}/disclose-cross-border-arrangements/validate-upload-submission"

  def sendForValidation(upScanUrl: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Either[ValidationErrors, Dac6MetaData]]] =
    http
      .POSTString[HttpResponse](url, upScanUrl)
      .map {
        response =>
          response.status match {
            case OK =>
              response.json.as[UploadSubmissionValidationResult] match {
                case x: UploadSubmissionValidationSuccess =>
                  Some(Right(x.dac6MetaData))
                case x: UploadSubmissionValidationFailure =>
                  Some(Left(x.validationErrors))
              }
          }
      }
      .recover {
        case e: Throwable if e.getMessage contains "Invalid XML" =>
          logger.warn(s"XML parsing failed. The XML parser in disclose-cross-border-arrangements backend has thrown the exception: $e")
          None
        case e: Throwable =>
          logger.warn(s"Remote service timed out. The XML parser in disclose-cross-border-arrangements backend has thrown the exception: $e")
          throw new UpscanTimeoutException
      }
}
