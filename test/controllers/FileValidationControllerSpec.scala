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
import models.{UserAnswers, ValidationFailure, ValidationSuccess}
import models.upscan.{Reference, UploadId, UploadSessionDetails, UploadedSuccessfully}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import reactivemongo.bson.BSONObjectID
import repositories.{SessionRepository, UploadSessionRepository}
import services.XMLValidationService
import org.mockito.Mockito._

import scala.concurrent.Future

class FileValidationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  val mockFileValidation = mock[XMLValidationService]
  val mockRepository = mock[UploadSessionRepository]
  val mockSessionRepository = mock[SessionRepository]


  override def beforeEach() = {
    reset(mockSessionRepository)
  }

  "FileValidationController" - {

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .overrides(
        bind[XMLValidationService].toInstance(mockFileValidation),
        bind[UploadSessionRepository].toInstance(mockRepository),
        bind[SessionRepository].toInstance(mockSessionRepository)
      )
      .build()

    val downloadURL = "http://dummy-url.com"
    val uploadDetails = UploadSessionDetails(
      BSONObjectID.generate(),
      UploadId("123"),
      Reference("123"),
      UploadedSuccessfully("afile",downloadURL)
    )

    "must return OK and the correct view for a GET" in {

      val uploadId = UploadId("123")

      when(mockRepository.findByUploadId(uploadId)).thenReturn(Future.successful(Some(uploadDetails)))
      when(mockFileValidation.validateXML(org.mockito.Matchers.anyString())).thenReturn(ValidationSuccess(downloadURL))
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])

      val controller = application.injector.instanceOf[FileValidationController]
      val result: Future[Result] = controller.onPageLoad(uploadId)(FakeRequest("", ""))

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())
      templateCaptor.getValue mustEqual "file-validation.njk"
    }

    "must redirect to invalid XML page if XML validation fails" in {

      val uploadId = UploadId("123")
      val userAnswersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
      val expectedData = Json.obj("invalidXML"-> "afile")

      when(mockRepository.findByUploadId(uploadId)).thenReturn(Future.successful(Some(uploadDetails)))
      when(mockFileValidation.validateXML(org.mockito.Matchers.anyString())).thenReturn(ValidationFailure(List()))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val controller = application.injector.instanceOf[FileValidationController]
      val result: Future[Result] = controller.onPageLoad(uploadId)(FakeRequest("", ""))

      status(result) mustBe SEE_OTHER
      verify(mockSessionRepository, times(1)).set(userAnswersCaptor.capture())
      userAnswersCaptor.getValue.data mustEqual expectedData
    }

    "must return an Exception when a valid UploadId cannot be found" in {

      val uploadId = UploadId("123")

      when(mockRepository.findByUploadId(uploadId)).thenReturn(Future.successful(None))
      when(mockFileValidation.validateXML(org.mockito.Matchers.anyString())).thenReturn(ValidationFailure(List()))

      val controller = application.injector.instanceOf[FileValidationController]

      val result: Future[Result] = controller.onPageLoad(uploadId)(FakeRequest("", ""))

      a[RuntimeException] mustBe thrownBy(status(result))
    }

  }
}
