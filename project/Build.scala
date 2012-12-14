import sbt._

object Build extends sbt.Build {

val liftVersion = SettingKey[String]("liftVersion", "Version number of the Lift Web Framework")

val project = Project("lift-couchdb", file("."))

}


