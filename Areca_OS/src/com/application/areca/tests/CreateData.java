package com.application.areca.tests;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.myJava.file.FileSystemManager;

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
public class CreateData {
	private static Set CURRENT_FILES = new HashSet();
	
	private static String[] CREATE = new String[] {
		"src",
		"name with space and 's",
		"name with;semicolon",
		"name with-s",
		"azerty/name with_s",
		"n[a]me wit�",
		"n{am}e with #",
		"n(am)e wit�",
		"src2",
		"subdir1/srcooo",
		"subdir1/src1.2",
		"subdir2/src",
		"subdir2/src2.2",
		"subdir2/subdir2.1/src",
		"subdir2/subdir2.2/src2.2"
	};
	
	private static String[] CREATE2 = new String[] {
		"src999",
		"src989",
		"src979",
		"subdir1/sr789"
	};
	
	private static String[] APPEND = new String[] {
		"src3",
		"subdir1/src1.2",
		"subdir1/src7",
		"subdir1/subdir2.2/subdir3.3/toto",
		"subdir2/subdir2.2/src2.8"
	};
	
	private static String[] APPEND2 = new String[] {
		"src41",
		"subdir1/src1.74.3",
		"subdir1/dr/poiuyt"
	};
	
	private static String[] FINAL_APPEND = new String[] {
		"src6",
		"subdir2/src3",
		"subdir3/subdir2.2/subdir3.3/titi",
		"subdir2/subdir8.2/src2.9"
	};
	
	private static String[] REMOVE = new String[] {
		"src2",
		"subdir2/src2.2",
		"subdir2/subdir2.1/src"
	};
	
	private static String[] REMOVE2 = new String[] {
		"src3"
	};
	
	public static String[] FINAL_REMOVE = new String[] {
		"subdir1/src7",
		"subdir2/src"
	};
	
	private static int[] SIZES = new int[] {
		2,
		0,
		5,
		0
	};
	
	public static void create(String root) throws IOException {
		doCreate(CREATE, SIZES, root);
	}
	
	public static void create2(String root) throws IOException {
		doCreate(CREATE2, null, root);
	}
	
	public static void append(String root) throws IOException {
		doCreate(APPEND, null, root);
	}
	
	public static void append2(String root) throws IOException {
		doCreate(APPEND2, null, root);
	}
	
	public static void finalAppend(String root) throws IOException {
		doCreate(FINAL_APPEND, null, root);
	}
	
	public static void remove(String root) throws IOException {
		doRemove(root, REMOVE);
	}
	
	public static void remove2(String root) throws IOException {
		doRemove(root, REMOVE2);
	}
	
	public static void finalRemove(String root) throws IOException {
		doRemove(root, FINAL_REMOVE);
	}
	
	private static void doRemove(String root, String[] toRemove) throws IOException {
		File rootFile = new File(root);

		for (int i=0; i<toRemove.length; i++) {
			CURRENT_FILES.remove(toRemove[i]);
			File f = new File(rootFile, toRemove[i]);
			f.delete();
		}
	}
	
	private static void doCreate(String[] files, int[] sizes, String root) throws IOException {
		File rootFile = new File(root);
		rootFile.mkdirs();

		for (int i=0; i<files.length; i++) {
			CURRENT_FILES.add(files[i]);
			File f = new File(rootFile, files[i]);
			int size;
			if (sizes == null || sizes.length <= i) {
				size = (int)(0.1*files[i].hashCode())%31;
			} else {
				size = sizes[i];
			}
			createFile(f, size);
		}
	}

	private static void createFile(File f, int repeats) throws IOException {
		f.getParentFile().mkdirs();
		OutputStreamWriter writer = null;
		try {
			OutputStream os = FileSystemManager.getFileOutputStream(f);
			writer = new OutputStreamWriter(os);
			for (int i=0; i<repeats; i++) {
				writer.write("Random Data For Areca\n");
			}
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	
	public static void check(String dir, String[] additionalFiles) throws Exception {
		HashSet content = new HashSet();
		Iterator iter = CURRENT_FILES.iterator();
		while (iter.hasNext()) {
			content.add(iter.next());
		}
		
		if (additionalFiles != null) {
			for (int i=0; i<additionalFiles.length; i++) {
				content.add(additionalFiles[i]);
			}
		}
		
		doCheck(new File(dir), dir, content);
		
		if (! content.isEmpty()) {
			String err = "The following files should have been recovered :";
			iter = content.iterator();
			while (iter.hasNext()) {
				err += iter.next() + " ";
			}
			throw new IllegalArgumentException(err);
		}
	}
	
	private static void doCheck(File dir, String root, Set content) throws Exception {
		File[] children = dir.listFiles();
		for (int i=0; i<children.length; i++) {
			if (children[i].isFile()) {
				String suffix = children[i].getAbsolutePath().replace('\\', '/');
				if (suffix.startsWith(root)) {
					suffix = suffix.substring(root.length());
					if (suffix.startsWith("/") || suffix.startsWith("\\")) {
						suffix = suffix.substring(1);
					}
					if (content.contains(suffix)) {
						content.remove(suffix);
					} else {
						throw new IllegalArgumentException("The following file should not be recovered : " + suffix);
					}
				} else {
					throw new IllegalArgumentException("Invalid file name : " + children[i] + " / " + root);
				}
			} else {
				doCheck(children[i], root, content);
			}
		}
	}
}
