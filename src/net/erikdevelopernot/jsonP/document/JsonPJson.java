package net.erikdevelopernot.jsonP.document;

import net.erikdevelopernot.jsonP.Common;


public class JsonPJson {

	private boolean useJson;
	private byte[] data;
	private byte[] metaData;
	private int dataLength;
	private int metaLength;
	private int docRoot;
	
	private boolean dontSortKeys;
	private boolean weakRef;
	private boolean convertNumberics;
	
	
	//variables used for stringify
	int len;
	int i;
//	int meta_i;
	byte txt[];
	byte numCvtBuf[];
	byte numCvtBuf_i;

	
	
	/*
	 * Constructors
	 */
	public JsonPJson(byte[] data, byte[] metaData, int dataLength, int metaLength, int docRoot, int options) {
		this.data = data;
		this.metaData = metaData;
		this.dataLength = dataLength;
		this.metaLength = metaLength;
		this.docRoot = docRoot;
		useJson = true;
		
		dontSortKeys = ((options & Common.DONT_SORT_KEYS) > 0) ? true : false;
		weakRef = ((options & Common.WEAK_REF) > 0) ? true : false;
		convertNumberics = ((options & Common.CONVERT_NUMERICS) > 0) ? true : false;
		
		if (convertNumberics) {
			numCvtBuf = new byte[48];
		}
	}
	
	
	public JsonPJson(byte[] data, int dataLength, int docRoot, int options) {
		this(data, data, dataLength, dataLength, docRoot, options);
		useJson = false;
	}
	
	
	
	
	public byte[] stringify(int precisionIgnoreForNow) {
		len = dataLength/4;
		i = 0;
		int meta_i = docRoot + Common.member_sz;
		txt = new byte[len];
		
//		char float_format[32];
//		float_format[0] = '%';
//		float_format[1] = '.';
//		sprintf(float_format+2, "%dlf", precision);
		
//		if (type == object)
		if (metaData[docRoot] == Common.object)
//			parse_object(len, meta_i, i, txt, (const char*)float_format);
//			parseObject(len, meta_i, i, txt);
			parseObject(meta_i);
//		else if (type == array)
		else if (metaData[docRoot] == Common.array)
			; //parse_array(len, meta_i, i, txt, (const char*)float_format);

//		txt[i] = '\0';
//		txt = (char*) realloc(txt, i+1);

		byte[] temp = txt;
		txt = null;
		return temp;
	}
	

	/*
	 * used by stringify to parse out object types
	 * 
	 * @param meta_i - current index of meta struct
	 */
	void parseObject(int meta_i) {
//		unsigned int k_cnt = get_key_count(meta_data, meta_i);
		int keyCnt = ((metaData[meta_i] << 24) & 0xFF000000) + 
					 ((metaData[meta_i + 1] << 16) & 0x00FF0000) + 
					 ((metaData[meta_i + 2] << 8) & 0x0000FF00) + 
					 (metaData[meta_i + 3] & 0x000000FF);
		
		meta_i += Common.root_sz;
		txt[i++] = '{';

		int loc;
		int keyLoc;
		int keyLen;
		int valLen;
		byte elementType;
		int j=0;
		boolean keepGoing = true;
		
//		object_id ext_loc = get_key_location(meta_data, meta_i + (k_cnt * obj_member_sz));
		int extLoc = ((metaData[meta_i + (keyCnt * Common.member_sz) + Common.member_keyvalue_offx] << 24) & 0xFF000000) + 
					 ((metaData[meta_i + 1 + (keyCnt * Common.member_sz) + Common.member_keyvalue_offx] << 16) & 0x00FF0000) + 
					 ((metaData[meta_i + 2 + (keyCnt * Common.member_sz) + Common.member_keyvalue_offx] << 8) & 0x0000FF00) + 
					 (metaData[meta_i + 3 + (keyCnt * Common.member_sz) + Common.member_keyvalue_offx] & 0x000000FF);
//		System.out.println("ext_loc b4 loop: " + extLoc + ", metat_i: " + meta_i);
		

		while (j < keyCnt || keepGoing) {
		
			if (j >= keyCnt) {
				if (extLoc > 0) {
					meta_i = extLoc;
//					extLoc = get_ext_next(metaData, meta_i);
					extLoc = ((metaData[meta_i + Common.member_keyvalue_ext_next_offx] << 24) & 0xFF000000) + 
							 ((metaData[meta_i + 1 + Common.member_keyvalue_ext_next_offx] << 16) & 0x00FF0000) + 
							 ((metaData[meta_i + 2 + Common.member_keyvalue_ext_next_offx] << 8) & 0x0000FF00) + 
							 (metaData[meta_i + 3 + Common.member_keyvalue_ext_next_offx] & 0x000000FF);
//					std::cout << "next ext_loc in loop: " << ext_loc << std::endl;
				} else {
					keepGoing = false;
					continue;
				}
			}
		
//			type = get_element_type(meta_data, meta_i);
			elementType = metaData[meta_i];
			
			if (elementType == Common.empty) {
				//all empty slots should be in end
				j = keyCnt;
				continue;
			}
				
			if (j != 0)
				txt[i++] = ',';
			
//			std::cout << "type of ele: " << type << ", meta_i: " << meta_i << std::endl;
//			loc = get_key_location(meta_data, meta_i);
			loc = ((metaData[meta_i + Common.member_keyvalue_offx] << 24) & 0xFF000000) + 
					 ((metaData[meta_i + 1 + Common.member_keyvalue_offx] << 16) & 0x00FF0000) + 
					 ((metaData[meta_i + 2 + Common.member_keyvalue_offx] << 8) & 0x0000FF00) + 
					 (metaData[meta_i + 3 + Common.member_keyvalue_offx] & 0x000000FF);
			
			keyLoc = loc;
			meta_i += Common.member_sz;
//			std::cout << "k_loc: " << k_loc << std::endl;		

			if (elementType == Common.object_ptr || elementType == Common.array_ptr) {
//				keyLoc = get_uint_a_indx(meta_data, loc);
				keyLoc = ((metaData[loc] << 24) & 0xFF000000) + 
						 ((metaData[loc + 1] << 16) & 0x00FF0000) + 
						 ((metaData[loc + 2] << 8) & 0x0000FF00) + 
						 (metaData[loc + 3] & 0x000000FF);
			}		

//			k_len = strlen(data+k_loc);
			keyLen = strLen(data, keyLoc);
			txt[i++] = '"';
			
//			len = increase_txt_buffer(k_len, len, i, txt);
			if (keyLen + 20 > len - i || i >= len) {
				len += (keyLen + 20 + (int)(len * 0.25));
				byte temp[] = txt;
				txt = new byte[len];
				
				for (int i_ = 0; i_ < i; i_++)
					txt[i_] = temp[i_];
			}
			
//			memcpy(&txt[i], &data[k_loc], k_len);
			for (int indx=0; indx<keyLen; indx++)
				txt[i++] = data[keyLoc + indx];
			
//			i += keyLen;
			
			txt[i++] = '"';
			txt[i++] = ':';
			
			if (elementType == Common.string) {
				txt[i++] = '"';
//				v_len = strlen(data + k_loc + k_len + 1);
				valLen = strLen(data, keyLoc + keyLen +1);
				
//				len = increase_txt_buffer(v_len, len, i, txt);
				if (valLen + 20 > len - i || i >= len) {
					len += (valLen + 20 + (int)(len * 0.25));
					byte temp[] = txt;
					txt = new byte[len];
					
					for (int i_ = 0; i_ < i; i_++)
						txt[i_] = temp[i_];
				}
				
//				memcpy(&txt[i], &data[k_loc + k_len + 1], v_len);
				for (int indx=0; indx<valLen; indx++)
					txt[i++] = data[keyLoc + keyLen + 1 + indx];
				
//				i+=v_len;
				txt[i++] = '"';
			} else if (elementType == Common.numeric_long || elementType == Common.numeric_double) {
				if (!convertNumberics) {
					valLen = strLen(data, keyLoc + keyLen + 1);
					
					if (valLen + 20 > len - i || i >= len) {
						len += (valLen + 20 + (int)(len * 0.25));
						byte temp[] = txt;
						txt = new byte[len];
						
						for (int i_ = 0; i_ < i; i_++)
							txt[i_] = temp[i_];
					}
					
//					memcpy(&txt[i], &data[k_loc + k_len + 1], v_len);
					for (int indx=0; indx<valLen; indx++)
						txt[i++] = data[keyLoc + keyLen + 1 + indx];
					
					i += valLen;
				} else { 
					if (48 > len - i || i >= len) {
						len += (48 + (int)(len * 0.25));
						byte temp[] = txt;
						txt = new byte[len];
						
						for (int i_ = 0; i_ < i; i_++)
							txt[i_] = temp[i_];
					}
					
					int valLoc = keyLoc + keyLen + 1;
					
					if (elementType == Common.numeric_long) {
//						len = increase_txt_buffer(sizeof(long), len, i, txt);
//						i += sprintf(&txt[i], "%ld", *(long*)&data[k_loc + k_len + 1]);

						long num = (((data[valLoc+4] << 24) & 0xFF000000L) |
								   ((data[valLoc+5] << 16) & 0x00FF0000) |
								   ((data[valLoc+6] << 8) & 0x0000FF00) |
								   (data[valLoc+7] & 0x000000FF)) |
								   ((((data[valLoc] << 24) & 0xFF000000L) |
								   ((data[valLoc+1] << 16) & 0x00FF0000) |
								   ((data[valLoc+2] << 8) & 0x0000FF00) |
								   (data[valLoc+3] & 0x000000FF)) << 32);
						
						if (num < 0) {
							num = ~(num) + 1;
							txt[i++] = '-';
						}
						
						long remainder=num, modulus;
						numCvtBuf_i = 47;
						
						do {
							modulus = remainder % 10;
						
							if (modulus == 0) 
								numCvtBuf[numCvtBuf_i--] = '0';
							else if (modulus == 1) 
								numCvtBuf[numCvtBuf_i--] = '1';
							else if (modulus == 2) 
								numCvtBuf[numCvtBuf_i--] = '2';
							else if (modulus == 3) 
								numCvtBuf[numCvtBuf_i--] = '3';
							else if (modulus == 4) 
								numCvtBuf[numCvtBuf_i--] = '4';
							else if (modulus == 5) 
								numCvtBuf[numCvtBuf_i--] = '5';
							else if (modulus == 6) 
								numCvtBuf[numCvtBuf_i--] = '6';
							else if (modulus == 7) 
								numCvtBuf[numCvtBuf_i--] = '7';
							else if (modulus == 8) 
								numCvtBuf[numCvtBuf_i--] = '8';
							else if (modulus == 9) 
								numCvtBuf[numCvtBuf_i--] = '9';
							
							remainder = remainder / 10;
						} while (remainder > 0);
						
						for (++numCvtBuf_i; numCvtBuf_i < 48; numCvtBuf_i++) {
							txt[i++] = numCvtBuf[numCvtBuf_i];

						}
					} else {
//						len = increase_txt_buffer(sizeof(double), len, i, txt);
//						i += sprintf(&txt[i], float_format, *(double*)&data[k_loc + k_len + 1]);
// !!!!!! REVISIT
						
						long doubleBits = (((data[valLoc+4] << 24) & 0xFF000000L) |
								   		  ((data[valLoc+5] << 16) & 0x00FF0000) |
								   		  ((data[valLoc+6] << 8) & 0x0000FF00) |
								   		  (data[valLoc+7] & 0x000000FF)) |
								   		  ((((data[valLoc] << 24) & 0xFF000000L) |
								   		  ((data[valLoc+1] << 16) & 0x00FF0000) |
								   		  ((data[valLoc+2] << 8) & 0x0000FF00) |
								   		  (data[valLoc+3] & 0x000000FF)) << 32);
						
						
						String doubleString = String.valueOf(Double.longBitsToDouble(doubleBits));
						
						for (int d=0; d<doubleString.length(); d++) {
							txt[i++] = (byte)doubleString.charAt(d);
							System.out.print(doubleString.charAt(d));
						}
						System.out.println();
					}
				}
			} else if (elementType == Common.bool) {
				if (data[keyLoc + keyLen + 1] == '1') {
					txt[i++] = 't';
					txt[i++] = 'r';
					txt[i++] = 'u';
					txt[i++] = 'e';
				} else {
					txt[i++] = 'f';
					txt[i++] = 'a';
					txt[i++] = 'l';
					txt[i++] = 's';
					txt[i++] = 'e';
				}
			} else if (elementType == Common.bool_true) {
				txt[i++] = 't';
				txt[i++] = 'r';
				txt[i++] = 'u';
				txt[i++] = 'e';
			} else if (elementType == Common.bool_false) {
				txt[i++] = 'f';
				txt[i++] = 'a';
				txt[i++] = 'l';
				txt[i++] = 's';
				txt[i++] = 'e';
			} else if (elementType == Common.object_ptr) {
//				loc += sizeof(object_id);
				loc += 4;
//				parse_object(len, loc, i, txt, float_format);
				parseObject(loc);
			} else if (elementType == Common.object) {
				//do nothing should never happen
			} else if (elementType == Common.array_ptr) {
				loc += 4;
				parseArray(loc);
			} else if (elementType == Common.nil) {
				txt[i++] = 'n';
				txt[i++] = 'u';
				txt[i++] = 'l';
				txt[i++] = 'l';
			}
			
			j++;
		}
		
		txt[i++] = '}';
	}
	
	
	
	/*
	 * parse a json array
	 */
	void parseArray(int meta_i) {
//		int numElements = get_key_count(meta_data, meta_i);
		int numElements = ((metaData[meta_i] << 24) & 0xFF000000) + 
				 		  ((metaData[meta_i + 1] << 16) & 0x00FF0000) + 
				 		  ((metaData[meta_i + 2] << 8) & 0x0000FF00) + 
				 		  (metaData[meta_i + 3] & 0x000000FF);
	
		meta_i += Common.root_sz;
		int valLen;
		int valLoc;
		byte elementType; 
		int k=0;
		boolean keepGoing = true;
		
//		int extLoc = get_val_location(meta_data, meta_i + (num_elements * arry_member_sz));
		int extLoc = ((metaData[meta_i + (numElements * Common.member_sz) + Common.member_keyvalue_offx] << 24) & 0xFF000000) + 
		 		  	 ((metaData[meta_i + 1 + (numElements * Common.member_sz) + Common.member_keyvalue_offx] << 16) & 0x00FF0000) + 
		 		  	 ((metaData[meta_i + 2 + (numElements * Common.member_sz) + Common.member_keyvalue_offx] << 8) & 0x0000FF00) + 
		 		  	 (metaData[meta_i + 3 + (numElements * Common.member_sz) + Common.member_keyvalue_offx] & 0x000000FF);

		txt[i++] = '[';
		
		while (k < numElements || keepGoing) {
		
			if (k >= numElements) {
				if (extLoc > 0) {
					meta_i = extLoc;
					//assign next ext value
//					extLoc = get_ext_next(meta_data, meta_i);
					extLoc = ((metaData[meta_i + Common.member_keyvalue_ext_next_offx] << 24) & 0xFF000000) + 
					 		  ((metaData[meta_i + Common.member_keyvalue_ext_next_offx + 1] << 16) & 0x00FF0000) + 
					 		  ((metaData[meta_i + Common.member_keyvalue_ext_next_offx + 2] << 8) & 0x0000FF00) + 
					 		  (metaData[meta_i + Common.member_keyvalue_ext_next_offx + 3] & 0x000000FF);
//					std::cout << "next ext_loc in loop: " << ext_loc << std::endl;
				} else {
					keepGoing = false;
					continue;
				}
			}
			
//			type = get_element_type(meta_data, meta_i);
			elementType = metaData[meta_i];
			
			if (elementType == Common.empty) {
				k = numElements;
				continue;
			}
			
			if (k != 0)
				txt[i++] = ',';
				
//			valLoc = get_key_location(meta_data, meta_i);
			valLoc = ((metaData[meta_i + Common.member_keyvalue_offx] << 24) & 0xFF000000) + 
			 		  ((metaData[meta_i + Common.member_keyvalue_offx + 1] << 16) & 0x00FF0000) + 
			 		  ((metaData[meta_i + Common.member_keyvalue_offx + 2] << 8) & 0x0000FF00) + 
			 		  (metaData[meta_i + Common.member_keyvalue_offx + 3] & 0x000000FF);
			
			meta_i += Common.member_sz;
			
			if (elementType == Common.string) {
				txt[i++] = '"';
//				v_len = strlen(data + v_loc);
				valLen = strLen(data, valLoc);
				
//				len = increase_txt_buffer(v_len, len, i, txt);
				if (valLen + 20 > len - i || i >= len) {
					len += (valLen + 20 + (int)(len * 0.25));
					byte temp[] = txt;
					txt = new byte[len];
					
					for (int i_ = 0; i_ < i; i_++)
						txt[i_] = temp[i_];
				}
				
//				memcpy(&txt[i], &data[v_loc], v_len);
				for (int indx=0; indx<valLen; indx++) 
					txt[i++] = data[valLoc + indx];
				
				i += valLen;
				txt[i++] = '"';
			} else if (elementType == Common.numeric_long || elementType == Common.numeric_double) {
//				if (!convert_numerics) {
//					v_len = strlen(data + v_loc);
//					len = increase_txt_buffer(v_len, len, i, txt);
//					memcpy(&txt[i], &data[v_loc], v_len);
//					i+=v_len;
//				} else if (type == numeric_long) {
//					len = increase_txt_buffer(sizeof(long), len, i, txt);
//					i += sprintf(&txt[i], "%ld", *(long*)&data[v_loc]);
//				} else {
//					len = increase_txt_buffer(sizeof(double), len, i, txt);
//					i += sprintf(&txt[i], float_format, *(double*)&data[v_loc]);
//				}
				
				if (!convertNumberics) {
					valLen = strLen(data, valLoc);
					
					if (valLen + 20 > len - i || i >= len) {
						len += (valLen + 20 + (int)(len * 0.25));
						byte temp[] = txt;
						txt = new byte[len];
						
						for (int i_ = 0; i_ < i; i_++)
							txt[i_] = temp[i_];
					}
					
//					memcpy(&txt[i], &data[k_loc + k_len + 1], v_len);
					for (int indx=0; indx<valLen; indx++)
						txt[i++] = data[valLoc];
					
					i += valLen;
				} else { 
					if (48 > len - i || i >= len) {
						len += (48 + (int)(len * 0.25));
						byte temp[] = txt;
						txt = new byte[len];
						
						for (int i_ = 0; i_ < i; i_++)
							txt[i_] = temp[i_];
					}
					
					if (elementType == Common.numeric_long) {
//						len = increase_txt_buffer(sizeof(long), len, i, txt);
//						i += sprintf(&txt[i], "%ld", *(long*)&data[k_loc + k_len + 1]);
						
						long num = (((data[valLoc+4] << 24) & 0xFF000000L) |
								   ((data[valLoc+5] << 16) & 0x00FF0000) |
								   ((data[valLoc+6] << 8) & 0x0000FF00) |
								   (data[valLoc+7] & 0x000000FF)) |
								   ((((data[valLoc] << 24) & 0xFF000000L) |
								   ((data[valLoc+1] << 16) & 0x00FF0000) |
								   ((data[valLoc+2] << 8) & 0x0000FF00) |
								   (data[valLoc+3] & 0x000000FF)) << 32);
						
						if (num < 0) {
							num = ~(num) + 1;
							txt[i++] = '-';
						}
						
						long remainder=num, modulus;
						numCvtBuf_i = 47;
						
						do {
							modulus = remainder % 10;
						
							if (modulus == 0) 
								numCvtBuf[numCvtBuf_i--] = '0';
							else if (modulus == 1) 
								numCvtBuf[numCvtBuf_i--] = '1';
							else if (modulus == 2) 
								numCvtBuf[numCvtBuf_i--] = '2';
							else if (modulus == 3) 
								numCvtBuf[numCvtBuf_i--] = '3';
							else if (modulus == 4) 
								numCvtBuf[numCvtBuf_i--] = '4';
							else if (modulus == 5) 
								numCvtBuf[numCvtBuf_i--] = '5';
							else if (modulus == 6) 
								numCvtBuf[numCvtBuf_i--] = '6';
							else if (modulus == 7) 
								numCvtBuf[numCvtBuf_i--] = '7';
							else if (modulus == 8) 
								numCvtBuf[numCvtBuf_i--] = '8';
							else if (modulus == 9) 
								numCvtBuf[numCvtBuf_i--] = '9';
							
							remainder = remainder / 10;
						} while (remainder > 0);
						
						for (++numCvtBuf_i; numCvtBuf_i < 47; numCvtBuf_i++)
							txt[i++] = numCvtBuf[numCvtBuf_i];

					} else {
//						len = increase_txt_buffer(sizeof(double), len, i, txt);
//						i += sprintf(&txt[i], float_format, *(double*)&data[k_loc + k_len + 1]);
						
						long doubleBits = (((data[valLoc+4] << 24) & 0xFF000000L) |
						   		  ((data[valLoc+5] << 16) & 0x00FF0000) |
						   		  ((data[valLoc+6] << 8) & 0x0000FF00) |
						   		  (data[valLoc+7] & 0x000000FF)) |
						   		  ((((data[valLoc] << 24) & 0xFF000000L) |
						   		  ((data[valLoc+1] << 16) & 0x00FF0000) |
						   		  ((data[valLoc+2] << 8) & 0x0000FF00) |
						   		  (data[valLoc+3] & 0x000000FF)) << 32);
				
						byte doubleBytes[] = String.valueOf(doubleBits).getBytes();
						
						for (int d=0; d<doubleBytes.length; d++) 
							txt[i++] = doubleBytes[d];
							
					}
				}
			} else if (elementType == Common.bool) {
				if (data[valLoc] == '1') {
					txt[i++] = 't';
					txt[i++] = 'r';
					txt[i++] = 'u';
					txt[i++] = 'e';
				} else {
					txt[i++] = 'f';
					txt[i++] = 'a';
					txt[i++] = 'l';
					txt[i++] = 's';
					txt[i++] = 'e';
				}
			} else if (elementType == Common.bool_true) {
				txt[i++] = 't';
				txt[i++] = 'r';
				txt[i++] = 'u';
				txt[i++] = 'e';
			} else if (elementType == Common.bool_false) {
				txt[i++] = 'f';
				txt[i++] = 'a';
				txt[i++] = 'l';
				txt[i++] = 's';
				txt[i++] = 'e';
			} else if (elementType == Common.object_ptr) {
				valLoc += 4;
				parseObject(valLoc);
			} else if (elementType == Common.object) {
				//nothing - should never happen
			} else if (elementType == Common.array_ptr) {
				valLoc += 4;
				parseArray(valLoc);
			} else if (elementType == Common.nil) {
				txt[i++] = 'n';
				txt[i++] = 'u';
				txt[i++] = 'l';
				txt[i++] = 'l';
			} 
			
			k++;
		}
		
		txt[i++] = ']';
	}

	
	
	/*
	 * c - equiv to length of null terminated char*
	 * 
	 * @param bytes - array holding bytes
	 * @param start - index to start at
	 * @returns byte count not including null terminator
	 */
	private int strLen(byte[] bytes, int start) {
		int s = start;

		while(bytes[s++] != '\0')
			;
		
		return s - start -1;
	}
	
	
	/*
	 * Used to increase buffers used by stringify ****use function for now, test later copy the copy to avoid jump
	 * 
	 * @param needed - amount needed
	 * @param indx - current txt[] index
	 * @returns either new or existing byte[]
	 */
//	byte[] increaseTxtBuffer(int needed, int sz, int indx, byte[] txt) {
//		//add check if indx is greater then sz,
//		if (needed + 20 > sz - indx || indx >= sz) {
////			std::cout << "Allocating, current sz: " << sz << ", needed: " << needed << ", indx: " << indx << std::endl;
//			sz += (needed + (unsigned int)(sz * 0.25));
//			txt = (char*) realloc(txt, sz);
////			std::cout << "stringify realloc needed, new size: " << sz << std::endl;
//		}
//		
//		return sz;
//	}
	
	
	public void crap() {
		System.out.println("Meta:\n" + new String(metaData));
		System.out.println("Data:\n" + new String(data));
	}
}
