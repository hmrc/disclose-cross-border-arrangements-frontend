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
import connectors.UpscanConnector
import generators.Generators
import matchers.JsonMatchers
import models.upscan.{Reference, UpscanInitiateRequest, UpscanInitiateResponse}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
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

  val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
    .overrides(
      bind[UpscanConnector].toInstance(mockUpscanInitiateConnector)
    )
    .build()

  "upload form controller" - {
    "must initiate a request to upscan to bring back an upload form" in {
      val controller = application.injector.instanceOf[UploadFormController]

      when(mockUpscanInitiateConnector.getUpscanFormData(any[UpscanInitiateRequest]())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(UpscanInitiateResponse(Reference(""), "", Map.empty[String, String])))
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val result = controller.onPageLoad()(FakeRequest("", ""))

      status(result) mustBe OK
      verify(mockUpscanInitiateConnector, times(1)).getUpscanFormData(any[UpscanInitiateRequest]())(any[HeaderCarrier]())
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())
      templateCaptor.getValue mustEqual "upload-form.njk"
    }

    /*"must show progress of the upload" in {
      val controller = application.injector.instanceOf[UploadFormController]

      forAll(arbitrary[UploadStatus]) {
        uploadStatus =>

          when(mockUploadProgressTracker.getUploadResult(any[UploadId]()))
            .thenReturn(Future.successful(Some(uploadStatus)))
          val templateCaptor = ArgumentCaptor.forClass(classOf[String])
          val result = controller.showResult(UploadId(""))(FakeRequest())

          status(result) mustBe OK
          verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())
          templateCaptor.getValue mustEqual "upload-result.njk"
      }
    }*/

    "must show any returned error" in {
      val controller = application.injector.instanceOf[UploadFormController]

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val argumentCaptor = ArgumentCaptor.forClass(classOf[JsObject])


      val expectedArgument = Json.obj("pageTitle" -> "Upload Error",
        "heading"-> "errorMessage",
        "message" -> s"Code: errorCode, RequestId: errorReqId",
        "config" -> Json.obj("betaFeedbackUnauthenticatedUrl" -> "http://localhost:9250/contact/beta-feedback-unauthenticated",
          "reportAProblemPartialUrl" -> "http://localhost:9250/contact/problem_reports_ajax?service=play26frontend",
          "reportAProblemNonJSUrl" -> "http://localhost:9250/contact/problem_reports_nonjs?service=play26frontend",
          "signOutUrl" -> "http://localhost:9514/feedback/disclose-cross-border-arrangements")
      )
      val result = controller.showError("errorCode", "errorMessage", "errorReqId")(FakeRequest("", ""))

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), argumentCaptor.capture())(any())
      templateCaptor.getValue mustEqual "upload-error.njk"
      argumentCaptor.getValue mustEqual expectedArgument
    }

    "must show File to large error when the errorCode is EntityTooLarge" in {
      val controller = application.injector.instanceOf[UploadFormController]

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val argumentCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val expectedArgument = Json.obj(
        "config" -> Json.obj("betaFeedbackUnauthenticatedUrl" -> "http://localhost:9250/contact/beta-feedback-unauthenticated",
          "reportAProblemPartialUrl" -> "http://localhost:9250/contact/problem_reports_ajax?service=play26frontend",
          "reportAProblemNonJSUrl" -> "http://localhost:9250/contact/problem_reports_nonjs?service=play26frontend",
          "signOutUrl" -> "http://localhost:9514/feedback/disclose-cross-border-arrangements"),
          "guidanceLink" -> "???"
      )

      val result = controller.showError("EntityTooLarge", "Your proposed upload exceeds the maximum allowed size", "errorReqId")(FakeRequest("", ""))

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), argumentCaptor.capture())(any())
      templateCaptor.getValue mustEqual "fileTooLargeError.njk"
      argumentCaptor.getValue mustEqual expectedArgument

    }

    }

}
