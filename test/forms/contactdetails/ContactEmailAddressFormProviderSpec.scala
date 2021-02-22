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

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class ContactEmailAddressFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "contactEmailAddress.error.required"
  val invalidKey = "contactEmailAddress.error.invalid"
  val lengthKey = "contactEmailAddress.error.length"
  val maxLength = 132

  val form = new ContactEmailAddressFormProvider()()

  ".email" - {

    val fieldName = "email"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLengthEmail(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like fieldWithInvalidData(
      form,
      fieldName,
      invalidString = "123/com.org.uk!",
      error = FormError(fieldName, invalidKey)
    )
  }
}