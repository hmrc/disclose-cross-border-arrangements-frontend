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

import connectors.{UpscanConnector, ValidationConnector}
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import handlers.ErrorHandler
import models.upscan.{UploadId, UploadSessionDetails, UploadedSuccessfully}
import models.{Dac6MetaData, GenericError, NormalMode, UserAnswers}
import navigation.Navigator
import pages._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.XMLValidationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileValidationController @Inject()(
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  val sessionRepository: SessionRepository,
  val controllerComponents: MessagesControllerComponents,
  upscanConnector: UpscanConnector,
  requireData: DataRequiredAction,
  validationService : XMLValidationService,
  validationConnector: ValidationConnector,
  errorHandler: ErrorHandler,
  navigator: Navigator
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async{
    implicit request =>
    {
      for {
        uploadId <- getUploadId(request.userAnswers)
        uploadSessions <- upscanConnector.getUploadDetails(uploadId)
        (fileName, downloadUrl) = getDownloadUrl(uploadSessions)
        xml = validationService.loadXML(downloadUrl)
        validation: Either[Seq[GenericError], Dac6MetaData] <- validationConnector.sendForValidation(xml)
      } yield {
        //TODO - Send file here to backend for validation - DAC6-858
        validation match   {
          case Right(metaData) =>
            println("@@@@@RIGHT\n\n\n")
            for {
              updatedAnswers <- Future.fromTry(UserAnswers(request.internalId).set(ValidXMLPage, fileName))
              updatedAnswersWithURL <- Future.fromTry(updatedAnswers.set(URLPage, downloadUrl))
              updatedAnswersWithMetaData <- Future.fromTry(updatedAnswersWithURL.set(Dac6MetaDataPage, metaData))
              _              <- sessionRepository.set(updatedAnswersWithMetaData)
            } yield {
              metaData.importInstruction match {
                case "DAC6DEL" => Redirect(routes.DeleteDisclosureSummaryController.onPageLoad())
                case _ => Redirect(navigator.nextPage(ValidXMLPage, NormalMode, updatedAnswers))
              }
            }

          case Left(errors: Seq[GenericError]) =>
            println("@@@@@LEFT  WITH ERROR\n\n\n")

            for {
              updatedAnswers <- Future.fromTry(UserAnswers(request.internalId).set(InvalidXMLPage, fileName))
              updatedAnswersWithErrors <- Future.fromTry(updatedAnswers.set(GenericErrorPage, errors))
              _              <- sessionRepository.set(updatedAnswersWithErrors)
            } yield {
              Redirect(navigator.nextPage(InvalidXMLPage, NormalMode, updatedAnswers))
            }
          case Left(_) =>
            println("@@@@@FAR LEFT\n\n\n")

            for {
              updatedAnswers <- Future.fromTry(UserAnswers(request.internalId).set(InvalidXMLPage, fileName))
              _              <- sessionRepository.set(updatedAnswers)
            } yield {
              Redirect(routes.FileErrorController.onPageLoad())
            }
          case _ =>
            println("@@@@@@ONSERVERERROR\n\n")
            errorHandler.onServerError(request, throw new RuntimeException("file validation failed - missing data"))
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

    uploadSessions match {
      case Some(uploadDetails) => uploadDetails.status match {
        case UploadedSuccessfully(name, downloadUrl) => (name, downloadUrl)
        case _ => throw new RuntimeException("File not uploaded successfully")
      }
      case _ => throw new RuntimeException("File not uploaded successfully")
    }
  }
}

