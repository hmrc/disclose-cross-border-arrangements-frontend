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

import java.time.LocalDateTime

import base.SpecBase
import connectors.CrossBorderArrangementsConnector
import models.{SubmissionDetails, SubmissionHistory}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, route, status}
import play.twirl.api.Html
import play.api.test.Helpers._

import scala.concurrent.Future

class HistoryControllerSpec extends SpecBase {
  
  "History Controller" - {
    "must return OK and the correct view for a GET" in {
      val mockCrossBorderArrangementsConnector = mock[CrossBorderArrangementsConnector]
      val submissionHistory = SubmissionHistory(
        Seq(
          SubmissionDetails(
            "enrolmentID",
            LocalDateTime.parse("2007-12-03T10:15:30"),
            "fileName",
            None,
            None,
            "New",
            initialDisclosureMA = false
          )
        )
      )

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("foo")))

      when(mockCrossBorderArrangementsConnector.retrievePreviousSubmissions(any())(any()))
        .thenReturn(Future.successful(submissionHistory))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector)
        ).build()

      val request = FakeRequest(GET, routes.HistoryController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual OK

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())

      templateCaptor.getValue mustEqual "submissionHistory.njk"

      application.stop()
    }
  }
}
