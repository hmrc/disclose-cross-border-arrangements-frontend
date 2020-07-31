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

import helpers.{ErrorConstants, LineNumberHelper}
import javax.inject.Inject
import models.{SaxParseError, ValidationFailure, ValidationSuccess, XMLValidationStatus}
import org.scalactic.ErrorMessage

import scala.collection.mutable.ListBuffer
import scala.util.{Success, Try}
import scala.xml.Elem
import scala.util.matching.Regex


class ValidationEngine @Inject()(xmlValidationService: XMLValidationService,
                                 businessRuleValidationService: BusinessRuleValidationService,
                                 lineNumberHelper: LineNumberHelper) extends ErrorConstants{



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
       val errorTypeOption = determineErrorType(groupedErrors._2.head.errorMessage)

     val ce =  errorTypeOption match {
         case Some(errorType) =>
           val elementName = getElementName(errorType, groupedErrors._2)
           val subType = getSubType(errorType, groupedErrors._2.head.errorMessage)
           groupedErrors._2.head.copy(errorType = errorTypeOption,
             elementName = elementName,
             subType = subType)
         case None => groupedErrors._2.head
       }
ce
    }).toList
  }

  private def getElementName(errorType: String, errorMessages: ListBuffer[SaxParseError]): Option[String] ={
    errorType match {
      case MAX_LENGTH_ERROR | MISSING_VALUE_ERROR => Some(errorMessages.last.errorMessage.split("of element").last.substring(2).dropRight(15))
      case INVALID_ENUM_ERROR => getElementNameForEnumerationError(errorMessages.last.errorMessage)//Some(errorMessages.last.errorMessage.split("of element").last.substring(2).dropRight(15))
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

     Some(s"$elementName $attributeName")
   }
   else None

  }

  private def getSubType(errorType: String, errorMessage: String): Option[String] ={

    errorType match {
      case MAX_LENGTH_ERROR =>  extractValueFromMessage(errorMessage, MAX_LENGTH_PATTERN)
      case  INVALID_ENUM_ERROR => getCapacitySubType(errorMessage)
      case _ => None
      }
  }

  private def getCapacitySubType(errorMessage: String):Option[String] = {
    if(errorMessage.contains("DAC61104, DAC61105, DAC61106")) Some("RelevantTaxpayerDiscloser")
    else
    if(errorMessage.contains("DAC61101, DAC61102")) Some("Intermediary")
    else None
  }


  private def determineErrorType(message:String): Option[String] = {
    ERROR_TYPES.find(error => extractValueFromMessage(message, error).isDefined)

  }
  private def extractValueFromMessage(message: String, pattern: String): Option[String] ={

      val regEx = pattern.stripMargin.r
val first = regEx.findFirstMatchIn(message)
    regEx.findFirstMatchIn(message) match{
        case Some(value) => Some(value.toString)
        case _ => None
      }
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



}
