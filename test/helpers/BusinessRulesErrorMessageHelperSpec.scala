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
import models.{GenericError, Validation}
import utils.TestXml

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

      "must  return correct error message when InitialDisclosureMA is true in the Initial disclosure and relevant taxpayers do not have implementing Date" in {

        val failedValidation = Validation(
          key = "businessrules.initialDisclosureMA.firstDisclosureHasInitialDisclosureMAAsTrue",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), missingTaxPayerImplementingDateXml)
        result mustBe List(GenericError(8, "Arrangement ID relates to a previous initial disclosure where " +
          "InitialDisclosureMA is true so each RelevantTaxpayer must have a TaxpayerImplementingDate"))
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

      "must  return correct error message taxpayerimplementing dates are populated when they shouldnt be" in {

        val failedValidation = Validation(
          key = "businessrules.nonMA.cantHaveRelevantTaxPayer",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), relevantTaxPayerDiscloserXml)
        result mustBe List(GenericError(15, "Remove the TaxpayerImplementingDate"))
      }

      "must  return correct error message for relevant taxpayer date of birth before 01/01/1903" in {

        val failedValidation = Validation(
          key = "businessrules.RelevantTaxPayersBirthDates.maxDateOfBirthExceeded",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), invalidDatesOfBirthXml)
        result mustBe List(GenericError(15, "Check BirthDate, all RelevantTaxPayer dates of birth must be on or after 1 January 1903"))
      }

      "must  return correct error message for AssociatedEnterprises date of birth before 01/01/1903" in {

        val failedValidation = Validation(
          key = "businessrules.AssociatedEnterprisesBirthDates.maxDateOfBirthExceeded",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), invalidDatesOfBirthXml)
        result mustBe List(GenericError(28, "Check BirthDate, all AssociatedEnterprises dates of birth must be on or after 1 January 1903"))
      }

      "must  return correct error message for disclosing date of birth before 01/01/1903" in {

        val failedValidation = Validation(
          key = "businessrules.DisclosingBirthDates.maxDateOfBirthExceeded",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), invalidDatesOfBirthXml)
        result mustBe List(GenericError(8, "Check BirthDate, Disclosing date of birth must be on or after 1 January 1903"))
      }

      "must  return correct error message for intermediary date of birth before 01/01/1903" in {

        val failedValidation = Validation(
          key = "businessrules.IntermediaryBirthDates.maxDateOfBirthExceeded",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), invalidDatesOfBirthXml)
        result mustBe List(GenericError(20, "Check BirthDate, all Intermediary dates of birth must be on or after 1 January 1903"))
      }

      "must  return correct error message for affectedPersons date of birth before 01/01/1903" in {

        val failedValidation = Validation(
          key = "businessrules.AffectedPersonsBirthDates.maxDateOfBirthExceeded",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), invalidDatesOfBirthXml)
        result mustBe List(GenericError(24, "Check BirthDate, all AffectedPersons dates of birth must be on or after 1 January 1903"))
      }

      "must  return correct error message when ArrangementID does not match HMRC's records" in {

        val failedValidation = Validation(
          key = "metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), importInstructionErrorXml)
        result mustBe List(GenericError(6, "ArrangementID does not match HMRC's records"))
      }

      "must  return correct error message when DisclosureInformation is not provided in a DAC6REP, " +
        "to replace the original arrangement details" in {

        val failedValidation = Validation(
          key = "metaDataRules.disclosureInformation.noInfoWhenReplacingDAC6NEW",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), missingTaxPayerImplementingDateXml)
        result mustBe List(GenericError(7, "Provide DisclosureInformation in this DAC6REP file, to replace the original arrangement details"))
      }

      "must  return correct error message when DACREP file for non MA does not contain DisclosureInformation" in {

        val failedValidation = Validation(
          key = "metaDataRules.disclosureInformation.noInfoForNonMaDAC6REP",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), missingTaxPayerImplementingDateXml)
        result mustBe List(GenericError(7, "Provide DisclosureInformation in this DAC6REP file. This is a mandatory field for arrangements that are not marketable"))
      }

      "must  return correct error message when user tries to change the InitialDisclosureMA flag to true" in {

        val failedValidation = Validation(
          key = "metaDataRules.initialDisclosureMA.arrangementNowMarketable",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), initialDisclosureNoRelevantTaxpyersXml)
        result mustBe List(GenericError(8, "Change the InitialDisclosureMA to match the original declaration. If the arrangement has since become marketable, you will need to make a new report"))
      }

      "must  return correct error message when user tries to change the InitialDisclosureMA flag to false" in {

        val failedValidation = Validation(
          key = "metaDataRules.initialDisclosureMA.arrangementNoLongerMarketable",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), initialDisclosureNoRelevantTaxpyersXml)
        result mustBe List(GenericError(8, "Change the InitialDisclosureMA to match the original declaration. If the arrangement is no longer marketable, you will need to make a new report"))
      }

      "must  return correct error message when DisclosureID does not match the ArrangementID provided" in {

        val failedValidation = Validation(
          key = "metaDataRules.disclosureId.disclosureIDDoesNotMatchArrangementID",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), importInstructionErrorXml)
        result mustBe List(GenericError(16, "DisclosureID does not match the ArrangementID provided"))
      }

      "must  return correct error message when DisclosureID has not been generated by this individual or organisation" in {

        val failedValidation = Validation(
          key = "metaDataRules.disclosureId.disclosureIDDoesNotMatchUser",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), importInstructionErrorXml)
        result mustBe List(GenericError(16, "DisclosureID has not been generated by this individual or organisation"))
      }


      "must  return correct error message when user does not Provide DisclosureInformation in a DAC6NEW file" in {

        val failedValidation = Validation(
          key = "metaDataRules.disclosureInformation.disclosureInformationMissingFromDAC6NEW",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), importInstructionErrorXml)
        result mustBe List(GenericError(8, "Provide DisclosureInformation in this DAC6NEW file"))
      }

      "must  return correct error message when user does not provide DisclosureInformation for a DAC6ADD file" +
        "linked to a non marketable arrangement" in {

        val failedValidation = Validation(
          key = "metaDataRules.disclosureInformation.disclosureInformationMissingFromDAC6ADD",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), importInstructionErrorXml)
        result mustBe List(GenericError(8, "Provide DisclosureInformation in this DAC6ADD file. This is a mandatory field for arrangements that are not marketable"))
      }


      "must  return correct error message when MessageRefID is in wrong format" in {

        val failedValidation = Validation(
          key = "metaDataRules.messageRefId.wrongFormat",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), importInstructionErrorXml)
        result mustBe List(GenericError(3, "The MessageRefID should start with GB, then your User ID, followed by identifying characters of your choice. It must be 200 characters or less"))
      }

      "must  return correct error message when MessageRefID does not contain userId" in {

        val failedValidation = Validation(
          key = "metaDataRules.messageRefId.noUserId",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), importInstructionErrorXml)
        result mustBe List(GenericError(3, "Check UserID is correct, it must match the ID you got at registration to create a valid MessageRefID"))
      }

      "must  return correct error message when MessageRefID is not unique" in {

        val failedValidation = Validation(
          key = "metaDataRules.messageRefId.notUnique",
          value = false
        )

        val result = errorHelper.convertToGenericErrors(Seq(failedValidation), importInstructionErrorXml)
        result mustBe List(GenericError(3, "Check your MessageRefID is unique. It should start with GB, then your User ID, followed by unique identifying characters of your choice. It must be 200 characters or less"))
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