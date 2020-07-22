import models.upscan._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{AsyncDriver, MongoConnection}
import reactivemongo.bson.BSONObjectID
import repositories.UploadSessionRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SessionRepositorySpec extends
PlaySpec with
ScalaFutures with
IntegrationPatience with
Eventually  {

  private lazy val builder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()

  def parsedUri(config: Configuration): Future[MongoConnection.ParsedURI] = Future.fromTry {
    MongoConnection.parseURI(config.get[String]("mongodb.uri"))
  }

  def database(config: Configuration) = {
    val driver = new AsyncDriver
    for {
      uri <- parsedUri(config)
      con <- driver.connect(uri)
      dn <- Future(uri.db.get)
      db <- con.database(dn)
    } yield db
  }

  "UserSessionRepository" must {
    "allow insertion of uploadDetails" in {
      val app = builder.build()

      running(app) {
        app.injector.instanceOf[ReactiveMongoApi]
        val sut = app.injector.instanceOf[UploadSessionRepository]
        val uploadDetails = UploadSessionDetails(BSONObjectID.generate(), UploadId("102"), Reference("103"), InProgress)
        val result = sut.insert(uploadDetails).futureValue
        result  mustBe true
        val updateResult = sut.updateStatus(Reference("103"), Failed).futureValue
        updateResult mustBe true
        val uploadStatus = sut.findByUploadId(UploadId("102")).futureValue
        uploadStatus.get.status mustBe Failed
        database(app.configuration).map(_.drop()).futureValue
      }
    }
  }

}
