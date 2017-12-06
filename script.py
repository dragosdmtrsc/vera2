
import os

type_list= { 'HDR' : 1<<0, 'ARR' : 1<<1, 'FLD' : 1<<2, 'FLDLIST' : 1<<3, 'VAL' : 1<<4, 'C_REF': 1<<5, 'M_REF' : 1 << 6, 'R_REF' : 1<<7, 'FLC_REF' : 1<<8}

boilerplate='package org.change.v2.p4.model.actions.primitives;\nimport org.change.v2.p4.model.actions.*;\nimport java.util.Collections;\nimport java.util.List;\n\npublic class '

constructorplate = '    public %s() {  super(P4ActionType.%s, "%s"); init(); }'

get_list_plate = '\n\n    @Override\n    public List<P4ActionParameter> getParameterList() {\n        return Collections.unmodifiableList(super.getParameterList());\n    }'

init_plate = '    protected void init() {\n%s\n    }\n'

init_line_plate = 'super.getParameterList().add(new P4ActionParameter(%d, "%s"));'


add_to_ref_plate = '        this.register(new %s());'

f=open('action-defs.txt')

name2name = {}

fnc = open('raw_actions.txt')
fnp = open('actions.txt')

cannames=[]
manipnames=[]
for l in fnc:
  cannames.append(l.strip())
for l in fnp:
  manipnames.append(l.strip())

for tup in zip(cannames, manipnames):
  name2name[tup[0]] = tup[1]

for l in f:
  l=l.strip()
  splitted=l.split(' ')
  action_name=splitted[0]
  generated = boilerplate + name2name[action_name] + ' extends P4Action {\n' + constructorplate % (name2name[action_name], name2name[action_name], action_name)
  generated += get_list_plate + '\n'
  line_by_line = ''
  if len(splitted) > 1:
    for i in range(1, len(splitted),2):
      arg_name=splitted[i]
      arg_kind=splitted[i+1]
      args=[]
      if '|' in arg_kind:
        args=arg_kind.strip().split('|')
      else:
        args.append(arg_kind)
      suma=0
      for v in args:
        suma+=type_list[v]
      #print 'action_name=' + action_name + ',kind=' + str(suma) + ',name=' + arg_name
      line_by_line += '        ' + (init_line_plate % (suma, arg_name)) +'\n'
  generated += init_plate % (line_by_line)
  generated += '\n}'
  fo=open('src/main/java/org/change/v2/p4/model/actions/primitives/' + name2name[action_name] + '.java', 'w')
  fo.write(generated)
  fo.close()
  print add_to_ref_plate % (name2name[action_name])
    
