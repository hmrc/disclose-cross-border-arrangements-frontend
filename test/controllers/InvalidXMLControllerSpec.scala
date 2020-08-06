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

package controllers

import base.SpecBase
import models.{GenericError, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{GenericErrorPage, InvalidXMLPage}
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels._
import utils.CheckYourAnswersHelper

import scala.concurrent.Future

class InvalidXMLControllerSpec extends SpecBase with MockitoSugar {

  "InvalidXMLController" - {

    "return OK and the correct view for a GET" in {

      val mockErrors = Seq(GenericError(1, "test"))
      val mockfileName = "fileName.xml"

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val userAnswers = UserAnswers(userAnswersId)
        .set(InvalidXMLPage, mockfileName)
        .success.value
        .set(GenericErrorPage, mockErrors)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      val request = FakeRequest(GET, routes.InvalidXMLController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, request).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual "invalidXML.njk"
      jsonCaptor.getValue.value.get("fileName") mustBe Some(Json.toJson("fileName.xml"))

      application.stop()
    }

    "throw Exception and redirect to Internal Server Error page when errors or fileName is missing" in {

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val request = FakeRequest(GET, routes.InvalidXMLController.onPageLoad().url)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val result = route(application, request).value

      an[RuntimeException] mustBe thrownBy {
        status(result) mustEqual OK

        verify(mockRenderer, times(1)).render(templateCaptor.capture(), any())(any())

        templateCaptor.getValue mustEqual "internalServerError.njk"
      }

      application.stop()
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
