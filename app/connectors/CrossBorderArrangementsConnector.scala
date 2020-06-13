package connectors

import config.FrontendAppConfig
import javax.inject.Inject
import uk.gov.hmrc.play.bootstrap.http.HttpClient

class CrossBorderArrangementsConnector @Inject()(configuration: FrontendAppConfig,
                                                 httpClient: HttpClient) {

  def submitDocument() = {

  }


}
