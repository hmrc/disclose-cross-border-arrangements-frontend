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
import forms.SecondaryContactEmailAddressFormProvider
import generators.Generators
import matchers.JsonMatchers
import models.UserAnswers
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.SecondaryContactEmailAddressPage
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import repositories.SessionRepository
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.Future

class SecondaryContactEmailAddressControllerSpec extends SpecBase
  with MockitoSugar
  with NunjucksSupport
  with JsonMatchers
  with ScalaCheckPropertyChecks
  with Generators {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new SecondaryContactEmailAddressFormProvider()
  val form = formProvider()

  lazy val secondaryContactEmailAddressRoute = routes.SecondaryContactEmailAddressController.onPageLoad().url

  "SecondaryContactEmailAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      val request = FakeRequest(GET, secondaryContactEmailAddressRoute)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val expectedJson = Json.obj(
        "form" -> form
      )

      templateCaptor.getValue mustEqual "secondaryContactEmailAddress.njk"
      jsonCaptor.getValue must containJson(expectedJson)

      application.stop()
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      forAll(validEmailAddress) {
        email =>
          reset(mockRenderer)

          when(mockRenderer.render(any(), any())(any()))
            .thenReturn(Future.successful(Html("")))

          val userAnswers = UserAnswers(userAnswersId).set(SecondaryContactEmailAddressPage, email).success.value
          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
          val request = FakeRequest(GET, secondaryContactEmailAddressRoute)
          val templateCaptor = ArgumentCaptor.forClass(classOf[String])
          val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

          val result = route(application, request).value

          status(result) mustEqual OK

          verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

          val filledForm = form.bind(Map("email" -> email))

          val expectedJson = Json.obj(
            "form" -> filledForm
          )

          templateCaptor.getValue mustEqual "secondaryContactEmailAddress.njk"
          jsonCaptor.getValue must containJson(expectedJson)

          application.stop()
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      forAll(validEmailAddress) {
        email =>
          val mockSessionRepository = mock[SessionRepository]

          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application =
            applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
                bind[SessionRepository].toInstance(mockSessionRepository)
              )
              .build()

          val request =
            FakeRequest(POST, secondaryContactEmailAddressRoute)
              .withFormUrlEncodedBody(("email", email))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          application.stop()
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      val request = FakeRequest(POST, secondaryContactEmailAddressRoute).withFormUrlEncodedBody(("email", ""))
      val boundForm = form.bind(Map("email" -> ""))
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val expectedJson = Json.obj(
        "form" -> boundForm
      )

      templateCaptor.getValue mustEqual "secondaryContactEmailAddress.njk"
      jsonCaptor.getValue must containJson(expectedJson)

      application.stop()
    }

    "must redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, secondaryContactEmailAddressRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }

    "must redirect to Session Expired for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request =
        FakeRequest(POST, secondaryContactEmailAddressRoute)
          .withFormUrlEncodedBody(("email", "answer"))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }
  }
}
