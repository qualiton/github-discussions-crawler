lazy val root =
  (project in file("."))
    .aggregate(server, crawler, slackapiclient, database, common)
    .dependsOn(server, crawler, slackapiclient, database, common)
    .settings(BaseSettings.default)
    .withDependencies
    .withDocker
    .withRelease
    .withAlpn

lazy val common =
  (project in file("common"))
    .withTestConfig(64)
    .withDependencies

lazy val slackapiclient =
  (project in file("slackapiclient"))
    .withTestConfig(40.6)
    .withDependencies

lazy val crawler =
  (project in file("crawler"))
    .dependsOn(database % "test->test;compile->compile", common % "test->test;compile->compile", slackapiclient % "test->test;compile->compile")
    .withTestConfig(82.9)
    .withDependencies

lazy val server =
  (project in file("server"))
    .dependsOn(crawler % "test->test;compile->compile")
    .settings(Seq(sources in (Compile, doc) := Seq.empty)) // REASON: No documentation generated with unsuccessful compiler run
    .withTestConfig(16.3)
    .withDependencies
    .withLocalAlpn

lazy val database =
  (project in file("database"))
    .dependsOn(common)
    .withDependencies

