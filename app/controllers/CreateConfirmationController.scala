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

import config.FrontendAppConfig
import controllers.actions.{ContactRetrievalAction, _}
import handlers.ErrorHandler
import helpers.ViewHelper
import javax.inject.Inject
import pages.{Dac6MetaDataPage, GeneratedIDPage}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.Html

import scala.concurrent.ExecutionContext

class CreateConfirmationController @Inject()(
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    contactRetrievalAction: ContactRetrievalAction,
    val controllerComponents: MessagesControllerComponents,
    renderer: Renderer,
    errorHandler: ErrorHandler,
    viewHelper: ViewHelper,
    appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData andThen contactRetrievalAction).async {
    implicit request =>

      val messageRef = request.userAnswers.get(Dac6MetaDataPage) match {
        case Some(metaData) => metaData.messageRefId
        case None => errorHandler.onServerError(request, new Exception("MessageRefID is missing"))
      }

      val emailMessage = request.contacts match {
        case Some(contactDetails) if contactDetails.secondEmail.isDefined =>  contactDetails.contactEmail.get + " and " + contactDetails.secondEmail.get
        case Some(contactDetails) => contactDetails.contactEmail.getOrElse("")
        case _ => errorHandler.onServerError(request, new Exception("Contact details are missing"))
      }

      request.userAnswers.get(GeneratedIDPage) match {
        case Some(value) => (value.arrangementID, value.disclosureID) match {
          case (Some(arrangementID), Some(disclosureID)) =>
              val json = Json.obj(
                "emailMessage" -> emailMessage.toString,
                "messageRefID" -> messageRef.toString,
                "disclosureID" -> disclosureID,
                "arrangementID" -> confirmationPanelText(arrangementID),
                "homePageLink" -> viewHelper.linkToHomePageText(Json.toJson(appConfig.discloseArrangeLink)),
                "betaFeedbackSurvey" -> viewHelper.surveyLinkText(Json.toJson(appConfig.betaFeedbackUrl))
              )
              renderer.render("createConfirmation.njk", json).map(Ok(_))
          case (None, Some(_)) => errorHandler.onServerError(request, new Exception("Arrangement ID is missing"))
          case (Some(_), None) =>  errorHandler.onServerError(request, new Exception("Disclosure ID is missing"))
          case (None, None) => errorHandler.onServerError(request, new Exception("Generated IDs present but empty"))
        }
      case _ => errorHandler.onServerError(request, new Exception("Generated IDs are missing"))
     }
  }

  private def confirmationPanelText(id: String)(implicit messages: Messages): Html = {
    Html(s"${{ messages("createConfirmation.panel.html") }}<br><strong>$id</strong>")
  }
}
