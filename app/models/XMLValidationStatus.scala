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

package models

import play.api.libs.json._
import play.api.libs.functional.syntax._


case class ValidationSuccess(downloadUrl : String) extends XMLValidationStatus

object ValidationSuccess {
  implicit val format = Json.format[ValidationSuccess]
}

case class SaxParseError(lineNumber: Int, errorMessage: String) {
  def toGenericError: GenericError = GenericError(errorMessage, Some(lineNumber))

}

object SaxParseError {
  implicit val format = Json.format[SaxParseError]
}

case class GenericError(errorMessage: String, lineNumber: Option[Int])

object GenericError {
  implicit val format = Json.format[GenericError]
}

case class ValidationFailure(error: Seq[SaxParseError]) extends XMLValidationStatus

object ValidationFailure {
  implicit val format = Json.format[ValidationFailure]
}

sealed trait XMLValidationStatus

object XMLValidationStatus {
  implicit val reads = new Reads[XMLValidationStatus] {
    override def reads(json: JsValue): JsResult[XMLValidationStatus] = json \ "downloadUrl" match {
      case JsDefined(_) => implicitly[Reads[ValidationSuccess]].reads(json)
      case JsUndefined() => implicitly[Reads[ValidationFailure]].reads(json)
    }
  }

  implicit val writes: Writes[XMLValidationStatus] = Writes {
    case ValidationSuccess(downloadUrl) => Json.obj(
      "downloadUrl" -> JsString(downloadUrl),
      "_type" -> "ValidationSuccess"
    )
    case ValidationFailure (error) => Json.obj(
      "error" -> JsArray(error.map(Json.toJson[SaxParseError](_))),
      "_type" -> "ValidationFailure"
    )
  }
}

