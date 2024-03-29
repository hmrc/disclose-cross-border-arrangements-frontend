/*
 * Copyright 2023 HM Revenue & Customs
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

package models

import play.api.libs.json._

case class Dac6MetaData(importInstruction: String,
                        arrangementID: Option[String] = None,
                        disclosureID: Option[String] = None,
                        disclosureInformationPresent: Boolean,
                        initialDisclosureMA: Boolean,
                        messageRefId: String
)

object Dac6MetaData {
  implicit val format = Json.format[Dac6MetaData]
}

case class SaxParseError(lineNumber: Int, errorMessage: String)

object SaxParseError {
  implicit val format = Json.format[SaxParseError]
}

case class GenericError(lineNumber: Int, messageKey: String)

object GenericError {

  implicit def orderByLineNumber[A <: GenericError]: Ordering[A] =
    Ordering.by(
      ge => (ge.lineNumber, ge.messageKey)
    )

  implicit val format = Json.format[GenericError]

}

case class ValidationErrors(errors: Seq[GenericError], dac6MetaData: Option[Dac6MetaData])

object ValidationErrors {
  implicit val format = Json.format[ValidationErrors]
}

sealed trait UploadSubmissionValidationResult

object UploadSubmissionValidationResult {

  implicit val validationWrites = new Format[UploadSubmissionValidationResult] {

    override def reads(json: JsValue): JsResult[UploadSubmissionValidationResult] =
      json
        .validate[UploadSubmissionValidationSuccess]
        .orElse(
          json.validate[UploadSubmissionValidationFailure]
        )

    override def writes(o: UploadSubmissionValidationResult): JsValue = o match {
      case m @ UploadSubmissionValidationSuccess(_) => UploadSubmissionValidationSuccess.format.writes(m)
      case m @ UploadSubmissionValidationFailure(_) => UploadSubmissionValidationFailure.format.writes(m)
    }
  }
}

case class UploadSubmissionValidationSuccess(dac6MetaData: Dac6MetaData) extends UploadSubmissionValidationResult

object UploadSubmissionValidationSuccess {
  implicit val format: OFormat[UploadSubmissionValidationSuccess] = Json.format[UploadSubmissionValidationSuccess]
}

case class UploadSubmissionValidationFailure(validationErrors: ValidationErrors) extends UploadSubmissionValidationResult

object UploadSubmissionValidationFailure {
  implicit val format: OFormat[UploadSubmissionValidationFailure] = Json.format[UploadSubmissionValidationFailure]
}
