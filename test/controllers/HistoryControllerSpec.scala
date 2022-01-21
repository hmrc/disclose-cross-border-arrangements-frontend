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

package controllers

import base.SpecBase
import connectors.CrossBorderArrangementsConnector
import forms.SearchDisclosuresFormProvider
import matchers.JsonMatchers
import models.{SubmissionDetails, SubmissionHistory}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, route, status, _}
import play.twirl.api.Html
import repositories.SessionRepository
import uk.gov.hmrc.viewmodels.NunjucksSupport

import java.time.LocalDateTime
import scala.concurrent.Future

class HistoryControllerSpec extends SpecBase with NunjucksSupport with JsonMatchers {

  def onwardRoute: Call                                                      = Call("GET", "/foo")
  val mockSessionRepository: SessionRepository                               = mock[SessionRepository]
  val mockCrossBorderArrangementsConnector: CrossBorderArrangementsConnector = mock[CrossBorderArrangementsConnector]

  val submissionHistory: SubmissionHistory = SubmissionHistory(
    List(
      SubmissionDetails(
        "enrolmentID",
        LocalDateTime.parse("2007-12-03T10:15:30"),
        "fileName",
        Some("arrangementID"),
        Some("disclosureID"),
        "New",
        initialDisclosureMA = true,
        messageRefId = "GB0000000XXX"
      )
    )
  )

  val formProvider       = new SearchDisclosuresFormProvider()
  val form: Form[String] = formProvider()

  "History Controller" - {
    "must return OK and the correct view for a GET" in {

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("foo")))

      when(mockCrossBorderArrangementsConnector.retrievePreviousSubmissions(any())(any()))
        .thenReturn(Future.successful(submissionHistory))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector)
        )
        .build()

      val request = FakeRequest(GET, routes.HistoryController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual OK

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())

      templateCaptor.getValue mustEqual "submissionHistory.njk"

      application.stop()
    }

    "must redirect to the next page when valid data is submitted" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      val request = FakeRequest(POST, routes.HistoryController.onSearch().url)
        .withFormUrlEncodedBody(("searchBox", "fileName.xml"))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual onwardRoute.url

      application.stop()
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      when(mockCrossBorderArrangementsConnector.retrievePreviousSubmissions(any())(any()))
        .thenReturn(Future.successful(submissionHistory))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector)
        )
        .build()

      val request = FakeRequest(POST, routes.HistoryController.onSearch().url)
        .withFormUrlEncodedBody(("searchBox", ""))
      val boundForm      = form.bind(Map("searchBox" -> ""))
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor     = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val expectedJson = Json.obj(
        "form" -> boundForm
      )

      templateCaptor.getValue mustEqual "submissionHistory.njk"
      jsonCaptor.getValue must containJson(expectedJson)

      application.stop()
    }
  }
}
