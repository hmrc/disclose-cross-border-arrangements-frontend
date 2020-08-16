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

import java.time.format.DateTimeFormatter

import connectors.CrossBorderArrangementsConnector
import controllers.actions.IdentifierAction
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

class HistoryController @Inject()(
                                   identify: IdentifierAction,
                                   crossBorderArrangementsConnector: CrossBorderArrangementsConnector,
                                   val controllerComponents: MessagesControllerComponents,
                                   renderer: Renderer
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify).async { implicit request =>
    val submissionDateFormat = DateTimeFormatter.ofPattern("hh:mm a 'on' d MMMM yyyy")

    {for {
      retrievedDetails <- crossBorderArrangementsConnector.retrievePreviousSubmissions(request.enrolmentID)
      context = Json.obj("datarows" -> retrievedDetails.details.map {
        submission =>
          JsArray(Seq(
            Json.obj("text" -> submission.arrangementID),
            Json.obj("text" -> submission.disclosureID),
            Json.obj("text" -> submission.submissionTime.format(submissionDateFormat)),
            Json.obj("text" -> submission.fileName)
          ))
      })
    } yield {
      renderer.render("submissionHistory.njk", context).map(Ok(_))
    }}.flatten
  }

}
