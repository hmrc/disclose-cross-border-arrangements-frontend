import org.slf4j.LoggerFactory
import org.xml.sax.helpers.DefaultHandler
import play.api.Logger

import scala.xml.SAXParseException

trait ExceptionErrorHandler extends DefaultHandler {

  override
  def warning(ex: SAXParseException): Unit = throw ex
  override def error(ex: SAXParseException): Unit = throw ex
  override def fatalError(ex: SAXParseException): Unit = throw ex

  protected def printError(errtype: String, ex: SAXParseException): Unit =
    throw ex
}

object Validator extends App {

  private val logger = LoggerFactory.getLogger(getClass)

  val schemaLang = javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI
  val xsdFile = java.nio.file.Paths.get("conf/sitemap-v0.9.xsd")
  val readOnly = java.nio.file.StandardOpenOption.READ
  val inputStream = java.nio.file.Files.newInputStream(xsdFile, readOnly)
  val xsdStream = new javax.xml.transform.stream.StreamSource(inputStream)
  val schema = javax.xml.validation.SchemaFactory.newInstance(schemaLang).newSchema(xsdStream)

  val factory = javax.xml.parsers.SAXParserFactory.newInstance()
  factory.setNamespaceAware(true)
  factory.setSchema(schema)

  val validatingParser = factory.newSAXParser()
  inputStream.close()

  try {
    val sitemap = new scala.xml.factory.XMLLoader[scala.xml.Elem] {
      override def parser = validatingParser

      override def adapter =
        new scala.xml.parsing.NoBindingFactoryAdapter
          with ExceptionErrorHandler
    }.loadFile("conf/sitemap.xml")
  } catch {
    case t: Throwable =>
      logger.error("XML Validation has thrown an exception", t)
  }
}