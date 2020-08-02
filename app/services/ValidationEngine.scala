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

import helpers.{ErrorConstants, ErrorMessageHelper, ErrorMessageInfo, LineNumberHelper}
import javax.inject.Inject
import models.{GenericError, SaxParseError, ValidationFailure, ValidationSuccess, XMLValidationStatus}
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

      val filteredErrors = generateErrorMessages(xmlErrors._2)

      (xmlErrors._1,  ValidationFailure(filteredErrors))
    }
  }


  private def generateErrorMessages(errors: ListBuffer[SaxParseError]): List[GenericError] ={


    val errorsGroupedByLineNumber = errors.groupBy(saxParseError => saxParseError.lineNumber)

    errorsGroupedByLineNumber.map(groupedErrors => {
      if(groupedErrors._2.length.equals(2)){

        val lineNumber = groupedErrors._1
        val e1 = groupedErrors._2.head.errorMessage
        val e2 = groupedErrors._2.last.errorMessage
       val err = extractInvalidEnumAttributeValues(e1, e2) match {
          case Some(info) => ErrorMessageHelper.buildErrorMessage(info)
          case None =>   extractMissingElementValues(e1, e2) match {
            case Some(info) => ErrorMessageHelper.buildErrorMessage(info)
            case None =>  extractMaxLengthErrorValues(e1, e2) match {
              case Some(info) => ErrorMessageHelper.buildErrorMessage(info)
              case None =>  extractEnumErrorValues(e1, e2) match {
                case Some(info) => ErrorMessageHelper.buildErrorMessage(info)
                case None =>  "There is something wrong with this line"
            }
          }

        }

      }
       GenericError(lineNumber, err)


    }else  extractMissingAttributeValues(groupedErrors._2.head.errorMessage) match {
        case Some(info) => GenericError(groupedErrors._2.head.lineNumber, ErrorMessageHelper.buildErrorMessage(info))
        case None => GenericError(groupedErrors._2.head.lineNumber, "There is something wrong with this line")
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



}
