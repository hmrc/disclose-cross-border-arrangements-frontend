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

package controllers.actions

import config.FrontendAppConfig
import javax.inject.Inject
import models.requests.{DataRequest, DataRequestWithContacts}
import play.api.mvc.ActionTransformer
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}
import connectors.EnrolmentStoreConnector
import models.ContactDetails
import uk.gov.hmrc.http.HeaderCarrier

class ContactRetrievalActionImpl @Inject()(enrolmentStoreConnecter: EnrolmentStoreConnector, frontendAppConfig: FrontendAppConfig)
                                          (implicit val executionContext: ExecutionContext) extends ContactRetrievalAction {

  override protected def transform[A](request: DataRequest[A]): Future[DataRequestWithContacts[A]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    if (!frontendAppConfig.sendEmailToggle) {
      Future.successful(DataRequestWithContacts(request.request, request.internalId, request.enrolmentID, request.userAnswers, None))
    }
    else {
      enrolmentStoreConnecter.getEnrolments(request.enrolmentID)(hc) map {
        enrolmentsResponse =>
          val contacts: Option[ContactDetails] = for {
            enres <- enrolmentsResponse
            enr <- enres.getEnrolment(request.enrolmentID)
          } yield enr.getContactDetails
          DataRequestWithContacts(request.request, request.internalId, request.enrolmentID, request.userAnswers, contacts)
      }
    }
  }

}

trait ContactRetrievalAction extends ActionTransformer[DataRequest, DataRequestWithContacts]
