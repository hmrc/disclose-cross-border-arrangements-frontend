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

class IndividualContactNameFormProviderSpec extends StringFieldBehaviours {

  val form = new IndividualContactNameFormProvider()()

  ".firstName" - {

    val fieldName = "firstName"
    val requiredKey = "individualContactName.error.firstName.required"
    val invalidKey = "individualContactName.error.firstName.invalid"
    val lengthKey = "individualContactName.error.firstName.length"
    val maxLength = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLengthAlpha(
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
      "##$123",
      FormError(fieldName, invalidKey)
    )
  }

  ".lastName" - {

    val fieldName = "lastName"
    val requiredKey = "individualContactName.error.lastName.required"
    val invalidKey = "individualContactName.error.lastName.invalid"
    val lengthKey = "individualContactName.error.lastName.length"
    val maxLength = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLengthAlpha(
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
      "##$123",
      FormError(fieldName, invalidKey)
    )
  }
}
