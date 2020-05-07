package net.erikdevelopernot.jsonP;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

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
//Google GSON stuff
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.erikdevelopernot.jsonP.document.*;
import net.erikdevelopernot.jsonP.parsers.*;





public class Driver {

	public static void main(String[] args) throws Exception {
		//uncomment to test manual json creation
		testManual();
		
		//uncomment to test Fangiong
//		testFangiongParser("/Users/user1/Downloads/large.json");
//		testFangiongParser("/Users/user1/eclipse-workspace/jsonP_java/examples/simple5.json");
				
		//uncomment to test Gson
//		testGson("/Users/user1/Downloads/large.json");
//		testGson("/Users/user1/eclipse-workspace/jsonP_java/examples/simple5.json");
		
//		byte[] jsonData = Files.readAllBytes(Paths.get("/Users/user1/eclipse-workspace/jsonP_java/examples/simple5.json"));
//		byte[] jsonData = Files.readAllBytes(Paths.get("/Users/user1/Downloads/large.json"));
//		byte[] jsonData = Files.readAllBytes(Paths.get("/Users/user1/udemy/CPP/UdemyCPP/jsonP_dyn_drvr/samples/med.json.orig"));
		
		
		long s = System.currentTimeMillis();

//		byte[] jsonData = getBytes("/Users/user1/Downloads/large.json");
		byte[] jsonData = getBytes("/Users/user1/eclipse-workspace/jsonP_java/examples/simple5.json");

		
		//uncomment to test Jackson
//		testJackson(jsonData);

		
		JsonP_Parser parser = new JsonP_Parser(jsonData, Common.PRESERVE_JSON); //, Common.CONVERT_NUMERICS);
		JsonPJson jsonPJson = parser.parse();
////jsonPJson.crap();	
//jsonData=null;
//parser=null;
//System.gc();

		long f = System.currentTimeMillis();
		
		byte[] j = jsonPJson.stringify(0, true);
		
		long f2 = System.currentTimeMillis();
		System.out.println("\n\n" + new String(j) + "\n");

		System.out.println("Parse Time: " + (f-s) + "m/s");
		System.out.println("Parse Stats:");
		System.out.println("  stack: " + parser.getParseStats().stackIncreases);
		System.out.println("  data: " + parser.getParseStats().dataIncreases);
		System.out.println("Stringify time: " + (f2-f) + "m/s");
System.gc();
// start remove
int id = jsonPJson.getObjectId("/obj_ptr_will_break/key__2", "/");
System.out.println("id: " + id);
// end remove
		System.console().readLine();
	}
	
	
	private static void testManual() {
		try {
			JsonPJson json = new JsonPJson(Common.object, 20, 1024, Common.CONVERT_NUMERICS);
		
			json.add_value_type(Common.string, 0, "key1", "value1");
			json.add_value_type(Common.string, 0, "akey2", "value2");
			int key3ID = json.add_value_type(Common.string, 0, "Key3", "value3");
			json.add_value_type(Common.string, 0, "akey4", "value4");
			json.add_value_type(Common.string, 0, "AKey5", "value5");
			json.add_value_type(Common.bool_true, 0, "Bool_T", null);
			json.add_value_type(Common.bool_false, 0, "Bool_f", null);
			json.add_value_type(Common.numeric_long, 0, "nLong", new Long(987654321));
			json.add_value_type(Common.numeric_double, 0, "nDouble", new Double(12345.9876));
			json.add_value_type(Common.nil, 0, "nil", null);
			
			int objContainer = json.addContainer("Container_1", 3, 0, Common.object);
			System.out.println("objContainer id: " + objContainer);
			json.add_value_type(Common.string, objContainer, "abc", "value abc");
			json.add_value_type(Common.string, objContainer, "Zee", "value Zee");
			json.add_value_type(Common.string, objContainer, "ABC", "value ABC");
			json.add_value_type(Common.bool_true, objContainer, "Bool_T", null);
			json.add_value_type(Common.bool_false, objContainer, "Bool_f", null);
			json.add_value_type(Common.numeric_long, objContainer, "Long", new Long(987654321));
			
			int arryContainer = json.addContainer("Array", 5, objContainer, Common.array);
//			int arryContainer = json.addContainer("/Container_1/", "/", "Array", 5, Common.array);
			json.add_value_type(Common.numeric_long, arryContainer, null, new Long(1));
			json.add_value_type(Common.numeric_long, arryContainer, null, new Long(2));
			json.add_value_type(Common.numeric_long, arryContainer, null, new Long(3));
			json.add_value_type(Common.numeric_long, arryContainer, null, new Long(4));
			json.add_value_type(Common.numeric_long, arryContainer, null, new Long(5));
			
			int embedObj = json.addContainer("/Container_1/Array", "/", null, 1, Common.object);
//			int embedObj = json.addContainer("www", 1, arryContainer, Common.object);
			json.add_value_type(Common.string, embedObj, "embed_key", "value");
			int embedAry = json.addContainer("Container_1/Array", "/", null, 1, Common.array);
			json.add_value_type(Common.string, embedAry, null, "value");
			
			json.add_value_type(Common.string, objContainer, "After_Array", "After");
			
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
	
	
	private static void testJackson(byte[] jsonData) {
		try {
			long s = System.currentTimeMillis();
			
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(jsonData);

			long f = System.currentTimeMillis();
			
			byte[] s2 = objectMapper.writeValueAsBytes(rootNode);
			
			long f2 = System.currentTimeMillis();
			
			System.out.println("Jackson Parse Time: " + (f-s) + "m/s");
			System.out.println("  Stringify time: " + (f2-f) + "m/s");
			System.console().readLine();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void testGson(String fullFileDir) {
		try {
			FileReader fileReader = new FileReader(fullFileDir);
						
			long s = System.currentTimeMillis();
			
			JsonElement jsonElement = JsonParser.parseReader(fileReader);

			long f = System.currentTimeMillis();
			
			String s2 = jsonElement.toString();
			
			long f2 = System.currentTimeMillis();
			
			System.out.println("Google GSON Parse Time: " + (f-s) + "m/s");
			System.out.println("  Stringify time: " + (f2-f) + "m/s");
			System.console().readLine();
		
		} catch (Exception e) {
			e.printStackTrace();
		}
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

}
