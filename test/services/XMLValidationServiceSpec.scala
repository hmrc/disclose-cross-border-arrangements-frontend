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

import java.net.URL

import base.SpecBase
import cats.data.ReaderT
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory}
import models.{GenericError, Validation, ValidationFailure, ValidationSuccess}
import org.junit.Before
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import org.mockito.Matchers.any
import org.mockito.Matchers.anyObject
import org.mockito.MockitoAnnotations

import scala.xml.NodeSeq


class XMLValidationServiceSpec extends SpecBase with MockitoSugar {

  val sitemapUrl: String = getClass.getResource("/sitemap.xml").toString
  val sitemap2Url: String = getClass.getResource("/sitemap2.xml").toString
  val mockBusinessRuleValidationService = mock[BusinessRuleValidationService]

  val application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).overrides(
    bind[XMLDacXSDValidationParser].toInstance(MockitoSugar.mock[XMLDacXSDValidationParser]),
    bind[BusinessRuleValidationService].toInstance(mockBusinessRuleValidationService),


  ).build()

  trait SitemapParserSetup {
    val schemaLang: String = javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI
    val testUrl: URL = getClass.getResource("/sitemap-v0.9.xsd")
    val testStream: StreamSource = new StreamSource(testUrl.openStream())
    val schema: Schema = SchemaFactory.newInstance(schemaLang).newSchema(testStream)

    val factory: SAXParserFactory = SAXParserFactory.newInstance()
    factory.setNamespaceAware(true)
    factory.setSchema(schema)

    lazy val sut: XMLValidationService = {
      when(application.injector.instanceOf[XMLDacXSDValidationParser].validatingParser).thenReturn(factory.newSAXParser())

      application.injector.instanceOf[XMLValidationService]
    }
  }

  "XmlValidation Service" - {
    "must return a ValidationFailure with one error" in new SitemapParserSetup {
      when(mockBusinessRuleValidationService.validateFile()(any())).thenReturn(Some(List()))
      val validationFailure: ValidationFailure = sut.validateXml(sitemapUrl).asInstanceOf[ValidationFailure]
      validationFailure.error.length mustBe 1
      validationFailure.error.head.lineNumber mustBe Some(7)
      validationFailure.error.head.errorMessage.startsWith("cvc-complex-type.2.4.a") mustBe true
    }

    "must return a ValidationSuccess with no errors" in new SitemapParserSetup {
      val validationSuccess: ValidationSuccess = sut.validateXml(sitemap2Url).asInstanceOf[ValidationSuccess]
      validationSuccess.downloadUrl mustBe sitemap2Url
    }


    "must return a ValidationFailure when passed xsd validation but fails on business rules" in new SitemapParserSetup {
      val result = sut.validateXml(sitemap2Url) //.asInstanceOf[ValidationSuccess]

      val expectedError = GenericError("generic error", Some(1))
      result mustBe ValidationFailure(Seq(expectedError))
    }
  }
}
