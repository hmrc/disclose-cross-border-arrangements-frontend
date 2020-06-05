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

package repositories

import javax.inject.Inject
import models.upscan._
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

class UploadSessionRepository @Inject()(mongo: ReactiveMongoApi)(implicit ec: ExecutionContext) {


  private val collectionName = "uploadSessionRepository"

  private def collection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection](collectionName))

  def findByUploadId(uploadId: UploadId): Future[Option[UploadSessionDetails]] = {
    collection.flatMap(_.find(Json.obj("uploadId" -> Json.toJson(uploadId)), None).one[UploadSessionDetails])
  }

  def updateStatus(reference : Reference, newStatus : UploadStatus): Future[Boolean] = {

   implicit val referenceFormatter = Json.format[Reference]
    val selector = Json.obj("reference" -> Json.toJson(reference))
    val modifier = Json.obj("$set" -> Json.obj("status" -> Json.toJson(newStatus)))

    collection.flatMap {
      _.update(ordered = false)
        .one(selector, modifier, upsert = true).map {
        lastError =>
          lastError.ok
      }
    }
  }

  def insert(uploadDetails: UploadSessionDetails): Future[Boolean] = {
    collection.flatMap(_.insert.one(uploadDetails)).map {
      lastError =>
        lastError.ok
    }
  }
}
