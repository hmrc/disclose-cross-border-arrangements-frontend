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
import models.{GenericError, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import pages.{GenericErrorPage, InvalidXMLPage}
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html

import scala.concurrent.Future

class InvalidXMLControllerSpec extends SpecBase {

  "InvalidXMLController" - {

    "return OK and the correct view for a GET" in {

      val mockErrors   = Seq(GenericError(1, "test"))
      val mockfileName = "fileName.xml"

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val userAnswers = UserAnswers(userAnswersId)
        .set(InvalidXMLPage, mockfileName)
        .success
        .value
        .set(GenericErrorPage, mockErrors)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      val request        = FakeRequest(GET, routes.InvalidXMLController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor     = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual "invalidXML.njk"
      jsonCaptor.getValue.value.get("fileName") mustBe Some(Json.toJson("fileName.xml"))

      application.stop()
    }

    "throw Exception and redirect to Internal Server Error page when errors or fileName is missing" in {

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val request        = FakeRequest(GET, routes.InvalidXMLController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val result         = route(application, request).value

      an[RuntimeException] mustBe thrownBy {
        status(result) mustEqual OK

        verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())

        templateCaptor.getValue mustEqual "internalServerError.njk"
      }

      application.stop()
    }
  }
}
