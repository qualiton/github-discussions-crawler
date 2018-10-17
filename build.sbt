lazy val root =
  (project in file("."))
    .aggregate(server, crawler, database, common)
    .dependsOn(server, crawler, database, common)
    .settings(BaseSettings.default)
    .withDependencies
    .withDocker
    .withRelease
    .withAlpn

lazy val common =
  (project in file("common"))
    .withTestConfig
    .withDependencies

lazy val crawler =
  (project in file("crawler"))
    .dependsOn(database % "test->test;compile->compile", common)
    .withTestConfig
    .withDependencies

lazy val server =
  (project in file("server"))
    .dependsOn(crawler % "test->test;compile->compile")
    .settings(Seq(sources in (Compile, doc) := Seq.empty)) // REASON: No documentation generated with unsuccessful compiler run
    .withTestConfig
    .withDependencies
    .withLocalAlpn

lazy val database =
  (project in file("database"))
    .dependsOn(common)
    .withDependencies

