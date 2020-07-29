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
import pages.{URLPage, ValidXMLPage}
import play.api.inject.bind
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import services.XMLValidationService

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {

  val userAnswers: UserAnswers = UserAnswers(userAnswersId)
    .set(ValidXMLPage, "file-name.xml")
    .success
    .value
    .set(URLPage, "url")
    .success
    .value

  val mockXmlValidationService: XMLValidationService =  mock[XMLValidationService]
  val mockCrossBorderArrangementsConnector: CrossBorderArrangementsConnector =  mock[CrossBorderArrangementsConnector]

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockXmlValidationService =  mock[XMLValidationService]

      when(mockXmlValidationService.loadXML(any[String]())).thenReturn(
        <test>
          <value>DAC6NEW</value>
        </test>
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

      val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual OK

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      application.stop()
    }

    "must redirect to upload form for a GET if userAnswers empty" in {

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.UploadFormController.onPageLoad().url

      application.stop()
    }

    "must redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }

    "when submitted the uploaded file must be submitted to the backend" in {

      val application = applicationBuilder(Some(userAnswers))
      .overrides(
        bind[XMLValidationService].toInstance(mockXmlValidationService),
        bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector)
      ).build()

      when(mockXmlValidationService.loadXML(any[String]())).
        thenReturn(<test><value>Success</value></test>)
      when(mockCrossBorderArrangementsConnector.submitDocument(any(), any(), any())(any())).
        thenReturn(Future.successful(GeneratedIDs(None, None)))

      val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      verify(mockCrossBorderArrangementsConnector, times(1))
        .submitDocument(any(), any(), any())(any())

      application.stop()
    }

    "must redirect to the creation confirmation page when user submits XML and the instructions is DAC6NEW" in {

      val application = applicationBuilder(Some(userAnswers))
        .overrides(
          bind[XMLValidationService].toInstance(mockXmlValidationService),
          bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector)
        ).build()

      val xml =
        <DAC6_Arrangement version="First">
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      when(mockXmlValidationService.loadXML(any[String]())).thenReturn(xml)
      when(mockCrossBorderArrangementsConnector.submitDocument(any(), any(), any())(any())).
        thenReturn(Future.successful(GeneratedIDs(None, None)))

      val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.CreateConfirmationController.onPageLoad().url

      application.stop()
    }

    "must redirect to the upload confirmation page when user submits XML and the instructions is DAC6ADD" in {

      val application = applicationBuilder(Some(userAnswers))
        .overrides(
          bind[XMLValidationService].toInstance(mockXmlValidationService),
          bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector)
        ).build()

      val xml =
        <DAC6_Arrangement version="First">
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      when(mockXmlValidationService.loadXML(any[String]())).thenReturn(xml)
      when(mockCrossBorderArrangementsConnector.submitDocument(any(), any(), any())(any())).
        thenReturn(Future.successful(GeneratedIDs(None, None)))

      val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UploadConfirmationController.onPageLoad().url

      application.stop()
    }

    "must redirect to the replacement confirmation page when user submits XML and the instructions is DAC6REP" in {

      val application = applicationBuilder(Some(userAnswers))
        .overrides(
          bind[XMLValidationService].toInstance(mockXmlValidationService),
          bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector)
        ).build()

      val xml =
        <DAC6_Arrangement version="First">
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      when(mockXmlValidationService.loadXML(any[String]())).thenReturn(xml)
      when(mockCrossBorderArrangementsConnector.submitDocument(any(), any(), any())(any())).
        thenReturn(Future.successful(GeneratedIDs(None, None)))

      val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.ReplaceConfirmationController.onPageLoad().url

      application.stop()
    }
  }
}
