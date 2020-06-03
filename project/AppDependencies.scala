import sbt._

object AppDependencies {
  import play.core.PlayVersion

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "org.reactivemongo" %% "play2-reactivemongo"           % "0.18.6-play26",
    "uk.gov.hmrc"       %% "logback-json-logger"           % "4.6.0",
    "uk.gov.hmrc"       %% "play-health"                   % "3.14.0-play-26",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.2.0-play-26",
    "uk.gov.hmrc"       %% "bootstrap-play-26"             % "1.4.0",
    "uk.gov.hmrc"       %% "play-whitelist-filter"         % "3.1.0-play-26",
    "uk.gov.hmrc"       %% "play-nunjucks"                 % "0.23.0-play-26",
    "uk.gov.hmrc"       %% "play-nunjucks-viewmodel"       % "0.8.0-play-26",
    "org.webjars.npm"   %  "govuk-frontend"                % "3.3.0"
  )

  val test = Seq(
    "org.scalatest"               %% "scalatest"          % "3.0.8",
    "org.scalatestplus.play"      %% "scalatestplus-play" % "3.1.3",
    "org.pegdown"                 %  "pegdown"            % "1.6.0",
    "org.jsoup"                   %  "jsoup"              % "1.10.3",
    "com.typesafe.play"           %% "play-test"          % PlayVersion.current,
    "org.mockito"                 %  "mockito-all"        % "1.10.19",
    "org.scalacheck"              %% "scalacheck"         % "1.14.0",
    "com.github.tomakehurst"      %  "wiremock-jre8"      % "2.26.0",
    "uk.gov.hmrc"                 %% "reactivemongo-test" % "4.16.0-play-26"
  ).map(_ % Test)

  val it = Seq(
    "org.pegdown"                 %  "pegdown"            % "1.6.0"
  ).map(_ % IntegrationTest)

  def apply(): Seq[ModuleID] = compile ++ test ++ it

  val akkaVersion = "2.5.23"
  val akkaHttpVersion = "10.0.15"

  val overrides = Seq(
    "com.typesafe.akka" %% "akka-stream"    % akkaVersion,
    "com.typesafe.akka" %% "akka-protobuf"  % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j"     % akkaVersion,
    "com.typesafe.akka" %% "akka-actor"     % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion
  )
}
