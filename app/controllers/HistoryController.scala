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

import connectors.CrossBorderArrangementsConnector
import controllers.actions.IdentifierAction
import forms.SearchDisclosuresFormProvider
import helpers.ViewHelper
import javax.inject.Inject
import models.SubmissionHistory
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

class HistoryController @Inject()(
                                   identify: IdentifierAction,
                                   crossBorderArrangementsConnector: CrossBorderArrangementsConnector,
                                   val controllerComponents: MessagesControllerComponents,
                                   renderer: Renderer,
                                   viewHelper: ViewHelper,
                                   formProvider: SearchDisclosuresFormProvider
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = identify.async { implicit request =>

    {for {
      retrievedDetails <- crossBorderArrangementsConnector.retrievePreviousSubmissions(request.enrolmentID)
      context = Json.obj("disclosuresTable" -> viewHelper.buildDisclosuresTable(retrievedDetails))
    } yield {
      renderer.render("submissionHistory.njk", context).map(Ok(_))
    }}.flatten
  }

  def onSearch: Action[AnyContent] = identify.async { implicit request =>

    val form = formProvider()

    form.bindFromRequest().fold(
      _ => {
        val context = Json.obj("disclosuresTable" -> viewHelper.buildDisclosuresTable(SubmissionHistory(Seq())))
        renderer.render("submissionHistory.njk", context).map(BadRequest(_))
      },
      searchCriteria => {
        for {
          retrievedDetails <- crossBorderArrangementsConnector.searchDisclosures(searchCriteria)
          context = Json.obj("disclosuresTable" -> viewHelper.buildDisclosuresTable(retrievedDetails))
        } yield {
          renderer.render("submissionHistory.njk", context).map(Ok(_))
        }
      }.flatten
    )
  }

}
