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
import connectors.CrossBorderArrangementsConnector
import controllers.actions._
import models.GeneratedIDs
import models.requests.DataRequestWithContacts
import org.slf4j.LoggerFactory
import pages.{Dac6MetaDataPage, GeneratedIDPage, URLPage, ValidXMLPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import repositories.SessionRepository
import services.{EmailService, XMLValidationService}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CheckYourAnswersHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class DeleteDisclosureSummaryController @Inject()(
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    contactRetrievalAction: ContactRetrievalAction,
    sessionRepository: SessionRepository,
    xmlValidationService: XMLValidationService,
    emailService: EmailService,
    frontendAppConfig: FrontendAppConfig,
    crossBorderArrangementsConnector: CrossBorderArrangementsConnector,
    val controllerComponents: MessagesControllerComponents,
    renderer: Renderer
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val logger = LoggerFactory.getLogger(getClass)

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(Dac6MetaDataPage) match {
        case Some(xmlData) =>
          val helper = new CheckYourAnswersHelper(request.userAnswers)
          val fileToDelete = helper.displaySummaryFromInstruction(
            xmlData.importInstruction,
            xmlData.arrangementID.getOrElse(""),
            xmlData.disclosureID.getOrElse(""),
            xmlData.messageRefId
          )

          renderer.render(
            "deleteDisclosure.njk",
            Json.obj(
              "fileToDelete" -> fileToDelete
            )
          ).map(Ok(_))

        case _ =>
          logger.warn("Dac6MetaData can't be retrieved for DAC6DEL. Redirecting to /upload page.")
          Future.successful(Redirect(routes.UploadFormController.onPageLoad().url))
      }
  }

  private def sendMail(implicit request: DataRequestWithContacts[_]): Future[Option[HttpResponse]] = {
    if (frontendAppConfig.sendEmailToggle && request.userAnswers.get(Dac6MetaDataPage).isDefined) {
      val dac6MetaData = request.userAnswers.get(Dac6MetaDataPage).get
      val generatedIDs = GeneratedIDs(dac6MetaData.arrangementID, dac6MetaData.disclosureID)

      emailService.sendEmail(request.contacts, generatedIDs, importInstruction = "DAC6DEL", dac6MetaData.messageRefId)
    }
    else {
      logger.warn("Unable to send out email for DAC6DEL.")
      Future.successful(None)
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData andThen contactRetrievalAction).async {
    implicit request =>
      (request.userAnswers.get(URLPage), request.userAnswers.get(ValidXMLPage)) match {
        case (Some(url), Some(fileName)) =>
          val xml: Elem = xmlValidationService.loadXML(url)
          for {
            ids                <- crossBorderArrangementsConnector.submitDocument(fileName, request.enrolmentID, xml)
            userAnswersWithIDs <- Future.fromTry(request.userAnswers.set(GeneratedIDPage, ids))
            _                  <- sessionRepository.set(userAnswersWithIDs)
            _                  <- sendMail
          } yield {
            Redirect(routes.DeleteDisclosureConfirmationController.onPageLoad().url)
          }
        case _ =>
          logger.warn("XML url or XML is missing for DAC6DEL. Redirecting to /upload page.")
          Future.successful(Redirect(routes.UploadFormController.onPageLoad().url))
      }
  }
}
