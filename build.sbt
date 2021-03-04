lazy val root =
  (project in file("."))
    .aggregate(server, crawler, slackapiclient, database, common)
    .dependsOn(server, crawler, slackapiclient, database, common)
    .settings(BaseSettings.default)
    .withDependencies
    .withDocker
    .withRelease
    .withAlpn
    .withAspectJ

lazy val common =
  (project in file("common"))
    .withTestConfig(77.36)
    .withDependencies

lazy val slackapiclient =
  (project in file("slackapiclient"))
    .withTestConfig(40.2)
    .withDependencies

lazy val crawler =
  (project in file("crawler"))
    .dependsOn(database % "test->test;compile->compile", common % "test->test;compile->compile", slackapiclient % "test->test;compile->compile")
    .withTestConfig(86.59)
    .withDependencies

lazy val server =
  (project in file("server"))
    .dependsOn(crawler % "test->test;compile->compile")
    .settings(Seq(sources in (Compile, doc) := Seq.empty)) // REASON: No documentation generated with unsuccessful compiler run
    .withTestConfig(17.14)
    .withDependencies
    .withLocalAlpn

lazy val database =
  (project in file("database"))
    .dependsOn(common)
    .withDependencies

