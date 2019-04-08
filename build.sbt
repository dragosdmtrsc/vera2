import java.io.PrintWriter

organization  := "org.change"
version       := "0.2.1-SNAPSHOT"
scalaVersion  := "2.10.2"

name := "symnet"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

fork := true

resolvers += "Sonatype OSS Snapshots" at
   "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies ++= {
  Seq(
    "org.antlr" % "antlr4" % "4.7",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test",
    "junit" % "junit" % "4.12"
  )
}

testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")
parallelExecution in Test := false
exportJars := true
//unmanagedJars in Compile += file("lib/scalaz3_2.11-2.1.jar")
unmanagedJars in Compile += file("lib/z3/com.microsoft.z3.jar")
unmanagedResourceDirectories in Compile += baseDirectory.value / "lib" / "z3"
includeFilter in(Compile, unmanagedResourceDirectories) := ".dll,.so"

fork in Test := true
javaOptions in Test += "-Xss128M"
javaOptions in Test += "-Xmx32G"
javaOptions in run += "-Xss128M"
//test in assembly := {}

val MkUnixlauncher = config("mkunixlauncher") extend Compile
val mkunixlauncher = taskKey[Unit]("mkunixlauncher")
mkunixlauncher <<= (target, fullClasspath in Runtime) map { (target, cp) =>
  def writeFile(file: File, str: String) {
    val writer = new PrintWriter(file)
    writer.println(str)
    writer.close()
  }
  val cpString = cp.map(_.data).mkString(System.getProperty("path.separator"))
  System.out.println(cpString)
  val runtime_entrypoint = "org.change.p4.tools.Vera"
  val launchString = """
CLASSPATH="%s"
java -Djava.class.path="${CLASSPATH}" %s "$@"
                     """.format(cpString, runtime_entrypoint)
  val targetFile = (target / "vera.sh").asFile
  writeFile(targetFile, launchString)
  targetFile.setExecutable(true)
}
