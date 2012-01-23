package com.application.areca.tests;

import java.io.File;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.application.areca.AbstractTarget;
import com.application.areca.ActionProxy;
import com.application.areca.ArecaConfiguration;
import com.application.areca.CheckParameters;
import com.application.areca.MergeParameters;
import com.application.areca.TargetGroup;
import com.application.areca.WorkspaceItem;
import com.application.areca.adapters.ConfigurationHandler;
import com.application.areca.context.ProcessContext;
import com.application.areca.impl.AbstractFileSystemMedium;
import com.application.areca.impl.AbstractIncrementalFileSystemMedium;
import com.application.areca.impl.FileSystemTarget;
import com.application.areca.impl.copypolicy.AlwaysOverwriteCopyPolicy;
import com.application.areca.launcher.tui.LoggerUserInformationChannel;
import com.application.areca.launcher.tui.MissingDataListener;
import com.application.areca.metadata.transaction.YesTransactionHandler;
import com.myJava.file.FileTool;
import com.myJava.util.log.LogMessagesContainer;
import com.myJava.util.log.Logger;
import com.myJava.util.log.ThreadLocalLogProcessor;
import com.myJava.util.taskmonitor.TaskMonitor;

/**
 * 
 * <BR>
 * @author Olivier PETRUCCI
 * <BR>
 *
 */

 /*
 Copyright 2005-2011, Olivier PETRUCCI.

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
public class Test {
	public static LoggerUserInformationChannel CHANNEL = new LoggerUserInformationChannel(false);
	private static String CURRENT_TEST = "";
	public static String CURRENT_TARGET = "";
	public static boolean SUCCESS = false;
	private static long START = System.currentTimeMillis();

	public static final String SOURCES_S = ".source_files";
	public static final String RECOVERY_DIR_S = ".recovery_dir";
	
	public static TargetGroup load(String path, String source) throws Exception {
		log("Loading targets ...");
		WorkspaceItem item = ConfigurationHandler.getInstance().readObject(new File(path), new MissingDataListener(), null, true);
		overrideSource(item, source);
		return (TargetGroup)item;
	}
	
	private static void overrideSource(WorkspaceItem item, final String src) throws Exception {
		WorkspaceProcessor.process(item, new TargetHandler() {
			public void handle(FileSystemTarget target) throws Exception {
				Set sources = new HashSet();
				sources.add(new File(src));
				target.setSources(sources);
			}
		});
	}
	
	private static void cleanArchives(WorkspaceItem item) throws Exception {
		log("Destroying storage directories ...");
		WorkspaceProcessor.process(item, new TargetHandler() {
			public void handle(FileSystemTarget target) throws Exception {
				target.destroyRepository();
			}
		});
	}
	
	private static void showData(WorkspaceItem item) throws Exception {
		WorkspaceProcessor.process(item, new TargetHandler() {
			public void handle(FileSystemTarget target) throws Exception {
				System.out.println(target.getDescription());
			}
		});
	}
	
	private static ProcessContext buildContext(WorkspaceItem item) {
		LogMessagesContainer container = Logger.defaultLogger().getTlLogProcessor().activateMessageTracking();
		ProcessContext context = new ProcessContext((item instanceof AbstractTarget) ? (AbstractTarget)item : null, CHANNEL, new TaskMonitor("tui-main"));
		context.getReport().setLogMessagesContainer(container);
		return context;
	}
	
	private static void doBackup(final WorkspaceItem item, final String backupScheme) throws Exception {
		WorkspaceProcessor.process(item, new TargetHandler() {
			public void handle(FileSystemTarget target) throws Exception {
				CheckParameters checkParams = new CheckParameters(true, true, true, false, null);
				
				ActionProxy.processBackupOnTarget(
						target,
						null,
						backupScheme,
						false,
						checkParams,
						new YesTransactionHandler(),
						buildContext(target)
				);
			}
		});
	}
	
	private static void doMerge(final WorkspaceItem item, final boolean keepDeleted) throws Exception {
		WorkspaceProcessor.process(item, new TargetHandler() {
			public void handle(FileSystemTarget target) throws Exception {
				MergeParameters params = new MergeParameters(keepDeleted, false, null);
				
				ActionProxy.processMergeOnTarget(target, null, new GregorianCalendar(), null, params, buildContext(target));
			}
		});
	}
	
	private static void doRecover(final WorkspaceItem item, final String targetDir, String[] additionalFiles) throws Exception {
		new File(targetDir).mkdirs();
		WorkspaceProcessor.process(item, new TargetHandler() {
			public void handle(FileSystemTarget target) throws Exception {
				ActionProxy.processRecoverOnTarget(
						target, 
						null, 
						new AlwaysOverwriteCopyPolicy(), 
						targetDir, 
						false, 
						new GregorianCalendar(), 
						false, 
						true, 
						buildContext(target));
			}
		});
		
		CreateData.check(targetDir, additionalFiles);
		FileTool.getInstance().delete(new File(targetDir));
	}
	
	private static void doCheck(final WorkspaceItem item) throws Exception {
		WorkspaceProcessor.process(item, new TargetHandler() {
			public void handle(FileSystemTarget target) throws Exception {
				CheckParameters checkParams = new CheckParameters(true, false, true, false, null);
				ProcessContext ctx = buildContext(target);
				ActionProxy.processCheckOnTarget(target, checkParams, new GregorianCalendar(), ctx);
				if (ctx.getInvalidRecoveredFiles() != null && !ctx.getInvalidRecoveredFiles().isEmpty()) {
					Iterator iter = ctx.getInvalidRecoveredFiles().iterator();
					String msg = "Some recovered files are invalid : ";
					while (iter.hasNext()) {
						msg += iter.next() + " ";
					}
					throw new IllegalStateException(msg);
				}
				if (ctx.getUncheckedRecoveredFiles() != null && !ctx.getUncheckedRecoveredFiles().isEmpty()) {
					Iterator iter = ctx.getUncheckedRecoveredFiles().iterator();
					String msg = "Some recovered files were not checked : ";
					while (iter.hasNext()) {
						msg += iter.next() + " ";
					}
					throw new IllegalStateException(msg);
				}
				if (ctx.getUnrecoveredFiles() != null && !ctx.getUnrecoveredFiles().isEmpty()) {
					Iterator iter = ctx.getUnrecoveredFiles().iterator();
					String msg = "Some files were not recovered : ";
					while (iter.hasNext()) {
						msg += iter.next() + " ";
					}
					throw new IllegalStateException(msg);
				}
			}
		});
	}
	
	private static void doCountArchives(final WorkspaceItem item, final int expected) throws Exception {
		WorkspaceProcessor.process(item, new TargetHandler() {
			public void handle(FileSystemTarget target) throws Exception {
				int toCheck = expected;
				if (((AbstractFileSystemMedium)target.getMedium()).isImage() && toCheck > 1) {
					toCheck = 1;
				}

				File[] files = ((AbstractIncrementalFileSystemMedium)target.getMedium()).listArchives(null, null, true);
				if (files == null) {
					if (toCheck != 0) {
						throw new IllegalStateException("Empty storage directory while expected " + toCheck + " archives.");
					}
				} else if (files.length != toCheck) {
					throw new IllegalStateException("Invalid archives number (" + files.length + ") ... expected " + toCheck + " archives.");
				}
				
				log("OK : " + files.length + " archives found.");
			}
		});
	}
	
	public static void main(String[] args) {
		String ws = args[0].replace('\\', '/');
		String sources = ws + "/" + SOURCES_S;
		String recoveryDir = ws + "/" + RECOVERY_DIR_S;
		
		log("Using " + ws + " as workspace.");
		
		try {
			ArecaConfiguration.initialize();
	    	Logger.defaultLogger().setTlLogProcessor(new ThreadLocalLogProcessor());
	    	
	    	switchTo("Create Data");
	    	File f = new File(sources);
	    	FileTool.getInstance().delete(f);
	    	f = new File(recoveryDir);
	    	FileTool.getInstance().delete(f);
			CreateData.create(sources);
			
			switchTo("Load Target");
			TargetGroup workspace = load(ws, sources);
			showData(workspace);
			
			switchTo("Initial Clean");
			cleanArchives(workspace);
			doCountArchives(workspace, 0);
			
			switchTo("First backup");
			doBackup(workspace, AbstractTarget.BACKUP_SCHEME_INCREMENTAL);
			doCountArchives(workspace, 1);
			
			switchTo("Check first backup");
			doCheck(workspace);
			
			switchTo("Recover first backup");
			doRecover(workspace, recoveryDir, null);
			
			switchTo("Modify Data");
			CreateData.append(sources);
			CreateData.remove(sources);
			
			switchTo("Second backup");
			doBackup(workspace, AbstractTarget.BACKUP_SCHEME_INCREMENTAL);
			doCountArchives(workspace, 2);
			
			switchTo("Check second backup");
			doCheck(workspace);
			
			switchTo("Recover second backup");
			doRecover(workspace, recoveryDir, null);

			switchTo("Modify Data (2)");
			CreateData.append2(sources);
			CreateData.remove2(sources);
			
			switchTo("Third backup");
			doBackup(workspace, AbstractTarget.BACKUP_SCHEME_INCREMENTAL);
			doCountArchives(workspace, 3);
			
			switchTo("Check third backup");
			doCheck(workspace);
			
			switchTo("Recover third backup");
			doRecover(workspace, recoveryDir, null);
	
			switchTo("Merge archives");
			doMerge(workspace, false);
			doCountArchives(workspace, 1);
			
			switchTo("Check merged archive");
			doCheck(workspace);
			
			switchTo("Recover merged archive");
			doRecover(workspace, recoveryDir, null);

			switchTo("Modify Data (3)");
			CreateData.finalAppend(sources);
			CreateData.finalRemove(sources);
			
			switchTo("Fourth backup");
			doBackup(workspace, AbstractTarget.BACKUP_SCHEME_DIFFERENTIAL);
			doCountArchives(workspace, 2);
			
			switchTo("Check fourth backup");
			doCheck(workspace);
			
			switchTo("Recover fourth backup");
			doRecover(workspace, recoveryDir, null);
			
			switchTo("Merge archives (2)");
			doMerge(workspace, true);
			doCountArchives(workspace, 1);
			
			switchTo("Check merged archive (2)");
			doCheck(workspace);
			
			switchTo("Recover merged archive (2)");
			doRecover(workspace, recoveryDir, CreateData.FINAL_REMOVE);

			switchTo("Modify Data (4)");
			CreateData.create(sources);
			CreateData.create2(sources);
			
			switchTo("Fifth backup");
			doBackup(workspace, AbstractTarget.BACKUP_SCHEME_INCREMENTAL);
			doCountArchives(workspace, 2);
			
			switchTo("Check fifth backup");
			doCheck(workspace);
			
			switchTo("Recover fifth backup");
			doRecover(workspace, recoveryDir, null);
	
			switchTo("Merge archives (third)");
			doMerge(workspace, false);
			doCountArchives(workspace, 1);
			
			switchTo("Check (3rd) merged archive");
			doCheck(workspace);
			
			switchTo("Recover (3rd) merged archive");
			doRecover(workspace, recoveryDir, null);

			switchTo("Final Clean");
			cleanArchives(workspace);
			doCountArchives(workspace, 0);
			
			SUCCESS = true;
			log("-----------------------------");
			log("Tests performed successfully.");
		} catch (Exception e) {
			e.printStackTrace();
			SUCCESS = false;
			log("------------------------------");
			log("Errors while performing tests.");
			log("Failed at the following step :");
			log(CURRENT_TEST);
			log("... and for :");
			log(CURRENT_TARGET);
		} finally {
	    	try {
				File f = new File(sources);
				FileTool.getInstance().delete(f);
				f = new File(recoveryDir);
				FileTool.getInstance().delete(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		long stop = System.currentTimeMillis();
		log("Elapsed : " + ((stop - START)/1000) + " sec.");
		log("------------------------------");
	}
	
	private static void switchTo(String task) {
		CURRENT_TEST = task;
		log("---------------------------------");
		log("Switching to \"" + task + "\" ...");
		log("---------------------------------");
	}
	
	private static void log(String msg) {
		System.out.println(msg);
	}
}
