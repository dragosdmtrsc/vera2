organization  := "org.change"

version       := "0.2"

scalaVersion  := "2.11.1"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

fork := true

libraryDependencies ++= {
  Seq(
    "org.antlr" % "antlr4" % "4.3",
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
    "io.spray" %%  "spray-json" % "1.3.2"
  )
}

test in assembly := {}

lazy val sample = taskKey[Unit]("Interpreting")

fullRunTask(sample, Compile, "org.change.v2.runners.experiments.SEFLRunner")

lazy val click = taskKey[Unit]("Symbolically running Template.click")

fullRunTask(click, Compile, "org.change.v2.runners.experiments.TemplateRunner")

lazy val symb = taskKey[Unit]("Symbolically running Template.click without validation")

fullRunTask(symb, Compile, "org.change.v2.runners.experiments.TemplateRunnerWithoutValidation")

lazy val mc = taskKey[Unit]("Running multiple VMs")

fullRunTask(mc, Compile, "org.change.v2.runners.experiments.MultipleVms")

lazy val switch_bench = taskKey[Unit]("Switch Bench")

fullRunTask(switch_bench, Compile, "org.change.v2.runners.experiments.ciscoswitchtest.CiscoSwitchTestBench")

lazy val policy = taskKey[Unit]("Policy testing")

fullRunTask(policy, Compile, "org.change.v2.verification.Test")

lazy val btdemo = taskKey[Unit]("BTDemo")

fullRunTask(btdemo, Compile, "org.change.v2.runners.experiments.BTDemo")

lazy val sefl = taskKey[Unit]("SEFL execution")

fullRunTask(sefl, Compile, "org.change.v2.runners.sefl.SEFLExecutor")

seq(Revolver.settings: _*)
