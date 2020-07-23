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
import connectors.CrossBorderArrangementsConnector
import models.{GeneratedIDs, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{URLPage, ValidXMLPage}
import play.api.inject.bind
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import services.XMLValidationService

import scala.concurrent.Future

class DeleteDisclosureSummaryControllerSpec extends SpecBase with MockitoSugar {

  "DeleteDisclosureSummaryController" - {

    "return OK and the correct view for a GET" in {

      val mockXmlValidationService =  mock[XMLValidationService]

      when(mockXmlValidationService.loadXML(any[String]())).thenReturn(
        <test><value>test</value></test>
      )

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val userAnswers = UserAnswers(userAnswersId)
        .set(ValidXMLPage, "file-name.xml")
        .success.value
        .set(URLPage, "url")
        .success.value

      val application = applicationBuilder(Some(userAnswers)).overrides(
        bind[XMLValidationService].toInstance(mockXmlValidationService),
      ).build()

      val request = FakeRequest(GET, routes.DeleteDisclosureSummaryController.onPageLoad().url)
      val result = route(application, request).value
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual "deleteDisclosure.njk"

      application.stop()
    }

    "must redirect to upload form for a GET if userAnswers empty" in {

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val request = FakeRequest(GET, routes.DeleteDisclosureSummaryController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.UploadFormController.onPageLoad().url

      application.stop()

    }

    "must redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, routes.DeleteDisclosureSummaryController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }

    "when submitted the uploaded file must be submitted to the backend" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(ValidXMLPage, "file-name.xml")
        .success
        .value
        .set(URLPage, "url")
        .success
        .value

      val mockXmlValidationService =  mock[XMLValidationService]
      val mockCrossBorderArrangementsConnector =  mock[CrossBorderArrangementsConnector]

      val application = applicationBuilder(Some(userAnswers))
        .overrides(
          bind[XMLValidationService].toInstance(mockXmlValidationService),
          bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector)
        ).build()

      when(mockXmlValidationService.loadXML(any[String]())).
        thenReturn(<test><value>Success</value></test>)
      when(mockCrossBorderArrangementsConnector.submitDocument(any(), any())(any())).
        thenReturn(Future.successful(GeneratedIDs(None, None)))

      val request = FakeRequest(POST, routes.DeleteDisclosureSummaryController.onSubmit().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      verify(mockCrossBorderArrangementsConnector, times(1))
        .submitDocument(any(), any())(any())

      application.stop()
    }
  }
}
