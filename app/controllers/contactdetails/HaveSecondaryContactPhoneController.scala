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

package controllers.contactdetails

import controllers.actions._
import forms.contactdetails.HaveSecondaryContactPhoneFormProvider
import helpers.ViewHelper
import models.NormalMode
import models.subscription.ContactInformation
import navigation.Navigator
import pages.DisplaySubscriptionDetailsPage
import pages.contactdetails.HaveSecondaryContactPhonePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.{NunjucksSupport, Radios}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HaveSecondaryContactPhoneController @Inject()(
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    viewHelper: ViewHelper,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: HaveSecondaryContactPhoneFormProvider,
    val controllerComponents: MessagesControllerComponents,
    renderer: Renderer
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with NunjucksSupport {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val preparedForm =
        (request.userAnswers.get(HaveSecondaryContactPhonePage), request.userAnswers.get(DisplaySubscriptionDetailsPage)) match {
          case (Some(value), _) => form.fill(value)
          case (None, Some(displaySubscription)) => //TODO secondaryContactPhoneExists is similar to this. Refactor?
            val haveSecondContactPhone =
              viewHelper.secondaryContactPhoneExists(displaySubscription.displaySubscriptionForDACResponse.responseDetail.secondaryContact
                .fold(Seq[ContactInformation]())(_.contactInformation), request.userAnswers)

            form.fill(haveSecondContactPhone)
          case _ => form
        }

      val json = Json.obj(
        "form"   -> preparedForm,
        "radios" -> Radios.yesNo(preparedForm("value")),
        "secondaryContactName" -> viewHelper.getSecondaryContactName(request.userAnswers)
      )

      renderer.render("contactdetails/haveSecondaryContactPhone.njk", json).map(Ok(_))
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors => {

          val json = Json.obj(
            "form"   -> formWithErrors,
            "radios" -> Radios.yesNo(formWithErrors("value")),
            "secondaryContactName" -> viewHelper.getSecondaryContactName(request.userAnswers)
          )

          renderer.render("contactdetails/haveSecondaryContactPhone.njk", json).map(BadRequest(_))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(HaveSecondaryContactPhonePage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(HaveSecondaryContactPhonePage, NormalMode, updatedAnswers))
      )
  }
}
