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

import com.google.inject.ImplementedBy
import javax.inject.Inject
import javax.xml.parsers.{SAXParser, SAXParserFactory}
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory}
import models.{SaxParseError, ValidationFailure, ValidationSuccess, XMLValidationStatus}
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler

import scala.collection.mutable.ListBuffer
import scala.xml.{Elem, NodeSeq}

class XMLValidationService @Inject()(xmlValidationParser: XMLValidationParser){
  def validateXml(downloadSrc: String): (Elem, ListBuffer[SaxParseError]) = {
    val list: ListBuffer[SaxParseError] = new ListBuffer[SaxParseError]

    trait AccumulatorState extends DefaultHandler {
      override def warning(e: SAXParseException): Unit = list += SaxParseError(e.getLineNumber, e.getMessage)
      override def error(e: SAXParseException): Unit = list += SaxParseError(e.getLineNumber, e.getMessage)
      override def fatalError(e: SAXParseException): Unit = list += SaxParseError(e.getLineNumber, e.getMessage)
    }

    val elem = new scala.xml.factory.XMLLoader[scala.xml.Elem] {
      override def parser: SAXParser = xmlValidationParser.validatingParser
      override def adapter =
        new scala.xml.parsing.NoBindingFactoryAdapter
          with AccumulatorState


    }.load(new URL(downloadSrc))

   (elem, list)
  }

  def loadXML(downloadSrc: String): Elem = {
    new scala.xml.factory.XMLLoader[scala.xml.Elem] {
      override def parser: SAXParser = xmlValidationParser.validatingParser
      override def adapter =
        new scala.xml.parsing.NoBindingFactoryAdapter
    }.load(new URL(downloadSrc))
  }

}

@ImplementedBy(classOf[XMLDacXSDValidationParser])
trait XMLValidationParser {
  def validatingParser: SAXParser
}


class XMLDacXSDValidationParser extends XMLValidationParser {
  val schemaLang: String = javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI
  val isoXsdUrl: URL = getClass.getResource("/schemas/IsoTypes_v1.01.xsd")
  val ukDAC6XsdUrl: URL = getClass.getResource("/schemas/UKDac6XSD_v0.4.xsd")
  val isoXsdStream: StreamSource = new StreamSource(isoXsdUrl.openStream())
  val ukDAC6XsdStream: StreamSource = new StreamSource(ukDAC6XsdUrl.openStream())

  //IsoTypes xsd is referenced by UKDac6XSD so must come first in the array
  val streams: Array[Source] = Array(isoXsdStream, ukDAC6XsdStream)

  val schema: Schema = SchemaFactory.newInstance(schemaLang).newSchema(streams)

  val factory: SAXParserFactory = SAXParserFactory.newInstance()
  factory.setNamespaceAware(true)
  factory.setSchema(schema)

  override def validatingParser: SAXParser = factory.newSAXParser()
}
