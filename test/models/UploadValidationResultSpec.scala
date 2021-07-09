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

///*
// * Copyright 2021 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
// TODO - REMOVE FILE DAC6-858
//package models
//
//import base.SpecBase
//import play.api.libs.json.Json
//
//class UploadValidationResultSpec extends SpecBase {
//
//  "XML Validation Status" - {
//    "must marshall correctly for validation success" in {
//      val json =
//        """
//          |{
//          | "downloadUrl": "myDownload"
//          |}""".stripMargin
//
//      val expectedResult = ValidationSuccess("myDownload")
//
//      Json.parse(json).as[UploadValidationResult] mustBe expectedResult
//    }
//
//    "must marshall correctly for validation failure" in {
//      val json =
//        """
//          |{
//          | "errors": [{
//          |     "lineNumber": 50,
//          |     "messageKey": "It's an error"
//          |   },
//          |   {
//          |     "lineNumber": 52,
//          |     "messageKey": "Oh no!"
//          |   }
//          | ]
//          |}""".stripMargin
//
//      val expectedResult = ValidationFailure(
//        Seq(
//          GenericError(50, "It's an error"),
//          GenericError(52, "Oh no!")
//        ))
//
//      Json.parse(json).as[UploadValidationResult] mustBe expectedResult
//    }
//  }
//
//}
