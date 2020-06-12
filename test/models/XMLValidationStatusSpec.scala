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

import base.SpecBase
import play.api.libs.json.Json

class XMLValidationStatusSpec extends SpecBase {

  "XML Validation Status" - {
    "must marshall correctly for validation success" in {
      val json =
        """
          |{
          | "downloadUrl": "myDownload"
          |}""".stripMargin

      val expectedResult = ValidationSuccess("myDownload")

      Json.parse(json).as[XMLValidationStatus] mustBe expectedResult
    }

    "must marshall correctly for validation failure" in {
      val json =
        """
          |{
          | "error": [{
          |     "lineNumber": 50,
          |     "errorMessage": "It's an error"
          |   },
          |   {
          |     "lineNumber": 52,
          |     "errorMessage": "Oh no!"
          |   }
          | ]
          |}""".stripMargin

      val expectedResult = ValidationFailure(
        Seq(
          GenericError("It's an error", Some(50)),
          GenericError("Oh no!", Some(52))
        ))

      Json.parse(json).as[XMLValidationStatus] mustBe expectedResult
    }
  }

}
