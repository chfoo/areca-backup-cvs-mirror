package com.myJava.file.metadata.posix;

import java.util.Iterator;
import java.util.StringTokenizer;

import com.myJava.file.metadata.FileMetaData;
import com.myJava.file.metadata.FileMetaDataSerializationException;
import com.myJava.file.metadata.FileMetaDataSerializer;
import com.myJava.util.Util;
import com.myJava.util.log.Logger;

/**
 * <BR>
 * @author Olivier PETRUCCI
 * <BR>
 *
 */

 /*
 Copyright 2005-2009, Olivier PETRUCCI.

This file is part of Areca.

    Areca is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Areca is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Areca; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class PosixMetaDataSerializer 
implements FileMetaDataSerializer {
	private static final String SEPARATOR = ";";

	/**
	 * Format : "mode;owner;group;xttr list;default acl;access acl"
	 * <BR>Where :
	 * <BR>- xattr list : "size;name;value;name;value ..."
	 * <BR>- acl : "size;tag;[identifier;]permissions;tag;[identifier;]permissions ..."
	 */
	public FileMetaData deserialize(String s, long version) throws FileMetaDataSerializationException {
		if (version <= 3) {
			return deserializePriorV4(s);
		} else {
			return deserializeCurrentVersion(s);
		}
	}
	
    /**
     * Read current format (version 4)
     */
	private FileMetaData deserializeCurrentVersion(String s) throws FileMetaDataSerializationException {
		PosixMetaDataImpl p = new PosixMetaDataImpl();
		
        try {
        	StringTokenizer stt = new StringTokenizer(s, ";");
        	
        	// Base attributes
        	p.setMode(Integer.parseInt(stt.nextToken()));
        	p.setOwner(stt.nextToken());
        	p.setGroup(stt.nextToken());
        	
        	// Extended attributes
        	int attrSize = Integer.parseInt(stt.nextToken());
        	if (attrSize != 0) {
	        	p.setXattrList(new ExtendedAttributeList());
	        	for (int i=0; i<attrSize; i++) {
	        		p.getXattrList().addAttribute(stt.nextToken(), Util.base64Decode(stt.nextToken()));
	        	}
        	}
        	
        	// Default acl
        	int defACLSize = Integer.parseInt(stt.nextToken());
        	if (defACLSize != 0) {
	        	p.setDefaultAcl(new ACL());
	        	readACL(p.getDefaultAcl(), stt, defACLSize);
        	}
        	
        	// Access acl
        	int accessACLSize = Integer.parseInt(stt.nextToken());
        	if (accessACLSize != 0) {
	        	p.setAccessAcl(new ACL());
	        	readACL(p.getAccessAcl(), stt, accessACLSize);
        	}
        	
			return p;
		} catch (RuntimeException e) {
			Logger.defaultLogger().error("Error caught during permission deserialization : " + s, e);
			throw e;
		}
	}
	
	private void readACL(ACL acl, StringTokenizer stt, int size) {
    	for (int i=0; i<size; i++) {
    		int tag =  Integer.parseInt(stt.nextToken());
    		int identifier = -1;
    		if (tag == ACLEntry.ACL_USER || tag == ACLEntry.ACL_GROUP) {
    			identifier = Integer.parseInt(stt.nextToken());
    		}
    		acl.addEntry(tag, identifier, Integer.parseInt(stt.nextToken()));
    	}
	}

	public void serialize(FileMetaData attr, StringBuffer sb) throws FileMetaDataSerializationException {
        PosixMetaDataImpl perm = (PosixMetaDataImpl)attr;
        write(perm.getMode(), sb);
        write(perm.getOwner(), sb);
        write(perm.getGroup(), sb);
        writeExtendedAttributeList(perm.getXattrList(), sb);
        writeACL(perm.getDefaultAcl(), sb);
        writeACL(perm.getAccessAcl(), sb);
	}
    
    private void writeExtendedAttributeList(ExtendedAttributeList lst, StringBuffer sb) {
    	if (lst == null) {
	    	write(0, sb);
    	} else {
	    	write(lst.size(), sb);
	    	Iterator iter = lst.iterator();
	    	while (iter.hasNext()) {
	    		writeExtendedAttribute((ExtendedAttribute)iter.next(), sb);
	    	}
    	}
    }
    
    private void writeExtendedAttribute(ExtendedAttribute attr, StringBuffer sb) {
    	write(attr.getName(), sb);
    	write(Util.base64Encode(attr.getData()), sb);
    }
    
    private void writeACL(ACL acl, StringBuffer sb) {
    	if (acl == null) {
	    	write(0, sb);
    	} else {
	    	write(acl.size(), sb);
	    	Iterator iter = acl.iterator();
	    	while (iter.hasNext()) {
	    		writeACLEntry((ACLEntry)iter.next(), sb);
	    	}
    	}
    }
    
    private void writeACLEntry(ACLEntry entry, StringBuffer sb) {
    	write(entry.getTag(), sb);
    	if (entry.getTag() == ACLEntry.ACL_USER || entry.getTag() == ACLEntry.ACL_GROUP) {
        	write(entry.getIdentifier(), sb);
    	}
    	write(entry.getPermissions(), sb);
    }
    
    private void write(long data, StringBuffer sb) {
		sb.append(data).append(SEPARATOR);
    }
    
    private void write(String data, StringBuffer sb) {
		sb.append(data).append(SEPARATOR);
    }
    
    /**
     * Read old format (prior to version 4)
     */
	private FileMetaData deserializePriorV4(String s) throws FileMetaDataSerializationException {
        try {
			String perms = s.substring(1, 4);
			int iPerms = Integer.parseInt(perms);
			
			int index = s.indexOf(' ');
			String owner = s.substring(4, index);
			String group = s.substring(index + 1).trim();
			
			PosixMetaDataImpl p = new PosixMetaDataImpl();
			p.setOwner(owner);
			p.setGroup(group);
			p.setModeBase10(iPerms);

			return p;
		} catch (RuntimeException e) {
			Logger.defaultLogger().error("Error caught during permission deserialization : " + s, e);
			throw e;
		}
	}
}
