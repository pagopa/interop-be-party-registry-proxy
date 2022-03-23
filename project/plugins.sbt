addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.9")

addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.16")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")

addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.6")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.5"