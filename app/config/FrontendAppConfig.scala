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

package config

import com.google.inject.{Inject, Singleton}
import controllers.routes
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration,  servicesConfig: ServicesConfig) {

  private val contactHost = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "DAC6"

  val analyticsToken: String = configuration.get[String](s"google-analytics.token")
  val analyticsHost: String = configuration.get[String](s"google-analytics.host")
  val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  val betaFeedbackUrl = s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier"
  val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"
  val signOutUrl: String             = configuration.get[String]("urls.logout")

  val discloseArrangeLink: String = configuration.get[String]("urls.homepage")

  lazy val authUrl: String = configuration.get[Service]("auth").baseUrl
  lazy val loginUrl: String = configuration.get[String]("urls.login")
  lazy val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")



  lazy val enrolmentStoreProxyBaseUrl: String = servicesConfig.baseUrl("enrolment-store-proxy")
  lazy val getEnrolmentsUrl: String = configuration.get[String]("microservice.services.enrolment-store-proxy.get-enrolments-url")

  val upscanInitiateHost: String = servicesConfig.baseUrl("upscan")
  //ToDo this host maybe different without the stubs
  val upscanBucketHost: String = servicesConfig.baseUrl("upscan")
  val upscanProtocol: String = servicesConfig.getConfString("upscan.protocol", "https")
  val upscanRedirectBase: String = configuration.get[String]("microservice.services.upscan.redirect-base")

  val crossBorderArrangementsUrl: String = servicesConfig.baseUrl("cross-border-arrangements")

  lazy val xmlTechnicialGuidanceUrl: String = "???" //ToDo link to govuk guidance when available

  lazy val sendEmailUrl: String = configuration.get[Service]("microservice.services.email").baseUrl

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
