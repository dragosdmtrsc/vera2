
def to_bytes(s):
    l = len(s) // 8
    x=int(s, 2)
    return x.to_bytes(l, 'big')