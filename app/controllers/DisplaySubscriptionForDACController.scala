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

import connectors.SubscriptionConnector
import controllers.actions._
import helpers.ViewHelper
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

class DisplaySubscriptionForDACController @Inject()(
    override val messagesApi: MessagesApi,
    subscriptionConnector: SubscriptionConnector,
    viewHelper: ViewHelper,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    val controllerComponents: MessagesControllerComponents,
    renderer: Renderer
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData).async {
    implicit request =>

      subscriptionConnector.displaySubscriptionDetails(request.enrolmentID).flatMap {
        details =>
          if (details.isDefined) {
            val responseDetail = details.get.displaySubscriptionForDACResponse.responseDetail

            val buildDisplaySubscription = if (responseDetail.secondaryContact.isDefined) {
              viewHelper.buildDisplaySubscription(responseDetail, hasSecondContact = true)
            } else {
              viewHelper.buildDisplaySubscription(responseDetail, hasSecondContact = false)
            }

            val displaySubscription = Json.obj(
              "displaySubscription" -> buildDisplaySubscription
            )

            renderer.render("displaySubscriptionForDAC.njk", displaySubscription).map(Ok(_))
          } else {
            Future.successful(Redirect(routes.IndexController.onPageLoad()))
          }
      }
  }
}
