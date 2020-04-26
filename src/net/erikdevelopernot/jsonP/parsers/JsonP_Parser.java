package net.erikdevelopernot.jsonP.parsers;

import net.erikdevelopernot.jsonP.Common;
import net.erikdevelopernot.jsonP.JsonP_ParseException;
import net.erikdevelopernot.jsonP.document.JsonPJson;

public class JsonP_Parser {
	
	//parse options
	private boolean preserveJson = false;
	private boolean shrinkBuffers = false;
	private boolean dontSortKeys = false;
	private boolean weakRef = false;
	private boolean convertNumerics = false;
	private int options;
	
	byte[] json;
	int jsonIdx;
	int jsonLen;
	boolean lookForKey;
	int valueStart;
	
	//stack (meta) variables
//	private byte[][] stackBuf;    !!!!!!test without block at first
//	private int[][] stackBufSz;
	private byte[] stackBuf;
	private int stackBufSz;
//	private int stackBufHeapIdx;
	private int stackBufIdx;
	
	//data (meta-data) variables
//	private byte[][] data;
//	private int[][] dataSz;
	private byte[] data;
	private int dataSz;
//	private int dataHeapIdx;
	private int dataIdx;
	
	private byte covtNumBuf[];
	
	//parse stats
	private ParseStats parseStats;
	
	
	/*
	 * Constructors
	 */
	public JsonP_Parser(byte[] json) {
		this(json, 0);		//use default options
	}
	
	public JsonP_Parser(byte[] json, int options) {
		setOptions(options);
		
		if (convertNumerics) {
			covtNumBuf = new byte[12];
			
			//converting numerics automatically means preserve json
			preserveJson = true;
			this.options |= Common.PRESERVE_JSON;
		}
		
		if (preserveJson) {
			// preserve json data and stack are separate structures
			dataSz = json.length / 2;
//			data = new byte[dataSz];
//			stackBufSz = 1000;
//			stackBuf = new byte[stackBufSz];
//			dataIdx = 0;
//			stackBufIdx = 0;
		} else {
			// data and stack share structure
			/*
			 * note unlike with c++ version where pointers can be used for indexes with java would need to use
			 * wrapper which is too slow, so instead will use if-else to check if stack and data are the same.
			 */
			dataSz = json.length / 4;
//			data = new byte[dataSz];
//			stackBufSz = dataSz;
//			stackBuf = data;
//			dataIdx = 0;
//			stackBufIdx = dataIdx;
		}
	
		/*
		 * !!!! For Java even if not preserve json better to separate the structures
		 */
		data = new byte[dataSz];
		stackBufSz = 1000;
		stackBuf = new byte[stackBufSz];
		dataIdx = 0;
		stackBufIdx = 0;
	
		this.json = json;
		jsonLen = json.length;
		jsonIdx = 0;
		parseStats = new ParseStats();
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
		
		if (json[jsonIdx] == '{') {
//			*(element_type*)&stack_buf[0] = object;
//			set_element_type(stack_buf, 0, object);
			stackBuf[stackBufIdx++] = Common.object;

			
//			*(unsigned int*)&stack_buf[obj_member_key_offx] = 0;
//			set_key_offx_value(stack_buf, 0, 0);
			stackBuf[stackBufIdx++] = (byte)(0xff & (0 >>> 24));
			stackBuf[stackBufIdx++] = (byte)(0xff & (0 >>> 16));
			stackBuf[stackBufIdx++] = (byte)(0xff & (0 >>> 8));
			stackBuf[stackBufIdx++] = (byte)(0xff & 0);
			
//			stack_i += obj_member_sz;
//			stackBufIdx += Common.member_sz;  // <-- no reason stackbufIndx should already be pointing to 5
			
//			data[data_i+4] = '\0';
//			data_i = 5;
			data[dataIdx+4] = '\0';
			dataIdx = 5;

			int dRoot = parseObject();

//for (int q=0; q<dataSz; q++) {
//	System.out.println(q + " - " + data[q]);
//
//	if (data[q] < 0)
//		System.out.println(data[q] & 0x000000FF);
//}
			
			int length = jsonLen;
			
			if (preserveJson)
				length = dataIdx;
			
			if (shrinkBuffers) {
//				std::cout << "shrinking data from: " << data_sz << ", to: " << data_i << std::endl;
//				data = (byte*)realloc(data, data_i);
				
				byte[] temp = data;
				data = new byte[dataIdx];
				
				for (int i=0; i<dataIdx; i++)
					data[i] = temp[i];
				
//				if (!preserveJson)
//					length = dataIdx;
			}

			if (!preserveJson)
//				jsonPjson = new jsonP_json{json, data, length, data_i, i, options};//i+5};
				jsonPJson = new JsonPJson(json, data, length, dataIdx, dRoot, options);
			else
//				jsonPjson = new jsonP_json{data, data, length, data_i, i, options};//i+5};
				jsonPJson = new JsonPJson(data, length, dRoot, options);
				
//			std::cout << "stack_i = " << stack_i << "\ndata_i = " << data_i << ", i = " << i << std::endl;
//			std::cout << "root type: " << *((element_type*)&stack_buf[0]) << ", key: " << (unsigned int)data[*(unsigned int*)&stack_buf[1]] << std::endl;
//			std::cout << "root2 type: " << *(element_type*)&data[i] << ", Key: " << *(unsigned int*)&data[i+1] << std::endl;
//			std::cout << "length: " << length << std::endl;

	//test_parse_object(i+5);

//		} else if (json[index] == '[') {
//			stack_i += arry_member_sz;
////			*(unsigned int*)&stack_buf[arry_member_val_offx] = 0;
//			set_key_offx_value(stack_buf, 0, 0);
////			*(element_type*)&stack_buf[0] = array;
//			set_element_type(stack_buf, 0, array);
//			data[data_i+4] = '\0';
//			data_i = 5;
//
//			unsigned int i = parse_array();
//		
//			if (shrink_buffers) {
////				std::cout << "shrinking data from: " << data_sz << ", to: " << data_i << std::endl;
//				data = (byte*)realloc(data, data_i);
//				
//				if (!use_json)
//					length = data_i;
//			}
//
//			if (use_json)
//				jsonPjson = new jsonP_json{json, data, length, data_i, i, options};//i+5};
//			else
//				jsonPjson = new jsonP_json{data, data, length, data_i, i, options};//i+5};
//			
//	//test_parse_array(i+5);
//
		} else {
			throw new JsonP_ParseException("Error parsing json text, does not appear to be an object or an array");
		}
		
		
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

		if (!lookForKey) {
			if (!preserveJson) {
				while (start < jsonIdx)
					json[valueStart++] = json[start++];
			} else {
//				if (increase_data_buffer(index - start + 5)) {
				if ((jsonIdx - start + 5 + dataIdx) > dataSz) {
					//						std::cout << "increasing the data buffer" << std::endl;
//					data_sz = (unsigned int)data_sz * 1.2 + index - start;
//					data = (byte*) realloc(data, data_sz);
//					stats.data_increases++;
					increaseDataBuffer((int) (dataSz * 1.2 + jsonIdx - start));
				}

				while (start < jsonIdx)
					data[dataIdx++] = json[start++];

				data[dataIdx-1] = '\0';
			}
		} else if (preserveJson) {
//			if (increase_data_buffer(index - start + 5)) {
			if ((jsonIdx - start + 5 + dataIdx) > dataSz) {
//				data_sz = (unsigned int)data_sz * 1.2 + index - start;
//				data = (byte*) realloc(data, data_sz);
//				stats.data_increases++;
				increaseDataBuffer((int) (dataSz * 1.2 + jsonIdx - start));
			}

			while (start < jsonIdx)
				data[dataIdx++] = json[start++];

			data[dataIdx-1] = '\0';
		}
	}
	
	
	/*
	 * Parse numeric, no conversion
	 */
	byte parseNumeric() throws JsonP_ParseException {
		boolean isLong = false;
		byte c = json[jsonIdx];
		boolean sign = true;
		boolean exp = false;
		boolean expSign = false;
		boolean dec = true;

		int s = jsonIdx-1;

		while (c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != ',' && c != ']' && c != '}') {
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
//			c = json[++index];
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
//				data = (byte*) realloc(data, dataSz);
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

		while (c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != ',' && c != ']' && c != '}') {
	
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
//			data = (byte*) realloc(data, data_sz);
			byte temp[] = data;
			data = new byte[dataSz];
			
			for (int i=0; i<dataIdx; i++)
				data[i] = temp[i];
			
			parseStats.dataIncreases++;
		}
		
		if (isLong) {
			if (d <= Long.MAX_VALUE) { 
//				*(long*)&data[data_i] = (long)(d * mult);
//				data_i += sizeof(long);
//				return numeric_long;
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
//				*(double*)&data[data_i] = d * mult;
//				data_i += sizeof(double);
//				return numeric_double;
//				double num = (d * mult);
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
//			*(double*)&data[data_i] = d * mult;
//			data_i += sizeof(double);
//			return numeric_double;
//			double num = (d * mult);
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
	private void parseValue() throws JsonP_ParseException {
		if (json[jsonIdx] == '{') {
			//object
			stackBuf[stackBufIdx] = Common.object;
			stackBufIdx += Common.member_sz;
			parseObject();
		} else if (json[jsonIdx] == '[') {
			//array
			stackBuf[stackBufIdx] = Common.array_ptr;
			stackBufIdx += Common.member_sz;
			parseArray();
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
////			std::string number;
			int s = jsonIdx;
			
////			switch (parse_numeric())//start, end)) 
			switch ((convertNumerics) ? parseNumericCVT() : parseNumeric())
			{
				case Common.numeric_int :
				{
////					*((element_type*)&stack_buf[stack_i]) = numeric_int;
//					set_element_type(stack_buf, stack_i, numeric_int);
					stackBuf[stackBufIdx] = Common.numeric_int;
					break;
				}
				case Common.numeric_long :
				{
//					set_element_type(stack_buf, stack_i, numeric_long);
					stackBuf[stackBufIdx] = Common.numeric_long;
					break;
				}
				case Common.numeric_double :
				{
//					set_element_type(stack_buf, stack_i, numeric_double);
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
	int parseObject() throws JsonP_ParseException {
		
		// create local index for this obj record and advace global index
		int locStackIndx = stackBufIdx;
		int numKeys = 0;
		int toReturn = -1;
		
		//	*((element_type*)&stack_buf[loc_stack_i]) = object;
//		set_element_type(stack_buf, loc_stack_i, object);
		stackBuf[locStackIndx] = Common.object;
		
		//	*(unsigned int*)&stack_buf[loc_stack_i + obj_member_key_offx] = *(unsigned int*)&stack_buf[loc_stack_i - obj_member_sz + obj_member_key_offx];
//		set_key_offx_value(stack_buf, loc_stack_i, get_key_location(stack_buf, loc_stack_i - obj_member_sz));
		stackBuf[locStackIndx+1] = stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx];
		stackBuf[locStackIndx+2] = stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx + 1];
		stackBuf[locStackIndx+3] = stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx + 2];
		stackBuf[locStackIndx+4] = stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx + 3];
		
//		stack_i += (obj_root_sz + obj_member_sz);
		stackBufIdx += Common.root_sz + Common.member_sz;
	
		boolean keepGoing = true;
		boolean localLookForKey = true;
		
		if (json[jsonIdx] != '{') {
			throw new JsonP_ParseException("parse error, expected '{' at index: " + jsonIdx);
		}
		
		++jsonIdx;
		
		try {
			while (keepGoing) {
				if (stackBufSz <= stackBufIdx + 50) {
					increaseStackBuffer(512);
				}
		
				eatWhiteSpace();
	
				//check for end object
				if (json[jsonIdx] == '}') {
					jsonIdx++;
	//				*((unsigned int*)&stack_buf[loc_stack_i + obj_member_sz]) = num_keys;
//					set_uint_a_indx(stack_buf, loc_stack_i + obj_member_sz, num_keys);
					stackBuf[locStackIndx + Common.member_sz] = (byte)(0xff & (numKeys >>> 24));
					stackBuf[locStackIndx + Common.member_sz + 1] = (byte)(0xff & (numKeys >>> 16));
					stackBuf[locStackIndx + Common.member_sz + 2] = (byte)(0xff & (numKeys >>> 8));
					stackBuf[locStackIndx + Common.member_sz + 3] = (byte)(0xff & numKeys);
					
//**** TODO ****
					if (!dontSortKeys) {
//						sort_keys(&stack_buf[loc_stack_i+obj_member_sz+obj_root_sz], 
//							&stack_buf[loc_stack_i+obj_member_sz+obj_root_sz+(obj_member_sz*num_keys)],
//							data, ((use_json) ? json : data));
					}
		
//					*((element_type*)&stack_buf[loc_stack_i+obj_member_sz+obj_root_sz+(obj_member_sz*num_keys)]) = extended;
//					set_element_type(stack_buf, loc_stack_i+obj_member_sz+obj_root_sz+(obj_member_sz*num_keys), extended);
					stackBuf[locStackIndx + Common.member_sz + Common.root_sz + (Common.member_sz * numKeys)] = Common.extended;
	
//					*((unsigned int*)&stack_buf[loc_stack_i+obj_member_sz+obj_root_sz+(obj_member_sz*num_keys)+obj_member_key_offx]) = 0;
//					set_key_offx_value(stack_buf, loc_stack_i+obj_member_sz+obj_root_sz+(obj_member_sz*num_keys), 0);
					int ls_i = locStackIndx + Common.member_sz + Common.root_sz + (Common.member_sz * numKeys) + Common.member_keyvalue_offx;
					stackBuf[ls_i] = (byte)(0xff & (0 >>> 24));
					stackBuf[ls_i + 1] = (byte)(0xff & (0 >>> 16));
					stackBuf[ls_i + 2] = (byte)(0xff & (0 >>> 8));
					stackBuf[ls_i + 3] = (byte)(0xff & 0);
					
//					stack_i += obj_member_sz;
					stackBufIdx += Common.member_sz;
	
//					if (increase_data_buffer(stack_i - loc_stack_i + obj_member_sz)) {
					if ((dataIdx + stackBufIdx - locStackIndx + Common.member_sz) >= dataSz) {
						increaseDataBuffer((int) (dataSz * 1.2 + stackBufIdx - locStackIndx));
					}
	
//					memcpy(&data[data_i], &stack_buf[loc_stack_i], stack_i - loc_stack_i);
					for (int i=locStackIndx, data_i=dataIdx; i < stackBufIdx; i++, data_i++) {
						data[data_i] = stackBuf[i];
					}

//					*(unsigned int*)&stack_buf[loc_stack_i - obj_member_sz + obj_member_key_offx] = data_i + obj_member_key_offx;
//					set_key_offx_value(stack_buf, loc_stack_i - obj_member_sz, data_i + obj_member_key_offx);
					stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx] = 
							(byte)(0xff & ((dataIdx + Common.member_keyvalue_offx) >>> 24));
					stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx + 1] = 
							(byte)(0xff & ((dataIdx + Common.member_keyvalue_offx) >>> 16));
					stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx + 2] = 
							(byte)(0xff & ((dataIdx + Common.member_keyvalue_offx) >>> 8));
					stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx + 3] = 
							(byte)(0xff & (dataIdx + Common.member_keyvalue_offx));
					
					
					
//					*(element_type*)&stack_buf[loc_stack_i - obj_member_sz] = object_ptr;
//					set_element_type(stack_buf, loc_stack_i - obj_member_sz, object_ptr);
					stackBuf[locStackIndx - Common.member_sz] = Common.object_ptr;
	
	//				std::cout << "set obj pointer to: " << *(unsigned int*)&stack_buf[loc_stack_i - obj_member_sz + obj_member_key_offx] <<
	//					", for pointer type: " << *(element_type*)&stack_buf[loc_stack_i - obj_member_sz] << std::endl;
	
//					to_return = data_i;
					toReturn = dataIdx;
//					data_i += (stack_i - loc_stack_i);
					dataIdx += (stackBufIdx - locStackIndx);
//					stack_i = loc_stack_i - obj_member_sz;
					stackBufIdx = locStackIndx - Common.member_sz;
	
					break;
				} else if (json[jsonIdx] == '"') {
					//check for key
					lookForKey = true;
					
					if (!preserveJson) {
	//					*(unsigned int*)&stack_buf[stack_i + obj_member_key_offx] = index + 1;
//						set_key_offx_value(stack_buf, stack_i, index + 1);
						stackBuf[stackBufIdx + Common.member_keyvalue_offx] = (byte)(0xff & ((jsonIdx + 1) >>> 24));
						stackBuf[stackBufIdx + Common.member_keyvalue_offx + 1] = (byte)(0xff & ((jsonIdx + 1) >>> 16));
						stackBuf[stackBufIdx + Common.member_keyvalue_offx + 2] = (byte)(0xff & ((jsonIdx + 1) >>> 8));
						stackBuf[stackBufIdx + Common.member_keyvalue_offx + 3] = (byte)(0xff & (jsonIdx + 1));
					} else {
	//					*(unsigned int*)&stack_buf[stack_i + obj_member_key_offx] = data_i;
//						set_key_offx_value(stack_buf, stack_i, data_i);
						stackBuf[stackBufIdx + Common.member_keyvalue_offx] = (byte)(0xff & (dataIdx >>> 24));
						stackBuf[stackBufIdx + Common.member_keyvalue_offx + 1] = (byte)(0xff & (dataIdx >>> 16));
						stackBuf[stackBufIdx + Common.member_keyvalue_offx + 2] = (byte)(0xff & (dataIdx >>> 8));
						stackBuf[stackBufIdx + Common.member_keyvalue_offx + 3] = (byte)(0xff & dataIdx);
					}
						
					numKeys ++;
	
//					parse_key();
					parseText();
	//				std::cout << "Key: " << *((char**)&stack_buf[stack_i + obj_member_key_offx]) << ", index =" << index << std::endl;
	
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
					eatWhiteSpace();;
					parseValue();
					stackBufIdx += Common.member_sz;
	
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
	int parseArray() throws JsonP_ParseException {
		// create local index for this array record and advace global index
		int locStackIndx = stackBufIdx;
		int numElements = 0;
		int toReturn;

		//std::cout << "Addess: " << &*((char**)&stack_buf[loc_stack_i - obj_member_sz + obj_member_key_offx]) << std::endl;
//		*((element_type*)&stack_buf[loc_stack_i]) = array;
//		set_element_type(stack_buf, loc_stack_i, array);
		stackBuf[locStackIndx] = Common.array;
		
//		*(unsigned int*)&stack_buf[loc_stack_i + arry_member_val_offx] = *(unsigned int*)&stack_buf[loc_stack_i - arry_member_sz + arry_member_val_offx];
//		set_key_offx_value(stack_buf, loc_stack_i, get_val_location(stack_buf, loc_stack_i - arry_member_sz));
		stackBuf[locStackIndx + Common.member_keyvalue_offx] = 
				stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx];
		stackBuf[locStackIndx + Common.member_keyvalue_offx + 1] = 
				stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx + 1];
		stackBuf[locStackIndx + Common.member_keyvalue_offx + 2] = 
				stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx + 2];
		stackBuf[locStackIndx + Common.member_keyvalue_offx + 3] = 
				stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx + 3];
		
//		stack_i += (arry_member_sz + arry_root_sz);
		stackBufIdx += (Common.member_sz + Common.root_sz);
//		value_start = index;
		valueStart = jsonIdx;
		
//		*(unsigned int*)&stack_buf[stack_i + arry_member_val_offx] = (use_json) ? index : data_i;
//		set_key_offx_value(stack_buf, stack_i, (use_json) ? index : data_i);
		int d_i = ((preserveJson) ? dataIdx : jsonIdx);
		stackBuf[stackBufIdx + Common.member_keyvalue_offx] = (byte)(0xff & (d_i >> 24));
		stackBuf[stackBufIdx + Common.member_keyvalue_offx + 1] = (byte)(0xff & (d_i >> 16));
		stackBuf[stackBufIdx + Common.member_keyvalue_offx + 2] = (byte)(0xff & (d_i >> 8));
		stackBuf[stackBufIdx + Common.member_keyvalue_offx + 3] = (byte)(0xff & d_i);
			
//		++index;
		jsonIdx++;
		eatWhiteSpace();
		
		//make sure array isn't empty
		if (json[jsonIdx] == ']') {
//			*((unsigned int*)&stack_buf[loc_stack_i + arry_member_sz]) = num_elements;
//			set_uint_a_indx(stack_buf, loc_stack_i + arry_member_sz, num_elements);
			stackBuf[locStackIndx + Common.member_sz] = (byte)(0xff & (numElements >> 24));
			stackBuf[locStackIndx + Common.member_sz + 1] = (byte)(0xff & (numElements >> 16));
			stackBuf[locStackIndx + Common.member_sz + 2] = (byte)(0xff & (numElements >> 8));
			stackBuf[locStackIndx + Common.member_sz + 3] = (byte)(0xff & numElements);
			
			
//			std::cout << "Arry number keys:" << *((unsigned int*)&stack_buf[loc_stack_i + obj_member_sz]) << ", loc_stack_i: " << loc_stack_i <<
//				", for key index: " << *((int*)&stack_buf[loc_stack_i + obj_member_sz]) << std::endl; 

//			*((element_type*)&stack_buf[stack_i]) = extended;
//			set_element_type(stack_buf, stack_i, extended);
			stackBuf[stackBufIdx] = Common.extended;
			
//			*((unsigned int*)&stack_buf[stack_i + arry_member_val_offx]) = 0; 
//			set_key_offx_value(stack_buf, stack_i, 0);
			stackBuf[stackBufIdx] = 0;
			stackBuf[stackBufIdx + 1] = 0;
			stackBuf[stackBufIdx + 2] = 0;
			stackBuf[stackBufIdx + 3] = 0;
			
//			stack_i += arry_member_sz;
			

//			if (increase_data_buffer(stack_i - loc_stack_i + arry_member_sz)) {
			if ((stackBufIdx - locStackIndx + Common.member_sz + dataIdx) >= dataSz) {			
//				std::cout << "increasing the data buffer" << std::endl;
//				data_sz = (unsigned int)data_sz * 1.2 + stack_i - loc_stack_i;
//				data = (byte*) realloc(data, data_sz);
				increaseDataBuffer(stackBufIdx - locStackIndx + Common.member_sz + dataIdx);
			}

//			memcpy(&data[data_i], &stack_buf[loc_stack_i], stack_i - loc_stack_i);
			for (int i=locStackIndx, data_i=dataIdx; i < stackBufIdx; i++, data_i++) {
				data[data_i] = stackBuf[i];
			}

//			*(unsigned int*)&stack_buf[loc_stack_i - arry_member_sz + arry_member_val_offx] = data_i + arry_member_val_offx;
//			set_key_offx_value(stack_buf, loc_stack_i - arry_member_sz, data_i + arry_member_val_offx);
			stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx] = 
					(byte)(0xff & ((dataIdx + Common.member_keyvalue_offx) >> 24));
			stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx + 1] = 
					(byte)(0xff & ((dataIdx + Common.member_keyvalue_offx) >> 16));
			stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx + 2] = 
					(byte)(0xff & ((dataIdx + Common.member_keyvalue_offx) >> 8));
			stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx + 3] = 
					(byte)(0xff & (dataIdx + Common.member_keyvalue_offx));
			
			
//			*(element_type*)&stack_buf[loc_stack_i - obj_member_sz] = array_ptr;
//			set_element_type(stack_buf, loc_stack_i - obj_member_sz, array_ptr);
			stackBuf[locStackIndx - Common.member_sz] = Common.array_ptr;

			toReturn = dataIdx;
			dataIdx += (stackBufIdx - locStackIndx);
			stackBufIdx = locStackIndx - Common.member_sz;
			
			jsonIdx++;
			return toReturn;
		}
		
		parseValue();
		numElements++;
		stackBufIdx += Common.member_sz;

		boolean lookForValue = false;
		
		while (true) {

//			if (increase_stack_buffer()) {
////				std::cout << "old stack_buf_sz: " << stack_buf_sz;
//				stack_buf_sz = (unsigned int) stack_buf_sz * 1.2;
////				std::cout << ", new stack_buf_sz: " << stack_buf_sz << std::endl;
//				stack_buf = (byte*) realloc(stack_buf, stack_buf_sz);
//				stats.stack_buf_increases++;
//			}
			if (stackBufSz <= stackBufIdx + 50) {
				increaseStackBuffer(512);
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
//					*(unsigned int*)&stack_buf[stack_i + arry_member_val_offx] = (use_json) ? index : data_i;
//					set_key_offx_value(stack_buf, stack_i, (use_json) ? index : data_i);
					d_i = ((preserveJson) ? dataIdx : jsonIdx);
					stackBuf[stackBufIdx + Common.member_keyvalue_offx] = (byte)(0xff & (d_i >> 24));
					stackBuf[stackBufIdx + Common.member_keyvalue_offx + 1] = (byte)(0xff & (d_i >> 16));
					stackBuf[stackBufIdx + Common.member_keyvalue_offx + 2] = (byte)(0xff & (d_i >> 8));
					stackBuf[stackBufIdx + Common.member_keyvalue_offx + 3] = (byte)(0xff & d_i);
					

					jsonIdx++;
					lookForValue = true;
				}
			} else {
				lookForValue = false;
				parseValue();
				numElements++;
				stackBufIdx += Common.member_sz;
			}
		}
		
//		*((unsigned int*)&stack_buf[loc_stack_i + arry_member_sz]) = num_elements;
//		set_uint_a_indx(stack_buf, loc_stack_i + arry_member_sz, num_elements);
		stackBuf[locStackIndx + Common.member_sz] = (byte)(0xff & (numElements >> 24));
		stackBuf[locStackIndx + Common.member_sz + 1] = (byte)(0xff & (numElements >> 16));
		stackBuf[locStackIndx + Common.member_sz + 2] = (byte)(0xff & (numElements >> 8));
		stackBuf[locStackIndx + Common.member_sz + 3] = (byte)(0xff & numElements);
		
		
//		*((element_type*)&stack_buf[stack_i]) = extended;
//		set_element_type(stack_buf, stack_i, extended);
		stackBuf[stackBufIdx] = Common.extended;
		
//		*((unsigned int*)&stack_buf[stack_i + arry_member_val_offx]) = 0; 
//		set_key_offx_value(stack_buf, stack_i, 0);
		stackBuf[stackBufIdx + Common.member_keyvalue_offx] = 0;
		stackBuf[stackBufIdx + Common.member_keyvalue_offx + 1] = 0;
		stackBuf[stackBufIdx + Common.member_keyvalue_offx + 2] = 0;
		stackBuf[stackBufIdx + Common.member_keyvalue_offx + 3] = 0;
		
//		stack_i += arry_member_sz;
		stackBufIdx += Common.member_sz;
		
		if ((stackBufIdx - locStackIndx + Common.member_sz + dataIdx) >= dataSz) {			
//			std::cout << "increasing the data buffer" << std::endl;
//			data_sz = (unsigned int)data_sz * 1.2 + stack_i - loc_stack_i;
//			data = (byte*) realloc(data, data_sz);
			increaseDataBuffer(stackBufIdx - locStackIndx + Common.member_sz + dataIdx);
		}

//		memcpy(&data[data_i], &stack_buf[loc_stack_i], stack_i - loc_stack_i);
		for (int i=locStackIndx, data_i=dataIdx; i < stackBufIdx; i++, data_i++) {
			data[data_i] = stackBuf[i];
		}
		
//		*(unsigned int*)&stack_buf[loc_stack_i - arry_member_sz + arry_member_val_offx] = data_i + arry_member_val_offx;
//		set_key_offx_value(stack_buf, loc_stack_i - arry_member_sz, data_i + arry_member_val_offx);
		stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx] = 
				(byte)(0xff & ((dataIdx + Common.member_keyvalue_offx) >> 24));
		stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx + 1] = 
				(byte)(0xff & ((dataIdx + Common.member_keyvalue_offx) >> 16));
		stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx + 2] = 
				(byte)(0xff & ((dataIdx + Common.member_keyvalue_offx) >> 8));
		stackBuf[locStackIndx - Common.member_sz + Common.member_keyvalue_offx + 3] = 
				(byte)(0xff & (dataIdx + Common.member_keyvalue_offx));
		
		
//		*(element_type*)&stack_buf[loc_stack_i - obj_member_sz] = array_ptr;
//		set_element_type(stack_buf, loc_stack_i - obj_member_sz, array_ptr);
		stackBuf[locStackIndx - Common.member_sz] = Common.array_ptr;

		toReturn = dataIdx;
		dataIdx += (stackBufIdx - locStackIndx);
		stackBufIdx = locStackIndx - Common.member_sz;
		
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
	 * Increase data buffer
	 */
	private void increaseDataBuffer(int newSz) {
//		data_sz = (unsigned int) data_sz * 1.2 + stack_i - loc_stack_i;
		dataSz = newSz;
//		data = (byte*) realloc(data, data_sz);
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
	 * set parser options
	 */
	private void setOptions(int options) {
		if ((options & Common.PRESERVE_JSON) > 0)
			preserveJson = true;
		
		if ((options & Common.SHRINK_BUFS) > 0)
			shrinkBuffers = true;
		
		if ((options & Common.DONT_SORT_KEYS) > 0)
			dontSortKeys = true;
		
		if ((options & Common.WEAK_REF) > 0)
			weakRef = true;
		
		if ((options & Common.CONVERT_NUMERICS) > 0)
			convertNumerics = true;
		
		this.options = options;
	}

	/**
	 * @return the parseStats
	 */
	public ParseStats getParseStats() {
		return parseStats;
	}
	
	
	public class ParseStats {
		public int stackIncreases;
		public int dataIncreases;
	}
}



