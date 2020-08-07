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

import connectors.CrossBorderArrangementsConnector
import javax.inject.Inject
import models.{Dac6MetaData, GenericError, Validation, ValidationFailure, ValidationSuccess, XMLValidationStatus}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class IdVerificationService @Inject()(connector:  CrossBorderArrangementsConnector) {

  def verifyIds(source: String, elem: Elem, dac6MetaData: Option[Dac6MetaData])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[XMLValidationStatus] = {
    connector.verifyArrangementId(dac6MetaData.get.arrangementID.get) map {
      case true => ValidationSuccess(source, dac6MetaData)
      case false =>
        ValidationFailure(List(GenericError(getLineNumber(elem, "ArrangementID"), "ArrangementID does not match HMRC's records")))
    }

  }

  private def getLineNumber(xml: Elem, path: String): Int = {
    val xmlArray = xml.toString().split("\n")

    xmlArray.indexWhere(str => str.contains(path)) + 1
  }


}
