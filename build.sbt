name := "gitbucket-commitgraphs-chartjs-plugin"
organization := "io.github.gitbucket"
version := "0.2.0"
scalaVersion := "2.13.18"
gitbucketVersion := "4.48.0"

lazy val root = (project in file(".")).enablePlugins(SbtTwirl)

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % "test"

// Tests run against a throwaway GITBUCKET_HOME
Test / fork := true
Test / javaOptions += s"-Dgitbucket.home=${(Test / target).value / "gitbucket-home"}"
