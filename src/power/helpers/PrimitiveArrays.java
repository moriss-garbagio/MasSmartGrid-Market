package power.helpers;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.HashMap;

public class PrimitiveArrays {
	private static final HashMap<Class<?>, Class<?>> primitiveArray = new HashMap<Class<?>, Class<?>>();
	
	static {
		primitiveArray.put(boolean.class, boolean[].class);
		primitiveArray.put(byte.class, byte[].class);
		primitiveArray.put(char.class, char[].class);
		primitiveArray.put(short.class, short[].class);
		primitiveArray.put(int.class, int[].class);
		primitiveArray.put(long.class, long[].class);
		primitiveArray.put(float.class, float[].class);
		primitiveArray.put(double.class, double[].class);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Class<T[]> getArrayType(Class<T> type) {
		return (Class<T[]>) primitiveArray.get(type);
	}
	
	public static Object getArray(Type type, int size) {
		if(type == boolean.class) {
//			System.out.println("boolean");
			return new boolean[size];
		} else if (type == byte.class){
//			System.out.println("byte");
			return new byte[size];
		}  else if (type == char.class){
//			System.out.println("char");
			return new char[size];
		} else if (type == short.class){
//			System.out.println("short");
			return new short[size];
		} else if (type == int.class){
//			System.out.println("int");
			return new int[size];
		} else if (type == long.class){
//			System.out.println("long");
			return new long[size];
		} else if (type == float.class){
//			System.out.println("float");
			return new float[size];
		} else if (type == double.class){
//			System.out.println("double");
			return new double[size];
		} else if (type == void.class){ 
//			System.out.println("void");
			return null;
		} else {
//			System.out.println("other");
			return Array.newInstance((Class<?>) type, size);
		}
	}
}
