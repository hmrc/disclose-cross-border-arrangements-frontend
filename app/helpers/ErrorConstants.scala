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

package helpers
import models.SaxParseError

import scala.util.matching.Regex

sealed trait ErrorMessageInfo

case class MissingAttributeInfo(element: String, attribute: String) extends ErrorMessageInfo
case class InvalidEnumAttributeInfo(element: String, attribute: String) extends ErrorMessageInfo
case class MissingElementInfo(element: String) extends ErrorMessageInfo
case class MaxLengthErrorInfo(element: String, allowedLength: String) extends ErrorMessageInfo
case class InvalidEnumErrorInfo(element: String) extends ErrorMessageInfo

trait ErrorConstants {

  case class RequestValues(url: String, method: String, status: String, remoteAddress: String)

  def extractRequestValues(data: String): Option[RequestValues] = {
    val r = """Request URL: (http.*?) Request Method: (GET|POST|PUT|DELETE) Status Code: ([0-9]{3}) Remote Address: (.*?)""".stripMargin.r

    data match {
      case r(url, method, status, remoteAddress) =>
        Some (RequestValues(url, method, status, remoteAddress))
      case _ => None
    }
  }

  def extractMissingAttributeValues(errorMessage: String): Option[MissingAttributeInfo] = {
    val format = """cvc-complex-type.4: Attribute '(.*?)' must appear on element '(.*?)'.""".stripMargin.r

    errorMessage match {
      case format(attribute, element) =>
        Some (MissingAttributeInfo(element, attribute))
      case _ => None
    }
  }

  def extractInvalidEnumAttributeValues(errorMessage1: String, errorMessage2: String): Option[InvalidEnumAttributeInfo] = {
    val formatOfFirstError = """cvc-enumeration-valid: Value '(.*?)' is not facet-valid with respect to enumeration '(.*?)'. It must be a value from the enumeration.""".stripMargin.r
    val formatOfSecondError = """cvc-attribute.3: The value '(.*?)' of attribute '(.*?)' on element '(.*?)' is not valid with respect to its type, '(.*?)'.""".stripMargin.r

    errorMessage1 match {
      case formatOfFirstError(_, _) =>
        errorMessage2 match {
          case formatOfSecondError(_, attribute, element, _) =>
            Some(InvalidEnumAttributeInfo(element, attribute))
          case _ => None
        }
          case _ => None

    }
  }

  def extractMissingElementValues(errorMessage1: String, errorMessage2: String): Option[MissingElementInfo] = {
    val formatOfFirstError = """cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max400_Type'.""".stripMargin.r
    val formatOfSecondError = """cvc-type.3.1.3: The value '' of element '(.*?)' is not valid.""".stripMargin.r

    errorMessage1 match {
      case formatOfFirstError() =>
        errorMessage2 match {
          case formatOfSecondError(element) =>
            Some(MissingElementInfo(element))
          case _ => None
        }
          case _ =>  None
    }
  }

  def extractMaxLengthErrorValues(errorMessage1: String, errorMessage2: String): Option[MaxLengthErrorInfo] = {
    val formatOfFirstError = """cvc-maxLength-valid: Value '(.*?)' with length = '(.*?)' is not facet-valid with respect to maxLength '(.*?)' for type '(.*?)'.""".stripMargin.r
    val formatOfSecondError = """cvc-type.3.1.3: The value '(.*?)' of element '(.*?)' is not valid.""".stripMargin.r


    errorMessage1 match {
      case formatOfFirstError(_, _, allowedLength, _) =>
        errorMessage2 match {
          case formatOfSecondError(_, element) =>
            Some(MaxLengthErrorInfo(element, allowedLength))
          case _ => None
        }
          case _ =>  None
    }
  }

  def extractEnumErrorValues(errorMessage1: String, errorMessage2: String): Option[InvalidEnumErrorInfo] = {
    val formatOfFirstError = """cvc-enumeration-valid: Value '(.*?)' is not facet-valid with respect to enumeration '(.*?)'. It must be a value from the enumeration.""".stripMargin.r
    val formatOfSecondError = """cvc-type.3.1.3: The value '(.*?)' of element '(.*?)' is not valid.""".stripMargin.r

    errorMessage1 match {
      case formatOfFirstError(_, _) =>
        errorMessage2 match {
          case formatOfSecondError(_, element) =>
            Some(InvalidEnumErrorInfo(element))
          case _ =>  println("11111111")
                     None
        }
          case _ => println("22222222222")
                    None
    }
  }



  val ERROR_TYPE_PATTERN = """cvc-maxLength-valid"""
  val MAX_LENGTH_PATTERN = """ maxLength ('[0-9]{3,4}')"""

  val ELEMENT_NAME_PATTERN = """of element '(.*?)'"""

  val ATTRIBUTE_NAME_PATTERN = """on element '(.*?)'"""

  val INVALID_ATTRIBUTE_ERROR = """cvc-attribute.3"""
  val MISSING_ATTRIBUTE_ERROR = """cvc-complex-type.4"""
  val MAX_LENGTH_ERROR = "cvc-maxLength-valid"
  val MISSING_VALUE_ERROR = "cvc-minLength-valid"
  val INVALID_ENUM_ERROR = "cvc-enumeration-valid"

  val ERROR_TYPES = List (MISSING_ATTRIBUTE_ERROR, INVALID_ATTRIBUTE_ERROR, INVALID_ENUM_ERROR, MAX_LENGTH_ERROR, MISSING_VALUE_ERROR)


}

