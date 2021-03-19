/*
 * Copyright 2021 HM Revenue & Customs
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

package config

import com.google.inject.{Inject, Singleton}
import controllers.routes
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration,  servicesConfig: ServicesConfig) {

  lazy val appName: String = configuration.get[String]("appName")
  private val contactHost = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "DAC6"

  val analyticsToken: String = configuration.get[String](s"google-analytics.token")
  val analyticsHost: String = configuration.get[String](s"google-analytics.host")
  val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  val betaFeedbackUrl = s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier"
  val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"
  val signOutUrl: String             = configuration.get[String]("urls.logout")

  lazy val discloseArrangeLink: String = configuration.get[String]("urls.homepage")
  lazy val searchAgainLink: String = discloseArrangeLink  + configuration.get[String]("urls.searchLink")

  lazy val authUrl: String = configuration.get[Service]("auth").baseUrl
  lazy val loginUrl: String = configuration.get[String]("urls.login")
  lazy val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  lazy val dacManualUrl: String = s"${configuration.get[String]("urls.dac-enter.host")}${configuration.get[String]("urls.dac-enter.startUrl")}"

  lazy val enrolmentStoreProxyBaseUrl: String = servicesConfig.baseUrl("enrolment-store-proxy")
  lazy val getEnrolmentsUrl: String = configuration.get[String]("microservice.services.enrolment-store-proxy.get-enrolments-url")

  lazy val timeoutSeconds: String = configuration.get[String]("session.timeoutSeconds")
  lazy val countdownSeconds: String = configuration.get[String]("session.countdownSeconds")

  val upscanInitiateHost: String = servicesConfig.baseUrl("upscan")
  //ToDo this host maybe different without the stubs
  val upscanBucketHost: String = servicesConfig.baseUrl("upscan")
  val upscanProtocol: String = servicesConfig.getConfString("upscan.protocol", "https")
  val upscanRedirectBase: String = configuration.get[String]("microservice.services.upscan.redirect-base")
  val upscanMaxFileSize: Int = configuration.get[Int]("microservice.services.upscan.max-file-size-in-mb")

  val crossBorderArrangementsUrl: String = servicesConfig.baseUrl("cross-border-arrangements")

  lazy val xmlTechnicalGuidanceUrl: String = "https://www.gov.uk/government/publications/cross-border-tax-arrangements-schema-and-supporting-documents"

  lazy val sendEmailUrl: String = configuration.get[Service]("microservice.services.email").baseUrl

  lazy val sendEmailToggle: Boolean = configuration.get[Boolean]("features.send-email")
  lazy val contactDetailsToggle: Boolean = configuration.get[Boolean]("contactDetailsToggle")
  lazy val manualJourneyToggle: Boolean = configuration.get[Boolean]("manualJourneyToggle")
  lazy val validationAuditToggle: Boolean = configuration.get[Boolean]("validationAuditToggle")
  lazy val contactUsToggle: Boolean = configuration.get[Boolean]("contactUsToggle")
  lazy val recruitmentBannerToggle: Boolean = configuration.get[Boolean]("recruitmentBannerToggle")

  val upscanUseSSL: Boolean = upscanProtocol == "https"

  lazy val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("microservice.services.features.welsh-translation")

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  def routeToSwitchLanguage: String => Call =
    (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)
}
