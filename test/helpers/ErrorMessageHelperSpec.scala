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

//  def invalidCodeMessage(elementName: String): Option[String] = {
//    elementName match {
//      case "Country" | "CountryExemption" => Some(s"$elementName is not one of the ISO country codes")
//      case "ConcernedMS" => Some("ConcernedMS is not one of the ISO EU Member State country codes")
//      case "Reason" => Some("Reason is not one of the allowed values")
//      case _ => None
//    }
//  }


  "ErrorMessageHelper"  - {
    "invalidCodeMessage" - {

      "must return correct message for 'Country'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("Country", None)
        result mustBe Some("Country is not one of the ISO country codes")
      }

      "must return correct message for 'CountryExemption'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("CountryExemption", None)
        result mustBe Some("CountryExemption is not one of the ISO country codes")
      }

      "must return correct message for 'ConcernedMS'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("ConcernedMS", None)
        result mustBe Some("ConcernedMS is not one of the ISO EU Member State country codes")
      }

      "must return correct message for 'Reason'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("Reason", None)
        result mustBe Some("Reason is not one of the allowed values")
      }

      "must return correct message for 'Capacity'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("Capacity", None)
        result mustBe Some("Intermediary/Capacity is not one of the allowed values")
      }

      "must return correct message for 'IntermediaryNexus'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("IntermediaryNexus", None)
        result mustBe Some("IntermediaryNexus is not one of the allowed values")
      }

      "must return correct message for 'RelevantTaxpayerNexus'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("RelevantTaxpayerNexus", None)
        result mustBe Some("RelevantTaxpayerNexus is not one of the allowed values")
      }

      "must return correct message for 'Hallmark'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("Hallmark", None)
        result mustBe Some("Hallmark is not one of the allowed values")
      }

      "must return correct message for 'ResCountryCode'" in {
        val result = ErrorMessageHelper.invalidCodeMessage("ResCountryCode", None)
        result mustBe Some("ResCountryCode is not one of the allowed values")
      }

      "must return None for unexpected elementName" in {
        val result = ErrorMessageHelper.invalidCodeMessage("Unexpected-name", None)
        result mustBe None
      }

    }
  }
}