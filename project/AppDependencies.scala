import sbt._

object AppDependencies {
  import play.core.PlayVersion

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"            % "0.73.0",
    "uk.gov.hmrc"       %% "logback-json-logger"           % "5.2.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.12.0-play-28",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % "7.11.0",
    "uk.gov.hmrc"       %% "play-nunjucks"                 % "0.40.0-play-28",
    "uk.gov.hmrc"       %% "play-nunjucks-viewmodel"       % "0.16.0-play-28",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"            % "3.26.0-play-28",
    "org.webjars.npm"   %  "govuk-frontend"                % "3.12.0",
    "org.webjars.npm"   %  "hmrc-frontend"                 % "1.35.0",
    "com.typesafe.play" %% "play-json-joda"                % "2.9.2"
  )

  val test = Seq(
    "org.scalatest"               %% "scalatest"          % "3.1.0",
    "org.scalatestplus.play"      %% "scalatestplus-play" % "5.1.0",
    "org.pegdown"                 %  "pegdown"            % "1.6.0",
    "org.jsoup"                   %  "jsoup"              % "1.10.3",
    "com.typesafe.play"           %% "play-test"          % PlayVersion.current,
    "org.mockito"                 %% "mockito-scala"      % "1.10.6" ,
    "com.github.tomakehurst"      %  "wiremock-jre8"      % "2.26.0",
    "org.scalatestplus"           %% "scalatestplus-scalacheck"  % "3.1.0.0-RC2",
    "wolfendale"                  %% "scalacheck-gen-regexp" % "0.1.2",
    "com.vladsch.flexmark"        %  "flexmark-all"          % "0.35.10"
  ).map(_ % "test, it")



  def apply(): Seq[ModuleID] = compile ++ test
}
