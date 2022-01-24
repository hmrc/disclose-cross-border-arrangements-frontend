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
import connectors.{CrossBorderArrangementsConnector, SubscriptionConnector}
import helpers.XmlLoadHelper
import models.subscription.DisplaySubscriptionDetailsAndStatus
import models.{Dac6MetaData, GeneratedIDs, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import pages.{Dac6MetaDataPage, URLPage, ValidXMLPage}
import play.api.inject.bind
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import services.EmailService
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class DeleteDisclosureSummaryControllerSpec extends SpecBase {

  "DeleteDisclosureSummaryController" - {

    "return OK and the correct view for a GET" in {

      val metaData = Dac6MetaData("DAC6DEL",
                                  Some("GBA20200601AAA000"),
                                  Some("GBD20200601AAA001"),
                                  disclosureInformationPresent = true,
                                  initialDisclosureMA = false,
                                  messageRefId = "GB0000000XXX"
      )
      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val userAnswers = UserAnswers(userAnswersId)
        .set(ValidXMLPage, "file-name.xml")
        .success
        .value
        .set(Dac6MetaDataPage, metaData)
        .success
        .value

      val application = applicationBuilder(Some(userAnswers)).build()

      val request        = FakeRequest(GET, routes.DeleteDisclosureSummaryController.onPageLoad().url)
      val result         = route(application, request).value
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor     = ArgumentCaptor.forClass(classOf[JsObject])

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

    "must redirect to upload form for a POST if XML url or XML is missing from user answers" in {

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val request = FakeRequest(GET, routes.DeleteDisclosureSummaryController.onPageLoad().url)
      val result  = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.UploadFormController.onPageLoad().url

      application.stop()

    }

    "when submitted the uploaded file must be submitted to the backend" in {
      val mockEmailService: EmailService                   = mock[EmailService]
      val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
      val metaData: Dac6MetaData =
        Dac6MetaData("DAC6NEW", None, None, disclosureInformationPresent = false, initialDisclosureMA = false, "GB0000000XXX")

      val userAnswers = UserAnswers(userAnswersId)
        .set(ValidXMLPage, "file-name.xml")
        .success
        .value
        .set(URLPage, "url")
        .success
        .value
        .set(Dac6MetaDataPage, metaData)
        .success
        .value

      val mockXmlValidationService             = mock[XmlLoadHelper]
      val mockCrossBorderArrangementsConnector = mock[CrossBorderArrangementsConnector]

      val application = applicationBuilder(Some(userAnswers))
        .overrides(
          bind[XmlLoadHelper].toInstance(mockXmlValidationService),
          bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector),
          bind[EmailService].toInstance(mockEmailService),
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[FrontendAppConfig].toInstance(mockAppConfig)
        )
        .build()

      when(mockXmlValidationService.loadXML(any[String]())).thenReturn(<test><value>Success</value></test>)
      when(mockCrossBorderArrangementsConnector.submitDocument(any(), any(), any())(any())).thenReturn(Future.successful(GeneratedIDs(None, None)))
      when(mockAppConfig.sendEmailToggle).thenReturn(true)
      when(mockEmailService.sendEmail(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(HttpResponse(ACCEPTED, ""))))
      when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
        .thenReturn(Future.successful(DisplaySubscriptionDetailsAndStatus(None)))

      val request = FakeRequest(POST, routes.DeleteDisclosureSummaryController.onSubmit().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.DeleteDisclosureConfirmationController.onPageLoad().url
      verify(mockCrossBorderArrangementsConnector, times(1))
        .submitDocument(any(), any(), any())(any())
      verify(mockEmailService, times(1)).sendEmail(any(), any(), any(), any())(any())

      application.stop()
    }

    "when submitted the uploaded file must be submitted to the backend with no email when toggled off" in {
      val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
      val metaData: Dac6MetaData =
        Dac6MetaData("DAC6NEW", None, None, disclosureInformationPresent = false, initialDisclosureMA = false, "GB0000000XXX")

      val userAnswers = UserAnswers(userAnswersId)
        .set(ValidXMLPage, "file-name.xml")
        .success
        .value
        .set(URLPage, "url")
        .success
        .value
        .set(Dac6MetaDataPage, metaData)
        .success
        .value

      val mockXmlLoadHelper                    = mock[XmlLoadHelper]
      val mockCrossBorderArrangementsConnector = mock[CrossBorderArrangementsConnector]

      val application = applicationBuilder(Some(userAnswers))
        .overrides(
          bind[XmlLoadHelper].toInstance(mockXmlLoadHelper),
          bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector),
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[FrontendAppConfig].toInstance(mockAppConfig)
        )
        .build()

      when(mockXmlLoadHelper.loadXML(any[String]())).thenReturn(<test><value>Success</value></test>)
      when(mockCrossBorderArrangementsConnector.submitDocument(any(), any(), any())(any())).thenReturn(Future.successful(GeneratedIDs(None, None)))
      when(mockAppConfig.sendEmailToggle).thenReturn(false)
      when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
        .thenReturn(Future.successful(DisplaySubscriptionDetailsAndStatus(None)))

      val request = FakeRequest(POST, routes.DeleteDisclosureSummaryController.onSubmit().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.DeleteDisclosureConfirmationController.onPageLoad().url
      verify(mockCrossBorderArrangementsConnector, times(1))
        .submitDocument(any(), any(), any())(any())

      application.stop()
    }
  }
}
