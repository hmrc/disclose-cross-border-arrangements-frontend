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
import scala.xml.{Elem, NodeSeq}

class AuditService @Inject()(auditConnector: AuditConnector)(implicit ex: ExecutionContext) {
  private val auditType = "disclosureXMlSubmission"
  private val emptyMap: Map[String, String] = Map.empty
  val nodeVal: NodeSeq => String = (node: NodeSeq) => if (node.isEmpty) "" else node.text

  type MapCont = Map[String, String] => Map[String, String]

  def withFileName(filename: String): MapCont = _ + ("fileName" -> filename)
  def withEnrolmentID(enrolmentId: String): MapCont = _ + ("enrolmentID" -> enrolmentId)
  def withArrangementID(arrangementID: Option[String]): MapCont = _ + ("arrangementID" -> arrangementID.getOrElse("None Provided"))
  def withDisclosureID(disclosureID: Option[String]): MapCont = _ + ("disclosureID" -> disclosureID.getOrElse("None Provided"))
  def withMessageRefID(xml: Elem): MapCont = _ + ("messageRefID" -> nodeVal(xml \ "Header" \ "MessageRefId"))
  def withImportInstruction(xml: Elem): MapCont = _ + ("disclosureImportInstruction" -> nodeVal(xml \ "DAC6Disclosures" \ "DisclosureImportInstruction"))
  def withInitialDisclosureMA(xml: Elem): MapCont = _ + ("initialDisclosureMA" -> nodeVal(xml \ "DAC6Disclosures" \ "InitialDisclosureMA"))

  def submissionAudit(enrolmentId: String, filename: String, arrangementID: Option[String],
                      disclosureID: Option[String], xml: Elem)(implicit hc: HeaderCarrier): Unit = {

    val auditMap = (
      withFileName(filename) andThen
        withEnrolmentID(enrolmentId) andThen
        withArrangementID(arrangementID) andThen
        withDisclosureID(disclosureID) andThen
        withMessageRefID(xml) andThen
        withImportInstruction(xml) andThen
        withInitialDisclosureMA(xml)
      ) (emptyMap)

    auditConnector.sendExplicitAudit(auditType, auditMap)
  }

} 