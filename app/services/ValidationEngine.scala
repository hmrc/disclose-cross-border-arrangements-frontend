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

import scala.collection.mutable.ListBuffer
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

      val sp = groupedErrors._2.last.errorMessage.split("of element")
      val elementName = groupedErrors._2.last.errorMessage.split("of element").last.substring(2)

      val eType = groupedErrors._2.head.errorMessage.substring(0, 10)
      val tidedUpMessage = groupedErrors._2.head.errorMessage.substring(0, 10) + " " +
        elementName.dropRight(14)

      groupedErrors._2.head.copy(errorMessage = tidedUpMessage)


        }else groupedErrors._2.head

    }).toList

  //  errorsGroupedByLineNumber.map(x => x._2.last).toList

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
