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
import connectors.{CrossBorderArrangementsConnector, UpscanConnector}
import generators.Generators
import helpers.FakeCrossBorderArrangementsConnector
import matchers.JsonMatchers
import models.UserAnswers
import models.upscan._
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.UploadIDPage
import play.api.inject.bind
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import repositories.SessionRepository
import services.UploadProgressTracker
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.Future

class UploadFormControllerSpec extends SpecBase
  with MockitoSugar
  with NunjucksSupport
  with ScalaCheckPropertyChecks
  with JsonMatchers
  with Generators {

  val mockUpscanInitiateConnector = mock[UpscanConnector]
  val mockUploadProgressTracker = mock[UploadProgressTracker]
  val mockSessionRepository = mock[SessionRepository]

  val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
    .overrides(
      bind[CrossBorderArrangementsConnector].to[FakeCrossBorderArrangementsConnector],
      bind[UpscanConnector].toInstance(mockUpscanInitiateConnector)
    )
    .build()


  "upload form controller" - {
    "must initiate a request to upscan to bring back an upload form" in {
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockUpscanInitiateConnector.getUpscanFormData(any[UpscanInitiateRequest]())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(UpscanInitiateResponse(Reference(""), "", Map.empty[String, String])))
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[CrossBorderArrangementsConnector].to[FakeCrossBorderArrangementsConnector],
          bind[UpscanConnector].toInstance(mockUpscanInitiateConnector)
        )
        .build()

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val request = FakeRequest(GET, routes.UploadFormController.onPageLoad().url)
      val result = route(application, request).value

      status(result) mustBe OK
      verify(mockUpscanInitiateConnector, times(1)).getUpscanFormData(any[UpscanInitiateRequest]())(any[HeaderCarrier]())
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())
      templateCaptor.getValue mustEqual "upload-form.njk"
    }

    "must read the progress of the upload from the database" in {

      val uploadId = UploadId("uploadId")
      val userAnswers = UserAnswers(userAnswersId)
        .set(UploadIDPage, uploadId)
        .success.value

      //TODO: This should be mocked but strange errors had to be worked around
      val crossBorderArrangementsConnector = application.injector.instanceOf[FakeCrossBorderArrangementsConnector]

      val applicationLocal = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[CrossBorderArrangementsConnector].toInstance(crossBorderArrangementsConnector),
          bind[UploadProgressTracker].toInstance(mockUploadProgressTracker),
          bind[UpscanConnector].toInstance(mockUpscanInitiateConnector)
        ).build()

      val request = FakeRequest(GET, routes.UploadFormController.getStatus().url)

      def verifyResult(uploadStatus: UploadStatus, expectedResult: Int = SEE_OTHER): Unit = {

        crossBorderArrangementsConnector.setStatus(uploadStatus)

        when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

        val templateCaptor = ArgumentCaptor.forClass(classOf[String])



        val result = route(applicationLocal, request).value

        status(result) mustBe expectedResult
        if (expectedResult == OK) {
          verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())
          templateCaptor.getValue mustEqual "upload-result.njk"
        }

        application.stop()
        reset(mockUploadProgressTracker, mockRenderer)
      }

      verifyResult(InProgress, OK)
      verifyResult(Quarantined)
      verifyResult(Failed, INTERNAL_SERVER_ERROR)
      verifyResult(UploadedSuccessfully("name", "downloadUrl"))

    }

    "must show any returned error" in {

      val controller = application.injector.instanceOf[UploadFormController]

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
      when(mockAppConfig.sendEmailToggle).thenReturn(true)

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val argumentCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = controller.showError("errorCode", "errorMessage", "errorReqId")(FakeRequest("", ""))

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), argumentCaptor.capture())(any())
      templateCaptor.getValue mustEqual "error.njk"
      val captured = argumentCaptor.getValue
      (captured \\ "pageTitle").head.as[String] mustEqual "Upload Error"
      (captured \\ "heading").head.as[String]   mustEqual "errorMessage"
      (captured \\ "message").head.as[String] mustEqual "Code: errorCode, RequestId: errorReqId"
    }

    "must show File to large error when the errorCode is EntityTooLarge" in {

      val controller = application.injector.instanceOf[UploadFormController]

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
      when(mockAppConfig.sendEmailToggle).thenReturn(true)

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val argumentCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = controller.showError("EntityTooLarge", "Your proposed upload exceeds the maximum allowed size", "errorReqId")(FakeRequest("", ""))

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), argumentCaptor.capture())(any())
      templateCaptor.getValue mustEqual "fileTooLargeError.njk"
      val captured = argumentCaptor.getValue
      (captured \\ "xmlTechnicalGuidanceUrl").head.as[String] mustEqual "https://www.gov.uk/government/publications/cross-border-tax-arrangements-schema-and-supporting-documents"
    }

    "must display result page while file is successfully updated " in {

      val controller = application.injector.instanceOf[UploadFormController]

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val result = controller.showResult()(FakeRequest("", ""))

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())
      templateCaptor.getValue mustEqual "upload-result.njk"

    }

    }

}
