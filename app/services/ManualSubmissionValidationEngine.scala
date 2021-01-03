/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem


class ManualSubmissionValidationEngine @Inject()(xmlValidationService: XMLValidationService,
                                                 businessRuleValidationService: BusinessRuleValidationService,
                                                 xmlErrorMessageHelper: XmlErrorMessageHelper,
                                                 businessRulesErrorMessageHelper: BusinessRulesErrorMessageHelper,
                                                 metaDataValidationService: MetaDataValidationService,
                                                 auditService: AuditService) {

  private val logger = LoggerFactory.getLogger(getClass)

//  def validateManualSubmission(xml : NodeSeq)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Seq[String]]] = {
//
//    val elem = xml.asInstanceOf[Elem]
//
//    xmlValidationService.validateManualSubmission(elem) match {
//      case ListBuffer() => businessRuleValidationService.validateFile () (hc, ec) (elem) match {
//                case Some (value) => value.map (seqValidation =>
//
//                  Some (seqValidation.map (_.key)))
//                case None => Future (Some(Seq()))
//               }
//      case ListBuffer(parseErrors) =>  auditService.auditManualSubmissionParseFailure(elem, ListBuffer(parseErrors))
//                                      Future(None)
//
//    }
//  }

  def validateFile(downloadUrl: String, enrolmentId: String,businessRulesCheckRequired: Boolean = true)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext) : Future[Either[Exception, XMLValidationStatus]] = {

    try {
      val xmlAndXmlValidationStatus: (Elem, XMLValidationStatus) = performXmlValidation(downloadUrl)
      val metaData = businessRuleValidationService.extractDac6MetaData()(xmlAndXmlValidationStatus._1)

      for {
        metaDateResult <- performMetaDataValidation(downloadUrl, xmlAndXmlValidationStatus._1, metaData, enrolmentId)
        businessRulesResult <- performBusinessRulesValidation(downloadUrl, xmlAndXmlValidationStatus._1, businessRulesCheckRequired)
      } yield {
        combineResults(xmlAndXmlValidationStatus._2, businessRulesResult, metaDateResult) match {
          case ValidationFailure(errors) =>
            auditService.auditValidationFailure(enrolmentId, metaData, errors, xmlAndXmlValidationStatus._1)
            errors.foreach(auditService.auditErrorMessage(_))
            Right(ValidationFailure(errors))

          case ValidationSuccess(_,_)=> Right(ValidationSuccess(downloadUrl, metaData))
        }
      }
    } catch {
      case e: Exception =>
        logger.warn(s"XML validation failed. The XML parser has thrown the exception: $e")
        Future.successful(Left(e))
    }
  }

  private def combineResults(xmlResult: XMLValidationStatus, businessRulesResult: XMLValidationStatus,
                             metaDataResult: XMLValidationStatus): XMLValidationStatus = {

    val xmlErrors = xmlResult match {
      case ValidationSuccess(_, _) => List()
      case ValidationFailure(errors) => errors
    }

    val businessRulesErrors = businessRulesResult match {
        case ValidationSuccess(_, _) => List()
        case ValidationFailure(errors) => errors
      }

    val idErrors = metaDataResult match {
      case ValidationSuccess(_, _) => List()
      case ValidationFailure(errors) => errors
    }

    val combinedErrors = (xmlErrors ++ businessRulesErrors ++ idErrors).sortBy(_.lineNumber)

    if (combinedErrors.isEmpty){
      ValidationSuccess("", None)
    } else {
      ValidationFailure(combinedErrors)
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

  def performBusinessRulesValidation(source: String, elem: Elem, businessRulesCheckRequired: Boolean)
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[XMLValidationStatus] = {

    if (businessRulesCheckRequired) {
      businessRuleValidationService.validateFile()(hc, ec)(elem) match {
        case Some(value) => value.map {
          seqValidation =>
            if (seqValidation.isEmpty) {
              ValidationSuccess(source)
            }
            else {
              ValidationFailure(businessRulesErrorMessageHelper.convertToGenericErrors(seqValidation, elem))
            }
        }
        case None => Future.successful(ValidationSuccess(source))
      }
    } else {
      Future.successful(ValidationSuccess(source))
    }
  }

  def performBusinessRulesValidationForMan(elem: Elem)
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[String]] = {

      businessRuleValidationService.validateFile()(hc, ec)(elem) match {
        case Some(value) => value.map {
          seqValidation =>
            if (seqValidation.isEmpty) {
              Seq()
            }
            else {
             seqValidation.map(_.key) //ValidationFailure(businessRulesErrorMessageHelper.convertToGenericErrors(seqValidation, elem))
            }
        }
        case None => Future(Seq())
      }

  }

  def performMetaDataValidation(source: String, elem: Elem, dac6MetaData: Option[Dac6MetaData], enrolmentId: String)
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[XMLValidationStatus] = {

      val result = metaDataValidationService.verifyMetaData(dac6MetaData, enrolmentId)

      result.map {
          seqValidation =>
            if (seqValidation.isEmpty) {
              ValidationSuccess(source)
            }
            else {
              ValidationFailure(businessRulesErrorMessageHelper.convertToGenericErrors(seqValidation, elem))
            }
        }

    }


}
