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
import generators.Generators
import helpers.JsonFixtures.{updateSubscriptionResponseJson, updateSubscriptionResponsePayload}
import models.subscription._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.{JsString, Json}

class UpdateSubscriptionForDACResponseSpec extends SpecBase with Generators {

  val returnParameters: ReturnParameters = ReturnParameters("Name", "Value")
  val responseCommon: ResponseCommon = ResponseCommon(
    status = "OK", statusText = None, processingDate = "2020-09-23T16:12:11Z", returnParameters = Some(Seq(returnParameters)))

  def updateSubscriptionResponse(safeID: String): UpdateSubscriptionForDACResponse = UpdateSubscriptionForDACResponse(
    UpdateSubscription(
      responseCommon = responseCommon,
      responseDetail = ResponseDetailForUpdate(safeID)
    )
  )

  "UpdateSubscriptionForDACResponse" - {
     "must deserialise UpdateSubscriptionForDACResponse" in {
       forAll(validDacID) {
         safeID =>
         Json.parse(updateSubscriptionResponsePayload(JsString(safeID)))
           .validate[UpdateSubscriptionForDACResponse].get mustBe updateSubscriptionResponse(safeID)
       }
     }

    "must serialise UpdateSubscriptionForDACResponse" in {
      forAll(validDacID) {
        safeID =>
          Json.toJson(updateSubscriptionResponse(safeID)) mustBe updateSubscriptionResponseJson(safeID)
      }
    }
  }

}
