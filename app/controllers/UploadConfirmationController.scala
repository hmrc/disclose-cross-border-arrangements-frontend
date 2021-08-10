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

package controllers

import config.FrontendAppConfig
import controllers.actions._
import handlers.ErrorHandler
import helpers.ViewHelper
import pages.{Dac6MetaDataPage, GeneratedIDPage, UploadIDPage}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.Html

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class UploadConfirmationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  renderer: Renderer,
  errorHandler: ErrorHandler,
  viewHelper: ViewHelper,
  appConfig: FrontendAppConfig,
  contactRetrievalAction: ContactRetrievalAction
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData andThen contactRetrievalAction).async {
    implicit request =>
      val disclosureID: String = request.userAnswers.get(GeneratedIDPage).fold("")(_.disclosureID.getOrElse(""))

      val emailMessage = request.contacts match {
        case Some(contactDetails) if contactDetails.secondEmail.isDefined => contactDetails.contactEmail.get + " and " + contactDetails.secondEmail.get
        case Some(contactDetails)                                         => contactDetails.contactEmail.get
        case _                                                            => errorHandler.onServerError(request, new Exception("Contact details are missing"))
      }

      val xmlData = request.userAnswers.get(Dac6MetaDataPage).get

      if (disclosureID.isEmpty) {
        errorHandler.onServerError(request, throw new Exception("Disclosure ID is missing"))
      } else {
        val json = Json.obj(
          "messageRefID"       -> xmlData.messageRefId,
          "emailMessage"       -> emailMessage.toString,
          "disclosureID"       -> confirmationPanelText(disclosureID),
          "homePageLink"       -> viewHelper.linkToHomePageText(Json.toJson(appConfig.discloseArrangeLink)),
          "betaFeedbackSurvey" -> viewHelper.surveyLinkText(Json.toJson(appConfig.betaFeedbackUrl))
        )

        for {
          updatedAnswers <- request.userAnswers.remove(UploadIDPage)
        } yield sessionRepository.set(updatedAnswers)

        renderer.render("uploadConfirmation.njk", json).map(Ok(_))
      }
  }

  private def confirmationPanelText(id: String)(implicit messages: Messages): Html =
    Html(s"${messages("uploadConfirmation.panel.html")}<br><strong class='breakString'>$id</strong>")
}
