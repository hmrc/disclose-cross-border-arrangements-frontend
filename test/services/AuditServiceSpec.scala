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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, times, verify}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._

class AuditServiceSpec extends SpecBase {
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val auditConnector = mock[AuditConnector]
  val auditService = new AuditService(auditConnector)(ec)

  "AuditService.submissionAudit" - {
    "must generate correct payload for disclosure submission audit" in {
      val xml = XMLFixture.dac6NotInitialDisclosureMA
      forAll(arbitrary[String], arbitrary[String], arbitrary[Option[String]], arbitrary[Option[String]])
      { ( enrolmentID, fileName,  arrangementID, disclosureID) =>
        reset(auditConnector)

        auditService.submissionAudit(enrolmentID, fileName, arrangementID, disclosureID, xml)

        val expected = Map(
          "fileName" -> fileName,
          "enrolmentID" -> enrolmentID,
          "arrangementID" -> arrangementID.getOrElse("None Provided"),
          "disclosureID" -> disclosureID.getOrElse("None Provided"),
          "messageRefID" -> "GB0000000XXX",
          "disclosureImportInstruction" ->"DAC6NEW",
          "initialDisclosureMA" -> "false"
        )

        verify(auditConnector, times(1)).sendExplicitAudit(eqTo("disclosureXMlSubmission"),eqTo(expected))(any(),any())
      }
    }
  }
}