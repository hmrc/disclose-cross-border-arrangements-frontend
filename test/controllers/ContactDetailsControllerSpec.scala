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

package controllers

import base.SpecBase
import connectors.SubscriptionConnector
import generators.Generators
import helpers.JsonFixtures.{displaySubscriptionPayload, displaySubscriptionPayloadNoSecondary, updateSubscriptionResponsePayload}
import models.subscription.{DisplaySubscriptionDetailsAndStatus, DisplaySubscriptionForDACResponse, UpdateSubscriptionForDACResponse}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class ContactDetailsControllerSpec extends SpecBase
  with MockitoSugar
  with ScalaCheckPropertyChecks
  with Generators {

  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]

  "ContactDetails Controller" - {

    "must return OK and the correct view for a GET if user is an Individual" in {

      forAll(validDacID ,validEmailAddress, validPhoneNumber) {
        (safeID, email, phone) =>

          reset(mockRenderer, mockSubscriptionConnector)

          val jsonPayload = displaySubscriptionPayloadNoSecondary(
            JsString(safeID), JsString("FirstName"), JsString("LastName"), JsString(email), JsString(phone))

          val displaySubscriptionDetails = Json.parse(jsonPayload).as[DisplaySubscriptionForDACResponse]

          when(mockRenderer.render(any(), any())(any()))
            .thenReturn(Future.successful(Html("")))

          when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
            .thenReturn(Future.successful(DisplaySubscriptionDetailsAndStatus(Some(displaySubscriptionDetails))))

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
          contactDetails.contains("Email address") mustBe true
          contactDetails.contains("Telephone") mustBe true
          contactDetails.contains("Secondary contact name") mustBe false
          contactDetails.contains("Secondary email address") mustBe false
          contactDetails.contains("Secondary telephone") mustBe false

          application.stop()
      }
    }

    "must return OK and the correct view for a GET if user is an organisation with a secondary contact" in {

      forAll(validDacID, validEmailAddress, validEmailAddress, validPhoneNumber) {
        (safeID, email, secondaryEmail, phone) =>

          reset(mockRenderer, mockSubscriptionConnector)

          val jsonPayload: String = displaySubscriptionPayload(
            JsString(safeID), JsString("Organisation Name"), JsString("Secondary contact name"), JsString(email),
            JsString(secondaryEmail), JsString(phone))

          val displaySubscriptionDetails: DisplaySubscriptionForDACResponse = Json.parse(jsonPayload).as[DisplaySubscriptionForDACResponse]

          when(mockRenderer.render(any(), any())(any()))
            .thenReturn(Future.successful(Html("")))

          when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
            .thenReturn(Future.successful(DisplaySubscriptionDetailsAndStatus(Some(displaySubscriptionDetails))))

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
          val secondaryContactDetails = (json \ "secondaryContactDetails").toString

          templateCaptor.getValue mustEqual "contactDetails.njk"
          contactDetails.contains("Contact name") mustBe true
          contactDetails.contains("Email address") mustBe true
          contactDetails.contains("Telephone") mustBe false
          secondaryContactDetails.contains("Additional contact name") mustBe true
          secondaryContactDetails.contains("Additional contact email address") mustBe true
          secondaryContactDetails.contains("Additional contact telephone number") mustBe true

          application.stop()
      }
    }

    "must redirect to the /details-already-updated page if update lock is true" in {

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
        .thenReturn(Future.successful(DisplaySubscriptionDetailsAndStatus(None, isLocked = true)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).overrides(
        bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
      ).build()
      val request = FakeRequest(GET, routes.ContactDetailsController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.DetailsAlreadyUpdatedController.onPageLoad().url

      application.stop()
    }

    "must display the InternalServerError page if display subscription details isn't available" in {

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
        .thenReturn(Future.successful(DisplaySubscriptionDetailsAndStatus(None)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).overrides(
        bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
      ).build()
      val request = FakeRequest(GET, routes.ContactDetailsController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])

      val result = route(application, request).value

      status(result) mustEqual INTERNAL_SERVER_ERROR

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())

      templateCaptor.getValue mustEqual "internalServerError.njk"

      application.stop()
    }

    "must redirect to the next page when valid data is submitted" in {

      forAll(validDacID, validEmailAddress, validPhoneNumber) {
        (safeID, email, phone) =>
          val jsonPayload = displaySubscriptionPayloadNoSecondary(
            JsString(safeID), JsString("FirstName"), JsString("LastName"), JsString(email), JsString(phone))

          val displaySubscriptionDetails = Json.parse(jsonPayload).as[DisplaySubscriptionForDACResponse]
          val updateSubscriptionForDACResponse =
            Json.parse(updateSubscriptionResponsePayload(JsString(safeID))).as[UpdateSubscriptionForDACResponse]

          when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
            .thenReturn(Future.successful(DisplaySubscriptionDetailsAndStatus(Some(displaySubscriptionDetails))))

          when(mockSubscriptionConnector.updateSubscription(any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(updateSubscriptionForDACResponse)))

          when(mockSubscriptionConnector.cacheSubscription(any(), any())(any(), any()))
            .thenReturn(Future.successful(HttpResponse(OK, "")))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).overrides(
            bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
          ).build()

          val request = FakeRequest(POST, routes.ContactDetailsController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.DetailsAlreadyUpdatedController.onPageLoad().url

          application.stop()
      }
    }

    "must redirect to /details-not-updated page if update was unsuccessful" in {

      forAll(validDacID, validEmailAddress, validPhoneNumber) {
        (safeID, email, phone) =>
          val jsonPayload = displaySubscriptionPayloadNoSecondary(
            JsString(safeID), JsString("FirstName"), JsString("LastName"), JsString(email), JsString(phone))

          val displaySubscriptionDetails = Json.parse(jsonPayload).as[DisplaySubscriptionForDACResponse]

          when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
            .thenReturn(Future.successful(DisplaySubscriptionDetailsAndStatus(Some(displaySubscriptionDetails))))

          when(mockSubscriptionConnector.updateSubscription(any(), any())(any(), any()))
            .thenReturn(Future.successful(None))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).overrides(
            bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
          ).build()

          val request = FakeRequest(POST, routes.ContactDetailsController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.DetailsNotUpdatedController.onPageLoad().url

          application.stop()
      }
    }

    "must display the InternalServerError page if users click submit and display or update subscription details failed" in {

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
        .thenReturn(Future.successful(DisplaySubscriptionDetailsAndStatus(None)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).overrides(
        bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
      ).build()
      val request = FakeRequest(POST, routes.ContactDetailsController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])

      val result = route(application, request).value

      status(result) mustEqual INTERNAL_SERVER_ERROR

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())

      templateCaptor.getValue mustEqual "internalServerError.njk"

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
