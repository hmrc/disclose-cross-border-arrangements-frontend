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

import connectors.CrossBorderArrangementsConnector
import controllers.actions._
import javax.inject.Inject
import pages.{GeneratedIDPage, URLPage, ValidXMLPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import repositories.SessionRepository
import services.XMLValidationService
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.CheckYourAnswersHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class DeleteDisclosureSummaryController @Inject()(
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    sessionRepository: SessionRepository,
    xmlValidationService: XMLValidationService,
    crossBorderArrangementsConnector: CrossBorderArrangementsConnector,
    val controllerComponents: MessagesControllerComponents,
    renderer: Renderer
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(URLPage) match {
        case Some(url) =>
          val xml: Elem = xmlValidationService.loadXML(url)
          val helper = new CheckYourAnswersHelper(request.userAnswers)
          val fileToDelete = helper.displaySummaryFromInstruction(xml)
          renderer.render(
            "deleteDisclosure.njk",
            Json.obj(
              "fileToDelete" -> fileToDelete
            )
          ).map(Ok(_))

        case _ => Future.successful(Redirect(routes.UploadFormController.onPageLoad().url))
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      (request.userAnswers.get(URLPage), request.userAnswers.get(ValidXMLPage)) match {
        case (Some(url), Some(fileName)) =>
          val xml: Elem = xmlValidationService.loadXML(url)
          for {
            ids <- crossBorderArrangementsConnector.submitDocument(fileName, xml)
            userAnswersWithIDs <- Future.fromTry(request.userAnswers.set(GeneratedIDPage, ids))
            _ <- sessionRepository.set(userAnswersWithIDs)
            //TODO: send confirmation emails
          } yield {
            Redirect("") //TODO: redirect to deletion confirmation controller
          }
        case _ => Future.successful(Redirect(routes.UploadFormController.onPageLoad().url))
      }
  }
}