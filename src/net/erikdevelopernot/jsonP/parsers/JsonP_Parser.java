package net.erikdevelopernot.jsonP.parsers;

import java.util.HashMap;
import java.util.Map;

import net.erikdevelopernot.jsonP.Common;
import net.erikdevelopernot.jsonP.JsonP_ParseException;
import net.erikdevelopernot.jsonP.document.JsonPJson;

public class JsonP_Parser {
	//parse options
	private boolean preserveJson = false;
	private boolean shrinkBuffers = false;
	private boolean sortKeys = true;
	private boolean weakRef = false;
	private boolean convertNumerics = false;
	private boolean indexObjects = true;
	private int options;
	
	byte[] json;
	int jsonIdx;
	int jsonLen;
	boolean lookForKey;
	int valueStart;
	
	//stack (meta) variables
	private byte[] stackBuf;
	private int stackBufSz;
	private int stackBufIdx;
	
	//data (meta-data) variables
	private byte[] data;
	private int dataSz;
	private int dataIdx;
	
	//used for parseText escaped
	private boolean hasEscapes;
	private int[] escapes;
	private int escapesIdx;
	
	//parse stats
	private ParseStats parseStats;
	
	//support indexing objects
	private int[][] objectIndexHashes;
	private int objectIndexHashesLen;
	private int objectIndexHashes_i[];
	private byte[] path;
	private int pathIndex;

	//support hashing keys
	private int hash;
	private int hashKey;
	private byte[] currentKey;
	private int currentKeyLength;

	/*
	 * Constructors
	 */
	public JsonP_Parser(byte[] json) {
		this(json, 0);		//use default options
	}
	
	public JsonP_Parser(byte[] json, int options) {
		setOptions(options);
		
		if (convertNumerics) {
//			covtNumBuf = new byte[12];
			
			//converting numerics automatically means preserve json
			preserveJson = true;
			this.options |= Common.PRESERVE_JSON;
		}
		
		if (preserveJson) {
			// preserve json data and stack are separate structures
			dataSz = (int)(json.length / 2);
			
			hasEscapes = false;
			escapes = new int[20];							//potential out of bounds Revisit
			escapesIdx = 0;
		} else {
			// data and stack share structure
			dataSz = json.length / 4;
		}
	
		data = new byte[dataSz];
		stackBufSz = 1000;
		stackBuf = new byte[stackBufSz];
		dataIdx = 0;
		stackBufIdx = 0;
	
		this.json = json;
		jsonLen = json.length;
		jsonIdx = 0;
		parseStats = new ParseStats();
		
		//TODO - should be a better way
		if (json.length < 100000) {
			objectIndexHashes = new int[64][10];
			objectIndexHashes_i = new int[64];
			objectIndexHashesLen = 64;
		} else if (json.length < 5000000) {
			objectIndexHashes = new int[2048][10];
			objectIndexHashes_i = new int[2048];
			objectIndexHashesLen = 2048;
		} else {
			objectIndexHashes = new int[100_000][10];
			objectIndexHashes_i = new int[100_000];
			objectIndexHashesLen = 100_000;
		}

		path = new byte[1024];
		pathIndex = 0;
		currentKey = new byte[1024];
	}
	
	public JsonP_Parser(String json, int options) {
		this(json.getBytes(), options);
	}
	
	public JsonP_Parser(String json) {
		this(json, 0);		//use default options
	}
	
	
	/*
	 * Methods
	 */
	public JsonPJson parse() throws JsonP_ParseException {
		JsonPJson jsonPJson = null;
		
		if (jsonLen < 2) 
			throw new JsonP_ParseException("Invalid Json file, too short");
		
		eatWhiteSpace();

//		stackBufIdx++;
		stackBufIdx += 5;
		stackBuf[stackBufIdx++] = (byte)(0xff & (0 >>> 24));
		stackBuf[stackBufIdx++] = (byte)(0xff & (0 >>> 16));
		stackBuf[stackBufIdx++] = (byte)(0xff & (0 >>> 8));
		stackBuf[stackBufIdx++] = (byte)(0xff & 0);

		data[dataIdx+4] = '\0';
		dataIdx = 5;

		currentKey[0] = '/';
		currentKeyLength = 1;

		int dRoot = 0; // = parseObject();

		if (json[jsonIdx] == '{') {
			stackBuf[stackBufIdx++] = Common.object;
			dRoot = parseObject(Common.object);
		} else if (json[jsonIdx] == '[') {
			stackBuf[stackBufIdx++] = Common.array;
			dRoot = parseArray(Common.array);
		} else {
			throw new JsonP_ParseException("Error parsing json text, does not appear to be an object or an array");
		}

		int length = jsonLen;

		if (preserveJson)
			length = dataIdx;

		if (shrinkBuffers) {
			byte[] temp = data;
			data = new byte[dataIdx];

			for (int i=0; i<dataIdx; i++)
				data[i] = temp[i];
		}

		if (!preserveJson)
//			jsonPJson = new JsonPJson(json, data, length, dataIdx, dRoot, options);
			jsonPJson = new JsonPJson(json, data, length, dataIdx, objectIndexHashes, objectIndexHashes_i, objectIndexHashesLen, dRoot, options);
		else
//			jsonPJson = new JsonPJson(data, length, dRoot, options);
			jsonPJson = new JsonPJson(data, length, objectIndexHashes, objectIndexHashes_i, objectIndexHashesLen, dRoot, options);


		return jsonPJson;
	}
	
	
	
	/*
	 * Parse key or 'text' values
	 */
	void parseText() throws JsonP_ParseException {
		jsonIdx++;
		int start = jsonIdx;	

		while (true) {
			if (json[jsonIdx] == '\\') {
				//					switch (json[index+1])
				switch (json[jsonIdx+1])
				{
					case '\\' :
					case '"' :
					case '/':
					{
// prior do nothing escape
//						jsonIdx += 2;
//						break;
	
// new escape testing
						if (!preserveJson) {
							int temp = jsonIdx;
							
							while (temp > start) {
								json[temp] = json[temp-1];
								temp--;
							}
							 start++;
						} else {
							hasEscapes = true;
							escapes[escapesIdx++] = jsonIdx;
						}
						
						jsonIdx += 2;
						break;
						
					}
					case 'b' :
					case 'f' :
					case 'r' :
					case 'n' :
					case 't' :
					case 'u' :			//treat the same as control chars for now
					{
						jsonIdx++;
						break;
					}
					default :
					{
						throw new JsonP_ParseException("parse Error, invalid escape found at index: " + jsonIdx);
					}
				}
			}	else if (json[jsonIdx] == '"') {
				if (!preserveJson)
					json[jsonIdx] = '\0';

				break;
			} else {
				
				jsonIdx++;
			}
		}

		jsonIdx++;
		
		//set current key and path
		if (lookForKey) {
			currentKeyLength = jsonIdx - start -1;

			for (int i = 0; i < currentKeyLength; i++) {
				currentKey[i] = json[start + i];
				path[pathIndex++] = json[start + i];
		    }
		}

		if (!lookForKey) {
			if (!preserveJson) {
				while (start < jsonIdx) {
					json[valueStart++] = json[start++];
				}
			} else {
				if ((jsonIdx - start + 5 + dataIdx) > dataSz) {
					increaseDataBuffer((int) (dataSz * 1.2 + jsonIdx - start));
				}
				
				if (hasEscapes) {
					escapes[escapesIdx++] = jsonIdx;
					
					for (int i=0; i<escapesIdx; i++) {
						while (start < escapes[i]) 
							data[dataIdx++] = json[start++];
						
						start++;
					}
					
					data[dataIdx-1] = '\0';
					hasEscapes = false;
					escapesIdx = 0;
				} else {
					while (start < jsonIdx)
						data[dataIdx++] = json[start++];
	
					data[dataIdx-1] = '\0';
				}
			}
		} else if (preserveJson) {
			if ((jsonIdx - start + 5 + dataIdx) > dataSz) {
				increaseDataBuffer((int) (dataSz * 1.2 + jsonIdx - start));
			}

			if (hasEscapes) {
				escapes[escapesIdx++] = jsonIdx;
				
				for (int i=0; i<escapesIdx; i++) {
					while (start < escapes[i]) 
						data[dataIdx++] = json[start++];
					
					start++;
				}
				
				data[dataIdx-1] = '\0';
				hasEscapes = false;
				escapesIdx = 0;
			} else {
				while (start < jsonIdx)
					data[dataIdx++] = json[start++];
	
				data[dataIdx-1] = '\0';
			}
		}
	}
	
	
	/*
	 * Parse numeric, no conversion
	 */
	byte parseNumeric() throws JsonP_ParseException {
		boolean isLong = true;
		byte c = json[jsonIdx];
		boolean sign = true;
		boolean exp = false;
		boolean expSign = false;
		boolean dec = true;

		int s = jsonIdx-1;

//		while (c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != ',' && c != ']' && c != '}') {
		while ((Common.parse_flags[c] & 0x40) != 0x40) {
			if (c >= '0' && c <= '9') {
//				zer0 = true;
				expSign = false;
			/*} else if (c == '0' && zer0) {
				exp_sign = false;*/
			} else if ((c == 'e' || c == 'E') && !exp) {
				exp = true;
				expSign = true;
				dec = false;
				isLong = false;
			} else if (c == '.' && dec) {
				exp = false;
				isLong = false;
			} else if ((c == '-' || c == '+') && (sign || expSign)) {
				expSign = false;
			} else {
				throw new JsonP_ParseException("parse error, trying to parse numeric value at index: " + jsonIdx);
			}
			
			sign = false;
			c = json[++jsonIdx];
		}

		if (!preserveJson) {
			while (s < jsonIdx-1) {
				json[valueStart++] = json[s+1];
				s++;
			}
			
			json[valueStart] = '\0';
		} else {
			if (jsonIdx+1 - s >= dataSz - dataIdx) {
				dataSz = (int)(dataSz * 1.2 + jsonIdx - s);
				byte temp[] = data;
				data = new byte[dataSz];
				
				for (int i=0; i<dataIdx; i++)
					data[i] = temp[i];
				
				parseStats.dataIncreases++;
			}
				
			while (s < jsonIdx-1) {
				data[dataIdx++] = json[s+1];
				s++;
			}

			data[dataIdx++] = '\0';
		}

		if (isLong)
			return Common.numeric_long;
		else
			return Common.numeric_double;

	}

	
	/*
	 * Parse Numeric and convert to proper numeric type
	 */
	byte parseNumericCVT() throws JsonP_ParseException {
		boolean isLong = true;
		byte c = json[jsonIdx];
		boolean sign = true;
		boolean exp = false;
		boolean expSign = false;
		boolean dec = true;
		double d = 0;
		double multiplier = 10;
		long mult = 1;
		long expMult = 1;
		int e = 0;

//		while (c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != ',' && c != ']' && c != '}') {
		while ((Common.parse_flags[c] & 0x40) != 0x40) {
	
			if (c >= '0' && c <= '9') {
				expSign = false;
				
				if (multiplier == 10) {
					d = d * multiplier + (c-48);
				} else if (multiplier > 0){
					d += multiplier * (c-48);
					multiplier *= .1;
				} else {
					e = e * 10 + (c-48);
				}
			} else if ((c == 'e' || c == 'E') && !exp) {
				exp = true;
				expSign = true;
				dec = false;
				isLong = false;
				multiplier = -1;
			} else if (c == '.' && dec) {
				exp = false;
				isLong = false;
				multiplier = .1;
			} else if ((c == '-' || c == '+') && (sign || expSign)) {
				if (c == '-') {
					if (sign) 
						mult = -1;
					else 
						expMult = -1;
				} 

				expSign = false;
			} else {
				throw new JsonP_ParseException("parse error, trying to parse numeric cvt value at index: " + jsonIdx);
			}
			
			sign = false;
			c = json[++jsonIdx];
		}
			
		if (e > 0) {
			for (int i=0; i < e; i++, d *= ((expMult == 1) ? 10 : .1))
				;
		}
		
		if (Common.LONG_DOUBLE_SZ + 2 >= dataSz - dataIdx) {
			dataSz = (int)(dataSz * 1.2 + Common.LONG_DOUBLE_SZ);
			byte temp[] = data;
			data = new byte[dataSz];
			
			for (int i=0; i<dataIdx; i++)
				data[i] = temp[i];
			
			parseStats.dataIncreases++;
		}
		
		if (isLong) {
			if (d <= Long.MAX_VALUE) { 
				long num = (long)(d * mult);
				data[dataIdx++] = (byte)(0xFF & (num >>> 56));
				data[dataIdx++] = (byte)(0xFF & (num >>> 48));
				data[dataIdx++] = (byte)(0xFF & (num >>> 40));
				data[dataIdx++] = (byte)(0xFF & (num >>> 32));
				data[dataIdx++] = (byte)(0xFF & (num >>> 24));
				data[dataIdx++] = (byte)(0xFF & (num >>> 16));
				data[dataIdx++] = (byte)(0xFF & (num >>> 8));
				data[dataIdx++] = (byte)(0xFF & num);
				return Common.numeric_long;
			} else {
				long num = Double.doubleToRawLongBits(d * mult);
				
				data[dataIdx++] = (byte)(0xFF & ((long)num >>> 56));
				data[dataIdx++] = (byte)(0xFF & ((long)num >>> 48));
				data[dataIdx++] = (byte)(0xFF & ((long)num >>> 40));
				data[dataIdx++] = (byte)(0xFF & ((long)num >>> 32));
				data[dataIdx++] = (byte)(0xFF & ((long)num >>> 24));
				data[dataIdx++] = (byte)(0xFF & ((long)num >>> 16));
				data[dataIdx++] = (byte)(0xFF & ((long)num >>> 8));
				data[dataIdx++] = (byte)(0xFF & (long)num);
				return Common.numeric_double;
			}
		} else {
			long num = Double.doubleToRawLongBits(d * mult);
			
			data[dataIdx++] = (byte)(0xFF & ((long)num >>> 56));
			data[dataIdx++] = (byte)(0xFF & ((long)num >>> 48));
			data[dataIdx++] = (byte)(0xFF & ((long)num >>> 40));
			data[dataIdx++] = (byte)(0xFF & ((long)num >>> 32));
			data[dataIdx++] = (byte)(0xFF & ((long)num >>> 24));
			data[dataIdx++] = (byte)(0xFF & ((long)num >>> 16));
			data[dataIdx++] = (byte)(0xFF & ((long)num >>> 8));
			data[dataIdx++] = (byte)(0xFF & (long)num);
			return Common.numeric_double;
		}
		
	}

	
	
	/*
	 * called by parseObject and parseArray to parse a value type
	 */
	private void parseValue(byte parentType) throws JsonP_ParseException {
		if (json[jsonIdx] == '{') {
			//object
			stackBuf[stackBufIdx] = Common.object_ptr;

			if (parentType == Common.object)
				stackBufIdx += Common.object_member_sz;
			else
				stackBufIdx += Common.array_member_sz;

			parseObject(parentType);
		} else if (json[jsonIdx] == '[') {
			//array
			stackBuf[stackBufIdx] = Common.array_ptr;
			
			if (parentType == Common.object)
				stackBufIdx += Common.object_member_sz;
			else
				stackBufIdx += Common.array_member_sz;
			
			parseArray(parentType);
		} else if (json[jsonIdx] == '"') {
			//string
			stackBuf[stackBufIdx] = Common.string;
			parseText();
		} else if (json[jsonIdx] == 't') {
			if (json[++jsonIdx] == 'r' && json[++jsonIdx] == 'u' && json[++jsonIdx] == 'e') {
				stackBuf[stackBufIdx] = Common.bool_true;
				jsonIdx++;
			} else {
				throw new JsonP_ParseException("Invalid bool value found at index: " + jsonIdx);
			}
		} else if (json[jsonIdx] == 'f') {
			if (json[++jsonIdx] == 'a' && json[++jsonIdx] == 'l' && json[++jsonIdx] == 's' && json[++jsonIdx] == 'e') {
				stackBuf[stackBufIdx] = Common.bool_false;
				jsonIdx++;
			} else {
				throw new JsonP_ParseException("Invalid bool value found at index: " + jsonIdx);
			}
		} else if ((json[jsonIdx] >= '0' && json[jsonIdx] <= '9') || json[jsonIdx] == '-' || json[jsonIdx] == '+') {

			switch ((convertNumerics) ? parseNumericCVT() : parseNumeric())
			{
				case Common.numeric_int :
				{
					stackBuf[stackBufIdx] = Common.numeric_int;
					break;
				}
				case Common.numeric_long :
				{
					stackBuf[stackBufIdx] = Common.numeric_long;
					break;
				}
				case Common.numeric_double :
				{
					stackBuf[stackBufIdx] = Common.numeric_double;
					break;
				}
				default :
					throw new JsonP_ParseException("parse error, invalid return type from parse_numeric at index: " + jsonIdx);
			}

		} else if (json[jsonIdx] == 'n') { 
			if (json[++jsonIdx] == 'u' && json[++jsonIdx] == 'l' && json[++jsonIdx] == 'l') {
				stackBuf[stackBufIdx] = Common.nil;
				jsonIdx++;
			} else {
				throw new JsonP_ParseException("parse error, trying to get null value at index: " + jsonIdx);
			}
		} else {
			throw new JsonP_ParseException("parse error, trying to get value at index: " + jsonIdx);
		}
	}

	
	/*
	 * Parse a json object
	 */
	private int parseObject(byte parentType) throws JsonP_ParseException {
		
		// create local index for this obj record and advace global index
		int locStackIndx = stackBufIdx;
		int numKeys = 0;
		int toReturn = -1;
		int parentElementSize = (parentType == Common.object) ? Common.object_member_sz : Common.array_member_sz;


		int localCurrentKeyLength = currentKeyLength;
		int localTempKeyLength;
		path[pathIndex++] = '/';
		
		stackBuf[locStackIndx] = Common.object;
		
		stackBuf[locStackIndx+1] = stackBuf[locStackIndx - parentElementSize + Common.object_member_key_offx];
		stackBuf[locStackIndx+2] = stackBuf[locStackIndx - parentElementSize + Common.object_member_key_offx + 1];
		stackBuf[locStackIndx+3] = stackBuf[locStackIndx - parentElementSize + Common.object_member_key_offx + 2];
		stackBuf[locStackIndx+4] = stackBuf[locStackIndx - parentElementSize + Common.object_member_key_offx + 3];

		hash=0;
		for (int i=0; i<currentKeyLength; i++) {
			hash = 31 * hash + currentKey[i];
		}

		stackBuf[locStackIndx + Common.object_member_hash_offx] = (byte)((hash >>> 24) & 0xFF);
		stackBuf[locStackIndx + Common.object_member_hash_offx + 1] = (byte)((hash >>> 16) & 0xFF);
		stackBuf[locStackIndx + Common.object_member_hash_offx + 2] = (byte)((hash >>> 8) & 0xFF);
		stackBuf[locStackIndx + Common.object_member_hash_offx + 3] = (byte)(hash & 0xFF);

		stackBufIdx += Common.root_sz + Common.object_member_sz;
	
		boolean keepGoing = true;
		boolean localLookForKey = true;
		
		if (json[jsonIdx] != '{') {
			throw new JsonP_ParseException("parse error, expected '{' at index: " + jsonIdx);
		}
		
		++jsonIdx;
		
		try {
			while (keepGoing) {
				if (stackBufSz <= stackBufIdx + 50) {
					increaseStackBuffer((int)(stackBufSz * 1.2 + 512));
				}
		
				eatWhiteSpace();
	
				//check for end object
				if (json[jsonIdx] == '}') {
					jsonIdx++;
					stackBuf[locStackIndx + Common.object_member_sz] = (byte)(0xff & (numKeys >>> 24));
					stackBuf[locStackIndx + Common.object_member_sz + 1] = (byte)(0xff & (numKeys >>> 16));
					stackBuf[locStackIndx + Common.object_member_sz + 2] = (byte)(0xff & (numKeys >>> 8));
					stackBuf[locStackIndx + Common.object_member_sz + 3] = (byte)(0xff & numKeys);
					
					if (sortKeys) {
//						Common.sortKeys(locStackIndx+Common.object_member_sz+Common.root_sz, 
//								locStackIndx+Common.object_member_sz+Common.root_sz+((numKeys-1)*Common.object_member_sz), 
//								(preserveJson) ? data : json, data, stackBuf);
						if (numKeys > Common.SORT_THRESH_HOLD) {
							Common.sortKeysByHash(locStackIndx+Common.object_member_sz+Common.root_sz, 
									locStackIndx+Common.object_member_sz+Common.root_sz+((numKeys-1)*Common.object_member_sz), stackBuf);
						}
					}
		
					stackBuf[locStackIndx + Common.object_member_sz + Common.root_sz + (Common.object_member_sz * numKeys)] = Common.extended;
	
					int ls_i = locStackIndx + Common.object_member_sz + Common.root_sz + (Common.object_member_sz * numKeys) + Common.object_member_key_offx;
					stackBuf[ls_i] = 0;
					stackBuf[ls_i + 1] = 0;
					stackBuf[ls_i + 2] = 0;
					stackBuf[ls_i + 3] = 0;
					
					stackBufIdx += Common.object_member_sz;
	
					if ((dataIdx + stackBufIdx - locStackIndx + Common.object_member_sz) >= dataSz) {
//						increaseDataBuffer((int) (dataSz * 1.2 + stackBufIdx - locStackIndx));
						increaseDataBuffer((int)(stackBufIdx - locStackIndx + Common.object_member_sz + dataIdx + dataSz * 1.2));
					}
	
					for (int i=locStackIndx, data_i=dataIdx; i < stackBufIdx; i++, data_i++) {
						data[data_i] = stackBuf[i];
					}

					
					stackBuf[locStackIndx - parentElementSize + Common.object_member_key_offx] = 
							(byte)(0xff & ((dataIdx + Common.object_member_key_offx) >>> 24));
					stackBuf[locStackIndx - parentElementSize + Common.object_member_key_offx + 1] = 
							(byte)(0xff & ((dataIdx + Common.object_member_key_offx) >>> 16));
					stackBuf[locStackIndx - parentElementSize + Common.object_member_key_offx + 2] = 
							(byte)(0xff & ((dataIdx + Common.object_member_key_offx) >>> 8));
					stackBuf[locStackIndx - parentElementSize + Common.object_member_key_offx + 3] = 
							(byte)(0xff & (dataIdx + Common.object_member_key_offx));
					
					stackBuf[locStackIndx - parentElementSize] = Common.object_ptr;

					//set hash in PTR element
					if (parentType == Common.object) {
					stackBuf[locStackIndx - Common.object_member_sz + Common.object_member_hash_offx] = 
						stackBuf[locStackIndx + Common.object_member_hash_offx];
					stackBuf[locStackIndx - Common.object_member_sz + Common.object_member_hash_offx + 1] = 
						stackBuf[locStackIndx + Common.object_member_hash_offx + 1];
					stackBuf[locStackIndx - Common.object_member_sz + Common.object_member_hash_offx + 2] = 
						stackBuf[locStackIndx + Common.object_member_hash_offx + 2];
					stackBuf[locStackIndx - Common.object_member_sz + Common.object_member_hash_offx + 3] = 
						stackBuf[locStackIndx + Common.object_member_hash_offx + 3];
					}
					
					pathIndex -= 1;
					
					// INDEX Objects
					if (indexObjects) {
						if (numKeys > 0) {
							hash=0;
							//System.out.print("Adding path: ");
							for (int i=0; i<pathIndex; i++) {
								hash = 31 * hash + path[i];
							//	System.out.print((char)path[i]);	
						}
							//System.out.print(" - " + hash + "\n");
							hashKey = (hash < 0) ? ((hash * -1) % objectIndexHashesLen) : (hash % objectIndexHashesLen);
												
							objectIndexHashes[hashKey][objectIndexHashes_i[hashKey]++] = hash;
							objectIndexHashes[hashKey][objectIndexHashes_i[hashKey]++] = dataIdx; // + Common.member_keyvalue_offx;
							//testMapMeta[hash][testMap_i[hash]++] = Common.object_ptr;
							//					
							if (objectIndexHashes_i[hashKey] == objectIndexHashes[hashKey].length) {
								increaseMapBucket(hashKey);
							}
						}
					}
					
					currentKeyLength = localCurrentKeyLength;

					toReturn = dataIdx;
					dataIdx += (stackBufIdx - locStackIndx);
					stackBufIdx = locStackIndx - parentElementSize;
						
					break;
				} else if (json[jsonIdx] == '"') {
					//check for key
					lookForKey = true;
					
					if (!preserveJson) {
						stackBuf[stackBufIdx + Common.object_member_key_offx] = (byte)(0xff & ((jsonIdx + 1) >>> 24));
						stackBuf[stackBufIdx + Common.object_member_key_offx + 1] = (byte)(0xff & ((jsonIdx + 1) >>> 16));
						stackBuf[stackBufIdx + Common.object_member_key_offx + 2] = (byte)(0xff & ((jsonIdx + 1) >>> 8));
						stackBuf[stackBufIdx + Common.object_member_key_offx + 3] = (byte)(0xff & (jsonIdx + 1));
					} else {
						stackBuf[stackBufIdx + Common.object_member_key_offx] = (byte)(0xff & (dataIdx >>> 24));
						stackBuf[stackBufIdx + Common.object_member_key_offx + 1] = (byte)(0xff & (dataIdx >>> 16));
						stackBuf[stackBufIdx + Common.object_member_key_offx + 2] = (byte)(0xff & (dataIdx >>> 8));
						stackBuf[stackBufIdx + Common.object_member_key_offx + 3] = (byte)(0xff & dataIdx);
					}
						
					numKeys ++;
	
					parseText();

					localTempKeyLength = currentKeyLength;	

					//set hash for the key just parsed
					hash=0;
					for (int i=0; i<currentKeyLength; i++) {
						hash = 31 * hash + currentKey[i];
					}
					
					stackBuf[stackBufIdx + Common.object_member_hash_offx] = (byte)((hash >>> 24) & 0xFF);
					stackBuf[stackBufIdx + Common.object_member_hash_offx + 1] = (byte)((hash >>> 16) & 0xFF);
					stackBuf[stackBufIdx + Common.object_member_hash_offx + 2] = (byte)((hash >>> 8) & 0xFF);
					stackBuf[stackBufIdx + Common.object_member_hash_offx + 3] = (byte)(hash & 0xFF);
			
					lookForKey = false;
					localLookForKey = false;
					valueStart = jsonIdx;
	
	/************* TO DO, figure out a check for below *********************/				
	//				if (key.length() < 1) {
	//					std::string err = "parse error, blank key found at index: " + std::to_string(index);
	//					set_error(err);
	//					throw jsonP_exception{err.c_str()};
	//				}
					
					eatWhiteSpace();
	
					if (json[jsonIdx] != ':') {
						throw new JsonP_ParseException("parse error, expected ':' at index: " + jsonIdx);
					}
						
					jsonIdx++;
					eatWhiteSpace();
					parseValue(Common.object);
					stackBufIdx += Common.object_member_sz;
	
					pathIndex -= localTempKeyLength;
					
					continue;
				} else if (json[jsonIdx] == ',') {
					//check for comma
					if (localLookForKey) {
						throw new JsonP_ParseException("parse error, found ',' while looking for a key at index: " + jsonIdx);
					}
					
					localLookForKey = true;
					jsonIdx++;
				} else {
					throw new JsonP_ParseException("parse error, in parse_object, found: " + (char)json[jsonIdx] + ", at: " + jsonIdx);
				}
				
			}
		} catch (JsonP_ParseException e) {
			System.out.println(e.getMessage());
			throw e;
		}
		
		return toReturn;
	}
	
	
	
	/*
	 * parse a json array
	 */
	private int parseArray(byte parentType) throws JsonP_ParseException {
		// create local index for this array record and advace global index
		int locStackIndx = stackBufIdx;
		int numElements = 0;
		int toReturn;
		int parentElementSize = (parentType == Common.object) ? Common.object_member_sz : Common.array_member_sz;

		int localCurrentKeyLength = currentKeyLength;
		path[pathIndex++] = '/';
		int indexMult = 10;
		int indexNumBytes;

		stackBuf[locStackIndx] = Common.array;
		
		stackBuf[locStackIndx + Common.array_member_value_offx] = 
				stackBuf[locStackIndx - parentElementSize + Common.array_member_value_offx];
		stackBuf[locStackIndx + Common.array_member_value_offx + 1] = 
				stackBuf[locStackIndx - parentElementSize + Common.array_member_value_offx + 1];
		stackBuf[locStackIndx + Common.array_member_value_offx + 2] = 
				stackBuf[locStackIndx - parentElementSize+ Common.array_member_value_offx + 2];
		stackBuf[locStackIndx + Common.array_member_value_offx + 3] = 
				stackBuf[locStackIndx - parentElementSize + Common.array_member_value_offx + 3];
		
		stackBufIdx += (Common.array_member_sz + Common.root_sz);
		valueStart = jsonIdx;
		
		int d_i = ((preserveJson) ? dataIdx : jsonIdx);
		stackBuf[stackBufIdx + Common.array_member_value_offx] = (byte)(0xff & (d_i >> 24));
		stackBuf[stackBufIdx + Common.array_member_value_offx + 1] = (byte)(0xff & (d_i >> 16));
		stackBuf[stackBufIdx + Common.array_member_value_offx + 2] = (byte)(0xff & (d_i >> 8));
		stackBuf[stackBufIdx + Common.array_member_value_offx + 3] = (byte)(0xff & d_i);
			
		jsonIdx++;
		eatWhiteSpace();
		
		//make sure array isn't empty
		if (json[jsonIdx] == ']') {
			stackBuf[locStackIndx + Common.array_member_sz] = (byte)(0xff & (numElements >> 24));
			stackBuf[locStackIndx + Common.array_member_sz + 1] = (byte)(0xff & (numElements >> 16));
			stackBuf[locStackIndx + Common.array_member_sz + 2] = (byte)(0xff & (numElements >> 8));
			stackBuf[locStackIndx + Common.array_member_sz + 3] = (byte)(0xff & numElements);
			
			stackBuf[stackBufIdx] = Common.extended;
			
			stackBuf[stackBufIdx] = 0;
			stackBuf[stackBufIdx + 1] = 0;
			stackBuf[stackBufIdx + 2] = 0;
			stackBuf[stackBufIdx + 3] = 0;
			
			if ((stackBufIdx - locStackIndx + Common.array_member_sz + dataIdx) >= dataSz) {			
//				increaseDataBuffer(stackBufIdx - locStackIndx + Common.member_sz + dataIdx);
				increaseDataBuffer((int)(stackBufIdx - locStackIndx + Common.array_member_sz + dataIdx + dataSz * 1.2));
			}

			for (int i=locStackIndx, data_i=dataIdx; i < stackBufIdx; i++, data_i++) {
				data[data_i] = stackBuf[i];
			}

			stackBuf[locStackIndx - parentElementSize + Common.array_member_value_offx] = 
					(byte)(0xff & ((dataIdx + Common.array_member_value_offx) >> 24));
			stackBuf[locStackIndx - parentElementSize + Common.array_member_value_offx + 1] = 
					(byte)(0xff & ((dataIdx + Common.array_member_value_offx) >> 16));
			stackBuf[locStackIndx - parentElementSize + Common.array_member_value_offx + 2] = 
					(byte)(0xff & ((dataIdx + Common.array_member_value_offx) >> 8));
			stackBuf[locStackIndx - parentElementSize + Common.array_member_value_offx + 3] = 
					(byte)(0xff & (dataIdx + Common.array_member_value_offx));
			
			stackBuf[locStackIndx - parentElementSize] = Common.array_ptr;


			pathIndex -= 1;
			currentKeyLength = localCurrentKeyLength;

			toReturn = dataIdx;
			dataIdx += (stackBufIdx - locStackIndx);
			stackBufIdx = locStackIndx - parentElementSize;
			jsonIdx++;
			return toReturn;
		}
		
		//first element
		path[pathIndex++] = '0';
		
		parseValue(Common.array);
		numElements++;
		stackBufIdx += Common.array_member_sz;
		boolean lookForValue = false;

		pathIndex--;
		
		while (true) {

			if (stackBufSz <= stackBufIdx + 50) {
				increaseStackBuffer((int)(stackBufSz * 1.2 + 512));
			}

			eatWhiteSpace();
			
			if (json[jsonIdx] == ']') {
				// end of array
				if (lookForValue) { 
					throw new JsonP_ParseException("parse error, in array, looking for another value at index: " + jsonIdx);
				} else {
					jsonIdx++;
					break;
				}
			} else if (json[jsonIdx] == ',') {
				// more elements
				if (lookForValue) { 
					throw new JsonP_ParseException("parse error, in array, looking for another value at index: " + jsonIdx);
				} else {
					valueStart = jsonIdx;
					d_i = ((preserveJson) ? dataIdx : jsonIdx);
					stackBuf[stackBufIdx + Common.array_member_value_offx] = (byte)(0xff & (d_i >> 24));
					stackBuf[stackBufIdx + Common.array_member_value_offx + 1] = (byte)(0xff & (d_i >> 16));
					stackBuf[stackBufIdx + Common.array_member_value_offx + 2] = (byte)(0xff & (d_i >> 8));
					stackBuf[stackBufIdx + Common.array_member_value_offx + 3] = (byte)(0xff & d_i);

					jsonIdx++;
					lookForValue = true;
				}
			} else {
			
				// start - set array index
				indexMult = 1;
				indexNumBytes = 1;
	
				while (numElements >= (indexMult * 10)) {
					indexMult *= 10;
					indexNumBytes++;
				}
					
				int tempNumElements = numElements;
	
				for (int j=1; j<indexNumBytes; j++) {
					if (indexMult > 0) {
						path[pathIndex++] = (byte)((tempNumElements / indexMult) + 48);
						tempNumElements %= indexMult;
						indexMult /= 10;
					} else {
						path[pathIndex++] = '0';
					}
				}

				path[pathIndex++] = (byte)((numElements % 10) + 48);
				// done - set array index
				
				lookForValue = false;
				parseValue(Common.array);
				numElements++;
				stackBufIdx += Common.array_member_sz;
				
				// after parse value back path back up over current index
				pathIndex -= indexNumBytes;
			}
		} // end while(true)
		
		stackBuf[locStackIndx + Common.array_member_sz] = (byte)(0xff & (numElements >> 24));
		stackBuf[locStackIndx + Common.array_member_sz + 1] = (byte)(0xff & (numElements >> 16));
		stackBuf[locStackIndx + Common.array_member_sz + 2] = (byte)(0xff & (numElements >> 8));
		stackBuf[locStackIndx + Common.array_member_sz + 3] = (byte)(0xff & numElements);
		
		stackBuf[stackBufIdx] = Common.extended;
		
		stackBuf[stackBufIdx + Common.array_member_value_offx] = 0;
		stackBuf[stackBufIdx + Common.array_member_value_offx + 1] = 0;
		stackBuf[stackBufIdx + Common.array_member_value_offx + 2] = 0;
		stackBuf[stackBufIdx + Common.array_member_value_offx + 3] = 0;
		
		stackBufIdx += Common.array_member_sz;
		
		if ((stackBufIdx - locStackIndx + Common.array_member_sz + dataIdx) >= dataSz) {			
			increaseDataBuffer((int)(stackBufIdx - locStackIndx + Common.array_member_sz + dataIdx + dataSz * 1.2));
		}

		for (int i=locStackIndx, data_i=dataIdx; i < stackBufIdx; i++, data_i++) {
			data[data_i] = stackBuf[i];
		}
		
		stackBuf[locStackIndx - parentElementSize + Common.array_member_value_offx] = 
				(byte)(0xff & ((dataIdx + Common.array_member_value_offx) >> 24));
		stackBuf[locStackIndx - parentElementSize + Common.array_member_value_offx + 1] = 
				(byte)(0xff & ((dataIdx + Common.array_member_value_offx) >> 16));
		stackBuf[locStackIndx - parentElementSize + Common.array_member_value_offx + 2] = 
				(byte)(0xff & ((dataIdx + Common.array_member_value_offx) >> 8));
		stackBuf[locStackIndx - parentElementSize + Common.array_member_value_offx + 3] = 
				(byte)(0xff & (dataIdx + Common.array_member_value_offx));
		
		stackBuf[locStackIndx - parentElementSize] = Common.array_ptr;

		pathIndex -= 1;

		// If array container were to be indexed uncomment
		//		if (numElements > 0) {
		//			hash=0;
		//			//System.out.print("Adding path: ");
		//			for (int i=pathIndex-4; i<pathIndex; i++) {
		//				hash = 31 * hash + path[i];
		//	//			System.out.print((char)path[i]);	
		//			}
		//			//System.out.print(" - " + hash + "\n");
		//			hashKey = (hash < 0) ? ((hash * -1) % testMapLength) : (hash % testMapLength);
		//								
		//			testMap[hashKey][testMap_i[hashKey]++] = hash;
		//			testMap[hashKey][testMap_i[hashKey]++] = dataIdx; // + Common.member_keyvalue_offx;
		//			//testMapMeta[hash][testMap_i[hash]++] = Common.object_ptr;
		//	//							
		//			if (testMap_i[hashKey] == testMap[hashKey].length) {
		//				increaseMapBucket(hashKey);
		//			}
		//		}
		//END if array container are to be indexed

		currentKeyLength = localCurrentKeyLength;
		
		toReturn = dataIdx;
		dataIdx += (stackBufIdx - locStackIndx);
		stackBufIdx = locStackIndx - parentElementSize;
		
		return toReturn;
	}

	
	
	/*
	 * utility to skip white spaces
	 */
	private void eatWhiteSpace() {
		while ((Common.parse_flags[json[jsonIdx]] & 2) == 2)
			jsonIdx++;
	}
	
	
	/*
	 * Increase a bucket in the Map Index
	 */
	private void increaseMapBucket(int bucketIndex) {
		int[] temp = new int[objectIndexHashes[bucketIndex].length * 2];
							
		for (int k=0; k<temp.length/2; k++) {
			temp[k] = objectIndexHashes[bucketIndex][k];
		}

		objectIndexHashes[bucketIndex] = temp;
	}
	
	
	/*
	 * Increase data buffer
	 */
	private void increaseDataBuffer(int newSz) {
		dataSz = newSz;
		byte temp[] = data; 
		data = new byte[dataSz];
		
		for (int i=0; i< dataIdx; i++)
			data[i] = temp[i];
		
		parseStats.dataIncreases++;
	}
	
	
	/*
	 * Increase the stack buffer
	 */
	private void increaseStackBuffer(int szToAdd) {
		stackBufSz += szToAdd;
		byte temp[] = stackBuf; 
		stackBuf = new byte[stackBufSz];
		
		for (int i=0; i< stackBufIdx; i++)
			stackBuf[i] = temp[i];
		
		parseStats.stackIncreases++;
	}
	
	
	/*
	 * Increase currentKey buffer
	 */
	private void increaseCurrentKeyBuffer(int sz) {
		byte[] temp = new byte[currentKey.length * 2 + sz];
							
		for (int k=0; k<currentKey.length; k++) {
			temp[k] = currentKey[k];
		}
		
		currentKey = temp;
	}
	
	
	/*
	 * Increase path buffer
	 */
	private void increasePathBuffer(int sz) {
		byte[] temp = new byte[path.length * 2 + sz];
							
		for (int k=0; k<path.length; k++) {
			temp[k] = path[k];
		}
		
		path = temp;
	}
	
	/*
	 * set parser options
	 */
	private void setOptions(int options) {
		if ((options & Common.PRESERVE_JSON) > 0)
			preserveJson = true;
		
		if ((options & Common.SHRINK_BUFS) > 0)
			shrinkBuffers = true;
		
		if ((options & Common.DONT_SORT_KEYS) > 0)
			sortKeys = false;
		
		if ((options & Common.WEAK_REF) > 0)
			weakRef = true;
		
		if ((options & Common.CONVERT_NUMERICS) > 0)
			convertNumerics = true;
		
		if ((options & Common.DONT_INDEX_OBJECTS) > 0)
			 indexObjects = false;
		
		
		this.options = options;
	}

	/**
	 * @return the parseStats
	 */
	public ParseStats getParseStats() {
		parseStats.metaLength = dataSz;
		parseStats.metaIndex = dataIdx;
		parseStats.dataLength = (preserveJson) ? dataSz : jsonLen;
		parseStats.dataIndex = (preserveJson) ? dataIdx :jsonIdx;
		
		return parseStats;
	}
	
	
	public class ParseStats {
		public int stackIncreases;
		public int dataIncreases;
		public int metaLength;
		public int metaIndex;
		public int dataLength;
		public int dataIndex;
		public int largestBucket;
		
		public String getStats() {
			for (int j=0; j<objectIndexHashes_i.length; j++) {
				if (objectIndexHashes_i[j] > largestBucket) {
					largestBucket = objectIndexHashes_i[j];
				}
		}
			
			return new StringBuilder("Parse Stats:\n  Stack Increases: ").append(stackIncreases).
				append("\n  Data Increases: ").append(dataIncreases).
				append("\n  Meta Length: ").append(metaLength).
				append("\n  Meta Index: ").append(metaIndex).
				append("\n  Data Length: ").append(dataLength).
				append("\n  Data Index: ").append(dataIndex).
				append("\n  Largest bucket: ").append(largestBucket).toString();
				
		}
	}
	
	
}



