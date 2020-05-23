package net.erikdevelopernot.jsonP;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

/*
 * jackson, fangiong, gson code invoke comes from https://github.com/terence-taih/aws-speed/
 * source page - https://blog.overops.com/the-ultimate-json-library-json-simple-vs-gson-vs-jackson-vs-json/
 */
//jackson stuff
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
//Fangiong stuff
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.gson.JsonArray;
//Google GSON stuff
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.erikdevelopernot.jsonP.document.*;
import net.erikdevelopernot.jsonP.document.JsonPJson.EntrySet;
import net.erikdevelopernot.jsonP.document.JsonPJson.KeyEntrySet;
import net.erikdevelopernot.jsonP.parsers.*;





public class Driver {

	static int stringValLength;
	static int stringKeyLength;
	static int boolT;
	static int boolF;
	static long longTotal;
	static double doubleTotal;
	static int nils;
	
	static final String bicycleCrashJson = "/Users/user1/udemy/CPP/UdemyCPP/jsonP_dyn_drvr/samples/bicycle-crash-data-chapel-hill-region.json";
	static final String webappJson = "/Users/user1/udemy/CPP/UdemyCPP/jsonP_dyn_drvr/samples/webapp2.json";
	static final String canadaJson = "/Users/user1/udemy/CPP/UdemyCPP/jsonP_dyn_drvr/samples/canada.json";
	static final String sample5Json = "/Users/user1/udemy/CPP/UdemyCPP/jsonP_dyn_drvr/samples/sample5.json";
	static final String citmJson = "/Users/user1/udemy/CPP/UdemyCPP/jsonP_dyn_drvr/samples/citm_catalog.json.txt";
	static final String simple5Json = "/Users/user1/eclipse-workspace/jsonP_java/examples/simple3.json";
	static final String largeJson = "/Users/user1/Downloads/large.json";
	
	public static void main(String[] args) throws Exception {
		//uncomment to test manual json creation
//		testManual();
//		testJsonPElementAccess("/Users/user1/eclipse-workspace/jsonP_java/examples/simple5.json");
//		testJsonPElementAccess("/Users/user1/Downloads/large.json");
		testJsonPElementAccess(largeJson, 1);
		
		//uncomment to test Fangiong
//		testFangiongParser("/Users/user1/Downloads/large.json");
//		testFangiongParser("/Users/user1/eclipse-workspace/jsonP_java/examples/simple5.json");

		//uncomment to test Jackson
		testJackson(webappJson,1);

		//uncomment to test Gson
//		testGson("/Users/user1/Downloads/large.json");
//		testGson("/Users/user1/eclipse-workspace/jsonP_java/examples/simple5.json");
		testGson(webappJson, 1);
		System.exit(0);
		
//		byte[] jsonData = Files.readAllBytes(Paths.get("/Users/user1/eclipse-workspace/jsonP_java/examples/simple5.json"));
//		byte[] jsonData = Files.readAllBytes(Paths.get("/Users/user1/Downloads/large.json"));
//		byte[] jsonData = Files.readAllBytes(Paths.get("/Users/user1/udemy/CPP/UdemyCPP/jsonP_dyn_drvr/samples/med.json.orig"));
		
		long s = System.currentTimeMillis();

//		byte[] jsonData = getBytes("/Users/user1/Downloads/large.json");
		byte[] jsonData = getBytes("/Users/user1/eclipse-workspace/jsonP_java/examples/simple5.json");

		
		JsonP_Parser parser = new JsonP_Parser(jsonData, Common.DEFAULTS); //, Common.CONVERT_NUMERICS);
		JsonPJson jsonPJson = parser.parse();
////jsonPJson.crap();	
//jsonData=null;
//parser=null;
//System.gc();

		long f = System.currentTimeMillis();
		
		byte[] j = jsonPJson.stringify(0, false);
		
		long f2 = System.currentTimeMillis();
		System.out.println("\n\n" + new String(j) + "\n");

		System.out.println("Parse Time: " + (f-s) + "m/s");
		System.out.println("Parse Stats:");
		System.out.println("  stack: " + parser.getParseStats().stackIncreases);
		System.out.println("  data: " + parser.getParseStats().dataIncreases);
		System.out.println("Stringify time: " + (f2-f) + "m/s");
//System.gc();
// start remove
//int id = jsonPJson.getObjectId("/obj_ptr_will_break/key__2", "/");
//System.out.println("id: " + id);
// end remove
//		System.console().readLine();
		String s1 = "/obj_ptr_will_break/key__2/true"; String s2 = "/";
		int objID = jsonPJson.getObjectId("/obj_ptr_will_break/key1", s2);
		s = System.currentTimeMillis();
		
//		for (int i=0; i<1000; i++)
//			jsonPJson.getStringValue(0, objID);
		
		jsonPJson.addValueType(Common.string, jsonPJson.getDocRoot(), "Extended_1", "exteded 1 value");
		
		KeyEntrySet keys = (KeyEntrySet) jsonPJson.getContainerElements(jsonPJson.getDocRoot());
		byte type;
		
		for (int i=0; i<keys.getNumberOfElements(); i++) {
			System.out.println(keys.getKeyName(i));
			type = keys.getElementType(i);
			
			if (type == Common.string) {
				System.out.println("  value: " + keys.getAsString(i));
			} else if (type == Common.bool_false) {
				System.out.println("  value: " + false);
			} else if (type == Common.bool_true) {
				System.out.println("  value: " + true);
			} else if (type == Common.array) {
				EntrySet es = jsonPJson.getContainerElements(keys.getAsArray(i));
				System.out.println("  value: array with " + es.getNumberOfElements() + " elements");
			} else if (type == Common.object) {
				KeyEntrySet ks = (KeyEntrySet) jsonPJson.getContainerElements(keys.getAsObject(i));
				System.out.println("  value: object with " + ks.getNumberOfElements() + " keys");
			} else if (type == Common.numeric_double) {
				System.out.println("  value: " + keys.getAsDouble(i));
			} else if (type == Common.numeric_long) {
				System.out.println("  value: " + keys.getAsLong(i));
			} else if (type == Common.nil) {
				System.out.println("  value: NULL");
			} else if (type == Common.array) {
				
			}
		}
		
		f = System.currentTimeMillis();
		System.out.println(" Time: " + (f-s) + "m/s");
		System.out.println(keys.getNumberOfElements());
		System.out.println(new String(jsonPJson.stringify(0, false)));
	}
	
	
	private static void testManual() {
		try {
			JsonPJson json = new JsonPJson(Common.object, 20, 1024, Common.DEFAULTS);
		
			json.addValueType(Common.string, 0, "key1", "value1");
			json.addValueType(Common.string, 0, "akey2", "value2");
			int key3ID = json.addValueType(Common.string, 0, "Key3", "value3");
			json.addValueType(Common.string, 0, "akey4", "value4");
			json.addValueType("/", "/", "AKey", Common.string, "AKey Value");
			json.addValueType(Common.bool_true, 0, "Bool_T", null);
			json.addValueType(Common.bool_false, 0, "Bool_f", null);
			json.addValueType(Common.numeric_long, 0, "nLong", new Long(987654321));
			json.addValueType(Common.numeric_double, 0, "nDouble", new Double(12345.9876));
			json.addValueType(Common.nil, 0, "nil", null);
			
			int objContainer = json.addContainer("Container_1", 3, 0, Common.object);
			System.out.println("objContainer id: " + objContainer);
			json.addValueType(Common.string, objContainer, "abc", "value abc");
			json.addValueType(Common.string, objContainer, "Zee", "value Zee");
			json.addValueType(Common.string, objContainer, "ABC", "value ABC");
			json.addValueType(Common.bool_true, objContainer, "Bool_T", null);
			json.addValueType(Common.bool_false, objContainer, "Bool_f", null);
			json.addValueType(Common.numeric_long, objContainer, "Long", new Long(987654321));
			
			int arryContainer = json.addContainer("Array", 5, objContainer, Common.array);
//			int arryContainer = json.addContainer("/Container_1/", "/", "Array", 5, Common.array);
			json.addValueType(Common.numeric_long, arryContainer, null, new Long(1));
			json.addValueType(Common.numeric_long, arryContainer, null, new Long(2));
			json.addValueType(Common.numeric_long, arryContainer, null, new Long(3));
			System.out.println("Number of members in array: " + json.getMembersCount(arryContainer));
			json.addValueType(Common.numeric_long, arryContainer, null, new Long(4));
			json.addValueType(Common.numeric_long, arryContainer, null, new Long(5));
			
//			System.out.println("Number of members in array: " + json.getMembersCount(arryContainer));
			
			int embedObj = json.addContainer("/Container_1/Array", "/", null, 1, Common.object);
//			int embedObj = json.addContainer("www", 1, arryContainer, Common.object);
			json.addValueType(Common.string, embedObj, "embed_key", "value");
			json.addValueType("/Container_1/Array/5", "/", "embed_key2", Common.bool_true, null);
			int embedAry = json.addContainer("Container_1/Array", "/", null, 1, Common.array);
			json.addValueType(Common.string, embedAry, null, "value");
			json.addContainer("/Container_1/Array/6", "/", null, 4, Common.array);
			json.addValueType("/Container_1/Array/6/1", "/", null, Common.bool_false, null);
			json.addValueType("/Container_1/Array/6/1", "/", null, Common.numeric_double, new Double(9876.1234));
			
			json.addValueType(Common.string, objContainer, "After_Array", "After");
			
			System.out.println(new String(json.stringify(0, true)));
			json.deleteValue("/Container_1/Zee", "/");
			json.deleteValue("/Container_1/Array", "/");
			
			int retVal = json.updateValue("/Container_1/Long", "/", Common.numeric_double, new Double(98234.234993));
			if (retVal < 0) {
				System.out.println("Error in updateValue: " + json.getErrorCode());
			}
			
			System.out.println("update Return val: " + retVal);
			System.out.println(new String(json.stringify(0, true)));
			
//			int id = json.getObjectId("/Bool_f", "/");
//			System.out.println("ID of Bool_f key: " + id);
			
//			double d = json.getDoubleValue("/Container_1/Array/6/1/1", "/");
			double d = json.getDoubleValue("/nDouble", "/");
			System.out.println("Double value: " + d);
			
			long l = json.getLongValue("/nLong", "/");
			System.out.println("Long value: " + l);
			
			boolean b = json.getBooleanValue("/Bool_T", "/");
			System.out.println("boolean value: " + b);
			
			String s = json.getStringValue("/AKeyQ", "/");
			System.out.println("string value: " + s);
			
			System.exit(1);
		} catch (JsonPException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	private static byte[] getBytes(String fileName) {
		try {
			File f = new File(fileName);
			FileInputStream fis = new FileInputStream(f);
			byte bytes[] = new byte[(int)f.length()];
			fis.read(bytes);
//			for (int i=0; i<bytes.length; i++) {
//				bytes[i] = (byte)fis.read();
//			System.out.println(bytes[i]);	
//			}
			
			return bytes;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		return null;
	}
	
	
//	private static void testJackson(byte[] jsonData) {
	private static void testJackson(String jsonDataPath, int cnt) {
		try {
			long pTime = 0L;
			long sTime = 0L;
			long wTime = 0L;
			
			for (int i=0; i<cnt; i++) {
				long s = System.currentTimeMillis();
				
				FileReader reader = new FileReader(jsonDataPath);
				ObjectMapper objectMapper = new ObjectMapper();
				JsonNode rootNode = objectMapper.readTree(reader);
			
	
				long f = System.currentTimeMillis();
				
				byte[] s2 = objectMapper.writeValueAsBytes(rootNode);
				
				long f2 = System.currentTimeMillis();
				
//				System.out.println("Jackson Parse Time: " + (f-s) + "m/s");
//				System.out.println("  Stringify time: " + (f2-f) + "m/s");
//				System.console().readLine();
			
				pTime += (f-s);
				sTime += (f2-f);
				
				s = System.currentTimeMillis();
	
				if (rootNode.isObject()) {
					parseJacksonObject(rootNode);
				} else {
					parseJacksonArray(rootNode);
				}
				
				f = System.currentTimeMillis();
//				System.out.println("Time: " + (f-s) + "m/s");
				wTime += (f-s);
			}
			
			System.out.println("Jackson parse Time: " + pTime + "m/s");
			System.out.println("Jackson stringify Time: " + sTime + "m/s");
			System.out.println("Jackson walk Time: " + wTime + "m/s");
			System.out.println("Jackson total Time: " + (wTime+pTime+sTime) + "m/s");
			
			System.out.println("Jackson stringValLength: " + stringValLength);
			System.out.println("Jackson stringKeyLength: " + stringKeyLength);
			System.out.println("Jackson boolT: " + boolT);
			System.out.println("Jackson boolF: " + boolF);
			System.out.println("Jackson longTotal: " + longTotal);
			System.out.println("Jackson doubleTotal: " + doubleTotal);
			System.out.println("\n");
			resetCounters();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void parseJacksonObject(JsonNode obj) {
		Iterator<Entry<String, JsonNode>> it = obj.fields();
		JsonNode el;
		Entry<String, JsonNode> e;
		
		while (it.hasNext()) {
			e = it.next();
//			System.out.print(e.getKey() + "-\n");
			stringKeyLength += e.getKey().length();
			el = e.getValue();
			
			if (el.isArray()) {
				parseJacksonArray(el);
			} else if (el.isObject()) {
				parseJacksonObject(el);
			} else if (el.isTextual()) {
				//					System.out.print(el.getAsString() + "\n");
				stringValLength += el.asText().length();
//System.out.println("-" + el.asText() + "-" + el.asText().length());
			} else if (el.isBoolean()) {
				boolean b = el.asBoolean();
				//					System.out.print(b);
				if (b)
					boolT++;
				else
					boolF++;
			} else if (el.isNull()) {
				//					System.out.print("NULL");
				nils++;
			} else if (el.isDouble()) {
				//					System.out.print(el.getAsDouble());
				doubleTotal += el.asDouble();
			} else {
				doubleTotal += el.asLong();
			}

//			System.out.println("\n");
		}
	}

	private static void parseJacksonArray(JsonNode obj) {
		Iterator<JsonNode> it = obj.iterator();
		JsonNode el;
		
		while (it.hasNext()) {
			el = it.next();
//			System.out.print(e.getKey() + "-\n");
			
			if (el.isArray()) {
				parseJacksonArray(el);
			} else if (el.isObject()) {
				parseJacksonObject(el);
			} else if (el.isTextual()) {
				//					System.out.print(el.getAsString() + "\n");
				stringValLength += el.asText().length();
//System.out.println("-" + el.asText() + "-");
			} else if (el.isBoolean()) {
				boolean b = el.asBoolean();
				//					System.out.print(b);
				if (b)
					boolT++;
				else
					boolF++;
			} else if (el.isNull()) {
				//					System.out.print("NULL");
				nils++;
			} else if (el.isDouble()) {
				//					System.out.print(el.getAsDouble());
				doubleTotal += el.asDouble();
			} else {
				doubleTotal += el.asLong();
			}

//			System.out.println("\n");
		}
	}

	
	private static void testGson(String fullFileDir, int cnt) {
		try {
			long pTime = 0L;
			long sTime = 0L;
			long wTime = 0L;
			
			for (int i=0; i<cnt; i++) {
				FileReader fileReader = new FileReader(fullFileDir);
							
				long s = System.currentTimeMillis();
				
				JsonElement jsonElement = JsonParser.parseReader(fileReader);
	
				long f = System.currentTimeMillis();
				
				String s2 = jsonElement.toString();
				
				long f2 = System.currentTimeMillis();
				
//				System.out.println(s2);
//				System.out.println("Google GSON Parse Time: " + (f-s) + "m/s");
//				System.out.println("  Stringify time: " + (f2-f) + "m/s");
//				System.console().readLine();
				pTime += (f-s);
				sTime += (f2-f);
				
				s = System.currentTimeMillis();
	
				if (jsonElement.isJsonObject()) {
					JsonObject root = jsonElement.getAsJsonObject();
					parseGsonObject(root);
				} else {
					JsonArray root = jsonElement.getAsJsonArray();
					parseGsonArray(root);
				}
				
				f = System.currentTimeMillis();
//				System.out.println("Time: " + (f-s) + "m/s");
				wTime += (f-s);
			}
			
			System.out.println("GSON parse Time: " + pTime + "m/s");
			System.out.println("GSON stringify Time: " + sTime + "m/s");
			System.out.println("GSON walk Time: " + wTime + "m/s");
			System.out.println("GSON total Time: " + (wTime+pTime+sTime) + "m/s");
			
			System.out.println("GSON stringValLength: " + stringValLength);
			System.out.println("GSON stringKeyLength: " + stringKeyLength);
			System.out.println("GSON boolT: " + boolT);
			System.out.println("GSON boolF: " + boolF);
			System.out.println("GSON longTotal: " + longTotal);
			System.out.println("GSON doubleTotal: " + doubleTotal);
			resetCounters();
//			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private static void parseGsonObject(JsonObject obj) {
		Set<Entry<String, JsonElement>> eSet = obj.entrySet();
		Iterator<Entry<String, JsonElement>> it = eSet.iterator();
		Entry<String, JsonElement> e;
		JsonElement el;
		
		while (it.hasNext()) {
			e = it.next();
//			System.out.print(e.getKey() + "-\n");
			stringKeyLength += e.getKey().length();
			el = e.getValue();
			
			if (el.isJsonArray()) {
				parseGsonArray(el.getAsJsonArray());
			} else if (el.isJsonObject()) {
				parseGsonObject(el.getAsJsonObject());
			} else if (el.isJsonPrimitive()) {
				if (el.getAsJsonPrimitive().isString()) {
//					System.out.print(el.getAsString() + "\n");
					stringValLength += el.getAsString().length();
				} else if (el.getAsJsonPrimitive().isBoolean()) {
					boolean b = el.getAsBoolean();
//					System.out.print(b);
					if (b)
						boolT++;
					else
						boolF++;
				} else if (el.getAsJsonPrimitive().isJsonNull()) {
//					System.out.print("NULL");
					nils++;
				} else {
//					System.out.print(el.getAsDouble());
					doubleTotal += el.getAsDouble();
				}
			}
//			System.out.println("\n");
		}
	}
	
	private static void parseGsonArray(JsonArray arry) {
		Iterator<JsonElement> it = arry.iterator();
		JsonElement el;
		
		while (it.hasNext()) {
			el = it.next();
			
			if (el.isJsonArray()) {
				parseGsonArray(el.getAsJsonArray());
			} else if (el.isJsonObject()) {
				parseGsonObject(el.getAsJsonObject());
			} else if (el.isJsonPrimitive()) {
				if (el.getAsJsonPrimitive().isString()) {
//					System.out.print(el.getAsString() + ", ");
					stringValLength += el.getAsString().length();
				} else if (el.getAsJsonPrimitive().isBoolean()) {
					boolean b = el.getAsBoolean();
//					System.out.print(b);
					if (b)
						boolT++;
					else
						boolF++;
				} else if (el.getAsJsonPrimitive().isJsonNull()) {
//					System.out.print("NULL");
					nils++;
				} else {
//					System.out.print(el.getAsDouble());
					doubleTotal += el.getAsDouble();
				}
			}
//			System.out.print(", ");
		}
//		System.out.print(" ]\n");
	}
	
	
	private static void testFangiongParser(String fullFileDir) {
		try {
			FileReader fileReader = new FileReader(fullFileDir);
			
			long s = System.currentTimeMillis();
			
			JSONParser parser = new JSONParser();
			JSONObject json = (JSONObject) parser.parse(fileReader);

			long f = System.currentTimeMillis();

			String s2 = json.toJSONString();
			
			long f2 = System.currentTimeMillis();
			
			System.out.println("Fangiong Parse Time: " + (f-s) + "m/s");
			System.out.println("  Stringify time: " + (f2-f) + "m/s");
			System.console().readLine();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private static void testJsonPElementAccess(String json, int cnt) {
		JsonPJson jsonPJson = null;
		
		try {
			long pTime = 0L;
			long sTime = 0L;
			long wTime = 0L;
			
			for (int i=0; i<cnt; i++) {
				long s = System.currentTimeMillis();
	
				byte[] jsonData = getBytes(json);
				JsonP_Parser parser = new JsonP_Parser(jsonData, Common.DONT_SORT_KEYS);// | Common.CONVERT_NUMERICS);
				jsonPJson = parser.parse();
				
				long f = System.currentTimeMillis();
				
				String str = new String(jsonPJson.stringify(6, false));
				
				long f2 = System.currentTimeMillis();
				
//				System.out.println("\n\n" + str + "\n");
	
//				System.out.println("Parse Time: " + (f-s) + "m/s");
//				System.out.println("Parse Stats:");
//				System.out.println("  stack: " + parser.getParseStats().stackIncreases);
//				System.out.println("  data: " + parser.getParseStats().dataIncreases);
//				System.out.println("Stringify time: " + (f2-f) + "m/s");
				
				pTime += (f-s);
				sTime += (f2-f);
				
				s = System.currentTimeMillis();
	
//				jsonPJson.addValueType(Common.string, jsonPJson.getDocRoot(), "Extended_1", "exteded 1 value");
				
				if (jsonPJson.getElementType(jsonPJson.getDocRoot()) == Common.object) {
					parseJsonPObject(jsonPJson.getDocRoot(), jsonPJson, 0);
				} else {
					parseJsonPArray(jsonPJson.getDocRoot(), jsonPJson, 0);
				}
				
				f = System.currentTimeMillis();
	
				wTime += (f-s);
				
//				int[][] temp = parser.testMap;
//				for (int t=0; t<temp.length; t++) {
//					for (int r=0; r<parser.testMap_i[t]; r++)
//						System.out.print(temp[t][r] + ",");
//					
//					System.out.println();
//				}
int highest = 0;		
int index =0;
for (int j=0; j<100_000; j++) {
	System.out.println(parser.testMap_i[j]);
		if (parser.testMap_i[j] > highest) {
			highest = parser.testMap_i[j];
			index = j;
		}
//	}
}
System.out.println("Highest: " + highest);
for (int j=0; j<highest; j++) {
	System.out.println("  " + parser.testMap[index][j]);
}

//long ls = System.currentTimeMillis();
//for (int j=0; j<highest-1; j++) {
//	if (parser.testMap[index][0] == parser.testMap[index][j%10]) {
//		System.out.println("same");
//	}
//}
//long fs = System.currentTimeMillis();
//System.out.println("Find time: " + (fs-ls));
				
//System.out.println("testMap2 key count: " + parser.testMap2.size());
//Iterator<Entry<Integer, Integer>> it = parser.testMap3.entrySet().iterator();
//while (it.hasNext()) {
//	Entry<Integer, Integer> e = it.next();
//	System.out.println(e.getKey() + " : " + e.getValue());
////	if (e.getValue() > 1) {
////		System.out.println(e.getKey() + " : " + e.getValue());
////	}
//}
//				
System.out.println("Resizes: " + parser.resizes);
//System.out.println("key count: " + parser.keyCount);				
//				System.out.println(" Time: " + (f-s) + "m/s");
//				System.out.println(keys.getNumberOfElements());
//				System.out.println(new String(jsonPJson.stringify(0, false)));
			
			}
			
			System.out.println("JsonP parse Time: " + pTime + "m/s");
			System.out.println("JsonP stringify Time: " + sTime + "m/s");
			System.out.println("JsonP walk Time: " + wTime + "m/s");
			System.out.println("JsonP total Time: " + (wTime+pTime+sTime) + "m/s");
			
			System.out.println("stringValLength: " + stringValLength);
			System.out.println("stringKeyLength: " + stringKeyLength);
			System.out.println("boolT: " + boolT);
			System.out.println("boolF: " + boolF);
			System.out.println("longTotal: " + longTotal);
			System.out.println("doubleTotal: " + doubleTotal);
			System.out.println("\n");
			resetCounters();
			
//String sPath = "/web-81app9/arr79/0/servlet174/0/init-param/redirectionClass";
String sPath = "/web-81app9/arr79/0/servlet174/0/init-param/init-param/init-param/init-param/init-param/init-param/redirectionClass";
//String sPath = "/obj1/obj_k3/obj_inner_k3/1";
//String sPath = "/key2/key3/3/key4";
int id = -1;
long s = System.currentTimeMillis();
for (int i=0; i<10000; i++)
	id = jsonPJson.getObjectId(sPath, "/");
long f = System.currentTimeMillis();
System.out.println("id: " + id);// + ", value: " + jsonPJson.getStringValue(Common.object, id) + ", time: " + (f-s) + "m/s");
System.out.println(", time: " + (f-s) + "m/s");


//			System.exit(0);
		} catch (JsonP_ParseException parseE) {
			parseE.printStackTrace();
		} catch (JsonPException jsonE) {
			jsonE.printStackTrace();
		}
	}
	
	private static void parseJsonPObject(int id, JsonPJson jsonPJson, int indent) throws JsonPException {
		KeyEntrySet keys = (KeyEntrySet) jsonPJson.getContainerElements(id);
		byte type;
//		String indt = "";
//		for (int j=0; j<indent; j++)
//			indt += " ";
		
		for (int i=0; i<keys.getNumberOfElements(); i++) {
				
			stringKeyLength += keys.getKeyName(i).length();
//			System.out.print(keys.getKeyName(i) + "-\n");
			type = keys.getElementType(i);
			
			if (type == Common.string) {
				stringValLength += keys.getAsString(i).length();
//System.out.println("-" + keys.getAsString(i) + "-" + keys.getAsString(i).length());
			} else if (type == Common.bool_false) {
				boolF++;
//				System.out.print("false\n");
			} else if (type == Common.bool_true) {
				boolT++;
//				System.out.print("true\n");
			} else if (type == Common.array) {
//				System.out.print("[ ");
				parseJsonPArray(keys.getAsArray(i), jsonPJson, indent + 4);
//				System.out.print(indt + "], ");
			} else if (type == Common.object) {
//				System.out.print("{\n");
				parseJsonPObject(keys.getAsObject(i), jsonPJson, indent + 4);
//				System.out.print(indt + "}\n");
			} else if (type == Common.numeric_double) {
				doubleTotal += keys.getAsDouble(i);
//				System.out.print(keys.getAsDouble(i) + "\n");
			} else if (type == Common.numeric_long) {
				doubleTotal += keys.getAsLong(i);
//				System.out.print(keys.getAsLong(i) + "\n");
			} else if (type == Common.nil) {
				nils++;
//				System.out.println("NULL\n");
			} 
		}
	}

	private static void parseJsonPArray(int id, JsonPJson jsonPJson, int indent) throws JsonPException {
		EntrySet keys = jsonPJson.getContainerElements(id);
		byte type;
//		String indt = "";
//		for (int j=0; j<indent; j++)
//			indt += " ";
		
		for (int i=0; i<keys.getNumberOfElements(); i++) {
				
			type = keys.getElementType(i);
			
			if (type == Common.string) {
				stringValLength += keys.getAsString(i).length();
//System.out.println("-" + keys.getAsString(i) + "-");
			} else if (type == Common.bool_false) {
				boolF++;
//				System.out.print("false, ");
			} else if (type == Common.bool_true) {
				boolT++;
//				System.out.print("true, ");
			} else if (type == Common.array) {
//				System.out.print("[ ");
				parseJsonPArray(keys.getAsArray(i), jsonPJson, indent + 4);
//				System.out.print(indt + "], ");
			} else if (type == Common.object) {
//				System.out.print("{\n");
				parseJsonPObject(keys.getAsObject(i), jsonPJson, indent + 4);
//				System.out.print(indt + "},\n");
			} else if (type == Common.numeric_double) {
				doubleTotal += keys.getAsDouble(i);
//				System.out.print(keys.getAsDouble(i) + ",");
			} else if (type == Common.numeric_long) {
				doubleTotal += keys.getAsLong(i);
//				System.out.print(keys.getAsLong(i) + ",");
			} else if (type == Common.nil) {
				nils++;
//				System.out.println("NULL,");
			} 
		}
	}
	
	
	private static void resetCounters() {
		stringValLength = 0;
		stringKeyLength = 0;
		boolT = 0;
		boolF = 0;
		longTotal = 0;
		doubleTotal = 0;
		nils = 0;
	}

}
