package net.erikdevelopernot.jsonP;

import java.io.FileReader;
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
		//uncomment to test Fangiong
//		testFangiongParser("/Users/user1/Downloads/large.json");
				
		//uncomment to test Gson
//		testGson("/Users/user1/Downloads/large.json");
		
		byte[] jsonData = Files.readAllBytes(Paths.get("/Users/user1/eclipse-workspace/jsonP_java/examples/simple3.json"));
//		byte[] jsonData = Files.readAllBytes(Paths.get("/Users/user1/Downloads/large.json"));
		
		//uncomment to test Jackson
//		testJackson(jsonData);
		
		long s = System.currentTimeMillis();
		
		JsonP_Parser parser = new JsonP_Parser(jsonData, Common.CONVERT_NUMERICS);
		JsonPJson jsonPJson = parser.parse();
		
		long f = System.currentTimeMillis();
//		byte[] j = jsonPJson.stringify(0);
//		System.out.println("\n\n" + new String(j) + "\n");

		System.out.println("Parse Time: " + (f-s) + "m/s");
		System.out.println("Parse Stats:");
		System.out.println("  stack: " + parser.getParseStats().stackIncreases);
		System.out.println("  data: " + parser.getParseStats().dataIncreases);
		System.console().readLine();
	}
	
	
	private static void testJackson(byte[] jsonData) {
		try {
			long s = System.currentTimeMillis();
			
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(jsonData);
			
			long f = System.currentTimeMillis();
			
			System.out.println("Jackson Parse Time: " + (f-s) + "m/s");
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
			
			System.out.println("Google GSON Parse Time: " + (f-s) + "m/s");
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
			
			System.out.println("Fangiong Parse Time: " + (f-s) + "m/s");
			System.console().readLine();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
