package org.change.v2.model.openflow;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

public class Decoder {
	
	
//	ALL	0xfffffffc
//	Controller	0xfffffffd
//	Table	0xfffffffe
//	Ingress Port	0xfffffff9
//	 Local	0xfffffff8
//	 Normal	0xfffffffa
//	 Flood	0xfffffffb
//	ANY	0xffffffff
	
	
	public static Entry<Long, Long> ipMaskToInterval(Long ip, Long mask)
	{
	    long ipv = ip;
	    long maskv = mask;
	    long addrS = 32 - maskv;
	    long lowerM = Long.MAX_VALUE << addrS;
		long higherM = Long.MAX_VALUE >>> (maskv + 31);
	    return new SimpleEntry<Long, Long>(ipv & lowerM, ipv | higherM);
	}
	
	public static Long decodeMAC(String value) {
		long v = 0;
		String[] ofVals = value.split(":");
		for (String ofVal : ofVals)
		{
			v = v * 256 + Long.parseUnsignedLong(ofVal, 16);
		}
		return v;
	}
	public static int convertNetmaskToCIDR(String nm){

		String[] bts = nm.split("\\.");
        int[] netmaskBytes = new int[4];

        int j = 0;
		for (String b : bts)
		{
			netmaskBytes[j++] = Integer.parseUnsignedInt(b);
		}
        int cidr = 0;
        boolean zero = false;
        for(int b : netmaskBytes){
            int mask = 0x80;

            for(int i = 0; i < 8; i++){
                int result = b & mask;
                if(result == 0){
                    zero = true;
                }else if(zero){
                    throw new IllegalArgumentException("Invalid netmask.");
                } else {
                    cidr++;
                }
                mask >>>= 1;
            }
        }
        return cidr;
    }

	
	
	public static Long decodePort(String value)
	{
		if (value == null) value = "";
		if ("ALL".equals(value))  return (long) 0xfffffffc;
		if ("CONTROLLER".equals(value))  return (long) 0xfffffffd;
		if ("TABLE".equals(value))  return (long) 0xfffffffe;
		if ("IN_PORT".equals(value))  return (long) 0xfffffff9;
		if ("LOCAL".equals(value))  return (long) 0xfffffff8;
		if ("NORMAL".equals(value))  return (long) 0xfffffffa;
		if ("FLOOD".equals(value))  return (long) 0xfffffffb;
		if ("ANY".equals(value))  return (long) 0xffffffff;
		return Long.parseLong(value);
	}
	
	public static Long decodeIP4(String value)
	{
		long v = 0;
		String[] ofVals = value.split("\\.");
		for (String ofVal : ofVals)
		{
			v = v * 256 + Long.parseUnsignedLong(ofVal);
		}
		return v;
	}
	
	public static Long decodeType(String field, String value) 
	{
		try
		{
			FormatType ft = TypeMappings.TYPE_MAPPINGS.get(field);
			Long theValue = (long) 0;
			switch (ft)
			{
			case CtState:
				break;
			case Decimal:
				theValue = Long.parseUnsignedLong(value);
				break;
			case Ethernet:
				theValue = decodeMAC(value);
				break;
			case Frag:
				break;
			case Hexadecimal:
				theValue = decodeLong(value);
				break;
			case IPv4:
				theValue = decodeIP4(value);
				break;
			case IPv6:
				break;
			case OpenFlow10port:
			case OpenFlow11port:
				theValue = decodePort(value);
				break;
			case TCPFlags:
				break;
			case TunnelFlags:
				break;
			default:
				break;
			
			}
			return theValue;
		}
		catch (NullPointerException ex)
		{
			throw new NullPointerException(String.format("In decodeType(%s, %s)\n%s", field, value, ex.toString()));
		}
	}

	public static Long decodeLong(String value) {
		Long theValue;
		if (value.toLowerCase().startsWith("0x"))
		{
			theValue = Long.parseLong(value.substring(2), 16);
		}
		else
		{
			theValue = Long.parseLong(value);
		}
		return theValue;
	}

	public static boolean isField(String firstOne) {
		if (firstOne.contains("["))
		{
			firstOne = firstOne.substring(0, firstOne.indexOf("["));
		}
		return TypeMappings.TYPE_MAPPINGS.containsKey(firstOne);
	}

	
}
