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

import helpers.{BusinessRulesErrorMessageHelper, XmlErrorMessageHelper}
import javax.inject.Inject
import models.{Dac6MetaData, ValidationFailure, ValidationSuccess, XMLValidationStatus}
import org.slf4j.LoggerFactory

import scala.xml.Elem


class ValidationEngine @Inject()(xmlValidationService: XMLValidationService,
                                 businessRuleValidationService: BusinessRuleValidationService,
                                 businessRulesErrorMessageHelper: BusinessRulesErrorMessageHelper,
                                 xmlErrorMessageHelper: XmlErrorMessageHelper) {

  private val logger = LoggerFactory.getLogger(getClass)


  def validateFile(downloadUrl: String, businessRulesCheckRequired: Boolean = true) : Either[Exception, XMLValidationStatus] = {

    try {
      val xmlAndXmlValidationStatus: (Elem, XMLValidationStatus) = performXmlValidation(downloadUrl)

      val businessRulesValidationResult: XMLValidationStatus = performBusinessRulesValidation(downloadUrl, xmlAndXmlValidationStatus._1, businessRulesCheckRequired)

      (xmlAndXmlValidationStatus._2, businessRulesValidationResult) match {
        case (ValidationFailure(xmlErrors), ValidationFailure(businessRulesErrors)) =>
          val orderedErrors = (xmlErrors ++ businessRulesErrors).sortBy(_.lineNumber)
          Right(ValidationFailure(orderedErrors))
        case (ValidationFailure(xmlErrors), ValidationSuccess(_, _)) => Right(ValidationFailure(xmlErrors))
        case (ValidationSuccess(_, _), ValidationFailure(errors)) => Right(ValidationFailure(errors))
        case (ValidationSuccess(_, _), ValidationSuccess(_, _)) =>
          val retrieveMetaData: Option[Dac6MetaData] = businessRuleValidationService.extractDac6MetaData()(xmlAndXmlValidationStatus._1)
          Right(ValidationSuccess(downloadUrl, retrieveMetaData))
      }
    } catch {
      case e: Exception =>
        logger.warn(s"XML validation failed. The XML parser has thrown the exception: $e")
        Left(e)
    }
 }



  def performXmlValidation(source: String): (Elem, XMLValidationStatus) = {

    val xmlErrors = xmlValidationService.validateXml(source)
    if(xmlErrors._2.isEmpty) {
      (xmlErrors._1, ValidationSuccess(source))
    } else {

      val filteredErrors = xmlErrorMessageHelper.generateErrorMessages(xmlErrors._2)

      (xmlErrors._1,  ValidationFailure(filteredErrors))
    }
  }

  def performBusinessRulesValidation(source: String, elem: Elem, businessRulesCheckRequired: Boolean): XMLValidationStatus = {

    if (businessRulesCheckRequired) {
      businessRuleValidationService.validateFile()(elem) match {
        case Some(List()) => ValidationSuccess(source)
        case Some(errors) => ValidationFailure(businessRulesErrorMessageHelper.convertToGenericErrors(errors, elem))
        case None => ValidationSuccess(source)
      }
    } else {
      ValidationSuccess(source)
    }
  }

}
