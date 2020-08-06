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
import models.{GenericError, UserAnswers}
import pages.{InvalidXMLPage, ValidXMLPage}
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Table.Cell
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

      "must return new arrangement content when import instruction is DAC6NEW" in {

        helper.displaySummaryFromInstruction("DAC6NEW", arrangementID, disclosureID) mustBe Seq(fileContent,
          Row(
            key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(msg"checkYourAnswers.new.text",
              classes = Seq("new-arrangement-text")))
        )
      }

      "must return arrangement ID & additional information content when import instruction is DAC6ADD" in {

        helper.displaySummaryFromInstruction("DAC6ADD", arrangementID, disclosureID) mustBe Seq(fileContent,
          Row(
            key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(msg"checkYourAnswers.additional.text".withArgs(arrangementID),
              classes = Seq("additional-disclosure-text"))
          ))
      }

      "must return arrangement ID, Disclosure ID & replacement content when import instruction is DAC6REP" in {

        helper.displaySummaryFromInstruction("DAC6REP", arrangementID, disclosureID) mustBe Seq(fileContent,
          Row(
            key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(msg"checkYourAnswers.replacement.text".withArgs(disclosureID, arrangementID),
              classes = Seq("replacement-disclosure-text"))
          ))
      }

      "must return arrangement ID, Disclosure ID & deletion content when import instruction is DAC6DEL" in {

        helper.displaySummaryFromInstruction("DAC6DEL", arrangementID, disclosureID) mustBe Seq(fileContent,
          Row(
            key = Key(msg"checkYourAnswers.deleteFile", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(msg"checkYourAnswers.deleteDisclosure.text".withArgs(disclosureID, arrangementID),
              classes = Seq("delete-disclosure-text"))
          )
        )
      }
    }

    "mapErrorsToTable" - {

      val userAnswers = UserAnswers(userAnswersId)
        .set(InvalidXMLPage, "file-name.xml")
        .success.value

      val helper: CheckYourAnswersHelper = new CheckYourAnswersHelper(userAnswers)


      val head: Seq[Cell] = Seq(
        Cell(msg"invalidXML.table.heading1", classes = Seq("govuk-!-width-one-quarter", "govuk-table__header")),
        Cell(msg"invalidXML.table.heading2", classes = Seq("govuk-!-font-weight-bold"))
      )

      "must return a table containing a row with a line number & error when given a generic error" in {

        val mockSingleError: Seq[GenericError] = Seq(GenericError(11, "Enter your cats name in meow format"))

        helper.mapErrorsToTable(mockSingleError) mustBe Table(
          head = head,
          rows = Seq(Seq(
              Cell(msg"11", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber")),
              Cell(msg"Enter your cats name in meow format", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage"))
            )),
          caption = Some(msg"invalidXML.h3"),
          attributes = Map("id" -> "errorTable", "aria-describedby" -> messages("invalidXML.h3"))
          )
      }

      "must return a table containing multiple rows with line numbers & errors when a given Generic Error" in {

        val mockMultiError: Seq[GenericError] =
          Seq(GenericError(22, "Enter cat years only"),
            GenericError(33,"Incorrect number of cat legs"))

        helper.mapErrorsToTable(mockMultiError) mustBe Table(
          head = head,
          rows = Seq(Seq(
              Cell(msg"22", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber")),
              Cell(msg"Enter cat years only", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage"))
            ),
            Seq(
              Cell(msg"33", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber")),
              Cell(msg"Incorrect number of cat legs", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage"))
            )),
          caption = Some(msg"invalidXML.h3"),
          attributes = Map("id" -> "errorTable", "aria-describedby" -> messages("invalidXML.h3"))
          )
      }

      "must return a table containing multiple rows in correct order with line numbers & errors when a given Generic Error" in {

        val mockMultiError: Seq[GenericError] =
          Seq(GenericError(33,"Incorrect number of cat legs"),
            GenericError(48, "You gotta be kitten me"),
            GenericError(22,"Enter cat years only"))

        helper.mapErrorsToTable(mockMultiError) mustBe Table(
          head = head,
          rows = Seq(Seq(
              Cell(msg"22", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber")),
              Cell(msg"Enter cat years only", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage"))),
            Seq(
              Cell(msg"33", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber")),
              Cell(msg"Incorrect number of cat legs", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage"))),
            Seq(
              Cell(msg"48", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber")),
              Cell(msg"You gotta be kitten me", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage"))
            )),
          caption = Some(msg"invalidXML.h3"),
          attributes = Map("id" -> "errorTable", "aria-describedby" -> messages("invalidXML.h3"))
          )
      }
    }

  }
}
