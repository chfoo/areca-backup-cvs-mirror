package com.application.areca.adapters;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import com.application.areca.ApplicationException;
import com.application.areca.TargetGroup;
import com.application.areca.impl.FileSystemTarget;
import com.myJava.file.FileSystemManager;
import com.myJava.file.FileTool;
import com.myJava.util.log.Logger;

/**
 * Process serializer
 * 
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
public class ProcessXMLWriter extends AbstractXMLWriter { 
	//public static final int CURRENT_VERSION = 2;
	//public static final int CURRENT_VERSION = 3; // introduced in v6.1
	//public static final int CURRENT_VERSION = 4; // introduced in v7.1 : Special files filters replace symbolic links filters 
	//public static final int CURRENT_VERSION = 5; // introduced in v7.1.3 : follow_subdirectories replaced by follow_subdirs, and fix of serialization bug
	//public static final int CURRENT_VERSION = 6; // introduced in v7.1.4 : filter parameterization change : "exclude" replaced by "logical_not" ... easier to understand
	public static final int CURRENT_VERSION = 7; // introduced in v7.1.5 : post processor parameterization (success / error / warning)
	
    private TargetXMLWriter targetWriter;
    
    public ProcessXMLWriter() {
        this(false);
    }
    
    public ProcessXMLWriter(boolean removeEncryptionData) {
        super(new StringBuffer());
        targetWriter = new TargetXMLWriter(this.sb);
        targetWriter.setRemoveSensitiveData(removeEncryptionData);
    }
    
    public boolean serializeProcess(TargetGroup process) throws ApplicationException {
        return serializeProcess(process, process.getSourceFile());
    }
    
    public boolean serializeProcess(TargetGroup process, File targetFile) throws ApplicationException {
        try {
            writeHeader();
            sb.append("\n<");
            sb.append(XML_PROCESS);
            sb.append(" ");
            sb.append(XML_PROCESS_DESCRIPTION);
            sb.append("=");
            sb.append(encode(process.getComments()));      
            sb.append(" ");
            sb.append(XML_VERSION);
            sb.append("=");
            sb.append(encode(CURRENT_VERSION));   
            sb.append(">");
            
            // Targets
            Iterator iter = process.getTargetIterator();
            while(iter.hasNext()) {
                FileSystemTarget tg = (FileSystemTarget)iter.next();
                this.targetWriter.serializeTarget(tg);
            }
            
            sb.append("\n</");
            sb.append(XML_PROCESS);
            sb.append(">");        
            
            // Create parent directory if it does not exist
            if (! FileSystemManager.exists(FileSystemManager.getParentFile(targetFile))) {
            	FileTool.getInstance().createDir(FileSystemManager.getParentFile(targetFile));
            }
            
            // In some cases, and for some unknown reason, the written file appears to be empty. (behavior reported by a user) 
            // There was probably an error the user didn't see in the log ... whatever : we want to make the process serialization
            // transactional : keep the old file, try to write the new one, keep the old one if the serialization fails.
            
            // Write the "uncommitted" file
            File uncommittedFile = new File(FileSystemManager.getParent(targetFile), FileSystemManager.getName(targetFile) + ".tmp");
            ensureAvailability(uncommittedFile);
            String content = this.sb.toString().trim();
            OutputStream fos = FileSystemManager.getFileOutputStream(uncommittedFile);
            OutputStreamWriter fw = new OutputStreamWriter(fos, getEncoding());
            fw.write(content);
            fw.close();
            
            // Check the written file : try to read it and compare its content to the original one
            String read = ("" + FileTool.getInstance().getFileContent(uncommittedFile)).trim();
            if (read.equals(content)) {
            	// The written file is OK -> "commit" the file
            	ensureAvailability(targetFile);
            	return FileSystemManager.renameTo(uncommittedFile, targetFile);
            } else {
            	// The written file is not OK -> "rollback" the file and keep the original one
            	Logger.defaultLogger().warn("An error occured while writing the XML configuration on " + FileSystemManager.getAbsolutePath(uncommittedFile) + " : Original content = [" + content + "], written content = [" + read + "]");
            	ensureAvailability(uncommittedFile);
            	return false;
            }
        } catch (UnsupportedEncodingException e) {
            throw new ApplicationException(e);
        } catch (IOException e) {
            throw new ApplicationException(e);
        }
    }
    
    private void ensureAvailability(File targetFile) throws ApplicationException {
        if (FileSystemManager.exists(targetFile)) {
            if (! FileSystemManager.delete(targetFile)) {
                throw new ApplicationException("The destination [" + FileSystemManager.getAbsolutePath(targetFile) + "] is a directory or can't be deleted.");
            }
        }
    }
}
