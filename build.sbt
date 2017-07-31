organization  := "org.change"

version       := "0.2"

scalaVersion  := "2.11.1"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

fork := true

libraryDependencies ++= {
  Seq(
    "org.antlr" % "antlr4" % "4.6",
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
    "io.spray" %%  "spray-json" % "1.3.2",
    "org.pacesys" % "openstack4j" % "3.0.4",
    "org.apache.commons" % "commons-lang3" % "3.5",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.3",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.3",
    "com.regblanc" %% "scala-smtlib" % "0.2",
    "junit" % "junit" % "4.12"
  )
}


unmanagedResourceDirectories in Compile += baseDirectory.value / "lib"

includeFilter in (Compile, unmanagedResourceDirectories):= ".dll,.so"


test in assembly := {}

lazy val sample = taskKey[Unit]("Interpreting")

fullRunTask(sample, Compile, "org.change.v2.runners.experiments.SEFLRunner")

lazy val click = taskKey[Unit]("Symbolically running Template.click")

fullRunTask(click, Compile, "org.change.v2.runners.experiments.TemplateRunner")

lazy val symb = taskKey[Unit]("Symbolically running Template.click without validation")

fullRunTask(symb, Compile, "org.change.v2.runners.experiments.TemplateRunnerWithoutValidation")

lazy val mc = taskKey[Unit]("Running multiple VMs")

fullRunTask(mc, Compile, "org.change.v2.runners.experiments.MultipleVms")

lazy val neutron = taskKey[Unit]("Neutron")

fullRunTask(neutron, Compile, "org.change.v2.runners.experiments.NeutronFullRunner")

lazy val sefl = taskKey[Unit]("SEFL execution")

fullRunTask(sefl, Compile, "org.change.v2.runners.sefl.SEFLExecutor")

lazy val switch_bench = taskKey[Unit]("Switch Bench")

fullRunTask(switch_bench, Compile, "org.change.v2.runners.experiments.ciscoswitchtest.CiscoSwitchTestBench")

lazy val policy = taskKey[Unit]("Policy testing")

fullRunTask(policy, Compile, "org.change.v2.verification.Tester")

lazy val btdemo = taskKey[Unit]("BTDemo")

fullRunTask(btdemo, Compile, "org.change.v2.runners.experiments.BTDemo")


lazy val neutron_tenant_l2 = taskKey[Unit]("Neutron in-tenant stress test")

fullRunTask(neutron_tenant_l2, Compile, "org.change.v2.runners.experiments.L2Connectivity")

lazy val neutron_tenant_fip = taskKey[Unit]("Neutron fip")

fullRunTask(neutron_tenant_fip, Compile, "org.change.v2.runners.experiments.IngressToMachine")

lazy val neutron_tenant_egress = taskKey[Unit]("Neutron egress")

fullRunTask(neutron_tenant_egress, Compile, "org.change.v2.runners.experiments.EgressFromMachine")

javaOptions in neutron_tenant_l2 += s"-Djava.library.path=lib"


lazy val matei_int_test = taskKey[Unit]("Verification tests")


fullRunTask(matei_int_test, Compile, "org.change.v2.verification.Tester")



seq(Revolver.settings: _*)



