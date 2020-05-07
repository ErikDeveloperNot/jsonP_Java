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
	private byte numCvtBuf_i;
	private boolean pretty;
	private int indentLength;
	
	//used for get_next array calls to loop through each element
	private byte[] getNextArrayBuf;
	private int getNextArrayIndx;
	private int getNextArrayMemCnt;
	private int getNextArrayId;
	private int getNextArrayExtNext;
	
	

	private enum TxtType { TXT, TXT_PRETTY };
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
			numCvtBuf = new byte[48];
		}
		
		if (data == metaData)
			metaEqData = true;
		else
			metaEqData = false;
		
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
			
		if (bufSz < (Common.member_sz*5 + Common.root_sz + numElements*Common.member_sz))
			bufSz = Common.member_sz*5 + Common.root_sz + numElements*Common.member_sz;
			
		dataLength = bufSz;
		data = new byte[dataLength];
		dataIndx = 0;
		metaData = data;
		metaLength = dataLength;
		metaIndx = dataIndx;
		metaEqData = true;
		
		if (type == Common.object) {
			metaData[metaIndx] = Common.object;
		} else if(type == Common.array) {
			metaData[metaIndx] = Common.array;
		} else {
			throw new JsonPException("Error creating json, type must be 'object' or 'array'");
		}
		
		metaIndx += Common.member_sz;

		metaData[metaIndx] = (byte)((numElements >> 24) & 0xFF);
		metaData[metaIndx + 1] = (byte)((numElements >> 16) & 0xFF);
		metaData[metaIndx + 2] = (byte)((numElements >> 8) & 0xFF);
		metaData[metaIndx + 3] = (byte)(numElements & 0xFF); 
		
		metaIndx += Common.root_sz;
		docRoot = 0;
		
		for (int i=0; i<numElements; i++, metaIndx += Common.member_sz) {
			metaData[metaIndx] = Common.empty;
		}
		
		metaData[metaIndx] = Common.extended;
		
		metaData[metaIndx + Common.member_keyvalue_offx] = 0;
		metaData[metaIndx + Common.member_keyvalue_offx + 1] = 0;
		metaData[metaIndx + Common.member_keyvalue_offx + 2] = 0;
		metaData[metaIndx + Common.member_keyvalue_offx + 3] = 0;

		metaIndx += Common.member_sz;
		
		getNextArrayBuf = new byte[8 * 2];
		dontSortKeys = ((options & Common.DONT_SORT_KEYS) > 0) ? true : false;
		weakRef = ((options & Common.WEAK_REF) > 0) ? true : false;
		convertNumberics = ((options & Common.CONVERT_NUMERICS) > 0) ? true : false;
		
		if (metaEqData)
			dataIndx = metaIndx;
		
		if (convertNumberics)
			numCvtBuf = new byte[48];
		
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
			
		//get a key slot from the parent object
//		int metaSlot = getMetaSlot((parentId + Common.member_sz), type);
		int metaSlot = getMetaSlot((parentId + Common.member_sz), parentType);
			
		//copy the new objects keys to the data buffer if parent container is an object
		int keyLoc = dataIndx;
		
		if (parentType == Common.object) {
			byte[] keyBytes = key.getBytes();
			increaseData(keyBytes.length + 4);

			for (int i=0; i<keyBytes.length; i++)
				data[dataIndx++] = keyBytes[i];
			
			data[dataIndx++] = '\0';
			
			if (metaEqData)
				metaIndx = dataIndx;
		}
		
		
		//start to create the new object block and set key to the data location from previous step
		increaseMetaData((5 + elementCnt) * Common.member_sz);
		int idToReturn = metaIndx;
		metaData[metaIndx++] = type;

		if (parentType == Common.object) {
			metaData[metaIndx++] = (byte)((keyLoc >>> 24) & 0xFF);
			metaData[metaIndx++] = (byte)((keyLoc >>> 16) & 0xFF);
			metaData[metaIndx++] = (byte)((keyLoc >>> 8) & 0xFF);
			metaData[metaIndx++] = (byte)(keyLoc & 0xFF);
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
		
		for (int i=0; i<elementCnt; i++, metaIndx += Common.member_sz) {
			metaData[metaIndx] = Common.empty;
		}
		
		metaData[metaIndx++] = Common.extended;
		
		metaData[metaIndx++] = 0;
		metaData[metaIndx++] = 0;
		metaData[metaIndx++] = 0;
		metaData[metaIndx++] = 0;

		if (!isExt && parentType == Common.object && !dontSortKeys) {
			int numKeys = ((metaData[parentId + Common.member_sz] << 24) & 0xFF000000) |
						  ((metaData[parentId + Common.member_sz + 1] << 16) & 0x00FF0000) |
						  ((metaData[parentId + Common.member_sz + 2] << 8) & 0x0000FF00) |
						  (metaData[parentId + Common.member_sz + 3] & 0x000000FF);
			
			parentId += (Common.root_sz + Common.member_sz);
			Common.sortKeys(parentId, (parentId + (numKeys-1) * Common.member_sz), data, metaData, metaData);
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
	public int add_value_type(byte type, int parentId, String key, Object value) throws JsonPException {
		byte containerType = metaData[parentId];

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
		int metaSlot = getMetaSlot(parentId + Common.member_sz, containerType);

		if (containerType == Common.object) {
			byte[] keyBytes = key.getBytes();
			
			increaseData(keyBytes.length + valLen + 4);
			
			metaData[metaSlot] = type;
			metaData[metaSlot + Common.member_keyvalue_offx] = (byte)((dataIndx >> 24) & 0xFF);
			metaData[metaSlot + Common.member_keyvalue_offx + 1] = (byte)((dataIndx >> 16) & 0xFF);
			metaData[metaSlot + Common.member_keyvalue_offx + 2] = (byte)((dataIndx >> 8) & 0xFF);
			metaData[metaSlot + Common.member_keyvalue_offx + 3] = (byte)(dataIndx & 0xFF);
			
			for (int i=0; i<keyBytes.length; i++)
				data[dataIndx++] = keyBytes[i];
			
			data[dataIndx++] = '\0';
		} else {
			increaseData(valLen + 4);
			
			metaData[metaSlot] = type;
			metaData[metaSlot + Common.member_keyvalue_offx] = (byte)((dataIndx >> 24) & 0xFF);
			metaData[metaSlot + Common.member_keyvalue_offx + 1] = (byte)((dataIndx >> 16) & 0xFF);
			metaData[metaSlot + Common.member_keyvalue_offx + 2] = (byte)((dataIndx >> 8) & 0xFF);
			metaData[metaSlot + Common.member_keyvalue_offx + 3] = (byte)(dataIndx & 0xFF);
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

			int numKeys = ((metaData[parentId + Common.member_sz] << 24) & 0xFF000000) |
						  ((metaData[parentId + Common.member_sz + 1] << 16) & 0x00FF0000) |
						  ((metaData[parentId + Common.member_sz + 2] << 8) & 0x0000FF00) |
						  ((metaData[parentId + Common.member_sz + 3]) & 0x000000FF);
			
			parentId += (Common.root_sz + Common.member_sz);
			
			Common.sortKeys(parentId, (parentId + (numKeys-1) * Common.member_sz), data, metaData, metaData);
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
		
		if (id > 0) {
			
			int keyLoc = ((metaData[id + Common.member_keyvalue_offx] << 24) & 0xFF000000) |
					((metaData[id + Common.member_keyvalue_offx + 1] << 16) & 0x00FF0000) |
					((metaData[id + Common.member_keyvalue_offx + 2] << 8) & 0x0000FF00) |
					((metaData[id + Common.member_keyvalue_offx + 3]) & 0x000000FF);
			
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
					metaData[id + Common.member_keyvalue_offx] = (byte)(dataIndx >>> 24);
					metaData[id + Common.member_keyvalue_offx + 1] = (byte)(dataIndx >>> 16);
					metaData[id + Common.member_keyvalue_offx + 2] = (byte)(dataIndx >>> 8);
					metaData[id + Common.member_keyvalue_offx + 3] = (byte)(dataIndx);

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
				metaData[id] = Common.empty;
				metaData[extDetail.priorElement + Common.member_keyvalue_ext_next_offx] = 
						metaData[id + Common.member_keyvalue_ext_next_offx];
				metaData[extDetail.priorElement + Common.member_keyvalue_ext_next_offx + 1] = 
						metaData[id + Common.member_keyvalue_ext_next_offx + 1];
				metaData[extDetail.priorElement + Common.member_keyvalue_ext_next_offx + 2] = 
						metaData[id + Common.member_keyvalue_ext_next_offx + 2];
				metaData[extDetail.priorElement + Common.member_keyvalue_ext_next_offx + 3] = 
						metaData[id + Common.member_keyvalue_ext_next_offx + 3];
			} else {
				metaData[id] = Common.empty;
				metaData[id+1] = 0;
				metaData[id+2] = 0;
				metaData[id+3] = 0;
				metaData[id+4] = 0;
				int numKeys = ((metaData[parentContainerID + Common.member_sz] << 24) & 0xFF000000) |
						((metaData[parentContainerID + Common.member_sz + 1] << 16) & 0x00FF0000) |
						((metaData[parentContainerID + Common.member_sz + 2] << 8) & 0x0000FF00) |
						((metaData[parentContainerID + Common.member_sz + 3]) & 0x000000FF);

				parentContainerID += (Common.member_sz + Common.root_sz);
				Common.sortKeys(parentContainerID, (parentContainerID + (numKeys-1) * Common.member_sz), data, metaData, metaData);
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

	
	
	
	
	
	
	
		/*
		 *  access methods
		 */
	
	private int getObjectId(String path, String delim, boolean retPtr, boolean setParentId, boolean setExtInfo)
	{
		StringTokenizer toker = new StringTokenizer(path, delim);
		
		if (toker.countTokens() < 1)
			return -1;
		
		String tok = toker.nextToken();
		String nextTok = null;
	
		int result = docRoot;
		int start = docRoot + Common.member_sz;

		int numKeys = ((metaData[start] << 24) & 0xFF000000) |
				      ((metaData[start + 1] << 16) & 0x00FF0000) |
				      ((metaData[start + 2] << 8) & 0x0000FF00) |
				      (metaData[start + 3] & 0x000000FF);
		
		start += Common.root_sz;
		byte type;
		byte parent = metaData[docRoot];
		boolean isArrayIndx = false;

		while (tok != null) {
			if (toker.hasMoreTokens())
				nextTok = toker.nextToken();
			else
				nextTok = null;
	
			if (setParentId)
				parentContainerID = result;
			
			result = 0;

			if (parent == Common.array) {
				int indx;
				
				try {
					indx = Integer.parseInt(tok);
				} catch (NumberFormatException e) {
					return result;
				}

				if (indx < numKeys) {
					if (metaData[start + (indx * Common.member_sz)] != Common.empty)
						result = start + (indx * Common.member_sz);
					else
						result = 0;
				} else {
					// check if it is in an ext slot
					int i = Common.member_keyvalue_offx + start + (Common.member_sz * numKeys);
					
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
							extStart = ((metaData[extStart + Common.member_keyvalue_ext_next_offx] << 24) & 0xFF000000) |
									   ((metaData[extStart + Common.member_keyvalue_ext_next_offx + 1] << 16) & 0x00FF0000) |
									   ((metaData[extStart + Common.member_keyvalue_ext_next_offx + 2] << 8) & 0x0000FF00) |
									   (metaData[extStart + Common.member_keyvalue_ext_next_offx + 3] & 0x000000FF);
							
							k++;
							result = 0;
						}
					}
				}
			} else {
				if (nextTok == null) {
					if (retPtr) {
						if (setExtInfo) {
							extDetail.isExt = false;
							result = Common.search_keys(tok.getBytes(), start, 
									(start + (numKeys * Common.member_sz) - Common.member_sz), 
									metaData, data, true, dontSortKeys, extDetail);
						} else {
							result = Common.search_keys(tok.getBytes(), start, 
									(start + (numKeys * Common.member_sz) - Common.member_sz), 
									metaData, data, true, dontSortKeys, null);
						}
					} else {
						result = Common.search_keys(tok.getBytes(), start, 
								(start + (numKeys * Common.member_sz) - Common.member_sz), 
								metaData, data, false, dontSortKeys, null);
					}
				} else {
					result = Common.search_keys(tok.getBytes(), start, 
							(start + (numKeys * Common.member_sz) - Common.member_sz), 
							metaData, data, false, dontSortKeys, null);
				}
			}
			
			type = metaData[result];
			
			if (result > 0) {
				
				if (type ==  Common.object) { // && i < path_count) {
					isArrayIndx = false;
					
					if (nextTok == null) {
						return result;
					} else {
						start = result + Common.member_sz;
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
						isArrayIndx = true;
						parent = metaData[result];
						start = result + Common.member_sz;

						numKeys = ((metaData[start] << 24) & 0xFF000000) |
								  ((metaData[start + 1] << 16) & 0x00FF0000) |
								  ((metaData[start + 2] << 8) & 0x0000FF00) |
								  (metaData[start + 3] & 0x000000FF);
						
						start += Common.root_sz;
					}
				} else if (type == Common.object_ptr || type == Common.array_ptr) {
					isArrayIndx = false;
					
					if (!retPtr) {
						result = ((metaData[result] << 24) & 0xFF000000) |
								  ((metaData[result + 1] << 16) & 0x00FF0000) |
								  ((metaData[result + 2] << 8) & 0x0000FF00) |
								  (metaData[result + 3] & 0x000000FF);
						
						result -= 1;
					}

					if (nextTok == null) {
						return result;
					} else {
						parent = metaData[result];
						start = result + Common.member_sz;

						numKeys = ((metaData[start] << 24) & 0xFF000000) |
								  ((metaData[start + 1] << 16) & 0x00FF0000) |
								  ((metaData[start + 2] << 8) & 0x0000FF00) |
								  (metaData[start + 3] & 0x000000FF);
						
						start += Common.root_sz;
					}
				} else if (nextTok != null) {
					//error trying to get a node below a leaf node
					return 0;
				} else if (nextTok == null) {
					return result;
				}
			} else {
				return result;
			}
			
			tok = nextTok;
		}
		
		return result;
	}

	
	
	public int getObjectId(String path, String delim) {
		return getObjectId(path, delim, false, false, false);
	}
	
	
	
	public int getDocRoot() { 
		return docRoot; 
	}
	
	
	
	public byte getElementType(int id) {
		return metaData[id];
	}

		
		
		
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
	
	
	
	
	
	
	private int getMetaSlot(int start, byte containerType)
	{
//		unsigned int num_keys = get_key_count(meta_data, start);
		int numKeys = ((metaData[start] << 24) & 0xFF000000) |
				  ((metaData[start + 1] << 16) & 0x00FF0000) |
				  ((metaData[start + 2] << 8) & 0x0000FF00) |
				  ((metaData[start + 3]) & 0x000000FF);
	
		
		start += Common.root_sz;
		isExt = false;
		
		if (containerType == Common.object) {
			//if there is an empty spot return it
//			if (get_element_type(meta_data, start + ((num_keys-1) * obj_member_sz)) == empty) {
			if (metaData[start + ((numKeys-1) * Common.member_sz)] == Common.empty) {
				if (!dontSortKeys) {
					//keys sorted so return and the caller will call sort 
					return (start + ((numKeys-1) * Common.member_sz));
				} else {
					//keys not sorts find first empty
					for (int i=0; i<numKeys; i++) {
//						if (get_element_type(meta_data, start + (i*1) * obj_member_sz) == empty) {
						if (metaData[start + (i*1) * Common.member_sz] == Common.empty) {
							return start + (i*1) * Common.member_sz;
						}
					}
				}
			}
		} else {
			//for array start from front and see if there is an empty slot
			for (int i=0; i<numKeys; i++) {
				if (metaData[start + (i*1) * Common.member_sz] == Common.empty) {
					return start + (i*1) * Common.member_sz;
				}
			}
		}
			
		//if all key spots are used fall back to slow linked list
		isExt = true;
		// make sure there is enough room
//		metaLength = increase_meta_buffer(Common.member_ext_sz, metaLength, metaIndx);
		increaseMetaData(Common.member_ext_sz);

		
		if (metaData[start + (numKeys * Common.member_sz)] == Common.extended) {
//			std::cout << "extended 1" << std::endl;
			int end = ((metaData[start + (numKeys * Common.member_sz) + Common.member_keyvalue_offx] << 24) & 0xFF000000) |
					((metaData[start + (numKeys * Common.member_sz) + Common.member_keyvalue_offx + 1] << 16) & 0x00FF0000) |
					((metaData[start + (numKeys * Common.member_sz) + Common.member_keyvalue_offx + 2] << 8) & 0x0000FF00) |
					((metaData[start + (numKeys * Common.member_sz) + Common.member_keyvalue_offx + 3]) & 0x000000FF);
			

			if (end == 0) {
//				std::cout << "extended 2" << std::endl;
				// adding first extended key/val
//				set_key_offx_value(meta_data, start + (numKeys * obj_member_sz), *meta_i_ptr);
				metaData[start + (numKeys * Common.member_sz) + Common.member_keyvalue_offx] = (byte)((metaIndx >> 24) & 0xFF);
				metaData[start + (numKeys * Common.member_sz) + Common.member_keyvalue_offx + 1] = (byte)((metaIndx >> 16) & 0xFF);
				metaData[start + (numKeys * Common.member_sz) + Common.member_keyvalue_offx + 2] = (byte)((metaIndx >> 8) & 0xFF);
				metaData[start + (numKeys * Common.member_sz) + Common.member_keyvalue_offx + 3] = (byte)(metaIndx & 0xFF); 
						
//				set_uint_a_indx(meta_data, *meta_i_ptr + obj_member_sz, 0);
				metaData[metaIndx + Common.member_sz] = 0;
				metaData[metaIndx + Common.member_sz + 1] = 0;
				metaData[metaIndx + Common.member_sz + 2] = 0;
				metaData[metaIndx + Common.member_sz + 3] = 0; 
				
//				*meta_i_ptr += obj_member_ext_sz;
				metaIndx += Common.member_ext_sz;
				
				if (metaEqData)
					dataIndx = metaIndx;
				
				 return metaIndx - Common.member_ext_sz;
			} else {
				// follow list to end
				int check = ((metaData[end + Common.member_sz] << 24) & 0xFF000000) |
						  ((metaData[end + Common.member_sz + 1] << 16) & 0x00FF0000) |
						  ((metaData[end + Common.member_sz + 2] << 8) & 0x0000FF00) |
						  ((metaData[end + Common.member_sz + 3]) & 0x000000FF);
				
//				while (get_uint_a_indx(meta_data, end + obj_member_sz) > 0) {
//					end = get_uint_a_indx(meta_data, end + obj_member_sz);
//				}
				while (check > 0) {
					end = check;
					check = ((metaData[end + Common.member_sz] << 24) & 0xFF000000) |
							  ((metaData[end + Common.member_sz + 1] << 16) & 0x00FF0000) |
							  ((metaData[end + Common.member_sz + 2] << 8) & 0x0000FF00) |
							  ((metaData[end + Common.member_sz + 3]) & 0x000000FF);
				}

//				std::cout << "final end: " << end << std::endl;

//				set_uint_a_indx(meta_data, end + obj_member_sz, *meta_i_ptr);
				metaData[end + Common.member_sz] = (byte)((metaIndx >> 24) & 0xFF);
				metaData[end + Common.member_sz + 1] = (byte)((metaIndx >> 16) & 0xFF);
				metaData[end + Common.member_sz + 2] = (byte)((metaIndx >> 8) & 0xFF);
				metaData[end + Common.member_sz + 3] = (byte)(metaIndx & 0xFF); 
				
				
//				set_uint_a_indx(meta_data, *meta_i_ptr + obj_member_sz, 0);
				metaData[metaIndx + Common.member_sz] = 0;
				metaData[metaIndx + Common.member_sz + 1] = 0;
				metaData[metaIndx + Common.member_sz + 2] = 0;
				metaData[metaIndx + Common.member_sz + 3] = 0; 
				
//				 *meta_i_ptr += obj_member_ext_sz;
				 metaIndx += Common.member_ext_sz;
				 
				 if (metaEqData)
					 dataIndx = metaIndx;
				 
//				std::cout << "extended returning: " << *meta_i_ptr - obj_member_ext_sz << std::endl;
				 
//				 return *meta_i_ptr - obj_member_ext_sz;
				 return metaIndx - Common.member_ext_sz;
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
	
	
	private int preIncMetaIndx() {
		metaIndx++;
		
		if (metaEqData)
			dataIndx = metaIndx;
		
		return metaIndx;
	}
	
	private int postIncMetaIndx() {
		metaIndx++;
		
		if (metaEqData)
			dataIndx = metaIndx;
		
		return metaIndx-1;
	}
	
	private void addToMetaIndx(int toAdd) {
		metaIndx += toAdd;
		
		if (metaEqData)
			metaIndx = dataIndx;
	}
	
	private int preIncDataIndx() {
		dataIndx++;
		
		if (metaEqData)
			metaIndx = dataIndx;
		
		return dataIndx;
	}
	
	private int postIncDataIndx() {
		dataIndx++;
		
		if (metaEqData)
			metaIndx = dataIndx;
		
		return dataIndx-1;
	}
		
	
	/**
	 * @return the errorCode
	 */
	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public void crap() {
		System.out.println("Meta:\n" + new String(metaData));
		System.out.println("Data:\n" + new String(data));
	}
}
