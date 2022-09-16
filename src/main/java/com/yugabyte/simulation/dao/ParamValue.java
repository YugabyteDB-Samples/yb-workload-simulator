package com.yugabyte.simulation.dao;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.NumberFormat;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@JsonSerialize(using = ParamValue.Serializer.class)
@JsonDeserialize(using = ParamValue.Deserializer.class)
public class ParamValue {
	private static final String INT = "intValue";
	private static final String BOOL = "boolValue";
	private static final String STRING = "stringValue";
	
	public static class Serializer extends StdSerializer<ParamValue> {
		public Serializer() {
			this(null);
		}
		public Serializer(Class<ParamValue> t) {
			super(t);
		}
		@Override
		public void serialize(ParamValue value, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeStartObject();
			switch (value.type) {
			case BOOLEAN:
				gen.writeBooleanField(BOOL, value.getBoolValue());
				break;
			case NUMBER:
				gen.writeNumberField(INT, value.getIntValue());
				break;
			case STRING:
				gen.writeStringField(STRING, value.getStringValue());
				break;
			}
			gen.writeStringField("type", value.getType().toString());
			gen.writeEndObject();
		}
		
	}
	
	public static class Deserializer extends StdDeserializer<ParamValue> {
		public Deserializer() {
			this(null);
		}
		public Deserializer(Class<ParamValue> t) {
			super(t);
		}
		private void checkNotNull(ValueNode node, String type) {
			if (node == null) {
				throw new IllegalArgumentException("Invalid ParamValue was expected to have a value in " + type + " but this field was not specified");
			}
		}
		@Override
		public ParamValue deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {
			String readAs = null;
			JsonNode node = jp.getCodec().readTree(jp);
			JsonNode type = node.get("type");
			if (type instanceof TextNode) {
				readAs = ((TextNode)type).asText();
			}
			if ((ParamType.NUMBER.toString().equals(readAs)) || (readAs == null && node.get(INT) != null)) {
				IntNode intNode = (IntNode)node.get(INT);
				checkNotNull(intNode, INT);
				return new ParamValue((Integer)intNode.numberValue());
			}
			if ((ParamType.BOOLEAN.toString().equals(readAs)) || (readAs == null && node.get(BOOL) != null)) {
				BooleanNode boolValue = (BooleanNode)node.get(BOOL);
				checkNotNull(boolValue, BOOL);
				return new ParamValue(boolValue.asBoolean());
			}
			if ((ParamType.STRING.toString().equals(readAs)) || (readAs == null && node.get(STRING) != null)) {
				TextNode textNode = (TextNode)node.get(STRING);
				checkNotNull(textNode, STRING);
				return new ParamValue(textNode.asText());
			}
			else {
				throw new IllegalArgumentException("Invalid ParamValue with no type or attributes set: " + jp);
			}
		}
	}

	private int intValue;
	private String stringValue;
	private boolean boolValue;
	private ParamType type;
	
	// DO not use, for serialization only
	public ParamValue() {}
	public ParamValue(int value) {
		this.intValue = value;
		this.type = ParamType.NUMBER;
	}
	
	public ParamValue(String value) {
		this.stringValue = value;
		this.type = ParamType.STRING;
	}
	
	public ParamValue(boolean value) {
		this.boolValue = value;
		this.type = ParamType.BOOLEAN;
	}
	
	private void checkType(ParamType desiredType) {
		if (desiredType != this.type) {
			// NOTE: We cannot throw an exception here, because the jackson serializer will attempt to read 
			// each value in turn to serialize this object, which will throw this exception.
			// We should implement a custom serializer.
			throw new InvalidParameterException("Attempt to get parameter of type " + type.toString() + " as a " + desiredType.toString());
		}
	}
	
	public int getIntValue() {
		checkType(ParamType.NUMBER);
		return this.intValue;
	}

	public String getStringValue() {
		checkType(ParamType.STRING);
		return this.stringValue;
	}

	public boolean getBoolValue() {
		checkType(ParamType.BOOLEAN);
		return this.boolValue;
	}
	
	public ParamType getType() {
		return type;
	}
	
	@Override
	public String toString() {
		switch (type) {
		case BOOLEAN: return Boolean.toString(this.boolValue);
		case NUMBER:  return NumberFormat.getInstance().format(this.intValue);
		case STRING:  return "'" + this.stringValue + "'";
		default: return "";
		}
	}
}
