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

import base.SpecBase
import connectors.CrossBorderArrangementsConnector
import models.{Dac6MetaData, GenericError, ValidationFailure, ValidationSuccess}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.time.Seconds
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class IdVerificationServiceSpec extends SpecBase{

  val arrangementId = "GBA20200101AAA123"

  val mockConnector: CrossBorderArrangementsConnector = mock[CrossBorderArrangementsConnector]

  val idVerificationService = new IdVerificationService(mockConnector)

  val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD",
                                  arrangementID = Some(arrangementId)))

  val downloadSource = "download-src"

  implicit val hc = HeaderCarrier()
  import scala.concurrent.ExecutionContext.Implicits._

  val testXml =
    <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
      <Header>
        <MessageRefId>GB0000000XXX</MessageRefId>
        <Timestamp>2020-05-14T17:10:00</Timestamp>
      </Header>
      <ArrangementID>AAA000000000</ArrangementID>
      <DAC6Disclosures>
        <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
        <DisclosureInformation>
          <ImplementingDate>2020-01-14</ImplementingDate>
        </DisclosureInformation>
        <DisclosureInformation>
          <ImplementingDate>2018-06-25</ImplementingDate>
        </DisclosureInformation>
      </DAC6Disclosures>
    </DAC6_Arrangement>


  "IdVerificationService" -{
    "verifyIds" -{
      "should return a ValidationSuccess if ArrangementId matches hmrcs record for DAC6DD" in {

        when(mockConnector.verifyArrangementId(any())(any())).thenReturn(Future.successful(true))
        val result = Await.result(idVerificationService.verifyIds(downloadSource, testXml, dac6MetaData), 10 seconds)
        result mustBe ValidationSuccess(downloadSource, dac6MetaData)

      }

      "should return a ValidationFailure if ArrangementId matches hmrcs record for DAC6DD" in {

        when(mockConnector.verifyArrangementId(any())(any())).thenReturn(Future.successful(false))
        val result = Await.result(idVerificationService.verifyIds(downloadSource, testXml, dac6MetaData), 10 seconds)
        result mustBe ValidationFailure(List(GenericError(6, "ArrangementID does not match HMRC's records")))

      }


    }


  }


}
