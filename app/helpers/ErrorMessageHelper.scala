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

object ErrorMessageHelper extends ErrorConstants{

  val defaultMessage = "There is something wrong with this line"

  def buildErrorMessage(errorMessageInfo: ErrorMessageInfo): String = {

    errorMessageInfo match {
      case MissingAttributeInfo(element, attribute) => missingInfoMessage(element + " " + attribute)
      case InvalidEnumAttributeInfo(element, attribute) => invalidCodeMessage(element  + " "+ attribute).getOrElse(defaultMessage)
      case MissingElementInfo(element) => missingInfoMessage(element)
      case MaxLengthErrorInfo(element, allowedLength) => s"$element must be $allowedLength characters or less"
      case InvalidEnumErrorInfo(element)  => invalidCodeMessage(element).getOrElse(defaultMessage)
      case _ =>   "There is something wrong with this line"
    }
  }

  private def missingInfoMessage(elementName: String): String = {
    val vowels = "aeiouAEIOU"
    if(vowels.contains(elementName.head)){
      s"Enter an $elementName"
    }else s"Enter a $elementName"

  }

   def invalidCodeMessage(elementName: String): Option[String] = {
     elementName match {
      case "Country" | "CountryExemption" | "TIN issuedBy" => Some(s"$elementName is not one of the ISO country codes")
      case "ConcernedMS" => Some("ConcernedMS is not one of the ISO EU Member State country codes")
      case "Reason" | "IntermediaryNexus" | "RelevantTaxpayerNexus" |
           "Hallmark" | "ResCountryCode"  => Some(s"$elementName is not one of the allowed values")
      case "Capacity"=> Some(s"Capacity is not one of the allowed values")
      case _ => None
    }
   }
}
