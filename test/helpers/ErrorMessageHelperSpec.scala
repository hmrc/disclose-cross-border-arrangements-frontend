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

class ErrorMessageHelperSpec extends SpecBase{

  "ErrorMessageHelper"  - {

    "getErrorInfo" - {

      "must return correct info for missing attribute error'" in {
        val missingAttributeError = "cvc-complex-type.4: Attribute 'currCode' must appear on element 'Amount'."
        val result = ErrorMessageHelper.extractMissingAttributeValues(missingAttributeError)
        result mustBe Some("Enter an Amount currCode")
      }

      "must return correct info for invalid enum error for attribute" in {
        val invalidEnumError1 = "cvc-enumeration-valid: Value 'GBf' is not facet-valid with respect to enumeration '[AF, AX]'. It must be a value from the enumeration."
        val invalidEnumError2 = "cvc-attribute.3: The value 'GBf' of attribute 'issuedBy' on element 'TIN' is not valid with respect to its type, 'CountryCode_Type'."
        val result = ErrorMessageHelper.extractInvalidEnumAttributeValues(invalidEnumError1, invalidEnumError2)
        result mustBe Some("TIN issuedBy is not one of the ISO country codes")
      }

      "must return correct info for missing element error'" in {

        val missingElementError1 = "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max400_Type'."

        val missingElementError2 = "cvc-type.3.1.3: The value '' of element 'Street' is not valid."

        val result = ErrorMessageHelper.extractMissingElementValues(missingElementError1, missingElementError2)
        result mustBe Some("Enter a Street")
      }

      "must return correct info when allowed length exceeded" in {

        val maxLengthError1 = "cvc-maxLength-valid: Value '$over400' with length = '401' is not facet-valid with respect to maxLength '400' for type 'StringMin1Max400_Type'."
        val maxlengthError2 = "cvc-type.3.1.3: The value '$over400' of element 'BuildingIdentifier' is not valid."

        val result = ErrorMessageHelper.extractMaxLengthErrorValues(maxLengthError1, maxlengthError2)
        result mustBe Some("BuildingIdentifier must be 400 characters or less")
      }


      "must return correct info when invalid enum given for element" in {

        val invalidEnumError1 = "cvc-enumeration-valid: Value 'Invalid code' is not facet-valid with respect to enumeration '[AF, AX, AL, DZ]'. It must be a value from the enumeration."
        val invalidEnumError2 = "cvc-type.3.1.3: The value 'Raneevev' of element 'Country' is not valid."

        val result = ErrorMessageHelper.extractEnumErrorValues(invalidEnumError1, invalidEnumError2)
        result mustBe Some("Country is not one of the ISO country codes")
      }
    }

    "invalidCodeMessage" - {

      "must return correct message for 'Country'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("Country")
        result mustBe Some("Country is not one of the ISO country codes")
      }

      "must return correct message for 'CountryExemption'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("CountryExemption")
        result mustBe Some("CountryExemption is not one of the ISO country codes")
      }

      "must return correct message for 'ConcernedMS'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("ConcernedMS")
        result mustBe Some("ConcernedMS is not one of the ISO EU Member State country codes")
      }

      "must return correct message for 'Reason'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("Reason")
        result mustBe Some("Reason is not one of the allowed values")
      }

      "must return correct message for 'Capacity'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("Capacity")
        result mustBe Some("Capacity is not one of the allowed values")
      }

      "must return correct message for 'IntermediaryNexus'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("IntermediaryNexus")
        result mustBe Some("IntermediaryNexus is not one of the allowed values")
      }

      "must return correct message for 'RelevantTaxpayerNexus'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("RelevantTaxpayerNexus")
        result mustBe Some("RelevantTaxpayerNexus is not one of the allowed values")
      }

      "must return correct message for 'Hallmark'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("Hallmark")
        result mustBe Some("Hallmark is not one of the allowed values")
      }

      "must return correct message for 'ResCountryCode'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("ResCountryCode")
        result mustBe Some("ResCountryCode is not one of the allowed values")
      }

      "must return None for unexpected elementName" in {
        val result = ErrorMessageHelper.invalidCodeMessage("Unexpected-name")
        result mustBe None
      }

    }
  }
}