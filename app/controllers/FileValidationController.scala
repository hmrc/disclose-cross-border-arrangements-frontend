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
import javax.inject.Inject
import models.upscan.{UploadId, UploadSessionDetails, UploadedSuccessfully}
import models.{NormalMode, UserAnswers, ValidationFailure, ValidationSuccess}
import navigation.Navigator
import pages.{InvalidXMLPage, URLPage, UploadIDPage, ValidXMLPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import repositories.{SessionRepository, UploadSessionRepository}
import services.{ValidationEngine, XMLValidationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class FileValidationController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          identify: IdentifierAction,
                                          getData: DataRetrievalAction,
                                          val sessionRepository: SessionRepository,
                                          val controllerComponents: MessagesControllerComponents,
                                          appConfig: FrontendAppConfig,
                                          repository: UploadSessionRepository,
                                          requireData: DataRequiredAction,
                                          validationEngine: ValidationEngine,
                                          xmlValidationService: XMLValidationService,
                                          renderer: Renderer,
                                          navigator: Navigator
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
    {
      for {
        uploadId <- getUploadId(request.userAnswers)
        uploadSessions <- repository.findByUploadId(uploadId)
        (fileName, downloadUrl) = getDownloadUrl(uploadSessions)
        validation = validationEngine.validateFile(downloadUrl)
      } yield {
        validation match {
          case ValidationSuccess(_) =>
            for {
              updatedAnswers <- Future.fromTry(UserAnswers(request.internalId).set(ValidXMLPage, fileName))
              updatedAnswersWithURL <- Future.fromTry(updatedAnswers.set(URLPage, downloadUrl))
              _              <- sessionRepository.set(updatedAnswersWithURL) // TODO - pass ID's here to use in Controller
              xml <- Future.successful(xmlValidationService.loadXML(downloadUrl))
            } yield {
              (xml \ "DAC6Disclosures" \ "DisclosureImportInstruction").text match {
                case "DAC6DEL" => Redirect(routes.DeleteDisclosureSummaryController.onPageLoad())
                case _ => Redirect(navigator.nextPage(ValidXMLPage, NormalMode, updatedAnswers))
              }
            }

          case ValidationFailure(_) =>
            for {
              updatedAnswers <- Future.fromTry(UserAnswers(request.internalId).set(InvalidXMLPage, fileName))
              _              <- sessionRepository.set(updatedAnswers)
            } yield {
              Redirect(navigator.nextPage(InvalidXMLPage, NormalMode, updatedAnswers))
            }
        }
      }
    }.flatten
  }

  private def getUploadId(userAnswers: UserAnswers): Future[UploadId] = {
    userAnswers.get(UploadIDPage) match {
      case Some(uploadId) => Future.successful(uploadId)
      case None => throw new RuntimeException("Cannot find uploadId")
    }
  }

  private def getDownloadUrl(uploadSessions: Option[UploadSessionDetails]) = {

    //TODO - pass Download URL to ValidationSuccess alongside DAC6MetaDataObject
    uploadSessions match {
      case Some(uploadDetails) => uploadDetails.status match {
        case UploadedSuccessfully(name, downloadUrl) => (name, downloadUrl)
        case _ => throw new RuntimeException("File not uploaded successfully")
      }
      case _ => throw new RuntimeException("File not uploaded successfully")
    }
  }

}

