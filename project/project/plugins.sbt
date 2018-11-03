addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.3")

// Fix https://github.com/coursier/coursier/issues/450
classpathTypes += "maven-plugin"
