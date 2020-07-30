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

import base.SpecBase
import cats.data.ReaderT
import cats.implicits._
import helpers.LineNumberHelper
import models.{GenericError, SaxParseError, Validation, ValidationFailure, ValidationSuccess}
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.mutable.ListBuffer
import scala.xml.{Elem, NodeSeq}

class ValidationEngineSpec  extends SpecBase with MockitoSugar {

  val xsdError = "xsd-error"
  val businessRulesError = "business-rules-error"
  val lineNumber = 0
  val noErrors: ListBuffer[SaxParseError] = ListBuffer()

  val addressError1 = SaxParseError(20, "cvc-minLength-valid: Value '' with length = '0' is " +
    "not facet-valid with respect to minLength '1' for type 'StringMin1Max400_Type'.")

  val addressError2 = SaxParseError(20, "cvc-type.3.1.3: The value '' of element 'Street' is not valid.")

  val cityError1 = SaxParseError(27, "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max400_Type'.")
  val cityError2 = SaxParseError(27, "cvc-type.3.1.3: The value '' of element 'City' is not valid.")

  val lengthError1 = SaxParseError(116, "cvc-maxLength-valid: Value 'qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq' with length = '685' is not facet-valid with respect to maxLength '400' for type 'StringMin1Max400_Type'.")
  val lengthError2 = SaxParseError(116, "cvc-type.3.1.3: The value 'qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq' of element 'BuildingIdentifier' is not valid.")


  val missingAddressErrors = ListBuffer(addressError1, addressError2)
  val missingCityErrors = ListBuffer(cityError1, cityError2)


  trait SetUp {
    val doesFileHaveBusinessErrors = false

    val mockXmlValidationService: XMLValidationService = mock[XMLValidationService]

    val lineNumberHelper: LineNumberHelper = new LineNumberHelper

    val mockBusinessRuleValidationService: BusinessRuleValidationService = new BusinessRuleValidationService {

      val dummyReader: ReaderT[Option, NodeSeq, Boolean] =
        ReaderT[Option, NodeSeq, Boolean](xml => {
          Some(!doesFileHaveBusinessErrors)
        })

      def dummyValidation(): ReaderT[Option, NodeSeq, Validation] = {
        for {
          result <- dummyReader
        } yield
          Validation(
            key = businessRulesError,
            value = result
          )
      }

      override def validateFile(): ReaderT[Option, NodeSeq, Seq[Validation]] = {
        for {
          v1 <- dummyValidation()
        } yield
          Seq(v1).filterNot(_.value)
      }
    }

    val validationEngine = new ValidationEngine(mockXmlValidationService, mockBusinessRuleValidationService, lineNumberHelper)

    val source = "src"
    val elem: Elem = <dummyElement>Test</dummyElement>
  }
  "ValidationEngine" - {
    "ValidateXml" -{

      "must return ValidationSuccess for valid file" in new SetUp {
        when(mockXmlValidationService.validateXml(any())).thenReturn((elem, noErrors))
        validationEngine.validateFile(source) mustBe ValidationSuccess(source)
      }

//      "must return ValidationFailure for file which fails xsd validation" in new SetUp {
//        val saxParseError = SaxParseError(lineNumber, xsdError)
//        when(mockXmlValidationService.validateXml(any())).thenReturn((elem,  ListBuffer(saxParseError)))
//
//        val expectedError = Seq(GenericError(lineNumber, xsdError))
//
//        validationEngine.validateFile(source) mustBe ValidationFailure(expectedError)
//      }


      "must return ValidationFailure for file which multiple pieces of mandatory information missing" in new SetUp {

        when(mockXmlValidationService.validateXml(any())).thenReturn((elem,
          ListBuffer(addressError1, addressError2, cityError1, cityError2)))

        val expectedErrors = Seq(GenericError(20, "Enter a Street"), GenericError(27, "Enter a City"))

        validationEngine.validateFile(source) mustBe ValidationFailure(expectedErrors)
      }

      "must return ValidationFailure for file missing mandatory attributes" in new SetUp {

        val missingAttributeError = SaxParseError(175,"cvc-complex-type.4: Attribute 'currCode' must appear on element 'Amount'.")

        when(mockXmlValidationService.validateXml(any())).thenReturn((elem,
          ListBuffer(missingAttributeError)))

        val expectedErrors = Seq(GenericError(175, "Enter an Amount currCode"))

        validationEngine.validateFile(source) mustBe ValidationFailure(expectedErrors)
      }


      "must return ValidationFailure for file where element is too long" in new SetUp {

       when(mockXmlValidationService.validateXml(any())).thenReturn((elem,
          ListBuffer(lengthError1, lengthError2)))

        val expectedErrors = Seq(GenericError(116, "BuildingIdentifier must be 400 characters or less"))

        validationEngine.validateFile(source) mustBe ValidationFailure(expectedErrors)
      }

      "must return ValidationFailure with generic error message if parse error is not in an expected format" in new SetUp {

        val randomParseError = SaxParseError(lineNumber, xsdError)
        when(mockXmlValidationService.validateXml(any())).thenReturn((elem,
          ListBuffer(randomParseError)))

        val expectedErrors = Seq(GenericError(lineNumber, "There is something wrong with this line"))

        validationEngine.validateFile(source) mustBe ValidationFailure(expectedErrors)
      }

      "must return ValidationFailure for file which fails business rules validation" in new SetUp {
        override val doesFileHaveBusinessErrors = true

        when(mockXmlValidationService.validateXml(any())).thenReturn((elem, noErrors))

        val expectedErrors = Seq(GenericError(lineNumber, businessRulesError))
        validationEngine.validateFile(source) mustBe ValidationFailure(expectedErrors)
      }


      "must return a ValidationFailure with a combined list of errors for a for file which " +
        "fails both xsd checks and business rules validation" in new SetUp {
        override val doesFileHaveBusinessErrors = true

        when(mockXmlValidationService.validateXml(any())).thenReturn((elem, missingAddressErrors))

        val expectedErrors = Seq(GenericError(20, "Enter a Street"), GenericError(lineNumber, businessRulesError))
        validationEngine.validateFile(source) mustBe ValidationFailure(expectedErrors)
      }

      "must return a ValidationFailure with only xmlErrors if Business Rules check is not required" in new SetUp {
        override val doesFileHaveBusinessErrors = true

        when(mockXmlValidationService.validateXml(any())).thenReturn((elem, missingAddressErrors))

        val expectedErrors = Seq(GenericError(20, "Enter a Street"))
        validationEngine.validateFile(source, businessRulesCheckRequired = false) mustBe ValidationFailure(expectedErrors)
      }

   }

  }

}
