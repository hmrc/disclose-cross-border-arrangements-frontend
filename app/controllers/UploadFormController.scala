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
import connectors.UpscanConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.mappings.Mappings
import handlers.ErrorHandler
import models.UserAnswers
import models.upscan._
import org.slf4j.LoggerFactory
import pages.UploadIDPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadFormController @Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  appConfig: FrontendAppConfig,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  upscanConnector: UpscanConnector,
  sessionRepository: SessionRepository,
  renderer: Renderer,
  errorHandler: ErrorHandler
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with NunjucksSupport {

  private val logger = LoggerFactory.getLogger(getClass)

  def onPageLoad: Action[AnyContent] = (identify andThen getData).async {
    implicit request =>
      val form: Form[String] = new Mappings {
        val apply: Form[String] = Form("file" -> text())
      }.apply

      {
        for {
          upscanInitiateResponse <- upscanConnector.getUpscanFormData
          uploadId               <- upscanConnector.requestUpload(upscanInitiateResponse.fileReference)
          updatedAnswers         <- Future.fromTry(UserAnswers(request.internalId).set(UploadIDPage, uploadId))
          _                      <- sessionRepository.set(updatedAnswers)
        } yield {
          val formWithErrors: Form[String] = request.flash.get("REJECTED").fold(form) {
            _ =>
              form.withError("file", "upload_form.error.file.invalid")
          }
          //The view for this controller does not contain a crsf token as Upscan cannot function with it
          renderer
            .render(
              "upload-form.njk",
              Json.obj("form" -> formWithErrors, "upscanInitiateResponse" -> Json.toJson(upscanInitiateResponse), "status" -> Json.toJson(0))
            )
            .map(Ok(_))
        }
      }.flatten
  }

  def showResult: Action[AnyContent] = Action.async {
    implicit uploadResponse =>
      renderer.render("upload-result.njk").map(Ok(_))
  }

  def showError(errorCode: String, errorMessage: String, errorRequestId: String): Action[AnyContent] = Action.async {
    implicit request =>
      errorCode match {
        case "EntityTooLarge" =>
          renderer
            .render(
              "fileTooLargeError.njk",
              Json.obj("xmlTechnicalGuidanceUrl" -> Json.toJson(appConfig.xmlTechnicalGuidanceUrl))
            )
            .map(Ok(_))
        case _ =>
          renderer
            .render(
              "error.njk",
              Json.obj("pageTitle" -> "Upload Error", "heading" -> errorMessage, "message" -> s"Code: $errorCode, RequestId: $errorRequestId")
            )
            .map(Ok(_))
      }
  }

  def getStatus: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      logger.debug("Show status called")

      request.userAnswers.get(UploadIDPage) match {
        case Some(uploadId) =>
          upscanConnector.getUploadStatus(uploadId) flatMap {
            case Some(_: UploadedSuccessfully) =>
              Future.successful(Redirect(routes.FileValidationController.onPageLoad()))
            case Some(Quarantined) =>
              Future.successful(Redirect(routes.VirusErrorController.onPageLoad()))
            case Some(Rejected) =>
              Future.successful(Redirect(routes.UploadFormController.onPageLoad()).flashing("REJECTED" -> "REJECTED"))
            case Some(Failed) =>
              errorHandler.onServerError(request, new Throwable("Upload to upscan failed"))
            case Some(_) =>
              renderer.render("upload-result.njk").map(Ok(_))
            case None =>
              errorHandler.onServerError(request, new Throwable(s"Upload with id $uploadId not found"))
          }
        case None =>
          errorHandler.onServerError(request, new Throwable("UploadId not found"))
      }
  }

}
