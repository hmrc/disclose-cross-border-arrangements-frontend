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
import controllers.actions.{ContactRetrievalAction, FakeContactRetrievalAction}
import matchers.JsonMatchers
import models.{ContactDetails, Dac6MetaData, GeneratedIDs, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import pages.{Dac6MetaDataPage, GeneratedIDPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html

import scala.concurrent.Future

class UploadConfirmationControllerSpec extends SpecBase with JsonMatchers {

  "UploadConfirmation Controller" - {

    "return OK and the correct view for a GET" in {

      val metaData = Dac6MetaData("DAC6NEW",
                                  Some("GBA20200701AAAB00"),
                                  Some("GBD20200701AA0001"),
                                  disclosureInformationPresent = true,
                                  initialDisclosureMA = false,
                                  messageRefId = "GB0000000XXX"
      )

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val userAnswers = UserAnswers(userAnswersId)
        .set(Dac6MetaDataPage, metaData)
        .success
        .value
        .set(GeneratedIDPage, GeneratedIDs(Some("GBA20200701AAAB00"), Some("GBD20200701AA0001")))
        .success
        .value

      val fakeDataRetrieval =
        new FakeContactRetrievalAction(userAnswers,
                                       Some(ContactDetails(Some("Test Testing"), Some("test@test.com"), Some("Test Testing"), Some("test@test.com")))
        )

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[ContactRetrievalAction].toInstance(fakeDataRetrieval))
          .build()
      val request        = FakeRequest(GET, routes.UploadConfirmationController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())

      templateCaptor.getValue mustEqual "uploadConfirmation.njk"

      application.stop()
    }

    "thrown an error then display the technical error page if there's no disclosure ID or users go straight to this page" in {
      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val application    = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      val request        = FakeRequest(GET, routes.UploadConfirmationController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])

      val result = route(application, request).value

      an[Exception] mustBe thrownBy {
        status(result) mustEqual OK

        verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())

        templateCaptor.getValue mustEqual "internalServerError.njk"
      }

      application.stop()
    }
  }
}
