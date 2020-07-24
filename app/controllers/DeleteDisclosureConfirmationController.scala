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
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

class DeleteDisclosureConfirmationController @Inject()(
    override val messagesApi: MessagesApi,
    appConfig: FrontendAppConfig,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    val controllerComponents: MessagesControllerComponents,
    renderer: Renderer
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      //TODO - pass ID's from summary
      val disclosureId = "GBD 2020 07 01 AAA 001"
      val arrangementId = "GBA 2020 07 01 AAA 000"

      renderer.render(
        "deleteDisclosureConfirmation.njk",
        Json.obj(
          "homePageLink" -> Json.toJson(appConfig.discloseArrangeLink),
          "disclosureId" -> disclosureId,
          "arrangementId" -> arrangementId
        )
      ).map(Ok(_))
  }
}
