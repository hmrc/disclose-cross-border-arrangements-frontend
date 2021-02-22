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

package forms.contactdetails

import forms.mappings.Mappings
import models.Name
import play.api.data.Form
import play.api.data.Forms._
import utils.RegexConstants

import javax.inject.Inject

class IndividualContactNameFormProvider @Inject() extends Mappings with RegexConstants {

  lazy val maxLength: Int = 35

   def apply(): Form[Name] = Form(
     mapping(
       "firstName" -> validatedText(
         "individualContactName.error.firstName.required",
         "individualContactName.error.firstName.invalid",
         "individualContactName.error.firstName.length",
         apiNameRegex,
         maxLength),
       "lastName" -> validatedText(
         "individualContactName.error.lastName.required",
         "individualContactName.error.lastName.invalid",
         "individualContactName.error.lastName.length",
         apiNameRegex,
         maxLength)
    )(Name.apply)(Name.unapply)
   )
 }
