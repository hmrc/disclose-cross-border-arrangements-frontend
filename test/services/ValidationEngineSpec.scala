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
import models.{SaxParseError, Validation, ValidationFailure, ValidationSuccess}
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import scala.xml.{Elem, NodeSeq}

class ValidationEngineSpec  extends SpecBase with MockitoSugar {

  val xsdError = "xsd-error"
  val businessRulesError = "business-rules-error"
  val lineNumber = 1

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
        when(mockXmlValidationService.validateXml(any())).thenReturn((elem, ValidationSuccess(source)))
        validationEngine.validateFile(source) mustBe ValidationSuccess(source)
      }

      "must return ValidationFailure for file which fails xsd validation" in new SetUp {
        val saxParseError = SaxParseError(lineNumber, xsdError)
        when(mockXmlValidationService.validateXml(any())).thenReturn((elem, ValidationFailure(Seq(saxParseError))))

        validationEngine.validateFile(source) mustBe ValidationFailure(Seq(saxParseError))
      }

      "must return ValidationFailure for file which fails business rules validation" in new SetUp {
        override val doesFileHaveBusinessErrors = true

        when(mockXmlValidationService.validateXml(any())).thenReturn((elem, ValidationSuccess(source)))

        val expectedErrors = Seq(SaxParseError(lineNumber, businessRulesError))
        validationEngine.validateFile(source) mustBe ValidationFailure(expectedErrors)
      }


      "must return a ValidationFailure with a combined list of errors for a for file which " +
        "fails both xsd checks and business rules validation" in new SetUp {
        override val doesFileHaveBusinessErrors = true

        val saxParseError = SaxParseError(lineNumber, xsdError)
        when(mockXmlValidationService.validateXml(any())).thenReturn((elem, ValidationFailure(Seq(saxParseError))))

        val expectedErrors = Seq(SaxParseError(lineNumber, xsdError), SaxParseError(lineNumber, businessRulesError))
        validationEngine.validateFile(source) mustBe ValidationFailure(expectedErrors)
      }

      "must return a ValidationFailure with only xmlErrors if Business Rules check is not required" in new SetUp {
        override val doesFileHaveBusinessErrors = true

        val saxParseError = SaxParseError(lineNumber, xsdError)
        when(mockXmlValidationService.validateXml(any())).thenReturn((elem, ValidationFailure(Seq(saxParseError))))

        val expectedErrors = Seq(SaxParseError(lineNumber, xsdError))
        validationEngine.validateFile(source, businessRulesCheckRequired = false) mustBe ValidationFailure(expectedErrors)
      }

   }

  }

}
