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

package helpers

import models.Validation

import scala.xml.Elem

class LineNumberHelper {

  def getLineNumbersOfErrors(validations: Seq[Validation], xml: Elem): Seq[Validation] = {
    val xmlArray = xml.toString().split("\n")

    validations.map(error => error.key match {
      case "businessrules.initialDisclosure.needRelevantTaxPayer" =>  error.copy(lineNumber = getLineNumberNeedRelevantTaxPayer(xmlArray))
      case "businessrules.dac6D10OtherInfo.needHallMarkToProvideInfo" => error.copy(lineNumber = getLineNumberNeedHallMarkToProvideInfo(xmlArray))
      case "businessrules.relevantTaxpayerDiscloser.needRelevantTaxPayer" => error.copy(lineNumber = getLineNumberNeedRelevantTaxPayer2(xmlArray))
      case "businessrules.intermediaryDiscloser.needIntermediary" => error.copy(lineNumber = getLineNumberIntermediaryDiscloser(xmlArray))
      case "businessrules.taxPayerImplementingDates.needToBeAfterStart" => error.copy(lineNumber = getLineNumberImplementingDates(xmlArray))
      case "businessrules.initialDisclosureMA.allRelevantTaxPayersHaveTaxPayerImplementingDate" => error.copy(lineNumber = getLineNumberImplementingDates2(xmlArray))
      case "businessrules.mainBenefitTest1.oneOfSpecificHallmarksMustBePresent" => error.copy(lineNumber = getLineMainBenefitsTest(xmlArray))
      case _ => error.copy(lineNumber = getLineNumberImportInstruction(xmlArray))

    })
  }



   private def getLineNumberNeedHallMarkToProvideInfo(xmlArray: Array[String])={
       val index = xmlArray.indexWhere(str => str.contains("DAC6D1OtherInfo")) + 1
        Some(index)
     }

  private def getLineNumberNeedRelevantTaxPayer(xmlArray: Array[String])={
       val index = xmlArray.indexWhere(str => str.contains("InitialDisclosureMA")) + 1
        Some(index)
     }

  private def getLineNumberNeedRelevantTaxPayer2(xmlArray: Array[String])={
       val index = xmlArray.indexWhere(str => str.contains("RelevantTaxpayerDiscloser")) + 1
        Some(index)
     }

  private def getLineNumberIntermediaryDiscloser(xmlArray: Array[String])={
       val index = xmlArray.indexWhere(str => str.contains("IntermediaryDiscloser")) + 1
        Some(index)
     }

  private def getLineNumberImplementingDates(xmlArray: Array[String])={
       val index = xmlArray.indexWhere(str => str.contains("TaxpayerImplementingDate")) + 1
        Some(index)
     }

  private def getLineNumberImplementingDates2(xmlArray: Array[String])={
       val index = xmlArray.indexWhere(str => str.contains("RelevantTaxpayer")) + 1
        Some(index)
     }

  private def getLineNumberImportInstruction(xmlArray: Array[String])={
       val index = xmlArray.indexWhere(str => str.contains("DisclosureImportInstruction")) + 1
        Some(index)
     }

  private def getLineMainBenefitsTest(xmlArray: Array[String])={
       val index = xmlArray.indexWhere(str => str.contains("MainBenefitTest1")) + 1
        Some(index)
     }

 }
