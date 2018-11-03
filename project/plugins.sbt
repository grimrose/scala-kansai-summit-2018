addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.5.1")

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.3")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.2")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

// Fix https://github.com/coursier/coursier/issues/450
classpathTypes += "maven-plugin"

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
