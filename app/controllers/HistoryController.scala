/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.actions.{DataRetrievalAction, IdentifierAction}
import forms.SearchDisclosuresFormProvider
import helpers.ViewHelper
import javax.inject.Inject
import models.{NormalMode, UserAnswers}
import navigation.Navigator
import pages.HistoryPage
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.{ExecutionContext, Future}

class HistoryController @Inject() (
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  sessionRepository: SessionRepository,
  crossBorderArrangementsConnector: CrossBorderArrangementsConnector,
  val controllerComponents: MessagesControllerComponents,
  renderer: Renderer,
  viewHelper: ViewHelper,
  formProvider: SearchDisclosuresFormProvider,
  navigator: Navigator
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with NunjucksSupport {

  def onPageLoad: Action[AnyContent] = identify.async {
    implicit request =>
      {
        for {
          retrievedDetails <- crossBorderArrangementsConnector.retrievePreviousSubmissions(request.enrolmentID)
          context = Json.obj("disclosuresTable" -> viewHelper.buildDisclosuresTable(retrievedDetails))
        } yield renderer.render("submissionHistory.njk", context).map(Ok(_))
      }.flatten
  }

  def onSearch: Action[AnyContent] = (identify andThen getData).async {
    implicit request =>
      val form = formProvider()

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            {

              for {
                retrievedDetails <- crossBorderArrangementsConnector.retrievePreviousSubmissions(request.enrolmentID)
                context = Json.obj(
                  "form"             -> formWithErrors,
                  "disclosuresTable" -> viewHelper.buildDisclosuresTable(retrievedDetails)
                )
              } yield renderer.render("submissionHistory.njk", context).map(BadRequest(_))
            }.flatten,
          searchCriteria =>
            for {
              updatedAnswers <- Future.fromTry(UserAnswers(request.internalId).set(HistoryPage, searchCriteria))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HistoryPage, NormalMode, updatedAnswers))
        )
  }

}
