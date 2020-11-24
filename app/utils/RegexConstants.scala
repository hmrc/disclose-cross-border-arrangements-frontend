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

package utils

trait RegexConstants {

  final val apiNameRegex = """^[a-zA-Z &`\-\\'^]*$"""
  final val apiAddressRegex = """^[A-Za-z0-9 \-,.&']*$"""
  final val apiOrganisationNameRegex = """^[a-zA-Z0-9 '&\\/]*$"""
  final val soleTraderNameRegex = """^[a-zA-Z0-9 '&\\/]*$"""
  final val nonApiNameRegex = """^[a-zA-Z0-9 `'&\-\\/^]*$"""


  final val utrRegex = "^[0-9]*$"
  final val emailRegex = "^(?:[a-zA-Z0-9!#$%&*+\\/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&*+\\/=?^_`{|}~-]+)*)" +
    "@(?:[a-zA-Z0-9!#$%&*+\\/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&*+\\/=?^_`{|}~-]+)*)$"
  final val digitsAndWhiteSpaceOnly = """^\+?[\d\s]+$"""
  final val regexPostcode = """^[A-Za-z]{1,2}[0-9Rr][0-9A-Za-z]?\s?[0-9][ABD-HJLNP-UW-Zabd-hjlnp-uw-z]{2}$"""
  final val postCodeAllowedChars = """^[A-Za-Z 0-9]*$"""
  final val ninoRegex = "^([ACEHJLMOPRSWXY][A-CEGHJ-NPR-TW-Z]|B[A-CEHJ-NPR-TW-Z]|G[ACEGHJ-NPR-TW-Z]|[KT][A-CEGHJ-MPR-TW-Z]|N[A-CEGHJL-NPR-SW-Z]|Z[A-CEGHJ-NPR-TW-Y])[0-9]{6}[A-D ]$"
  final val safeIDRegex = "^[X][A-Z][0-9]{13}"

}
