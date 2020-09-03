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

import java.text.SimpleDateFormat
import java.util.{Calendar, Date, GregorianCalendar}

import cats.data.ReaderT
import cats.implicits._
import connectors.CrossBorderArrangementsConnector
import javax.inject.Inject
import models.{Dac6MetaData, Validation}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}
import scala.xml.NodeSeq

class BusinessRuleValidationService @Inject()(crossBorderArrangementsConnector: CrossBorderArrangementsConnector) {
  import BusinessRuleValidationService._

  def validateInitialDisclosureHasRelevantTaxPayer(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      isInitialDisclosureMA <- isInitialDisclosureMA
      noOfRelevantTaxPayers <- noOfRelevantTaxPayers
    } yield
      Validation(
        key = "businessrules.initialDisclosure.needRelevantTaxPayer",
        value = if(isInitialDisclosureMA) noOfRelevantTaxPayers > 0 else true
      )
  }

  def validateRelevantTaxpayerDiscloserHasRelevantTaxPayer(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      hasRelevantTaxpayerDiscloser <- hasRelevantTaxpayerDiscloser
      noOfRelevantTaxPayers <- noOfRelevantTaxPayers
    } yield
      Validation(
        key = "businessrules.relevantTaxpayerDiscloser.needRelevantTaxPayer",
        value = if(hasRelevantTaxpayerDiscloser) noOfRelevantTaxPayers > 0 else true
      )
  }

  def validateIntermediaryDiscloserHasIntermediary(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      hasIntermediaryDiscloser <- hasIntermediaryDiscloser
      noOfIntermediaries <- noOfIntermediaries
    } yield
      Validation(
        key = "businessrules.intermediaryDiscloser.needIntermediary",
        value = if(hasIntermediaryDiscloser) noOfIntermediaries > 0 else true
      )
  }

  def validateAllTaxpayerImplementingDatesAreAfterStart(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      taxPayerImplementingDates <- taxPayerImplementingDates
    } yield
      Validation(
        key = "businessrules.taxPayerImplementingDates.needToBeAfterStart",
        value = taxPayerImplementingDates.forall(implDate => !implDate.before(implementationStartDate))
      )
  }

  def validateAllImplementingDatesAreAfterStart(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      disclosureInformationImplementingDates <- disclosureInformationImplementingDates
    } yield
      Validation(
        key = "businessrules.implementingDates.needToBeAfterStart",
        value = disclosureInformationImplementingDates.forall(implDate => !implDate.before(implementationStartDate))
      )
  }

  def validateDisclosureImportInstruction(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      disclosureImportInstruction <- disclosureImportInstruction
      arrangementID <- arrangementID
      disclosureID <- disclosureID
      messageRefID <- messageRefID
    } yield
        disclosureImportInstruction match {
          case "DAC6NEW" => Validation(
            key = "businessrules.newDisclosure.mustNotHaveArrangementIDOrDisclosureID",
            value = arrangementID.isEmpty && disclosureID.isEmpty)
          case "DAC6ADD" => Validation(
            key = "businessrules.addDisclosure.mustHaveArrangementIDButNotDisclosureID",
            value = arrangementID.nonEmpty && disclosureID.isEmpty)
          case "DAC6REP" => Validation(
            key = "businessrules.repDisclosure.mustHaveArrangementIDDisclosureIDAndMessageRefID",
            value = arrangementID.nonEmpty && disclosureID.nonEmpty && messageRefID.nonEmpty)
          case "DAC6DEL" => Validation(
            key = "businessrules.delDisclosure.mustHaveArrangementIDDisclosureIDAndMessageRefID",
            value = arrangementID.nonEmpty && disclosureID.nonEmpty && messageRefID.nonEmpty)
          case _ =>  Validation(
            key = "businessrules.disclosure.notAValidDisclosureInstruction",
            value = false) //TODO: This is because I haven't used an enum
        }
  }

  def validateInitialDisclosureMAWithRelevantTaxPayerHasImplementingDate(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      initialDisclosureMA <- isInitialDisclosureMA
      relevantTaxPayers <- noOfRelevantTaxPayers
      taxPayerImplementingDate <- taxPayerImplementingDates
    } yield Validation(
      key = "businessrules.initialDisclosureMA.allRelevantTaxPayersHaveTaxPayerImplementingDate",
      value = if(initialDisclosureMA && relevantTaxPayers > 0)
                relevantTaxPayers == taxPayerImplementingDate.length
              else true
    )
  }

  def validateTaxPayerImplementingDateAgainstFirstDisclosure()(implicit hc: HeaderCarrier): ReaderT[Option, NodeSeq, Future[Validation]] = {
    for {
      relevantTaxPayers <- noOfRelevantTaxPayers
      taxPayerImplementingDate <- taxPayerImplementingDates
      arrangementID <- arrangementID
    } yield {
      crossBorderArrangementsConnector.retrieveFirstDisclosureForArrangementID(arrangementID).map {
        submissionDetails =>
          Validation(
            key = "businessrules.initialDisclosureMA.allRelevantTaxPayersHaveTaxPayerImplementingDate",
            value = if (submissionDetails.initialDisclosureMA && relevantTaxPayers > 0)
              relevantTaxPayers == taxPayerImplementingDate.length
            else true
          )
      }.recover {
        case _ =>
          Validation(
            key = "businessrules.initialDisclosureMA.allRelevantTaxPayersHaveTaxPayerImplementingDate",
            value = true)
      }
    }
  }

  /* TODO: Delete later
  * If InitialDisclosureMA is false but ArrangementID is present and relates to a previous InitialDisclosure then if
  * RelevantTaxpayer is present TaxpayerImplementingDate is Mandatory.
  * Recognise that the first disclosure for the arrangement had true in InitialDisclosureMA
  *
  * e.g. Need to check arrID and discID is the same in mongo when user submits a DAC6ADD
  * e.g. If user submits a DAC6REP for first disclosure to change value to false then any subsequent disclosures with the
  *      same arrID doesn't necessarily need implementing date. Other business rules will still apply as normal.
  *
  * Possible scenarios:
  * 1. User submits a DAC6NEW (first disclosure) and an arrID and discID are generated. InitialDisclosureMA is true
  * 2a. User submits a DAC6REP for first disclosure. InitialDisclosureMA is false. TaxpayerImplementingDate depends on this now.
  * 2b. User submits a DAC6ADD for first disclosure. InitialDisclosureMA is false but it's true for first disclosure. TaxpayerImplementingDate is mandatory.
  * 3a. No validation error if TaxpayerImplementingDate is missing.
  * 3b. Validation error if TaxpayerImplementingDate is missing.
  *
  * TODO:
  *  - Need to return first disclosure's submission details for arrID
  *  - Need to also check if InitialDisclosureMA value has been changed from true to false
  *  - Return to frontend the correct submission details for that arrID and discID
  *  - OR do all this in the backend and create a new case class for InitialDisclosureMA
  *    - e.g. case class Name(isInitialDisclosureMA: Boolean, updatedInitialDisclosureMA: Boolean)
  * */

  def validateMainBenefitTestHasASpecifiedHallmark(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      mainBenefitTest1 <- hasMainBenefitTest1
      hallmarks <- hallmarks
    } yield Validation(
      key = "businessrules.mainBenefitTest1.oneOfSpecificHallmarksMustBePresent",
      value = if(!mainBenefitTest1)
                hallmarks.toSet.intersect(hallmarksForMainBenefitTest).isEmpty
              else true
    )
  }

  def validateDAC6D1OtherInfoHasNecessaryHallmark(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      hasDAC6D1OtherInfo <- hasDAC6D1OtherInfo
      hallmarks <- hallmarks
    } yield Validation(
      key = "businessrules.dac6D10OtherInfo.needHallMarkToProvideInfo",
      value = if(hasDAC6D1OtherInfo)
        hallmarks.contains("DAC6D1Other")
      else true
    )
  }

  def extractDac6MetaData(): ReaderT[Option, NodeSeq, Dac6MetaData] = {
    for {
      disclosureImportInstruction <- disclosureImportInstruction
      arrangementID <- arrangementID
      disclosureID <- disclosureID
    } yield
      disclosureImportInstruction match {
        case "DAC6NEW" => Dac6MetaData(disclosureImportInstruction)
        case "DAC6ADD" => Dac6MetaData(disclosureImportInstruction, Some(arrangementID))
        case "DAC6REP" => Dac6MetaData(disclosureImportInstruction, Some(arrangementID), Some(disclosureID))
        case "DAC6DEL" => Dac6MetaData(disclosureImportInstruction, Some(arrangementID), Some(disclosureID))
        case _ => throw new RuntimeException("XML Data extraction failed - disclosure import instruction Missing")
      }
  }
  
  def validateFile()(implicit hc: HeaderCarrier): ReaderT[Option, NodeSeq, Future[Seq[Validation]]] = {
    for {
       v1 <- validateInitialDisclosureHasRelevantTaxPayer()
       v2 <- validateRelevantTaxpayerDiscloserHasRelevantTaxPayer()
       v3 <- validateIntermediaryDiscloserHasIntermediary()
       v4 <- validateAllTaxpayerImplementingDatesAreAfterStart()
       v5 <- validateAllImplementingDatesAreAfterStart()
       v6 <- validateDisclosureImportInstruction()
       v7 <- validateInitialDisclosureMAWithRelevantTaxPayerHasImplementingDate()
       v8 <- validateMainBenefitTestHasASpecifiedHallmark()
       v9 <- validateDAC6D1OtherInfoHasNecessaryHallmark()
       v10 <- validateTaxPayerImplementingDateAgainstFirstDisclosure()
    } yield {
      v10.map { v10Validation =>
        Seq(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10Validation).filterNot(_.value)
      }
    }
  }
}

object BusinessRuleValidationService {
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  val implementationStartDate: Date = new GregorianCalendar(2018, Calendar.JUNE, 25).getTime
  val hallmarksForMainBenefitTest: Set[String] = Set("DAC6A1","DAC6A2","DAC6A2b","DAC6A3","DAC6B1","DAC6B2","DAC6B3","DAC6C1bi","DAC6C1c","DAC6C1d")

  val isInitialDisclosureMA: ReaderT[Option, NodeSeq, Boolean] =
    ReaderT[Option, NodeSeq, Boolean](xml => {
      (xml \\ "InitialDisclosureMA").text match {
        case "true" => Some(true)
        case "false" => Some(false)
        case _ => Some(false)
      }
    })

  val hasRelevantTaxpayerDiscloser: ReaderT[Option, NodeSeq, Boolean] =
    ReaderT[Option, NodeSeq, Boolean](xml =>
      Some((xml \\ "RelevantTaxpayerDiscloser").length > 0)
    )

  val hasIntermediaryDiscloser: ReaderT[Option, NodeSeq, Boolean] =
    ReaderT[Option, NodeSeq, Boolean](xml =>
      Some((xml \\ "IntermediaryDiscloser").length > 0)
    )

  val noOfIntermediaries: ReaderT[Option, NodeSeq, Int] =
    ReaderT[Option, NodeSeq, Int](xml =>
      Some((xml \\ "Intermediary").length)
    )

  val noOfRelevantTaxPayers: ReaderT[Option, NodeSeq, Int] =
    ReaderT[Option, NodeSeq, Int](xml => {
      Some((xml \\ "RelevantTaxpayer").length)
    })

  val taxPayerImplementingDates: ReaderT[Option, NodeSeq, Seq[Date]] =
    ReaderT[Option, NodeSeq, Seq[Date]](xml => {
      Some {
        (xml \\ "TaxpayerImplementingDate")
          .map(_.text)
          .map(parseDate _)
      }
    })

  def parseDate(dateString: String): Date = {
    Try(dateFormat.parse(dateString)) match {
      case Success(date) => date
      case _ => new Date()
    }

  }

  val disclosureInformationImplementingDates: ReaderT[Option, NodeSeq, Seq[Date]] = {
  ReaderT[Option, NodeSeq, Seq[Date]](xml => {
      Some {
        (xml \\ "ImplementingDate")
          .map(_.text)
          .map(parseDate _)
      }
    }
    )
  }
  val disclosureImportInstruction: ReaderT[Option, NodeSeq, String] =
    ReaderT[Option, NodeSeq, String](xml => {
      Some((xml \\ "DisclosureImportInstruction").text)
    })

  val disclosureID: ReaderT[Option, NodeSeq, String] =
    ReaderT[Option, NodeSeq, String](xml => {
      Some((xml \\ "DisclosureID").text)
    })

  val arrangementID: ReaderT[Option, NodeSeq, String] =
    ReaderT[Option, NodeSeq, String](xml => {
      Some((xml \\ "ArrangementID").text)
    })

  val messageRefID: ReaderT[Option, NodeSeq, String] =
    ReaderT[Option, NodeSeq, String](xml => {
      Some((xml \\ "MessageRefId").text)
    })

  val hasMainBenefitTest1: ReaderT[Option, NodeSeq, Boolean] =
    ReaderT[Option, NodeSeq, Boolean](xml => {
      (xml \\ "MainBenefitTest1").text match {
        case "true" => Some(true)
        case "false" => Some(false)
        case _ => Some(false)
      }
    })

  val hallmarks: ReaderT[Option, NodeSeq, Seq[String]] =
    ReaderT[Option, NodeSeq, Seq[String]](xml => {
      Some((xml \\ "Hallmark").map(_.text))
    })

  val hasDAC6D1OtherInfo: ReaderT[Option, NodeSeq, Boolean] =
    ReaderT[Option, NodeSeq, Boolean](xml => {
      Some((xml \\ "DAC6D1OtherInfo").length > 0)
    })

}
