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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, times, verify}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._

class AuditServiceSpec extends SpecBase {
  implicit val hc = HeaderCarrier()
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val auditConnector = mock[AuditConnector]
  val auditService = new AuditService(auditConnector)(ec)

  "AuditService.submissionAudit" - {
    "must generate correct payload for disclosure submission audit" in {
      forAll(arbitrary[String], arbitrary[String]) { ( enrolmentID, fileName) =>
        reset(auditConnector)

        auditService.submissionAudit(enrolmentID, fileName)
        val expected = Map(
          "fileName" -> fileName,
          "enrolmentID" -> enrolmentID
        )

        verify(auditConnector, times(1)).sendExplicitAudit(eqTo("discloseCrossBorderArrangement"),eqTo(expected))(any(),any())
      }
    }
  }
}
