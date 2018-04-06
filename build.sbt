organization  := "org.change"
version       := "0.2.1-SNAPSHOT"
scalaVersion  := "2.11.1"

name := "symnet"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

fork := true

resolvers += "Sonatype OSS Snapshots" at
   "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies ++= {
  Seq(
    "org.antlr" % "antlr4" % "4.7",
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
    "io.spray" %% "spray-json" % "1.3.2",
    "org.apache.commons" % "commons-lang3" % "3.5",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.3",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.3",
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.8.3",
    "com.regblanc" %% "scala-smtlib" % "0.2",
    "junit" % "junit" % "4.12",
    "com.storm-enroute" %% "scalameter" % "0.8.2"
  )
}

testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")
parallelExecution in Test := false
exportJars := true
unmanagedJars in Compile += file("lib/scalaz3_2.11-2.1.jar")
unmanagedResourceDirectories in Compile += baseDirectory.value / "lib"
includeFilter in(Compile, unmanagedResourceDirectories) := ".dll,.so"

fork in Test := true
javaOptions in Test += "-Xss128M"
javaOptions in Test += "-Xmx4G"
javaOptions in run += "-Xss128M"
//test in assembly := {}

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

lazy val neutron = taskKey[Unit]("Neutron")
fullRunTask(neutron, Compile, "org.change.v2.runners.experiments.NeutronFullRunner")

lazy val sefl = taskKey[Unit]("SEFL execution")
fullRunTask(sefl, Compile, "org.change.v2.runners.sefl.SEFLExecutor")

lazy val switch_bench = taskKey[Unit]("Switch Bench")
fullRunTask(switch_bench, Compile, "org.change.v2.runners.experiments.ciscoswitchtest.CiscoSwitchTestBench")

lazy val policy = taskKey[Unit]("Policy testing")
fullRunTask(policy, Compile, "org.change.v2.verification.Tester")

 javaOptions in policy += "-Xss128M"
// javaOptions in policy += s"-Xms256m"

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

lazy val p4 = taskKey[Unit]("p4")
fullRunTask(p4, Compile, "org.change.v2.verification.P4Tester")

lazy val printer = taskKey[Unit]("printer")
fullRunTask(printer, Compile, "org.change.v2.verification.Printer")
// seq(Revolver.settings: _*)
