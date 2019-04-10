Vera2 - P4 program verification
=====================
##### Requirements

Make sure to have z3 installed with libz3.so and libz3java.so 
in your LD_LIBRARY_PATH (Linux) or libz3.dll and libz3java.dll
in your PATH (Windows).

##### Build and run 
To build and run on Linux-based machines (including cygwin):
```
sbt package
sbt mkunixlauncher
./target/vera.sh --help
```

##### Example

```
#without commands
./target/vera.sh inputs/test-cases/simple-router/simple_router-ppc.p4
#with commands
./target/vera.sh --commands inputs/test-cases/simple-router/commands.txt \
inputs/test-cases/simple-router/simple_router-ppc.p4
```