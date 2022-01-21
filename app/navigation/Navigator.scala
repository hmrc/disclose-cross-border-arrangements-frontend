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

package navigation

import controllers.routes
import models._
import pages._
import pages.contactdetails._
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject() () {

  private val normalRoutes: Page => UserAnswers => Call = {
    case InvalidXMLPage => _ => routes.InvalidXMLController.onPageLoad()
    case ValidXMLPage   => _ => routes.CheckYourAnswersController.onPageLoad()
    case HistoryPage    => _ => routes.SearchHistoryResultsController.onPageLoad()

    case ContactNamePage                     => _ => controllers.contactdetails.routes.ContactEmailAddressController.onPageLoad()
    case ContactEmailAddressPage             => _ => controllers.contactdetails.routes.HaveContactPhoneController.onPageLoad()
    case HaveContactPhonePage                => haveContactPhoneRoutes
    case ContactTelephoneNumberPage          => _ => routes.ContactDetailsController.onPageLoad()
    case HaveSecondContactPage               => haveSecondContactRoutes
    case SecondaryContactNamePage            => _ => controllers.contactdetails.routes.SecondaryContactEmailAddressController.onPageLoad()
    case SecondaryContactEmailAddressPage    => _ => controllers.contactdetails.routes.HaveSecondaryContactPhoneController.onPageLoad()
    case HaveSecondaryContactPhonePage       => haveSecondaryContactPhoneRoutes
    case SecondaryContactTelephoneNumberPage => _ => routes.ContactDetailsController.onPageLoad()

    case _ => _ => routes.IndexController.onPageLoad()
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case _ => _ => routes.CheckYourAnswersController.onPageLoad()
  }

  private def haveSecondContactRoutes(ua: UserAnswers): Call =
    ua.get(HaveSecondContactPage) match {
      case Some(true) => controllers.contactdetails.routes.SecondaryContactNameController.onPageLoad()
      case _          => routes.ContactDetailsController.onPageLoad()
    }

  private def haveContactPhoneRoutes(ua: UserAnswers): Call =
    ua.get(HaveContactPhonePage) match {
      case Some(true) => controllers.contactdetails.routes.ContactTelephoneNumberController.onPageLoad()
      case _          => routes.ContactDetailsController.onPageLoad()
    }

  private def haveSecondaryContactPhoneRoutes(ua: UserAnswers): Call =
    ua.get(HaveSecondaryContactPhonePage) match {
      case Some(true) => controllers.contactdetails.routes.SecondaryContactTelephoneNumberController.onPageLoad()
      case _          => routes.ContactDetailsController.onPageLoad()
    }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call =
    mode match {
      case NormalMode =>
        normalRoutes(page)(userAnswers)
      case CheckMode =>
        checkRouteMap(page)(userAnswers)
    }
}
