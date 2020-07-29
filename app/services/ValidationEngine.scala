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
import models.{ValidationFailure, ValidationSuccess, XMLValidationStatus}

import scala.xml.Elem

class ValidationEngine @Inject()(xmlValidationService: XMLValidationService,
                                 businessRuleValidationService: BusinessRuleValidationService,
                                 lineNumberHelper: LineNumberHelper){

  def validateFile(downloadUrl: String, businessRulesCheckRequired: Boolean = true) : XMLValidationStatus = {

    val xmlAndXmlValidationStatus: (Elem, XMLValidationStatus) = xmlValidationService.validateXml(downloadUrl)

    val businessRulesValidationResult: XMLValidationStatus = performBusinessRulesValidation(downloadUrl, xmlAndXmlValidationStatus._1, businessRulesCheckRequired)

    //TODO - clean up code - Change match order so Success is last
    // Add metaData to Success
    (xmlAndXmlValidationStatus._2, businessRulesValidationResult) match {
      case (ValidationSuccess(_), ValidationSuccess(_)) => ValidationSuccess(downloadUrl)
      case (ValidationFailure(xmlErrors), ValidationSuccess(_)) => ValidationFailure(xmlErrors)
      case (ValidationSuccess(_), ValidationFailure(errors)) => ValidationFailure(errors)
      case (ValidationFailure(xmlErrors), ValidationFailure(businessRulesErrors)) => ValidationFailure(xmlErrors ++ businessRulesErrors)

    }
 }


  def performBusinessRulesValidation(source: String, elem: Elem, businessRulesCheckRequired: Boolean): XMLValidationStatus = {

    if(businessRulesCheckRequired) {
      businessRuleValidationService.validateFile()(elem) match {
        case Some(List()) => ValidationSuccess(source)
        case Some(errors) => ValidationFailure(lineNumberHelper.getLineNumbersOfErrors(errors, elem).map(
                                               error => error.toSaxParseError))
        case None => ValidationSuccess(source)
      }
    } else {
      ValidationSuccess(source)
    }
  }
}
