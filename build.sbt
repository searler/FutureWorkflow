name := "Akka Actor Workflow"

version := "0.4"

scalaVersion := "2.10.0-RC5"


//Specs2
libraryDependencies ++= Seq(
 "org.specs2" % "specs2_2.10.0-RC2" % "1.12.2",
   "org.slf4j" % "slf4j-log4j12" % "1.6.3" % "test" ,
   "junit" % "junit" % "4.7"
   )

resolvers ++= Seq("snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
                    "releases"  at "http://oss.sonatype.org/content/repositories/releases")

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

 
libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-actor" % "2.1.0-RC5" cross CrossVersion.full,
                            "com.typesafe.akka" %% "akka-camel" % "2.1.0-RC5" cross CrossVersion.full 
                           )

 //parallelExecution in Test := false


