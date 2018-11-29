name := "Scala Kansai Summit 2018"

organization in ThisBuild := "ninja.grimrose"
scalaVersion in ThisBuild := "2.12.7"

lazy val akkaHttpVersion = "10.1.5"
lazy val akkaVersion     = "2.5.18"

lazy val openCensusVersion      = "0.17.0" // for opencensus-scala
lazy val openCensusScalaVersion = "0.6.1"

lazy val airframeVersion = "0.72"

lazy val skinnyVersion      = "3.0.0"
lazy val scalikejdbcVersion = "3.3.1"

lazy val postgresqlVersion = "42.2.5"
lazy val h2Version         = "1.4.197"

lazy val slf4jVersion = "1.7.25"

lazy val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j"           % akkaVersion,
  "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-testkit"         % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion % Test,
  "org.scalatest"     %% "scalatest"            % "3.0.5" % Test
)

lazy val loggingDependencies = Seq(
  "net.logstash.logback" % "logstash-logback-encoder" % "5.2",
  "ch.qos.logback"       % "logback-classic"          % "1.2.3",
  "org.skinny-framework" % "skinny-logback"           % "1.0.14",
  "org.slf4j"            % "jul-to-slf4j"             % slf4jVersion
)

lazy val opencensusDependencies = Seq(
  // opencensus
  "com.github.sebruck" %% "opencensus-scala-core"      % openCensusScalaVersion,
  "com.github.sebruck" %% "opencensus-scala-akka-http" % openCensusScalaVersion,
  // trace
  "io.opencensus" % "opencensus-exporter-trace-stackdriver" % openCensusVersion,
  "io.opencensus" % "opencensus-exporter-trace-logging"     % openCensusVersion,
  "io.opencensus" % "opencensus-exporter-trace-jaeger"      % openCensusVersion,
  "io.opencensus" % "opencensus-exporter-trace-zipkin"      % openCensusVersion,
  // stats
  "io.opencensus" % "opencensus-exporter-stats-stackdriver" % openCensusVersion,
  "io.opencensus" % "opencensus-exporter-stats-prometheus"  % openCensusVersion,
  "io.prometheus" % "simpleclient_httpserver"               % "0.5.0"
)

lazy val airframeCliDependencies = Seq(
  "org.wvlet.airframe" %% "airframe-config" % airframeVersion,
  "org.wvlet.airframe" %% "airframe-log"    % airframeVersion,
  "org.wvlet.airframe" %% "airframe-opts"   % airframeVersion,
  "org.slf4j"          % "slf4j-jdk14"      % slf4jVersion
)

lazy val baseSettings = Seq(
  resolvers += Resolver.bintrayRepo("danslapman", "maven"), // for reactive-memcached-core
  resolvers += Resolver.sonatypeRepo("releases"),
  updateOptions := updateOptions.value.withCachedResolution(true),
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture", "-Ywarn-unused:imports"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8", "-Xlint:-options"),
  sources in (Compile, doc) := Seq.empty,
  publishArtifact in (Compile, packageDoc) := false,
  publishArtifact in (Compile, packageSrc) := false,
  logBuffered in Test := false,
  fork in Test := true,
  testForkedParallel in Test := true,
  parallelExecution in Test := false,
  scalafmtOnCompile := true,
  libraryDependencies ++= akkaDependencies ++ opencensusDependencies ++ Seq(
    "org.wvlet.airframe" %% "airframe" % airframeVersion,
    "org.pegdown"        % "pegdown"   % "1.6.0" % Test
  ),
  testOptions in Test ++= Seq(
    Tests.Argument(TestFrameworks.ScalaTest, "-oDF")
  ),
  // docker
  dockerBaseImage := "openjdk:8-jre",
  dockerUpdateLatest := true
)

lazy val buildInfoSettings = Seq(
  // build info
  buildInfoOptions += BuildInfoOption.ToJson,
  buildInfoOptions += BuildInfoOption.BuildTime,
  buildInfoPackage := "ninja.grimrose"
)

testOptions in ThisBuild += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports")

lazy val framework = (project in file("app/framework"))
  .settings(
    baseSettings ++ Seq(
      name := "framework"
    )
  )

lazy val identityCore = (project in file("app/identity/core"))
  .enablePlugins(BuildInfoPlugin, GitBranchPrompt)
  .settings(
    baseSettings ++ buildInfoSettings ++ Seq(
      name := "identity-core",
      // build info
      buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, git.gitHeadCommit),
      libraryDependencies ++= Seq(
        "com.github.j5ik2o" %% "reactive-memcached-core" % "1.0.4"
      ),
      testOptions in Test ++= Seq(
        Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports/identity/core")
      )
    )
  )
  .dependsOn(framework % "compile->compile;test->test")

lazy val identityHttp = (project in file("app/identity/http"))
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)
  .settings(
    baseSettings ++ Seq(
      name := "identity-http",
      libraryDependencies ++= loggingDependencies,
      mappings in Docker += {
        ((resourceDirectory in Compile).value / "logback.docker.xml") -> "/opt/docker/conf/logback.xml"
      },
      testOptions in Test ++= Seq(
        Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports/identity/http")
      )
    )
  )
  .dependsOn(identityCore)

lazy val identityCli = (project in file("app/identity/cli"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    baseSettings ++ Seq(
      name := "identity-cli",
      libraryDependencies ++= airframeCliDependencies,
      mainClass in Compile := Some("ninja.grimrose.sandbox.identity.cli.Main"),
      testOptions in Test ++= Seq(
        Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports/identity/cli")
      )
    )
  )
  .dependsOn(identityCore)

lazy val identity = (project in file("app/identity"))
  .settings(
    Seq(
      name := "identity"
    )
  )
  .aggregate(identityHttp, identityCli, identityCore)

lazy val messageCore = (project in file("app/message/core"))
  .enablePlugins(BuildInfoPlugin, GitBranchPrompt)
  .settings(
    baseSettings ++ buildInfoSettings ++ Seq(
      name := "message-core",
      // build info
      buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, git.gitHeadCommit),
      libraryDependencies ++= Seq(
        // database
        "org.postgresql"       % "postgresql"           % postgresqlVersion,
        "org.skinny-framework" %% "skinny-orm"          % skinnyVersion,
        "com.zaxxer"           % "HikariCP"             % "3.2.0",
        "io.opencensus"        % "ocjdbc"               % "0.0.1",
        "org.skinny-framework" %% "skinny-factory-girl" % skinnyVersion % Test,
        "org.scalikejdbc"      %% "scalikejdbc-test"    % scalikejdbcVersion % Test,
        "com.h2database"       % "h2"                   % h2Version % Test
      ),
      testOptions in Test ++= Seq(
        Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports/message/core")
      )
    )
  )
  .dependsOn(framework % "compile->compile;test->test")

lazy val messageHttp = (project in file("app/message/http"))
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)
  .settings(
    baseSettings ++ Seq(
      name := "message-http",
      libraryDependencies ++= loggingDependencies ++ Seq(
        "org.scalikejdbc" %% "scalikejdbc-test" % scalikejdbcVersion % Test,
        "com.h2database"  % "h2"                % h2Version          % Test
      ),
      mappings in Docker += {
        ((resourceDirectory in Compile).value / "logback.docker.xml") -> "/opt/docker/conf/logback.xml"
      },
      testOptions in Test ++= Seq(
        Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports/message/http")
      )
    )
  ).dependsOn(messageCore % "compile->compile;test->test")

lazy val messageCli = (project in file("app/message/cli"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    baseSettings ++ Seq(
      name := "message-cli",
      libraryDependencies ++= airframeCliDependencies ++ Seq(
        "org.scalikejdbc" %% "scalikejdbc-test" % scalikejdbcVersion % Test,
        "com.h2database"  % "h2"                % h2Version          % Test
      ),
      mainClass in Compile := Some("ninja.grimrose.sandbox.message.cli.Main"),
      testOptions in Test ++= Seq(
        Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports/message/cli")
      )
    )
  )
  .dependsOn(messageCore % "compile->compile;test->test")

lazy val message = (project in file("app/message"))
  .settings(
    Seq(
      name := "message"
    )
  ).aggregate(messageCore, messageHttp, messageCli)

lazy val root = (project in file(".")).aggregate(identity, message)
