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

#### Actions allowing symbolic parameters: 

Currently implemented: `add_to_field`, `add`, `subtract_from_field`, `subtract`.

Should be implementable: `modify_field`, `modify_field_rng_uniform`, TBD: bitwise operations.

Cannot be implemented: `modify_field_with_hash_based_offset`

#### Actions allowing expansion:

Currently implemented: TBD

Should be implementable: `add_header`, `copy_header`, `remove_header`, `push`, `pop`

Cannot be implemented: TBD

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

### Per table metadata

Some per-table metadata are required for matching in the control flow functions.
- `<action_name>.Fired` - a flag which indicates if the actions was fired (0 or 1)
- `<table_name>.Hit` - a flag which indicates if any entry in the table was matched (0 or 1)
- `IsClone` - a flag which indicates that the current state is a clone. This flag
is used to bypass the current table, as there might be rules which also
apply to it. At the end of the current table, the clone (aka clone `Forward` operation is
executed and the packet is sent to the desired location `egress` or `ingress`)

### Notes on sub-systems of a P4 switch and naming conventions
A P4 switch is composed of the following components:
- control functions (denoted `control.<control_function_name>`)
- table calls (denoted `table.<table_name>.in.<table_call_id>`, where `table_call_id` is
a pseudo-random string which ensures proper connection in the control flow
- parser mechanism (denoted `<switch_instance_name>.parser.in`) - shall convert
the current tagged representation to a parsed packet representation and shall set
all `<header_instance>.IsValid` flags to 1 for all headers which were extracted. NB: All
header instance fields shall be allocated with their fixed size as declared. At the end
of the parser function, the resulting parsed representation will be saved for clone cases.
As such, let `<header_instance>.<field>` be a field in the resulting parsed packet, then
`Original.<header_instance>.<field>` shall be allocated with the corresponding width and
assigned as a reference to the current `<header_instance>.<field>`'s value
- deparser mechanism (denoted `<switch_instance_name>.deparser.in`) - shall convert 
the current parsed packet representation to a tag-based representation
- buffer mechanism (denoted `<switch_instance_name>.buffer.in`) - performs `egress_port` 
population based on the current `egress_spec` if normal copy or `clone_spec` if clone copy 
- output mechanism (denoted `<switch_instance_name>.output.in`) perfoms the actual forwarding
of a packet to one of the switch's declared interfaces based on the set `egress_port`. 
If a port with the given id is not found, the execution fails.
- input mechanism (denoted `<switch_instance_name>.input.in`) - performs `ingress_port`
population and initializes header instances and metadata. After this point, all header
instances shall have `<header_instance>.IsValid` flag set to 0

Links: 
- A **control function** may perform one or more **table call**s.

- The **input mechanism** links to the **parser mechanism**

- The **parser mechanism** links to the **_ingress_ control function**

- The **_ingress_ control function** links to the **buffer mechanism**

- The **buffer mechanism** links to the **_egress_ control function**

- The **_egress_ control function** links to the **output mechanism**

### Notes on bitwise operators

Currently, a limited implementation for some bitwise operands is provided. 
As such, the `Z3BVSolver` succesfully handles the `and`, `or`, `xor` operations.

### Known limitations (to date)

1. Some bit operations are not implemented - `shl`, `shr`
2. `recirculate` and `resubmit` instructions are not implemented as per spec - 
in the spec the packet is marked for resubmission and only at the end of the current
pipeline is it resubmited for processing to the parser. Also, the effect of multiple
recirculate/resubmit actions is
3. Meters and counters are not implemented. Consequently, no actions which take
inputs meter or counter references are implemented
4. No calculations are currently implemented. Consequently, no actions which take
 inputs calculated fields are implemented
5. `truncate` and `count` instructions are not implemented
6. The current parser and interpreter needs declarations prior to usage in functions. 
As such, all header types need to be declared first, then all header instances. 
7. The current parser cannot ignore comments. - Worked around using approach in issue 8.
8. The current parser has no built-in precompile step and will throw whenever
macro definitions are observed and used - in order to use this, run the following command
assuming `f.p4` is your input file: `gcc -E -x c -P f.p4 > f-ppc.p4` and then
run Symnet-P4 integration against the pre-processed file `f-ppc.p4`
9. Little to no testing done for array instances - most likely they will generate bugs
10. No support for parser `value sets`
11. No support for variable-width header fields

SymNet v2
=========

Symbolic execution for middleboxes made easy and fast.

### Setup (*nix machine)

1. JDK 8 (in case a different one is used the ScalaZ3 jar needs to be rebuilt against this, different, JDK)
2. sbt - The simple build tool for Scala projects

See _setup.sh_ for a concrete setup script (it was tested on 64bit Ubuntu 12.04, 14.04 and 15.10).        

Then from project root issue _sbt sample_.

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