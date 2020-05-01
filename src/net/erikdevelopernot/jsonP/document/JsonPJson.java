package net.erikdevelopernot.jsonP.document;

import org.omg.CORBA.OMGVMCID;

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
	private byte txt[];
	private byte txtPretty[];
	private byte numCvtBuf[];
	private byte numCvtBuf_i;
	private boolean pretty;
	int indentLength;
	

	private enum TxtType { TXT, TXT_PRETTY };
	
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
	
	
	
	
	
	
	/*
	 * Todo conversion from jsonP c++ parser, see how far I get
	 */
	// manipulate methods
//		object_id add_container(const char *, unsigned int, object_id, element_type);
//		int add_value_type(element_type, object_id, const char *, void * = NULL);
//		
//		int update_value(object_id, index_type, element_type, void *);
//		int update_value(search_path_element *, unsigned int, element_type, void *);
//		int update_value(const char *, const char *, element_type, void *);
//
//		//for delete value it makes it way easier to have full path so no option for object_id for now
//		int delete_value(search_path_element *, unsigned int, /*char *,*/ error*);
//		int delete_value(const char *path, const char *delim, /*char *,*/ error *);
//		int delete_value(object_id id, object_id parent, /*char *,*/ error *);
//		
//		// access methods
//		object_id get_doc_root() { return doc_root; }
//		object_id get_object_id(search_path_element *, unsigned int);
//		object_id get_object_id(const char *path, const char *delim);
//
//		unsigned int get_members_count(object_id);
//		unsigned int get_members_count(search_path_element *, unsigned int);
//		unsigned int get_members_count(const char *path, const char *delim);
//		
//		inline unsigned int get_elements_count(object_id id) { return get_members_count(id); }
//		inline unsigned int get_elements_count(search_path_element *path, unsigned int path_count) { return get_members_count(path, path_count); }
//		inline unsigned int get_elements_count(const char *path, const char *delim) { return get_members_count(path, delim); }
//		
//		//object_key* needs to be freed by user when done
//		unsigned int get_keys(search_path_element *, unsigned int, struct object_key *&);
//		unsigned int get_keys(const char *path, const char *delim, struct object_key *&);
//		unsigned int get_keys(object_id, struct object_key *&);
//		
//		
//		/*
//		 * for the first call pass the object_id or search_path or char *path, for remaining calls pass NULL or 0 (object_id)
//		 * when no more elements are avail and empty element_type will be returned.
//		 * an internal buffer is used to hold the value returned. This value will be replaced
//		 * with each call to next. The memoery alloc/dealloc of this buffer is handled by the parser
//		 */
//		element_type get_next_array_element(object_id, const void *&);
//		element_type get_next_array_element(search_path_element *, unsigned int, const void *&);
//		element_type get_next_array_element(const char *path, const char *delim, const void *&);
//		
//		double get_double_value(search_path_element *, unsigned int, error*);
//		double get_double_value(const char *, const char *, error*);
//		double get_double_value(object_id, index_type, error*);
//		double get_double_value(const char *key, object_id parent, error*);
//		long get_long_value(search_path_element *, unsigned int, error*);
//		long get_long_value(const char *, const char *, error*);
//		long get_long_value(object_id, index_type, error*);
//		long get_long_value(const char *key, object_id parent, error*);
//		bool get_bool_value(search_path_element *, unsigned int, error*);
//		bool get_bool_value(const char *, const char *, error*);
//		bool get_bool_value(object_id, index_type, error*);
//		bool get_bool_value(const char *key, object_id parent, error*);
//		const char* get_string_value(search_path_element *, unsigned int, error*);
//		const char* get_string_value(const char *, const char *, error*);
//		const char* get_string_value(object_id, index_type, error*);
//		const char* get_string_value(const char *key, object_id parent, error*);
//		
	
	
	/*
	 * Zero whitespace text representation of the Json
	 * 
	 * @param precisionIgnoreForNow deciaml preceision for real numbers
	 * 
	 * returns byte[]
	 */
	public byte[] stringify(int precisionIgnoreForNow, boolean pretty) {
		this.pretty = pretty;
		indentLength = 0;
		len = dataLength/4;
		i = 0;
		int meta_i = docRoot + Common.member_sz;
		txt = new byte[len];
		
		if (metaData[docRoot] == Common.object)
			parseObject(meta_i);
		else if (metaData[docRoot] == Common.array)
			parseArray(meta_i);


		byte[] temp = txt;
		txt = null;
		return temp;
	}
	
	
	
		

	/*
	 * used by stringify to parse out object types
	 * 
	 * @param meta_i - current index of meta struct
	 */
	private void parseObject(int meta_i) {
		int keyCnt = ((metaData[meta_i] << 24) & 0xFF000000) + 
					 ((metaData[meta_i + 1] << 16) & 0x00FF0000) + 
					 ((metaData[meta_i + 2] << 8) & 0x0000FF00) + 
					 (metaData[meta_i + 3] & 0x000000FF);

		meta_i += Common.root_sz;
		txt[i++] = '{';

		//check to make sure dont allow creating object with 0 keys but allow ext locations ????, if so remove
		if (keyCnt <= 0) {
			txt[i++] = '}';
			return;
		}
		
		if (pretty) {
			txt[i++] = '\n';
			indentLength += 2;
		}
		
		int loc;
		int keyLoc;
		int keyLen;
		int valLen;
		byte elementType;
		int j=0;
		boolean keepGoing = true;
		
		int extLoc = ((metaData[meta_i + (keyCnt * Common.member_sz) + Common.member_keyvalue_offx] << 24) & 0xFF000000) + 
					 ((metaData[meta_i + 1 + (keyCnt * Common.member_sz) + Common.member_keyvalue_offx] << 16) & 0x00FF0000) + 
					 ((metaData[meta_i + 2 + (keyCnt * Common.member_sz) + Common.member_keyvalue_offx] << 8) & 0x0000FF00) + 
					 (metaData[meta_i + 3 + (keyCnt * Common.member_sz) + Common.member_keyvalue_offx] & 0x000000FF);

		while (j < keyCnt || keepGoing) {
		
			if (j >= keyCnt) {
				if (extLoc > 0) {
					meta_i = extLoc;
					extLoc = ((metaData[meta_i + Common.member_keyvalue_ext_next_offx] << 24) & 0xFF000000) + 
							 ((metaData[meta_i + 1 + Common.member_keyvalue_ext_next_offx] << 16) & 0x00FF0000) + 
							 ((metaData[meta_i + 2 + Common.member_keyvalue_ext_next_offx] << 8) & 0x0000FF00) + 
							 (metaData[meta_i + 3 + Common.member_keyvalue_ext_next_offx] & 0x000000FF);
				} else {
					keepGoing = false;
					continue;
				}
			}
		
			elementType = metaData[meta_i];
			
			if (elementType == Common.empty) {
				//all empty slots should be in end
				j = keyCnt;
				continue;
			}
				
			loc = ((metaData[meta_i + Common.member_keyvalue_offx] << 24) & 0xFF000000) + 
					 ((metaData[meta_i + 1 + Common.member_keyvalue_offx] << 16) & 0x00FF0000) + 
					 ((metaData[meta_i + 2 + Common.member_keyvalue_offx] << 8) & 0x0000FF00) + 
					 (metaData[meta_i + 3 + Common.member_keyvalue_offx] & 0x000000FF);
			
			keyLoc = loc;
			meta_i += Common.member_sz;

			if (elementType == Common.object_ptr || elementType == Common.array_ptr) {
				keyLoc = ((metaData[loc] << 24) & 0xFF000000) + 
						 ((metaData[loc + 1] << 16) & 0x00FF0000) + 
						 ((metaData[loc + 2] << 8) & 0x0000FF00) + 
						 (metaData[loc + 3] & 0x000000FF);
			}		

			keyLen = strLen(data, keyLoc);
			
			if (pretty) {
				if (indentLength + keyLen + 20 > len - i || i >= len) 
					increaseJsonBuffer(indentLength + keyLen + 20);
				
				if (j != 0) {
					txt[i++] = ',';
					txt[i++] = '\n';
				}
				
				for (int ind=0; ind<indentLength; ind++)
					txt[i++] = ' ';
			} else {
				if (keyLen + 20 + indentLength > len - i || i >= len) 
					increaseJsonBuffer(keyLen + 20);
				
				if (j != 0)
					txt[i++] = ',';
			}
			
			txt[i++] = '"';
			
			for (int indx=0; indx<keyLen; indx++)
				txt[i++] = data[keyLoc + indx];

			if (pretty) {
				txt[i++] = '"';
				txt[i++] = ' ';
				txt[i++] = ':';
				txt[i++] = ' ';
			} else {
				txt[i++] = '"';
				txt[i++] = ':';
			}
			
			if (elementType == Common.string) {
				valLen = strLen(data, keyLoc + keyLen +1);
				
				if (valLen + 20 > len - i || i >= len) 
					increaseJsonBuffer(valLen + 20);
				
				txt[i++] = '"';
				
				for (int indx=0; indx<valLen; indx++)
					txt[i++] = data[keyLoc + keyLen + 1 + indx];
				
				txt[i++] = '"';
				
			} else if (elementType == Common.numeric_long || elementType == Common.numeric_double) {
				if (!convertNumberics) {
					valLen = strLen(data, keyLoc + keyLen + 1);
					
					if (valLen + 20 > len - i || i >= len) 
						increaseJsonBuffer(valLen + 20);
					
					for (int indx=0; indx<valLen; indx++)
						txt[i++] = data[keyLoc + keyLen + 1 + indx];
					
					i += valLen;
				} else { 
					if (64 > len - i || i >= len) 
						increaseJsonBuffer(64);
						
					
					int valLoc = keyLoc + keyLen + 1;
					
					if (elementType == Common.numeric_long) {

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
//							System.out.print(doubleString.charAt(d));
						}
//						System.out.println();
					}
				}
			} else if (elementType == Common.bool) {
				if (64 > len - i || i >= len) 
					increaseJsonBuffer(64);
				
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
				if (64 > len - i || i >= len) 
					increaseJsonBuffer(64);
				
				txt[i++] = 't';
				txt[i++] = 'r';
				txt[i++] = 'u';
				txt[i++] = 'e';
			} else if (elementType == Common.bool_false) {
				if (64 > len - i || i >= len) 
					increaseJsonBuffer(64);
				
				txt[i++] = 'f';
				txt[i++] = 'a';
				txt[i++] = 'l';
				txt[i++] = 's';
				txt[i++] = 'e';
			} else if (elementType == Common.object_ptr) {
				loc += 4;
				parseObject(loc);
			} else if (elementType == Common.object || elementType == Common.array) {
				//do nothing should never happen
			} else if (elementType == Common.array_ptr) {
				loc += 4;
				parseArray(loc);
			} else if (elementType == Common.nil) {
				if (64 > len - i || i >= len) 
					increaseJsonBuffer(64);
				
				txt[i++] = 'n';
				txt[i++] = 'u';
				txt[i++] = 'l';
				txt[i++] = 'l';
			}
			
//			if (pretty)
//				txt[i++] = '\n';
			
			j++;
		}
		
		if (pretty) {
			if (indentLength + 10 > len - i || i >= len) 
				increaseJsonBuffer(indentLength + 10);
			
			txt[i++] = '\n';
			indentLength -= 2;
			
			for (int ind=0; ind<indentLength; ind++)
				txt[i++] = ' ';
			
			txt[i++] = '}';
		
		} else {
			txt[i++] = '}';
		}
	}

	
	/*
	 * parse a json array
	 */
	private void parseArray(int meta_i) {
		int numElements = ((metaData[meta_i] << 24) & 0xFF000000) + 
				 		  ((metaData[meta_i + 1] << 16) & 0x00FF0000) + 
				 		  ((metaData[meta_i + 2] << 8) & 0x0000FF00) + 
				 		  (metaData[meta_i + 3] & 0x000000FF);
	
		meta_i += Common.root_sz;
		
		if (40 > len - i || i >= len) 
			increaseJsonBuffer(40);
		
		
		txt[i++] = '[';
		
		if (numElements == 0) {
			txt[i++] = ']';
			return;
		}
		
		if (pretty) {
			txt[i++] = '\n';
			indentLength += 2;
		}
		
		int valLen;
		int valLoc;
		byte elementType; 
		int k=0;
		boolean keepGoing = true;
		
		int extLoc = ((metaData[meta_i + (numElements * Common.member_sz) + Common.member_keyvalue_offx] << 24) & 0xFF000000) + 
		 		  	 ((metaData[meta_i + 1 + (numElements * Common.member_sz) + Common.member_keyvalue_offx] << 16) & 0x00FF0000) + 
		 		  	 ((metaData[meta_i + 2 + (numElements * Common.member_sz) + Common.member_keyvalue_offx] << 8) & 0x0000FF00) + 
		 		  	 (metaData[meta_i + 3 + (numElements * Common.member_sz) + Common.member_keyvalue_offx] & 0x000000FF);

//		txt[i++] = '[';
		
		while (k < numElements || keepGoing) {

			if (k >= numElements) {
				if (extLoc > 0) {
					meta_i = extLoc;
					//assign next ext value
					extLoc = ((metaData[meta_i + Common.member_keyvalue_ext_next_offx] << 24) & 0xFF000000) + 
					 		  ((metaData[meta_i + Common.member_keyvalue_ext_next_offx + 1] << 16) & 0x00FF0000) + 
					 		  ((metaData[meta_i + Common.member_keyvalue_ext_next_offx + 2] << 8) & 0x0000FF00) + 
					 		  (metaData[meta_i + Common.member_keyvalue_ext_next_offx + 3] & 0x000000FF);
				} else {
					keepGoing = false;
					continue;
				}
			}
			
			elementType = metaData[meta_i];
			
			if (elementType == Common.empty) {
				k = numElements;
				continue;
			}
			
			if (k != 0)
				txt[i++] = ',';
			
			if (pretty) {
				if (indentLength + 20 > len - i || i >= len) 
					increaseJsonBuffer(indentLength + 20);
				
				if (k != 0)
					txt[i++] = '\n';
				
				for (int ind=0; ind<indentLength; ind++)
					txt[i++] = ' ';
			}
				
			valLoc = ((metaData[meta_i + Common.member_keyvalue_offx] << 24) & 0xFF000000) + 
			 		  ((metaData[meta_i + Common.member_keyvalue_offx + 1] << 16) & 0x00FF0000) + 
			 		  ((metaData[meta_i + Common.member_keyvalue_offx + 2] << 8) & 0x0000FF00) + 
			 		  (metaData[meta_i + Common.member_keyvalue_offx + 3] & 0x000000FF);
			
			meta_i += Common.member_sz;
			
			if (elementType == Common.string) {
				valLen = strLen(data, valLoc);
				
				if (valLen + 20 > len - i || i >= len) 
					increaseJsonBuffer(valLen + 20);
				
				txt[i++] = '"';
				
				for (int indx=0; indx<valLen; indx++) 
					txt[i++] = data[valLoc + indx];
				
				i += valLen;
				txt[i++] = '"';
			} else if (elementType == Common.numeric_long || elementType == Common.numeric_double) {
				
				if (!convertNumberics) {
					valLen = strLen(data, valLoc);
					
					if (valLen + 20 > len - i || i >= len) 
						increaseJsonBuffer(valLen + 20);
					
					for (int indx=0; indx<valLen; indx++)
						txt[i++] = data[valLoc + indx];
					
					i += valLen;
				} else { 
					if (64 > len - i || i >= len) 
						increaseJsonBuffer(64);
						
					
					if (elementType == Common.numeric_long) {
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
						
						for (++numCvtBuf_i; numCvtBuf_i < 48; numCvtBuf_i++)
							txt[i++] = numCvtBuf[numCvtBuf_i];

					} else {
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
						}
					}
				}
			} else if (elementType == Common.bool) {
				if (64 > len - i || i >= len) 
					increaseJsonBuffer(64);
				
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
				if (64 > len - i || i >= len) 
					increaseJsonBuffer(64);
				
				txt[i++] = 't';
				txt[i++] = 'r';
				txt[i++] = 'u';
				txt[i++] = 'e';
			} else if (elementType == Common.bool_false) {
				if (64 > len - i || i >= len) 
					increaseJsonBuffer(64);
				
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
				if (64 > len - i || i >= len) 
					increaseJsonBuffer(64);
				
				txt[i++] = 'n';
				txt[i++] = 'u';
				txt[i++] = 'l';
				txt[i++] = 'l';
			} 
			
			k++;
		}
		
		if (pretty) {
			if (indentLength + 20 > len - i || i >= len) 
				increaseJsonBuffer(indentLength + 20);
			
			txt[i++] = '\n';
			indentLength -= 2;
			
			for (int ind=0; ind<indentLength; ind++)
				txt[i++] = ' ';
			
			txt[i++] = ']';
		} else {
			txt[i++] = ']';
		}
	}

	
	
	/*
	 * Increase the byte buffer used for json
	 * 
	 * @param szToAdd - bytes to increase
	 */
	private void increaseJsonBuffer(int szToAdd) {
		len += (szToAdd + (int)(len * 0.25));
		byte temp[] = txt;
		txt = new byte[len];

		for (int i_ = 0; i_ < i; i_++)
			txt[i_] = temp[i_];

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
	
		
	
	public void crap() {
		System.out.println("Meta:\n" + new String(metaData));
		System.out.println("Data:\n" + new String(data));
	}
}
