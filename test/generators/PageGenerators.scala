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

package generators

import org.scalacheck.Arbitrary
import pages._

trait PageGenerators {

  implicit lazy val arbitraryIndividualContactNamePage: Arbitrary[IndividualContactNamePage.type] =
    Arbitrary(IndividualContactNamePage)

  implicit lazy val arbitrarySecondaryContactTelephoneNumberPage: Arbitrary[SecondaryContactTelephoneNumberPage.type] =
    Arbitrary(SecondaryContactTelephoneNumberPage)

  implicit lazy val arbitrarySecondaryContactEmailAddressPage: Arbitrary[SecondaryContactEmailAddressPage.type] =
    Arbitrary(SecondaryContactEmailAddressPage)

  implicit lazy val arbitrarySecondaryContactNamePage: Arbitrary[SecondaryContactNamePage.type] =
    Arbitrary(SecondaryContactNamePage)

  implicit lazy val arbitraryContactTelephoneNumberPage: Arbitrary[ContactTelephoneNumberPage.type] =
    Arbitrary(ContactTelephoneNumberPage)

  implicit lazy val arbitraryContactEmailAddressPage: Arbitrary[ContactEmailAddressPage.type] =
    Arbitrary(ContactEmailAddressPage)

  implicit lazy val arbitraryContactNamePage: Arbitrary[ContactNamePage.type] =
    Arbitrary(ContactNamePage)
}
