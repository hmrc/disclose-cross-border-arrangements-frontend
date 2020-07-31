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

package services

import helpers.LineNumberHelper
import javax.inject.Inject
import models.{SaxParseError, ValidationFailure, ValidationSuccess, XMLValidationStatus}
import org.scalactic.ErrorMessage

import scala.collection.mutable.ListBuffer
import scala.util.{Success, Try}
import scala.xml.Elem

class ValidationEngine @Inject()(xmlValidationService: XMLValidationService,
                                 businessRuleValidationService: BusinessRuleValidationService,
                                 lineNumberHelper: LineNumberHelper){

  def validateFile(source: String, businessRulesCheckRequired: Boolean = true) : XMLValidationStatus = {

    val xmlAndXmlValidationStatus = performXmlValidation(source)



    val businessRulesValidationResult = performBusinessRulesValidation(source, xmlAndXmlValidationStatus._1, businessRulesCheckRequired)

    (xmlAndXmlValidationStatus._2, businessRulesValidationResult) match{
      case (ValidationSuccess(_), ValidationSuccess(_)) => ValidationSuccess(source)
      case (ValidationFailure(xmlErrors), ValidationSuccess(_)) => ValidationFailure(xmlErrors)
      case (ValidationSuccess(_), ValidationFailure(errors)) => ValidationFailure(errors)
      case (ValidationFailure(xmlErrors), ValidationFailure(businessRulesErrors)) => ValidationFailure(xmlErrors ++ businessRulesErrors)

    }
 }



  def performXmlValidation(source: String): (Elem, XMLValidationStatus) = {

    val xmlErrors = xmlValidationService.validateXml(source)
    if(xmlErrors._2.isEmpty) {
      (xmlErrors._1, ValidationSuccess(source))
    }else {

      val filteredErrors = cleanseParseErrors(xmlErrors._2)

      (xmlErrors._1,  ValidationFailure(filteredErrors.map(parseError => parseError.toGenericError)))
    }
  }

  private def cleanseParseErrors(errors: ListBuffer[SaxParseError]): List[SaxParseError] ={

    val errorsGroupedByLineNumber = errors.groupBy(saxParseError => saxParseError.lineNumber)

    errorsGroupedByLineNumber.map(groupedErrors => {
    if(groupedErrors._2.length.equals(2)){

     val cleansedError =  Try {
        val errorType = groupedErrors._2.head.errorMessage.substring(0, 10)

        val elementName = getElementName(errorType, groupedErrors._2)

        val subType = getSubType(errorType, groupedErrors._2.head.errorMessage)
        groupedErrors._2.head.copy(errorType = Some(errorType),
                                   elementName = elementName,
                                   subType = subType)
      }

      cleansedError match {
        case Success(error) => error
        case _ => groupedErrors._2.head

      }

    }else {
       val errorType = getErrorTypeForAttributeError(groupedErrors._2.head.errorMessage)
      groupedErrors._2.head.copy(errorType = errorType)
    }
    }).toList
  }

  def performBusinessRulesValidation(source: String, elem: Elem, businessRulesCheckRequired: Boolean): XMLValidationStatus = {

    if(businessRulesCheckRequired) {
      businessRuleValidationService.validateFile()(elem) match {
        case Some(List()) => ValidationSuccess(source)
        case Some(errors) => ValidationFailure(lineNumberHelper.getLineNumbersOfErrors(errors, elem).map(
                                               error => error.toGenericError))
        case None => ValidationSuccess(source)
      }
    }else ValidationSuccess(source)

  }

  private def getErrorTypeForAttributeError(errorMessage: String): Option[String] = {
    if(errorMessage.contains("must appear on element")) Some("missingAttribute")
    else
    if(errorMessage.contains("is not valid with respect to its type")) Some("invalidAttribute")
    else None

  }

  private def getElementName(errorType: String, errorMessages: ListBuffer[SaxParseError]): Option[String] ={
    errorType match {
      case "cvc-maxLen" | "cvc-minLen" => Some(errorMessages.last.errorMessage.split("of element").last.substring(2).dropRight(15))
      case  "cvc-enumer" => getElementNameForEnumerationError(errorMessages.last.errorMessage)//Some(errorMessages.last.errorMessage.split("of element").last.substring(2).dropRight(15))
      case _ => None
    }




  }

  private def getElementNameForEnumerationError(errorMessage: String): Option[String] = {
    println("error = " + errorMessage)
   if(errorMessage.contains("of element"))   Some(errorMessage.split("of element").last.substring(2).dropRight(15))
    else
   if(errorMessage.contains("on element")) {

     val elementName = errorMessage.split("on element")(1).substring(2,5)
     val attributeName = errorMessage.split("of attribute")(1).substring(2, 10)
     println("attributeName = " + attributeName)

     Some(s"$elementName $attributeName")
   }
   else None

  }

  private def getSubType(errorType: String, errorMessage: String): Option[String] ={

    errorType match {
      case "cvc-maxLen" =>  Some(errorMessage.split("StringMin1Max").last.replaceAll("[^0-9]", ""))
      case  "cvc-enumer" => getCapacitySubType(errorMessage)
      case _ => None
      }
  }

  private def getCapacitySubType(errorMessage: String):Option[String] = {
    if(errorMessage.contains("DAC61104, DAC61105, DAC61106")) Some("RelevantTaxpayerDiscloser")
    else
    if(errorMessage.contains("DAC61101, DAC61102")) Some("Intermediary")
    else None
  }
}
