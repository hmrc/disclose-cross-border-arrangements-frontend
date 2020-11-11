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
import config.FrontendAppConfig
import connectors.UpscanConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import handlers.ErrorHandler
import javax.inject.Singleton
import models.UserAnswers
import models.upscan.{Quarantined, UploadId, UploadedSuccessfully, UpscanInitiateRequest}
import org.slf4j.LoggerFactory
import pages.UploadIDPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import repositories.SessionRepository
import services.UploadProgressTracker
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadFormController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      val controllerComponents: MessagesControllerComponents,
                                      appConfig: FrontendAppConfig,
                                      identify: IdentifierAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      upscanInitiateConnector: UpscanConnector,
                                      uploadProgressTracker: UploadProgressTracker,
                                      sessionRepository: SessionRepository,
                                      renderer: Renderer,
                                      errorHandler: ErrorHandler
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val logger = LoggerFactory.getLogger(getClass)

  def onPageLoad: Action[AnyContent] = (identify andThen getData).async  {
    implicit request =>

      val uploadId           = UploadId.generate
      val successRedirectUrl = controllers.routes.UploadFormController.showResult().absoluteURL(appConfig.upscanUseSSL)
      val errorRedirectUrl   = appConfig.upscanRedirectBase + "/disclose-cross-border-arrangements/error"
      val callbackUrl        = controllers.routes.UploadCallbackController.callback().absoluteURL(appConfig.upscanUseSSL)
      val initiateBody       = UpscanInitiateRequest(callbackUrl, successRedirectUrl, errorRedirectUrl)

      {
        for {
          upscanInitiateResponse <- upscanInitiateConnector.getUpscanFormData(initiateBody)
          _                      <- uploadProgressTracker.requestUpload(uploadId,   upscanInitiateResponse.fileReference)
          updatedAnswers         <- Future.fromTry(UserAnswers(request.internalId).set(UploadIDPage, uploadId))
          _                      <- sessionRepository.set(updatedAnswers)
        } yield {
          renderer.render(
            "upload-form.njk",
            Json.obj("upscanInitiateResponse" -> Json.toJson(upscanInitiateResponse),
              "status" -> Json.toJson(0))
          ).map(Ok(_))
        }
      }.flatten
  }

  def showResult: Action[AnyContent] = Action.async {
    implicit uploadResponse => {
      renderer.render(
        "upload-result.njk").map (Ok (_))
      }
    }

  def showError(errorCode: String, errorMessage: String, errorRequestId: String): Action[AnyContent] = Action.async {
    implicit request => errorCode match {
      case "EntityTooLarge" =>
        renderer.render(
          "fileTooLargeError.njk",
          Json.obj("guidanceLink" -> Json.toJson(appConfig.xmlTechnicialGuidanceUrl))
        ).map (Ok (_))
      case _ =>
          renderer.render (
          "error.njk",
          Json.obj ("pageTitle" -> "Upload Error",
          "heading" -> errorMessage,
          "message" -> s"Code: $errorCode, RequestId: $errorRequestId")
          ).map (Ok (_) )
    }
  }

  def getStatus: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request => {
      logger.debug("Show status called")

      request.userAnswers.get(UploadIDPage) match {
        case Some(uploadId) =>
          uploadProgressTracker.getUploadResult(uploadId) flatMap {
            case Some(_: UploadedSuccessfully) =>
              Future.successful(Redirect(routes.FileValidationController.onPageLoad()))
            case Some(Quarantined) =>
              Future.successful(Redirect(routes.VirusErrorController.onPageLoad()))
            case Some(result) =>
              Future.successful(Ok(Json.toJson(result)))
            case None         => Future.successful(BadRequest(s"Upload with id $uploadId not found"))
          }
        case None => Future.successful(BadRequest (s"UploadId not found") )
      }
    }
  }


}
