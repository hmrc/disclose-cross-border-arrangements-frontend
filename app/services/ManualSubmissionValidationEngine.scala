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
import models.{Dac6MetaData, SaxParseError, ValidationFailure, ValidationSuccess, XMLValidationStatus}
import org.slf4j.LoggerFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Elem, NodeSeq}
import collection.immutable.IndexedSeq


class ManualSubmissionValidationEngine @Inject()(xmlValidationService: XMLValidationService,
                                                 businessRuleValidationService: BusinessRuleValidationService,
                                                 xmlErrorMessageHelper: XmlErrorMessageHelper,
                                                 businessRulesErrorMessageHelper: BusinessRulesErrorMessageHelper,
                                                 metaDataValidationService: MetaDataValidationService,
                                                 auditService: AuditService) {

  private val logger = LoggerFactory.getLogger(getClass)
  private val noErrors: Seq[String] = Seq()


  def validateManualSubmission(xml: NodeSeq, enrolmentId: String)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext) : Future[Option[Seq[String]]] = {

    val elem = xml.asInstanceOf[Elem]

    try {
      val xmlAndXmlValidationStatus: ListBuffer[SaxParseError] = performXmlValidation(elem)
      val metaData = businessRuleValidationService.extractDac6MetaData()(elem)

      for {
        metaDateResult <- performMetaDataValidation(metaData, enrolmentId)
        businessRulesResult <- performBusinessRulesValidation(elem)
      } yield {
        combineResults(xmlAndXmlValidationStatus, businessRulesResult, metaDateResult) match {

          case None =>  auditService.auditManualSubmissionParseFailure(elem, xmlAndXmlValidationStatus)
                         None
          case result => result
        }
      }
    } catch {
      case e: Exception =>
        logger.warn(s"XML validation failed. The XML parser has thrown the exception: $e")
        Future.successful(None)
    }
  }

  private def combineResults(xmlResult: ListBuffer[SaxParseError], businessRulesResult: Seq[String],
                             metaDataResult:  Seq[String]):  Option[Seq[String]] = {

     if(xmlResult.isEmpty){
       Some(businessRulesResult ++ metaDataResult)
       }else None
 }

  def performXmlValidation(elem: Elem): ListBuffer[SaxParseError] = {

    xmlValidationService.validateManualSubmission(elem)

  }

  def performBusinessRulesValidation(elem: Elem)
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[String]] = {

      businessRuleValidationService.validateFile()(hc, ec)(elem) match {
        case Some(value) => value.map {
          seqValidation =>
             seqValidation.map(_.key)

        }
        case None => Future.successful(noErrors)
      }

  }

  def performBusinessRulesValidationForMan(elem: Elem)
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[String]] = {

      businessRuleValidationService.validateFile()(hc, ec)(elem) match {
        case Some(value) => value.map {
          seqValidation =>
             seqValidation.map(_.key)

        }
        case None => Future(Seq())
      }

  }

  def performMetaDataValidation(dac6MetaData: Option[Dac6MetaData], enrolmentId: String)
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[String]] = {

     metaDataValidationService.verifyMetaData(dac6MetaData, enrolmentId).map {
          seqValidation =>
              seqValidation.map(_.key)

        }
 }

}
