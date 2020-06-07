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
import javax.inject.Inject
import models.Validation

import scala.xml.NodeSeq

class BusinessRuleValidationService @Inject()() {
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

}

object BusinessRuleValidationService {
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  val implementationStartDate: Date = new GregorianCalendar(2018, Calendar.JUNE, 25).getTime

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
          .map(dateFormat.parse)
      }
    })

  val disclosureInformationImplementingDates: ReaderT[Option, NodeSeq, Seq[Date]] =
    ReaderT[Option, NodeSeq, Seq[Date]](xml => {
      Some {
        (xml \\ "ImplementingDate")
          .map(_.text)
          .map(dateFormat.parse)
      }
    })

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


}
