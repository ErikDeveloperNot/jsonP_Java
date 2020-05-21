package net.erikdevelopernot.jsonP.document;

import java.util.StringTokenizer;

import net.erikdevelopernot.jsonP.Common;
import net.erikdevelopernot.jsonP.Common.ExtDetail;
import net.erikdevelopernot.jsonP.JsonPException;


public class JsonPJson {

	private boolean useJson;
	private byte[] data;
	private byte[] metaData;
	private int dataLength;
	private int dataIndx;
	private int metaLength;
	private int metaIndx;
	private boolean metaEqData;
	private int docRoot;
	
	//options
	private boolean dontSortKeys;
	private boolean weakRef;
	private boolean convertNumberics;
	
	//used when getting a meta-slot
	private boolean isExt;
	
	//used by private getObjectId
	private int parentContainerID;
	
	//Used for searches that need access to ext information
	private ExtDetail extDetail;
	
	//Used to hold status of last error
	private ErrorCode errorCode;
	
	//variables used for stringify
	private int len;
	private int i;
	private byte txt[];
	private byte numCvtBuf[];
	private short numCvtBuf_i;
	private boolean pretty;
	private int indentLength;
	
	private static final int DOUBLE_SIGNIFICANT = 7;
	private static final int DEFAULT_DOUBLE_PRECISION = 8;
	private int doublePrecision;
	

	public enum ErrorCode { ERROR, NOT_FOUND, INVALID_TYPE };
	
	/*
	 * Constructors 
	 */
	// called by parser
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
			numCvtBuf = new byte[512];
		}
		
		if (data == metaData) {
			metaEqData = true;
		} else {
			metaEqData = false;
		}
		
		metaIndx = metaLength;
		dataIndx = dataLength;
		
		extDetail = new Common.ExtDetail();
	}
	
	// called by parser
	public JsonPJson(byte[] data, int dataLength, int docRoot, int options) {
		this(data, data, dataLength, dataLength, docRoot, options);
		useJson = false;
	}
	
	/*
	 * called by user to create json doc
	 * 
	 * @param type - either ElementType.OBJECT or ElementType.ARRAY
	 * @param numElements - number of child elements
	 * @param bufSize - initial buffer size
	 * @param options - either DONT_SORT_KEYS or CONVERT_NUMERICS
	 */
	public JsonPJson(byte type, int numElements, int bufSz, int options) throws JsonPException {
		if (numElements < 1)
			numElements = 1;
			
		if (bufSz < (Common.object_member_sz*5 + Common.root_sz + numElements*Common.object_member_sz))
			bufSz = Common.object_member_sz*5 + Common.root_sz + numElements*Common.object_member_sz;
			
		dataLength = bufSz;
		data = new byte[dataLength];
		dataIndx = 0;
		metaData = data;
		metaLength = dataLength;
		metaIndx = dataIndx;
		metaEqData = true;
byte elementSize;
		
		if (type == Common.object) {
			metaData[metaIndx] = Common.object;
			metaIndx += Common.object_member_sz;
			elementSize = Common.object_member_sz;
		} else if(type == Common.array) {
			metaData[metaIndx] = Common.array;
			metaIndx += Common.array_member_sz;
			elementSize = Common.array_member_sz;
		} else {
			throw new JsonPException("Error creating json, type must be 'object' or 'array'");
		}
		
//		metaIndx += Common.object_member_sz;

		metaData[metaIndx] = (byte)((numElements >> 24) & 0xFF);
		metaData[metaIndx + 1] = (byte)((numElements >> 16) & 0xFF);
		metaData[metaIndx + 2] = (byte)((numElements >> 8) & 0xFF);
		metaData[metaIndx + 3] = (byte)(numElements & 0xFF); 
		
		metaIndx += Common.root_sz;
		docRoot = 0;
		
		for (int i=0; i<numElements; i++, metaIndx += elementSize) {
			metaData[metaIndx] = Common.empty;
		}
		
		metaData[metaIndx] = Common.extended;
		
		//just use object_member_key_offx for either object or array ere
		metaData[metaIndx + Common.object_member_key_offx] = 0;
		metaData[metaIndx + Common.object_member_key_offx + 1] = 0;
		metaData[metaIndx + Common.object_member_key_offx + 2] = 0;
		metaData[metaIndx + Common.object_member_key_offx + 3] = 0;

		metaIndx += elementSize;
		
//		getNextArrayBuf = new byte[8 * 2];
		dontSortKeys = ((options & Common.DONT_SORT_KEYS) > 0) ? true : false;
		weakRef = ((options & Common.WEAK_REF) > 0) ? true : false;
		convertNumberics = ((options & Common.CONVERT_NUMERICS) > 0) ? true : false;
		
		if (metaEqData)
			dataIndx = metaIndx;
		
		if (convertNumberics)
			numCvtBuf = new byte[512];
		
		extDetail = new Common.ExtDetail();
	}
	
	
	
	
	
	
	/*********************
	 *  manipulate methods
	 *********************/
	
	/*
	 * Add Object ot Array Container to an existing container
	 * 
	 * @param path json path to add the new container into
	 * @param delim - path delim
	 * @param key - key name if parent container is an object, otherwise null for an array
	 * @param elementCnt - number of elements the container will hold
	 * @param type - Container type (object/array)
	 * 
	 * @returns new container id
	 */
	public int addContainer(String path, String delim, String key, int elementCnt, byte type) throws JsonPException {
		int parentId = getObjectId(path, delim);
		
		return addContainer(key, elementCnt, parentId, type);
	}

	/*
	 * Add Object ot Array Container to an existing container
	 * 
	 * @param key - Key name if adding to an Object container, null for array parent container
	 * @param elementCnt - number of elements the container will hold
	 * @param parentId - parent container id
	 * @param type - Container type (object/array)
	 * 
	 * @returns new container id
	 */
	public int addContainer(String key, int elementCnt, int parentId, byte type) throws JsonPException {
		byte parentType = metaData[parentId];
		
		if (parentType != Common.object && parentType != Common.array)
			throw new JsonPException("object_id is not an object or an array");
		if (type != Common.object && type != Common.array)
			throw new JsonPException("container type is not an object or array");
			
		if (elementCnt < 1)
			elementCnt = 1;
		
		int elementSize = (parentType == Common.object) ? Common.object_member_sz : Common.array_member_sz;
			
		//get a key slot from the parent object
//		int metaSlot = getMetaSlot((parentId + Common.member_sz), type);
		int metaSlot = getMetaSlot((parentId + elementSize), parentType);
			
		//copy the new objects keys to the data buffer if parent container is an object
		int keyLoc = dataIndx;
		int hash = 0;
		
		if (parentType == Common.object) {
			byte[] keyBytes = key.getBytes();
			increaseData(keyBytes.length + 4);

			for (int i=0; i<keyBytes.length; i++) {
				data[dataIndx++] = keyBytes[i];
				hash = 31 * hash + keyBytes[i];
			}
			
			data[dataIndx++] = '\0';
			
			if (metaEqData)
				metaIndx = dataIndx;
		}
		
		
		//start to create the new object block and set key to the data location from previous step
		increaseMetaData((5 + elementCnt) * elementSize);
		int idToReturn = metaIndx;
		metaData[metaIndx++] = type;

		if (parentType == Common.object) {
			metaData[metaIndx++] = (byte)((keyLoc >>> 24) & 0xFF);
			metaData[metaIndx++] = (byte)((keyLoc >>> 16) & 0xFF);
			metaData[metaIndx++] = (byte)((keyLoc >>> 8) & 0xFF);
			metaData[metaIndx++] = (byte)(keyLoc & 0xFF);

//metaIndx += 4;	//move past hash field
metaData[metaIndx++] = (byte)((hash >>> 24) & 0xFF);
metaData[metaIndx++] = (byte)((hash >>> 16) & 0xFF);
metaData[metaIndx++] = (byte)((hash >>> 8) & 0xFF);
metaData[metaIndx++] = (byte)(hash & 0xFF);
		} else {
			metaData[metaIndx++] = 0;
			metaData[metaIndx++] = 0;
			metaData[metaIndx++] = 0;
			metaData[metaIndx++] = 0;
		}
		
		//assign the parent object key a type of pointer and point to the start of the new block
		if (type == Common.object)
			metaData[metaSlot] = Common.object_ptr;
		else
			metaData[metaSlot] = Common.array_ptr;
		
		metaData[metaSlot+1] = (byte)(((idToReturn + 1) >>> 24) & 0xFF);
		metaData[metaSlot+2] = (byte)(((idToReturn + 1) >>> 16) & 0xFF);
		metaData[metaSlot+3] = (byte)(((idToReturn + 1) >>> 8) & 0xFF);
		metaData[metaSlot+4] = (byte)((idToReturn + 1) & 0xFF);
		
		//set number of keys for new object or num elements for an array
		metaData[metaIndx++] = (byte)((elementCnt >>> 24) & 0xFF);
		metaData[metaIndx++] = (byte)((elementCnt >>> 16) & 0xFF);
		metaData[metaIndx++] = (byte)((elementCnt >>> 8) & 0xFF);
		metaData[metaIndx++] = (byte)(elementCnt & 0xFF);
		
		for (int i=0; i<elementCnt; i++, metaIndx += elementSize) {
			metaData[metaIndx] = Common.empty;
		}
		
		metaData[metaIndx++] = Common.extended;
		
		metaData[metaIndx++] = 0;
		metaData[metaIndx++] = 0;
		metaData[metaIndx++] = 0;
		metaData[metaIndx++] = 0;

		if (!isExt && parentType == Common.object && !dontSortKeys) {
			int numKeys = ((metaData[parentId + Common.object_member_sz] << 24) & 0xFF000000) |
						  ((metaData[parentId + Common.object_member_sz + 1] << 16) & 0x00FF0000) |
						  ((metaData[parentId + Common.object_member_sz + 2] << 8) & 0x0000FF00) |
						  (metaData[parentId + Common.object_member_sz + 3] & 0x000000FF);
			
			parentId += (Common.root_sz + Common.object_member_sz);
			Common.sortKeys(parentId, (parentId + (numKeys-1) * Common.object_member_sz), data, metaData, metaData);
		}
		
		if (metaEqData)
			dataIndx = metaIndx;

		return idToReturn;
		
	}

	
	
	
	/*
	 * Add value type
	 * 
	 * @param type - element type
	 * @param parentId - id of the parent container
	 * @param key - key name if parent is object, null if parent is an array
	 * @value - element value (String, Double, Long, null)
	 * 
	 * returns elements id
	 */
	public int addValueType(String path, String delim, String key, byte type, Object value) throws JsonPException {
		int parentId = getObjectId(path, delim);

		if (parentId >= 0) {
			return addValueType(type, parentId, key, value);
		} else {
			errorCode = ErrorCode.NOT_FOUND;
			return -1;
		}
	}
	
	
	
	/*
	 * Add value type
	 * 
	 * @param type - element type
	 * @param parentId - id of the parent container
	 * @param key - key name if parent is object, null if parent is an array
	 * @value - element value (String, Double, Long, null)
	 * 
	 * returns elements id
	 */
	public int addValueType(byte type, int parentId, String key, Object value) throws JsonPException {
		byte containerType = metaData[parentId];
		int elementSize = (containerType == Common.object) ? Common.object_member_sz : Common.array_member_sz;

		if (containerType != Common.object && containerType != Common.array) {
			throw new JsonPException("parentId is not an object or and array");
		}
			
		int valLen = 8;
		
/*
 * !!!!!!!!!!1 Revisit conversion
 */
		if (type == Common.string) {
			if (value.getClass() != String.class)
				throw new JsonPException("value not of type String");
			
			value = ((String)value).getBytes();
			valLen = ((byte[])value).length;
		} else if (type == Common.numeric_long && !convertNumberics) {
			if (value.getClass() != Long.class)
				throw new JsonPException("value not of type Long");
			
			value = ((Long)value).toString().getBytes();
			valLen = ((byte[])value).length;
		} else if (type == Common.numeric_double && !convertNumberics) {
			if (value.getClass() != Double.class)
				throw new JsonPException("value not of type Double");
		
			value = ((Double)value).toString().getBytes();
			valLen = ((byte[])value).length;
		} 

		isExt = true;
		int metaSlot = getMetaSlot(parentId + elementSize, containerType);

		if (containerType == Common.object) {
			byte[] keyBytes = key.getBytes();
			int hash = 0;
			
			increaseData(keyBytes.length + valLen + 8);
			
			metaData[metaSlot] = type;
			metaData[metaSlot + Common.object_member_key_offx] = (byte)((dataIndx >> 24) & 0xFF);
			metaData[metaSlot + Common.object_member_key_offx + 1] = (byte)((dataIndx >> 16) & 0xFF);
			metaData[metaSlot + Common.object_member_key_offx + 2] = (byte)((dataIndx >> 8) & 0xFF);
			metaData[metaSlot + Common.object_member_key_offx + 3] = (byte)(dataIndx & 0xFF);
			
			for (int i=0; i<keyBytes.length; i++) {
				data[dataIndx++] = keyBytes[i];
				hash = 31 * hash + keyBytes[i];
			}
			
			data[dataIndx++] = '\0';
			
metaData[metaSlot + Common.object_member_hash_offx] = (byte)((hash >> 24) & 0xFF);
metaData[metaSlot + Common.object_member_hash_offx + 1] = (byte)((hash >> 16) & 0xFF);
metaData[metaSlot + Common.object_member_hash_offx + 2] = (byte)((hash >> 8) & 0xFF);
metaData[metaSlot + Common.object_member_hash_offx + 3] = (byte)(hash & 0xFF);
		} else {
			increaseData(valLen + 4);
			
			metaData[metaSlot] = type;
			metaData[metaSlot + Common.array_member_value_offx] = (byte)((dataIndx >> 24) & 0xFF);
			metaData[metaSlot + Common.array_member_value_offx + 1] = (byte)((dataIndx >> 16) & 0xFF);
			metaData[metaSlot + Common.array_member_value_offx + 2] = (byte)((dataIndx >> 8) & 0xFF);
			metaData[metaSlot + Common.array_member_value_offx + 3] = (byte)(dataIndx & 0xFF);
		}

		if (convertNumberics && type == Common.numeric_double) { //(e_type == numeric_double || e_type == numeric_double_cvt)) {
			long d = Double.doubleToLongBits((Double)value);
			data[dataIndx++] = (byte)(0xFF & ((long)d >>> 56));
			data[dataIndx++] = (byte)(0xFF & ((long)d >>> 48));
			data[dataIndx++] = (byte)(0xFF & ((long)d >>> 40));
			data[dataIndx++] = (byte)(0xFF & ((long)d >>> 32));
			data[dataIndx++] = (byte)(0xFF & ((long)d >>> 24));
			data[dataIndx++] = (byte)(0xFF & ((long)d >>> 16));
			data[dataIndx++] = (byte)(0xFF & ((long)d >>> 8));
			data[dataIndx++] = (byte)(0xFF & (long)d);
			
		} else if (convertNumberics && type == Common.numeric_long) { //(e_type == numeric_long || e_type == numeric_long_cvt)) {
			data[dataIndx++] = (byte)(0xFF & ((long)value >>> 56));
			data[dataIndx++] = (byte)(0xFF & ((long)value >>> 48));
			data[dataIndx++] = (byte)(0xFF & ((long)value >>> 40));
			data[dataIndx++] = (byte)(0xFF & ((long)value >>> 32));
			data[dataIndx++] = (byte)(0xFF & ((long)value >>> 24));
			data[dataIndx++] = (byte)(0xFF & ((long)value >>> 16));
			data[dataIndx++] = (byte)(0xFF & ((long)value >>> 8));
			data[dataIndx++] = (byte)(0xFF & (long)value);
			
		} else if (type == Common.string || type == Common.numeric_double || type == Common.numeric_long) {
			for (int i=0; i<valLen; i++)
				data[dataIndx++] = ((byte[])value)[i];
			
			data[dataIndx++] = '\0';
		} 
// !!!!!!!!!!!! TODO
		if (!isExt && containerType == Common.object && !dontSortKeys) {

			int numKeys = ((metaData[parentId + elementSize] << 24) & 0xFF000000) |
						  ((metaData[parentId + elementSize + 1] << 16) & 0x00FF0000) |
						  ((metaData[parentId + elementSize + 2] << 8) & 0x0000FF00) |
						  ((metaData[parentId + elementSize + 3]) & 0x000000FF);
			
			parentId += (Common.root_sz + elementSize);
			
			Common.sortKeys(parentId, (parentId + (numKeys-1) * elementSize), data, metaData, metaData);
		}
		
		if (metaEqData)
			metaIndx = dataIndx;
		
		return metaSlot;
	}

	
	
	/*
	 * update an existing Non container elements values
	 * 
	 * @param path - json path to element
	 * @param delim - paths delim
	 * @param type - new elements types
	 * @param value - Java type (String for string, Double for numeric_double, Long for Numberic_long, null for the rest)
	 * 
	 * returns 1=success, -1=error (sets errorCode)
	 */
	public int updateValue(String path, String delim, byte type, Object value) {
		int id = getObjectId(path, delim, true, true, false);
		
		return updateValue(id, type, value);
	}
	
//TODO - update Value only works for object elements, not array, make like addValue
	
	/*
	 * update an existing Non container elements values
	 * 
	 * @param id - element id to update
	 * @param type - new elements types
	 * @param value - Java type (String for string, Double for numeric_double, Long for Numberic_long, null for the rest)
	 * 
	 * returns 1=success, -1=error (sets errorCode)
	 */
	public int updateValue(int id, byte type, Object value) {
		
		if (id >= 0) {
			
			int keyLoc = ((metaData[id + Common.object_member_key_offx] << 24) & 0xFF000000) |
					((metaData[id + Common.object_member_key_offx + 1] << 16) & 0x00FF0000) |
					((metaData[id + Common.object_member_key_offx + 2] << 8) & 0x0000FF00) |
					((metaData[id + Common.object_member_key_offx + 3]) & 0x000000FF);
			
			int valLoc = strLen(data, keyLoc) + 1 + keyLoc;
			int valLen;
			
			if (metaData[id] == Common.string || ( (metaData[id] == Common.numeric_long || metaData[id] == Common.numeric_double) && !convertNumberics) ) {
				valLen = strLen(data, valLoc);
			} else {
				valLen = 0;
			}
			
			if (type == Common.string || ( (type == Common.numeric_long || type == Common.numeric_double) && !convertNumberics) ) {
				byte[] bytes;
				
				if (value instanceof String && type == Common.string) {
					bytes = ((String) value).getBytes();
					metaData[id] = Common.string;
				} else if (value instanceof Long && type == Common.numeric_long) {
					bytes = ((Long) value).toString().getBytes();
					metaData[id] = Common.numeric_long;
				} else if (value instanceof Double && type == Common.numeric_double) {
					bytes = ((Double) value).toString().getBytes();
					metaData[id] = Common.numeric_double;
				} else {
					errorCode = ErrorCode.INVALID_TYPE;
					return -1;
				}

				if (valLen >= bytes.length) {
					//reuse space
					for (int i=0; i<bytes.length; i++)
						data[valLoc++] = bytes[i];

					data[valLoc] = '\0';
				} else {
					//Copy key to new location and add value
					increaseData(valLoc - keyLoc + bytes.length + 5);

					//point to new key location
					metaData[id + Common.object_member_key_offx] = (byte)(dataIndx >>> 24);
					metaData[id + Common.object_member_key_offx + 1] = (byte)(dataIndx >>> 16);
					metaData[id + Common.object_member_key_offx + 2] = (byte)(dataIndx >>> 8);
					metaData[id + Common.object_member_key_offx + 3] = (byte)(dataIndx);

					do {
						data[dataIndx++] = data[keyLoc++];
					} while (data[keyLoc] != '\0');

					data[dataIndx++] = '\0';

					for (int i=0; i<bytes.length; i++) {
						data[dataIndx++] = bytes[i];
					}

					data[dataIndx++] = '\0';

					if (metaEqData)
						metaIndx = dataIndx;
				}
			} else if (type == Common.numeric_double) {
				if (value instanceof Double) {
					metaData[id] = Common.numeric_double;
					long d = Double.doubleToLongBits((Double)value);
					
					data[valLoc++] = (byte)(0xFF & ((long)d >>> 56));
					data[valLoc++] = (byte)(0xFF & ((long)d >>> 48));
					data[valLoc++] = (byte)(0xFF & ((long)d >>> 40));
					data[valLoc++] = (byte)(0xFF & ((long)d >>> 32));
					data[valLoc++] = (byte)(0xFF & ((long)d >>> 24));
					data[valLoc++] = (byte)(0xFF & ((long)d >>> 16));
					data[valLoc++] = (byte)(0xFF & ((long)d >>> 8));
					data[valLoc++] = (byte)(0xFF & (long)d);
				} else {
					errorCode = ErrorCode.INVALID_TYPE;
					return -1;
				}
			} else if (type == Common.numeric_long) {
				if (value instanceof Long) {
					metaData[id] = Common.numeric_long;
					long d = (Long)value;
					
					data[valLoc++] = (byte)(0xFF & ((long)d >>> 56));
					data[valLoc++] = (byte)(0xFF & ((long)d >>> 48));
					data[valLoc++] = (byte)(0xFF & ((long)d >>> 40));
					data[valLoc++] = (byte)(0xFF & ((long)d >>> 32));
					data[valLoc++] = (byte)(0xFF & ((long)d >>> 24));
					data[valLoc++] = (byte)(0xFF & ((long)d >>> 16));
					data[valLoc++] = (byte)(0xFF & ((long)d >>> 8));
					data[valLoc++] = (byte)(0xFF & (long)d);
				} else {
					errorCode = ErrorCode.INVALID_TYPE;
					return -1;
				}
			} else if (type == Common.bool_true) {
				metaData[id] = Common.bool_true;
			} else if (type == Common.bool_false) {
				metaData[id] = Common.bool_false;
			} else if (type == Common.nil) {
				metaData[id] = Common.nil;
			} else {
				errorCode = ErrorCode.INVALID_TYPE;
				return -1;
			}
		} else {
			errorCode = ErrorCode.NOT_FOUND;
			return -1;
		}
		
		return 1;
	}


	
	//for delete value it makes it way easier to have full path so no option for object_id for now
	public void deleteValue(String path, String delim) throws JsonPException {
		int id = getObjectId(path, delim, true, true, true);
		
		if (id > 0) {
			if (extDetail.isExt) {
				int nextExtOffxSize = (metaData[parentContainerID] == Common.object) ? Common.object_member_key_ext_next_offx :
					Common.array_member_value_ext_next_offx;
				
				metaData[id] = Common.empty;
				metaData[extDetail.priorElement + nextExtOffxSize] = metaData[id + nextExtOffxSize];
				metaData[extDetail.priorElement + nextExtOffxSize + 1] = metaData[id + nextExtOffxSize + 1];
				metaData[extDetail.priorElement + nextExtOffxSize + 2] = metaData[id + nextExtOffxSize + 2];
				metaData[extDetail.priorElement + nextExtOffxSize + 3] = metaData[id + nextExtOffxSize + 3];
			} else {
				int elementSize = (metaData[parentContainerID] == Common.object) ? Common.object_member_sz : Common.array_member_sz;
				
				metaData[id] = Common.empty;
				metaData[id+1] = 0;
				metaData[id+2] = 0;
				metaData[id+3] = 0;
				metaData[id+4] = 0;
				int numKeys = ((metaData[parentContainerID + elementSize] << 24) & 0xFF000000) |
						((metaData[parentContainerID + elementSize + 1] << 16) & 0x00FF0000) |
						((metaData[parentContainerID + elementSize + 2] << 8) & 0x0000FF00) |
						((metaData[parentContainerID + elementSize + 3]) & 0x000000FF);

				parentContainerID += (elementSize + Common.root_sz);
				Common.sortKeys(parentContainerID, (parentContainerID + (numKeys-1) * elementSize), data, metaData, metaData);
			}
		} else {
			throw new JsonPException("Element not found");
		}
	}

	
	/*
	 * potentially dangerous as data could become corrupt if an id is passed that does not point to an
	 * element but whose meta type still equals the type byte !!!
	 * 
	 * Bad - when keys are sorted the id can change
	 */
//	public void deleteValue(int id, byte type) throws JsonPException {
//		if (metaData[id] == type) {
//			/*
//			 *  as with all the the delete update operation space is lost. Assumption is a
//			 *  json object should not be used as a datastore but rather a short term object or one
//			 *  where edits are not made.
//			 */
//			metaData[id] = Common.empty;
//		} else {
//			throw new JsonPException("The element id passed in is not of the type specified");
//		}
//	}

	
	
	
	
	
	
	
	/*****************
	*  access methods
	*****************/
	
	/*
	 * Return the object id of an element - internal implementation
	 * 
	 * @param path - json path to the element
	 * @param delim - delimiter used in the path
	 * @param retPtr - for objects/arrays return the pointers object id
	 * @param setParentId - set the parent container id in @parentContainerID
	 * @param setExtInfo - if the objectid is part of the ext set set @extDetail
	 * 
	 * returns elements object id or -1 if not found
	 */
	private int getObjectId(String path, String delim, boolean retPtr, boolean setParentId, boolean setExtInfo)
	{
		StringTokenizer toker = new StringTokenizer(path, delim);
		
		if (toker.countTokens() < 1)		//assume this is docroot
			return docRoot;
		
		String tok = toker.nextToken();
		String nextTok = null;
		
		byte type;
		byte parent = metaData[docRoot];
//		int elementSize = (parent == Common.object) ? Common.object_member_sz : Common.array_member_sz;
//		int extNextOffx = (parent == Common.object) ? Common.object_member_key_ext_next_offx : Common.array_member_value_ext_next_offx;
	
		int result = docRoot;
		int start = docRoot + ((parent == Common.object) ? Common.object_member_sz : Common.array_member_sz);

		int numKeys = ((metaData[start] << 24) & 0xFF000000) |
				      ((metaData[start + 1] << 16) & 0x00FF0000) |
				      ((metaData[start + 2] << 8) & 0x0000FF00) |
				      (metaData[start + 3] & 0x000000FF);
		
		start += Common.root_sz;
		
		while (tok != null) {
			if (toker.hasMoreTokens())
				nextTok = toker.nextToken();
			else
				nextTok = null;
	
			if (setParentId)
				parentContainerID = result;
			
			result = -1;

			if (parent == Common.array) {
				int indx;
				
				try {
					indx = Integer.parseInt(tok);
				} catch (NumberFormatException e) {
					return result;
				}

				if (indx < numKeys) {
					if (metaData[start + (indx * Common.array_member_sz)] != Common.empty)
						result = start + (indx * Common.array_member_sz);
					else
						result = -1;
				} else {
					// check if it is in an ext slot
					int i = Common.array_member_value_offx + start + (Common.array_member_sz * numKeys);
					
					int extStart = ((metaData[i] << 24) & 0xFF000000) |
								   ((metaData[i + 1] << 16) & 0x00FF0000) |
								   ((metaData[i + 2] << 8) & 0x0000FF00) |
								   (metaData[i + 3] & 0x000000FF);
	
					int k = numKeys;

					while (extStart > 0) {
						if (k == indx) {
							result = extStart;
							break;
						} else {
							extStart = ((metaData[extStart + Common.array_member_value_ext_next_offx] << 24) & 0xFF000000) |
									   ((metaData[extStart + Common.array_member_value_ext_next_offx + 1] << 16) & 0x00FF0000) |
									   ((metaData[extStart + Common.array_member_value_ext_next_offx + 2] << 8) & 0x0000FF00) |
									   (metaData[extStart + Common.array_member_value_ext_next_offx + 3] & 0x000000FF);
							
							k++;
							result = -1;
						}
					}
				}
			} else {
				if (nextTok == null) {
					if (retPtr) {
						if (setExtInfo) {
							extDetail.isExt = false;
							result = Common.searchKeysByHash(tok.getBytes(), start, 
									(start + (numKeys * Common.object_member_sz) - Common.object_member_sz), 
									metaData, data, true, dontSortKeys, extDetail);
						} else {
							result = Common.searchKeysByHash(tok.getBytes(), start, 
									(start + (numKeys * Common.object_member_sz) - Common.object_member_sz), 
									metaData, data, true, dontSortKeys, null);
						}
					} else {
						result = Common.searchKeysByHash(tok.getBytes(), start, 
								(start + (numKeys * Common.object_member_sz) - Common.object_member_sz), 
								metaData, data, false, dontSortKeys, null);
					}
				} else {
					result = Common.searchKeysByHash(tok.getBytes(), start, 
							(start + (numKeys * Common.object_member_sz) - Common.object_member_sz), 
							metaData, data, false, dontSortKeys, null);
				}
			}
			
			type = metaData[result];
			
			if (result > 0) {
				
				if (type ==  Common.object) { // && i < path_count) {
					
					if (nextTok == null) {
						return result;
					} else {
						start = result + Common.object_member_sz;
						parent = metaData[result];

						numKeys = ((metaData[start] << 24) & 0xFF000000) |
								  ((metaData[start + 1] << 16) & 0x00FF0000) |
								  ((metaData[start + 2] << 8) & 0x0000FF00) |
								  (metaData[start + 3] & 0x000000FF);
						
						start += Common.root_sz;
					}
				} else if (type ==  Common.array) { //_ptr && i < path_count) {
					if (nextTok == null) {
						return result;
					} else {
						parent = metaData[result];
						start = result + Common.array_member_sz;

						numKeys = ((metaData[start] << 24) & 0xFF000000) |
								  ((metaData[start + 1] << 16) & 0x00FF0000) |
								  ((metaData[start + 2] << 8) & 0x0000FF00) |
								  (metaData[start + 3] & 0x000000FF);
						
						start += Common.root_sz;
					}
				} else if (type == Common.object_ptr || type == Common.array_ptr) {
					
					if (!retPtr) {
						result = ((metaData[result + 1] << 24) & 0xFF000000) |
								  ((metaData[result + 2] << 16) & 0x00FF0000) |
								  ((metaData[result + 3] << 8) & 0x0000FF00) |
								  (metaData[result + 4] & 0x000000FF);
						
						result -= 1; 		//above ptr points key offset, so move back to element type 
					}

					if (nextTok == null) {
						return result;
					} else {
						parent = metaData[result];
//						start = result + elementSize;
						if (parent == Common.object_ptr || parent == Common.object)
							start = result + Common.object_member_sz;
						else
							start = result + Common.array_member_sz;

						numKeys = ((metaData[start] << 24) & 0xFF000000) |
								  ((metaData[start + 1] << 16) & 0x00FF0000) |
								  ((metaData[start + 2] << 8) & 0x0000FF00) |
								  (metaData[start + 3] & 0x000000FF);
						
						start += Common.root_sz;
					}
				} else if (nextTok != null) {
					//error trying to get a node below a leaf node
					errorCode = ErrorCode.ERROR;
					return -1;
				} else if (nextTok == null) {
					return result;
				}
			} else {
				errorCode = ErrorCode.ERROR;
				return result;
			}
			
			tok = nextTok;
		}
		
		return result;
	}

	
	/*
	 * Return the object id of an element
	 * 
	 * @param path - json path to the element
	 * @param delim - delimiter used in the path
	 * 
	 * returns elements object id or -1 if not found
	 */
	public int getObjectId(String path, String delim) {
		return getObjectId(path, delim, false, false, false);
	}
	
	
	/*
	 * Get the either the Object or Array root element id of the json model
	 * 
	 * returns json root id
	 */
	public int getDocRoot() { 
		return docRoot; 
	}
	
	
	/*
	 * return the type of an element
	 * 
	 * Common.string
	 * Common.boolean_false
	 * Common.boolean_true
	 * Common.numeric_long
	 * Common.numeric_double
	 * Common.nil
	 * Common.object
	 * Common.array
	 */
	public byte getElementType(int id) {
		return metaData[id];
	}

	
	
	/*
	 * Get the member count for object or array
	 * 
	 * @param path - json path to element
	 * @param delim - delimiter used in the path 
	 * 
	 * returns number of elements
	 */
	public int getMembersCount(String path, String delim) throws JsonPException {
		int id = getObjectId(path, delim);
		
		if (id >= 0)
			return getMembersCount(id);
		else
			throw new JsonPException("Element for path: " + path + ", not found");
	}
		

	/*
	 * Get the member count for object or array
	 * 
	 * @param id - object or array id
	 * 
	 * returns number of elements
	 */
	public int getMembersCount(int elementID) {
		int id = elementID;
		int elementSize = (metaData[elementID] == Common.object) ? Common.object_member_sz : Common.array_member_sz;
		
		if (metaData[id] == Common.object || metaData[id] == Common.array) {
			id += elementSize;
			
			int keyMembers = ((metaData[id++] << 24) & 0xFF000000) | ((metaData[id++] << 16) & 0x00FF0000) |
					  		 ((metaData[id++] << 8) & 0x00FF00) | ((metaData[id++]) & 0x000000FF);
			int slots = keyMembers;
			
			id += (elementSize * keyMembers) + 1;
			
			int ext = ((metaData[id++] << 24) & 0xFF000000) | ((metaData[id++] << 16) & 0x00FF0000) |
					  ((metaData[id++] << 8) & 0x00FF00) | ((metaData[id++]) & 0x000000FF);
	
			if (ext > 0) {
				do {
					keyMembers++;
					ext += elementSize;
					
					ext = ((metaData[ext++] << 24) & 0xFF000000) | ((metaData[ext++] << 16) & 0x00FF0000) |
						  ((metaData[ext++] << 8) & 0x00FF00) | ((metaData[ext++]) & 0x000000FF);
				} while (ext > 0);
			} 

			/*
			 * the only way to get an accurate number is to verify all keys are not empty values
			 * design issue, should add another field in each object/array for current count 
			 */
			if (dontSortKeys) {
				//unsorted, need to check each slot since there can be holes if deletes were done
				id = elementID + elementSize + Common.root_sz;
				
				for (int i=0; i<slots; i++) {
					if (metaData[id] == Common.empty)
						keyMembers--;
					
					id += elementSize;
				}
			} else {
				//sorted keys can start from end since all empty slots should always sort to the end
				id -= (2 * elementSize);
				
				while (metaData[id] == Common.empty && (slots-- > 0)) {
					keyMembers--;
					id -= elementSize;
				}
			}
			
			return keyMembers;
			
		} else {
			errorCode = ErrorCode.INVALID_TYPE;
		}
		
		return -1;
	}
		


	/*
	 * @EntrySet
	 * class to access Json array elements
	 */
	public class EntrySet {
		protected int parentID;
		protected int[] idList;
		protected int idListIndx;
		protected int keys;
		int temp;
		
		EntrySet(int _parentID, int sz) {
			idListIndx = 0;
			idList = new int[sz];
			parentID = _parentID;
			keys = 0;
		}
		
		void addID(int id) {
			idList[idListIndx++] = id;
			
			if (idListIndx >= idList.length) {
				int[] temp_ints = idList;
				idList = new int[(int)(idList.length * 1.5)];
				
				for (int j=0; j<idListIndx; j++) {
					idList[j] = temp_ints[j];
				}
			}
			
			keys++;
		}
		
		public boolean getAsBoolean(int i) throws JsonPException {
			if (i > keys) {
				throw new JsonPException("No KeyEntry for id: " + i + ", number of keys: " + keys);
			}
			
			if (metaData[idList[i]] == Common.bool_false)
				return false;
			else if (metaData[idList[i]] == Common.bool_true)
				return true;
			else
				throw new JsonPException("KeyEntrySet id: " + i + ", is not a boolean, type is: " + metaData[idList[i]]);
		}
		
		
		public String getAsString(int i) throws JsonPException {
			if (i > keys) {
				throw new JsonPException("No KeyEntry for id: " + i + ", number of keys: " + keys);
			}
			
			return getStringValue(metaData[parentID], idList[i]);
		}
		
		
		public long getAsLong(int i) throws JsonPException {
			if (i > keys) {
				throw new JsonPException("No KeyEntry for id: " + i + ", number of keys: " + keys);
			}

			return getLongValue(metaData[parentID], idList[i]);
		}
		
		
		public double getAsDouble(int i) throws JsonPException {
			if (i > keys) {
				throw new JsonPException("No KeyEntry for id: " + i + ", number of keys: " + keys);
			}

			return getDoubleValue(metaData[parentID], idList[i]);
		}
		
		
		public int getAsObject(int i) throws JsonPException {
			if (i > keys) {
				throw new JsonPException("No KeyEntry for id: " + i + ", number of keys: " + keys);
			}
			
			if (metaData[idList[i]] == Common.object) {
				return idList[i];
			} else {
				throw new JsonPException("KeyEntrySet id: " + i + ", is not an object, type is: " + metaData[idList[i]]);
			}
		}
		
		
		public int getAsArray(int i) throws JsonPException {
			if (i > keys) {
				throw new JsonPException("No KeyEntry for id: " + i + ", number of keys: " + keys);
			}
			
			if (metaData[idList[i]] == Common.array) {
				return idList[i];
			} else {
				throw new JsonPException("KeyEntrySet id: " + i + ", is not an array, type is: " + metaData[idList[i]]);
			}
		}
		
		
		public byte getElementType(int i) {
			return metaData[idList[i]];
		}
		
		
		public int getNumberOfElements() {
			return keys;
		}

	}
	

	/*
	 * @KeyEntrySet
	 * class used to access objects keys.values
	 */
	public class KeyEntrySet extends EntrySet {
		private int[] lengths;
		
		KeyEntrySet(int _parentID, int sz) {
			super(_parentID, sz);
			lengths = new int[sz];
		}
		
		void addID(int id) {
			super.addID(id);
			temp = ((metaData[id + 1] << 24) & 0xFF000000) |
			   ((metaData[id + 2] << 16) & 0x00FF0000) |
			   ((metaData[id + 3] << 8) & 0x0000FF00) |
			   (metaData[id + 4] & 0x000000FF);

			id = temp;
			temp = 0;

			while (data[id++] != '\0')
				temp++;

			lengths[idListIndx - 1] = temp;

			if (idList.length > lengths.length) {
				int[] temp_len = lengths;
				lengths = new int[idList.length];
				
				for (int j=0; j<idListIndx; j++) {
					lengths[j] = temp_len[j];
				}
			}
		}
		
		
		public String getKeyName(int i) throws JsonPException {
			if (i > keys) {
				throw new JsonPException("No KeyEntry for id: " + i + ", number of keys: " + keys);
			}
			
			int offSet = ((metaData[idList[i] + 1] << 24) & 0xFF000000) |
					   ((metaData[idList[i] + 2] << 16) & 0x00FF0000) |
					   ((metaData[idList[i] + 3] << 8) & 0x0000FF00) |
					   (metaData[idList[i] + 4] & 0x000000FF);
//System.out.println("Type: " + metaData[idList[i]] + ", " + offSet + " - " + lengths[i]);
			return new String(data, offSet, lengths[i]);
		}
	}
	
	
	/*
	 * Return an object/array element entry set
	 * 
	 * @param elementID - object/array id to get members
	 * 
	 * returns @EntrySet or @KeyEntrySet
	 */
	public EntrySet getContainerElements(int elementID) throws JsonPException {
		if (metaData[elementID] != Common.object && metaData[elementID] != Common.array) {
			throw new JsonPException("Element id: " + elementID + ", is not an object or array, type: " 
									+ Common.getElementType(metaData[elementID]));
		}
		
		int elementSize = (metaData[elementID] == Common.object) ? Common.object_member_sz : Common.array_member_sz;
		
//		objectID; += Common.member_sz;
		int keys = ((metaData[elementID + elementSize] << 24) & 0xFF000000) |
				   ((metaData[elementID + elementSize + 1] << 16) & 0x00FF0000) |
				   ((metaData[elementID + elementSize + 2] << 8) & 0x0000FF00) |
				   (metaData[elementID + elementSize + 3] & 0x000000FF);
		
		EntrySet entrySet;
		
		if (metaData[elementID] == Common.object)
			entrySet = new KeyEntrySet(elementID, keys);
		else
			entrySet = new EntrySet(elementID, keys);
			
		elementID += (elementSize + Common.root_sz);
		int id, i = 0;
		
		while(i++ < keys) {
			if (metaData[elementID] == Common.empty) {
				elementID += elementSize;
				continue;
			}

			id = elementID;
			
			if (metaData[elementID] == Common.array_ptr || metaData[elementID] == Common.object_ptr) {
				id = (((metaData[elementID + 1] << 24) & 0xFF000000) |
						   ((metaData[elementID + 2] << 16) & 0x00FF0000) |
						   ((metaData[elementID + 3] << 8) & 0x0000FF00) |
						   (metaData[elementID + 4] & 0x000000FF)) - 1;
			}
			
			entrySet.addID(id);
			elementID += elementSize;
		}
		
		if (metaData[elementID] == Common.extended) {
			id = (((metaData[elementID + 1] << 24) & 0xFF000000) |
					   ((metaData[elementID + 2] << 16) & 0x00FF0000) |
					   ((metaData[elementID + 3] << 8) & 0x0000FF00) |
					   (metaData[elementID + 4] & 0x000000FF));
			
			while (id > 0) {
				elementID = id;
				
				if (metaData[elementID] == Common.array_ptr || metaData[elementID] == Common.object_ptr) {
					id = (((metaData[elementID + 1] << 24) & 0xFF000000) |
							   ((metaData[elementID + 2] << 16) & 0x00FF0000) |
							   ((metaData[elementID + 3] << 8) & 0x0000FF00) |
							   (metaData[elementID + 4] & 0x000000FF)) - 1;
				}
				
				entrySet.addID(id);
				id = (((metaData[elementID + 5] << 24) & 0xFF000000) |
						   ((metaData[elementID + 6] << 16) & 0x00FF0000) |
						   ((metaData[elementID + 7] << 8) & 0x0000FF00) |
						   (metaData[elementID + 8] & 0x000000FF));
			}
			
		}
		
		
		return entrySet;
	}
	
	
	
	/*
	 * Get the double value of an element
	 * 
	 * @param path - json path
	 * @param delim - path delim
	 * 
	 * returns double value
	 * throws JsonPException
	 */
	public double getDoubleValue(String path, String delim) throws JsonPException {
		int id = getObjectId(path, delim, false, true, false);
		
		if (id > 0) {
			return getDoubleValue(metaData[parentContainerID], id);
		} else {
			throw new JsonPException("Element for path: " + path + ", not found");
		}
	}
	
	/*
	 * Get the double value of an element
	 * 
	 * @param elementID - id of the element
	 * 
	 * returns double value
	 * throws JsonPException
	 */
	public double getDoubleValue(byte containerType, int elementID) throws JsonPException {
		if (metaData[elementID] != Common.numeric_double)
			throw new JsonPException("Element is of type: " + Common.getElementType(metaData[elementID]));
		
		int valLoc = ((metaData[elementID + 1] << 24) & 0xFF000000) |
					 ((metaData[elementID + 2] << 16) & 0x00FF0000) |
					 ((metaData[elementID + 3] << 8) & 0x0000FF00) |
					 (metaData[elementID + 4] & 0x000000FF);
		
		if (containerType == Common.object) {
			while (data[valLoc++] != '\0')
				;
		}	
		
		if (convertNumberics) {
			return Double.longBitsToDouble((((data[valLoc+4] << 24) & 0xFF000000L) |
					((data[valLoc+5] << 16) & 0x00FF0000) |
					((data[valLoc+6] << 8) & 0x0000FF00) |
					(data[valLoc+7] & 0x000000FF)) |
					((((data[valLoc] << 24) & 0xFF000000L) |
							((data[valLoc+1] << 16) & 0x00FF0000) |
							((data[valLoc+2] << 8) & 0x0000FF00) |
							(data[valLoc+3] & 0x000000FF)) << 32));
		} else {
			int sz = valLoc;

			while (data[++sz] != '\0')
				;

			return Double.parseDouble(new String(data, valLoc, sz - valLoc));
		}
	}
	

	
	/*
	 * Get the long value of an element
	 * 
	 * @param path - json path
	 * @param delim - path delim
	 * 
	 * returns long value
	 * throws JsonPException
	 */
	public long getLongValue(String path, String delim) throws JsonPException {
		int id = getObjectId(path, delim, false, true, false);
		
		if (id > 0) {
			return getLongValue(metaData[parentContainerID], id);
		} else {
			throw new JsonPException("Element for path: " + path + ", not found");
		}
	}
	
	/*
	 * Get the long value of an element
	 * 
	 * @param elementID - id of the element
	 * 
	 * returns long value
	 * throws JsonPException
	 */
	public long getLongValue(byte containerType, int elementID) throws JsonPException {
		if (metaData[elementID] != Common.numeric_long)
			throw new JsonPException("Element is of type: " + Common.getElementType(metaData[elementID]));
		
		int valLoc = ((metaData[elementID + 1] << 24) & 0xFF000000) |
					 ((metaData[elementID + 2] << 16) & 0x00FF0000) |
					 ((metaData[elementID + 3] << 8) & 0x0000FF00) |
					 (metaData[elementID + 4] & 0x000000FF);
		
		if (containerType == Common.object) {
			while (data[valLoc++] != '\0')
				;
		}	
		
		if (convertNumberics) {
			return ((((data[valLoc+4] << 24) & 0xFF000000L) |
					((data[valLoc+5] << 16) & 0x00FF0000) |
					((data[valLoc+6] << 8) & 0x0000FF00) |
					(data[valLoc+7] & 0x000000FF)) |
					((((data[valLoc] << 24) & 0xFF000000L) |
					((data[valLoc+1] << 16) & 0x00FF0000) |
					((data[valLoc+2] << 8) & 0x0000FF00) |
					(data[valLoc+3] & 0x000000FF)) << 32));
		} else {
			int sz = valLoc;

			while (data[++sz] != '\0')
				;

			return Long.parseLong(new String(data, valLoc, sz - valLoc));
		}
	}

	
	
	/*
	 * Get the boolean value of an element
	 * 
	 * @param path - json path
	 * @param delim - path delim
	 * 
	 * returns boolean value
	 * throws JsonPException
	 */
	public boolean getBooleanValue(String path, String delim) throws JsonPException {
		int id = getObjectId(path, delim, false, true, false);
		
		if (id > 0) {
			if (metaData[id] == Common.bool_true)
				return true;
			else if (metaData[id] == Common.bool_false)
				return false;
			else
				throw new JsonPException("Element is of type: " + Common.getElementType(metaData[id]));
		} else {
			throw new JsonPException("Element for path: " + path + ", not found");
		}
	}
	
	
	
	public boolean getBooleanValue(int objectID) throws JsonPException {
		if (objectID > 0) {
			if (metaData[objectID] == Common.bool_true)
				return true;
			else if (metaData[objectID] == Common.bool_false)
				return false;
			else
				throw new JsonPException("Element is of type: " + Common.getElementType(metaData[objectID]));
		} else {
			throw new JsonPException("Invalid element id: " + objectID);
		}
	}
	
	
	
	/*
	 * Get the string value of an element
	 * 
	 * @param path - json path
	 * @param delim - path delim
	 * 
	 * returns string value
	 * throws JsonPException
	 */
	public String getStringValue(String path, String delim) throws JsonPException {
		int id = getObjectId(path, delim, false, true, false);
	
		if (id > 0) {
			return getStringValue(metaData[parentContainerID], id);
		} else {
			throw new JsonPException("Element for path: " + path + ", not found");
		}
	}
	
	/*
	 * Get the string value of an element
	 * 
	 * @param elementID - id of the element
	 * 
	 * returns string value
	 * throws JsonPException
	 */
	public String getStringValue(byte containerType, int elementID) throws JsonPException {
		if (metaData[elementID] != Common.string)
			throw new JsonPException("Element is of type: " + Common.getElementType(metaData[elementID]));
		
		int valLoc = ((metaData[elementID + 1] << 24) & 0xFF000000) |
					 ((metaData[elementID + 2] << 16) & 0x00FF0000) |
					 ((metaData[elementID + 3] << 8) & 0x0000FF00) |
					 (metaData[elementID + 4] & 0x000000FF);
		
		if (containerType == Common.object) {
			while (data[valLoc++] != '\0')
				;
		}	
		
		int sz = valLoc;

		while (data[sz] != '\0')
			sz++;

		return new String(data, valLoc, sz - valLoc);
	}

	
	
	/*
	 * Gets an open slot in an array or object container. if not empty slot is available an ext slot will be returned
	 */
	private int getMetaSlot(int start, byte containerType)
	{
		int numKeys = ((metaData[start] << 24) & 0xFF000000) |
				  ((metaData[start + 1] << 16) & 0x00FF0000) |
				  ((metaData[start + 2] << 8) & 0x0000FF00) |
				  ((metaData[start + 3]) & 0x000000FF);
	
		
		start += Common.root_sz;
		isExt = false;
		int elementSize;
		int elementExtSize;
		
		if (containerType == Common.object) {
			elementSize = Common.object_member_sz;
			elementExtSize = Common.object_member_ext_sz;
			//if there is an empty spot return it
			if (metaData[start + ((numKeys-1) * elementSize)] == Common.empty) {
				if (!dontSortKeys) {
					//keys sorted so return and the caller will call sort 
					return (start + ((numKeys-1) * elementSize));
				} else {
					//keys not sorts find first empty
					for (int i=0; i<numKeys; i++) {
						if (metaData[start + (i*1) * elementSize] == Common.empty) {
							return start + (i*1) * elementSize;
						}
					}
				}
			}
		} else {
			elementSize = Common.array_member_sz;
			elementExtSize = Common.array_member_ext_sz;
			//for array start from front and see if there is an empty slot
			for (int i=0; i<numKeys; i++) {
				if (metaData[start + (i*1) * elementSize] == Common.empty) {
					return start + (i*1) * elementSize;
				}
			}
		}
			
		//if all key spots are used fall back to slow linked list
		isExt = true;
		// make sure there is enough room
		increaseMetaData(elementExtSize);

		
		if (metaData[start + (numKeys * elementSize)] == Common.extended) {
			int end = ((metaData[start + (numKeys * elementSize) + Common.object_member_key_offx] << 24) & 0xFF000000) |
					((metaData[start + (numKeys * elementSize) + Common.object_member_key_offx + 1] << 16) & 0x00FF0000) |
					((metaData[start + (numKeys * elementSize) + Common.object_member_key_offx + 2] << 8) & 0x0000FF00) |
					((metaData[start + (numKeys * elementSize) + Common.object_member_key_offx + 3]) & 0x000000FF);
			

			if (end == 0) {
				// adding first extended key/val
				metaData[start + (numKeys * elementSize) + Common.object_member_key_offx] = (byte)((metaIndx >> 24) & 0xFF);
				metaData[start + (numKeys * elementSize) + Common.object_member_key_offx + 1] = (byte)((metaIndx >> 16) & 0xFF);
				metaData[start + (numKeys * elementSize) + Common.object_member_key_offx + 2] = (byte)((metaIndx >> 8) & 0xFF);
				metaData[start + (numKeys * elementSize) + Common.object_member_key_offx + 3] = (byte)(metaIndx & 0xFF); 
						
				metaData[metaIndx + elementSize] = 0;
				metaData[metaIndx + elementSize + 1] = 0;
				metaData[metaIndx + elementSize + 2] = 0;
				metaData[metaIndx + elementSize + 3] = 0; 
				
				metaIndx += elementExtSize;
				
				if (metaEqData)
					dataIndx = metaIndx;
				
				 return metaIndx - elementExtSize;
			} else {
				// follow list to end
				int check = ((metaData[end + elementSize] << 24) & 0xFF000000) |
						  ((metaData[end + elementSize + 1] << 16) & 0x00FF0000) |
						  ((metaData[end + elementSize + 2] << 8) & 0x0000FF00) |
						  ((metaData[end + elementSize + 3]) & 0x000000FF);
				
				while (check > 0) {
					end = check;
					check = ((metaData[end + elementSize] << 24) & 0xFF000000) |
							  ((metaData[end + elementSize + 1] << 16) & 0x00FF0000) |
							  ((metaData[end + elementSize + 2] << 8) & 0x0000FF00) |
							  ((metaData[end + elementSize + 3]) & 0x000000FF);
				}

				metaData[end + elementSize] = (byte)((metaIndx >> 24) & 0xFF);
				metaData[end + elementSize + 1] = (byte)((metaIndx >> 16) & 0xFF);
				metaData[end + elementSize + 2] = (byte)((metaIndx >> 8) & 0xFF);
				metaData[end + elementSize + 3] = (byte)(metaIndx & 0xFF); 
				
				metaData[metaIndx + elementSize] = 0;
				metaData[metaIndx + elementSize + 1] = 0;
				metaData[metaIndx + elementSize + 2] = 0;
				metaData[metaIndx + elementSize + 3] = 0; 
				
				 metaIndx += elementExtSize;
				 
				 if (metaEqData)
					 dataIndx = metaIndx;
				 
				 return metaIndx - elementExtSize;
			}
		} else {
			System.err.println("SHOULD NEVER see this get_meta_slot");
		}
		
		return 0;
	}

	
	
	
	
	
	
	/*
	 * Zero whitespace text representation of the Json
	 * 
	 * @param precisionIgnoreForNow deciaml preceision for real numbers
	 * 
	 * returns byte[]
	 */
	public byte[] stringify(int precision, boolean pretty) {
		this.pretty = pretty;
		indentLength = 0;
		len = dataLength/2;
		i = 0;
		int meta_i;  // = docRoot + Common.member_sz;
		txt = new byte[len];
		
		if (precision > 0 && precision < 18)
			doublePrecision = precision;
		else
			doublePrecision = DEFAULT_DOUBLE_PRECISION;
		
		if (metaData[docRoot] == Common.object) {
			meta_i = docRoot + Common.object_member_sz;
			parseObject(meta_i);
		} else if (metaData[docRoot] == Common.array) {
			meta_i = docRoot + Common.array_member_sz;
			parseArray(meta_i);
		}

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
		
		int extLoc = ((metaData[meta_i + (keyCnt * Common.object_member_sz) + Common.object_member_key_offx] << 24) & 0xFF000000) + 
					 ((metaData[meta_i + 1 + (keyCnt * Common.object_member_sz) + Common.object_member_key_offx] << 16) & 0x00FF0000) + 
					 ((metaData[meta_i + 2 + (keyCnt * Common.object_member_sz) + Common.object_member_key_offx] << 8) & 0x0000FF00) + 
					 (metaData[meta_i + 3 + (keyCnt * Common.object_member_sz) + Common.object_member_key_offx] & 0x000000FF);

		while (j < keyCnt || keepGoing) {
		
			if (j >= keyCnt) {
				if (extLoc > 0) {
					meta_i = extLoc;
					extLoc = ((metaData[meta_i + Common.object_member_key_ext_next_offx] << 24) & 0xFF000000) + 
							 ((metaData[meta_i + 1 + Common.object_member_key_ext_next_offx] << 16) & 0x00FF0000) + 
							 ((metaData[meta_i + 2 + Common.object_member_key_ext_next_offx] << 8) & 0x0000FF00) + 
							 (metaData[meta_i + 3 + Common.object_member_key_ext_next_offx] & 0x000000FF);
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
				
			loc = ((metaData[meta_i + Common.object_member_key_offx] << 24) & 0xFF000000) + 
					 ((metaData[meta_i + 1 + Common.object_member_key_offx] << 16) & 0x00FF0000) + 
					 ((metaData[meta_i + 2 + Common.object_member_key_offx] << 8) & 0x0000FF00) + 
					 (metaData[meta_i + 3 + Common.object_member_key_offx] & 0x000000FF);
			
			keyLoc = loc;
			meta_i += Common.object_member_sz;

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
				
				if (valLen + 100 > len - i || i >= len) 
					increaseJsonBuffer(valLen + 100);
				
				txt[i++] = '"';
				
				for (int indx=0; indx<valLen; indx++) {
					//TODO  //TESTING !!!!!!!!!! - testing failed, issue for bytes 
					if ((Common.parse_flags[0xFF & data[keyLoc + keyLen + 1 + indx]] & 0x20) == 0x20) 				
						txt[i++] = '\\';
					
					txt[i++] = data[keyLoc + keyLen + 1 + indx];
				}
				
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
							numCvtBuf[numCvtBuf_i--] = (byte)(48+modulus);
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
						
						
//						String doubleString = String.valueOf(Double.longBitsToDouble(doubleBits));
//					
//						for (int d=0; d<doubleString.length(); d++) {
//							txt[i++] = (byte)doubleString.charAt(d);
//						}

						i = writeDouble(Double.longBitsToDouble(doubleBits), txt, i);
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
//				loc += 4;
				loc += 8;
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
		
		int extLoc = ((metaData[meta_i + (numElements * Common.array_member_sz) + Common.array_member_value_offx] << 24) & 0xFF000000) + 
		 		  	 ((metaData[meta_i + 1 + (numElements * Common.array_member_sz) + Common.array_member_value_offx] << 16) & 0x00FF0000) + 
		 		  	 ((metaData[meta_i + 2 + (numElements * Common.array_member_sz) + Common.array_member_value_offx] << 8) & 0x0000FF00) + 
		 		  	 (metaData[meta_i + 3 + (numElements * Common.array_member_sz) + Common.array_member_value_offx] & 0x000000FF);

//		txt[i++] = '[';
		
		while (k < numElements || keepGoing) {

			if (k >= numElements) {
				if (extLoc > 0) {
					meta_i = extLoc;
					//assign next ext value
					extLoc = ((metaData[meta_i + Common.array_member_value_ext_next_offx] << 24) & 0xFF000000) + 
					 		  ((metaData[meta_i + Common.array_member_value_ext_next_offx + 1] << 16) & 0x00FF0000) + 
					 		  ((metaData[meta_i + Common.array_member_value_ext_next_offx + 2] << 8) & 0x0000FF00) + 
					 		  (metaData[meta_i + Common.array_member_value_ext_next_offx + 3] & 0x000000FF);
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
				
			valLoc = ((metaData[meta_i + Common.array_member_value_offx] << 24) & 0xFF000000) + 
			 		  ((metaData[meta_i + Common.array_member_value_offx + 1] << 16) & 0x00FF0000) + 
			 		  ((metaData[meta_i + Common.array_member_value_offx + 2] << 8) & 0x0000FF00) + 
			 		  (metaData[meta_i + Common.array_member_value_offx + 3] & 0x000000FF);
			
			meta_i += Common.array_member_sz;
			
			if (elementType == Common.string) {
				valLen = strLen(data, valLoc);
				
				if (valLen + 20 > len - i || i >= len) 
					increaseJsonBuffer(valLen + 20);
				
				txt[i++] = '"';
				
				for (int indx=0; indx<valLen; indx++) {
					if ((Common.parse_flags[data[valLoc + indx]] & 0x20) == 0x20) 				//TESTING !!!!!!!!!!
						txt[i++] = '\\';
					
					txt[i++] = data[valLoc + indx];
				}
				
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
							numCvtBuf[numCvtBuf_i--] = (byte)(48+modulus);
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
				
//						String doubleString = String.valueOf(Double.longBitsToDouble(doubleBits));
//				
//						for (int d=0; d<doubleString.length(); d++) {
//							txt[i++] = (byte)doubleString.charAt(d);
//						}
	
						i = writeDouble(Double.longBitsToDouble(doubleBits), txt, i);
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
//				valLoc += 4;
				valLoc += 8;
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
	private void increaseJsonBuffer(int szToAdd) { long s=System.currentTimeMillis();
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
	
	
	
	private void increaseMetaData(int needed) {
		if (((needed + 20) > (metaLength - metaIndx)) || (metaIndx >= metaLength)) {
			metaLength += (needed + (int)(metaLength));
			byte temp[] = metaData;
			metaData = new byte[metaLength];
			
			for (int i=0; i<temp.length; i++)
				metaData[i] = temp[i];
			
			if (metaEqData) {
				data = metaData;
				dataLength = metaLength;
			}
		}
	}
	
	
	
	private void increaseData(int needed) {
		if (((needed + 20) > (dataLength - dataIndx)) || (dataIndx >= dataLength)) {
			dataLength += (needed + (int)(dataLength));
			byte temp[] = data;
			data = new byte[dataLength];
			
			for (int i=0; i<temp.length; i++)
				data[i] = temp[i];
			
			if (metaEqData) {
				metaData = data;
				metaLength = dataLength;
			}
		}
	}
	
	
	
	private int writeDouble(double num, byte[] txt, int i) { 
		if (num < 0) {
			txt[i++] = '-';
			num *= -1;
		}
		
		double remainder=num, modulus;
		numCvtBuf_i = 511;
		int e = 0;
		int fracLeft = doublePrecision;
		
		if (num < 0.001) {
			do {
				num *= 10;
				e--;
			} while (num < 1);
			
			remainder = num;
		}
		
		do {
			modulus = remainder % 10;
			numCvtBuf[numCvtBuf_i--] = (byte)(modulus+48);
			remainder = remainder / 10;
		} while (remainder >= 1);
		
		++numCvtBuf_i;
		
		if ((512 - numCvtBuf_i) > DOUBLE_SIGNIFICANT) {
			txt[i++] = numCvtBuf[numCvtBuf_i++];
			e = 512 - numCvtBuf_i;
			txt[i++] = '.';
			
			for (int k=1; k < DOUBLE_SIGNIFICANT; k++)
				txt[i++] = numCvtBuf[numCvtBuf_i++];
			
			
			while (fracLeft-- > 0 && numCvtBuf_i < 512) 
				txt[i++] = numCvtBuf[numCvtBuf_i++];
				
		} else {
			while (numCvtBuf_i < 512)
				txt[i++] = numCvtBuf[numCvtBuf_i++];
			
			txt[i++] = '.';
		}
		
		remainder = num % 1;
		
		do { 
			modulus = remainder / .1;
			txt[i++] = (byte)(48 + modulus);
			remainder = modulus % 1;
		} while (fracLeft-- > 0);
		
		
		if (e != 0) {
			txt[i++] = 'E';
			int mod;
			numCvtBuf_i = 511;
			
			if (e < 0) {
				txt[i++] = '-';
				e *= -1;
			}
			
			do {
				mod = e % 10;
				numCvtBuf[numCvtBuf_i--] = (byte)(48 + mod);
				e = e / 10;
			} while (e > 0);
			
			numCvtBuf_i++;
			
			while (numCvtBuf_i < 512)
				txt[i++] = numCvtBuf[numCvtBuf_i++];
			
		}

		return i;
	}
	
	
//	private int getHashForKey(int keyLoc) {
//		int dataLoc = ((metaData[keyLoc + 1] << 24) & 0xFF000000) |
//				 	  ((metaData[keyLoc + 2] << 16) & 0x00FF0000) |
//				 	  ((metaData[keyLoc + 3] << 8) & 0x0000FF00) |
//				 	  (metaData[keyLoc + 4] & 0x000000FF);
//		
//		int hash = 0;
//	
//		while (data[dataLoc] != '\0')
//			hash = 31 * hash + data[dataLoc++];
//	
//		return hash;
//	}
	
	
	/**
	 * @return the errorCode
	 */
	public ErrorCode getErrorCode() {
		return errorCode;
	}

	private void crap() {
		System.out.println("Meta:\n" + new String(metaData));
		System.out.println("Data:\n" + new String(data));
	}
}
