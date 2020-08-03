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

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.viewmodels.Html

class ViewHelperSpec extends SpecBase with MockitoSugar {

  val viewHelper = new ViewHelper

  "linkToHomePageText" - {

    "must return the correct HTML when given Valid JSValue" in {

      val homePageLink = Json.toJson("www.test.com")

      viewHelper.linkToHomePageText(homePageLink) mustBe Html(s"<a id='disclose-link' href=$homePageLink>" +
        s"disclose cross-border arrangements</a>.")

    }
  }
}
