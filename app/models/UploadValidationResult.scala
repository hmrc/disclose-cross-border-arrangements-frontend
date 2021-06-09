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

package models

import play.api.libs.json._

case class Dac6MetaData(importInstruction: String,
                        arrangementID: Option[String] = None,
                        disclosureID: Option[String] = None,
                        disclosureInformationPresent: Boolean,
                        initialDisclosureMA: Boolean,
                        messageRefId: String)

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
    Ordering.by(ge => (ge.lineNumber, ge.messageKey))

  implicit val format = Json.format[GenericError]
}

sealed trait UploadValidationResult

object UploadValidationResult {

  implicit val validationWrites = new Format[UploadValidationResult] {
    override def reads(json: JsValue): JsResult[UploadValidationResult] =
      json.validate[ValidationSuccess].orElse(
        json.validate[ValidationFailure]
      )

    override def writes(o: UploadValidationResult): JsValue = o match {
      case ValidationSuccess(metaData) => Json.obj(
        "metaData" -> metaData
      )

      case ValidationFailure(errors) => Json.obj(
        "error" -> JsArray(errors.map(Json.toJson[GenericError](_)))
      )
  }

//  implicit val reads = new Reads[UploadValidationResult] {
//    override def reads(json: JsValue): JsResult[UploadValidationResult] = {
//      json \ "downloadUrl" match {
//        case JsDefined(_) => implicitly[Reads[ValidationSuccess]].reads(json)
//        case JsUndefined() => implicitly[Reads[ValidationFailure]].reads(json)
//      }
//    }
  }

//  implicit val writes: Writes[UploadValidationResult] = Writes[UploadValidationResult] {
//    case ValidationSuccess(downloadUrl, metaData) => Json.obj(
//      "downloadUrl" -> downloadUrl,
//      "metaData" -> metaData,
//      "_type" -> "ValidationSuccess")
//
//    case ValidationFailure (error) => Json.obj(
//      "error" -> JsArray(error.map(Json.toJson[GenericError](_))),
//      "_type" -> "ValidationFailure"
//    )
//  }
}

case class ValidationFailure (errors: Seq[GenericError]) extends UploadValidationResult

object ValidationFailure {
  implicit val format: OFormat[ValidationFailure] = Json.format[ValidationFailure]
}

case class ValidationSuccess(metaData: Dac6MetaData) extends UploadValidationResult

object ValidationSuccess {
  implicit val format: OFormat[ValidationSuccess] = Json.format[ValidationSuccess]
}

