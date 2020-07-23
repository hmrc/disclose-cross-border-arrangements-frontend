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

import controllers.actions._
import javax.inject.Inject
import pages.GeneratedIDPage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.Html

import scala.concurrent.ExecutionContext

class UploadConfirmationController @Inject()(
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    val controllerComponents: MessagesControllerComponents,
    renderer: Renderer
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val arrangementID = request.userAnswers.get(GeneratedIDPage) match {
        case Some(value) if value.disclosureID.isDefined => value.disclosureID.get
        case _ => "Default" //TODO
      }

      val json = Json.obj(
        "disclosureID" -> confirmationPanelText(arrangementID)
      )

      renderer.render("uploadConfirmation.njk", json).map(Ok(_))
  }

  private def confirmationPanelText(id: String)(implicit messages: Messages): Html = {
    Html(s"${{ messages("uploadConfirmation.panel.html") }}<br><strong>$id</strong>")
  }
}
