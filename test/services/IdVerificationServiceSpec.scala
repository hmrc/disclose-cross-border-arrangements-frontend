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

  implicit val hc = HeaderCarrier()
  import scala.concurrent.ExecutionContext.Implicits._

  "IdVerificationService" -{
    "verifyIds" -{
      "should return a ValidationSuccess if ArragmentId matches hmrcs record for DAC6DD" in {

      //  when(mockConnector.verifyArrangementId(any()(any(), any()).thenReturn(Future.successful(trt))
          when(mockConnector.verifyArrangementId(any())(any())).thenReturn(Future.successful(true))
         val result = Await.result(idVerificationService.verifyIds(dac6MetaData), 10 seconds)
        result mustBe ValidationSuccess("", dac6MetaData)

      }

      "should return a ValidationFailure if ArragmentId matches hmrcs record for DAC6DD" in {
        when(mockConnector.verifyArrangementId(any())(any())).thenReturn(Future.successful(false))

        val result = Await.result(idVerificationService.verifyIds(dac6MetaData), 10 seconds)
        result mustBe ValidationFailure(List(GenericError(1, "Error")))

      }


    }


  }


}
