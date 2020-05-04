package net.erikdevelopernot.jsonP;

public class Common {
	
	//parse options
	public static final int PRESERVE_JSON = 0x1;
	public static final int SHRINK_BUFS = PRESERVE_JSON << 1;
	public static final int DONT_SORT_KEYS = PRESERVE_JSON << 2;
	public static final int WEAK_REF = PRESERVE_JSON << 3;
	public static final int CONVERT_NUMERICS = PRESERVE_JSON << 4;
		
	
	//Heap indexes - max size for each segment is 2GB, max allowed size of json =~ 20GB
	public static final byte HEAP_0 = 0;
	public static final byte HEAP_1 = 1;
	public static final byte HEAP_2 = 2;
	public static final byte HEAP_3 = 3;
	public static final byte HEAP_4 = 4;
	public static final byte HEAP_5 = 5;
	public static final byte HEAP_6 = 6;
	public static final byte HEAP_7 = 7;
	public static final byte HEAP_8 = 8;
	public static final byte HEAP_9 = 9;

	//Element types
	public static final byte object_ptr = 1;
	public static final byte object = 2;
	public static final byte string = 3;
	public static final byte numeric_int = 4;
	public static final byte numeric_long = 5;
	public static final byte numeric_double = 6;
	public static final byte array_ptr = 7;
	public static final byte array = 8;
	public static final byte bool = 9;
	public static final byte nil = 10;
	public static final byte extended = 11;
	public static final byte empty = 12;
	public static final byte bool_true = 13;
	public static final byte bool_false = 14;
	public static final byte search = 15;
	public static final byte invalid = 16;
	
//	public static enum ElementType { OBJECT, ARRAY, STRING, NUMERIC_LONG, NUMERIC_DOUBLE, BOOL, NULL };
	
	//size of meta data types (the +1 is for heap/stack index. not used in c++ version)
	public static final int element_type_sz = 1;
	public static final int member_sz = element_type_sz + 4; // + 1;
	public static final int member_ext_sz = member_sz + 4; // + 1;
	public static final int root_sz = 4; // + 1;
	//keyvalue types, in objects offset of key, in array offset of value
	public static final int member_keyvalue_offx = element_type_sz;
	public static final int member_keyvalue_ext_next_offx = member_sz;

	public static final int LONG_DOUBLE_SZ = 8;

	/*
	 * borrowed from Chad Austin, sajson; who borrowed it from Rich Geldreich's Purple JSON parser
	 */
	public static final byte parse_flags[] = {
	//  0    1    2    3    4    5    6    7      8    9    A    B    C    D    E    F
		0,   0,   0,   0,   0,   0,   0,   0,     0,   2,   2,   0,   0,   2,   0,   0, // 0
		0,   0,   0,   0,   0,   0,   0,   0,     0,   0,   0,   0,   0,   0,   0,   0, // 1
		3,   1,   0,   1,   1,   1,   1,   1,     1,   1,   1,   1,   1,   1,   0x11,1, // 2
		0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,  0x11,0x11,1,   1,   1,   1,   1,   1, // 3
		1,   1,   1,   1,   1,   0x11,1,   1,     1,   1,   1,   1,   1,   1,   1,   1, // 4
		1,   1,   1,   1,   1,   1,   1,   1,     1,   1,   1,   1,   0,   1,   1,   1, // 5
		1,   1,   1,   1,   1,   0x11,1,   1,     1,   1,   1,   1,   1,   1,   1,   1, // 6
		1,   1,   1,   1,   1,   1,   1,   1,     1,   1,   1,   1,   1,   1,   1,   1, // 7

	// 128-255
		0,0,0,0,0,0,0,0,  0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,  0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,  0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,  0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,  0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,  0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,  0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,  0,0,0,0,0,0,0,0
	};

	
	/*
	 * Used to compare null terminated strings
	 * simply compares byte value **Assumes length at least 1
	 * 
	 * @param left - start index for left string
	 * @param right - start index for right string
	 * @param data - array holding string
	 * 
	 * returns 0=same, -1=left less, 1=left more
	 */
	public static int strcmp(int left, int right, byte[] data) {
//System.out.println("String cmp, left: " + left + ", right: " + right + "\nleft:");
//for (int i=0; i<3; i++)
//	System.out.print((char)data[left+i] + " ");
//System.out.println("\nright:");
//for (int i=0; i<3; i++)
//	System.out.print((char)data[right + i] + " ");

		/*
		 * Special case if comparing an empty element its key location will be set to 0
		 */
		if (left == 0)
			return 1;
		if (right == 0)
			return -1;
		
		do {
			if (data[left] < data[right])
				return -1;
			else if (data[left] > data[right])
				return 1;
			
			left++;
			right++;
		} while (data[left] != '\0' && data[right] != '\0'); 
		
		if (data[left] == data[right])
			return 0;
		else if (data[left] == '\0')
			return -1;
		else if (data[right] == '\0')
			return 1;
		else if (data[left] < data[right])
			return -1;
		else
			return 1;
	}
	

	
	/*
	 * Used to compare null terminated strings
	 * simply compares byte value **Assumes length at least 1
	 * 
	 * @param left - start index for left string
	 * @param right - start index for right string
	 * @param data - array holding string
	 * 
	 * returns 0=same, -1=left less, 1=left more
	 */
	public static int strcmp(byte[] left, int right, byte[] data) {
//System.out.println("String cmp, left: " + left + ", right: " + right + "\nleft:");
//for (int i=0; i<3; i++)
//	System.out.print((char)data[left+i] + " ");
//System.out.println("\nright:");
//for (int i=0; i<3; i++)
//	System.out.print((char)data[right + i] + " ");
		int i = 0;		//index for left
		
		/*
		 * Special case if comparing an empty element its key location will be set to 0
		 */
		if (right == 0)
			return -1;
		
		do {
			if (left[i] < data[right])
				return -1;
			else if (left[i] > data[right])
				return 1;
			
			i++;
			right++;
		} while (i < left.length && data[right] != '\0'); 
		
		if ((i == left.length) && (data[right] == '\0'))
			return 0;
		else if (i == left.length)
			return -1;
		else if (data[right] == '\0')
			return 1;
		else if (left[i] < data[right]) {
			System.err.println("Didnt think this could be seen, Common.strcmp()");
			return -1;
		} else
			return 1;
	}

	
	
	/*
	 * Used to sort keys 
	 */
	public static void sortKeys(int low, int high, byte[] data, byte[] meta, byte[] stackBuf) {
		int pivot = high;
		int lo = low, hi = high;
		int lowPtr, highPtr, pivotPtr;
		byte typeTemp;

		if ((high-low) / Common.member_sz < 1)
			return;
		
		if ((high-low) / Common.member_sz == 1) {
			lowPtr =  ((stackBuf[low + element_type_sz] << 24) & 0xFF000000) |
		  	 	  	  ((stackBuf[low + element_type_sz + 1] << 16) & 0x00FF0000) |
			 	  	  ((stackBuf[low + element_type_sz + 2] << 8) & 0x0000FF00) |
	  		 	  	  (stackBuf[low + element_type_sz + 3] & 0x000000FF);

			highPtr =  ((stackBuf[high + element_type_sz] << 24) & 0xFF000000) |
					   ((stackBuf[high + element_type_sz + 1] << 16) & 0x00FF0000) |
	  	 	  	   	   ((stackBuf[high + element_type_sz + 2] << 8) & 0x0000FF00) |
	  	 	  	   	   (stackBuf[high + element_type_sz + 3] & 0x000000FF);
			
			if (stackBuf[low] == Common.object_ptr || stackBuf[low] == Common.array_ptr) {
				lowPtr =  ((meta[lowPtr] << 24) & 0xFF000000) |
			  	 	  	  ((meta[lowPtr + 1] << 16) & 0x00FF0000) |
				 	  	  ((meta[lowPtr + 2] << 8) & 0x0000FF00) |
		  		 	  	  (meta[lowPtr + 3] & 0x000000FF);
			}
			
			if (stackBuf[high] == Common.object_ptr || stackBuf[high] == Common.array_ptr) {
				highPtr =  ((meta[highPtr] << 24) & 0xFF000000) |
			  	 	  	  ((meta[highPtr + 1] << 16) & 0x00FF0000) |
				 	  	  ((meta[highPtr + 2] << 8) & 0x0000FF00) |
		  		 	  	  (meta[highPtr + 3] & 0x000000FF);
			}

			if (strcmp(lowPtr, highPtr, data) > 0 ) {
//			if ( (stackBuf[low] == Common.empty) || (strcmp(lowPtr, highPtr, data) > 0 ) ) {
				typeTemp = stackBuf[high];
				stackBuf[high] = stackBuf[low];			//swap element type
				stackBuf[low] = typeTemp;

				//swap elements
				typeTemp = stackBuf[low + element_type_sz];
				stackBuf[low + element_type_sz] = stackBuf[high + element_type_sz];
				stackBuf[high + element_type_sz] = typeTemp;
				typeTemp = stackBuf[low + element_type_sz + 1];
				stackBuf[low + element_type_sz + 1] = stackBuf[high + element_type_sz + 1];
				stackBuf[high + element_type_sz + 1] = typeTemp;
				typeTemp = stackBuf[low + element_type_sz + 2];
				stackBuf[low + element_type_sz + 2] = stackBuf[high + element_type_sz + 2];
				stackBuf[high + element_type_sz + 2] = typeTemp;
				typeTemp = stackBuf[low + element_type_sz + 3];
				stackBuf[low + element_type_sz + 3] = stackBuf[high + element_type_sz + 3];
				stackBuf[high + element_type_sz + 3] = typeTemp;
			}
			
			return;
		}
		
		high -= Common.member_sz;

		//used to hold int pointer to first byte of String in data
		pivotPtr =  ((stackBuf[pivot + element_type_sz] << 24) & 0xFF000000) |
					((stackBuf[pivot + element_type_sz + 1] << 16) & 0x00FF0000) |
					((stackBuf[pivot + element_type_sz + 2] << 8) & 0x0000FF00) |
					(stackBuf[pivot + element_type_sz + 3] & 0x000000FF);
		
		if (stackBuf[pivot] == Common.object_ptr || stackBuf[pivot] == Common.array_ptr) {
			pivotPtr =  ((meta[pivotPtr] << 24) & 0xFF000000) |
		  	 	  	  ((meta[pivotPtr + 1] << 16) & 0x00FF0000) |
			 	  	  ((meta[pivotPtr + 2] << 8) & 0x0000FF00) |
	  		 	  	  (meta[pivotPtr + 3] & 0x000000FF);
		}
		
		
		while (true) {
			lowPtr =  ((stackBuf[low + element_type_sz] << 24) & 0xFF000000) |
	  		  		  ((stackBuf[low + element_type_sz + 1] << 16) & 0x00FF0000) |
	  		  		  ((stackBuf[low + element_type_sz + 2] << 8) & 0x0000FF00) |
	  		  	 	  (stackBuf[low + element_type_sz + 3] & 0x000000FF);

			highPtr =  ((stackBuf[high + element_type_sz] << 24) & 0xFF000000) |
		  		   	   ((stackBuf[high + element_type_sz + 1] << 16) & 0x00FF0000) |
		  		  	   ((stackBuf[high + element_type_sz + 2] << 8) & 0x0000FF00) |
		  	 	  	   (stackBuf[high + element_type_sz + 3] & 0x000000FF);

			if (stackBuf[low] == Common.object_ptr || stackBuf[low] == Common.array_ptr) {
				lowPtr =  ((meta[lowPtr] << 24) & 0xFF000000) |
			  	 	  	  ((meta[lowPtr + 1] << 16) & 0x00FF0000) |
				 	  	  ((meta[lowPtr + 2] << 8) & 0x0000FF00) |
		  		 	  	  (meta[lowPtr + 3] & 0x000000FF);
			}
			
			if (stackBuf[high] == Common.object_ptr || stackBuf[high] == Common.array_ptr) {
				highPtr =  ((meta[highPtr] << 24) & 0xFF000000) |
			  	 	  	  ((meta[highPtr + 1] << 16) & 0x00FF0000) |
				 	  	  ((meta[highPtr + 2] << 8) & 0x0000FF00) |
		  		 	  	  (meta[highPtr + 3] & 0x000000FF);
			}

			while (strcmp(lowPtr, pivotPtr, data) < 0 && low < high) {
//			while ( ((stackBuf[low] != Common.empty) && (strcmp(lowPtr, pivotPtr, data) < 0)) && (low < high) ) {
				low += member_sz;
				lowPtr =  ((stackBuf[low + element_type_sz] << 24) & 0xFF000000) |
	  		  		 	  ((stackBuf[low + element_type_sz + 1] << 16) & 0x00FF0000) |
	  		  		 	  ((stackBuf[low + element_type_sz + 2] << 8) & 0x0000FF00) |
	  		  		 	  (stackBuf[low + element_type_sz + 3] & 0x000000FF);
				
				if (stackBuf[low] == Common.object_ptr || stackBuf[low] == Common.array_ptr) {
					lowPtr =  ((meta[lowPtr] << 24) & 0xFF000000) |
				  	 	  	  ((meta[lowPtr + 1] << 16) & 0x00FF0000) |
					 	  	  ((meta[lowPtr + 2] << 8) & 0x0000FF00) |
			  		 	  	  (meta[lowPtr + 3] & 0x000000FF);
				}
			}
			
			while (strcmp(highPtr, pivotPtr, data) >= 0 && low < high) {
//			while ( ((stackBuf[high] == Common.empty) || (strcmp(highPtr, pivotPtr, data) < 0)) && (low < high) ) {
				high -= member_sz;
				highPtr =  ((stackBuf[high + element_type_sz] << 24) & 0xFF000000) |
	  		  		 	   ((stackBuf[high + element_type_sz + 1] << 16) & 0x00FF0000) |
	  		  		 	   ((stackBuf[high + element_type_sz + 2] << 8) & 0x0000FF00) |
	  		  		 	   (stackBuf[high + element_type_sz + 3] & 0x000000FF);
				
				if (stackBuf[high] == Common.object_ptr || stackBuf[high] == Common.array_ptr) {
					highPtr =  ((meta[highPtr] << 24) & 0xFF000000) |
				  	 	  	  ((meta[highPtr + 1] << 16) & 0x00FF0000) |
					 	  	  ((meta[highPtr + 2] << 8) & 0x0000FF00) |
			  		 	  	  (meta[highPtr + 3] & 0x000000FF);
				}
			}
			
			
			if (low == high) {
				if (strcmp(highPtr, pivotPtr, data) >= 0) {
//				if ( (stackBuf[high] == Common.empty) || (strcmp(highPtr, pivotPtr, data) >= 0) ) {
					typeTemp = stackBuf[high];
					stackBuf[high] = stackBuf[pivot];			//swap element type
					stackBuf[pivot] = typeTemp;					
					
					//swap elements
					typeTemp = stackBuf[pivot + element_type_sz];
					stackBuf[pivot + element_type_sz] = stackBuf[high + element_type_sz];
					stackBuf[high + element_type_sz] = typeTemp;
					typeTemp = stackBuf[pivot + element_type_sz + 1];
					stackBuf[pivot + element_type_sz + 1] = stackBuf[high + element_type_sz + 1];
					stackBuf[high + element_type_sz + 1] = typeTemp;
					typeTemp = stackBuf[pivot + element_type_sz + 2];
					stackBuf[pivot + element_type_sz + 2] = stackBuf[high + element_type_sz + 2];
					stackBuf[high + element_type_sz + 2] = typeTemp;
					typeTemp = stackBuf[pivot + element_type_sz + 3];
					stackBuf[pivot + element_type_sz + 3] = stackBuf[high + element_type_sz + 3];
					stackBuf[high + element_type_sz + 3] = typeTemp;
					
					if (lo == high)
						high += member_sz;
					
					sortKeys(lo, high-member_sz, data, meta, stackBuf);
					sortKeys(high, hi, data, meta, stackBuf);
				} else {
					if (lo == high)
						high += member_sz;
					
					sortKeys(lo, high, data, meta, stackBuf);
					sortKeys(high+member_sz, hi, data, meta, stackBuf);
				}
				
				return;
			}
			
			typeTemp = stackBuf[high];
			stackBuf[high] = stackBuf[low];			//swap element type
			stackBuf[low] = typeTemp;				
			
			//swap elements
			typeTemp = stackBuf[low + element_type_sz];
			stackBuf[low + element_type_sz] = stackBuf[high + element_type_sz];
			stackBuf[high + element_type_sz] = typeTemp;
			typeTemp = stackBuf[low + element_type_sz + 1];
			stackBuf[low + element_type_sz + 1] = stackBuf[high + element_type_sz + 1];
			stackBuf[high + element_type_sz + 1] = typeTemp;
			typeTemp = stackBuf[low + element_type_sz + 2];
			stackBuf[low + element_type_sz + 2] = stackBuf[high + element_type_sz + 2];
			stackBuf[high + element_type_sz + 2] = typeTemp;
			typeTemp = stackBuf[low + element_type_sz + 3];
			stackBuf[low + element_type_sz + 3] = stackBuf[high + element_type_sz + 3];
			stackBuf[high + element_type_sz + 3] = typeTemp;
			
		}
	}
	
	
	/*
	 * B-search for sort, list if not
	 * 
	 * @param key - null terminated array of key to search for
	 * @param start - lower bounds of keys, inclusive
	 * @param end - upper bounds of keys, inclusive
	 * @param meta - metadata
	 * @param data - json txt data
	 * @param retPtr - return pointer type for Array/Object
	 * @param dontSortKeys - are keys sorted
	 * 
	 * returns object id if found or 0 or -1 if not
	 */
	public static int search_keys(byte[] key, int start, int end, byte[] meta, byte[] data, 
											boolean retPtr, boolean dontSortKeys) {
	
		int mid; // = (((end - start) / sizeof(obj_member)) / 2) * sizeof(obj_member) + start;
//		int ext = get_ext_start(meta, end + obj_member_sz);
		int ext = ((meta[Common.member_keyvalue_offx + end + Common.member_sz] << 24) & 0xFF000000) |
				  ((meta[Common.member_keyvalue_offx + end + Common.member_sz + 1] << 16) & 0x00FF0000) |
				  ((meta[Common.member_keyvalue_offx + end + Common.member_sz + 2] << 8) & 0x0000FF00) |
				  (meta[Common.member_keyvalue_offx + end + Common.member_sz + 3] & 0x000000FF);

		
		
		int keyCmp;
		byte type;
		int result;

		if (!dontSortKeys) {
			//keys are sort binary search
			while (start <= end) {
				mid = (int)(((end - start) / Common.member_sz) / 2) * Common.member_sz + start;
				
				type = meta[mid];

				if (type == Common.empty) {
					end = mid - Common.member_sz;
					continue;
				}

//				key_cmp = get_key_location(meta, mid);
				keyCmp = ((meta[mid + Common.member_keyvalue_offx] << 24) & 0xFF000000) |
						  ((meta[mid + Common.member_keyvalue_offx + 1] << 16) & 0x00FF0000) |
						  ((meta[mid + Common.member_keyvalue_offx + 2] << 8) & 0x0000FF00) |
						  (meta[mid + Common.member_keyvalue_offx + 3] & 0x000000FF);

				
				if ( type == object_ptr || type == array_ptr)
					//key_cmp = get_key_location(meta, key_cmp);
//					key_cmp = get_uint_a_indx(meta, key_cmp);
					keyCmp = ((meta[keyCmp] << 24) & 0xFF000000) |
							  ((meta[keyCmp + 1] << 16) & 0x00FF0000) |
							  ((meta[keyCmp + 2] << 8) & 0x0000FF00) |
							  (meta[keyCmp + 3] & 0x000000FF);

				//std::cout << "key_cmp = " << key_cmp << ", char: " << data+key_cmp << std::endl;
//				result = std::strcmp(key, data+key_cmp);
				result = strcmp(key, keyCmp, data);
				
				//std::cout << "start: " << start << ", mid: " << mid << ", end: " << end << ", result: " << result << std::endl;

				if (result == 0) {
					//found
					if ((type == object_ptr || type == array_ptr) && !retPtr) {
						//return get_key_location(meta, mid + obj_member_key_offx) - element_type_sz;
//						return get_key_location(meta, mid) - element_type_sz;
						return ((meta[mid + member_keyvalue_offx] << 24) & 0xFF000000) |
							   ((meta[mid + member_keyvalue_offx + 1] << 16) & 0x00FF0000) |
							   ((meta[mid + member_keyvalue_offx + 2] << 8) & 0x0000FF00) |
							   (meta[mid + member_keyvalue_offx + 3] & 0x000000FF) - 1;
					} else {
						return mid;
					}
				} else if (result < 0) {
					end = mid - member_sz;
				} else {
					start = mid + member_sz;
				}
			}
		} else {
			//keys not sorted go through each
			while (start <= end) {
				type = meta[start];
//				key_cmp = get_key_location(meta, start);
				keyCmp = ((meta[start + member_keyvalue_offx] << 24) & 0xFF000000) |
						   ((meta[start + member_keyvalue_offx + 1] << 16) & 0x00FF0000) |
						   ((meta[start + member_keyvalue_offx + 2] << 8) & 0x0000FF00) |
						   (meta[start + member_keyvalue_offx + 3] & 0x000000FF);

				if ( type == object_ptr || type == array_ptr) {
//					key_cmp = get_uint_a_indx(meta, key_cmp);
					keyCmp = ((meta[keyCmp] << 24) & 0xFF000000) |
							   ((meta[keyCmp + 1] << 16) & 0x00FF0000) |
							   ((meta[keyCmp + 2] << 8) & 0x0000FF00) |
							   (meta[keyCmp + 3] & 0x000000FF);

				}

//				result = std::strcmp(key, data+key_cmp);
				result = strcmp(key, keyCmp, data);

				if (result == 0) {
					//found
					if ((type == object_ptr || type == array_ptr) && !retPtr) {
//						return get_key_location(meta, start) - element_type_sz;
						return ((meta[start + member_keyvalue_offx] << 24) & 0xFF000000) |
								   ((meta[start + member_keyvalue_offx + 1] << 16) & 0x00FF0000) |
								   ((meta[start + member_keyvalue_offx + 2] << 8) & 0x0000FF00) |
								   (meta[start + member_keyvalue_offx + 3] & 0x000000FF) - 1;
					} else {
						return start;
					}
				} else {
					start += member_sz;
				}
			}
		}

		while (ext > 0) {
			type = meta[ext];
			//key_cmp = get_key_location(meta, ext + obj_member_key_offx);
//			key_cmp = get_key_location(meta, ext);
			keyCmp = ((meta[ext + member_keyvalue_offx] << 24) & 0xFF000000) |
					   ((meta[ext + member_keyvalue_offx + 1] << 16) & 0x00FF0000) |
					   ((meta[ext + member_keyvalue_offx + 2] << 8) & 0x0000FF00) |
					   (meta[ext + member_keyvalue_offx + 3] & 0x000000FF);

			if ( type == object_ptr || type == array_ptr) {
//				key_cmp = get_uint_a_indx(meta, key_cmp);
				keyCmp = ((meta[keyCmp] << 24) & 0xFF000000) |
						   ((meta[keyCmp + 1] << 16) & 0x00FF0000) |
						   ((meta[keyCmp + 2] << 8) & 0x0000FF00) |
						   (meta[keyCmp + 3] & 0x000000FF);
			}

//			result = std::strcmp(key, data+key_cmp);
			result = strcmp(key, keyCmp, data);

			if (result == 0) {
				//found
				if ((type == object_ptr || type == array_ptr) && !retPtr) {
					//return get_key_location(meta, ext + obj_member_key_offx) - element_type_sz;
//					return get_key_location(meta, ext) - element_type_sz;
					return ((meta[ext + member_keyvalue_offx] << 24) & 0xFF000000) |
							   ((meta[ext + member_keyvalue_offx + 1] << 16) & 0x00FF0000) |
							   ((meta[ext + member_keyvalue_offx + 2] << 8) & 0x0000FF00) |
							   (meta[ext + member_keyvalue_offx + 3] & 0x000000FF) - 1;
				} else {
					return ext;
				}
			} else {
//				ext = get_ext_next(meta, ext);
				ext = ((meta[ext + member_keyvalue_ext_next_offx] << 24) & 0xFF000000) |
						   ((meta[ext + member_keyvalue_ext_next_offx + 1] << 16) & 0x00FF0000) |
						   ((meta[ext + member_keyvalue_ext_next_offx + 2] << 8) & 0x0000FF00) |
						   (meta[ext + member_keyvalue_ext_next_offx + 3] & 0x000000FF);
			}
		}

		return 0;
	}

}
