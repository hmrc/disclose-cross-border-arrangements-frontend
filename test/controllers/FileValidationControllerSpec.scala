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
import connectors.{UpscanConnector, ValidationConnector}
import helpers.{FakeUpscanConnector, XmlLoadHelper}
import models.upscan.{Reference, UploadId, UploadSessionDetails, UploadedSuccessfully}
import models.{Dac6MetaData, GenericError, UserAnswers}
import org.bson.types.ObjectId
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import pages.UploadIDPage
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.AuditService

import scala.concurrent.{ExecutionContextExecutor, Future}

class FileValidationControllerSpec extends SpecBase with BeforeAndAfterEach {

  val mockXmlLoadHelper       = mock[XmlLoadHelper]
  val mockSessionRepository   = mock[SessionRepository]
  val mockValidationConnector = mock[ValidationConnector]
  val mockAuditService        = mock[AuditService]

  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  override def beforeEach: Unit =
    reset(mockSessionRepository, mockAuditService)

  val fakeUpscanConnector = app.injector.instanceOf[FakeUpscanConnector]

  "FileValidationController" - {
    val uploadId    = UploadId("123")
    val userAnswers = UserAnswers(userAnswersId).set(UploadIDPage, uploadId).success.value
    val application = applicationBuilder(userAnswers = Some(userAnswers))
      .overrides(
        bind[UpscanConnector].toInstance(fakeUpscanConnector),
        bind[SessionRepository].toInstance(mockSessionRepository),
        bind[XmlLoadHelper].toInstance(mockXmlLoadHelper),
        bind[ValidationConnector].toInstance(mockValidationConnector),
        bind[AuditService].toInstance(mockAuditService)
      )
      .build()

    val downloadURL = "http://dummy-url.com"
    val uploadDetails = UploadSessionDetails(
      new ObjectId(),
      UploadId("123"),
      Reference("123"),
      UploadedSuccessfully("afile", downloadURL)
    )

    "must redirect to Check your answers and present the correct view for a GET" in {

      val metaData          = Dac6MetaData("DAC6NEW", disclosureInformationPresent = true, initialDisclosureMA = false, messageRefId = "GB0000000XXX")
      val userAnswersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
      val expectedData      = Json.obj("validXML" -> "afile", "dac6MetaData" -> metaData, "url" -> downloadURL)

      when(mockValidationConnector.sendForValidation(any())(any(), any())).thenReturn(Future.successful(Some(Right(metaData))))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      fakeUpscanConnector.setDetails(uploadDetails)

      val request                = FakeRequest(GET, routes.FileValidationController.onPageLoad().url)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad().url

      verify(mockSessionRepository, times(1)).set(userAnswersCaptor.capture())
      userAnswersCaptor.getValue.data mustEqual expectedData
    }

    "must redirect to Delete Disclosure Summary when import instruction is 'DAC6DEL' and present the correct view for a GET" in {

      val metaData = Dac6MetaData("DAC6DEL",
                                  Some("GBA20200601AAA000"),
                                  Some("GBD20200601AAA000"),
                                  disclosureInformationPresent = true,
                                  initialDisclosureMA = false,
                                  messageRefId = "GB0000000XXX"
      )
      val userAnswersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
      val expectedData      = Json.obj("validXML" -> "afile", "dac6MetaData" -> metaData, "url" -> downloadURL)

      fakeUpscanConnector.setDetails(uploadDetails)
      when(mockValidationConnector.sendForValidation(any())(any(), any())).thenReturn(Future.successful(Some(Right(metaData))))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val controller             = application.injector.instanceOf[FileValidationController]
      val result: Future[Result] = controller.onPageLoad()(FakeRequest("", ""))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustEqual routes.DeleteDisclosureSummaryController.onPageLoad().url

      verify(mockSessionRepository, times(1)).set(userAnswersCaptor.capture())
      userAnswersCaptor.getValue.data mustEqual expectedData
    }

    "must redirect to invalid XML page if XML validation fails" in {

      val errors: Seq[GenericError] = Seq(GenericError(1, "error"))
      val userAnswersCaptor         = ArgumentCaptor.forClass(classOf[UserAnswers])
      val expectedData              = Json.obj("invalidXML" -> "afile", "errors" -> errors)

      fakeUpscanConnector.setDetails(uploadDetails)

      when(mockValidationConnector.sendForValidation(any())(any(), any())).thenReturn(Future.successful(Some(Left(errors))))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val controller             = application.injector.instanceOf[FileValidationController]
      val result: Future[Result] = controller.onPageLoad()(FakeRequest("", ""))

      status(result) mustBe SEE_OTHER
      verify(mockSessionRepository, times(1)).set(userAnswersCaptor.capture())
      verify(mockAuditService, times(1)).auditErrorMessage(any())(any())

      userAnswersCaptor.getValue.data mustEqual expectedData
    }

    "must redirect to file error page if XML parser fails" in {

      val errors: Seq[GenericError] = Seq(GenericError(1, "error"))
      val userAnswersCaptor         = ArgumentCaptor.forClass(classOf[UserAnswers])
      val expectedData              = Json.obj("invalidXML" -> "afile", "errors" -> errors)

      fakeUpscanConnector.setDetails(uploadDetails)
      //noinspection ScalaStyle

      when(mockValidationConnector.sendForValidation(any())(any(), any())).thenReturn(Future.successful(Some(Left(errors))))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val controller             = application.injector.instanceOf[FileValidationController]
      val result: Future[Result] = controller.onPageLoad()(FakeRequest("", ""))

      status(result) mustBe SEE_OTHER
      verify(mockSessionRepository, times(1)).set(userAnswersCaptor.capture())
      userAnswersCaptor.getValue.data mustEqual expectedData
    }

    "must return an Exception when a valid UploadId cannot be found" in {

      val controller             = application.injector.instanceOf[FileValidationController]
      val result: Future[Result] = controller.onPageLoad()(FakeRequest("", ""))

      a[RuntimeException] mustBe thrownBy(status(result))
    }

    "must return an Exception when meta data cannot be found" in {

      fakeUpscanConnector.setDetails(uploadDetails)
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val controller             = application.injector.instanceOf[FileValidationController]
      val result: Future[Result] = controller.onPageLoad()(FakeRequest("", ""))

      a[RuntimeException] mustBe thrownBy {
        status(result) mustEqual OK
      }
    }
  }
}
