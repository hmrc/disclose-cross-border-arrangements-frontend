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

import base.SpecBase
import cats.data.ReaderT
import models.Validation
import services.BusinessRuleValidationService.{hallmarks, hallmarksForMainBenefitTest, hasIntermediaryDiscloser, hasMainBenefitTest1, implementationStartDate, isInitialDisclosureMA, noOfIntermediaries, noOfRelevantTaxPayers, taxPayerImplementingDates}
import utils.TestXml

import scala.xml.NodeSeq

class LineNumberHelperSpec extends SpecBase with TestXml {

  val lineNumberHelper = new LineNumberHelper


  "LineNumberService" - {
    "getLineNumbersOfErrors" - {

      "must correct line number when other info is provided when hallmark absent" in {

          val failedValidation = Validation(
          key = "businessrules.dac6D10OtherInfo.needHallMarkToProvideInfo",
          value = false
        )

        val result = lineNumberHelper.getLineNumbersOfErrors(Seq(failedValidation), otherInfoPopulatedXml)
        result mustBe Seq(failedValidation.copy(lineNumber = Some(14)))
      }
    }

    "must correct line number when other initialDisclosureMa is true and no relevantTaxpayers provided" in {

      val failedValidation = Validation(
        key = "businessrules.initialDisclosure.needRelevantTaxPayer",
        value = false
      )

        val result = lineNumberHelper.getLineNumbersOfErrors(Seq(failedValidation), initialDisclosureNoRelevantTaxpyersXml)
        result mustBe Seq(failedValidation.copy(lineNumber = Some(8)))
      }


    "must correct line number when RelevantTaxpayerDiscloser does not have a RelevantTaxPayer" in {

      val failedValidation = Validation(
        key = "businessrules.relevantTaxpayerDiscloser.needRelevantTaxPayer",
        value = false
      )

        val result = lineNumberHelper.getLineNumbersOfErrors(Seq(failedValidation), relevantTaxPayerDiscloserXml)
        result mustBe Seq(failedValidation.copy(lineNumber = Some(10)))
      }

    "must correct line number when IntermediaryDiscloser does not have an Intermediary" in {

      val failedValidation = Validation(
        key = "businessrules.intermediaryDiscloser.needIntermediary",
        value = false
      )

        val result = lineNumberHelper.getLineNumbersOfErrors(Seq(failedValidation), intermediaryDiscloserXml)
        result mustBe Seq(failedValidation.copy(lineNumber = Some(10)))
      }




    "must correct line number when implementingDates are not after start date" in {

      val failedValidation = Validation(
        key = "businessrules.taxPayerImplementingDates.needToBeAfterStart",
        value = false
      )

        val result = lineNumberHelper.getLineNumbersOfErrors(Seq(failedValidation), implementingDateAfterStartDateXml)
        result mustBe Seq(failedValidation.copy(lineNumber = Some(17)))
      }

    "must correct line number when for DisclosureImportInstruction error" in {

      val failedValidation = Validation(
        key = "businessrules.newDisclosure.mustNotHaveArrangementIDOrDisclosureID",
        value = false
      )

        val result = lineNumberHelper.getLineNumbersOfErrors(Seq(failedValidation), importInstructionErrorXml)
        result mustBe Seq(failedValidation.copy(lineNumber = Some(8)))
      }

 "must correct line number when InitialDisclosure Ma is true and relevant taxpayers do not have implementing Date" in {

      val failedValidation = Validation(
        key = "businessrules.initialDisclosureMA.allRelevantTaxPayersHaveTaxPayerImplementingDate",
        value = false
      )

        val result = lineNumberHelper.getLineNumbersOfErrors(Seq(failedValidation), missingTaxPayerImplementingDateXml)
        result mustBe Seq(failedValidation.copy(lineNumber = Some(10)))
      }


   "must correct line number when main benefit test does not have a specified hallmark" in {

      val failedValidation = Validation(
        key = "businessrules.mainBenefitTest1.oneOfSpecificHallmarksMustBePresent",
        value = false
      )

        val result = lineNumberHelper.getLineNumbersOfErrors(Seq(failedValidation), mainBenefitTestErrorXml)
        result mustBe Seq(failedValidation.copy(lineNumber = Some(10)))
      }





  }



}