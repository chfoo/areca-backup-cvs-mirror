package com.application.areca.adapters;

import java.io.File;
import java.util.Iterator;

import com.application.areca.filter.ArchiveFilter;
import com.application.areca.filter.DirectoryArchiveFilter;
import com.application.areca.filter.FileDateArchiveFilter;
import com.application.areca.filter.FileExtensionArchiveFilter;
import com.application.areca.filter.FileOwnerArchiveFilter;
import com.application.areca.filter.FileSizeArchiveFilter;
import com.application.areca.filter.FilterGroup;
import com.application.areca.filter.LockedFileFilter;
import com.application.areca.filter.RegexArchiveFilter;
import com.application.areca.filter.SpecialFileFilter;
import com.application.areca.impl.AbstractIncrementalFileSystemMedium;
import com.application.areca.impl.FileSystemRecoveryTarget;
import com.application.areca.impl.IncrementalDirectoryMedium;
import com.application.areca.impl.IncrementalZipMedium;
import com.application.areca.impl.handler.DefaultArchiveHandler;
import com.application.areca.impl.handler.DeltaArchiveHandler;
import com.application.areca.impl.policy.EncryptionPolicy;
import com.application.areca.impl.policy.FileSystemPolicy;
import com.application.areca.plugins.StoragePlugin;
import com.application.areca.plugins.StoragePluginRegistry;
import com.application.areca.processor.DeleteProcessor;
import com.application.areca.processor.FileDumpProcessor;
import com.application.areca.processor.MailSendProcessor;
import com.application.areca.processor.MergeProcessor;
import com.application.areca.processor.Processor;
import com.application.areca.processor.ProcessorList;
import com.application.areca.processor.ShellScriptProcessor;
import com.myJava.file.FileSystemManager;

/**
 * Target serializer
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
public class TargetXMLWriter extends AbstractXMLWriter {

    protected boolean removeSensitiveData = false;
    
    public TargetXMLWriter() {
        this(new StringBuffer());
    }
    
    public TargetXMLWriter(StringBuffer sb) {
        super(sb);
    }

    public void setRemoveSensitiveData(boolean removeSensitiveData) {
        this.removeSensitiveData = removeSensitiveData;
    }
    
    public void serializeTarget(FileSystemRecoveryTarget tg) {
        sb.append("\n\n<");
        sb.append(XML_TARGET);
        
        sb.append(" ");
        sb.append(XML_TARGET_ID);
        sb.append("=");
        sb.append(encode(tg.getId()));
        
        sb.append(" ");
        sb.append(XML_TARGET_UID);
        sb.append("=");
        sb.append(encode(tg.getUid()));
        
        sb.append(" ");
        sb.append(XML_TARGET_FOLLOW_SYMLINKS);
        sb.append("=");
        sb.append(encode(! tg.isTrackSymlinks()));
        
        sb.append(" ");
        sb.append(XML_TARGET_TRACK_EMPTY_DIRS);
        sb.append("=");
        sb.append(encode(tg.isTrackEmptyDirectories()));
        
        sb.append(" ");
        sb.append(XML_TARGET_FOLLOW_SUBDIRECTORIES);
        sb.append("=");
        sb.append(encode(! tg.isFollowSubdirectories()));
        
        sb.append(" ");
        sb.append(XML_TARGET_CREATE_XML_SECURITY_COPY);
        sb.append("=");
        sb.append(encode(tg.isCreateSecurityCopyOnBackup()));
        
        sb.append(" ");
        sb.append(XML_TARGET_NAME);
        sb.append("=");
        sb.append(encode(tg.getTargetName()));     
        
        sb.append(" ");
        sb.append(XML_TARGET_DESCRIPTION);
        sb.append("=");
        sb.append(encode(tg.getComments()));     
        
        sb.append(">");   
        
        // Sources
        Iterator sources = tg.getSources().iterator();
        while (sources.hasNext()) {
            File source = (File)sources.next();
            serializeSource(source);
        }
        
        // Support
        if (IncrementalDirectoryMedium.class.isAssignableFrom(tg.getMedium().getClass())) {
            serializeMedium((IncrementalDirectoryMedium)tg.getMedium());            
        } else if (IncrementalZipMedium.class.isAssignableFrom(tg.getMedium().getClass())) {
            serializeMedium((IncrementalZipMedium)tg.getMedium());
        }
       
        // Filtres
        serializeFilter(tg.getFilterGroup());
        
        // Preprocessors
        serializeProcessors(tg.getPreProcessors(), false);
        
        // Postprocessors
        serializeProcessors(tg.getPostProcessors(), true);
        
        sb.append("\n</");
        sb.append(XML_TARGET);
        sb.append(">");            
    }
    
    protected void serializeProcessors(ProcessorList actions, boolean preProcesses) {
        Iterator iter = actions.iterator();
        while (iter.hasNext()) {
            Object pp = iter.next();
            if (FileDumpProcessor.class.isAssignableFrom(pp.getClass())) {
                serializeProcessor((FileDumpProcessor)pp, preProcesses);
            } else if (MailSendProcessor.class.isAssignableFrom(pp.getClass())) {
                serializeProcessor((MailSendProcessor)pp, preProcesses);            
            } else if (ShellScriptProcessor.class.isAssignableFrom(pp.getClass())) {
                serializeProcessor((ShellScriptProcessor)pp, preProcesses); 
            } else if (MergeProcessor.class.isAssignableFrom(pp.getClass())) {
                serializeProcessor((MergeProcessor)pp, preProcesses); 
            } else if (DeleteProcessor.class.isAssignableFrom(pp.getClass())) {
                serializeProcessor((DeleteProcessor)pp, preProcesses); 
            }
        }
    }
    
    protected void serializeSource(File source) {
        sb.append("\n<");
        sb.append(XML_SOURCE);
        sb.append(" ");
        sb.append(XML_SOURCE_PATH);
        sb.append("=");
        sb.append(encode(FileSystemManager.getAbsolutePath(source)));
        sb.append("/>");     
    }
    
    protected void serializeProcessorHeader(String header, boolean postProcess, Processor proc) {
        sb.append("\n<");
        sb.append(header);
        sb.append(" ");     
        sb.append(XML_PP_AFTER);
        sb.append("=");
        sb.append(encode(postProcess));  
        sb.append(" "); 
        sb.append(XML_PP_RUN_SCHEME);
        sb.append("=");
        sb.append(encode(transcodeRunScheme(proc)));  
        sb.append(" "); 
    }
    
    protected String transcodeRunScheme(Processor proc) {
    	if (proc.getRunScheme() == Processor.RUN_SCHEME_ALWAYS) {
    		return XML_PP_RUN_SCHEME_ALWAYS;
    	} else if (proc.getRunScheme() == Processor.RUN_SCHEME_FAILURE) {
    		return XML_PP_RUN_SCHEME_FAILURE;
    	} else {
    		return XML_PP_RUN_SCHEME_SUCCESS;
    	}
    }

    protected void serializeProcessor(FileDumpProcessor pp, boolean postProcess) {
        serializeProcessorHeader(XML_PROCESSOR_DUMP, postProcess, pp);
        sb.append(XML_PP_DUMP_DIRECTORY);
        sb.append("=");
        sb.append(encode(FileSystemManager.getAbsolutePath(pp.getDestinationFolder())));
        sb.append(" ");     
        sb.append(XML_PP_DUMP_NAME);
        sb.append("=");
        sb.append(encode(pp.getReportName()));  
        sb.append("/>");        
    }
    
    protected void serializeProcessor(MergeProcessor pp, boolean postProcess) {
        serializeProcessorHeader(XML_PROCESSOR_MERGE, postProcess, pp);
        sb.append(XML_PP_MERGE_FROM_DELAY);
        sb.append("=");
        sb.append(encode(pp.getFromDelay()));
        sb.append(" ");
        sb.append(XML_PP_MERGE_TO_DELAY);
        sb.append("=");
        sb.append(encode(pp.getToDelay()));
        sb.append(" ");
        sb.append(XML_PP_MERGE_KEEP_DELETED);
        sb.append("=");
        sb.append(encode(pp.isKeepDeletedEntries()));
        sb.append("/>");        
    }
    
    protected void serializeProcessor(DeleteProcessor pp, boolean postProcess) {
        serializeProcessorHeader(XML_PROCESSOR_DELETE, postProcess, pp);
        sb.append(XML_PP_DELAY);
        sb.append("=");
        sb.append(encode(pp.getDelay()));
        sb.append("/>");        
    }
    
    protected void serializeProcessor(MailSendProcessor pp, boolean postProcess) {
        serializeProcessorHeader(XML_PROCESSOR_EMAIL, postProcess, pp);
        sb.append(XML_PP_EMAIL_RECIPIENTS);
        sb.append("=");
        sb.append(encode(pp.getRecipients()));
        sb.append(" ");
        sb.append(XML_PP_EMAIL_SMTP);
        sb.append("=");
        sb.append(encode(pp.getSmtpServer()));
        sb.append(" ");
        sb.append(XML_PP_EMAIL_USER);
        sb.append("=");
        sb.append(encode(pp.getUser()));      
        sb.append(" ");
        sb.append(XML_PP_EMAIL_PASSWORD);
        sb.append("=");
        sb.append(encode(pp.getPassword()));  
        sb.append(" ");        
        sb.append(XML_PP_EMAIL_SMTPS);
        sb.append("=");
        sb.append(encode(pp.isSmtps()));    
        sb.append(" ");
        sb.append(XML_PP_EMAIL_TITLE);
        sb.append("=");
        sb.append(encode(pp.getTitle()));   
        sb.append(" ");
        sb.append(XML_PP_EMAIL_FROM);
        sb.append("=");
        sb.append(encode(pp.getFrom()));   
        sb.append(" ");
        sb.append(XML_PP_EMAIL_INTRO);
        sb.append("=");
        sb.append(encode(pp.getIntro())); 
        sb.append("/>");        
    }
    
    protected void serializeProcessor(ShellScriptProcessor pp, boolean postProcess) {
        serializeProcessorHeader(XML_PROCESSOR_SHELL, postProcess, pp);
        sb.append(XML_PP_SHELL_SCRIPT);
        sb.append("=");
        sb.append(encode(pp.getCommand()));
        sb.append(" ");
        sb.append(XML_PP_SHELL_PARAMS);
        sb.append("=");
        sb.append(encode(pp.getCommandParameters()));
        sb.append("/>");        
    }
    
    protected void serializeFilter(FilterGroup filters) {
        sb.append("\n<");
        sb.append(XML_FILTER_GROUP);
        sb.append(" ");
        sb.append(XML_FILTER_EXCLUDE);
        sb.append("=");
        sb.append(encode(filters.isExclude()));
        sb.append(" ");
        sb.append(XML_FILTER_GROUP_OPERATOR);
        sb.append("=");
        sb.append(encode(filters.isAnd() ? XML_FILTER_GROUP_OPERATOR_AND : XML_FILTER_GROUP_OPERATOR_OR));
        sb.append(" ");
        sb.append(">");             
        Iterator iter = filters.getFilterIterator();
        while (iter.hasNext()) {
            Object filter = iter.next();
            if (DirectoryArchiveFilter.class.isAssignableFrom(filter.getClass())) {
                serializeFilter((DirectoryArchiveFilter)filter);
            } else if (FileExtensionArchiveFilter.class.isAssignableFrom(filter.getClass())) {
                serializeFilter((FileExtensionArchiveFilter)filter);            
            } else if (RegexArchiveFilter.class.isAssignableFrom(filter.getClass())) {
                serializeFilter((RegexArchiveFilter)filter); 
            } else if (FileSizeArchiveFilter.class.isAssignableFrom(filter.getClass())) {
                serializeFilter((FileSizeArchiveFilter)filter); 
            } else if (SpecialFileFilter.class.isAssignableFrom(filter.getClass())) {
                serializeFilter((SpecialFileFilter)filter); 
            } else if (LockedFileFilter.class.isAssignableFrom(filter.getClass())) {
                serializeFilter((LockedFileFilter)filter); 
            } else if (FileDateArchiveFilter.class.isAssignableFrom(filter.getClass())) {
                serializeFilter((FileDateArchiveFilter)filter); 
            } else if (FileOwnerArchiveFilter.class.isAssignableFrom(filter.getClass())) {
                serializeFilter((FileOwnerArchiveFilter)filter);                 
            } else if (FilterGroup.class.isAssignableFrom(filter.getClass())) {
                serializeFilter((FilterGroup)filter); 
            }
        }
        sb.append("\n</");
        sb.append(XML_FILTER_GROUP);
        sb.append(">");     
    }
    
    protected void serializeFilter(RegexArchiveFilter filter) {
        sb.append("\n<");
        sb.append(XML_FILTER_REGEX);
        sb.append(" ");
        sb.append(XML_FILTER_EXCLUDE);
        sb.append("=");
        sb.append(encode(filter.isExclude()));
        sb.append(" ");
        sb.append(XML_FILTER_RG_PATTERN);
        sb.append("=");
        sb.append(encode(filter.getRegex()));
        sb.append(" ");
        sb.append(XML_FILTER_RG_MODE);
        sb.append("=");
        sb.append(encode(filter.getScheme()));
        sb.append(" ");
        sb.append(XML_FILTER_RG_FULL_MATCH);
        sb.append("=");
        sb.append(encode(filter.isMatch()));
        sb.append("/>");        
    }
    
    protected void serializeFilter(DirectoryArchiveFilter filter) {
        sb.append("\n<");
        sb.append(XML_FILTER_DIRECTORY);
        sb.append(" ");
        sb.append(XML_FILTER_EXCLUDE);
        sb.append("=");
        sb.append(encode(filter.isExclude()));
        sb.append(" ");
        sb.append(XML_FILTER_DIR_PATH);
        sb.append("=");
        sb.append(encode(filter.getStringParameters()));
        sb.append("/>");        
    }
    
    protected void serializeFilter(FileExtensionArchiveFilter filter) {
        sb.append("\n<");
        sb.append(XML_FILTER_FILEEXTENSION);
        sb.append(" ");
        sb.append(XML_FILTER_EXCLUDE);
        sb.append("=");
        sb.append(encode(filter.isExclude()));
        sb.append(">");
        
        Iterator iter = filter.getExtensionIterator();
        while (iter.hasNext()) {
            sb.append("\n<");
            sb.append(XML_FILTER_EXTENSION);
            sb.append(">");
            sb.append(iter.next().toString());
            sb.append("</");
            sb.append(XML_FILTER_EXTENSION);
            sb.append(">");            
        }
        
        sb.append("\n</");
        sb.append(XML_FILTER_FILEEXTENSION);
        sb.append(">");         
    }    
    
    protected void serializeFilter(FileSizeArchiveFilter filter) {
        serializeFilterGenericData(filter, XML_FILTER_FILESIZE, true);
    }
    
    protected void serializeFilter(FileOwnerArchiveFilter filter) {
        serializeFilterGenericData(filter, XML_FILTER_OWNER, true);
    }
    
    protected void serializeFilter(FileDateArchiveFilter filter) {
        serializeFilterGenericData(filter, XML_FILTER_FILEDATE, true);
    }
    
    protected void serializeFilter(SpecialFileFilter filter) {
        sb.append("\n<");
        sb.append(XML_FILTER_TP);
        sb.append(" ");
        sb.append(XML_FILTER_EXCLUDE);
        sb.append("=");
        sb.append(encode(filter.isExclude()));
        sb.append(" ");
        sb.append(XML_FILTER_TP_BLOCKSPECFILE);
        sb.append("=");
        sb.append(encode(filter.isBlockSpecFile()));
        sb.append(" ");
        sb.append(XML_FILTER_TP_CHARSPECFILE);
        sb.append("=");
        sb.append(encode(filter.isCharSpecFile()));
        sb.append(" ");
        sb.append(XML_FILTER_TP_PIPE);
        sb.append("=");
        sb.append(encode(filter.isPipe()));
        sb.append(" ");
        sb.append(XML_FILTER_TP_SOCKET);
        sb.append("=");
        sb.append(encode(filter.isSocket()));
        sb.append(" ");
        sb.append(XML_FILTER_TP_LINK);
        sb.append("=");
        sb.append(encode(filter.isLink()));
        sb.append("/>");  
    }

    protected void serializeFilter(LockedFileFilter filter) {
        serializeFilterGenericData(filter, XML_FILTER_LOCKED, false);
    }
    
    protected void serializeFilterGenericData(ArchiveFilter filter, String filterName, boolean addParam) {
        sb.append("\n<");
        sb.append(filterName);
        sb.append(" ");
        sb.append(XML_FILTER_EXCLUDE);
        sb.append("=");
        sb.append(encode(filter.isExclude()));
        if (addParam) {
            sb.append(" ");
            sb.append(XML_FILTER_PARAM);
            sb.append("=");
            sb.append(encode(filter.getStringParameters()));
        }
        sb.append("/>");        
    }
    
    protected void serializeMedium(IncrementalZipMedium medium) {
        sb.append("\n<");
        sb.append(XML_MEDIUM);
        sb.append(" ");
        sb.append(XML_MEDIUM_TYPE);
        sb.append("=");
        sb.append(encode(XML_MEDIUM_TYPE_ZIP));
        
        this.endMedium(medium);     
    } 
    
    protected void serializeMedium(IncrementalDirectoryMedium medium) {
        sb.append("\n<");
        sb.append(XML_MEDIUM);
        sb.append(" ");
        sb.append(XML_MEDIUM_TYPE);
        sb.append("=");
        sb.append(encode(XML_MEDIUM_TYPE_DIR));
        
        if (medium.getCompressionArguments().isCompressed()) {
            sb.append(" ");      
            sb.append(XML_MEDIUM_FILECOMPRESSION);
            sb.append("=");
            sb.append(encode("true"));
        }
        
        this.endMedium(medium); 
    }   
    
    protected void endMedium(AbstractIncrementalFileSystemMedium medium) {
        sb.append(" ");
        serializeFileSystemPolicy(medium.getFileSystemPolicy());
        serializeEncryptionPolicy(medium.getEncryptionPolicy());
         
        sb.append(" ");
        sb.append(XML_MEDIUM_TRACK_PERMS);
        sb.append("=");
        sb.append(encode(medium.isTrackPermissions())); 
        
        sb.append(" ");
        sb.append(XML_MEDIUM_OVERWRITE);
        sb.append("=");
        sb.append(encode(medium.isOverwrite())); 
        
        if (medium.getCompressionArguments().isCompressed()) {
            if (medium.getCompressionArguments().isMultiVolumes()) {
                sb.append(" ");      
                sb.append(XML_MEDIUM_VOLUME_SIZE);
                sb.append("=");
                sb.append(encode(medium.getCompressionArguments().getVolumeSize()));
                
                sb.append(" ");     
                sb.append(XML_MEDIUM_VOLUME_DIGITS);
                sb.append("=");
                sb.append(encode(medium.getCompressionArguments().getNbDigits()));
            }
            
            if (medium.getCompressionArguments().getComment() != null) {
                sb.append(" ");     
                sb.append(XML_MEDIUM_ZIP_COMMENT);
                sb.append("=");
                sb.append(encode(medium.getCompressionArguments().getComment()));
            }
            
            if (medium.getCompressionArguments().getLevel() >= 0) {
	            sb.append(" ");     
	            sb.append(XML_MEDIUM_ZIP_LEVEL);
	            sb.append("=");
	            sb.append(encode(medium.getCompressionArguments().getLevel()));
            }

            sb.append(" ");     
            sb.append(XML_MEDIUM_ZIP_EXTENSION);
            sb.append("=");
            sb.append(encode(medium.getCompressionArguments().isAddExtension()));
            
            if (medium.getCompressionArguments().getCharset() != null) {
                sb.append(" ");    
                sb.append(XML_MEDIUM_ZIP_CHARSET);
                sb.append("=");
                sb.append(encode(medium.getCompressionArguments().getCharset().name()));        
            }
            
            if (medium.getCompressionArguments().isUseZip64()) {
                sb.append(" ");      
                sb.append(XML_MEDIUM_Z64);
                sb.append("=");
                sb.append(encode("true"));
            }
        }
        sb.append(">");
        if (medium.getHandler() instanceof DefaultArchiveHandler) {
            serializeHandler((DefaultArchiveHandler)medium.getHandler());
        } else if (medium.getHandler() instanceof DeltaArchiveHandler) {
            serializeHandler((DeltaArchiveHandler)medium.getHandler()); 
        } else {
            throw new IllegalArgumentException("Unknown handler type : " + medium.getHandler().getClass().getName());
        }
        sb.append("\n</" + XML_MEDIUM + ">");
        
    }
    
    protected void startHandler(String type) {
        sb.append("\n<");
        sb.append(XML_HANDLER);
        sb.append(" ");
        sb.append(XML_HANDLER_TYPE);
        sb.append("=");
        sb.append(encode(type));
    }
    
    protected void serializeHandler(DefaultArchiveHandler handler) {
        startHandler(XML_HANDLER_TYPE_STANDARD);
        sb.append("/>");
    }
    
    protected void serializeHandler(DeltaArchiveHandler handler) {
        startHandler(XML_HANDLER_TYPE_DELTA);
        sb.append("/>");
    }
    
    protected void serializeEncryptionPolicy(EncryptionPolicy policy) {
        sb.append(" ");
        sb.append(XML_MEDIUM_ENCRYPTED);
        sb.append("=");
        sb.append(encode(policy.isEncrypted()));     
        
        if (policy.isEncrypted() && (! removeSensitiveData)) {
	        sb.append(" ");
	        sb.append(XML_MEDIUM_ENCRYPTIONKEY);
	        sb.append("=");
	        sb.append(encode(policy.getEncryptionKey())); 
	        
	        sb.append(" ");
	        sb.append(XML_MEDIUM_ENCRYPTIONALGO);
	        sb.append("=");
	        sb.append(encode(policy.getEncryptionAlgorithm()));   
	        
	        sb.append(" ");
	        sb.append(XML_MEDIUM_ENCRYPTNAMES);
	        sb.append("=");
	        sb.append(encode(policy.isEncryptNames()));
        }
    }
    
    protected void serializeFileSystemPolicy(FileSystemPolicy policy) {
        String id = policy.getId();
        
        sb.append(" ");
        sb.append(XML_MEDIUM_POLICY);
        sb.append("=");
        sb.append(encode(id)); 
        
        StoragePlugin plugin = StoragePluginRegistry.getInstance().getById(id);
        plugin.buildFileSystemPolicyXMLHandler().write(policy, this, removeSensitiveData, sb);
    }
}
