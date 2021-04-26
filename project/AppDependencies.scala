import sbt._

object AppDependencies {
  import play.core.PlayVersion

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "org.reactivemongo" %% "play2-reactivemongo"           % "0.18.6-play27",
    "uk.gov.hmrc"       %% "logback-json-logger"           % "4.8.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.6.0-play-27",
    "uk.gov.hmrc"       %% "bootstrap-health-play-27"      % "3.4.0",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-27"    % "3.4.0",
    "uk.gov.hmrc"       %% "play-nunjucks"                 % "0.27.0-play-27",
    "uk.gov.hmrc"       %% "play-nunjucks-viewmodel"       % "0.13.0-play-27",
    "uk.gov.hmrc"       %% "emailaddress"                  % "3.5.0",
    "org.webjars.npm"   %  "govuk-frontend"                % "3.9.1",
    "org.webjars.npm"   %  "hmrc-frontend"                 % "1.19.0",
    "com.typesafe.play" %% "play-json-joda"                % "2.9.0"
  )

  val test = Seq(
    "org.scalatest"               %% "scalatest"          % "3.0.8",
    "org.scalatestplus.play"      %% "scalatestplus-play" % "4.0.3",
    "org.pegdown"                 %  "pegdown"            % "1.6.0",
    "org.jsoup"                   %  "jsoup"              % "1.10.3",
    "com.typesafe.play"           %% "play-test"          % PlayVersion.current,
    "org.mockito"                 %  "mockito-all"        % "1.10.19",
    "com.github.tomakehurst"      %  "wiremock-jre8"      % "2.26.0",
    "uk.gov.hmrc"                 %% "reactivemongo-test" % "4.16.0-play-26",
    "org.scalacheck"              %% "scalacheck"            % "1.14.3",
    "wolfendale"                  %% "scalacheck-gen-regexp" % "0.1.2"
  ).map(_ % Test)

  val it = Seq(
    "org.pegdown"                 %  "pegdown"            % "1.6.0"
  ).map(_ % IntegrationTest)

  def apply(): Seq[ModuleID] = compile ++ test ++ it
}
