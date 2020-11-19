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
      case Some(Dac6MetaData("DAC6NEW", _,_, _, _)) =>  Future(verifyDisclosureInformation(source, elem, dac6MetaData.get))

      case Some(Dac6MetaData(_, _,_, _, _)) =>

        for {
          history <- connector.getSubmissionHistory(enrolmentId)
          idResult <- verifyIds(source, elem, dac6MetaData.get, history)
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
      case Dac6MetaData("DAC6ADD", Some(arrangementId), None, _, _) => verifyDAC6ADD(source, elem, dac6MetaData, arrangementId, history)
      case Dac6MetaData(instruction, Some(arrangementId), Some(disclosureId), _, _) if replaceOrDelete.contains(instruction)  => Future(verifyReplaceOrDelete(source, elem, dac6MetaData, arrangementId, disclosureId, history))
      case _ => Future(ValidationSuccess(source, Some(dac6MetaData)))

    }


  }

  private def verifyDAC6ADD(source: String, elem: Elem, dac6MetaData: Dac6MetaData, arrangementId: String, history: SubmissionHistory)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[XMLValidationStatus] = {

    val disclosureInformationResult = verifyDisclosureInformation(source, elem, dac6MetaData, Some(history))

    verifyArrangementId(dac6MetaData, arrangementId, history) map { r1 =>

        val results = (r1, disclosureInformationResult)

        results match {
          case (true, ValidationSuccess(_, _)) => ValidationSuccess(source, Some(dac6MetaData))
          case (false, ValidationSuccess(_, _)) => ValidationFailure(List(GenericError(getLineNumber(elem, "ArrangementID"), "ArrangementID does not match HMRC's records")))
          case (true,  ValidationFailure(errors)) => ValidationFailure(errors)
        case (false,  ValidationFailure(errors)) => val combinedErrors = GenericError(getLineNumber(elem, "ArrangementID"), "ArrangementID does not match HMRC's records") :: errors.toList
            ValidationFailure(combinedErrors)

        }
      }

  }

  private def verifyArrangementId( dac6MetaData: Dac6MetaData, arrangementId: String, history: SubmissionHistory)
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {

    val arrangementIdExists = history.details.exists(submission => submission.arrangementID.contains(arrangementId))
     if(arrangementIdExists) {
       Future(true)
     }else {
       connector.verifyArrangementId(arrangementId)
     }

  }

  private def verifyReplaceOrDelete(source: String, elem: Elem, dac6MetaData: Dac6MetaData, arrangementId: String, disclosureId: String,
                            history: SubmissionHistory)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): XMLValidationStatus = {

    val submissionContainingDisclosureId = history.details.find(submission => submission.disclosureID.contains(disclosureId))


    submissionContainingDisclosureId match {
      case Some(submission) => if (submission.arrangementID.contains(arrangementId)) {
        dac6MetaData.importInstruction match {

          case "DAC6REP" if submission.importInstruction.equals("New") && !dac6MetaData.disclosureInformationPresent =>
            ValidationFailure(List(GenericError(getLineNumber(elem, "DisclosureImportInstruction"), "Provide DisclosureInformation in this DAC6REP file, to replace the original arrangement details")))

          case "DAC6REP" if !isMarketableArrangement(dac6MetaData,history) && !dac6MetaData.disclosureInformationPresent =>
            ValidationFailure(List(GenericError(getLineNumber(elem, "DisclosureImportInstruction"), "Provide DisclosureInformation in this DAC6REP file. This is a mandatory field for arrangements that are not marketable")))

          case "DAC6REP" if !isMarketableArrangement(dac6MetaData,history) && dac6MetaData.initialDisclosureMA =>
            ValidationFailure(List(GenericError(getLineNumber(elem, "InitialDisclosureMA"), "Change the InitialDisclosureMA to match the original declaration. If the arrangement has since become marketable, you will need to make a new report")))

          case "DAC6REP" if isMarketableArrangement(dac6MetaData,history) && !dac6MetaData.initialDisclosureMA =>
            ValidationFailure(List(GenericError(getLineNumber(elem, "InitialDisclosureMA"), "Change the InitialDisclosureMA to match the original declaration. If the arrangement is no longer marketable, you will need to make a new report")))

          case _ => ValidationSuccess(source, Some(dac6MetaData))

        }


      } else ValidationFailure(List(GenericError(getLineNumber(elem, "DisclosureID"), "DisclosureID does not match the ArrangementID provided")))


      case None => ValidationFailure(List(GenericError(getLineNumber(elem, "DisclosureID"), "DisclosureID has not been generated by this individual or organisation")))

    }
  }
  private def verifyDisclosureInformation(source: String, elem: Elem, dac6MetaData: Dac6MetaData, history: Option[SubmissionHistory] = None)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext): XMLValidationStatus = {
   dac6MetaData.importInstruction match {
     case "DAC6NEW" => if(dac6MetaData.disclosureInformationPresent) {
       ValidationSuccess(source, Some(dac6MetaData))

     }else ValidationFailure(List(GenericError(getLineNumber(elem, "DisclosureImportInstruction"), "Provide DisclosureInformation in this DAC6NEW file")))

     case "DAC6ADD" => if(!isMarketableArrangement(dac6MetaData, history.get) && !dac6MetaData.disclosureInformationPresent){
       ValidationFailure(List(GenericError(getLineNumber(elem, "DisclosureImportInstruction"), "Provide DisclosureInformation in this DAC6ADD file. This is a mandatory field for arrangements that are not marketable")))


     }else        ValidationSuccess(source, Some(dac6MetaData))

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
