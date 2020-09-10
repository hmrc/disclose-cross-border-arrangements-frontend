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
import models.{GenericError, Validation}
import services.BusinessRuleValidationService.{hallmarks, hallmarksForMainBenefitTest, hasIntermediaryDiscloser, hasMainBenefitTest1, implementationStartDate, isInitialDisclosureMA, noOfIntermediaries, noOfRelevantTaxPayers, taxPayerImplementingDates}
import utils.TestXml

import scala.xml.NodeSeq

class BusinessRulesErrorMessageHelperSpec extends SpecBase with TestXml {

  val errorHelper = new BusinessRulesErrorMessageHelper


  "BusinessRulesErrorMessageHelper" - {
    "getErrorMessage" - {

      "must correct error message when other info is provided when hallmark absent" in {

        val failedValidation = Validation(
          key = "businessrules.dac6D10OtherInfo.needHallMarkToProvideInfo",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), otherInfoPopulatedXml)
        result mustBe List(GenericError(14, "DAC6D1OtherInfo has been provided but hallmark DAC6D1Other has not been selected"))
      }

      "must return correct error message when implementing date is before start date" in {

        val failedValidation = Validation(
          key = "businessrules.implementingDates.needToBeAfterStart",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), otherInfoPopulatedXml)
        result mustBe List(GenericError(8, "The DisclosureInformation/ImplementingDate on which the first step in the implementation of the reportable cross-border arrangement has been made or will be made must be on or after 25 June 2018"))
      }

      "must return correct error message when other initialDisclosureMa is false and no relevantTaxpayers provided" in {

      val failedValidation = Validation(
        key = "businessrules.initialDisclosure.needRelevantTaxPayer",
        value = false
      )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), initialDisclosureNoRelevantTaxpyersXml)
        result mustBe List(GenericError(8, "InitialDisclosureMA is false so there should be a RelevantTaxpayer"))
      }

      "must return correct error message when other initialDisclosureMa is true and importInstruction is DAC6ADD" in {

      val failedValidation = Validation(
        key = "businessrules.addDisclosure.mustNotBeInitialDisclosureMA",
        value = false
      )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), initialDisclosureNoRelevantTaxpyersXml)
        result mustBe List(GenericError(7, "InitialDisclosureMA is true so DisclosureImportInstruction cannot be DAC6ADD"))
      }


      "must  return correct error message when RelevantTaxpayerDiscloser does not have a RelevantTaxPayer" in {

      val failedValidation = Validation(
        key = "businessrules.relevantTaxpayerDiscloser.needRelevantTaxPayer",
        value = false
      )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), relevantTaxPayerDiscloserXml)
        result mustBe List(GenericError(10, "RelevantTaxpayerDiscloser has been provided so there must be at least one RelevantTaxpayer"))
      }

      "must  return correct error message when IntermediaryDiscloser does not have an Intermediary" in {

      val failedValidation = Validation(
        key = "businessrules.intermediaryDiscloser.needIntermediary",
        value = false
      )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), intermediaryDiscloserXml)
        result mustBe List(GenericError(10, "IntermediaryDiscloser has been provided so there must be at least one Intermediary"))
      }


      "must  return correct error message when implementingDates are not after start date" in {

      val failedValidation = Validation(
        key = "businessrules.taxPayerImplementingDates.needToBeAfterStart",
        value = false
      )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), implementingDateAfterStartDateXml)
        result mustBe List(GenericError(17, "The TaxpayerImplementingDate when the arrangement has been or will be made available to each taxpayer must be on or after 25 June 2018"))
      }

     "must  return correct error message when InitialDisclosure Ma is true and relevant taxpayers do not have implementing Date" in {

      val failedValidation = Validation(
        key = "businessrules.initialDisclosureMA.allRelevantTaxPayersHaveTaxPayerImplementingDate",
        value = false
      )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), missingTaxPayerImplementingDateXml)
        result mustBe List(GenericError(8, "InitialDisclosureMA is true and there are RelevantTaxpayers so each RelevantTaxpayer must have a TaxpayerImplementingDate"))
      }

     "must  return correct error message when main benefit test does not have a specified hallmark" in {

      val failedValidation = Validation(
        key = "businessrules.mainBenefitTest1.oneOfSpecificHallmarksMustBePresent",
        value = false
      )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), mainBenefitTestErrorXml)
        result mustBe List(GenericError(10, "MainBenefitTest1 is false but the hallmarks A, B, C1bi, C1c and/or C1d have been selected"))
      }


      "must  return correct error message when IntitialDisclosureMA is true but arrangement/disclosure id's provided" in {

      val failedValidation = Validation(
        key = "businessrules.newDisclosure.mustNotHaveArrangementIDOrDisclosureID",
        value = false
      )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), initialDisclosureNoRelevantTaxpyersXml)
        result mustBe List(GenericError(7, "DisclosureImportInstruction is DAC6NEW so there should be no ArrangementID or DisclosureID"))
      }



      "must  return correct error message when DAC6ADD has disclosureID" in {

        val failedValidation = Validation(
          key = "businessrules.addDisclosure.mustHaveArrangementIDButNotDisclosureID",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), missingTaxPayerImplementingDateXml)
        result mustBe List(GenericError(7, "DisclosureImportInstruction is DAC6ADD so there should be an ArrangementID and no DisclosureID"))
      }

      "must  return correct error message when DAC6REP does not have Arrangement ID/disclosureID" in {

        val failedValidation = Validation(
          key = "businessrules.repDisclosure.mustHaveArrangementIDDisclosureIDAndMessageRefID",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), missingTaxPayerImplementingDateXml)
        result mustBe List(GenericError(7, "DisclosureImportInstruction is DAC6REP so there should be an ArrangementID and a DisclosureID"))
      }

      "must  return correct error message when DAC6DEL does not have Arrangement ID/disclosureID" in {

        val failedValidation = Validation(
          key = "businessrules.delDisclosure.mustHaveArrangementIDDisclosureIDAndMessageRefID",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), missingTaxPayerImplementingDateXml)
        result mustBe List(GenericError(7, "DisclosureImportInstruction is DAC6DEL so there should be an ArrangementID and a DisclosureID"))
      }

      "must  return correct default message for unexpected error key" in {

        val failedValidation = Validation(
          key = "random error",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), missingTaxPayerImplementingDateXml)
        result mustBe List(GenericError(7, "There is a problem with this line number"))
      }


    }


  }



}