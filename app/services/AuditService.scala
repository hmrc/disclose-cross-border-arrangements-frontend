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

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext

class AuditService @Inject()(auditConnector: AuditConnector)(implicit ex: ExecutionContext) {
  private val auditType = "discloseCrossBorderArrangement"
  private val emptyMap: Map[String, String] = Map.empty

  type MapCont = Map[String, String] => Map[String, String]

  def withFileName(filename: String): MapCont = _ + ("fileName" -> filename)
  def withEnrolmentID(enrolmentId: String): MapCont = _ + ("enrolmentID" -> enrolmentId)

  def submissionAudit(enrolmentId: String, filename: String)(implicit hc: HeaderCarrier): Unit = {

    val auditMap = (
      withFileName(filename) andThen withEnrolmentID(enrolmentId)
    )(emptyMap)

    auditConnector.sendExplicitAudit(auditType, auditMap)
  }
}
