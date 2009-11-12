package com.application.areca.launcher.gui;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import com.application.areca.AbstractTarget;
import com.application.areca.ResourceManager;
import com.application.areca.Utils;
import com.application.areca.filter.ArchiveFilter;
import com.application.areca.filter.FileExtensionArchiveFilter;
import com.application.areca.filter.FilterGroup;
import com.application.areca.filter.LockedFileFilter;
import com.application.areca.impl.AbstractFileSystemMedium;
import com.application.areca.impl.AbstractIncrementalFileSystemMedium;
import com.application.areca.impl.EncryptionConfiguration;
import com.application.areca.impl.FileSystemTarget;
import com.application.areca.impl.IncrementalDirectoryMedium;
import com.application.areca.impl.IncrementalZipMedium;
import com.application.areca.impl.handler.DefaultArchiveHandler;
import com.application.areca.impl.handler.DeltaArchiveHandler;
import com.application.areca.impl.policy.DefaultFileSystemPolicy;
import com.application.areca.impl.policy.EncryptionPolicy;
import com.application.areca.impl.policy.FileSystemPolicy;
import com.application.areca.launcher.gui.common.AbstractWindow;
import com.application.areca.launcher.gui.common.ArecaPreferences;
import com.application.areca.launcher.gui.common.FileComparator;
import com.application.areca.launcher.gui.common.ListPane;
import com.application.areca.launcher.gui.common.LocalPreferences;
import com.application.areca.launcher.gui.common.SavePanel;
import com.application.areca.launcher.gui.composites.ProcessorsTable;
import com.application.areca.plugins.StoragePlugin;
import com.application.areca.plugins.StoragePluginRegistry;
import com.application.areca.plugins.StorageSelectionHelper;
import com.myJava.encryption.EncryptionUtil;
import com.myJava.file.CompressionArguments;
import com.myJava.file.FileSystemManager;
import com.myJava.file.FileTool;
import com.myJava.file.archive.zip64.ZipConstants;
import com.myJava.system.OSTool;
import com.myJava.util.PasswordQualityEvaluator;
import com.myJava.util.Util;
import com.myJava.util.history.History;
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
public class TargetEditionWindow
extends AbstractWindow {

	private static final ResourceManager RM = ResourceManager.instance();
	private static final String TITLE = RM.getLabel("targetedition.dialog.title");
	private static final String PLUGIN_HD = "hd";
	private static final String DEFAULT_ARCHIVE_PATTERN = "%YY%%MM%%DD%";

	protected AbstractTarget target;
	public FileSystemPolicy currentPolicy = null;
	protected boolean hasBeenSaved = false;
	protected ArrayList lstEncryptionAlgorithms = new ArrayList();

	protected Button btnSave;

	protected Text txtTargetName;
	protected Text txtDesc;

	protected Text txtMediumPath;
	protected Label lblArchiveName;
	protected Text txtArchiveName;
	protected Button rdFile;
	protected Button btnMediumPath;

	protected Map strRadio = new HashMap();
	protected Map strText = new HashMap();
	protected Map strButton = new HashMap();
	protected String currentFileSystemPolicyId = null;

	protected Group grpCompression;
	protected Group grpEncryption;
	protected Group grpConfiguration;
	protected Group grpFileManagement;
	protected Group grpStorage;
	protected Group grpZipOptions;
	protected Group grpZipComment;
	protected Button rdDir;
	public Button rdZip;
	protected Button rdZip64;
	protected Button chkTrackDirectories;
	protected Button chkFollowSubDirectories;
	protected Button chkTrackPermissions;
	protected Button chkNoXMLCopy;
	protected Button chkEncrypted;
	protected Button chkMultiVolumes;
	protected Button chkAddExtension;
	protected Button chkFollowLinks;
	protected Text txtEncryptionKey;
	protected Button chkEncrypNames;
	protected Text txtMultiVolumes;
	protected Combo cboEncryptionAlgorithm;
	protected Label lblEncryptionExample;
	protected Label lblEncryptionKey;
	protected Button btnGenerateKey;
	protected Label lblQuality;
	protected ProgressBar pgbPwdQuality;
	protected Button btnReveal;
	protected Label lblMultiVolumesUnit;
	protected Label lblEncryptionAlgorithm;
	protected Label lblMultiVolumesDigits;
	protected Text txtMultivolumesDigits;
	protected Text txtZipComment;
	protected Label lblEncoding;
	protected Combo cboEncoding;
	protected Label lblZipLevel;
	protected Combo cboZipLevel;
	protected Button rdArchive;
	protected Button rdSingle;
	protected Button rdImage;
	protected Button rdMultiple;
	protected Button rdDelta;

	private TreeItem transfered;
	protected Tree treFilters;
	protected Button btnAddFilter;
	protected Button btnRemoveFilter;
	protected Button btnModifyFilter;
	protected FilterGroup mdlFilters;

	protected ProcessorsTable postProcessesTab;
	protected ProcessorsTable preProcessesTab;

	protected Table tblSources;  
	protected Button btnAddSource;
	protected Button btnRemoveSource;
	protected Button btnModifySource;

	public TargetEditionWindow(AbstractTarget target) {
		super();
		this.target = target;
	}

	protected Control createContents(Composite parent) {
		application.enableWaitCursor();
		Composite ret = new Composite(parent, SWT.NONE);
		try {
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			ret.setLayout(layout);

			ListPane tabs = new ListPane(ret, SWT.NONE, false);
			GridData dt1 = new GridData();
			dt1.grabExcessHorizontalSpace = true;
			dt1.grabExcessVerticalSpace = true;
			dt1.horizontalAlignment = SWT.FILL;
			dt1.verticalAlignment = SWT.FILL;
			tabs.setLayoutData(dt1);

			initGeneralTab(initTab(tabs, RM.getLabel("targetedition.maingroup.title")));
			initSourcesTab(initTab(tabs, RM.getLabel("targetedition.sourcesgroup.title")));
			initCompressionTab(initTab(tabs, RM.getLabel("targetedition.compression.label")));
			initAdvancedTab(initTab(tabs, RM.getLabel("targetedition.advancedgroup.title")));
			initFiltersTab(initTab(tabs, RM.getLabel("targetedition.filtersgroup.title")));
			initPreProcessorsTab(initTab(tabs, RM.getLabel("targetedition.preprocessing.title")));
			initPostProcessorsTab(initTab(tabs, RM.getLabel("targetedition.postprocessing.title")));
			initDescriptionTab(initTab(tabs, RM.getLabel("targetedition.descriptiongroup.title")));

			SavePanel pnlSave = new SavePanel(this);
			Composite save = pnlSave.buildComposite(ret);
			GridData dt2 = new GridData();
			dt2.grabExcessHorizontalSpace = true;
			dt2.horizontalAlignment = SWT.FILL;
			save.setLayoutData(dt2);
			btnSave = pnlSave.getBtnSave();

			tabs.setSelection(0);
			ret.pack(true);
			initValues();
		} finally {
			application.disableWaitCursor();
		}
		return ret;
	}

	private Composite initTab(ListPane tabs, String title) {
		Composite itm = tabs.addElement(title, title);
		return itm;
	}

	private GridLayout initLayout(int nbCols) {
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.numColumns = nbCols;
		layout.marginHeight = 0;
		layout.verticalSpacing = 10;
		layout.horizontalSpacing = 10;
		return layout;
	}

	private void initGeneralTab(Composite composite) {
		composite.setLayout(initLayout(1));

		// NAME
		Group grpTargetName = new Group(composite, SWT.NONE);
		grpTargetName.setText(RM.getLabel("targetedition.targetnamefield.label"));
		grpTargetName.setLayout(new GridLayout(1, false));
		grpTargetName.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		txtTargetName = new Text(grpTargetName, SWT.BORDER);
		txtTargetName.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		monitorControl(txtTargetName);

		// PATH (FILE)
		Group grpPath = new Group(composite, SWT.NONE);
		grpPath.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		grpPath.setText(RM.getLabel("targetedition.storagedirfield.label"));
		grpPath.setToolTipText(RM.getLabel("targetedition.storagedirfield.tooltip"));
		grpPath.setLayout(new GridLayout(3, false));

		rdFile = new Button(grpPath, SWT.RADIO);
		rdFile.setText(RM.getLabel("targetedition.storage.file"));
		rdFile.setToolTipText(RM.getLabel("targetedition.storage.file.tt"));
		txtMediumPath = new Text(grpPath, SWT.BORDER);
		GridData dt = new GridData(SWT.FILL, SWT.CENTER, true, false);
		dt.minimumWidth = computeWidth(250);
		txtMediumPath.setLayoutData(dt);
		monitorControl(txtMediumPath);
		btnMediumPath = new Button(grpPath, SWT.PUSH);
		btnMediumPath.setText(RM.getLabel("common.browseaction.label"));
		btnMediumPath.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				String dir = txtMediumPath.getText();
				if (Utils.isEmpty(dir)) {
					dir = LocalPreferences.instance().get("target.lasttargetdir");
				}
				String path = Application.getInstance().showDirectoryDialog(dir, TargetEditionWindow.this);
				if (path != null) {
					LocalPreferences.instance().set("target.lasttargetdir", path);
					txtMediumPath.setText(path);
				}
			}
		});
		rdFile.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				processSelection(PLUGIN_HD, "");
			}
		});
		this.strButton.put(PLUGIN_HD, btnMediumPath);
		this.strText.put(PLUGIN_HD, txtMediumPath);
		this.strRadio.put(PLUGIN_HD, rdFile);

		// Plugins
		Iterator iter = StoragePluginRegistry.getInstance().getDisplayable().iterator();
		while (iter.hasNext()) {
			final StoragePlugin plugin = (StoragePlugin)iter.next();
			Button rd = new Button(grpPath, SWT.RADIO);
			rd.setText(plugin.getDisplayName() == null ? "UNDEFINED" : plugin.getDisplayName());
			rd.setToolTipText(plugin.getToolTip() == null ? "" : plugin.getToolTip());
			rd.addListener(SWT.Selection, new Listener(){
				public void handleEvent(Event event) {
					StorageSelectionHelper helper = plugin.getStorageSelectionHelper(); 
					helper.setWindow(TargetEditionWindow.this);
					helper.handleSelection();
					processSelection(plugin.getId(), "");
				}
			});
			this.strRadio.put(plugin.getId(), rd);

			final Text text = new Text(grpPath, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			text.setEditable(false);
			monitorControl(text);
			this.strText.put(plugin.getId(), text);

			Button btn = new Button(grpPath, SWT.PUSH);
			btn.setText(RM.getLabel("common.browseaction.label"));
			btn.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					StorageSelectionHelper helper = plugin.getStorageSelectionHelper(); 
					helper.setWindow(TargetEditionWindow.this);
					FileSystemPolicy newPolicy = helper.handleConfiguration();
					if (newPolicy != null) {
						currentPolicy = newPolicy;
						text.setText(currentPolicy.getDisplayableParameters());
						registerUpdate();                
					} 
				}
			});
			this.strButton.put(plugin.getId(), btn);
		}

		new Label(grpPath, SWT.NONE).setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));

		// Name
		lblArchiveName = new Label(grpPath, SWT.NONE);
		lblArchiveName.setText(RM.getLabel("targetedition.archivenamefield.label"));
		lblArchiveName.setToolTipText(RM.getLabel("targetedition.archivenamefield.tt"));
		txtArchiveName = new Text(grpPath, SWT.BORDER);
		txtArchiveName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		txtArchiveName.setToolTipText(RM.getLabel("targetedition.archivenamefield.tt"));
		monitorControl(txtArchiveName);

		// Type
		Group grpType = new Group(composite, SWT.NONE);
		grpType.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		grpType.setText(RM.getLabel("targetedition.typefield.label"));
		GridLayout gl = new GridLayout(1, false);
		gl.horizontalSpacing = 50;
		grpType.setLayout(gl);

		rdMultiple = new Button(grpType, SWT.RADIO);
		monitorControl(rdMultiple);
		rdMultiple.setText(RM.getLabel("targetedition.storagetype.multiple"));
		rdMultiple.setToolTipText(RM.getLabel("targetedition.storagetype.multiple.tt"));

		rdDelta = new Button(grpType, SWT.RADIO);
		monitorControl(rdDelta);
		rdDelta.setText(RM.getLabel("targetedition.storagetype.delta"));
		rdDelta.setToolTipText(RM.getLabel("targetedition.storagetype.delta.tt"));

		rdImage = new Button(grpType, SWT.RADIO);
		monitorControl(rdImage);
		rdImage.setText(RM.getLabel("targetedition.storagetype.image"));
		rdImage.setToolTipText(RM.getLabel("targetedition.storagetype.image.tt"));
	}

	private void initDescriptionTab(Composite composite) {
		composite.setLayout(initLayout(1));

		// DESC
		Group grpDesc = new Group(composite, SWT.NONE);
		grpDesc.setText(RM.getLabel("targetedition.descriptionfield.label"));
		grpDesc.setLayout(new GridLayout(1, false));
		grpDesc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		txtDesc = new Text(grpDesc, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		monitorControl(txtDesc);
		GridData dt = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		dt.widthHint = AbstractWindow.computeWidth(500);
		dt.heightHint = AbstractWindow.computeHeight(70);
		txtDesc.setLayoutData(dt);
	}

	private void initSourcesTab(Composite composite) {
		composite.setLayout(initLayout(4));

		TableViewer viewer = new TableViewer(composite, SWT.BORDER | SWT.MULTI);
		tblSources = viewer.getTable();
		GridData dt = new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1);
		dt.heightHint = computeHeight(100);
		tblSources.setLayoutData(dt);

		TableColumn col1 = new TableColumn(tblSources, SWT.NONE);
		col1.setText(RM.getLabel("targetedition.sourcedirfield.label"));
		col1.setWidth(AbstractWindow.computeWidth(400));
		col1.setMoveable(true);

		tblSources.setHeaderVisible(true);
		tblSources.setLinesVisible(AbstractWindow.getTableLinesVisible());

		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				editCurrentSource();
			}
		});

		tblSources.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent evt) {
				if (evt.character == SWT.DEL) {
					deleteCurrentSource();
				}
			}

			public void keyReleased(KeyEvent evt) {
			}
		});

		Label lblDnd = new Label(composite, SWT.NONE | SWT.WRAP);
		lblDnd.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1));
		lblDnd.setText(RM.getLabel("targetedition.sources.dnd.label"));

		btnAddSource = new Button(composite, SWT.PUSH);
		btnAddSource.setText(RM.getLabel("targetedition.addprocaction.label"));
		btnAddSource.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				File newFile = showSourceEditionFrame(null);
				if (newFile != null) {
					addSource(newFile);
					sortSources();
					registerUpdate();                
				}
			}
		});

		btnModifySource = new Button(composite, SWT.PUSH);
		btnModifySource.setText(RM.getLabel("targetedition.editprocaction.label"));
		btnModifySource.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				editCurrentSource();
			}
		});

		btnRemoveSource = new Button(composite, SWT.PUSH);
		btnRemoveSource.setText(RM.getLabel("targetedition.removeprocaction.label"));
		btnRemoveSource.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				deleteCurrentSource();
			}
		});

		tblSources.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				updateSourceListState();
			}
		});

		final int operation = DND.DROP_MOVE;
		Transfer[] types = new Transfer[] { FileTransfer.getInstance() };
		DropTarget target = new DropTarget(tblSources, operation);
		target.setTransfer(types);
		target.addDropListener(new DropTargetAdapter() {
			public void dragEnter(DropTargetEvent event) {
				event.detail = operation;
				event.feedback = DND.FEEDBACK_SCROLL;
			}

			public void dragOver(DropTargetEvent event) {
				event.detail = operation;
				event.feedback = DND.FEEDBACK_SCROLL;
			}

			public void drop(DropTargetEvent event) {
				String[] files = (String[])event.data;
				for (int i=0; i<files.length; i++) {
					addSource(new File(files[i]));	
				} 
				sortSources();
				registerUpdate();
			}
		});
	}

	private void deleteCurrentSource() {
		int[] idx = tblSources.getSelectionIndices();
		if (idx.length != 0) {
			int result = application.showConfirmDialog(
					RM.getLabel("targetedition.removesourceaction.confirm.message"),
					RM.getLabel("targetedition.confirmremovesource.title"));

			if (result == SWT.YES) {
				tblSources.remove(idx);
				tblSources.setSelection(Math.max(0, Math.min(tblSources.getItemCount() - 1, idx[0])));
				registerUpdate();                  
			}
		}
	}

	private void editCurrentSource() {
		if (tblSources.getSelectionIndex() != -1) {
			TableItem item = tblSources.getItem(tblSources.getSelectionIndex());
			File source = (File)item.getData();
			updateSource(item, showSourceEditionFrame(source));
			sortSources();
			registerUpdate();  
		}
	}

	private void enableZipOptions(boolean enable) {
		this.chkMultiVolumes.setEnabled(enable);
		if (! enable) {
			this.chkMultiVolumes.setSelection(false);
		} else if (! rdSingle.getSelection() && ! rdArchive.getSelection()) {
			this.rdArchive.setSelection(true);
		}
		this.txtZipComment.setEnabled(enable);
		this.chkAddExtension.setEnabled(enable);
		this.lblEncoding.setEnabled(enable);
		this.cboEncoding.setEnabled(enable);
		this.lblZipLevel.setEnabled(enable);
		this.cboZipLevel.setEnabled(enable);
		this.rdArchive.setEnabled(enable);
		this.rdSingle.setEnabled(enable);
		this.resetMVData();
	}

	private void initCompressionTab(Composite composite) {
		composite.setLayout(initLayout(2));

		// COMPRESSION
		grpCompression = new Group(composite, SWT.NONE);
		grpCompression.setText(RM.getLabel("targetedition.compression.label"));
		RowLayout lytCompression = new RowLayout(SWT.VERTICAL);
		grpCompression.setLayout(lytCompression);
		grpCompression.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		rdDir = new Button(grpCompression, SWT.RADIO);
		monitorControl(rdDir);
		rdDir.setText(RM.getLabel("targetedition.compression.none"));
		rdDir.setToolTipText(RM.getLabel("targetedition.compression.none.tt"));
		rdDir.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				enableZipOptions(false);
			}
		});

		rdZip = new Button(grpCompression, SWT.RADIO);
		monitorControl(rdZip);
		rdZip.setText(RM.getLabel("targetedition.compression.zip"));
		rdZip.setToolTipText(RM.getLabel("targetedition.compression.zip.tt"));
		rdZip.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				enableZipOptions(true);
			}
		});

		rdZip64 = new Button(grpCompression, SWT.RADIO);
		monitorControl(rdZip64);
		rdZip64.setText(RM.getLabel("targetedition.compression.zip64"));
		rdZip64.setToolTipText(RM.getLabel("targetedition.compression.zip64.tt"));
		rdZip64.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				enableZipOptions(true);
			}
		});

		// STORAGE
		grpStorage = new Group(composite, SWT.NONE);
		grpStorage.setText(RM.getLabel("targetedition.zipmode.label"));
		grpStorage.setLayout(new GridLayout(1, false));
		grpStorage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		rdArchive = new Button(grpStorage, SWT.RADIO);
		rdArchive.setText(RM.getLabel("targetedition.zip.archive.label"));
		rdArchive.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		rdSingle = new Button(grpStorage, SWT.RADIO);
		rdSingle.setText(RM.getLabel("targetedition.zip.unit.label"));
		rdSingle.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

		new Label(grpStorage, SWT.NONE);

		chkAddExtension = new Button(grpStorage, SWT.CHECK);
		chkAddExtension.setText(RM.getLabel("targetedition.addextension.label"));
		chkAddExtension.setToolTipText(RM.getLabel("targetedition.addextension.tt"));
		monitorControl(chkAddExtension);
		chkAddExtension.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

		// ZIP OPTIONS
		grpZipOptions = new Group(composite, SWT.NONE);
		grpZipOptions.setText(RM.getLabel("targetedition.zipoptions.label"));
		grpZipOptions.setLayout(new GridLayout(5, false));
		grpZipOptions.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		lblEncoding = new Label(grpZipOptions, SWT.NONE);
		lblEncoding.setText(RM.getLabel("targetedition.encoding.label") + "            ");
		lblEncoding.setToolTipText(RM.getLabel("targetedition.encoding.tt"));
		cboEncoding = new Combo(grpZipOptions, SWT.READ_ONLY);
		cboEncoding.setToolTipText(RM.getLabel("targetedition.encoding.tt"));
		for (int i=0; i<OSTool.getCharsets().length; i++) {
			cboEncoding.add(OSTool.getCharsets()[i].name());
		}
		monitorControl(cboEncoding);
		cboEncoding.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		Label lblb = new Label(grpZipOptions, SWT.NONE);
		lblb.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));

		lblZipLevel = new Label(grpZipOptions, SWT.NONE);
		lblZipLevel.setText(RM.getLabel("targetedition.ziplevel.label") + "            ");
		lblZipLevel.setToolTipText(RM.getLabel("targetedition.ziplevel.tt"));
		cboZipLevel = new Combo(grpZipOptions, SWT.READ_ONLY);
		cboZipLevel.setToolTipText(RM.getLabel("targetedition.ziplevel.tt"));
		for (int i=0; i<=9; i++) {
			cboZipLevel.add("" + i);
		}
		monitorControl(cboZipLevel);
		cboZipLevel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		Label lblb2 = new Label(grpZipOptions, SWT.NONE);
		lblb2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));

		chkMultiVolumes = new Button(grpZipOptions, SWT.CHECK);
		chkMultiVolumes.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		chkMultiVolumes.setText(RM.getLabel("targetedition.mv.label"));
		chkMultiVolumes.setToolTipText(RM.getLabel("targetedition.mv.tooltip"));
		monitorControl(chkMultiVolumes);
		chkMultiVolumes.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				resetMVData();
			}
		});

		txtMultiVolumes = new Text(grpZipOptions, SWT.BORDER);
		txtMultiVolumes.setToolTipText(RM.getLabel("targetedition.mv.size.tt"));
		monitorControl(txtMultiVolumes);
		txtMultiVolumes.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		lblMultiVolumesUnit = new Label(grpZipOptions, SWT.NONE);
		lblMultiVolumesUnit.setText(RM.getLabel("targetedition.mv.unit.label"));
		lblMultiVolumesUnit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		txtMultivolumesDigits = new Text(grpZipOptions, SWT.BORDER);
		monitorControl(txtMultivolumesDigits);
		GridData dtmvd = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
		dtmvd.widthHint = 20;
		txtMultivolumesDigits.setLayoutData(dtmvd);
		txtMultivolumesDigits.setToolTipText(RM.getLabel("targetedition.mv.digits.tt"));

		lblMultiVolumesDigits = new Label(grpZipOptions, SWT.NONE);
		lblMultiVolumesDigits.setText(RM.getLabel("targetedition.mv.digits.label"));
		lblMultiVolumesDigits.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		lblMultiVolumesDigits.setToolTipText(RM.getLabel("targetedition.mv.digits.tt"));       

		// ZIP COMMENT
		grpZipComment = new Group(composite, SWT.NONE);
		grpZipComment.setText(RM.getLabel("targetedition.zipcommentgrp.label"));
		grpZipComment.setLayout(new GridLayout(1, false));
		grpZipComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		txtZipComment = new Text(grpZipComment, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		monitorControl(txtZipComment);
		GridData dt = new GridData(SWT.FILL, SWT.FILL, true, true);
		txtZipComment.setLayoutData(dt);
	}

	private void initAdvancedTab(Composite composite) {
		composite.setLayout(initLayout(2));

		// FILE MANAGEMENT
		grpFileManagement = new Group(composite, SWT.NONE);
		grpFileManagement.setText(RM.getLabel("targetedition.filemanagement.label"));
		RowLayout lytFileManagement = new RowLayout(SWT.VERTICAL);
		grpFileManagement.setLayout(lytFileManagement);
		grpFileManagement.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

		chkTrackDirectories = new Button(grpFileManagement, SWT.CHECK);
		monitorControl(chkTrackDirectories);
		chkTrackDirectories.setText(RM.getLabel("targetedition.trackemptydirs.label"));
		chkTrackDirectories.setToolTipText(RM.getLabel("targetedition.trackemptydirs.tooltip"));

		chkTrackPermissions = new Button(grpFileManagement, SWT.CHECK);
		monitorControl(chkTrackPermissions);
		chkTrackPermissions.setText(RM.getLabel("targetedition.trackperms.label"));
		chkTrackPermissions.setToolTipText(RM.getLabel("targetedition.trackperms.tooltip"));

		chkFollowSubDirectories = new Button(grpFileManagement, SWT.CHECK);
		monitorControl(chkFollowSubDirectories);
		chkFollowSubDirectories.setText(RM.getLabel("targetedition.followsubdirs.label"));
		chkFollowSubDirectories.setToolTipText(RM.getLabel("targetedition.followsubdirs.tooltip"));

		chkFollowLinks = new Button(grpFileManagement, SWT.CHECK);
		monitorControl(chkFollowLinks);
		chkFollowLinks.setText(RM.getLabel("targetedition.followlinks.label"));
		chkFollowLinks.setToolTipText(RM.getLabel("targetedition.followlinks.tooltip"));
		if (OSTool.isSystemWindows()) {
			chkFollowLinks.setVisible(false);
		}

		// ENCRYPTION
		grpEncryption = new Group(composite, SWT.NONE);
		grpEncryption.setText(RM.getLabel("targetedition.encryption.label"));
		grpEncryption.setLayout(new GridLayout(3, false));
		grpEncryption.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		chkEncrypted = new Button(grpEncryption, SWT.CHECK);
		chkEncrypted.setText(RM.getLabel("targetedition.encryption.label"));
		chkEncrypted.setToolTipText(RM.getLabel("targetedition.encryption.tooltip"));
		monitorControl(chkEncrypted);
		chkEncrypted.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		chkEncrypted.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				resetEcryptionKey();
			}
		});
		chkEncrypNames = new Button(grpEncryption, SWT.CHECK);
		chkEncrypNames.setText(RM.getLabel("targetedition.encryptnames.label"));
		chkEncrypNames.setToolTipText(RM.getLabel("targetedition.encryptnames.tooltip"));
		chkEncrypNames.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1)); 

		lblEncryptionAlgorithm = new Label(grpEncryption, SWT.NONE);
		lblEncryptionAlgorithm.setText(RM.getLabel("targetedition.algorithmfield.label"));    

		EncryptionConfiguration recConf = EncryptionConfiguration.getParameters(EncryptionConfiguration.RECOMMENDED_ALGORITHM);
		if (recConf != null) {
			lblEncryptionAlgorithm.setToolTipText(RM.getLabel("targetedition.algorithmfield.tooltip", new Object[] {recConf.getAlgorithm()}));
		}

		cboEncryptionAlgorithm = new Combo(grpEncryption, SWT.READ_ONLY);
		monitorControl(cboEncryptionAlgorithm);
		cboEncryptionAlgorithm.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				handleAlgorithmModification();
			}
		});

		cboEncryptionAlgorithm.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		String[] algs = EncryptionConfiguration.getAvailableAlgorithms();
		for (int i=0; i<algs.length; i++) {
			String id = algs[i];
			EncryptionConfiguration conf = EncryptionConfiguration.getParameters(id);
			lstEncryptionAlgorithms.add(conf);
			cboEncryptionAlgorithm.add(conf.getFullName());
		}
		if (algs.length == 0) {
			chkEncrypted.setEnabled(false);
			grpEncryption.setEnabled(false);
		}

		lblEncryptionKey = new Label(grpEncryption, SWT.NONE);
		lblEncryptionKey.setText(RM.getLabel("targetedition.keyfield.label"));
		lblEncryptionKey.setToolTipText(RM.getLabel("targetedition.keyfield.tooltip"));
		txtEncryptionKey = new Text(grpEncryption, SWT.BORDER);
		txtEncryptionKey.setEchoChar('*');
		txtEncryptionKey.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				evaluatePassword();
			}
		});
		monitorControl(txtEncryptionKey);
		GridData dtenckey = new GridData(SWT.FILL, SWT.CENTER, true, false);
		txtEncryptionKey.setLayoutData(dtenckey);

		btnReveal = new Button(grpEncryption, SWT.PUSH);
		btnReveal.setText(RM.getLabel("targetedition.reveal.label"));
		btnReveal.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnReveal.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (txtEncryptionKey.getEchoChar() == '*') {
					txtEncryptionKey.setEchoChar('\0');
					btnReveal.setText(RM.getLabel("targetedition.mask.label"));
					grpEncryption.layout();
				} else {
					txtEncryptionKey.setEchoChar('*');
					btnReveal.setText(RM.getLabel("targetedition.reveal.label"));
					grpEncryption.layout();
				}
			}
		});

		new Label(grpEncryption, SWT.NONE);
		lblEncryptionExample = new Label(grpEncryption, SWT.NONE);
		lblEncryptionExample.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1)); 

		btnGenerateKey = new Button(grpEncryption, SWT.PUSH);
		btnGenerateKey.setText(RM.getLabel("targetedition.generatekey.label"));
		btnGenerateKey.setToolTipText(RM.getLabel("targetedition.generatekey.tooltip"));
		btnGenerateKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnGenerateKey.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				boolean ok = true;
				if (txtEncryptionKey.getText().trim().length() != 0) {
					int rep = Application.getInstance().showConfirmDialog(
							RM.getLabel("targetedition.generatekey.warningtext"), 
							RM.getLabel("targetedition.generatekey.warningtitle")
					);

					if (rep != SWT.YES) {
						ok = false;
					}
				}

				if (ok) {
					int index = cboEncryptionAlgorithm.getSelectionIndex();
					if (index != -1) {
						EncryptionConfiguration config = (EncryptionConfiguration)lstEncryptionAlgorithms.get(index);
						byte[] b = EncryptionUtil.generateRandomKey(config.getKeySize());
						txtEncryptionKey.setText(Util.base16Encode(b));
					}
				}
			}
		});

		lblQuality = new Label(grpEncryption, SWT.NONE);
		lblQuality.setText(RM.getLabel("targetedition.pwdquality.label"));
		lblQuality.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, true));
		pgbPwdQuality = new ProgressBar(grpEncryption, SWT.SMOOTH);
		pgbPwdQuality.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true, 2, 1));
		pgbPwdQuality.setMinimum(0);
		pgbPwdQuality.setMaximum(100);        

		// CONFIG
		grpConfiguration = new Group(composite, SWT.NONE);
		grpConfiguration.setText(RM.getLabel("targetedition.configuration.label"));
		grpConfiguration.setLayout(new GridLayout(1, false));
		grpConfiguration.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		chkNoXMLCopy = new Button(grpConfiguration, SWT.CHECK);
		chkNoXMLCopy.setText(RM.getLabel("targetedition.noxmlbackup.label"));
		chkNoXMLCopy.setToolTipText(RM.getLabel("targetedition.noxmlbackup.tooltip"));
		monitorControl(chkNoXMLCopy);
		chkNoXMLCopy.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
	}

	private void initFiltersTab(Composite composite) {
		composite.setLayout(initLayout(4));

		TreeViewer viewer = new TreeViewer(composite, SWT.BORDER | SWT.SINGLE);
		treFilters = viewer.getTree();
		GridData dt = new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1);
		dt.heightHint = computeHeight(50);
		treFilters.setLayoutData(dt);

		TreeColumn col1 = new TreeColumn(treFilters, SWT.NONE);
		col1.setText(RM.getLabel("targetedition.filterstable.type.label"));
		col1.setWidth(computeWidth(200));
		col1.setMoveable(true);

		TreeColumn col2 = new TreeColumn(treFilters, SWT.NONE);
		col2.setText(RM.getLabel("targetedition.filterstable.parameters.label"));
		col2.setWidth(computeWidth(200));
		col2.setMoveable(true);

		TreeColumn col3 = new TreeColumn(treFilters, SWT.NONE);
		col3.setText(RM.getLabel("targetedition.filterstable.mode.label"));
		col3.setWidth(computeWidth(100));
		col3.setMoveable(true);

		treFilters.setHeaderVisible(true);
		treFilters.setLinesVisible(AbstractWindow.getTableLinesVisible());

		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				editCurrentFilter();
			}
		});

		treFilters.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent evt) {
				if (evt.character == SWT.DEL) {
					deleteCurrentFilter();
				}
			}

			public void keyReleased(KeyEvent evt) {
			}
		});

		Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
		final int operation = DND.DROP_MOVE;

		final DragSource source = new DragSource(treFilters, operation);
		source.setTransfer(types);
		source.addDragListener(
				new DragSourceAdapter() {
					public void dragStart(DragSourceEvent event) {   
						TreeItem[] selection = treFilters.getSelection();
						if (selection.length > 0 && selection[0].getParentItem() != null) {
							event.doit = true;
							transfered = selection[0];
						} else {
							event.doit = false;
						}
					};
					public void dragSetData(DragSourceEvent event) {
						event.data = "dummy data";
					}
				}
		);

		DropTarget target = new DropTarget(treFilters, operation);
		target.setTransfer(types);
		target.addDropListener(new DropTargetAdapter() {
			public void dragEnter(DropTargetEvent event) {
				event.detail = DND.DROP_NONE;
				event.feedback = DND.FEEDBACK_NONE;
			}

			public void dragOver(DropTargetEvent event) {
				event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;

				TreeItem selected = getSelected(event);
				if (selected != null && (selected.getData() instanceof FilterGroup) && (! contains(transfered, selected))) {
					event.feedback |= DND.FEEDBACK_SELECT;
					event.detail = operation;
				} else {
					event.feedback |= DND.FEEDBACK_NONE;
					event.detail = DND.DROP_NONE;
				}
			}

			public void drop(DropTargetEvent event) {
				TreeItem targetItem = getSelected(event);
				if (targetItem != null) {
					FilterGroup target = (FilterGroup)targetItem.getData();
					ArchiveFilter filter = (ArchiveFilter)transfered.getData();
					FilterGroup parent = (FilterGroup)transfered.getParentItem().getData();

					parent.remove(filter);
					target.addFilter(filter);

					updateFilterData(null);
					expandAll(treFilters.getItem(0));
				}
			}
		});

		btnAddFilter = new Button(composite, SWT.PUSH);
		btnAddFilter.setText(RM.getLabel("targetedition.addfilteraction.label"));
		btnAddFilter.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				if (treFilters.getSelectionCount() != 0) {
					TreeItem parentItem = treFilters.getSelection()[0];
					ArchiveFilter parent = (ArchiveFilter)parentItem.getData();

					if (parent instanceof FilterGroup) {
						ArchiveFilter newFilter = showFilterEditionFrame(null);

						if (newFilter != null) {
							((FilterGroup)parent).addFilter(newFilter);

							addFilter(parentItem, newFilter);
							expandAll(treFilters.getItem(0));
							registerUpdate();  
						}
					}
				}
			}
		});

		btnModifyFilter = new Button(composite, SWT.PUSH);
		btnModifyFilter.setText(RM.getLabel("targetedition.editfilteraction.label"));
		btnModifyFilter.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				editCurrentFilter();
			}
		});

		btnRemoveFilter = new Button(composite, SWT.PUSH);
		btnRemoveFilter.setText(RM.getLabel("targetedition.removefilteraction.label"));
		btnRemoveFilter.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				deleteCurrentFilter();
			}
		});

		treFilters.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				updateFilterListState();
			}
		});
	}

	private void deleteCurrentFilter() {
		if (treFilters.getSelectionCount() != 0) {
			TreeItem item = treFilters.getSelection()[0];
			TreeItem parentItem = item.getParentItem();

			if (parentItem != null) {
				int result = application.showConfirmDialog(
						RM.getLabel("targetedition.removefilteraction.confirm.message"),
						RM.getLabel("targetedition.removefilteraction.confirm.title"));

				if (result == SWT.YES) {
					FilterGroup fg = (FilterGroup)parentItem.getData();
					ArchiveFilter filter = (ArchiveFilter)item.getData();
					fg.remove(filter);

					updateFilterData(parentItem);
					expandAll(treFilters.getItem(0));
					registerUpdate();   
				}                  
			}
		}

	}

	private void editCurrentFilter() {
		if (treFilters.getSelectionCount() != 0) {
			TreeItem item = treFilters.getSelection()[0];
			ArchiveFilter filter = (ArchiveFilter)item.getData();
			showFilterEditionFrame(filter);

			updateFilterData(item.getParentItem());
			expandAll(treFilters.getItem(0));
			registerUpdate();  
		}
	}

	private TreeItem getSelected(DropTargetEvent event) {
		try {
			TreeItem selected = (TreeItem)event.item;
			if (! OSTool.isSystemWindows()) {
				// There is a bug in SWT under Linux :
				// If the Tree has been configured to display column headers ("setHeaderVisible(true)"), 
				// the "item" attribute of the DropTargetEvent references the TreeItem UNDER the actually selected item !
				// That's why we must "play" with the item's coordinates :(
				int x = selected.getBounds().x + 2;
				int y = selected.getBounds().y - selected.getBounds().height + 2;

				selected = treFilters.getItem(new Point(x, y));
			}
			return selected;
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static boolean contains(TreeItem parent, TreeItem child) {
		TreeItem item = child;
		while (item != null) {
			if (item.getData().equals(parent.getData())) {
				return true;
			}
			item = item.getParentItem();
		}
		return false;
	}

	private void initPostProcessorsTab(Composite composite) {
		this.postProcessesTab = new ProcessorsTable(composite, this, false);
	}

	private void initPreProcessorsTab(Composite composite) {
		this.preProcessesTab = new ProcessorsTable(composite, this, true);
	}

	public void publicRegisterUpdate() {
		registerUpdate();
	}

	protected void registerUpdate() {
		super.registerUpdate();

		updateFilterListState();
		this.preProcessesTab.updateProcListState();
		this.postProcessesTab.updateProcListState();
		this.updateSourceListState();
	}

	protected void updateFilterListState() {
		boolean selected = (this.treFilters.getSelectionCount() > 0);

		this.btnRemoveFilter.setEnabled(selected && this.treFilters.getSelection()[0].getParentItem() != null);
		this.btnModifyFilter.setEnabled(selected);        
		this.btnAddFilter.setEnabled(selected && this.treFilters.getSelection()[0].getData() instanceof FilterGroup);
	}

	protected void updateSourceListState() {
		int[] idx =  this.tblSources.getSelectionIndices();
		this.btnRemoveSource.setEnabled(idx.length != 0);
		this.btnModifySource.setEnabled(idx.length == 1);       
	}

	private void initValues() {
		// INIT VALUES
		if (target != null) {
			txtTargetName.setText(target.getName());
			txtDesc.setText(target.getComments());

			AbstractIncrementalFileSystemMedium fMedium = (AbstractIncrementalFileSystemMedium)target.getMedium();

			if (fMedium.isOverwrite()) {
				rdImage.setSelection(true);
			} else if (fMedium.getHandler() instanceof DeltaArchiveHandler) {
				rdDelta.setSelection(true);
			} else {
				rdMultiple.setSelection(true);
			}

			chkTrackDirectories.setSelection(((FileSystemTarget)target).isTrackEmptyDirectories());
			chkFollowSubDirectories.setSelection(((FileSystemTarget)target).isFollowSubdirectories());
			chkTrackPermissions.setSelection(fMedium.isTrackPermissions());
			chkNoXMLCopy.setSelection(! target.isCreateSecurityCopyOnBackup());
			chkFollowLinks.setSelection( ! ((FileSystemTarget)target).isTrackSymlinks());

			if (fMedium.getCompressionArguments().isCompressed()) {
				if (fMedium.getCompressionArguments().isMultiVolumes()) {
					chkMultiVolumes.setSelection(true);
					txtMultiVolumes.setText("" + fMedium.getCompressionArguments().getVolumeSize());
					txtMultivolumesDigits.setText("" + fMedium.getCompressionArguments().getNbDigits());
				}

				if (fMedium.getCompressionArguments().getComment() != null) {
					txtZipComment.setText(fMedium.getCompressionArguments().getComment());
				}

				selectEncoding(
						fMedium.getCompressionArguments().getCharset() != null ? 
								fMedium.getCompressionArguments().getCharset().name() : 
									ZipConstants.DEFAULT_CHARSET
				);

				// Compression level
				int cpr = fMedium.getCompressionArguments().getLevel() == -1 ? 9 : fMedium.getCompressionArguments().getLevel();
				cboZipLevel.select(cpr);

				chkAddExtension.setSelection(fMedium.getCompressionArguments().isAddExtension());

				if (fMedium.getCompressionArguments().isUseZip64()) {
					rdZip64.setSelection(true);
				} else {
					rdZip.setSelection(true);
				}

				if (IncrementalZipMedium.class.isAssignableFrom(fMedium.getClass())) {
					rdArchive.setSelection(true);
				} else {
					rdSingle.setSelection(true);
				}
			} else {
				rdDir.setSelection(true);
			}

			if (fMedium.getFileSystemPolicy() instanceof DefaultFileSystemPolicy) {
				DefaultFileSystemPolicy policy = (DefaultFileSystemPolicy)fMedium.getFileSystemPolicy();
				this.rdFile.setSelection(true);
				processSelection(PLUGIN_HD, FileSystemManager.getParent(new File(policy.getArchivePath())));
			} else {
				FileSystemPolicy clone = (FileSystemPolicy)fMedium.getFileSystemPolicy().duplicate();
				String id = clone.getId();
				Button rd = (Button)this.strRadio.get(id);
				rd.setSelection(true);
				processSelection(id, clone.getDisplayableParameters());
				this.currentPolicy = clone;
			}

			txtArchiveName.setText(fMedium.getFileSystemPolicy().getArchiveName());
			chkEncrypted.setSelection(fMedium.getEncryptionPolicy().isEncrypted());
			if (fMedium.getEncryptionPolicy().isEncrypted()) {
				txtEncryptionKey.setText(fMedium.getEncryptionPolicy().getEncryptionKey());
				chkEncrypNames.setSelection(fMedium.getEncryptionPolicy().isEncryptNames());
				String algoId = fMedium.getEncryptionPolicy().getEncryptionAlgorithm();

				if (EncryptionConfiguration.validateAlgorithmId(algoId)) {
					for (int i=0; i<lstEncryptionAlgorithms.size(); i++) {
						EncryptionConfiguration conf = (EncryptionConfiguration)lstEncryptionAlgorithms.get(i);
						if (conf.getId().equals(algoId)) {
							cboEncryptionAlgorithm.select(i);
							break;
						}
					}
				} else {
					cboEncryptionAlgorithm.deselectAll();
				}
				handleAlgorithmModification();
			}

			// INIT SOURCES
			Iterator sources = ((FileSystemTarget)target).getSources().iterator();
			while (sources.hasNext()) {
				addSource((File)sources.next());
			}
			sortSources();

			// INIT FILTERS
			this.mdlFilters = (FilterGroup)target.getFilterGroup().duplicate();
			addFilter(null, this.mdlFilters);

			// INIT PROCS
			this.preProcessesTab.setProcessors(target.getPreProcessors());
			this.postProcessesTab.setProcessors(target.getPostProcessors());
		} else {     
			// Default settings
			rdZip64.setSelection(true);
			rdFile.setSelection(true);
			rdMultiple.setSelection(true);
			chkFollowSubDirectories.setSelection(true);
			rdSingle.setSelection(true);
			selectEncoding(ZipConstants.DEFAULT_CHARSET);
			chkTrackDirectories.setSelection(true);
			chkTrackPermissions.setSelection(true);
			cboZipLevel.select(9);
			if (OSTool.isSystemWindows()) {
				this.chkFollowLinks.setSelection(true);
			}
			processSelection(PLUGIN_HD, ArecaPreferences.getDefaultArchiveStorage());
			txtArchiveName.setText(DEFAULT_ARCHIVE_PATTERN);

			// Default filters
			this.mdlFilters = new FilterGroup();
			mdlFilters.setAnd(true);
			mdlFilters.setLogicalNot(false);

			FileExtensionArchiveFilter filter1 = new FileExtensionArchiveFilter();
			filter1.acceptParameters("*.tmp, *.temp");
			filter1.setLogicalNot(true);
			mdlFilters.addFilter(filter1);

			LockedFileFilter filter2 = new LockedFileFilter();
			filter2.setLogicalNot(true);
			mdlFilters.addFilter(filter2);

			addFilter(null, mdlFilters);
		}

		expandAll(treFilters.getItem(0));
		this.resetEcryptionKey();
		this.resetMVData();
		enableZipOptions(! (rdDir.getSelection()));
		handleAlgorithmModification();

		// FREEZE
		if (isFrozen(true)) {
			Iterator iter = this.strRadio.keySet().iterator();
			while (iter.hasNext()) {
				String id = (String)iter.next();
				Button rd = (Button)strRadio.get(id);
				Button btn = (Button)strButton.get(id);
				Text txt = (Text)strText.get(id);

				btn.setEnabled(false);
				txt.setEnabled(false);
				rd.setEnabled(false);
			}

			grpCompression.setEnabled(false);
			grpEncryption.setEnabled(false);
			grpZipOptions.setEnabled(false);
			grpZipComment.setEnabled(false);
			grpStorage.setEnabled(false);
			grpFileManagement.setEnabled(false);
			grpConfiguration.setEnabled(false);

			rdDir.setEnabled(false);
			rdZip.setEnabled(false);
			rdZip64.setEnabled(false);
			chkEncrypted.setEnabled(false);
			chkMultiVolumes.setEnabled(false);
			rdMultiple.setEnabled(false);
			rdDelta.setEnabled(false);
			rdImage.setEnabled(false);
			cboEncryptionAlgorithm.setEnabled(false);
			lblEncryptionAlgorithm.setEnabled(false);
			lblEncryptionKey.setEnabled(false);
			lblQuality.setEnabled(false);
			txtArchiveName.setEnabled(false);
			lblArchiveName.setEnabled(false);
			lblEncryptionExample.setEnabled(false);
			txtEncryptionKey.setEnabled(false);
			chkEncrypNames.setEnabled(false);
			txtMultiVolumes.setEnabled(false);
			txtMultivolumesDigits.setEnabled(false);
			lblMultiVolumesUnit.setEnabled(false);
			lblMultiVolumesDigits.setEnabled(false);
			chkFollowSubDirectories.setEnabled(false);
			chkTrackDirectories.setEnabled(false);
			chkTrackPermissions.setEnabled(false);
			chkNoXMLCopy.setEnabled(false);
			chkFollowLinks.setEnabled(false);
			txtZipComment.setEnabled(false);
			chkAddExtension.setEnabled(false);
			lblEncoding.setEnabled(false);
			cboEncoding.setEnabled(false);
			cboZipLevel.setEnabled(false);
			lblZipLevel.setEnabled(false);
			rdArchive.setEnabled(false);
			rdSingle.setEnabled(false);
			btnReveal.setEnabled(false);
			btnGenerateKey.setEnabled(false);
			pgbPwdQuality.setEnabled(false);
		}    
	}

	private void selectEncoding(String encoding) {
		if (encoding != null) {
			for (int i=0; i<cboEncoding.getItemCount(); i++) {
				if (cboEncoding.getItem(i).equals(encoding)) {
					cboEncoding.select(i);
					break;
				}
			}
		}
	}

	private ArchiveFilter showFilterEditionFrame(ArchiveFilter filter) {
		FilterEditionWindow frm = new FilterEditionWindow(filter, (FileSystemTarget)this.getTarget());
		showDialog(frm);
		ArchiveFilter ft = frm.getCurrentFilter();
		return ft;
	}

	private File showSourceEditionFrame(File source) {
		SourceEditionWindow frm = new SourceEditionWindow(source, (FileSystemTarget)this.getTarget());
		showDialog(frm);
		return frm.getSource();
	}

	private void addFilter(TreeItem parent, ArchiveFilter filter) {
		TreeItem item;
		if (parent == null) {
			item = new TreeItem(treFilters, SWT.NONE);
		} else {
			item = new TreeItem(parent, SWT.NONE);
		}
		item.setData(filter);

		updateFilterData(item);
	}

	private void updateFilterData(TreeItem item) {
		if (item == null) {
			item = treFilters.getItem(0);
		}

		boolean isFirst = (
				item.getParentItem() == null 
				|| item.getParentItem().getItemCount() == 0 
				|| item.getParentItem().getItem(0).getData().equals(item.getData())
		);

		item.removeAll();

		ArchiveFilter filter = (ArchiveFilter)item.getData();
		TreeItem parent = item.getParentItem();

		String prefix = "";
		if (! isFirst) {
			prefix = (RM.getLabel(((FilterGroup)parent.getData()).isAnd() ? "common.operator.and" : "common.operator.or") + " ");
		}

		String filterExclude = RM.getLabel(
				filter.isLogicalNot() ? "filteredition.exclusion.label" : "filteredition.inclusion.label"
		);
		item.setText(0, prefix + FilterRepository.getName(filter.getClass()));
		item.setText(1, filter.getStringParameters() == null ? "" : filter.getStringParameters());
		item.setText(2, filterExclude);

		if (filter instanceof FilterGroup) {
			Iterator iter = ((FilterGroup)filter).getFilterIterator();
			while (iter.hasNext()) {
				ArchiveFilter child = (ArchiveFilter)iter.next();
				addFilter(item, child);
			}
		}
	}

	private static void expandAll(TreeItem item) {
		item.setExpanded(true);
		TreeItem[] children = item.getItems();
		for (int i=0; i<children.length; i++) {
			expandAll(children[i]);
		}
	}

	private void addSource(File source) {
		TableItem item = new TableItem(tblSources, SWT.NONE);
		updateSource(item, source);
	}

	private void sortSources() {
		TableItem[] items = tblSources.getItems();    	
		File[] files = new File[items.length];
		for (int i=0; i<items.length; i++) {
			files[i] = (File)items[i].getData();
		}

		Arrays.sort(files, new FileComparator());
		tblSources.setItemCount(0);

		File pred = null;
		for (int i=0; i<files.length; i++) {
			if (! files[i].equals(pred)) {
				TableItem item = new TableItem(tblSources, SWT.NONE);
				updateSource(item, files[i]);
				pred = files[i];
			}
		}
	}

	private void updateSource(TableItem item, File source) {
		item.setText(0, FileSystemManager.getAbsolutePath(source));
		item.setData(source);
	}

	private void processSelection(String refId, String s) {
		this.currentPolicy = null;
		this.currentFileSystemPolicyId = refId;

		Iterator iter = this.strRadio.keySet().iterator();
		while (iter.hasNext()) {
			String id = (String)iter.next();
			Button btn = (Button)strButton.get(id);
			Text txt = (Text)strText.get(id);

			if (refId.equals(id)) {
				btn.setEnabled(true);
				txt.setEnabled(true);
				if (s != null) {
					txt.setText(s);
				}
			} else {
				btn.setEnabled(false);
				txt.setEnabled(false);
				txt.setText("");
			}
		}
	}

	protected boolean checkBusinessRules() {
		// - TARGET NAME
		this.resetErrorState(txtTargetName);        
		if (this.txtTargetName.getText() == null || this.txtTargetName.getText().length() == 0) {
			this.setInError(txtTargetName);
			return false;
		}  

		// - STORAGE + valider qu'il n'est pas un sous repertoire des repertoires sources
		Text txt = (Text)this.strText.get(this.currentFileSystemPolicyId);
		Button rd = (Button)this.strRadio.get(this.currentFileSystemPolicyId);
		this.resetErrorState(txt);
		if (rd.getSelection()) {
			if (PLUGIN_HD.equals(this.currentFileSystemPolicyId)) {
				if (this.txtMediumPath.getText() == null || this.txtMediumPath.getText().length() == 0) {
					this.setInError(txtMediumPath);
					return false;
				} else {
					for (int i=0; i<this.tblSources.getItemCount(); i++) {
						File src  =(File)this.tblSources.getItem(i).getData();

						File backupDir = new File(this.txtMediumPath.getText());
						FileTool tool = FileTool.getInstance();
						if (AbstractFileSystemMedium.CHECK_DIRECTORY_CONSISTENCY && tool.isParentOf(src, backupDir)) {
							this.setInError(txtMediumPath);
							return false;           
						}
					}
				}  
			} else if (currentPolicy == null) {
				this.setInError(txt);
				return false;
			}
		}

		// - ARCHIVE NAME
		this.resetErrorState(txtArchiveName);        
		if (this.txtArchiveName.getText() == null || this.txtArchiveName.getText().length() == 0) {
			this.setInError(txtArchiveName);
			return false;
		}  

		// MULTI-VOLUMES
		this.resetErrorState(txtMultiVolumes);
		this.resetErrorState(txtMultivolumesDigits);
		if (this.chkMultiVolumes.getSelection()) {
			if (
					this.txtMultiVolumes.getText() == null 
					|| this.txtMultiVolumes.getText().length() == 0    
			) {
				this.setInError(txtMultiVolumes);
				return false;
			} else {
				try {
					Long.parseLong(this.txtMultiVolumes.getText());
				} catch (NumberFormatException e) {
					this.setInError(txtMultiVolumes);
					return false;
				}
			}

			if (
					this.txtMultivolumesDigits.getText() == null 
					|| this.txtMultivolumesDigits.getText().length() == 0    
			) {
				this.setInError(txtMultivolumesDigits);
				return false;
			} else {
				try {
					Integer.parseInt(this.txtMultivolumesDigits.getText());
				} catch (NumberFormatException e) {
					this.setInError(txtMultivolumesDigits);
					return false;
				}
			}
		}

		// CRYPTAGE
		this.resetErrorState(cboEncryptionAlgorithm);
		if (
				this.chkEncrypted.getSelection()
				&& (! this.isFrozen(false))
				&& (this.cboEncryptionAlgorithm.getSelectionIndex() == -1)
		) {
			this.setInError(cboEncryptionAlgorithm);
			return false;
		}    

		this.resetErrorState(txtEncryptionKey);
		if (this.chkEncrypted.getSelection()) {
			int index = this.cboEncryptionAlgorithm.getSelectionIndex();
			if (index != -1) {
				EncryptionConfiguration config = (EncryptionConfiguration)lstEncryptionAlgorithms.get(index);
				if (! EncryptionPolicy.validateEncryptionKey(txtEncryptionKey.getText(), config)) {
					setInError(this.txtEncryptionKey);
					return false;
				}
			}
		}

		return true;        
	}

	private void resetMVData() {
		if (this.chkMultiVolumes.getSelection()) {
			this.txtMultiVolumes.setEditable(true);
			this.txtMultiVolumes.setEnabled(true);
			this.lblMultiVolumesUnit.setEnabled(true);
			this.lblMultiVolumesDigits.setEnabled(true);
			this.txtMultivolumesDigits.setEditable(true);
			this.txtMultivolumesDigits.setEnabled(true);
		} else {
			this.txtMultiVolumes.setEditable(false);
			this.txtMultiVolumes.setBackground(null);
			this.txtMultiVolumes.setEnabled(false);
			if (txtMultiVolumes.getText() != null && txtMultiVolumes.getText().length() != 0) {
				this.txtMultiVolumes.setText("");
			}
			this.lblMultiVolumesUnit.setEnabled(false);
			this.lblMultiVolumesDigits.setEnabled(false);

			this.txtMultivolumesDigits.setEditable(false);
			this.txtMultivolumesDigits.setBackground(null);
			this.txtMultivolumesDigits.setEnabled(false);
			if (txtMultivolumesDigits.getText() != null && txtMultivolumesDigits.getText().length() != 0) {
				this.txtMultivolumesDigits.setText("");
			}
		}

		if (chkMultiVolumes.getSelection()) {
			chkAddExtension.setSelection(true);
		}
		chkAddExtension.setEnabled((! chkMultiVolumes.getSelection()) && chkMultiVolumes.isEnabled());
	}

	private void handleAlgorithmModification() {
		int index = cboEncryptionAlgorithm.getSelectionIndex();
		if (index != -1) {
			EncryptionConfiguration config = (EncryptionConfiguration)lstEncryptionAlgorithms.get(index);
			String configId = config.getId();
			String example = RM.getLabel("targetedition.encryption." + configId.toLowerCase() + ".example");
			this.lblEncryptionExample.setText(example);

			boolean isRaw = config.getKeyConvention().equals(EncryptionConfiguration.KEYCONV_RAW);
			btnGenerateKey.setVisible(isRaw);
			btnGenerateKey.setEnabled(true);

			pgbPwdQuality.setVisible(!isRaw);
			pgbPwdQuality.setEnabled(true);
			lblQuality.setVisible(!isRaw);
			lblQuality.setEnabled(true);
			evaluatePassword();
		} else {
			btnGenerateKey.setVisible(false);
			pgbPwdQuality.setVisible(false);
			lblQuality.setVisible(false);
			this.lblEncryptionExample.setText("");
		}
	}

	private void evaluatePassword() {
		double quality = PasswordQualityEvaluator.evaluate(txtEncryptionKey.getText());
		this.pgbPwdQuality.setSelection((int)(quality*100));
	}

	private void resetEcryptionKey() {
		if (this.chkEncrypted.getSelection()) {
			this.txtEncryptionKey.setEditable(true);
			this.txtEncryptionKey.setEnabled(true);
			this.cboEncryptionAlgorithm.setEnabled(true);
			this.lblEncryptionAlgorithm.setEnabled(true);
			this.chkEncrypNames.setEnabled(true);
			this.lblEncryptionExample.setEnabled(true);
			this.lblEncryptionKey.setEnabled(true);
			this.btnReveal.setEnabled(true);
			handleAlgorithmModification();
		} else {
			this.txtEncryptionKey.setEditable(false);
			this.txtEncryptionKey.setEnabled(false);
			this.txtEncryptionKey.setBackground(null);
			if (txtEncryptionKey.getText() != null && txtEncryptionKey.getText().length() != 0) {
				this.txtEncryptionKey.setText("");
			}
			this.cboEncryptionAlgorithm.setEnabled(false);
			this.lblEncryptionExample.setEnabled(false);
			this.chkEncrypNames.setEnabled(false);
			this.cboEncryptionAlgorithm.setBackground(null);
			this.lblEncryptionAlgorithm.setEnabled(false);
			this.lblEncryptionKey.setEnabled(false);
			this.btnGenerateKey.setEnabled(false);
			this.pgbPwdQuality.setEnabled(false);
			this.lblQuality.setEnabled(false);
			this.btnReveal.setEnabled(false);
		}
	}

	/**
	 * Indique si certaines zones sont desactivees ou non
	 * @return
	 */
	protected boolean isFrozen(boolean showWarning) {
		if (target == null) {
			return false;
		} else {
			try {
				return (((AbstractFileSystemMedium)target.getMedium()).listArchives(null, null).length != 0);
			} catch (Throwable e) {
				if (showWarning) {
					this.application.handleException(RM.getLabel("targetedition.frozen.message"), e);
				} else {
					Logger.defaultLogger().error(e);
				}
				return false;
			}
		}
	}

	public AbstractTarget getTarget() {
		return target;
	}

	public AbstractTarget getTargetIfValidated() {
		if (this.hasBeenSaved) {
			return target;
		} else {
			return null;
		}
	}

	public String getTitle() {
		return TITLE;
	}

	protected void saveChanges() {
		try {
			FileSystemTarget newTarget = new FileSystemTarget();
			String storageSubDirectory; // Necessary for backward compatibility
			if (target != null) {
				newTarget.setId(target.getId());
				newTarget.setUid(target.getUid());

				// Necessary for backward compatibility
				File fStorageSubDirectoryFile = ((AbstractIncrementalFileSystemMedium)target.getMedium()).getFileSystemPolicy().getArchiveDirectory();
				storageSubDirectory = FileSystemManager.getName(fStorageSubDirectoryFile);
			} else {
				// Should be the standard behavior, but a workaround is necessary for backward compatibility
				storageSubDirectory = newTarget.getUid();
			}

			newTarget.setComments(this.txtDesc.getText());
			newTarget.setTargetName(txtTargetName.getText());
			newTarget.setCreateSecurityCopyOnBackup(! chkNoXMLCopy.getSelection());
			newTarget.setTrackSymlinks( ! this.chkFollowLinks.getSelection());
			newTarget.setTrackEmptyDirectories(this.chkTrackDirectories.getSelection());
			newTarget.setFollowSubdirectories(this.chkFollowSubDirectories.getSelection());

			// Sources
			HashSet sources = new HashSet();
			for (int i=0; i<this.tblSources.getItemCount(); i++) {
				sources.add((File)this.tblSources.getItem(i).getData());
			}
			newTarget.setSources(sources);

			if (isFrozen(false)) {
				newTarget.setMedium(target.getMedium(), false);
			} else {
				boolean isEncrypted = this.chkEncrypted.getSelection();
				EncryptionPolicy encrArgs = new EncryptionPolicy();
				encrArgs.setEncrypted(isEncrypted);
				if (isEncrypted) {
					String encryptionKey = this.txtEncryptionKey.getText();
					EncryptionConfiguration config = (EncryptionConfiguration)lstEncryptionAlgorithms.get(cboEncryptionAlgorithm.getSelectionIndex());
					encrArgs.setEncryptionAlgorithm(config.getId());
					encrArgs.setEncryptNames(chkEncrypNames.getSelection());
					encrArgs.setEncryptionKey(encryptionKey);
				}

				AbstractIncrementalFileSystemMedium medium = null;
				FileSystemPolicy storagePolicy;
				if (this.currentPolicy != null) {
					storagePolicy = this.currentPolicy;
				} else {
					storagePolicy = new DefaultFileSystemPolicy();

					((DefaultFileSystemPolicy)storagePolicy).setId(PLUGIN_HD);
					String archivePath = this.txtMediumPath.getText() + "/" + storageSubDirectory;
					((DefaultFileSystemPolicy)storagePolicy).setArchivePath(archivePath);
				}
				storagePolicy.setArchiveName(txtArchiveName.getText());
				storagePolicy.validate(false);

				// Clear the history - it will be written after the drivers have been initialized
				History historyBck = null;
				if (target != null) {
					historyBck = this.target.getMedium().getHistoryHandler().readHistory();
					try {
						this.target.getMedium().getHistoryHandler().clearData();
					} catch (Exception e) {
						Logger.defaultLogger().error("Error trying to clear the target's history", e);
						// Non-blocking error.
					}
				}

				CompressionArguments compression = new CompressionArguments();
				compression.setUseZip64(this.rdZip64.getSelection());
				compression.setComment(this.txtZipComment.getText());
				compression.setAddExtension(this.chkAddExtension.getSelection());

				if (this.chkMultiVolumes.getSelection()) {
					compression.setMultiVolumes(Long.parseLong(txtMultiVolumes.getText()), Integer.parseInt(txtMultivolumesDigits.getText()));
				}

				if (cboEncoding.getSelectionIndex() != -1) {
					compression.setCharset(Charset.forName(cboEncoding.getItem(cboEncoding.getSelectionIndex())));
				}

				if (cboZipLevel.getSelectionIndex() != -1) {
					compression.setLevel(cboZipLevel.getSelectionIndex());
				}

				if ((! this.rdDir.getSelection()) && this.rdArchive.getSelection()) {
					medium = new IncrementalZipMedium();
				} else {
					medium = new IncrementalDirectoryMedium();   
				}

				if (this.rdDir.getSelection()) {
					compression.setCompressed(false);
				} else if (this.rdZip.getSelection() || this.rdZip64.getSelection()) {
					compression.setCompressed(true);
				}
				medium.setCompressionArguments(compression);
				medium.setFileSystemPolicy(storagePolicy);
				medium.setEncryptionPolicy(encrArgs);
				medium.setTrackPermissions(this.chkTrackPermissions.getSelection());

				if (rdDelta.getSelection()) {
					medium.setHandler(new DeltaArchiveHandler());
				} else {
					medium.setHandler(new DefaultArchiveHandler());
					medium.setOverwrite(this.rdImage.getSelection());
				}

				newTarget.setMedium(medium, false);
				medium.install();

				if (historyBck != null && ! historyBck.isEmpty()) {
					// Write the history
					try {
						newTarget.getMedium().getHistoryHandler().writeHistory(historyBck);
					} catch (Throwable e) {
						Logger.defaultLogger().error("Error during user history import.", e);
					}
				}
			}
			newTarget.setFilterGroup(this.mdlFilters);

			preProcessesTab.addProcessors(newTarget.getPreProcessors());
			postProcessesTab.addProcessors(newTarget.getPostProcessors());

			this.target = newTarget;
		} catch (Exception e) {
			this.application.handleException(RM.getLabel("error.updateprocess.message", new Object[] {e.getMessage()}), e);
		}

		this.hasBeenSaved = true;
		this.hasBeenUpdated = false;
		this.close();
	}

	protected void updateState(boolean rulesSatisfied) {
		btnSave.setEnabled(rulesSatisfied);
	}

	public void showDialog(AbstractWindow window) {
		window.setModal(this);
		window.setBlockOnOpen(true);
		window.open();
	}
}
