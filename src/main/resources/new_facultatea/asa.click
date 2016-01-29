global_nat :: IPRewriter(keep 0 1,pattern 141.85.225.202 60000-65535 - - 2 3,keep 2 4,pattern 141.85.225.204 60000-65535 - - 5 6,keep 5 7);
dest_cl :: IPClassifier(dst net 172.16.2.254/24,dst net 172.16.4.254/22,-);
dest_clp :: IPClassifier(dst net 172.16.2.254/24,dst net 172.16.4.254/22,-);
unstatic_cl :: IPClassifier(dst host 141.85.225.151,dst host 141.85.225.152,-);
unstatic_rw :: IPRewriter(pattern - - 172.16.12.172 - 0 1,pattern - - 172.16.5.222 - 0 1,keep 0 1);
pn_1 :: Paint(1);
pn_2 :: Paint(2);
pn_3 :: Paint(3);
pn_0 :: Paint(0);
outside_in :: Null;
outside_out :: Null;
outside_ether_encap :: EtherEncap(0x0800,00:23:eb:bb:f1:4c,00:19:e72:a77:ff);
outside_vlan_encap :: VLANEncap(225);
out_PROF_CS :: Null;
out_etherEncap_PROF_CS :: EtherEncap(0x0800,00:23:eb:bb:f1:4d,-);
out_vlan_PROF_CS :: VLANEncap(302);
in_PROF_CS :: Null;
host_ether_PROF_CS :: HostEtherFilter(00:23:eb:bb:f1:4d);
in_vlan_PROF_CS :: VLANDecap;
ether_decap_PROF_CS :: EtherDecap;
pn_PROF_CS :: Paint(80);
isTCP1 :: IPClassifier(tcp,udp,-);
nat_cl_PROF_CS :: IPClassifier(src net 172.16.2.0/24,-);
opts_PROF_CS :: TCPOptions;

ps_PROF_CS :: PaintSwitch();
out_LABS_CS :: Null;
out_etherEncap_LABS_CS :: EtherEncap(0x0800,00:23:eb:bb:f1:4e,-);
out_vlan_LABS_CS :: VLANEncap(304);
in_LABS_CS :: Null;
host_ether_LABS_CS :: HostEtherFilter(00:23:eb:bb:f1:4e);
in_vlan_LABS_CS :: VLANDecap;
ether_decap_LABS_CS :: EtherDecap;
pn_LABS_CS :: Paint(5);
isTCP2 :: IPClassifier(tcp,udp,-);
static_cl_LABS_CS :: IPClassifier(src host 172.16.5.222,-);
static_rw_LABS_CS :: IPRewriter(pattern 141.85.225.152 - - - 0 1);
nat_cl_LABS_CS :: IPClassifier(src net 172.16.4.0/22,-);
opts_LABS_CS :: TCPOptions;
ps_LABS_CS :: PaintSwitch();
just_pass_paint :: Paint(100);
outside_host_ether :: HostEtherFilter(00:23:eb:bb:f1:4c);
outside_vlan_decap :: VLANDecap;
outside_ether_decap :: EtherDecap;
isTCP3 :: IPClassifier(tcp,udp,-);
opts_outside :: TCPOptions;
ps :: PaintSwitch();
cl_incoming :: IPClassifier( src host 37.128.224.6,dst net 141.85.228.0/26,dst  host 141.85.225.151 and dst tcp port 21,dst  host 141.85.225.151 and dst tcp port 80,dst  host 141.85.225.151 and dst tcp port 5000,dst  host 141.85.225.151 and dst tcp port 5001,dst  host 141.85.225.151 and dst tcp port 443,dst  host 141.85.225.152 and dst tcp port 22,dst  host 141.85.225.153,-);
	global_nat[0] -> [0]pn_0;
		pn_0[0] -> [0]unstatic_cl;
			unstatic_cl[0] -> [0]unstatic_rw;
				unstatic_rw[0] -> [0]ps;
					ps[0] -> [0]cl_incoming;
						cl_incoming[0] -> [0]dest_clp;
							dest_clp[0] -> [0]out_etherEncap_PROF_CS;
								out_etherEncap_PROF_CS[0] -> [0]out_vlan_PROF_CS;
									out_vlan_PROF_CS[0] -> [0]out_PROF_CS;
							dest_clp[1] -> [0]out_etherEncap_LABS_CS;
								out_etherEncap_LABS_CS[0] -> [0]out_vlan_LABS_CS;
									out_vlan_LABS_CS[0] -> [0]out_LABS_CS;
						cl_incoming[1] -> [0]dest_clp;
						cl_incoming[2] -> [0]dest_clp;
						cl_incoming[3] -> [0]dest_clp;
						cl_incoming[4] -> [0]dest_clp;
						cl_incoming[5] -> [0]dest_clp;
						cl_incoming[6] -> [0]dest_clp;
						cl_incoming[7] -> [0]dest_clp;
						cl_incoming[8] -> [0]dest_clp;
						cl_incoming[9] -> [0]Discard;
					ps[1] -> [0]dest_clp;
					ps[2] -> [0]dest_clp;
					ps[3] -> [0]cl_incoming;
				unstatic_rw[1] -> [0]Discard;
			unstatic_cl[1] -> [1]unstatic_rw;
			unstatic_cl[2] -> [2]unstatic_rw;
	global_nat[1] -> [0]just_pass_paint;
		just_pass_paint[0] -> [0]dest_cl;
			dest_cl[0] -> [0]ps_PROF_CS;
				ps_PROF_CS[0] -> [0]Discard;
				ps_PROF_CS[1] -> [0]Discard;
				ps_PROF_CS[2] -> [0]Discard;
				ps_PROF_CS[3] -> [0]Discard;
				ps_PROF_CS[4] -> [0]Discard;
				ps_PROF_CS[5] -> [0]Discard;
				ps_PROF_CS[6] -> [0]Discard;
				ps_PROF_CS[7] -> [0]Discard;
				ps_PROF_CS[8] -> [0]Discard;
				ps_PROF_CS[9] -> [0]Discard;
				ps_PROF_CS[10] -> [0]Discard;
				ps_PROF_CS[11] -> [0]Discard;
				ps_PROF_CS[12] -> [0]Discard;
				ps_PROF_CS[13] -> [0]Discard;
				ps_PROF_CS[14] -> [0]Discard;
				ps_PROF_CS[15] -> [0]Discard;
				ps_PROF_CS[17] -> [0]Discard;
				ps_PROF_CS[16] -> [0]Discard;
				ps_PROF_CS[19] -> [0]Discard;
				ps_PROF_CS[18] -> [0]Discard;
				ps_PROF_CS[21] -> [0]Discard;
				ps_PROF_CS[20] -> [0]Discard;
				ps_PROF_CS[23] -> [0]Discard;
				ps_PROF_CS[22] -> [0]Discard;
				ps_PROF_CS[25] -> [0]Discard;
				ps_PROF_CS[24] -> [0]Discard;
				ps_PROF_CS[27] -> [0]Discard;
				ps_PROF_CS[26] -> [0]Discard;
				ps_PROF_CS[29] -> [0]Discard;
				ps_PROF_CS[28] -> [0]Discard;
				ps_PROF_CS[31] -> [0]Discard;
				ps_PROF_CS[30] -> [0]Discard;
				ps_PROF_CS[34] -> [0]Discard;
				ps_PROF_CS[35] -> [0]Discard;
				ps_PROF_CS[32] -> [0]Discard;
				ps_PROF_CS[33] -> [0]Discard;
				ps_PROF_CS[38] -> [0]Discard;
				ps_PROF_CS[39] -> [0]Discard;
				ps_PROF_CS[36] -> [0]Discard;
				ps_PROF_CS[37] -> [0]Discard;
				ps_PROF_CS[42] -> [0]Discard;
				ps_PROF_CS[43] -> [0]Discard;
				ps_PROF_CS[40] -> [0]Discard;
				ps_PROF_CS[41] -> [0]Discard;
				ps_PROF_CS[46] -> [0]Discard;
				ps_PROF_CS[47] -> [0]Discard;
				ps_PROF_CS[44] -> [0]Discard;
				ps_PROF_CS[45] -> [0]Discard;
				ps_PROF_CS[51] -> [0]Discard;
				ps_PROF_CS[50] -> [0]Discard;
				ps_PROF_CS[49] -> [0]Discard;
				ps_PROF_CS[48] -> [0]Discard;
				ps_PROF_CS[55] -> [0]Discard;
				ps_PROF_CS[54] -> [0]Discard;
				ps_PROF_CS[53] -> [0]Discard;
				ps_PROF_CS[52] -> [0]Discard;
				ps_PROF_CS[59] -> [0]Discard;
				ps_PROF_CS[58] -> [0]Discard;
				ps_PROF_CS[57] -> [0]Discard;
				ps_PROF_CS[56] -> [0]Discard;
				ps_PROF_CS[63] -> [0]Discard;
				ps_PROF_CS[62] -> [0]Discard;
				ps_PROF_CS[61] -> [0]Discard;
				ps_PROF_CS[60] -> [0]Discard;
				ps_PROF_CS[68] -> [0]Discard;
				ps_PROF_CS[69] -> [0]Discard;
				ps_PROF_CS[70] -> [0]Discard;
				ps_PROF_CS[71] -> [0]Discard;
				ps_PROF_CS[64] -> [0]Discard;
				ps_PROF_CS[65] -> [0]Discard;
				ps_PROF_CS[66] -> [0]Discard;
				ps_PROF_CS[67] -> [0]Discard;
				ps_PROF_CS[76] -> [0]Discard;
				ps_PROF_CS[77] -> [0]Discard;
				ps_PROF_CS[78] -> [0]Discard;
				ps_PROF_CS[79] -> [0]Discard;
				ps_PROF_CS[72] -> [0]Discard;
				ps_PROF_CS[73] -> [0]Discard;
				ps_PROF_CS[74] -> [0]Discard;
				ps_PROF_CS[75] -> [0]Discard;
				ps_PROF_CS[80] -> [0]out_etherEncap_PROF_CS;
			dest_cl[1] -> [0]ps_LABS_CS;
				ps_LABS_CS[0] -> [0]Discard;
				ps_LABS_CS[1] -> [0]Discard;
				ps_LABS_CS[2] -> [0]Discard;
				ps_LABS_CS[3] -> [0]Discard;
				ps_LABS_CS[4] -> [0]Discard;
				ps_LABS_CS[5] -> [0]out_etherEncap_LABS_CS;
				ps_LABS_CS[6] -> [0]Discard;
				ps_LABS_CS[7] -> [0]Discard;
				ps_LABS_CS[8] -> [0]Discard;
				ps_LABS_CS[9] -> [0]Discard;
				ps_LABS_CS[10] -> [0]Discard;
				ps_LABS_CS[11] -> [0]Discard;
				ps_LABS_CS[12] -> [0]Discard;
				ps_LABS_CS[13] -> [0]Discard;
				ps_LABS_CS[14] -> [0]Discard;
				ps_LABS_CS[15] -> [0]Discard;
				ps_LABS_CS[17] -> [0]Discard;
				ps_LABS_CS[16] -> [0]Discard;
				ps_LABS_CS[19] -> [0]Discard;
				ps_LABS_CS[18] -> [0]Discard;
				ps_LABS_CS[21] -> [0]Discard;
				ps_LABS_CS[20] -> [0]Discard;
				ps_LABS_CS[23] -> [0]Discard;
				ps_LABS_CS[22] -> [0]Discard;
				ps_LABS_CS[25] -> [0]Discard;
				ps_LABS_CS[24] -> [0]Discard;
				ps_LABS_CS[27] -> [0]Discard;
				ps_LABS_CS[26] -> [0]Discard;
				ps_LABS_CS[29] -> [0]Discard;
				ps_LABS_CS[28] -> [0]Discard;
				ps_LABS_CS[31] -> [0]Discard;
				ps_LABS_CS[30] -> [0]Discard;
				ps_LABS_CS[34] -> [0]Discard;
				ps_LABS_CS[35] -> [0]Discard;
				ps_LABS_CS[32] -> [0]Discard;
				ps_LABS_CS[33] -> [0]Discard;
				ps_LABS_CS[38] -> [0]Discard;
				ps_LABS_CS[39] -> [0]Discard;
				ps_LABS_CS[36] -> [0]Discard;
				ps_LABS_CS[37] -> [0]Discard;
				ps_LABS_CS[42] -> [0]Discard;
				ps_LABS_CS[43] -> [0]Discard;
				ps_LABS_CS[40] -> [0]Discard;
				ps_LABS_CS[41] -> [0]Discard;
				ps_LABS_CS[46] -> [0]Discard;
				ps_LABS_CS[47] -> [0]Discard;
				ps_LABS_CS[44] -> [0]Discard;
				ps_LABS_CS[45] -> [0]Discard;
				ps_LABS_CS[51] -> [0]Discard;
				ps_LABS_CS[50] -> [0]Discard;
				ps_LABS_CS[49] -> [0]Discard;
				ps_LABS_CS[48] -> [0]Discard;
				ps_LABS_CS[55] -> [0]Discard;
				ps_LABS_CS[54] -> [0]Discard;
				ps_LABS_CS[53] -> [0]Discard;
				ps_LABS_CS[52] -> [0]Discard;
				ps_LABS_CS[59] -> [0]Discard;
				ps_LABS_CS[58] -> [0]Discard;
				ps_LABS_CS[57] -> [0]Discard;
				ps_LABS_CS[56] -> [0]Discard;
				ps_LABS_CS[63] -> [0]Discard;
				ps_LABS_CS[62] -> [0]Discard;
				ps_LABS_CS[61] -> [0]Discard;
				ps_LABS_CS[60] -> [0]Discard;
				ps_LABS_CS[68] -> [0]Discard;
				ps_LABS_CS[69] -> [0]Discard;
				ps_LABS_CS[70] -> [0]Discard;
				ps_LABS_CS[71] -> [0]Discard;
				ps_LABS_CS[64] -> [0]Discard;
				ps_LABS_CS[65] -> [0]Discard;
				ps_LABS_CS[66] -> [0]Discard;
				ps_LABS_CS[67] -> [0]Discard;
				ps_LABS_CS[76] -> [0]Discard;
				ps_LABS_CS[77] -> [0]Discard;
				ps_LABS_CS[78] -> [0]Discard;
				ps_LABS_CS[79] -> [0]Discard;
				ps_LABS_CS[72] -> [0]Discard;
				ps_LABS_CS[73] -> [0]Discard;
				ps_LABS_CS[74] -> [0]Discard;
				ps_LABS_CS[75] -> [0]Discard;
				ps_LABS_CS[80] -> [0]out_etherEncap_LABS_CS;
			dest_cl[2] -> [0]outside_ether_encap;
				outside_ether_encap[0] -> [0]outside_vlan_encap;
					outside_vlan_encap[0] -> [0]outside_out;
	global_nat[2] -> [0]dest_cl;
	global_nat[3] -> [0]pn_2;
		pn_2[0] -> [0]unstatic_cl;
	global_nat[4] -> [0]pn_1;
		pn_1[0] -> [0]unstatic_cl;
	global_nat[5] -> [0]dest_cl;
	global_nat[6] -> [0]pn_2;
	global_nat[7] -> [0]pn_1;
	pn_3[0] -> [0]unstatic_cl;
	outside_in[0] -> [0]outside_host_ether;
		outside_host_ether[0] -> [0]outside_vlan_decap;
			outside_vlan_decap[0] -> [0]outside_ether_decap;
				outside_ether_decap[0] -> [0]isTCP3;
					isTCP3[0] -> [0]opts_outside;
						opts_outside[0] -> [0]global_nat;
					isTCP3[1] -> [0]opts_outside;
					isTCP3[2] -> [0]pn_3;
	in_PROF_CS[0] -> [0]host_ether_PROF_CS;
		host_ether_PROF_CS[0] -> [0]in_vlan_PROF_CS;
			in_vlan_PROF_CS[0] -> [0]ether_decap_PROF_CS;
				ether_decap_PROF_CS[0] -> [0]pn_PROF_CS;
					pn_PROF_CS[0] -> [0]isTCP1;
						isTCP1[0] -> [0]opts_PROF_CS;
							opts_PROF_CS[0] -> [0]nat_cl_PROF_CS;
								nat_cl_PROF_CS[0] -> [1]global_nat;
								nat_cl_PROF_CS[1] -> [2]global_nat;
						isTCP1[1] -> [0]opts_PROF_CS;
						isTCP1[2] -> [0]dest_cl;
	in_LABS_CS[0] -> [0]host_ether_LABS_CS;
		host_ether_LABS_CS[0] -> [0]in_vlan_LABS_CS;
			in_vlan_LABS_CS[0] -> [0]ether_decap_LABS_CS;
				ether_decap_LABS_CS[0] -> [0]pn_LABS_CS;
					pn_LABS_CS[0] -> [0]static_cl_LABS_CS;
						static_cl_LABS_CS[0] -> [0]static_rw_LABS_CS;
							static_rw_LABS_CS[0] -> [0]isTCP2;
								isTCP2[0] -> [0]opts_LABS_CS;
									opts_LABS_CS[0] -> [0]nat_cl_LABS_CS;
										nat_cl_LABS_CS[0] -> [3]global_nat;
										nat_cl_LABS_CS[1] -> [4]global_nat;
								isTCP2[1] -> [0]opts_LABS_CS;
								isTCP2[2] -> [0]dest_cl;
							static_rw_LABS_CS[1] -> [0]Discard;
						static_cl_LABS_CS[1] -> [0]isTCP2;