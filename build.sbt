name := "lift-couchdb"

organization := "net.liftmodules"

liftVersion <<= liftVersion ?? "2.5-SNAPSHOT"

version <<= liftVersion apply { _ + "-1.0-SNAPSHOT" }

scalaVersion := "2.9.2"

scalacOptions ++= Seq("-unchecked", "-deprecation")

crossScalaVersions := Seq("2.9.2", "2.9.1-1", "2.9.1")

resolvers += "CB Central Mirror" at "http://repo.cloudbees.com/content/groups/public"

resolvers += "Java.net Maven2 Repository" at "http://download.java.net/maven/2/"

libraryDependencies <<= (liftVersion, scalaVersion) { (lv, sv) =>
  "net.liftweb" %% "lift-record" % lv % "compile->default" ::
   "org.specs2" %% "specs2"  % "1.11" % "test"  ::
    (sv match {
      case "2.9.2" | "2.9.1" | "2.9.1-1" => "net.databinder"  % "dispatch-http_2.9.1" % "0.7.8" % "compile->default"
      case _ => "net.databinder"  %% "dispatch-http" % "0.7.8" % "compile->default"
    }) ::
  Nil
}


publishTo <<= version { _.endsWith("SNAPSHOT") match {
  case true  => Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
  case false => Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
}
}


// For local deployment:

credentials += Credentials( file("sonatype.credentials") )

// For the build server:

credentials += Credentials( file("/private/liftmodules/sonatype.credentials") )

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses += ("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

pomExtra := (
  <url>https://github.com/liftmodules/couchdb</url>
    <licenses>
      <license>
        <name>Apache 2.0 License</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:liftmodules/couchdb.git</url>
      <connection>scm:git:git@github.com:liftmodules/couchdb.git</connection>
    </scm>
    <developers>
      <developer>
        <id>liftmodules</id>
        <name>Lift Team</name>
        <url>http://www.liftmodules.net</url>
      </developer>
    </developers>
  )

