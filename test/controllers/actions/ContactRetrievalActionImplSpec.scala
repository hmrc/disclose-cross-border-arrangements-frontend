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

import base.SpecBase
import connectors.EnrolmentStoreConnector
import models.ContactDetails
import models.enrolments.{Enrolment, EnrolmentResponse, KnownFact}
import models.requests.{DataRequest, DataRequestWithContacts}
import org.mockito.Matchers.any
import org.mockito.Mockito._
import utils.EnrolmentConstants

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
class ContactRetrievalActionImplSpec extends SpecBase {



  class Harness(enrolmentConnector: EnrolmentStoreConnector) extends ContactRetrievalActionImpl(enrolmentConnector, mockAppConfig) {
    def callTransform[A](request: DataRequest[A]): Future[DataRequestWithContacts[A]] = transform(request)
  }

  "Contact retrieval action" - {
    "return request with contactinformation" in {
      when(mockAppConfig.sendEmailToggle).thenReturn(true)
      val mockEnrolmentConnector = mock[EnrolmentStoreConnector]
      when(mockEnrolmentConnector.getEnrolments(any())(any())).thenReturn(
        Future.successful(Some(
        EnrolmentResponse(EnrolmentConstants.dac6EnrolmentKey,Seq(
          Enrolment(
            Seq(KnownFact(EnrolmentConstants.dac6IdentifierKey, "id")),
            Seq(KnownFact(EnrolmentConstants.contactNameKey,"Test Testing"),
              KnownFact(EnrolmentConstants.contactEmailKey,"test@test.com")
            )
          )
        ))
      ))
      )
      val action = new Harness(mockEnrolmentConnector)

      val futureResult = action.callTransform(new DataRequest(fakeRequest,
        "id", "id",emptyUserAnswers))

      whenReady(futureResult) { result =>
        result.contacts mustBe Some(ContactDetails(Some("Test Testing"), Some("test@test.com"), None, None))
      }
      }

    }

}
