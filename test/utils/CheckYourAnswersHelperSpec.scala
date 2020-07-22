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

package utils

import base.SpecBase
import controllers.routes
import models.UserAnswers
import pages.ValidXMLPage
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels._

import scala.xml.Elem

class CheckYourAnswersHelperSpec extends SpecBase {

  "Check Your Answers Helper" - {

    val userAnswers = UserAnswers(userAnswersId)
      .set(ValidXMLPage, "file-name.xml")
      .success.value

    lazy val fileContent = Row(
      key = Key(msg"checkYourAnswers.uploadedFile", classes = Seq("govuk-!-width-one-third")),
      value = Value(lit"file-name.xml"),
      actions = List(
        Action(
          content = msg"site.edit",
          href = routes.UploadFormController.onPageLoad().url,
          visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"checkYourAnswers.uploadedFile")),
          attributes = Map("id" -> "change-link")
        )
      )
    )

    val helper: CheckYourAnswersHelper = new CheckYourAnswersHelper(userAnswers)

    "must return new arrangement content when import instruction is DAC6NEW" in {

      val mockXML: Elem =
        <DAC6_Arrangement>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          </DAC6Disclosures>
        </DAC6_Arrangement >

      helper.displaySummaryFromInstruction(mockXML) mustBe Seq(fileContent,
        Row(
          key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
          value = Value(msg"checkYourAnswers.new.text", classes = Seq("new-arrangement-text")))
      )
    }

    "must return arrangement ID & additional information content when import instruction is DAC6ADD" in {

      val mockXML: Elem =
        <DAC6_Arrangement>
          <ArrangementID>GBA20200701AAA000</ArrangementID>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          </DAC6Disclosures>
        </DAC6_Arrangement >

      helper.displaySummaryFromInstruction(mockXML) mustBe Seq(fileContent,
        Row(
          key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
          value = Value(msg"checkYourAnswers.additional.text".withArgs("GBA20200701AAA000"), classes = Seq("additional-disclosure-text"))
        ))
    }

    "must return arrangement ID, Disclosure ID & replacement content when import instruction is DAC6REP" in {

      val mockXML: Elem =
        <DAC6_Arrangement>
          <ArrangementID>GBA20200701AAA000</ArrangementID>
          <DAC6Disclosures>
            <DisclosureID>GBD20200701AAA001</DisclosureID>
            <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
          </DAC6Disclosures>
        </DAC6_Arrangement >

      helper.displaySummaryFromInstruction(mockXML) mustBe Seq(fileContent,
        Row(
          key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
          value = Value(msg"checkYourAnswers.replacement.text".withArgs("GBD20200701AAA001", "GBA20200701AAA000"), classes = Seq("replacement-disclosure-text"))
        ))
    }
  }
}
