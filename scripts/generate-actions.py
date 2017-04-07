

template = 'package org.change.v2.model.openflow.actions;\n' + \
    'import org.change.v2.model.openflow.Action;\n' + \
    'import org.change.v2.model.openflow.FlowVisitor;\n\n' + \
    'import org.change.v2.model.openflow.ActionType;\n\n' + \
    'public class {0} extends Action {{\npublic {0}() {{ super(ActionType.{1}); }}\n@Override\n	public void accept(FlowVisitor visitor) {{\nvisitor.visit(this);\n}}\n}}'
codegen = 'void visit(%s action);\n'

f = open('txt.txt')
fouut = open('fout.java', 'w')

for line in f:
    fout = open(line.strip() + 'Action.java', 'w')
    fout.write(template.format(line.strip() + 'Action', line.strip()))
    fout.close()
    fouut.write(codegen.format(line.strip() + 'Action'))
fouut.close()
    