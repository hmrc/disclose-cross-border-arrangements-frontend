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
import fixtures.XMLFixture
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.Future

class AuditServiceSpec extends SpecBase with ScalaCheckPropertyChecks {
  val auditConnector: AuditConnector = mock[AuditConnector]

  val application: Application = applicationBuilder(None)
    .overrides(bind[AuditConnector].toInstance(auditConnector))
    .build()

  val auditService: AuditService = application.injector.instanceOf[AuditService]

  "AuditService.submissionAudit" - {
    "must generate correct payload for disclosure submission audit" in {
      val xml = XMLFixture.dac6NotInitialDisclosureMA
      forAll(arbitrary[String], arbitrary[String], arbitrary[Option[String]], arbitrary[Option[String]]) {
        (enrolmentID, fileName, arrangementID, disclosureID) =>
        reset(auditConnector)

        when(auditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        auditService.submissionAudit(enrolmentID, fileName, arrangementID, disclosureID, xml)

        val arrangementAudit = arrangementID.getOrElse("None Provided")
        val disclosureAudit = disclosureID.getOrElse("None Provided")

        val expectedjson = Json.obj(
          "fileName" -> fileName,
          "enrolmentID" -> enrolmentID,
          "arrangementID" -> arrangementAudit,
          "disclosureID" -> disclosureAudit,
          "messageRefID" -> "GB0000000XXX",
          "disclosureImportInstruction" -> "DAC6NEW",
          "initialDisclosureMA" -> "false"
        )

        val eventCaptor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

        verify(auditConnector, times(1)).sendExtendedEvent(eventCaptor.capture())(any(), any())

        eventCaptor.getValue.detail mustBe expectedjson
      }
    }
  }
}