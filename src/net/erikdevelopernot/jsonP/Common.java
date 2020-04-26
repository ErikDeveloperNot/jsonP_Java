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

	
}
