import ByteConversions._

name := """play-scala"""

version := "1.0-SNAPSHOT"

BundleKeys.nrOfCpus := 1.0

BundleKeys.memory := 64.MiB

BundleKeys.diskSpace := 10.MB

BundleKeys.roles := Set("web")

BundleKeys.endpoints := Map("my-app" -> Endpoint("http", services = Set(URI("http://:9000"))))

BundleKeys.startCommand += "-Dhttp.address=$MY_APP_BIND_IP -Dhttp.port=$MY_APP_BIND_PORT"

SandboxKeys.imageVersion in Global := "1.0.11"

SandboxKeys.nrOfContainers in Global := 3

SandboxKeys.ports in Global := Set(1111)

SandboxKeys.debugPort := 5095

lazy val root = (project in file(".")).enablePlugins(JavaAppPackaging, PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.typesafe.conductr" %% "play23-conductr-bundle-lib" % "1.0.0"
)

