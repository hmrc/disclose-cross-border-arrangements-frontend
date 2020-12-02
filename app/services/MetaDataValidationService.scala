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

import java.time.{LocalDate, LocalDateTime}

import connectors.CrossBorderArrangementsConnector
import javax.inject.Inject
import models.{Dac6MetaData, GenericError, SubmissionHistory, ValidationFailure, ValidationSuccess, XMLValidationStatus}
import org.joda.time.DateTime
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class MetaDataValidationService @Inject()(connector: CrossBorderArrangementsConnector) {

  implicit val localDateOrdering: Ordering[LocalDateTime] = Ordering.by(_.toLocalTime)

  val replaceOrDelete = List("DAC6REP", "DAC6DEL")
  def verifyMetaData(source: String, elem: Elem, dac6MetaData: Option[Dac6MetaData], enrolmentId: String)
                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[XMLValidationStatus] = {

    dac6MetaData match {
      case Some(metaData) =>

        for {
          history <- connector.getSubmissionHistory(enrolmentId)
          idResult <- verifyIds(source, elem, metaData, history)
        } yield {

          idResult match {
            case ValidationFailure(errors) => ValidationFailure(errors )
            case ValidationSuccess(_, _) => ValidationSuccess(source, dac6MetaData)
          }

        }
      case _ => Future(ValidationFailure(List(GenericError(0, "File does not contain necessary data"))))
    }

  }

  def verifyIds(source: String, elem: Elem, dac6MetaData: Dac6MetaData, history: SubmissionHistory)
               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[XMLValidationStatus] = {
    dac6MetaData match {
      case Dac6MetaData("DAC6ADD", Some(arrangementId), None, _) => verifyDAC6ADD(source, elem, dac6MetaData, arrangementId, history)
      case Dac6MetaData(instruction, Some(arrangementId), Some(disclosureId), _) if replaceOrDelete.contains(instruction) =>
        Future(verifyReplaceOrDelete(source, elem, dac6MetaData, arrangementId, disclosureId, history))
      case _ => Future(ValidationSuccess(source, Some(dac6MetaData)))

    }


  }

  private def verifyDAC6ADD(source: String, elem: Elem, dac6MetaData: Dac6MetaData, arrangementId: String, history: SubmissionHistory)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[XMLValidationStatus] = {
    val arrangementIdExists = history.details.exists(submission => submission.arrangementID.contains(arrangementId))

    if (arrangementIdExists) {
      Future(ValidationSuccess(source, Some(dac6MetaData)))
    } else {
      connector.verifyArrangementId(arrangementId) map {
        case true => ValidationSuccess(source, Some(dac6MetaData))
        case false =>
          ValidationFailure(List(GenericError(getLineNumber(elem, "ArrangementID"), "ArrangementID does not match HMRC's records")))
      }
    }
  }

  private def verifyReplaceOrDelete(source: String, elem: Elem, dac6MetaData: Dac6MetaData, arrangementId: String, disclosureId: String,
                            history: SubmissionHistory)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): XMLValidationStatus = {
    val submissionContainingDisclosureId = history.details.find(submission => submission.disclosureID.contains(disclosureId))
    submissionContainingDisclosureId match {
      case Some(submission) => if (submission.arrangementID.contains(arrangementId)) {
        ValidationSuccess(source, Some(dac6MetaData))
      } else ValidationFailure(List(GenericError(getLineNumber(elem, "DisclosureID"), "DisclosureID does not match the ArrangementID provided")))


      case None => ValidationFailure(List(GenericError(getLineNumber(elem, "DisclosureID"), "DisclosureID has not been generated by this individual or organisation")))

    }

  }

  private def isMarketableArrangement(dac6MetaData: Dac6MetaData, history: SubmissionHistory): Boolean = {
    val relevantArrangement = history.details.filter(submission => submission.arrangementID.equals(dac6MetaData.arrangementID))
    val firstDisclosureId = relevantArrangement.find(submissionDetails => submissionDetails.importInstruction.equals("New")) match {
      case Some(details) =>
        details.disclosureID

      case _ => None
    }
    val filterOnDisclosureId = history.details.filter(submissionDetails => submissionDetails.disclosureID.equals(firstDisclosureId))
    filterOnDisclosureId.sortBy(_.submissionTime).lastOption match {
      case Some(submission) => submission.initialDisclosureMA
      case _ => false
    }

  }

  private def getLineNumber(xml: Elem, path: String): Int = {
    val xmlArray = xml.toString().split("\n")

    xmlArray.indexWhere(str => str.contains(path)) + 1
  }


}
