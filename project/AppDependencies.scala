import sbt._

object AppDependencies {
  import play.core.PlayVersion

  private val bootstrapVersion = "7.15.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"            % "7.3.0-play-28",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"            % "1.1.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.13.0-play-28",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-nunjucks"                 % "0.43.0-play-28",
    "uk.gov.hmrc"       %% "play-nunjucks-viewmodel"       % "0.18.0-play-28",
    "com.typesafe.play" %% "play-json-joda"                % "2.9.2"
  )

  val test = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-28"     % bootstrapVersion,
    "org.pegdown"                 %  "pegdown"                    % "1.6.0",
    "org.jsoup"                   %  "jsoup"                      % "1.10.3",
    "com.typesafe.play"           %% "play-test"                  % PlayVersion.current,
    "org.mockito"                 %% "mockito-scala"              % "1.10.6" ,
    "com.github.tomakehurst"      %  "wiremock-jre8"              % "2.26.0",
    "org.scalatestplus"           %% "scalatestplus-scalacheck"   % "3.1.0.0-RC2",
    "wolfendale"                  %% "scalacheck-gen-regexp"      % "0.1.2",
    "com.vladsch.flexmark"        %  "flexmark-all"               % "0.35.10"
  ).map(_ % "test, it")



  def apply(): Seq[ModuleID] = compile ++ test
}
