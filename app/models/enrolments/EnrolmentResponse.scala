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

package models.enrolments


import models.ContactDetails
import play.api.libs.json.{Json, OFormat}
import utils.EnrolmentConstants

case class Enrolment(identifiers: Seq[KnownFact], verifiers: Seq[KnownFact])
{
  def getContactDetails: ContactDetails = {
    val contacts = verifiers.foldLeft(Map[String, String]())((contactMap, knownFact) => contactMap ++ Map(knownFact.key -> knownFact.value))
    ContactDetails(contacts.get(EnrolmentConstants.contactNameKey),
      contacts.get(EnrolmentConstants.contactEmailKey),
      contacts.get(EnrolmentConstants.secondContactNameKey),
      contacts.get(EnrolmentConstants.secondContactEmailKey))
  }
  def getDac6EnrolmentId: Option[String] = {
    val indet = identifiers.find {_.key.equalsIgnoreCase(EnrolmentConstants.dac6IdentifierKey)}.map(_.value)
    indet
  }
}

object Enrolment {
  implicit val formats: OFormat[Enrolment] = Json.format[Enrolment]
}

case class EnrolmentResponse(service: String, enrolments:  Seq[Enrolment]){
  def getEnrolment(enrolmentId: String): Option[Enrolment] =
    enrolments.find(enr => enr.getDac6EnrolmentId match {case Some(id) => id == enrolmentId case None => false})
}


object EnrolmentResponse {
  implicit val formats: OFormat[EnrolmentResponse] = Json.format[EnrolmentResponse]
}

