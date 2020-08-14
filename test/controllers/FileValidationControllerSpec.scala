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
import models.upscan.{Reference, UploadId, UploadSessionDetails, UploadedSuccessfully}
import models.{Dac6MetaData, GenericError, UserAnswers, ValidationFailure, ValidationSuccess}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.UploadIDPage
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.bson.BSONObjectID
import repositories.{SessionRepository, UploadSessionRepository}
import services.{ValidationEngine, XMLValidationService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class FileValidationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  val mockValidationEngine = mock[ValidationEngine]
  val mockXmlValidationService =  mock[XMLValidationService]
  val mockRepository = mock[UploadSessionRepository]
  val mockSessionRepository = mock[SessionRepository]

  implicit val hc = HeaderCarrier()
  implicit val ec = scala.concurrent.ExecutionContext.global

  override def beforeEach() = {
    reset(mockSessionRepository)
  }

  "FileValidationController" - {
    val uploadId = UploadId("123")
    val userAnswers = UserAnswers(userAnswersId).set(UploadIDPage, uploadId).success.value
    val application = applicationBuilder(userAnswers = Some(userAnswers))
      .overrides(
        bind[UploadSessionRepository].toInstance(mockRepository),
        bind[SessionRepository].toInstance(mockSessionRepository),
        bind[ValidationEngine].toInstance(mockValidationEngine),
        bind[XMLValidationService].toInstance(mockXmlValidationService)
      )
      .build()

    val downloadURL = "http://dummy-url.com"
    val uploadDetails = UploadSessionDetails(
      BSONObjectID.generate(),
      UploadId("123"),
      Reference("123"),
      UploadedSuccessfully("afile",downloadURL)
    )

    "must redirect to Check your answers and present the correct view for a GET" in {

      val uploadId = UploadId("123")
      val metaData = Dac6MetaData("DAC6NEW", doAllRelevantTaxpayersHaveImplementingDate = true)
      val userAnswersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
      val expectedData = Json.obj("validXML"-> "afile", "dac6MetaData" -> metaData, "url" -> downloadURL)

      when(mockRepository.findByUploadId(uploadId)).thenReturn(Future.successful(Some(uploadDetails)))
      when(mockValidationEngine.validateFile(any(), any(), any())(any(), any())).thenReturn(Future.successful(ValidationSuccess(downloadURL, Some(metaData))))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val controller = application.injector.instanceOf[FileValidationController]
      val result: Future[Result] = controller.onPageLoad()(FakeRequest("", ""))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad().url

      verify(mockSessionRepository, times(1)).set(userAnswersCaptor.capture())
      userAnswersCaptor.getValue.data mustEqual expectedData
    }


    "must redirect to Delete Disclosure Summary when import instruction is 'DAC6DEL' and present the correct view for a GET" in {

      val uploadId = UploadId("123")
      val metaData = Dac6MetaData("DAC6DEL", Some("GBA20200601AAA000"), Some("GBD20200601AAA000"),
                                  doAllRelevantTaxpayersHaveImplementingDate = true)
      val userAnswersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
      val expectedData = Json.obj("validXML"-> "afile","dac6MetaData" -> metaData, "url" -> downloadURL)

      when(mockRepository.findByUploadId(uploadId)).thenReturn(Future.successful(Some(uploadDetails)))
      when(mockValidationEngine.validateFile(any(), any(), any())(any(), any())).thenReturn(Future.successful(ValidationSuccess(downloadURL, Some(metaData))))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val controller = application.injector.instanceOf[FileValidationController]
      val result: Future[Result] = controller.onPageLoad()(FakeRequest("", ""))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustEqual routes.DeleteDisclosureSummaryController.onPageLoad().url

      verify(mockSessionRepository, times(1)).set(userAnswersCaptor.capture())
      userAnswersCaptor.getValue.data mustEqual expectedData
    }

    "must redirect to invalid XML page if XML validation fails" in {

      val uploadId = UploadId("123")
      val errors: Seq[GenericError] = Seq(GenericError(1, "error"))
      val userAnswersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
      val expectedData = Json.obj("invalidXML"-> "afile", "error" -> errors)

      when(mockRepository.findByUploadId(uploadId)).thenReturn(Future.successful(Some(uploadDetails)))
      when(mockValidationEngine.validateFile(any(), any(), any())(any(), any())).thenReturn(Future.successful(ValidationFailure(errors)))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val controller = application.injector.instanceOf[FileValidationController]
      val result: Future[Result] = controller.onPageLoad()(FakeRequest("", ""))

      status(result) mustBe SEE_OTHER
      verify(mockSessionRepository, times(1)).set(userAnswersCaptor.capture())
      userAnswersCaptor.getValue.data mustEqual expectedData
    }

    "must return an Exception when a valid UploadId cannot be found" in {

      val uploadId = UploadId("123")

      when(mockRepository.findByUploadId(uploadId)).thenReturn(Future.successful(None))

      val controller = application.injector.instanceOf[FileValidationController]

      val result: Future[Result] = controller.onPageLoad()(FakeRequest("", ""))

      a[RuntimeException] mustBe thrownBy(status(result))
    }

    "must return an Exception when meta data cannot be found" in {

      val uploadId = UploadId("123")

      when(mockRepository.findByUploadId(uploadId)).thenReturn(Future.successful(Some(uploadDetails)))
      when(mockValidationEngine.validateFile(any(), any(), any())(any(), any())).thenReturn(Future.successful(ValidationSuccess(downloadURL, None)))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val controller = application.injector.instanceOf[FileValidationController]
      val result: Future[Result] = controller.onPageLoad()(FakeRequest("", ""))

      a[RuntimeException] mustBe thrownBy {
        status(result) mustEqual OK
      }
    }
  }
}
