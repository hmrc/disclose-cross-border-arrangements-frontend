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
import models.ContactDetails
import models.enrolments.{EnrolmentRequest, EnrolmentResponse, KnownFact}
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import utils.EnrolmentConstants

import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStoreConnector @Inject()(val config: FrontendAppConfig, http: HttpClient)(implicit ex: ExecutionContext) {

  def getEnrolments(enrolmentID: String)(implicit hc: HeaderCarrier): Future[Option[EnrolmentResponse]] = {
    val getEnrolmentsRequest = EnrolmentRequest(EnrolmentConstants.dac6EnrolmentKey, Seq(KnownFact(EnrolmentConstants.dac6IdentifierKey, enrolmentID)))

    val url = config.enrolmentStoreProxyBaseUrl + config.getEnrolmentsUrl
    http.POST[JsValue, HttpResponse](url, Json.toJson(getEnrolmentsRequest)) map { response =>

      response.status match {
        case OK => response.json.validate[EnrolmentResponse] match {
          case JsSuccess(enrolmentResponse, _) =>
            Some(enrolmentResponse)
          case JsError(_) =>
            None
        }
        case _ =>
         None
      }
    }
  }
}

