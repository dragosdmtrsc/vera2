in :: Null;
out :: Null;
rw :: IPRewriter (pattern 10.0.0.1 9999 9.9.9.9 100 0 1);

disc :: Discard;

in -> rw[0] -> EtherEncap(2048, 00:23:eb:bb:f1:4c, 00:23:eb:bb:f1:4d) -> VLANEncap(225) -> out;
rw[1] -> disc;
