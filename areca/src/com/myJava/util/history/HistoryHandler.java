package com.myJava.util.history;

import java.io.File;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import com.myJava.file.FileSystemManager;
import com.myJava.file.FileTool;
import com.myJava.util.log.Logger;
import com.myJava.util.xml.AdapterException;
import com.myJava.util.xml.XMLTool;

/**
 * 
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
public class HistoryHandler {
	private File file;
	private History cachedHistory;

	public HistoryHandler(File file) {
		this.file = file;
	}
	
	public void clearData() {
        if (file != null && FileSystemManager.exists(file)) {
            FileSystemManager.delete(file);
        }
        cachedHistory = null;
	}
	
	public History readHistory() {
		if (cachedHistory == null) {
			try {
				Logger.defaultLogger().info("Loading history ...");
				if (FileSystemManager.exists(file)) {
					HistoryReader adapter = buildReader();
					cachedHistory = adapter.read(file);
				} else {
					cachedHistory = new History();
				}

				Logger.defaultLogger().info("History loaded.");
			} catch (AdapterException e) {
				Logger.defaultLogger().error("An error occured while reading the target's history", e);
				cachedHistory = new History();
			}
		}
		return cachedHistory;
	}
	
	public void writeHistory(History history) throws AdapterException {
		XMLHistoryAdapter adapter = new XMLHistoryAdapter();
		adapter.write(history, file);
	}
	
	public void addEntryAndFlush(HistoryEntry entry) {
		try {
			History history = readHistory();
			if (history != null) {
				history.addEntry(entry);
				writeHistory(history);
			}
		} catch (AdapterException e) {
			// Non blocking
			Logger.defaultLogger().error(e);
		}
	}
	
	private HistoryReader buildReader() {
		try {
			String r = FileTool.getInstance().getFirstRow(new GZIPInputStream(FileSystemManager.getFileInputStream(file)), XMLHistoryAdapter.ENCODING);
			if (r.equalsIgnoreCase(XMLTool.getHeader(XMLHistoryAdapter.ENCODING))) {
				return new XMLHistoryAdapter();
			} else {
				return new DeprecatedHistoryAdapter();
			}
		} catch (IOException e) {
			return new XMLHistoryAdapter();
		}
	}
}
