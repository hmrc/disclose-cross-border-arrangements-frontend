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

package models

case class Validation(key: String, value: Boolean, lineNumber: Option[Int] = None) {

  def toGenericError: GenericError = GenericError(lineNumber.getOrElse(0), getErrorMessage)

  def setLineNumber(xmlArray: Array[String]): Validation ={
    val index = xmlArray.indexWhere(str => str.contains(path)) + 1
    copy(lineNumber = Some(index))
  }


   def getErrorMessage: String ={
     key match {
       case "businessrules.initialDisclosure.needRelevantTaxPayer" => "InitialDisclosureMA is false so there should be a RelevantTaxpayer"
       case "businessrules.dac6D10OtherInfo.needHallMarkToProvideInfo" => "DAC6D1OtherInfo has been provided but hallmark DAC6D1Other has not been selected"
       case "businessrules.relevantTaxpayerDiscloser.needRelevantTaxPayer" => "RelevantTaxpayerDiscloser has been provided so there must be at least one RelevantTaxpayer"
       case "businessrules.intermediaryDiscloser.needIntermediary" => "IntermediaryDiscloser has been provided so there must be at least one Intermediary"
       case "businessrules.taxPayerImplementingDates.needToBeAfterStart" => "The TaxpayerImplementingDate when the arrangement has been or will be made available to each taxpayer must be on or after 25 June 2018"
       case "businessrules.initialDisclosureMA.allRelevantTaxPayersHaveTaxPayerImplementingDate" => "InitialDisclosureMA is true and there are RelevantTaxpayers so each RelevantTaxpayer must have a TaxpayerImplementingDate"
       case "businessrules.mainBenefitTest1.oneOfSpecificHallmarksMustBePresent" => "MainBenefitTest1 is false but the hallmarks A, B, C1bi and/or C1d have been selected"
       case "businessrules.implementingDates.needToBeAfterStart" => "The DisclosureInformation/ImplementingDate on which the first step in the implementation of the reportable cross-border arrangement has been made or will be made must be on or after 25 June 2018"
       case "businessrules.addDisclosure.mustHaveArrangementIDButNotDisclosureID" => "DisclosureImportInstruction is DAC6ADD so there should be an ArrangementID and no DisclosureID"
       case "businessrules.newDisclosure.mustNotHaveArrangementIDOrDisclosureID" => "DisclosureImportInstruction is DAC6NEW so there should be no ArrangementID or DisclosureID"
       case "businessrules.repDisclosure.mustHaveArrangementIDDisclosureIDAndMessageRefID" => "DisclosureImportInstruction is DAC6REP so there should be an ArrangementID and a DisclosureID"
       case "businessrules.delDisclosure.mustHaveArrangementIDDisclosureIDAndMessageRefID" => "DisclosureImportInstruction is DAC6DEL so there should be an ArrangementID and a DisclosureID"
       case _ => "There is a problem with this line number"

     }
  }



//  disclosureImportInstruction match {
//    case "DAC6NEW" => Validation(
//      key = "businessrules.newDisclosure.mustNotHaveArrangementIDOrDisclosureID",
//      value = arrangementID.isEmpty && disclosureID.isEmpty)
//    case "DAC6ADD" => Validation(
//      key = "businessrules.addDisclosure.mustHaveArrangementIDButNotDisclosureID",
//      value = arrangementID.nonEmpty && disclosureID.isEmpty)
//    case "DAC6REP" => Validation(
//      key = "businessrules.repDisclosure.mustHaveArrangementIDDisclosureIDAndMessageRefID",
//      value = arrangementID.nonEmpty && disclosureID.nonEmpty && messageRefID.nonEmpty)
//    case "DAC6DEL" => Validation(
//      key = "businessrules.delDisclosure.mustHaveArrangementIDDisclosureIDAndMessageRefID",
//      value = arrangementID.nonEmpty && disclosureID.nonEmpty && messageRefID.nonEmpty)
//    case _ =>  Validation(
//      key = "businessrules.disclosure.notAValidDisclosureInstruction",
//      value = false) //TODO: This is because I haven't used an enum
//  }

  def path: String = {

    key match {
      case "businessrules.initialDisclosure.needRelevantTaxPayer" => "InitialDisclosureMA"
      case "businessrules.relevantTaxpayerDiscloser.needRelevantTaxPayer" => "RelevantTaxpayerDiscloser"
      case "businessrules.intermediaryDiscloser.needIntermediary" => "IntermediaryDiscloser"
      case "businessrules.taxPayerImplementingDates.needToBeAfterStart" => "TaxpayerImplementingDate"
      case "businessrules.implementingDates.needToBeAfterStart" => "ImplementingDate"
      case "businessrules.initialDisclosureMA.allRelevantTaxPayersHaveTaxPayerImplementingDate" => "InitialDisclosureMA"
      case "businessrules.mainBenefitTest1.oneOfSpecificHallmarksMustBePresent" => "MainBenefitTest1"
      case "businessrules.dac6D10OtherInfo.needHallMarkToProvideInfo" => "DAC6D1OtherInfo"
      case  _ => "DisclosureImportInstruction"

    }
  }
 }





