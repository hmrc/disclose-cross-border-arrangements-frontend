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
import config.FrontendAppConfig
import connectors.{CrossBorderArrangementsConnector, EnrolmentStoreConnector, SubscriptionConnector}
import models.enrolments.{Enrolment, EnrolmentResponse, KnownFact}
import models.{Dac6MetaData, GeneratedIDs, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import pages.{Dac6MetaDataPage, URLPage, ValidXMLPage}
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import services.{EmailService, XMLValidationService}
import uk.gov.hmrc.http.HttpResponse
import utils.EnrolmentConstants

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with BeforeAndAfterEach {

  val userAnswers: UserAnswers = UserAnswers(userAnswersId)
    .set(ValidXMLPage, "file-name.xml")
    .success
    .value
    .set(URLPage, "url")
    .success
    .value

  val mockXmlValidationService: XMLValidationService =  mock[XMLValidationService]
  val mockCrossBorderArrangementsConnector: CrossBorderArrangementsConnector =  mock[CrossBorderArrangementsConnector]
  val mockEnrolmentStoreConnector: EnrolmentStoreConnector = mock[EnrolmentStoreConnector]
  val mockEmailService: EmailService = mock[EmailService]
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]

  val identifiers = Seq(KnownFact(EnrolmentConstants.dac6IdentifierKey, "id"))
  val verifiers = Seq(
    KnownFact("CONTACTNAME", "test testing"),
    KnownFact("EMAIL", "me@test.com"))
  val enrolmentResponse: EnrolmentResponse = EnrolmentResponse(EnrolmentConstants.dac6EnrolmentKey, Seq(Enrolment(identifiers, verifiers)))

   override def beforeEach: Unit = {
     when(mockEnrolmentStoreConnector.getEnrolments(any())(any())).
       thenReturn(Future.successful(Some(enrolmentResponse)))

     when(mockSubscriptionConnector.displaySubscriptionDetails(any())(any(), any()))
       .thenReturn(Future.successful(None))

     reset(mockEmailService)
   }


  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET" in {

      val metaData = Dac6MetaData("DAC6NEW", None, None, disclosureInformationPresent = true,
                                  initialDisclosureMA = false, messageRefId = "GB0000000XXX")

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val userAnswers = UserAnswers(userAnswersId)
        .set(ValidXMLPage, "file-name.xml")
        .success.value
        .set(Dac6MetaDataPage, metaData)
        .success.value

      val application = applicationBuilder(Some(userAnswers))
        .overrides(bind[EnrolmentStoreConnector].toInstance(mockEnrolmentStoreConnector))
        .build()

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

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[EnrolmentStoreConnector].toInstance(mockEnrolmentStoreConnector))
        .build()

      val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.UploadFormController.onPageLoad().url

      application.stop()
    }

    "must redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[EnrolmentStoreConnector].toInstance(mockEnrolmentStoreConnector))
        .build()

      val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.SessionExpiredController.onPageLoad().url

      application.stop()
    }

    "when submitted the uploaded file must be submitted to the backend and redirect to /upload if import instruction is missing" in {

      val application = applicationBuilder(Some(userAnswers))
      .overrides(
        bind[XMLValidationService].toInstance(mockXmlValidationService),
        bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector),
        bind[EnrolmentStoreConnector].toInstance(mockEnrolmentStoreConnector),
        bind[FrontendAppConfig].toInstance(mockAppConfig)
      ).build()

      when(mockAppConfig.sendEmailToggle).thenReturn(false)

      when(mockXmlValidationService.loadXML(any[String]())).
        thenReturn(<test><value>Success</value></test>)
      when(mockCrossBorderArrangementsConnector.submitDocument(any(), any(), any())(any())).
        thenReturn(Future.successful(GeneratedIDs(None, None)))

      val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustBe routes.UploadFormController.onPageLoad().url
      verify(mockCrossBorderArrangementsConnector, times(1))
        .submitDocument(any(), any(), any())(any())
      verify(mockEmailService, times(0)).sendEmail(any(), any(), any(), any())(any())

      application.stop()
    }

    "must redirect to the creation confirmation page when user submits XML and the instructions is DAC6NEW" in {
      val metaData: Dac6MetaData = Dac6MetaData("DAC6NEW", None, None, "GB0000000XXX")
      val updatedUserAnswers: UserAnswers = userAnswers.set(Dac6MetaDataPage, metaData).success.value

      val xml =
        <DAC6_Arrangement version="First">
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val application = applicationBuilder(Some(updatedUserAnswers))
        .overrides(
          bind[XMLValidationService].toInstance(mockXmlValidationService),
          bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector),
          bind[EnrolmentStoreConnector].toInstance(mockEnrolmentStoreConnector),
          bind[EmailService].toInstance(mockEmailService),
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[FrontendAppConfig].toInstance(mockAppConfig)
        ).build()

      when(mockXmlValidationService.loadXML(any[String]())).thenReturn(xml)
      when(mockCrossBorderArrangementsConnector.submitDocument(any(), any(), any())(any())).
        thenReturn(Future.successful(GeneratedIDs(None, None)))
      when(mockAppConfig.sendEmailToggle).thenReturn(true)
      when(mockEmailService.sendEmail(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(HttpResponse(ACCEPTED, ""))))

      val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.CreateConfirmationController.onPageLoad().url
      verify(mockEmailService, times(1)).sendEmail(any(), any(), any(), any())(any())

      application.stop()
    }

    "must redirect to the upload confirmation page when user submits XML and the instructions is DAC6ADD" in {
      val metaData: Dac6MetaData = Dac6MetaData("DAC6ADD", None, None, "GB0000000XXX")
      val updatedUserAnswers: UserAnswers = userAnswers.set(Dac6MetaDataPage, metaData).success.value

      val application = applicationBuilder(Some(updatedUserAnswers))
        .overrides(
          bind[XMLValidationService].toInstance(mockXmlValidationService),
          bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector),
          bind[EnrolmentStoreConnector].toInstance(mockEnrolmentStoreConnector),
          bind[EmailService].toInstance(mockEmailService),
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[FrontendAppConfig].toInstance(mockAppConfig)
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
      when(mockAppConfig.sendEmailToggle).thenReturn(true)
      when(mockEmailService.sendEmail(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(HttpResponse(ACCEPTED, ""))))

      val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UploadConfirmationController.onPageLoad().url
      verify(mockEmailService, times(1)).sendEmail(any(), any(), any(), any())(any())

      application.stop()
    }

    "must redirect to the replacement confirmation page when user submits XML and the instructions is DAC6REP" in {
      val metaData: Dac6MetaData = Dac6MetaData("DAC6REP", None, None, "GB0000000XXX")
      val updatedUserAnswers: UserAnswers = userAnswers.set(Dac6MetaDataPage, metaData).success.value

      val application = applicationBuilder(Some(updatedUserAnswers))
        .overrides(
          bind[XMLValidationService].toInstance(mockXmlValidationService),
          bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector),
          bind[EnrolmentStoreConnector].toInstance(mockEnrolmentStoreConnector),
          bind[EmailService].toInstance(mockEmailService),
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[FrontendAppConfig].toInstance(mockAppConfig)
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
      when(mockAppConfig.sendEmailToggle).thenReturn(true)
      when(mockEmailService.sendEmail(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(HttpResponse(ACCEPTED, ""))))

      val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.ReplaceConfirmationController.onPageLoad().url
      verify(mockEmailService, times(1)).sendEmail(any(), any(), any(), any())(any())

      application.stop()
    }
  }
}
