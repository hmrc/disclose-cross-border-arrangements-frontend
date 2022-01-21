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
import models.{SubmissionHistory, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import pages.HistoryPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html

import scala.concurrent.Future

class SearchHistoryResultsControllerSpec extends SpecBase {

  "SearchHistoryResults Controller" - {

    "return OK and the correct view for a GET" in {

      val mockCrossBorderArrangementsConnector = mock[CrossBorderArrangementsConnector]

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      when(mockCrossBorderArrangementsConnector.searchDisclosures(any())(any()))
        .thenReturn(Future.successful(SubmissionHistory(List())))

      val userAnswers = UserAnswers(userAnswersId)
        .set(HistoryPage, "fileName.xml")
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector)
        )
        .build()
      val request        = FakeRequest(GET, routes.SearchHistoryResultsController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())

      templateCaptor.getValue mustEqual "submissionHistorySearchResults.njk"

      application.stop()
    }
  }
}
