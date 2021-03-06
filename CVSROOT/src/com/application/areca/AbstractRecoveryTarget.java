package com.application.areca;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;

import com.application.areca.context.ProcessContext;
import com.application.areca.context.StatusList;
import com.application.areca.filter.ArchiveFilter;
import com.application.areca.filter.FilterGroup;
import com.application.areca.impl.AbstractFileSystemMedium;
import com.application.areca.impl.FileSystemRecoveryEntry;
import com.application.areca.indicator.IndicatorMap;
import com.application.areca.metadata.manifest.Manifest;
import com.application.areca.metadata.manifest.ManifestKeys;
import com.application.areca.metadata.manifest.ManifestManager;
import com.application.areca.processor.ProcessorList;
import com.application.areca.search.SearchCriteria;
import com.application.areca.search.TargetSearchResult;
import com.application.areca.version.VersionInfos;
import com.myJava.object.Duplicable;
import com.myJava.object.EqualsHelper;
import com.myJava.object.HashHelper;
import com.myJava.system.OSTool;
import com.myJava.util.CalendarUtils;
import com.myJava.util.errors.ActionReport;
import com.myJava.util.history.History;
import com.myJava.util.history.HistoryEntry;
import com.myJava.util.log.Logger;
import com.myJava.util.taskmonitor.TaskCancelledException;
import com.myJava.util.taskmonitor.TaskMonitor;

/**
 * Abstract base implementation for recovery targets.
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
public abstract class AbstractRecoveryTarget 
implements HistoryEntryTypes, Duplicable, Identifiable, TargetActions {
	public static final String BACKUP_SCHEME_FULL = "Full backup";
	public static final String BACKUP_SCHEME_INCREMENTAL = "Incremental backup";
	public static final String BACKUP_SCHEME_DIFFERENTIAL = "Differential backup";

	protected ArchiveMedium medium;
	protected FilterGroup filterGroup = new FilterGroup();
	protected int id; // Numeric unique id of the target within its process
	protected String uid; // Unique identifier
	protected String targetName; // Name of the target
	protected TargetGroup group;
	protected String comments;
	protected ProcessorList postProcessors = new ProcessorList();
	protected ProcessorList preProcessors = new ProcessorList();
	protected boolean running;
	protected boolean createSecurityCopyOnBackup = true;

	public void setGroup(TargetGroup group) {
		this.group = group;
	}

	protected void copyAttributes(Object clone) {
		AbstractRecoveryTarget other = (AbstractRecoveryTarget)clone;
		other.group = group;
		other.id = group.getNextFreeTargetId();
		other.uid = generateNewUID();
		other.targetName = "Copy of " + targetName;
		other.comments = comments;
		other.filterGroup = (FilterGroup)this.filterGroup.duplicate();
		other.postProcessors = (ProcessorList)postProcessors.duplicate();
		other.preProcessors = (ProcessorList)preProcessors.duplicate();
		other.setMedium((ArchiveMedium)medium.duplicate(), true);
		other.createSecurityCopyOnBackup = this.createSecurityCopyOnBackup;
	}

	public boolean supportsBackupScheme(String backupScheme) {
		return this.medium.supportsBackupScheme(backupScheme);
	}

	public ProcessorList getPostProcessors() {
		return postProcessors;
	}

	public ProcessorList getPreProcessors() {
		return preProcessors;
	}

	public boolean isRunning() {
		return running;
	}

	public boolean isCreateSecurityCopyOnBackup() {
		return createSecurityCopyOnBackup;
	}

	public void setCreateSecurityCopyOnBackup(boolean createSecurityCopyOnBackup) {
		this.createSecurityCopyOnBackup = createSecurityCopyOnBackup;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public FilterGroup getFilterGroup() {
		return filterGroup;
	}

	public void setFilterGroup(FilterGroup filterGroup) {
		this.filterGroup = filterGroup;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUid() {
		if (this.uid == null) {
			Logger.defaultLogger().info("No UID was specified for target '" + targetName + "'. Creating a random ID");
			this.uid = generateNewUID();
		}      

		return uid;
	}

	public String getTargetName() {
		return targetName;
	}

	public void setTargetName(String taskName) {
		this.targetName = taskName;
	}

	public String getDescription() {
		StringBuffer buf = new StringBuffer("Target #");
		buf.append(this.id);
		buf.append("\n\tName : ");
		buf.append(getTargetName());
		buf.append("\n\t");
		buf.append(this.getSpecificTargetDescription());
		buf.append("\n\tMedium : ");
		buf.append(medium.getDescription());
		return new String(buf);
	}

	protected abstract String getSpecificTargetDescription();

	public TargetGroup getGroup() {
		return this.group;
	}

	/**
	 * Check the system state before critical operations (merges, deletions, ...)
	 */
	public ActionReport checkTargetState(int action) {
		return this.medium.checkMediumState(action);
	}

	public void validateTargetState(int action) throws ApplicationException {
		ActionReport report = checkTargetState(action);
		if (! report.isDataValid()) {
			ApplicationException ex = new ApplicationException(report);
			Logger.defaultLogger().error("Incoherent state detected for target " + getTargetName() + " : " + ex.getMessage());
			throw ex;
		}  
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	/**
	 * @return Returns the medium.
	 */
	public ArchiveMedium getMedium() {
		return medium;
	}

	public TargetSearchResult search(SearchCriteria criteria) throws ApplicationException {
		return this.medium.search(criteria);
	}

	/**
	 * @param medium The medium to set.
	 */
	public void setMedium(ArchiveMedium medium, boolean revalidateMedium) {
		this.medium = medium;
		this.medium.setTarget(this, revalidateMedium);
	}

	public void addFilter(ArchiveFilter filter) {
		this.filterGroup.addFilter(filter);
	}

	public Iterator getFilterIterator() {
		return this.filterGroup.getFilterIterator();
	} 

	public History getHistory() {
		return this.medium.getHistory();
	}

	public void clearHistory() {
		History hist = this.medium.getHistory();
		if (hist != null) {
			hist.clear();
			hist.flush();
		}
	}

	/**
	 * Open and lock the target
	 */
	protected void open(Manifest manifest, ProcessContext context, String backupScheme) throws ApplicationException {
		context.setBackupScheme(backupScheme);
		medium.open(manifest, context, backupScheme);
	}   

	public synchronized void processBackup(
			Manifest manifest, 
			String backupScheme,
			boolean disablePreCheck,
			boolean disableArchiveCheck,
			ProcessContext context
	) throws ApplicationException {
		boolean backupRequired = true;
		try {
			this.validateTargetState(ACTION_BACKUP);

			if (this.medium.isPreBackupCheckUseful() && (!disablePreCheck) && backupScheme.equals(BACKUP_SCHEME_INCREMENTAL)) {
				context.getTaskMonitor().getCurrentActiveSubTask().addNewSubTask(0.2, "pre-check");
				context.getInfoChannel().print("Pre-check in progress ...");
				this.processSimulateImpl(context, false);
				context.getInfoChannel().print("Pre-check completed.");
				backupRequired = (context.getReport().getSavedFiles() > 0 || context.getReport().getDeletedFiles() > 0);
				context.getTaskMonitor().getCurrentActiveSubTask().addNewSubTask(0.8, "backup");
				context.reset(false);
			}

			if (backupRequired) {
				if (! this.preProcessors.isEmpty()) {
					context.getTaskMonitor().getCurrentActiveSubTask().addNewSubTask(0.1, "pre-processors");
					TaskMonitor preProcessMon = context.getTaskMonitor().getCurrentActiveSubTask();
					try {     
						this.preProcessors.run(context);
					} finally {
						preProcessMon.enforceCompletion();
					}
				}

				// Create main task monitor
				double remaining = 
					1.0 
					- (this.postProcessors.isEmpty() ? 0 : 0.1) 
					- (this.preProcessors.isEmpty() ? 0 : 0.1)
					- (disableArchiveCheck ? 0 : 0.4);

				if (remaining != 1) {
					context.getTaskMonitor().getCurrentActiveSubTask().addNewSubTask(remaining, "backup-main");
				}

				if (manifest == null) {
					manifest = new Manifest(Manifest.TYPE_BACKUP);
				}

				try {
					// Start the backup
					context.reset(false);
					context.getInfoChannel().print("Backup in progress ...");
					context.getTaskMonitor().checkTaskState();
					context.getReport().startDataFlowTimer();
					this.open(manifest, context, backupScheme);

					History h = this.getHistory();
					if (h != null) {
						h.addEntry(new HistoryEntry(HISTO_BACKUP, "Backup."));
					}

					RecoveryEntry entry = this.nextElement(context);
					long index = 0;
					while (entry != null) {
						context.getInfoChannel().getTaskMonitor().checkTaskState();
						if (this.filterEntryBeforeStore(entry)) {
							try {
								index++;
								context.getInfoChannel().updateCurrentTask(index, 0, entry.toString());
								this.medium.store(entry, context);
							} catch (StoreException e) {
								throw new ApplicationException(e);
							}
						}
						entry = this.nextElement(context); 
					}
					this.commitBackup(context);
					context.getReport().setWrittenKBytes(context.getOutputBytesInKB());
					context.getReport().stopDataFlowTimer();
					Logger.defaultLogger().info(Utils.formatLong(context.getInputBytesInKB()) + " kb read in " + Utils.formatLong(context.getReport().getDataFlowTimeInSecond()) + " seconds.");                
					Logger.defaultLogger().info("Average data input : " + Utils.formatLong(context.getInputBytesInKBPerSecond()) + " kb/second.");
					Logger.defaultLogger().info(Utils.formatLong(context.getReport().getWrittenKBytes()) + " kb written in " + Utils.formatLong(context.getReport().getDataFlowTimeInSecond()) + " seconds.");                
					Logger.defaultLogger().info("Average data output : " + Utils.formatLong(context.getOutputBytesInKBPerSecond()) + " kb/second.");

				} catch (Throwable e) {
					if (! TaskCancelledException.isTaskCancellation(e)) {
						Logger.defaultLogger().error(e);
					}
					this.rollbackBackup(context, e.getMessage());
					throw wrapException(e);
				}
			}
		} finally {
			if ((! context.getReport().getStatus().hasError()) && (! disableArchiveCheck)) {
				context.getTaskMonitor().getCurrentActiveSubTask().addNewSubTask(0.4, "archive check");
				TaskMonitor checkMon = context.getTaskMonitor().getCurrentActiveSubTask();

				try {
					// Get the date
					Manifest mf = ManifestManager.readManifestForArchive((AbstractFileSystemMedium)this.medium, ((AbstractFileSystemMedium)this.medium).getLastArchive());
					GregorianCalendar cal = mf.getDate();

					// Check the archive
					context.getTaskMonitor().getCurrentActiveSubTask().addNewSubTask(0.6, "effective check");
					this.processArchiveCheck(null, true, cal, context);
					if (context.getInvalidRecoveredFiles() != null && context.getInvalidRecoveredFiles().size() != 0) {
						String msg = "The created archive was not successfully checked. It will be deleted.";
						context.getReport().getStatus().addItem(StatusList.KEY_BACKUP, msg);
						context.getInfoChannel().error(msg);
						context.getTaskMonitor().getCurrentActiveSubTask().addNewSubTask(0.4, "deletion");  
						this.processDeleteArchives(cal, context);
					}
				} finally {
					checkMon.enforceCompletion();
				}
			}

			if (backupRequired) {
				if (! this.postProcessors.isEmpty()) {
					context.getTaskMonitor().getCurrentActiveSubTask().addNewSubTask(0.1, "post-processors");
					TaskMonitor postProcessMon = context.getTaskMonitor().getCurrentActiveSubTask();
					try {     
						this.postProcessors.run(context);
					} finally {
						postProcessMon.enforceCompletion();
					}
				}
				context.getInfoChannel().print("Backup completed."); 
			} else {
				// No backup is necessary
				context.getTaskMonitor().getCurrentActiveSubTask().setCurrentCompletion(1.0);
				context.getInfoChannel().print("No backup required - Operation completed.");     
			}
		}
	}

	/**
	 * Launch a simulation process.
	 */
	public synchronized SimulationResult processSimulate(ProcessContext context) throws ApplicationException {
		try {
			validateTargetState(ACTION_SIMULATE);
			context.getInfoChannel().print("Simulation in progress ...");
			return this.processSimulateImpl(context, true);
		} finally {
			context.getInfoChannel().print("Simulation completed.");            
		}
	}    

	/**
	 * Launch a simulation process.
	 */
	public synchronized SimulationResult processSimulateImpl(ProcessContext context, boolean returnDetailedResult) throws ApplicationException {
		try {  
			TaskMonitor simulationGlobalMonitor = context.getInfoChannel().getTaskMonitor().getCurrentActiveSubTask();

			SimulationResult entries = new SimulationResult();
			FileSystemRecoveryEntry entry = (FileSystemRecoveryEntry)this.nextElement(context);
			long index = 0;
			while (entry != null) {
				context.getTaskMonitor().checkTaskState();
				if (this.filterEntryBeforeStore(entry)) {
					index++;
					context.getInfoChannel().updateCurrentTask(index, 0, entry.toString());
					this.medium.simulateEntryProcessing(entry, !returnDetailedResult, context);
					if (entry.getStatus() != EntryStatus.STATUS_NOT_STORED) {
						if (entry.getStatus() == EntryStatus.STATUS_DELETED) {
							context.getReport().addDeletedFile();
						} else {
							context.getReport().addSavedFile();
						}

						if (returnDetailedResult) {
							entries.addEntry(entry);
						} else {
							// Once we get a stored entry in "not detailed" mode, stop this method --> We know that it will be necessary to make a backup
							simulationGlobalMonitor.enforceCompletion();
							break;
						}
					} else {
						context.getReport().addIgnoredFile();
					}
				}
				entry = (FileSystemRecoveryEntry)this.nextElement(context); 
			}
			context.getTaskMonitor().checkTaskState();
			medium.closeSimulation(context); 

			return entries;
		} catch (Throwable e) {
			throw wrapException(e);
		}
	}    

	public static void addBasicInformationsToManifest(Manifest mf) {
		mf.addProperty(ManifestKeys.VERSION, VersionInfos.getLastVersion().getVersionId());
		mf.addProperty(ManifestKeys.VERSION_DATE, VersionInfos.formatVersionDate(VersionInfos.getLastVersion().getVersionDate()));        
		mf.addProperty(ManifestKeys.BUILD_ID, VersionInfos.getBuildId());
		mf.addProperty(ManifestKeys.ENCODING, OSTool.getIANAFileEncoding());        
		mf.addProperty(ManifestKeys.OS_NAME, OSTool.getOSDescription());
	}

	/**
	 * Commit the backup and release the lock on the target
	 */
	protected void commitBackup(ProcessContext context) throws ApplicationException {
		try {
			context.getTaskMonitor().checkTaskState();
			context.getTaskMonitor().setCancellable(false);
			context.getManifest().addProperty(ManifestKeys.FILTERED_ENTRIES, context.getReport().getFilteredEntries());
			context.getManifest().addProperty(ManifestKeys.BACKUP_DURATION, Utils.formatDuration(System.currentTimeMillis() - context.getReport().getStartMillis()));     
			context.getManifest().addProperty(ManifestKeys.TARGET_ID, this.getUid());
			addBasicInformationsToManifest(context.getManifest());

			medium.commitBackup(context);
			context.getReport().getStatus().addItem(StatusList.KEY_BACKUP);
		} catch (Throwable e) {
			Logger.defaultLogger().error("Exception caught during backup commit.", e);
			this.rollbackBackup(context, e.getMessage());
			throw wrapException(e);
		}
	}

	private ApplicationException wrapException(Throwable e) {
		if (e instanceof ApplicationException) {
			return (ApplicationException)e;
		} else {
			Logger.defaultLogger().error(e);
			return new ApplicationException(e);
		}
	}

	/**
	 * Rollback the backup and release the lock on the target
	 */
	protected void rollbackBackup(ProcessContext context, String message) throws ApplicationException {
		try {
			context.getTaskMonitor().setCancellable(false);
			History h = this.getHistory();
			if (h != null) {
				h.addEntry(new HistoryEntry(HISTO_BACKUP_CANCEL, "Backup cancellation."));
			}
		} finally {
			try {
				medium.rollbackBackup(context);
			} finally {
				context.getReport().getStatus().addItem(StatusList.KEY_BACKUP, message);
			}
		}
	}

	public void processMerge(
			int fromDelay, 
			int toDelay, 
			Manifest manifest, 
			boolean keepDeletedEntries,
			ProcessContext context
	) throws ApplicationException {
		if (fromDelay != 0 && toDelay != 0 && fromDelay < toDelay) {
			// switch from/to
			int tmp = toDelay;
			toDelay = fromDelay;
			fromDelay = tmp;
		}

		// From
		GregorianCalendar fromDate = null;
		if (fromDelay != 0) {
			fromDate = new GregorianCalendar();
			fromDate.add(Calendar.DATE, -1 * fromDelay);
		}

		// To
		GregorianCalendar toDate = new GregorianCalendar();
		toDate.add(Calendar.DATE, -1 * toDelay);

		// Go !
		processMerge(fromDate, toDate, manifest, keepDeletedEntries, context);
	}

	/**
	 * Merge archives
	 */
	public void processMerge(
			GregorianCalendar fromDate, 
			GregorianCalendar toDate, 
			Manifest manifest,
			boolean keepDeletedEntries,
			ProcessContext context
	) throws ApplicationException {
		try {
			validateTargetState(ACTION_MERGE_OR_DELETE);  
			context.getInfoChannel().print("Merge in progress ...");
			History h = this.getHistory();
			if (h != null) {
				h.addEntry(new HistoryEntry(HISTO_MERGE, "Merge from " + Utils.formatDisplayDate(fromDate) + " to " + Utils.formatDisplayDate(toDate) + "."));
			}       
			this.medium.merge(fromDate, toDate, manifest, keepDeletedEntries, context);
			this.commitMerge(context);
		} catch (Throwable e) {
			Logger.defaultLogger().error(e);
			this.rollbackMerge(context, e.getMessage());
			throw wrapException(e);
		} finally {
			context.getInfoChannel().print("Merge completed.");
		}
	}  

	/**
	 * Delete archives
	 */
	public void processDeleteArchives(
			GregorianCalendar fromDate,
			ProcessContext context
	) throws ApplicationException {
		validateTargetState(ACTION_MERGE_OR_DELETE);  
		try {
			context.getTaskMonitor().setCancellable(false);
			context.getInfoChannel().print("Deletion in progress ...");

			History h = this.getHistory();
			if (h != null) {
				h.addEntry(new HistoryEntry(HISTO_DELETE, "Archive deletion from " + Utils.formatDisplayDate(fromDate) + "."));
			}       
			this.medium.deleteArchives(fromDate, context);
		} catch (Throwable e) {
			throw wrapException(e);
		} finally {
			context.getTaskMonitor().resetCancellationState();
			context.getInfoChannel().print("Deletion completed.");
		}   
	}  

	/**
	 * Deletes the archive which are newer than "delay" days.
	 */
	public void processDeleteArchives(int delay, ProcessContext context) throws ApplicationException {
		GregorianCalendar mergeDate = new GregorianCalendar();
		mergeDate.add(Calendar.DATE, -1 * delay);

		processDeleteArchives(mergeDate, context);
	}

	protected void commitMerge(ProcessContext context) throws ApplicationException {
		try {
			context.getTaskMonitor().checkTaskState();
			context.getTaskMonitor().setCancellable(false);
			this.medium.commitMerge(context);
			context.getReport().getStatus().addItem(StatusList.KEY_MERGE);
			context.getTaskMonitor().resetCancellationState();
		} catch (TaskCancelledException e) {
			throw new ApplicationException(e);
		}
	}

	protected void rollbackMerge(ProcessContext context, String message) throws ApplicationException {
		context.getInfoChannel().getTaskMonitor().setCancellable(false);
		try {
			HistoryEntry entry = new HistoryEntry(HISTO_MERGE_CANCEL, "Merge cancellation.");
			History h = this.getHistory();
			if (h != null) {
				h.addEntry(entry);
			}
		} catch (Throwable e) {
			Logger.defaultLogger().error(e);
		} 

		try {
			this.medium.rollbackMerge(context);
		} finally {
			context.getReport().getStatus().addItem(StatusList.KEY_MERGE, message);
		}
		context.getTaskMonitor().resetCancellationState();
	}

	/**
	 * Recover stored data
	 */
	public void processRecover(
			String destination, 
			String[] filters, 
			GregorianCalendar date, 
			boolean keepDeletedEntries,
			boolean checkRecoveredFiles, 
			ProcessContext context
	) throws ApplicationException {
		validateTargetState(ACTION_RECOVER);
		TaskMonitor globalMonitor = context.getTaskMonitor().getCurrentActiveSubTask();
		try {
			String strDate = date == null ? "" : " as of " + CalendarUtils.getDateToString(date);
			context.getInfoChannel().print("Recovery" + strDate + " in progress ...");
			StringBuffer sb = new StringBuffer("Recovery destination = " + destination);
			if (filters != null && filters.length != 0) {
				sb.append(", Items = {");
				for (int i=0; i<filters.length; i++) {
					if (i != 0) {
						sb.append(", ");
					}
					sb.append(filters[i]);
				}
				sb.append("}");
			}
			Logger.defaultLogger().info(sb.toString());

			if (date == null) {
				date = new GregorianCalendar();
			}
			History h = this.getHistory();
			if (h != null) {
				h.addEntry(new HistoryEntry(HISTO_RECOVER, "Recovery : " + Utils.formatDisplayDate(date) + "."));
			} 
			this.processRecoverImpl(destination, filters, date, keepDeletedEntries, checkRecoveredFiles, context);
		} finally {
			context.getInfoChannel().print("Recovery completed.");
			globalMonitor.enforceCompletion();
		}
	}

	/**
	 * Recovers a specific version of a given file
	 */
	public void processRecover(
			String destination, 
			GregorianCalendar date, 
			String entry, 
			boolean checkRecoveredFiles, 
			ProcessContext context
	) throws ApplicationException {
		validateTargetState(ACTION_RECOVER);
		TaskMonitor globalMonitor = context.getTaskMonitor().getCurrentActiveSubTask();
		try {
			String strDate = date == null ? "" : " as of " + CalendarUtils.getDateToString(date);
			context.getInfoChannel().print("Recovery of " + entry + strDate + " in progress ...");
			Logger.defaultLogger().info("Recovery destination = " + destination);
			this.processRecoverImpl(destination, date, entry, checkRecoveredFiles,context);
		} finally {
			context.getInfoChannel().print("Recovery of " + entry + " completed.");
			globalMonitor.enforceCompletion();
		}
	}

	/**
	 * Recover stored data
	 */
	protected abstract void processRecoverImpl(
			String destination, 
			String[] filters, 
			GregorianCalendar date, 
			boolean keepDeletedEntries,
			boolean checkRecoveredFiles, 
			ProcessContext context
	) throws ApplicationException;    

	/**
	 * Check the archive's content
	 */
	public abstract void processArchiveCheck(
			String destination, 
			boolean checkOnlyArchiveContent, 
			GregorianCalendar date, 
			ProcessContext context
	) throws ApplicationException;

	/**
	 * Recover stored data
	 */
	protected abstract void processRecoverImpl(
			String destination, 
			GregorianCalendar date, 
			String name, 
			boolean checkRecoveredFiles,
			ProcessContext context
	) throws ApplicationException;    

	public void doBeforeDelete() {
		if (this.getMedium() != null) {
			this.getMedium().doBeforeDelete();
		}
	}

	public void doAfterDelete() {
		if (this.getMedium() != null) {
			this.getMedium().doAfterDelete();
		}
	}

	public abstract RecoveryEntry nextElement(ProcessContext context) throws ApplicationException;

	public abstract Manifest buildDefaultMergeManifest(GregorianCalendar fromDate, GregorianCalendar toDate) throws ApplicationException;

	protected boolean filterEntryBeforeStore(RecoveryEntry entry) {
		return true;
	}

	public String toString() {
		if (this.targetName == null) {
			return "Element " + this.id;
		} else {
			return this.targetName;
		}
	}

	public boolean equals(Object arg0) {
		if (arg0 == null) {
			return false;
		} else if (! (arg0 instanceof AbstractRecoveryTarget)) {
			return false;
		} else {
			AbstractRecoveryTarget other = (AbstractRecoveryTarget)arg0;
			return (
					EqualsHelper.equals(other.getUid(), this.getUid())
			);
		}
	}

	public int hashCode() {
		int result = HashHelper.initHash(this);
		result = HashHelper.hash(result, this.getUid());
		return result;
	}

	/**
	 * Compute indicators on the stored data. 
	 */
	public IndicatorMap computeIndicators() throws ApplicationException {
		try {
			validateTargetState(ACTION_INDICATORS);
			return this.medium.computeIndicators();
		} catch (TaskCancelledException e) {
			throw new ApplicationException(e);
		}
	}

	/**
	 * Build a new UID.
	 */
	private static String generateNewUID() {
		try {
			SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
			return String.valueOf(Math.abs((long)prng.nextInt())); // Math.abs(Integer.MIN_VALUE) == Integer.MIN_VALUE ... hence the cast as "long"
		} catch (NoSuchAlgorithmException e) {
			Logger.defaultLogger().error("Error generating a random integer. Using Math.random instead.", e);
			return "" + Math.abs((int)(Math.random() * 10000000 + System.currentTimeMillis()));
		}
	}

	public void secureUpdateCurrentTask(long taskIndex, long taskCount, String task, ProcessContext context) {
		try {
			context.getInfoChannel().updateCurrentTask(taskIndex, taskCount, task);
		} catch (Throwable e) {
			Logger.defaultLogger().error(e);
		}
	}

	public void secureUpdateCurrentTask(String task, ProcessContext context) {
		secureUpdateCurrentTask(0, 1, task, context);
	}
}

