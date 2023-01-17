/*
 * Copyright 2023 HM Revenue & Customs
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
import pages.{Dac6MetaDataPage, UploadIDPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DeleteDisclosureConfirmationController @Inject() (
  override val messagesApi: MessagesApi,
  appConfig: FrontendAppConfig,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  contactRetrievalAction: ContactRetrievalAction,
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  renderer: Renderer,
  errorHandler: ErrorHandler,
  viewHelper: ViewHelper
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData andThen contactRetrievalAction).async {
    implicit request =>
      val emailMessage = request.contacts match {
        case Some(contactDetails) if contactDetails.secondEmail.isDefined => contactDetails.contactEmail.get + " and " + contactDetails.secondEmail.get
        case Some(contactDetails)                                         => contactDetails.contactEmail.get
        case _                                                            => errorHandler.onServerError(request, new Exception("Contact details are missing"))
      }

      request.userAnswers.get(Dac6MetaDataPage) match {
        case Some(xmlData) =>
          for {
            updatedAnswers <- request.userAnswers.remove(UploadIDPage)
          } yield sessionRepository.set(updatedAnswers)

          renderer
            .render(
              "deleteDisclosureConfirmation.njk",
              Json.obj(
                "messageRefID"       -> xmlData.messageRefId,
                "emailMessage"       -> emailMessage.toString,
                "disclosureID"       -> xmlData.disclosureID,
                "arrangementID"      -> xmlData.arrangementID,
                "homePageLink"       -> viewHelper.linkToHomePageText(Json.toJson(appConfig.discloseArrangeLink)),
                "betaFeedbackSurvey" -> viewHelper.surveyLinkText(Json.toJson(appConfig.betaFeedbackUrl))
              )
            )
            .map(Ok(_))

        case _ => errorHandler.onServerError(request, throw new RuntimeException("ID's from XML are missing"))
      }
  }
}
