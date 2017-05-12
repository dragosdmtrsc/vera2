package org.change.v2.model.openflow;

public class QualifiedField {
	private String name;
	private FormatType type;
	private int startBit, endBit;

	@Override
	public String toString() {
		return "QualifiedField [name=" + name + ", type=" + type
				+ ", startBit=" + startBit + ", endBit=" + endBit + "]";
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public FormatType getType() {
		return type;
	}

	public int getStartBit() {
		return startBit;
	}

	public int getEndBit() {
		return endBit;
	}
	
	public static QualifiedField fromString(String decode)
	{
		String name;
		if (decode.contains("["))
		{
			name = decode.substring(0, decode.indexOf('['));
		}
		else
		{
			name = decode;
		}
		try
		{
			return fromString(decode, TypeMappings.LEN_MAPPINGS.get(name));
		}
		catch (Exception ex)
		{
			throw new IllegalArgumentException("Failed to decode " + decode, ex);
		}
	}

	public static QualifiedField fromString(String decode, int maxLen)
	{
		String name = decode;
		int startBit = 0;
		int endBit = -1;
		if (decode.contains("["))
		{
			name = decode.substring(0, decode.indexOf('['));
			String restOfString = decode.substring(decode.indexOf("[") + 1, decode.indexOf(']'));
			int intervalStart = restOfString.indexOf("..");
			if (intervalStart < 0)
			{
				startBit = 0;
				endBit = -1;
			}
			else
			{
				String firstNumber = restOfString.substring(0, intervalStart);
				String secondNumber = restOfString.substring(intervalStart + 2);
				try
				{
					startBit = Integer.parseInt(firstNumber);
				}
				catch (NumberFormatException nfe)
				{
				}
				
				try
				{
					endBit = Integer.parseInt(secondNumber);
				}
				catch (NumberFormatException nfe)
				{
				}
			}
			
		}
		
		if (endBit < 0) endBit = TypeMappings.LEN_MAPPINGS.get(name);
		QualifiedField theField = new QualifiedField();
		theField.endBit = endBit;
		theField.startBit = startBit;
		theField.type = TypeMappings.TYPE_MAPPINGS.get(name);
		theField.name = name;
		
		return theField;
	}
}
