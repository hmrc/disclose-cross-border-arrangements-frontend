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

import java.net.URL

import base.SpecBase
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory}
import models.SaxParseError
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import utils.TestXml

import scala.collection.mutable.ListBuffer



class XMLValidationServiceSpec extends SpecBase with MockitoSugar with TestXml {

  val sitemapUrl: String = getClass.getResource("/sitemap.xml").toString
  val sitemap2Url: String = getClass.getResource("/sitemap2.xml").toString
  val validXmlUrl: String = getClass.getResource("/valid.xml").toString
  val invalidXmlUrl: String = getClass.getResource("/invalid.txt").toString

  val application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).overrides(
    bind[XMLDacXSDValidationParser].toInstance(MockitoSugar.mock[XMLDacXSDValidationParser])
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
  val noErrors : ListBuffer[SaxParseError] = ListBuffer()

  trait ActualSetup {
    val schemaLang: String = javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI
    //val testUrl: URL = getClass.getResource("/sitemap-v0.9.xsd")
    val isoXsdUrl: URL = getClass.getResource("/schemas/IsoTypes_v1.01.xsd")
    val xsdUrl: URL = getClass.getResource("/schemas/UKDac6XSD_v0.5.xsd")

    val isoXsdStream: StreamSource = new StreamSource(isoXsdUrl.openStream())
    val ukDAC6XsdStream: StreamSource = new StreamSource(xsdUrl.openStream())

    val streams: Array[Source] = Array(isoXsdStream, ukDAC6XsdStream)

    val schema: Schema = SchemaFactory.newInstance(schemaLang).newSchema(streams)

 //   val streams: Array[Source] = Array(isoXsdStream, ukDAC6XsdStream)

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
      val result = sut.validateXml(sitemapUrl)._2
      result.length mustBe 1
      result.head.lineNumber mustBe 7
      result.head.errorMessage.startsWith("cvc-complex-type.2.4.a") mustBe true
    }

    "must return a ValidationSuccess with no errors" in new SitemapParserSetup {
      sut.validateXml(sitemap2Url)._2 mustBe noErrors
    }

    "must return a ValidationSuccess with no errors 2" in new ActualSetup {
      sut.validateXml(validXmlUrl)._2 mustBe noErrors
    }

    "must return a ValidationFailure with correct errors for missing mandatory information" in new ActualSetup {
      val result = sut.validateXml(invalidXmlUrl)._2
      result.length mustBe 2

      result.head.lineNumber mustBe 20
      result.head.errorMessage mustBe "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max400_Type'."
      result(1).lineNumber mustBe 20
      result(1).errorMessage mustBe "cvc-type.3.1.3: The value '' of element 'Street' is not valid."
    }

    "must return a ValidationSuccess with no errors for valid manual submission" in new SitemapParserSetup {

      sut.validateManualSubmission(validXml) mustBe noErrors
    }

    "must return a ValidationFailure with errors for invalid manual submission" in new SitemapParserSetup {

      val result = sut.validateManualSubmission(mainBenefitTestErrorXml)

//      result.length mustBe 2
//      result.head.lineNumber mustBe 20
      result.head.errorMessage mustBe "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max400_Type'."
      result(1).lineNumber mustBe 20
      result(1).errorMessage mustBe "cvc-type.3.1.3: The value '' of element 'Street' is not valid."
    }

  }
}
