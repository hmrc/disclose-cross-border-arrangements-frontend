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
import config.FrontendAppConfig
import connectors.CrossBorderArrangementsConnector
import matchers.JsonMatchers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html

import scala.concurrent.Future

class IndexControllerSpec extends SpecBase with JsonMatchers {

  "Index Controller" - {

    "must return OK and the correct view for a GET" in {
      val mockCrossBorderArrangementsConnector = mock[CrossBorderArrangementsConnector]

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("foo")))

      when(mockCrossBorderArrangementsConnector.findNoOfPreviousSubmissions(any())(any()))
        .thenReturn(Future.successful(0L))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector),
          bind[FrontendAppConfig].toInstance(mockAppConfig)
        )
        .build()

      val request = FakeRequest(GET, routes.IndexController.onPageLoad.url)

      val result = route(application, request).value

      status(result) mustEqual OK

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor     = ArgumentCaptor.forClass(classOf[JsObject])

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val expectedJson = Json.obj(
        "hasSubmissions" -> false
      )

      templateCaptor.getValue mustEqual "index.njk"
      jsonCaptor.getValue must containJson(expectedJson)

      application.stop()
    }

    "must return OK and the correct view for a GET with both links present" in {
      val mockCrossBorderArrangementsConnector = mock[CrossBorderArrangementsConnector]

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("foo")))

      when(mockCrossBorderArrangementsConnector.findNoOfPreviousSubmissions(any())(any()))
        .thenReturn(Future.successful(2L))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector),
          bind[FrontendAppConfig].toInstance(mockAppConfig)
        )
        .build()

      val request = FakeRequest(GET, routes.IndexController.onPageLoad.url)

      val result = route(application, request).value

      status(result) mustEqual OK

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor     = ArgumentCaptor.forClass(classOf[JsObject])

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val expectedJson = Json.obj(
        "hasSubmissions" -> true
      )

      templateCaptor.getValue mustEqual "index.njk"
      jsonCaptor.getValue must containJson(expectedJson)

      application.stop()
    }
  }
}
