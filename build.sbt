organization  := "org.change"

version       := "0.2.1-SNAPSHOT"

scalaVersion  := "2.11.1"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

fork := true

libraryDependencies ++= {
  Seq(
    "org.antlr" % "antlr4" % "4.7",
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
    "io.spray" %%  "spray-json" % "1.3.2",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.0",
    "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.9.0"
  )
}

test in assembly := {}

lazy val p4control = taskKey[Unit]("P4 control function to SEFL")


//fullRunTask(p4control, Compile, "org.change.v2.runners.experiments.P4ControlRunner")
p4control := {
  val file = Option(System.getProperty("file")).getOrElse("/Users/localadmin/poli/symnet/symPatru/src/main/resources/p4_test_files/control.p4")
  val r = (runner in Compile).value
  //val cp: Seq[File] = (dependencyClasspath in Compile).value.files
  val cp = (fullClasspath in Compile).value.files
  toError(r.run("org.change.v2.runners.experiments.P4ControlRunner", cp, Seq(file), streams.value.log))
}


lazy val sample = taskKey[Unit]("Interpreting")

fullRunTask(sample, Compile, "org.change.v2.runners.experiments.SEFLRunner")

lazy val click = taskKey[Unit]("Symbolically running Template.click")

fullRunTask(click, Compile, "org.change.v2.runners.experiments.TemplateRunner")

lazy val symb = taskKey[Unit]("Symbolically running Template.click without validation")

fullRunTask(symb, Compile, "org.change.v2.runners.experiments.TemplateRunnerWithoutValidation")

lazy val mc = taskKey[Unit]("Running multiple VMs")

fullRunTask(mc, Compile, "org.change.v2.runners.experiments.MultipleVms")

lazy val fuck= taskKey[Unit]("Running multiple VMs")

fullRunTask(fuck, Compile, "org.change.v2.executor.clickabstractnetwork.AggregatedBuilder")

lazy val switch_bench = taskKey[Unit]("Switch Bench")

fullRunTask(switch_bench, Compile, "org.change.v2.runners.experiments.ciscoswitchtest.CiscoSwitchTestBench")

seq(Revolver.settings: _*)
