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

package services

import base.SpecBase
import connectors.EmailConnector
import generators.Generators
import models.{ContactDetails, GeneratedIDs}
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.OK
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class EmailServiceSpec extends SpecBase with MockitoSugar
  with Generators
  with ScalaCheckPropertyChecks {

  override def beforeEach: Unit =
    reset(
      mockEmailConnector
    )

  val mockEmailConnector: EmailConnector = mock[EmailConnector]
  val emailService: EmailService = injector.instanceOf[EmailService]

  override lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(
      bind[EmailConnector].toInstance(mockEmailConnector)
    )
    .build()

  "Email Service" - {
    "must submit to the email connector when 1 set of contact details are provided" in {
      val contactDetails = ContactDetails(Some("Test Testing"), Some("test@test.com"), None, None)
      val filename = "test.xml"
      val ids = GeneratedIDs(Some("123"),Some("345"))
      when(mockEmailConnector.sendEmail(any())(any()))
        .thenReturn(
          Future.successful(HttpResponse(OK, ""))
        )
      val result = emailService.sendEmail(Some(contactDetails), filename, ids)

      whenReady(result) { result =>
        result.map(_.status) mustBe Some(OK)

        verify(mockEmailConnector, times(1)).sendEmail(any())(any())
      }
    }

    "must submit to the email connector twice when 2 sets of valid details provided" in {
      val contactDetails = ContactDetails(Some("Test Testing"), Some("test@test.com"), Some("Test Testing"), Some("test@test.com"))
      val filename = "test.xml"
      val ids = GeneratedIDs(Some("123"),Some("345"))

      when(mockEmailConnector.sendEmail(any())(any()))
        .thenReturn(
          Future.successful(HttpResponse(OK, ""))
        )

      val result = emailService.sendEmail(Some(contactDetails), filename, ids)

      whenReady(result) { result =>
        result.map(_.status) mustBe Some(OK)

        verify(mockEmailConnector, times(2)).sendEmail(any())(any())
      }
    }

    "must fail to submit to the email connector when invalid email address provided" in {
      val contactDetails = ContactDetails(Some("Test Testing"), Some("test"), None, None)
      val filename = "test.xml"
      val ids = GeneratedIDs(Some("123"),Some("345"))

      when(mockEmailConnector.sendEmail(any())(any()))
        .thenReturn(
          Future.successful(HttpResponse(OK, ""))
        )

      val result = emailService.sendEmail(Some(contactDetails), filename, ids)

      whenReady(result) { result =>
        result.map(_.status) mustBe None
      }
    }
  }
}
