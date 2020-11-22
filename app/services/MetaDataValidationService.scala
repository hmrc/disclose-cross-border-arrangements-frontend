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
import models.{Dac6MetaData, GenericError, SubmissionHistory, Validation, ValidationFailure, ValidationSuccess, XMLValidationStatus}
import org.joda.time.DateTime
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}
import scala.xml.Elem

class MetaDataValidationService @Inject()(connector: CrossBorderArrangementsConnector) {

  implicit val localDateOrdering: Ordering[LocalDateTime] = Ordering.by(_.toLocalTime)

  val replaceOrDelete = List("DAC6REP", "DAC6DEL")

  def verifyMetaData(dac6MetaData: Option[Dac6MetaData], enrolmentId: String)
                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[Validation]] = {

    dac6MetaData match {
      case Some(Dac6MetaData("DAC6NEW", _,_, _, _, _)) =>  Future(verifyDisclosureInformation(dac6MetaData.get)).map(
                         status => {
                        val messageRefResult =   verifyMessageRefId(dac6MetaData, enrolmentId)
                               status ++ messageRefResult
                         }

      )

      case Some(Dac6MetaData(_, _,_, _, _, _)) =>

        for {
          history <- connector.getSubmissionHistory(enrolmentId)
          idResult <- verifyIds(dac6MetaData.get, history)
        } yield {

          idResult

        }
      case _ => Future(Seq(Validation("File does not contain necessary data", false)))
    }

  }

  def verifyIds(dac6MetaData: Dac6MetaData, history: SubmissionHistory)
               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[Validation]] = {
    dac6MetaData match {
      case Dac6MetaData("DAC6ADD", Some(arrangementId), None, _, _, _) => verifyDAC6ADD(dac6MetaData, arrangementId, history)
      case Dac6MetaData(instruction, Some(arrangementId), Some(disclosureId), _, _, _) if replaceOrDelete.contains(instruction)  =>
                    Future(verifyReplaceOrDelete(dac6MetaData, arrangementId, disclosureId, history))
      case _ => Future(Seq())

    }
  }

  private def verifyDAC6ADD(dac6MetaData: Dac6MetaData, arrangementId: String, history: SubmissionHistory)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[Validation]] = {

    val disclosureInformationResult = verifyDisclosureInformation(dac6MetaData, Some(history))

    verifyArrangementId(arrangementId, history) map { r1 =>

        val results = (r1, disclosureInformationResult)

        results match {
          case (true, Seq()) => Seq()
          case (false, Seq()) => Seq(Validation("metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords", false))
          case (true,  Seq(errors)) => Seq(errors)
        case (false,  Seq(errors))  => Seq(Validation("metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords", false), errors)

        }
      }

  }

  private def verifyArrangementId(arrangementId: String, history: SubmissionHistory)
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {

    val arrangementIdExists = history.details.exists(submission => submission.arrangementID.contains(arrangementId))
     if(arrangementIdExists) {
       Future(true)
     }else {
       connector.verifyArrangementId(arrangementId)
     }

  }

  private def verifyReplaceOrDelete(dac6MetaData: Dac6MetaData, arrangementId: String, disclosureId: String,
                            history: SubmissionHistory)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Seq[Validation] = {

    val submissionContainingDisclosureId = history.details.find(submission => submission.disclosureID.contains(disclosureId))


    submissionContainingDisclosureId match {
      case Some(submission) => if (submission.arrangementID.contains(arrangementId)) {
        dac6MetaData.importInstruction match {

          case "DAC6REP" if submission.importInstruction.equals("New") && !dac6MetaData.disclosureInformationPresent =>
            Seq(Validation("metaDataRules.disclosureInformation.noInfoWhenReplacingDAC6NEW", false))

          case "DAC6REP" if !isMarketableArrangement(dac6MetaData,history) && !dac6MetaData.disclosureInformationPresent =>
            Seq(Validation("metaDataRules.disclosureInformation.noInfoForNonMaDAC6REP", false))

          case "DAC6REP" if !isMarketableArrangement(dac6MetaData,history) && dac6MetaData.initialDisclosureMA =>
            Seq(Validation("metaDataRules.initialDisclosureMA.arrangementNowMarketable", false))

          case "DAC6REP" if isMarketableArrangement(dac6MetaData,history) && !dac6MetaData.initialDisclosureMA =>
            Seq(Validation("metaDataRules.initialDisclosureMA.arrangementNoLongerMarketable", false))

          case _ => Seq()

        }


      } else Seq(Validation("metaDataRules.disclosureId.disclosureIDDoesNotMatchArrangementID", false))


      case None => Seq(Validation("metaDataRules.disclosureId.disclosureIDDoesNotMatchUser", false))

    }
  }
  private def verifyDisclosureInformation(dac6MetaData: Dac6MetaData, history: Option[SubmissionHistory] = None)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Seq[Validation] = {
   dac6MetaData.importInstruction match {
     case "DAC6NEW" => if(dac6MetaData.disclosureInformationPresent) {
       Seq()

     }else Seq(Validation("metaDataRules.disclosureInformation.disclosureInformationMissingFromDAC6NEW", false))

     case "DAC6ADD" => if(!isMarketableArrangement(dac6MetaData, history.get) && !dac6MetaData.disclosureInformationPresent){
       Seq(Validation("metaDataRules.disclosureInformation.disclosureInformationMissingFromDAC6ADD", false))


     }else Seq()

  }

  }

  private def verifyMessageRefId(dac6MetaData: Option[Dac6MetaData], enrolmentId: String): Seq[Validation] ={
   val result = Try {
     val prefixValid = dac6MetaData.get.messageRefId.startsWith("GB")
     val userId = dac6MetaData.get.messageRefId.substring(2, 17)
     val userIdValid = userId.equals(enrolmentId)
     if(prefixValid && userIdValid && dac6MetaData.get.messageRefId.length > 19){
       None }else
       if(userIdValid){
         Some("metaDataRules.messageRefId.wrongFormat")
       }
       else Some("metaDataRules.messageRefId.noUserId")


    }

    result match {
      case Success(Some(error)) => Seq(Validation(error, false))
      case Success(None) => Seq()
      case _ =>  Seq(Validation("metaDataRules.messageRefId.wrongFormat", false))
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

}
