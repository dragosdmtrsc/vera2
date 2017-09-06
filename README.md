P4 Comments and Ideas
=====================

### Section 2

#### 2.1 Header Type Declarations

The parser yields an instance of class `HeaderDeclaration` for every encountered P4 declaration.

This data structure contains the names of the fields, the width and the offset from the beginning of the header.

_Note that header field lengths that are runtime determined are not supported by vanilla SEFL._

_Also field modifiers are ignored (saturating, signed etc)._

#### Section 2.2 Header and Metadata Instances

1. Header Instances

A defined header instance is identified either by:
- a name
- a name + an index - in case of array instances


### Actions

Implemented and tested (visually): `modify_field`, `subtract_from_field`, `register_read`

Actions currently implemented, but not tested thouroughly: `add_header`, `copy_header`, 
`remove_header`, `add_to_field`, `add`, `push`, `pop`, `resubmit`, `register_write`,
`resubmit`, `recirculate`, `clone_i2i`, `clone_i2e`, `clone_e2i`, `clone_e2e`

See test case _"NAT spec can be parsed - actions, reg defs and field lists are there_" in
`HeaderDefinitionParsingTest` class

#### Initializers

The switch needs to be initialized whenever it is started - i.e. zero out registers and counters - 
and whenever a new packet enters the pipeline - i.e. zero out or initialize metadata instances and
headers.

Class `org.change.parser.p4.InitializeCode` has methods `switchInitializeGlobally` for global init
and `switchInitializePacketEnter` which initializes metadata and header instances
prior to parsing assuming `standard_metadata.instance_type == normal` (i.e. not clone, nor recirculate or resubmit)

See test cases _"SWITCH - new packet initializer"_ and _"SWITCH - global initializer"_ in `HeaderDefinitionParsingTest` class

#### Registers

Addressing a global register (string with the following format): `s"${switchInstance.getName}.reg.${argSource1.toString}[$intVal]"`
Addressing a static register - per table - (string with the following format): `s"${switchInstance.getName}.reg[$table].${argSource1.toString}[$intVal]"`
Addressing a direct register - per flow@table - not implemented

#### Metadata access

Addressing metadata instance: as a string of the following format:

- `x.getName + "." + f.getName`, where x is a HeaderInstance and f is a field defined for the given layout
- `x.getName + index + "." + f.getName`, where x is an ArrayInstance, index is an integer referencing an array index and f is a field
for the given layout

#### Header validity

Each `header_instance` declared by the P4 program may be **valid** or **invalid**. Thus, a validity flag
is declared for all header instances declared by the P4 program. In the _packet_in_ initializer code,
all validity flags for all declared header instances are set to 0. 
 
In order to handle validity of array instances, there is one validity flag for each
entry in the array instance (assuming static length). To address the validity flag
for each header instance, the following naming convention was employed:
- For arrays: `hname + x + ".IsValid"`, where `hname` is the name of the header array,
`x` is the index in the array
- For scalars: `hname + ".IsValid"`, where `hname` is the name of the scalar header
instance


### Known limitations (to date)

1. No bit operations are implemented - `and`, `or`, `xor`, `shl`, `shr`
2. Cloning is **not as per specification**. 
    - **As per specification**, whenever the packet encounters a  `clone` instruction, 
    it should be marked for cloning and, at the end of the current control
flow (ingress or egress), it will be cloned at the specified insertion point. 
The packet, however, continues to experience all instructions encountered while 
continuing the current pipeline and only at the end will it be cloned. 
    - **In the implemented model**, the packet is cloned as soon as it encounters the 
    `clone` operation, the execution forks with the first path forwarding the packet 
    to its specified insertion point and the second path forwarding the packet to the 
    table's output port. This approach is not compliant to spec, but is believed to 
    cover most use-cases, where the `clone` operation is the **last in the pipeline**. 
    - A more accurate implementation should be implemented
3. The same as in 2 goes for `recirculate` and `resubmit` instructions
4. Meters and counters are not implemented. Consequently, no actions which take
inputs meter or counter references are implemented
5. No calculations are currently implemented. Consequently, no actions which take
 inputs calculated fields are implemented
6. `truncate` and `count` instructions are not implemented
7. The current parser and interpreter needs declarations prior to usage in functions. 
As such, all header types need to be declared first, then all header instances. 
8. The current parser cannot ignore comments. - Solved using approach in issue 9.
9. The current parser has no built-in precompile step and will throw whenever
macro definitions are observed and used - in order to use this, run the following command
assuming `f.p4` is your input file: `gcc -E -x c -P f.p4 > f-ppc.p4` and then
run Symnet-P4 integration against the pre-processed file `f-ppc.p4`


SymNet v2
=========

Symbolic execution for middleboxes made easy and fast.

### Setup (nix machine)

1. JDK 8 (in case a different one is used the ScalaZ3 jar needs to be rebuilt against this, different, JDK)
2. sbt - The simple build tool for Scala projects

See _setup.sh_ for a concrete setup script (it was tested on 64bit Ubuntu 12.04, 14.04 and 15.10).        

Then from project root issue _sbt test_.

### Setup (Vagrant)

There is also a _Vagrantfile_ if you prefer this option. This also uses _setup.sh_ for provisioning.

### SEFL sample

See _src/main/scala/change/v2/runners/experiments/SEFLRunner.scala_ to start experimenting with SEFL. _sbt sample_ will run that code.

### Click models in Symnet

Look at the description of the _instructions_ method in _src/main/scala/org/change/v2/Template.scala_; try to understand
what the instructions attatched to input port 0 do. Check _src/main/resources/click_test_files/Template.click_ in order
to get the complete picture.

From the project root issue _sbt click_ which performs the symbolic execution of _Template.click_ file.

You should find the output in _template.output_.

Play with the code, check the output. Loop.
