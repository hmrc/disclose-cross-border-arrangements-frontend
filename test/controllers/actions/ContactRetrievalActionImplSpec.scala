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
import connectors.SubscriptionConnector
import helpers.JsonFixtures.displaySubscriptionPayloadNoSecondary
import models.ContactDetails
import models.requests.{DataRequest, DataRequestWithContacts}
import models.subscription.DisplaySubscriptionForDACResponse
import org.mockito.Matchers.any
import org.mockito.Mockito._
import play.api.libs.json.{JsString, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
class ContactRetrievalActionImplSpec extends SpecBase {

  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]

  class Harness(subscriptionConnector: SubscriptionConnector) extends ContactRetrievalActionImpl(mockAppConfig, subscriptionConnector) {
    def callTransform[A](request: DataRequest[A]): Future[DataRequestWithContacts[A]] = transform(request)
  }

  "Contact retrieval action" - {
    "return request with contact information" in {
      val jsonPayload: String = displaySubscriptionPayloadNoSecondary(
        JsString("id"), JsString("FirstName"), JsString("LastName"), JsString("test@test.com"), JsString("0191 111 2222"))
      val displaySubscriptionDetails: DisplaySubscriptionForDACResponse = Json.parse(jsonPayload).as[DisplaySubscriptionForDACResponse]

      when(mockAppConfig.sendEmailToggle).thenReturn(true)
      when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
        .thenReturn(Future.successful(Some(displaySubscriptionDetails)))

      val action = new Harness(mockSubscriptionConnector)

      val futureResult = action.callTransform(new DataRequest(fakeRequest,
        "id", "id",emptyUserAnswers))

      whenReady(futureResult) { result =>
        result.contacts mustBe Some(ContactDetails(Some("FirstName LastName"), Some("test@test.com"), None, None))
      }
    }

    "return request without contact information" in {
      when(mockAppConfig.sendEmailToggle).thenReturn(false)

      val action = new Harness(mockSubscriptionConnector)

      val futureResult = action.callTransform(new DataRequest(fakeRequest,
        "id", "id",emptyUserAnswers))

      whenReady(futureResult) { result =>
        result.contacts mustBe None
      }
    }

  }

}
