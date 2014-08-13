package power.helpers;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.internal.Primitives;

public class XmlTools {
	
	public enum XmlAttribute { name, value, type, format }
	public enum FormatType { json, xml }
	
	public static <T extends Enum<T>> ArrayList<Node> getAllNodes(Node xml, Enum<T> tag) {
		NodeList results = xml.getChildNodes();
		ArrayList<Node> nodeList = new ArrayList<Node>();
		for (int index = 0; index < results.getLength(); index++) {
			Node node = results.item(index);
			if (tag.toString().equals(node.getNodeName())) {
				nodeList.add(node);
			}
		}
		return nodeList;
	}
	
	public static <T extends Enum<T>> Node getUptoOneNode(Node xml, Enum<T> tag) {
		if (xml == null) return null;
		ArrayList<Node> nodeList = getAllNodes(xml, tag);
		
		if (nodeList.isEmpty()) {
			return null;
		} else if (nodeList.size() == 1) {
			return nodeList.get(0);
		} else {
			System.err.println("More than one elemenet of type " + tag + " exists");
			(new Exception()).printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	public static <T extends Enum<T>> Node getExactlyOneNode(Node xml, Enum<T> tag) {
		ArrayList<Node> nodeList = getAllNodes(xml, tag);
		
		if (nodeList.size() == 1) { 
			return nodeList.get(0);
		} else if (nodeList.size() > 1) {
			System.err.println("More than one elemenet of type " + tag + " exists");
			(new Exception()).printStackTrace();
			System.exit(1);
		} else {
			System.err.println("Could not find an element of type " + tag);
			(new Exception()).printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	public static <T extends Enum<T>> ArrayList<Node> getAtLeastOneNode(Node xml, Enum<T> tag) {
		ArrayList<Node> nodeList = getAllNodes(xml, tag);
		
		if (nodeList.size() > 0) { 
			return nodeList;
		} else {
			System.err.println("Could not find at least one element of type " + tag);
			(new Exception()).printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	public static <S extends Enum<S>, T extends Enum<T>> T getAttributeValue(Node xml, Enum<S> attribute, Class<T> enumType) {
		if (xml == null) return null;
		Node node = xml.getAttributes().getNamedItem(attribute.toString());
		if (node != null) {
			T enumValue = null;
			try {
				enumValue = Enum.valueOf(enumType, node.getNodeValue());
			} catch (Exception e) {
				System.err.println("Could not match the attribute " + node.toString() + " to a type of " + enumType.getCanonicalName());
				(new Exception()).printStackTrace();
				System.exit(1);
			}
			return enumValue; 
		} else if (enumType.getEnumConstants().length > 0){
			return enumType.getEnumConstants()[0];
		} else {
			return null;
		}
	}
	
	public static <T extends Enum<T>> String getHardAttributeValue(Node xml, Enum<T> attribute) {
		if (xml == null) return null;
		Node node = xml.getAttributes().getNamedItem(attribute.toString());
		if (node != null) {
			return node.getNodeValue();
		} else {
			System.err.println("Could not find the attribute " + attribute);
			(new Exception()).printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	public static <T extends Enum<T>> String getAttributeValue(Node xml, Enum<T> attribute) {
		if (xml == null) return null;
		Node node = xml.getAttributes().getNamedItem(attribute.toString());
		if (node != null) {
			return node.getNodeValue();
		} else {
			return null;
		}
	}
	
	public static <S extends Enum<S>, T extends Enum<T>> Object getListFromNode(Node xml, Enum<S> tag, Enum<T> attribute, Class<?> type) {
		ArrayList<Node> nodeList = getAllNodes(xml, tag);
		Class<?> realType = Primitives.wrap(type);  
		Constructor<?> constructor = null;
		try {
			Object list = Array.newInstance(realType, nodeList.size());
			constructor = realType.getConstructor(new Class[] { String.class });
			
			for (int index = 0; index < nodeList.size(); index++) {
				Array.set(list, index, constructor.newInstance(getAttributeValue(nodeList.get(index), attribute)));
			}
			
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	public static <S extends Enum<S>, T extends Enum<T>, U> U getTypedListFromNode(Node xml, Enum<S> tag, Enum<T> attribute, Class<U> type) {
		if(!type.isArray() || type.getComponentType().isArray()) {
			System.err.println("A one dimensional array type is expected");
			(new Exception()).printStackTrace();
			System.exit(1);
		}
		
		ArrayList<Node> nodeList = getAllNodes(xml, tag);

		Class<?> realType = Primitives.wrap(type.getComponentType());  
		Constructor<?> constructor = null;
		
		try {
			Object list = Array.newInstance(type.getComponentType(), nodeList.size());
			constructor = realType.getConstructor(new Class[] { String.class });
			
			for (int index = 0; index < nodeList.size(); index++) {
				Array.set(list, index, constructor.newInstance(getAttributeValue(nodeList.get(index), attribute)));
			}
			
			return type.cast(list);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
