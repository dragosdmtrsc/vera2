in :: FromDevice() -> lb :: IPClassifier(src net 192.168.1.0/24, src net 192.168.2.1/32, -)

exit :: ToDevice

lb[0] -> exit
lb[1] -> exit
lb[2] -> exit