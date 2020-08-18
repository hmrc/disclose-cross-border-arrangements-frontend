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

import com.google.inject.Inject
import connectors.CrossBorderArrangementsConnector
import controllers.actions.{ContactRetrievalAction, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import pages.{Dac6MetaDataPage, GeneratedIDPage, URLPage, ValidXMLPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import repositories.SessionRepository
import services.{EmailService, AuditService, XMLValidationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CheckYourAnswersHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class CheckYourAnswersController @Inject()(
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    contactRetrievalAction: ContactRetrievalAction,
    sessionRepository: SessionRepository,
    xmlValidationService: XMLValidationService,
    auditService: AuditService,
    emailService: EmailService,
    crossBorderArrangementsConnector: CrossBorderArrangementsConnector,
    val controllerComponents: MessagesControllerComponents,
    renderer: Renderer
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(Dac6MetaDataPage) match {
        case Some(xmlData) =>
          val helper = new CheckYourAnswersHelper(request.userAnswers)
          val fileInfo = helper.displaySummaryFromInstruction(
            xmlData.importInstruction, xmlData.arrangementID.getOrElse(""), xmlData.disclosureID.getOrElse("")
          )
          renderer.render(
            "check-your-answers.njk",
            Json.obj(
              "fileInfo" -> fileInfo
            )
          ).map(Ok(_))

        case _ => Future.successful(Redirect(routes.UploadFormController.onPageLoad().url))
      }
  }

  //ToDo add "andThen contactRetrievalAction" when contact storage corrected
  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      (request.userAnswers.get(URLPage), request.userAnswers.get(ValidXMLPage)) match {
        case (Some(url), Some(fileName)) =>
          val xml: Elem = xmlValidationService.loadXML(url)
          for {
            ids <- crossBorderArrangementsConnector.submitDocument(fileName, request.enrolmentID, xml)
            userAnswersWithIDs <- Future.fromTry(request.userAnswers.set(GeneratedIDPage, ids))
            _              <- sessionRepository.set(userAnswersWithIDs)
 _ =  auditService.submissionAudit(request.enrolmentID, fileName, ids.disclosureID, ids.disclosureID, xml)
            //TODO: send confirmation emails when contact details retrieval is corrected
            //emailResult <- emailService.sendEmail(request.contacts, fileName, ids)
          } yield {
            val importInstruction = xml \ "DAC6Disclosures" \ "DisclosureImportInstruction"
            val instruction = if (importInstruction.isEmpty) "" else importInstruction.text

            instruction match {
              case "DAC6NEW" => Redirect(routes.CreateConfirmationController.onPageLoad())
              case "DAC6ADD" => Redirect(routes.UploadConfirmationController.onPageLoad())
              case "DAC6REP" => Redirect(routes.ReplaceConfirmationController.onPageLoad())
              case _ => Redirect(routes.UploadFormController.onPageLoad().url)
            }
          }

        case _ => Future.successful(Redirect(routes.UploadFormController.onPageLoad().url))
      }

  }
}
