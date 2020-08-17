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

package services

import connectors.EmailConnector
import javax.inject.{Inject, Singleton}
import models.{ContactDetails, EmailRequest, GeneratedIDs}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject()(emailConnector:EmailConnector)(implicit executionContext: ExecutionContext) {

  def sendEmail(contacts: Option[ContactDetails], filename: String, ids: GeneratedIDs)(implicit hc: HeaderCarrier): Future[Option[HttpResponse]] = {

    contacts match {
      case Some(contactDetails) =>
        val emailAddress = contactDetails.contactEmail
        val primaryContactName = contactDetails.contactName
        val secondaryEmailAddress = contactDetails.secondEmail
        val secondaryName = contactDetails.secondContactName

        val arrangementID = ids.arrangementID.getOrElse(throw new Exception("Cannot find arrangement ID"))
        val disclosureID = ids.disclosureID.getOrElse(throw new Exception("Cannot find disclosure ID"))
        val dateSubmitted = DateTimeFormat.forPattern("dd MMMM yyyy").print(new LocalDate())
        //ToDo extract submission number
        val submissionNumber = ""

        for {
          primaryResponse <- emailAddress
            .filter(EmailAddress.isValid)
            .fold(Future.successful(Option.empty[HttpResponse])) { email =>
              emailConnector.sendEmail(EmailRequest.sendConfirmation(email, arrangementID, disclosureID,
                dateSubmitted, filename, submissionNumber, primaryContactName)).map(Some.apply)
            }

          _ <- secondaryEmailAddress
            .filter(EmailAddress.isValid)
            .fold(Future.successful(Option.empty[HttpResponse])) { secondaryEmailAddress =>
              emailConnector.sendEmail(EmailRequest.sendConfirmation(secondaryEmailAddress, arrangementID, disclosureID,
                dateSubmitted, filename, submissionNumber, secondaryName)).map(Some.apply)
            }
        }
          yield primaryResponse
      case _ => Future.successful(None)
    }
  }
}
