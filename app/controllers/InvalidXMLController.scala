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
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import handlers.ErrorHandler
import helpers.ViewHelper
import javax.inject.Inject
import pages.{GenericErrorPage, InvalidXMLPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels._

import scala.concurrent.ExecutionContext

class InvalidXMLController @Inject()(
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  renderer: Renderer,
  errorHandler: ErrorHandler,
  viewHelper: ViewHelper,
  appConfig: FrontendAppConfig,

)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      (request.userAnswers.get(GenericErrorPage), request.userAnswers.get(InvalidXMLPage)) match {
        case (Some(errors), Some(fileName)) =>
          val tableWithErrors: Table = viewHelper.mapErrorsToTable(errors)

          renderer.render(
            "invalidXML.njk",
            Json.obj(
              "fileName" -> Json.toJson(fileName),
              "fileName" -> Json.toJson(fileName),
              "tableWithErrors" -> tableWithErrors,
              "xmlTechnicalGuidanceUrl" -> Json.toJson(appConfig.xmlTechnicalGuidanceUrl)
            )
          ).map(Ok(_))

        case _ => errorHandler.onServerError(request, throw new RuntimeException("fileName or errors missing for InvalidXMLPage"))
      }
  }
}

