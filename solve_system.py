from sympy import *
import sys
import json
from sympy.solvers import solve


def solve_eqs(fix, fiy):
    matx=json.load(open(fix + '_mat'))
    bx=json.load(open(fix + '_b'))
    symx=symbols(json.load(open(fix + '_syms')))
    matx=[x + bx[i] for i, x in enumerate(matx)]
    solvedx = next(iter(linsolve(Matrix(matx), symx)))
    maty=Matrix(json.load(open(fiy + '_mat')))
    by=json.load(open(fiy + '_b'))
    symsy=symbols(json.load(open(fiy + '_syms')))
    sysy=maty * Matrix(symsy)
    neweqy=sysy.subs([(x, y) for x, y in zip(symx, solvedx)])
    symsy=[x for x in symsy if x not in symx]
    neweqy=[x - by[i][0] for i, x in enumerate(neweqy)]
    return next(iter(linsolve(neweqy, symsy)), None) is not None

if __name__== '__main__':
    print solve_eqs(sys.argv[1], sys.argv[2])