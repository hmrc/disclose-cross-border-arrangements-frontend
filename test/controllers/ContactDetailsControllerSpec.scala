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

package controllers

import base.SpecBase
import connectors.SubscriptionConnector
import helpers.JsonFixtures.{displaySubscriptionPayload, displaySubscriptionPayloadNoSecondary, updateSubscriptionResponsePayload}
import models.subscription.{DisplaySubscriptionForDACResponse, UpdateSubscriptionForDACResponse}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html

import scala.concurrent.Future

class ContactDetailsControllerSpec extends SpecBase with MockitoSugar {

  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]

  "ContactDetails Controller" - {

    "must return OK and the correct view for a GET without a secondary contact" in {

      val jsonPayload = displaySubscriptionPayloadNoSecondary(
        JsString("FirstName"), JsString("LastName"), JsString("email@email.com"), JsString("07111222333"))
      val displaySubscriptionDetails = Json.parse(jsonPayload).as[DisplaySubscriptionForDACResponse]

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
        .thenReturn(Future.successful(displaySubscriptionDetails))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).overrides(
        bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
      ).build()

      val request = FakeRequest(GET, routes.ContactDetailsController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val json = jsonCaptor.getValue
      val contactDetails = (json \ "contactDetails").toString

      templateCaptor.getValue mustEqual "contactDetails.njk"
      contactDetails.contains("Contact name") mustBe true
      contactDetails.contains("Email address") mustBe true
      contactDetails.contains("Telephone") mustBe true
      contactDetails.contains("Secondary contact name") mustBe false
      contactDetails.contains("Secondary email address") mustBe false
      contactDetails.contains("Secondary telephone") mustBe false

      application.stop()
    }

    "must return OK and the correct view for a GET with a secondary contact" in {

      val jsonPayload: String = displaySubscriptionPayload(
        JsString("FirstName"), JsString("LastName"), JsString("Organisation Name"), JsString("email@email.com"),
        JsString("email2@email.com"), JsString("07111222333"))

      val displaySubscriptionDetails: DisplaySubscriptionForDACResponse = Json.parse(jsonPayload).as[DisplaySubscriptionForDACResponse]

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
        .thenReturn(Future.successful(displaySubscriptionDetails))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).overrides(
        bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
      ).build()

      val request = FakeRequest(GET, routes.ContactDetailsController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val json = jsonCaptor.getValue
      val contactDetails = (json \ "contactDetails").toString

      templateCaptor.getValue mustEqual "contactDetails.njk"
      contactDetails.contains("Contact name") mustBe true
      contactDetails.contains("Email address") mustBe true
      contactDetails.contains("Telephone") mustBe true
      contactDetails.contains("Secondary contact name") mustBe true
      contactDetails.contains("Secondary email address") mustBe true
      contactDetails.contains("Secondary telephone") mustBe true

      application.stop()
    }

    "must redirect to Problem page if display subscription details isn't available" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      val request = FakeRequest(GET, routes.ContactDetailsController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.IndexController.onPageLoad().url

      application.stop()
    }

    "must redirect to the next page when valid data is submitted" in {

      val jsonPayload = displaySubscriptionPayloadNoSecondary(
        JsString("FirstName"), JsString("LastName"), JsString("email@email.com"), JsString("07111222333"))
      val displaySubscriptionDetails = Json.parse(jsonPayload).as[DisplaySubscriptionForDACResponse]

      val updateSubscriptionForDACResponse = Json.parse(updateSubscriptionResponsePayload).as[UpdateSubscriptionForDACResponse]

      when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
        .thenReturn(Future.successful(displaySubscriptionDetails))

      when(mockSubscriptionConnector.updateSubscription(any(), any())(any(), any()))
        .thenReturn(Future.successful(updateSubscriptionForDACResponse))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).overrides(
        bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
      ).build()

      val request = FakeRequest(POST, routes.ContactDetailsController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.IndexController.onPageLoad().url

      application.stop()
    }

    "must redirect to the Problem page if users click submit and display subscription details isn't available" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val request = FakeRequest(POST, routes.ContactDetailsController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.IndexController.onPageLoad().url

      application.stop()
    }

    "must redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, routes.ContactDetailsController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }

    "must redirect to Session Expired for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(POST, routes.ContactDetailsController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }
  }
}
