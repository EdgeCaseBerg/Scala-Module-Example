import sbt._
import Keys._
import complete.DefaultParsers.spaceDelimited

name := "sbt-query"

scalaVersion in ThisBuild := "2.11.7"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature")

test in assembly in ThisBuild := {}

lazy val hello = (project in file("./module/hello"))

lazy val world = (project in file("./module/world"))

lazy val alien = (project in file("./module/alien"))

lazy val root = (project in file(".")).aggregate(hello, world, alien)

// sbt "assemblyWith hello world"
// aims to produce hello-assembly.jar and world-assembly.jar (not alien-assembly.jar)

lazy val assemblyWith = inputKey[Unit]("assembly sub-projects specified in args")

assemblyWith := {
    val args = spaceDelimited("<arg>").parsed
    val stateee = state.value
    val log = stateee.globalLogging.full
    val extractedRoot = sbt.Project.extract(stateee)
    val destDirectory = (crossTarget in extractedRoot.currentRef get extractedRoot.structure.data).get
    args.collect {
        case projectName if (file("module") / projectName).exists =>
            ProjectRef(file("module") / projectName, projectName)
    }.map { proj =>
        log.info(s"managedProject: $proj")
        // improve: https://github.com/sbt/sbt/issues/1095
        // -> outside the task's definition (i.e. as a top level statement in build.sbt), then it works.
        val assemblyJarFile = proj.project match {
            case "hello" => assembly.all(ScopeFilter(inProjects(hello))).value.head
            case "world" => assembly.all(ScopeFilter(inProjects(world))).value.head
            case "alien" => assembly.all(ScopeFilter(inProjects(alien))).value.head
        }
        log.info(s"out: ${assemblyJarFile.getCanonicalPath}")
        // IO.copyFile(assemblyJarFile, destDirectory)
    }
}