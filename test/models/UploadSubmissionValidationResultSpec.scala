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

import base.SpecBase
import play.api.libs.json.{JsSuccess, Json}

class UploadSubmissionValidationResultSpec extends SpecBase {

  val mockMetaData = Dac6MetaData("DAC6NEW", disclosureInformationPresent = true, initialDisclosureMA = false, messageRefId = "messageRefId")

  "UploadSubmissionValidationResult" - {
    "must be able to read UploadSubmissionValidationSuccess from the trait" in {
      val jsonPayload =
        """
          |{
          |  "dac6MetaData": {
          |  "importInstruction": "DAC6NEW",
          |  "disclosureInformationPresent": true,
          |  "initialDisclosureMA": false,
          |  "messageRefId": "messageRefId"
          |  }
          |}""".stripMargin

      Json.parse(jsonPayload).validate[UploadSubmissionValidationResult] mustBe
        JsSuccess(UploadSubmissionValidationSuccess(mockMetaData))
    }

    "must be able to read UploadSubmissionValidationFailure from the trait" in {
      val jsonPayload =
        """
          |{"errors":[
          |{"lineNumber":1,"messageKey":"Some Error"}
          |]}""".stripMargin
      Json.parse(jsonPayload).validate[UploadSubmissionValidationResult] mustBe
        JsSuccess(UploadSubmissionValidationFailure(Seq(GenericError(1, "Some Error"))))
    }
  }
}
