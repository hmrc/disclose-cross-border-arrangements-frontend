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

package utils

import base.SpecBase
import controllers.routes
import models.UserAnswers
import pages.ValidXMLPage
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels._

class CheckYourAnswersHelperSpec extends SpecBase {

  "Check Your Answers Helper" - {

    "displaySummaryFromInstruction" - {

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
      val arrangementID = "GBA20200701AAA000"
      val disclosureID = "GBD20200701AAA001"
      val messageRefID = "GBXADAC0001234567AAA00101"

      "must return new arrangement content when import instruction is DAC6NEW" in {

        helper.displaySummaryFromInstruction("DAC6NEW", arrangementID, disclosureID, messageRefID) mustBe Seq(fileContent,
          Row(
            key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(msg"checkYourAnswers.new.text",
              classes = Seq("new-arrangement-text"))),
          Row(
            key = Key(msg"checkYourAnswers.messageRefID.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(lit"$messageRefID", classes = Seq("messageRefID"))
          )
        )
      }

      "must return arrangement ID & additional information content when import instruction is DAC6ADD" in {

        helper.displaySummaryFromInstruction("DAC6ADD", arrangementID, disclosureID, messageRefID) mustBe Seq(fileContent,
          Row(
            key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(msg"checkYourAnswers.additional.text".withArgs(arrangementID),
              classes = Seq("additional-disclosure-text"))
          ),
          Row(
            key = Key(msg"checkYourAnswers.messageRefID.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(lit"$messageRefID", classes = Seq("messageRefID"))
          )
        )
      }

      "must return arrangement ID, Disclosure ID & replacement content when import instruction is DAC6REP" in {

        helper.displaySummaryFromInstruction("DAC6REP", arrangementID, disclosureID, messageRefID) mustBe Seq(fileContent,
          Row(
            key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(msg"checkYourAnswers.replacement.text".withArgs(disclosureID, arrangementID),
              classes = Seq("replacement-disclosure-text"))
          ),
          Row(
            key = Key(msg"checkYourAnswers.messageRefID.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(lit"$messageRefID", classes = Seq("messageRefID"))
          )
        )
      }

      "must return arrangement ID, Disclosure ID & deletion content when import instruction is DAC6DEL" in {

        helper.displaySummaryFromInstruction("DAC6DEL", arrangementID, disclosureID, messageRefID) mustBe Seq(fileContent,
          Row(
            key = Key(msg"checkYourAnswers.deleteFile", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(msg"checkYourAnswers.deleteDisclosure.text".withArgs(disclosureID, arrangementID),
              classes = Seq("delete-disclosure-text"))
          ),
          Row(
            key = Key(msg"checkYourAnswers.messageRefID.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(lit"$messageRefID", classes = Seq("messageRefID"))
          )
        )
      }
    }
  }
}
