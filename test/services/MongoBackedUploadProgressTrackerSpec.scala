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
import models.upscan.{InProgress, Reference, UploadId, UploadSessionDetails}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import reactivemongo.bson.BSONObjectID
import repositories.UploadSessionRepository

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class MongoBackedUploadProgressTrackerSpec extends SpecBase with MockitoSugar {

  val mockUploadSessionRepository = mock[UploadSessionRepository]

  import scala.concurrent.ExecutionContext.Implicits._

  val sut = new MongoBackedUploadProgressTracker(mockUploadSessionRepository)

  "Progress tracker" - {
    "must insert an upload" in {
      when(mockUploadSessionRepository.insert(any[UploadSessionDetails]())).thenReturn(Future.successful(true))
      val insert = Await.result((sut.requestUpload(UploadId("123"), Reference("123"))), 10 seconds)
      insert mustEqual true
    }

    "must find an upload" in {
      val uploadId = UploadId("123")
      val uploadDetails = UploadSessionDetails(
        BSONObjectID.generate(),
        UploadId("123"),
        Reference("123"),
        InProgress
      )
      when(mockUploadSessionRepository.findByUploadId(uploadId)).thenReturn(
        Future.successful(
          Some(uploadDetails)))
      val result = Await.result(sut.getUploadResult(uploadId), 10 seconds)
      result.get mustBe InProgress
    }

    "must register an upload result" in {
      val reference = Reference("")

    when(mockUploadSessionRepository.updateStatus(reference, InProgress)).thenReturn(
      Future.successful(
        true))
      val result = Await.result(sut.registerUploadResult(reference, InProgress), 10 seconds)
      result mustEqual true
    }
  }
 }
