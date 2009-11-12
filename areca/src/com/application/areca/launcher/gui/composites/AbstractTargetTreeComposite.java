package com.application.areca.launcher.gui.composites;

import java.util.Iterator;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.application.areca.AbstractTarget;
import com.application.areca.TargetGroup;
import com.application.areca.Workspace;
import com.application.areca.WorkspaceItem;
import com.application.areca.launcher.gui.common.ArecaImages;

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
public abstract class AbstractTargetTreeComposite 
extends Composite 
implements MouseListener, Listener {
    protected Tree tree;
    protected TreeViewer viewer;

    public AbstractTargetTreeComposite(Composite parent, boolean multi) {
        super(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        setLayout(layout);
        
        int style = SWT.BORDER;
        if (multi) {
        	style |= SWT.MULTI;
        }
        
        viewer = new TreeViewer(this, style);
        tree = viewer.getTree();
        tree.addMouseListener(this);
        tree.addListener(SWT.Selection, this);
        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        refresh();
    }
    
    protected abstract Workspace getWorkspace();
    protected abstract String getCurrentId();

    public void refresh() {
        tree.removeAll();
        String currentObjectId = getCurrentId();

        if (getWorkspace() != null) {
        	Iterator iter = getWorkspace().getContent().getSortedIterator();
        	while (iter.hasNext()) {
        		TreeItem node = new TreeItem(tree, SWT.NONE);
        		Object o = iter.next();
        		
        		if (o instanceof TargetGroup) {
            		fillGroupData(node, (TargetGroup)o, currentObjectId);
        		} else {
        			fillTargetData(node, (AbstractTarget)o, currentObjectId);
        		}
        	}
        }
    }

    private void fillGroupData(TreeItem groupNode, TargetGroup group, String currentObjectId) {
    	groupNode.setText(" " + group.getName());
    	groupNode.setImage(ArecaImages.ICO_REF_PROCESS);
    	groupNode.setData(group);

        Iterator iter = group.getSortedIterator();
        while (iter.hasNext()) {
            TreeItem targetNode = new TreeItem(groupNode, SWT.NONE);
            groupNode.setExpanded(true);
            Object o = iter.next();
            if (o instanceof AbstractTarget) {
	            fillTargetData(targetNode, (AbstractTarget)o, currentObjectId);
            } else {
	            fillGroupData(targetNode, (TargetGroup)o, currentObjectId);
            }
        }

        if (group.getUid().equals(currentObjectId)) {
            tree.setSelection(groupNode);
        }
    }
    
    protected void fillTargetData(TreeItem targetNode, AbstractTarget target, String currentObjectId) {
        targetNode.setText(" " + target.getName());
        targetNode.setImage(ArecaImages.ICO_REF_TARGET);
        targetNode.setData(target);

        if (target.getUid().equals(currentObjectId)) {
            tree.setSelection(targetNode);
        }
    }

    public void setSelectedTarget(AbstractTarget target) {
        if (target != null) {
            TreeItem processNode = null;
            TargetGroup process = target.getParent();
            TreeItem[] processes = tree.getItems();
            for (int i=0; i<processes.length; i++) {
                TreeItem child = processes[i];
                TargetGroup cProcess = (TargetGroup)child.getData();
                if (cProcess.getUid().equals(process.getUid())) {
                    processNode = child;
                    break;
                }
            }

            TreeItem[] targets = processNode.getItems();
            for (int i=0; i<targets.length; i++) {
                TreeItem child = targets[i];
                AbstractTarget cTarget = (AbstractTarget)child.getData();
                if (cTarget.equals(target)) {
                    tree.setSelection(child);
                    break;
                }
            }
        }
    }

    public void mouseDoubleClick(MouseEvent e) {
    }

    public void mouseDown(MouseEvent e) {
    }
    
    public void mouseUp(MouseEvent e) {
    }

    public void handleEvent(Event event) {
    }

    protected void showMenu(MouseEvent e, Menu m) {
        if (e.button == 3) {
            m.setVisible(true);
        }
    }

    public WorkspaceItem[] getSelectedItems() {
    	TreeItem[] items = tree.getSelection();
    	WorkspaceItem[] ret = new WorkspaceItem[items.length];
    	for (int i=0; i<items.length; i++) {
    		ret[i] = (WorkspaceItem)items[i].getData();
    	}
    	return ret;
    }
}
