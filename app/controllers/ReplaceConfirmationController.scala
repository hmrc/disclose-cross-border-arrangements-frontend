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
import controllers.actions._
import helpers.ViewHelper
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import handlers.ErrorHandler
import pages.Dac6MetaDataPage

import scala.concurrent.ExecutionContext

class ReplaceConfirmationController @Inject()(
    override val messagesApi: MessagesApi,
    frontEndAppConfig: FrontendAppConfig,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    val controllerComponents: MessagesControllerComponents,
    renderer: Renderer,
    viewHelper: ViewHelper,
    contactRetrievalAction: ContactRetrievalAction,
    errorHandler: ErrorHandler
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

      renderer.render("replaceConfirmation.njk",
        Json.obj(
          "messageRefID" -> messageRef.toString,
          "emailMessage" -> emailMessage.toString,
          "homePageLink" -> viewHelper.linkToHomePageText(Json.toJson(frontEndAppConfig.discloseArrangeLink)),
          "betaFeedbackSurvey" -> viewHelper.surveyLinkText(Json.toJson(frontEndAppConfig.betaFeedbackUrl))
        )
      ).map(Ok(_))
  }
}
