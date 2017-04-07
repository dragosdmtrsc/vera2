package org.change.v2.model.openflow;

public class Decoder {
	
	
//	ALL	0xfffffffc
//	Controller	0xfffffffd
//	Table	0xfffffffe
//	Ingress Port	0xfffffff9
//	 Local	0xfffffff8
//	 Normal	0xfffffffa
//	 Flood	0xfffffffb
//	ANY	0xffffffff
	
	public static Long decodeMAC(String value) {
		long v = 0;
		String[] ofVals = value.split(":");
		for (String ofVal : ofVals)
		{
			v = v * 16 + Long.parseUnsignedLong(ofVal, 16);
		}
		return v;
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
		String[] ofVals = value.split(".");
		for (String ofVal : ofVals)
		{
			v = v * 16 + Long.parseUnsignedLong(ofVal);
		}
		return v;
	}
	
	public static Long decodeType(String field, String value)
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

	public static Long decodeLong(String value) {
		Long theValue;
		if (value.toLowerCase().startsWith("0x"))
		{
			theValue = Long.parseLong(value.substring(2), 16);
		}
		else
		{
			theValue = Long.parseLong(value, 16);
		}
		return theValue;
	}
	
}
