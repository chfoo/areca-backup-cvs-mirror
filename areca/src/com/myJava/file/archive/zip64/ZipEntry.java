/*
 * @(#)ZipEntry.java	1.35 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.myJava.file.archive.zip64;

import java.io.Serializable;
import java.util.Date;

import com.myJava.object.ToStringHelper;

/**
 * This class is used to represent a ZIP file entry.
 *
 * @version	1.35, 01/23/03
 * @author	David Connelly
 * 
 * <BR>This class was derived from the original java.util.zip.ZipEntry.
 * <BR>No modifications were made except package change.
 * <BR>
 * <BR>CAUTION :
 * <BR>This file has been integrated into Areca.
 * <BR>It is has also possibly been adapted to meet Areca's needs. If such modifications has been made, they are described above.
 * <BR>Thanks to the authors for their work.
 *
 */
public
class ZipEntry implements ZipConstants, Cloneable, Serializable {
    String name;	// entry name
    long time = -1;	// modification time (in DOS time)
    long crc = -1;	// crc-32 of entry data
    private long size = -1;	// uncompressed size of entry data
    long csize = -1;   	// compressed size of entry data
    int method = -1;	// compression method
    byte[] extra;	// optional extra field data for entry
    String comment;	// optional comment string for entry
    // The following flags are used only by Zip{Input,Output}Stream
    int flag;		// bit flags
    int version;	// version needed to extract
    long offset;	// offset of loc header
    boolean isZip64;
    int volumeNumber;

    /**
     * Compression method for uncompressed entries.
     */
    public static final int STORED = 0;

    /**
     * Compression method for compressed (deflated) entries.
     */
    public static final int DEFLATED = 8;

    static {
        /* load the zip library */
	java.security.AccessController.doPrivileged(
		  new sun.security.action.LoadLibraryAction("zip"));
    }


    /**
     * Creates a new zip entry with the specified name.
     *
     * @param name the entry name
     * @exception NullPointerException if the entry name is null
     * @exception IllegalArgumentException if the entry name is longer than
     *		  0xFFFF bytes
     */
    public ZipEntry(String name) {
	if (name == null) {
	    throw new NullPointerException();
	}
	if (name.length() > 0xFFFF) {
	    throw new IllegalArgumentException("entry name too long");
	}
	this.name = name;
    }

    /**
     * Creates a new zip entry with fields taken from the specified
     * zip entry.
     * @param e a zip Entry object
     */
    public ZipEntry(ZipEntry e) {
	name = e.name;
	time = e.time;
	crc = e.crc;
	size = e.size;
	csize = e.csize;
	method = e.method;
	extra = e.extra;
	comment = e.comment;
    }

    public int getVolumeNumber() {
        return volumeNumber;
    }

    public void setVolumeNumber(int volumeNumber) {
        this.volumeNumber = volumeNumber;
    }

    /**
     * Returns the name of the entry.
     * @return the name of the entry
     */
    public String getName() {
	return name;
    }

    /**
     * Sets the modification time of the entry.
     * @param time the entry modification time in number of milliseconds
     *		   since the epoch
     * @see #getTime()
     */
    public void setTime(long time) {
	this.time = javaToDosTime(time);
    }

    /**
     * Returns the modification time of the entry, or -1 if not specified.
     * @return the modification time of the entry, or -1 if not specified
     * @see #setTime(long)
     */
    public long getTime() {
	return time != -1 ? dosToJavaTime(time) : -1;
    }

    /**
     * Sets the uncompressed size of the entry data.
     */
    public void setSize(long size) {
    	this.size = size;
    }

    /**
     * Returns the uncompressed size of the entry data, or -1 if not known.
     * @return the uncompressed size of the entry data, or -1 if not known
     * @see #setSize(long)
     */
    public long getSize() {
    	return size;
    }

    /**
     * Returns the size of the compressed entry data, or -1 if not known.
     * In the case of a stored entry, the compressed size will be the same
     * as the uncompressed size of the entry.
     * @return the size of the compressed entry data, or -1 if not known
     * @see #setCompressedSize(long)
     */
    public long getCompressedSize() {
	return csize;
    }

    /**
     * Sets the size of the compressed entry data.
     * @param csize the compressed size to set to
     * @see #getCompressedSize()
     */
    public void setCompressedSize(long csize) {
	this.csize = csize;
    }

    /**
     * Sets the CRC-32 checksum of the uncompressed entry data.
     * @param crc the CRC-32 value
     * @exception IllegalArgumentException if the specified CRC-32 value is
     *		  less than 0 or greater than 0xFFFFFFFF
     * @see #setCrc(long)
     */
    public void setCrc(long crc) {
	if (crc < 0 || crc > 0xFFFFFFFFL) {
	    throw new IllegalArgumentException("invalid entry crc-32");
	}
	this.crc = crc;
    }

    /**
     * Returns the CRC-32 checksum of the uncompressed entry data, or -1 if
     * not known.
     * @return the CRC-32 checksum of the uncompressed entry data, or -1 if
     * not known
     * @see #getCrc()
     */
    public long getCrc() {
	return crc;
    }

    /**
     * Sets the compression method for the entry.
     * @param method the compression method, either STORED or DEFLATED
     * @exception IllegalArgumentException if the specified compression
     *		  method is invalid
     * @see #getMethod()
     */
    public void setMethod(int method) {
	if (method != STORED && method != DEFLATED) {
	    throw new IllegalArgumentException("invalid compression method");
	}
	this.method = method;
    }

    /**
     * Returns the compression method of the entry, or -1 if not specified.
     * @return the compression method of the entry, or -1 if not specified
     * @see #setMethod(int)
     */
    public int getMethod() {
	return method;
    }

    /**
     * Sets the optional extra field data for the entry.
     * @param extra the extra field data bytes
     * @exception IllegalArgumentException if the length of the specified
     *		  extra field data is greater than 0xFFFF bytes
     * @see #getExtra()
     */
    public void setExtra(byte[] extra) {
	if (extra != null && extra.length > 0xFFFF) {
	    throw new IllegalArgumentException("invalid extra field length");
	}
	this.extra = extra;
    }

    /**
     * Returns the extra field data for the entry, or null if none.
     * @return the extra field data for the entry, or null if none
     * @see #setExtra(byte[])
     */
    public byte[] getExtra() {
	return extra;
    }

    /**
     * Sets the optional comment string for the entry.
     */
    public void setComment(String comment) {
	this.comment = comment;
    }

    /**
     * Returns the comment string for the entry, or null if none.
     * @return the comment string for the entry, or null if none
     * @see #setComment(String)
     */
    public String getComment() {
	return comment;
    }

    /**
     * Returns true if this is a directory entry. A directory entry is
     * defined to be one whose name ends with a '/'.
     * @return true if this is a directory entry
     */
    public boolean isDirectory() {
	return name.endsWith("/");
    }

    /**
     * Returns a string representation of the ZIP entry.
     */
    public String toString() {
        StringBuffer sb = ToStringHelper.init(this);
        ToStringHelper.append("crc", this.crc, sb);
        ToStringHelper.append("csize", this.csize, sb);
        ToStringHelper.append("flag", this.flag, sb);
        ToStringHelper.append("isZip64", this.isZip64, sb);
        ToStringHelper.append("method", this.method, sb);
        ToStringHelper.append("offset", this.offset, sb);
        ToStringHelper.append("size", this.size, sb);
        ToStringHelper.append("time", this.time, sb);
        ToStringHelper.append("version", this.version, sb);
        ToStringHelper.append("name", this.name, sb);        
        ToStringHelper.append("extra", this.extra, sb);       
        ToStringHelper.append("comment", this.comment, sb);       
        ToStringHelper.append("volumeNumber", this.volumeNumber, sb);
        return ToStringHelper.close(sb);
    }

    /*
     * Converts DOS time to Java time (number of milliseconds since epoch).
     */
    private static long dosToJavaTime(long dtime) {
	Date d = new Date((int)(((dtime >> 25) & 0x7f) + 80),
			  (int)(((dtime >> 21) & 0x0f) - 1),
			  (int)((dtime >> 16) & 0x1f),
			  (int)((dtime >> 11) & 0x1f),
			  (int)((dtime >> 5) & 0x3f),
			  (int)((dtime << 1) & 0x3e));
	return d.getTime();
    }

    /*
     * Converts Java time to DOS time.
     */
    private static long javaToDosTime(long time) {
	Date d = new Date(time);
	int year = d.getYear() + 1900;
	if (year < 1980) {
	    return (1 << 21) | (1 << 16);
	}
	return (year - 1980) << 25 | (d.getMonth() + 1) << 21 |
               d.getDate() << 16 | d.getHours() << 11 | d.getMinutes() << 5 |
               d.getSeconds() >> 1;
    }

    /**
     * Returns the hash code value for this entry.
     */
    public int hashCode() {
	return name.hashCode();
    }

    /**
     * Returns a copy of this entry.
     */
    public Object clone() {
	try {
	    ZipEntry e = (ZipEntry)super.clone();
	    e.extra = (extra == null ? null : (byte[])extra.clone());
	    return e;
	} catch (CloneNotSupportedException e) {
	    // This should never happen, since we are Cloneable
	    throw new InternalError();
	}
    }
    
    
}