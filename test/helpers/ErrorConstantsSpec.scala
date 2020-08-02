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
import models.SaxParseError

class ErrorConstantsSpec extends SpecBase with ErrorConstants {

  val over400 = "a" * 401
  val over4000 = "a" * 4001

  //  def invalidCodeMessage(elementName: String): Option[String] = {
  //    elementName match {
  //      case "Country" | "CountryExemption" => Some(s"$elementName is not one of the ISO country codes")
  //      case "ConcernedMS" => Some("ConcernedMS is not one of the ISO EU Member State country codes")
  //      case "Reason" => Some("Reason is not one of the allowed values")
  //      case _ => None
  //    }
  //  }


  val s1 = "Request URL: https://start.duckduckgo.com/ Request Method: GET Status Code: 304 Remote Address: 107.20.240.232:443"


  "ErrorMessageHelper" - {
    "invalidCodeMessage" - {

      "must return correct message for 'Country'" in {
        val result = extractRequestValues(s1)
        result mustBe Some(RequestValues("https://start.duckduckgo.com/", "GET", "304", "107.20.240.232:443"))
      }


//      "must return correct info invalid attribute error'" in {
//        val invalidAttributeError = "cvc-attribute.3: The value 'VUVs' of attribute 'currCode' on element 'Amount' is not valid with respect to its type, 'currCode_Type'."
//
//        val result = extractInvalidAttributeValues(invalidAttributeError)
//        result mustBe Some(InvalidAttributeInfo("Amount", "currCode"))
//      }

      "must return correct info for missing attribute error'" in {
        val missingAttributeError = "cvc-complex-type.4: Attribute 'currCode' must appear on element 'Amount'."
        val result = extractMissingAttributeValues(missingAttributeError)
        result mustBe Some(MissingAttributeInfo("Amount", "currCode"))
      }

      "must return correct info for invalid enum error for attribute" in {
        val invalidEnumError1 = "cvc-enumeration-valid: Value 'GBf' is not facet-valid with respect to enumeration '[AF, AX]'. It must be a value from the enumeration."
        val invalidEnumError2 = "cvc-attribute.3: The value 'GBf' of attribute 'issuedBy' on element 'TIN' is not valid with respect to its type, 'CountryCode_Type'."
        val result = extractInvalidEnumAttributeValues(invalidEnumError1, invalidEnumError2)
        result mustBe Some(InvalidEnumAttributeInfo("TIN", "issuedBy"))
      }

      "must return correct info for missing element error'" in {

        val missingElementError1 = "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max400_Type'."

        val missingElementError2 = "cvc-type.3.1.3: The value '' of element 'Street' is not valid."

          val result = extractMissingElementValues(missingElementError1, missingElementError2)
          result mustBe Some(MissingElementInfo("Street"))
      }

      "must return correct info when allowed length exceeded" in {

        val maxLengthError1 = "cvc-maxLength-valid: Value '$over400' with length = '401' is not facet-valid with respect to maxLength '400' for type 'StringMin1Max400_Type'."
        val maxlengthError2 = "cvc-type.3.1.3: The value '$over400' of element 'BuildingIdentifier' is not valid."

        val result = extractMaxLengthErrorValues(maxLengthError1, maxlengthError2)
          result mustBe Some(MaxLengthErrorInfo("BuildingIdentifier", "400"))
      }


      "must return correct info when invalid enum given for element" in {

        val invalidEnumError1 = "cvc-enumeration-valid: Value 'Invalid code' is not facet-valid with respect to enumeration '[AF, AX, AL, DZ]'. It must be a value from the enumeration."
        val invalidEnumError2 = "cvc-type.3.1.3: The value 'Raneevev' of element 'Country' is not valid."

        val result = extractEnumErrorValues(invalidEnumError1, invalidEnumError2)
          result mustBe Some(InvalidEnumErrorInfo("Country"))
      }



    }
  }
}