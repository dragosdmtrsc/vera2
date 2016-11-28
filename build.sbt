organization  := "org.change"

version       := "0.2"

scalaVersion  := "2.11.1"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

fork := true

libraryDependencies ++= {
  Seq(
    "org.antlr" % "antlr4" % "4.3",
    "commons-io" % "commons-io" % "2.4",
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
    "io.spray" %%  "spray-json" % "1.3.2",
    "org.pacesys" % "openstack4j-core" % "2.0.9",
    "org.pacesys.openstack4j.connectors" % "openstack4j-httpclient" % "2.0.9",
    "org.apache.commons" % "commons-lang3" % "3.5",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.3",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.3",
    "com.regblanc" %% "scala-smtlib" % "0.2"
  )
}

lazy val sample = taskKey[Unit]("Interpreting")

fullRunTask(sample, Compile, "org.change.v2.runners.experiments.SEFLRunner")

lazy val click = taskKey[Unit]("Symbolically running Template.click")

fullRunTask(click, Compile, "org.change.v2.runners.experiments.TemplateRunner")

lazy val click_exampl = taskKey[Unit]("Symbolically running TemplateExampl.click with example generation")

fullRunTask(click_exampl, Compile, "org.change.v2.runners.experiments.TemplateRunnerWithExamples")

lazy val mc = taskKey[Unit]("Running multiple VMs")

fullRunTask(mc, Compile, "org.change.v2.runners.experiments.MultipleVms")

lazy val neutron = taskKey[Unit]("Neutron")

fullRunTask(neutron, Compile, "org.change.v2.runners.experiments.NeutronFullRunner")

seq(Revolver.settings: _*)
