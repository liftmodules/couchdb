name := "lift-couchdb"

organization := "net.liftmodules"

version := "1.2-SNAPSHOT"

liftVersion <<= liftVersion ?? "2.6-SNAPSHOT"

liftEdition <<= liftVersion apply { _.substring(0,3) }

moduleName <<= (name, liftEdition) { (n, e) =>  n + "_" + e }

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-unchecked", "-deprecation")

crossScalaVersions := Seq("2.11.0", "2.10.0", "2.9.2", "2.9.1-1", "2.9.1")

resolvers += "CB Central Mirror" at "http://repo.cloudbees.com/content/groups/public"

resolvers += "Java.net Maven2 Repository" at "http://download.java.net/maven/2/"

libraryDependencies <<= (liftVersion, scalaVersion) { (lv, sv) =>
  "net.liftweb" 	  %% "lift-record"   % lv 	    % "provided" ::
  "net.databinder"  %% "dispatch-core" % "0.8.10"  % "compile" ::
  "net.databinder"  %% "dispatch-http" % "0.8.10"  % "compile" ::
    (sv match {
      case "2.9.2" | "2.9.1" | "2.9.1-1" => "org.specs2" %% "specs2" % "1.12.3" % "test"
      case "2.11.0" | "2.11.1" =>  "org.specs2" %% "specs2" % "2.3.11" % "test"
      case _ => "org.specs2" %% "specs2" % "1.13" % "test"
    })  ::
  Nil
}


publishTo <<= version { _.endsWith("SNAPSHOT") match {
  case true  => Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
  case false => Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
 }
}

parallelExecution in Test := false

// For local deployment:

credentials += Credentials( file("sonatype.credentials") )

// For the build server:

credentials += Credentials( file("/private/liftmodules/sonatype.credentials") )

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

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

