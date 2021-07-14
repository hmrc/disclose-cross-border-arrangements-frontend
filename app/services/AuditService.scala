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

package services

import config.FrontendAppConfig
import models.GenericError
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.xml.{Elem, NodeSeq}

class AuditService @Inject() (appConfig: FrontendAppConfig, auditConnector: AuditConnector)(implicit ex: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(getClass)

  private val auditType    = "DisclosureFileSubmission"
  private val noneProvided = "None Provided"

  val nodeVal: NodeSeq => String = (node: NodeSeq) => if (node.isEmpty) "" else node.text

  def submissionAudit(enrolmentId: String, filename: String, arrangementID: Option[String], disclosureID: Option[String], xml: Elem)(implicit
    hc: HeaderCarrier
  ): Unit = {

    val arrangementAudit: String = arrangementID.getOrElse(noneProvided)
    val disclosureAudit: String  = disclosureID.getOrElse(noneProvided)
    val auditMap: JsObject = Json.obj(
      "fileName"                    -> filename,
      "enrolmentID"                 -> enrolmentId,
      "arrangementID"               -> arrangementAudit,
      "disclosureID"                -> disclosureAudit,
      "messageRefID"                -> nodeVal(xml \ "Header" \ "MessageRefId"),
      "disclosureImportInstruction" -> nodeVal(xml \ "DAC6Disclosures" \ "DisclosureImportInstruction"),
      "initialDisclosureMA"         -> nodeVal(xml \ "DAC6Disclosures" \ "InitialDisclosureMA")
    )

    val transactionName: String = "/disclose-cross-border-arrangements/upload/submission"
    val path: String            = "/disclose-cross-border-arrangements/upload/submission"

    auditConnector.sendExtendedEvent(
      ExtendedDataEvent(
        auditSource = appConfig.appName,
        auditType = auditType,
        detail = auditMap,
        tags = AuditExtensions.auditHeaderCarrier(hc).toAuditDetails()
          ++ AuditExtensions.auditHeaderCarrier(hc).toAuditTags(transactionName, path)
      )
    ) map {
      ar: AuditResult =>
        ar match {
          case Failure(msg, ex) =>
            ex match {
              case Some(throwable) =>
                logger.warn(s"The attempt to issue audit event $auditType failed with message : $msg", throwable)
              case None =>
                logger.warn(s"The attempt to issue audit event $auditType failed with message : $msg")
            }
            ar
          case Disabled =>
            logger.warn(s"The attempt to issue audit event $auditType was unsuccessful, as auditing is currently disabled in config"); ar
          case _ => logger.debug(s"Audit event $auditType issued successfully."); ar
        }
    }
  }

  def auditErrorMessage(error: GenericError)(implicit hc: HeaderCarrier): Unit = {

    val errorMessageType = "ErrorMessage"

    val auditMap: JsObject = Json.obj("errorMessage" -> error.messageKey)

    if (appConfig.validationAuditToggle) {
      auditConnector.sendExtendedEvent(
        ExtendedDataEvent(
          auditSource = appConfig.appName,
          auditType = errorMessageType,
          detail = auditMap,
          tags = AuditExtensions.auditHeaderCarrier(hc).toAuditDetails()
        )
      ) map {
        ar: AuditResult =>
          ar match {
            case Failure(msg, ex) =>
              ex match {
                case Some(throwable) =>
                  logger.warn(s"The attempt to issue audit event $errorMessageType failed with message : $msg", throwable)
                case None =>
                  logger.warn(s"The attempt to issue audit event $errorMessageType failed with message : $msg")
              }
              ar
            case Disabled =>
              logger.warn(s"The attempt to issue audit event $errorMessageType was unsuccessful, as auditing is currently disabled in config"); ar
            case _ => logger.debug(s"Audit event $errorMessageType issued successfully."); ar
          }
      }
    } else {
      logger.warn(s"Validation has failed and auditing currently disabled for this event type")
    }
  }
}
