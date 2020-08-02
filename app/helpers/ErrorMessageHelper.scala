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

import models.{GenericError, SaxParseError}

import scala.collection.mutable.ListBuffer

object ErrorMessageHelper {

  val defaultMessage = "There is something wrong with this line"

  def generateErrorMessages(errors: ListBuffer[SaxParseError]): List[GenericError] = {
    val errorsGroupedByLineNumber = errors.groupBy(saxParseError => saxParseError.lineNumber)

    errorsGroupedByLineNumber.map(groupedErrors => {
        val lineNumber = groupedErrors._1
        val error1 = groupedErrors._2.head.errorMessage
        val error2 = groupedErrors._2.last.errorMessage

        val error = extractInvalidEnumAttributeValues(error1, error2).orElse(
                  extractMissingElementValues(error1, error2)).orElse(
                  extractMaxLengthErrorValues(error1, error2)).orElse(
                  extractEnumErrorValues(error1, error2)).orElse(
          extractMissingAttributeValues(groupedErrors._2.head.errorMessage))


        GenericError(lineNumber, error.getOrElse(defaultMessage))

    }).toList

  }



  def extractMissingAttributeValues(errorMessage: String): Option[String] = {
    val format = """cvc-complex-type.4: Attribute '(.*?)' must appear on element '(.*?)'.""".stripMargin.r

    errorMessage match {
      case format(attribute, element) =>
           Some(missingInfoMessage(element + " " + attribute))
      case _ => None
    }
  }

  def extractInvalidEnumAttributeValues(errorMessage1: String, errorMessage2: String): Option[String] = {
    val formatOfFirstError = """cvc-enumeration-valid: Value '(.*?)' is not facet-valid with respect to enumeration '(.*?)'. It must be a value from the enumeration.""".stripMargin.r
    val formatOfSecondError = """cvc-attribute.3: The value '(.*?)' of attribute '(.*?)' on element '(.*?)' is not valid with respect to its type, '(.*?)'.""".stripMargin.r

    errorMessage1 match {
      case formatOfFirstError(_, _) =>
        errorMessage2 match {
          case formatOfSecondError(_, attribute, element, _) =>
            invalidCodeMessage(element  + " "+ attribute)
          case _ => None
        }
      case _ => None

    }
  }

  def extractMissingElementValues(errorMessage1: String, errorMessage2: String): Option[String] = {
    val formatOfFirstError = """cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max400_Type'.""".stripMargin.r
    val formatOfSecondError = """cvc-type.3.1.3: The value '' of element '(.*?)' is not valid.""".stripMargin.r

    errorMessage1 match {
      case formatOfFirstError() =>
        errorMessage2 match {
          case formatOfSecondError(element) =>
            Some(missingInfoMessage(element))
          case _ => None
        }
      case _ =>  None
    }
  }

  def extractMaxLengthErrorValues(errorMessage1: String, errorMessage2: String): Option[String] = {
    val formatOfFirstError = """cvc-maxLength-valid: Value '(.*?)' with length = '(.*?)' is not facet-valid with respect to maxLength '(.*?)' for type '(.*?)'.""".stripMargin.r
    val formatOfSecondError = """cvc-type.3.1.3: The value '(.*?)' of element '(.*?)' is not valid.""".stripMargin.r

    errorMessage1 match {
      case formatOfFirstError(_, _, allowedLength, _) =>
        errorMessage2 match {
          case formatOfSecondError(_, element) =>
            Some(s"$element must be $allowedLength characters or less")
          case _ => None
        }
      case _ =>  None
    }
  }

  def extractEnumErrorValues(errorMessage1: String, errorMessage2: String): Option[String] = {
    val formatOfFirstError = """cvc-enumeration-valid: Value '(.*?)' is not facet-valid with respect to enumeration '(.*?)'. It must be a value from the enumeration.""".stripMargin.r
    val formatOfSecondError = """cvc-type.3.1.3: The value '(.*?)' of element '(.*?)' is not valid.""".stripMargin.r

    errorMessage1 match {
      case formatOfFirstError(_, _) =>
        errorMessage2 match {
          case formatOfSecondError(_, element) =>
            invalidCodeMessage(element)
          case _ =>  None
        }
      case _ => None
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