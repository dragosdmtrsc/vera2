import argparse
import os
import sys
import glob
import subprocess
import time

parser = argparse.ArgumentParser(description='run vera')
parser.add_argument('p4_file', help='p4 file to analyze')
parser.add_argument('--no-preprocess', action='store_true', help='don\'t preprocess input p4 file prior to running vera. '
  'If unset, vera will produce a file called p4_file-ppc.p4 in the same directory as p4_file')
parser.add_argument('--preprocess-only', action='store_true', help='preprocess only and don\'t run symbolic execution')
parser.add_argument('commands', help = 'dataplane to run vera against')
parser.add_argument('--name', help='box name', default='switch')
parser.add_argument('--interface', help='interfaces names', nargs='*')
#parser.add_argument('--with-packet', action='store_true')
parser.add_argument('--success-out-file', default='success.json', help='output file for successful states')
parser.add_argument('--fail-out-file', default='fail.json', help='output file for failure states')
parser.add_argument('--nr-interfaces', type = int, help = 'number of interfaces')
parser.add_argument('--clone-spec', help='clone specification, a mapping between a clone id and an egress port', nargs = '*')
parser.add_argument('--parse-only', action='store_true', help='only run through the parser. perf assessment option')

args = parser.parse_args()

p4_file = args.p4_file
if not args.no_preprocess:
  preproc = []
  preproc.append('gcc')
  #cc -E -x c -P f.p4 > f-ppc.p4
  preproc.append('-E')
  preproc.append('-x')
  preproc.append('c')
  preproc.append('-P')
  preproc.append(p4_file)
  dir = os.path.dirname(args.p4_file)
  name, ext = os.path.splitext(args.p4_file)
  p4_file = name + '-ppc' + ext
  with open(p4_file, 'w') as fd:
    ret = subprocess.call(preproc, stdout=fd)
    if ret != 0:
      print('can\'t preprocess p4 file with command {}'.format(preproc), file=sys.stderr)
      sys.exit(-1)
  if args.preprocess_only:
    sys.exit(0)
topo_plugin_name = 'org.change.v2.plugins.vera.VeraTopologyPlugin'
input_packet_plugin = 'org.change.v2.plugins.eq.NoPacketPlugin'
no_packet = True
#if args.with_packet:
#  no_packet = False

symnet_path=os.getenv('SYMNET_ROOT')
if not symnet_path:
  p=__file__
  dir=os.path.dirname(p)
  symnet_path = os.path.abspath(os.path.join(dir, os.pardir))
jar_location=os.path.join(symnet_path, 'target', 'scala-2.12')
first_jar = glob.glob(os.path.join(jar_location, 'symnet_2.12*.jar'))[0]
target_path = os.path.join(symnet_path, 'target')
classpath='' + first_jar + os.pathsep
streams = os.path.join(target_path, 'streams', 'compile', 'dependencyClasspath', '$global', 'streams', 'export')
with open(streams, 'r') as fd:
  classpath = classpath + fd.read() + ''
exec_name='org.change.v2.tools.sefl.SeflRun'
cmds = []
cmds.append('java')
cmds.append('-cp')
cmds.append(classpath)
cmds.append(exec_name)
cmds.append('--input-packet-plugin')
cmds.append(input_packet_plugin)
cmds.append('--topology-plugin')
cmds.append(topo_plugin_name)
cmds.append('--topology-parm')
cmds.append('p4')
cmds.append(p4_file)

cmds.append('--topology-parm')
cmds.append('commands')
cmds.append(args.commands)

cmds.append('--topology-parm')
cmds.append('noinputpacket')
cmds.append(str(no_packet).lower())

if args.parse_only:
  cmds.append('--topology-parm')
  cmds.append('justparser')
  cmds.append('true')

if args.interface:
  for i in range(0, len(args.interface)):
    cmds.append('--topology-parm')
    cmds.append('interface{}'.format(i))
    cmds.append(args.interface[i])
elif args.nr_interfaces:
  cmds.append('--topology-parm')
  cmds.append('ninterfaces')
  cmds.append(str(args.nr_interfaces))
else:
  raise Exception('pass either a list of interfaces or the number of ports')
cmds.append('--consumer-plugin')
cmds.append('org.change.v2.plugins.runner.ToFileConsumer')
cmds.append('--consumer-parm')
cmds.append('success')
cmds.append(args.success_out_file)
cmds.append('--consumer-parm')
cmds.append('fail')
cmds.append(args.fail_out_file)

#print(cmds)
start = time.time()
ret=subprocess.call(cmds)
end = time.time()
if ret == 0:
  print('success in {}s'.format(round(end - start, 3)))
else:
  print('failure', file=sys.stderr)
  sys.exit(-1)
#--input-packet-plugin
#org.change.v2.plugins.eq.NoPacketPlugin
#--topology-plugin
#org.change.v2.plugins.vera.VeraTopologyPlugin
#--topology-parm
#p4
#C:\Users\dragos\source\repos\symnet-neutron\inputs\axon\axon-ppc.p4
#--topology-parm
#commands
#C:\Users\dragos\source\repos\symnet-neutron\inputs\axon\commands.txt
#--topology-parm
#ninterfaces
#3
#--topology-parm
#noinputpacket
#true
#--topology-parm
#name
#copy2cpu
