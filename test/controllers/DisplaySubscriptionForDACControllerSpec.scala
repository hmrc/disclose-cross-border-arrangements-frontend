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
import helpers.JsonFixtures._
import models.subscription.{DisplaySubscriptionDetailsAndStatus, DisplaySubscriptionForDACResponse}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.libs.json.{JsString, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html

import scala.concurrent.Future

class DisplaySubscriptionForDACControllerSpec extends SpecBase
  with MockitoSugar
  with ScalaCheckPropertyChecks
  with Generators {

  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]

  "DisplaySubscriptionForDAC Controller" - {

    "return OK and the correct view for a GET and there are subscription details available" in {

      forAll(validDacID, validEmailAddress, validEmailAddress, validPhoneNumber) {
        (safeID, email, secondaryEmail, phone) =>

          reset(mockRenderer, mockSubscriptionConnector)

          val jsonPayload = displaySubscriptionPayloadNoSecondary(
            JsString(safeID), JsString("FirstName"), JsString("LastName"),
            JsString(email), JsString(phone))

          val displaySubscriptionDetails = Json.parse(jsonPayload).validate[DisplaySubscriptionForDACResponse].get

          when(mockRenderer.render(any(), any())(any()))
            .thenReturn(Future.successful(Html("")))

          when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
            .thenReturn(Future.successful(DisplaySubscriptionDetailsAndStatus(Some(displaySubscriptionDetails))))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
            ).build()

          val request = FakeRequest(GET, routes.DisplaySubscriptionForDACController.onPageLoad().url)
          val templateCaptor = ArgumentCaptor.forClass(classOf[String])

          val result = route(application, request).value

          status(result) mustEqual OK

          verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())

          templateCaptor.getValue mustEqual "displaySubscriptionForDAC.njk"

          application.stop()
      }
    }

    "return OK and the correct view for a GET and there are two contacts & subscription details available" in {

      forAll(validDacID, validEmailAddress, validEmailAddress, validPhoneNumber) {
      (safeID, email, secondaryEmail, phone) =>

      reset(mockRenderer, mockSubscriptionConnector)

      val jsonPayload = displaySubscriptionPayload(
      JsString(safeID), JsString("FirstName"), JsString("Secondary contact name"),
      JsString(email), JsString(secondaryEmail), JsString(phone))

      val displaySubscriptionDetails = Json.parse(jsonPayload).validate[DisplaySubscriptionForDACResponse].get

      when(mockRenderer.render(any(), any())(any()))
      .thenReturn(Future.successful(Html("")))

      when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
      .thenReturn(Future.successful(DisplaySubscriptionDetailsAndStatus(Some(displaySubscriptionDetails))))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .overrides(
      bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
      ).build()

      val request = FakeRequest(GET, routes.DisplaySubscriptionForDACController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())

      templateCaptor.getValue mustEqual "displaySubscriptionForDAC.njk"

      application.stop()
      }
    }

    "redirect to index page if display subscription isn't available" in {

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
        .thenReturn(Future.successful(DisplaySubscriptionDetailsAndStatus(None)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
        ).build()

      val request = FakeRequest(GET, routes.DisplaySubscriptionForDACController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.IndexController.onPageLoad().url

      application.stop()
    }
  }
}
