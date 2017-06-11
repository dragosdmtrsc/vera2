f=open('code')
fout = open('out.txt', 'w')
i=0
for line in f:
    if line.startswith('val bl'):
        line = line.replace('blll', 'bl%d' % i)
    elif line.startswith('val str'):
        line = line.replace('str', 'inport%d' % i)
    elif line.startswith('val exitPort'):
        line = line.replace('exitPort', 'exitPort%d' % i)
        fout.write(line)
        line = 'links+=(%s -> %s)\n' % ('exitPort%d' % (i-1), 'inport%d' % i)
        i = i+1
    fout.write(line)
fout.close()