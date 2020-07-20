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

import java.time.format.DateTimeFormatter

import controllers.routes
import models.UserAnswers
import pages.ValidXMLPage
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels._

import scala.xml.Elem

class CheckYourAnswersHelper(userAnswers: UserAnswers)(implicit messages: Messages) {

  private def yesOrNo(answer: Boolean)(implicit messages: Messages): Html = {
    if (answer) {
      HtmlFormat.escape(messages("site.yes"))
    } else {
      HtmlFormat.escape(messages("site.no"))
    }
  }

  def uploadedFile: Option[Row] = userAnswers.get(ValidXMLPage) map {
    fileName =>
      Row(
        key = Key(msg"checkYourAnswers.uploadedFile", classes = Seq("govuk-!-width-one-third")),
        value = Value(lit"$fileName"),
        actions = List(
          Action(
            content = msg"site.edit",
            href = routes.UploadFormController.onPageLoad().url,
            visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"checkYourAnswers.uploadedFile"))
          )
        )
      )
  }

  def displaySummaryFromInstruction(xml: Elem) : Seq[SummaryList.Row] = {
    val arrangementID = (xml \ "ArrangementID").text
    val importInstruction = (xml \ "DAC6Disclosures" \ "DisclosureImportInstruction").text
    val disclosureID = (xml \ "DAC6Disclosures" \ "DisclosureID").text

    importInstruction match {
      case "DAC6NEW" => Seq(uploadedFile.get, Row(
        key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third")),
        value = Value(msg"checkYourAnswers.new.text")
        )
      )
      case "DAC6ADD" => Seq(uploadedFile.get, Row(
        key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third")),
        value = Value(msg"checkYourAnswers.additional.text".withArgs(arrangementID))
        )
      )
      case "DAC6REP" => Seq(uploadedFile.get, Row(
        key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third")),
        value = Value(msg"checkYourAnswers.replacement.text".withArgs(disclosureID, arrangementID))
        )
      )
      case _ => Seq() //TODO - add DAC6DEL to cover all scenarios
    }
  }
}






object CheckYourAnswersHelper {

  private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
}
