/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.contactdetails

import controllers.actions._
import forms.contactdetails.ContactNameFormProvider
import helpers.ViewHelper
import models.NormalMode
import navigation.Navigator
import pages.DisplaySubscriptionDetailsPage
import pages.contactdetails.ContactNamePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContactNameController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  viewHelper: ViewHelper,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: ContactNameFormProvider,
  val controllerComponents: MessagesControllerComponents,
  renderer: Renderer
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with NunjucksSupport {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val preparedForm =
        (request.userAnswers.get(ContactNamePage), request.userAnswers.get(DisplaySubscriptionDetailsPage)) match {
          case (Some(value), _) => form.fill(value)
          case (None, Some(displaySubscription)) =>
            val contactName =
              viewHelper.retrieveContactName(displaySubscription.displaySubscriptionForDACResponse.responseDetail.primaryContact.contactInformation)

            form.fill(contactName)
          case _ => form
        }

      val json = Json.obj(
        "form" -> preparedForm
      )

      renderer.render("contactdetails/contactName.njk", json).map(Ok(_))
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {

            val json = Json.obj(
              "form" -> formWithErrors
            )

            renderer.render("contactdetails/contactName.njk", json).map(BadRequest(_))
          },
          newContactName =>
            request.userAnswers.get(DisplaySubscriptionDetailsPage) match {
              case Some(displaySubscription) =>
                val contactName =
                  viewHelper.retrieveContactName(displaySubscription.displaySubscriptionForDACResponse.responseDetail.primaryContact.contactInformation)

                if (newContactName != contactName) {
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(ContactNamePage, newContactName))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(navigator.nextPage(ContactNamePage, NormalMode, updatedAnswers))
                } else {
                  Future.successful(Redirect(controllers.contactdetails.routes.ContactEmailAddressController.onPageLoad()))
                }
              case None => Future.successful(Redirect(controllers.routes.ContactDetailsController.onPageLoad()))
            }
        )
  }
}
