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
import connectors.{CrossBorderArrangementsConnector, UpscanConnector}
import generators.Generators
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
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import repositories.SessionRepository
import services.UploadProgressTracker
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.Future

class UploadFormControllerSpec extends SpecBase
  with MockitoSugar
  with NunjucksSupport
  with ScalaCheckPropertyChecks
  with JsonMatchers
  with Generators {

  val mockCrossBorderArrangementsConnector = mock[CrossBorderArrangementsConnector]
  val mockUpscanInitiateConnector = mock[UpscanConnector]
  val mockUploadProgressTracker = mock[UploadProgressTracker]
  val mockSessionRepository = mock[SessionRepository]


  "upload form controller" - {
    "must initiate a request to upscan to bring back an upload form" in {

      when(mockCrossBorderArrangementsConnector.requestUpload(any[UploadId](), any[Reference]())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(HttpResponse(200, "")))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockUpscanInitiateConnector.getUpscanFormData(any[UpscanInitiateRequest]())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(UpscanInitiateResponse(Reference(""), "", Map.empty[String, String])))
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[UpscanConnector].toInstance(mockUpscanInitiateConnector),
          bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector)
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

    /*"must read the progress of the upload from the database" in {

      val uploadId = UploadId("uploadId")
      val userAnswers = UserAnswers(userAnswersId)
        .set(UploadIDPage, uploadId)
        .success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
            bind[UploadProgressTracker].toInstance(mockUploadProgressTracker),
            bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector)
        ).build()

      val request = FakeRequest(GET, routes.UploadFormController.getStatus().url)

      def verifyResult(uploadStatus: UploadStatus, expectedResult: Int = SEE_OTHER): Unit = {

        when(mockCrossBorderArrangementsConnector.getUploadStatus(any())(any()))
          .thenReturn(Future.successful(Some(uploadStatus)))
        when(mockCrossBorderArrangementsConnector.requestUpload(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(200, "")))

        when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

        val templateCaptor = ArgumentCaptor.forClass(classOf[String])



        val result = route(application, request).value

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

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val argumentCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val expectedArgument = Json.obj("pageTitle" -> "Upload Error",
        "heading"-> "errorMessage",
        "message" -> s"Code: errorCode, RequestId: errorReqId",
        "config" -> Json.obj("betaFeedbackUnauthenticatedUrl" -> "http://localhost:9250/contact/beta-feedback-unauthenticated?service=DAC6",
          "reportAProblemPartialUrl" -> "http://localhost:9250/contact/problem_reports_ajax?service=DAC6",
          "reportAProblemNonJSUrl" -> "http://localhost:9250/contact/problem_reports_nonjs?service=DAC6",
          "signOutUrl" -> "http://localhost:9514/feedback/disclose-cross-border-arrangements")
      )
      val result = controller.showError("errorCode", "errorMessage", "errorReqId")(FakeRequest("", ""))

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), argumentCaptor.capture())(any())
      templateCaptor.getValue mustEqual "error.njk"
      argumentCaptor.getValue mustEqual expectedArgument
    }

    "must show File to large error when the errorCode is EntityTooLarge" in {

      val controller = application.injector.instanceOf[UploadFormController]

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val argumentCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val expectedArgument = Json.obj(
        "config" -> Json.obj("betaFeedbackUnauthenticatedUrl" -> "http://localhost:9250/contact/beta-feedback-unauthenticated?service=DAC6",
          "reportAProblemPartialUrl" -> "http://localhost:9250/contact/problem_reports_ajax?service=DAC6",
          "reportAProblemNonJSUrl" -> "http://localhost:9250/contact/problem_reports_nonjs?service=DAC6",
          "signOutUrl" -> "http://localhost:9514/feedback/disclose-cross-border-arrangements"),
          "guidanceLink" -> "???"
      )

      val result = controller.showError("EntityTooLarge", "Your proposed upload exceeds the maximum allowed size", "errorReqId")(FakeRequest("", ""))

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), argumentCaptor.capture())(any())
      templateCaptor.getValue mustEqual "fileTooLargeError.njk"
      argumentCaptor.getValue mustEqual expectedArgument

    }

    "must display result page while file is successfully updated " in {

      val controller = application.injector.instanceOf[UploadFormController]

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val result = controller.showResult()(FakeRequest("", ""))

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())
      templateCaptor.getValue mustEqual "upload-result.njk"

    }*/

    }

}
