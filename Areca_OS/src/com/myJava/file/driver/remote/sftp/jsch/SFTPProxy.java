package com.myJava.file.driver.remote.sftp.jsch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;
import com.myJava.configuration.FrameworkConfiguration;
import com.myJava.file.FileNameUtil;
import com.myJava.file.driver.remote.AbstractProxy;
import com.myJava.file.driver.remote.AbstractRemoteFileSystemDriver;
import com.myJava.file.driver.remote.FictiveFile;
import com.myJava.file.driver.remote.RemoteConnectionException;
import com.myJava.file.driver.remote.RemoteFileInputStream;
import com.myJava.file.driver.remote.RemoteFileOutputStream;
import com.myJava.file.driver.remote.sftp.SFTPConnectionException;
import com.myJava.object.EqualsHelper;
import com.myJava.object.HashHelper;
import com.myJava.object.ToStringHelper;
import com.myJava.util.Util;
import com.myJava.util.log.Logger;


/**
 * Proxy that abstracts the ftp access layer.
 * <BR>It wraps the ftp framework.
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
public class SFTPProxy extends AbstractProxy {
	private static final long TIME_BETWEEN_OPS = FrameworkConfiguration.getInstance().getFTPNoopDelay(); // Time between noops (milliseconds)

	// Default permissions is determined by default_permissions ^ umask
    private int umask = 0022;
    private int default_permissions = 0777;

	// PARAMETERS
	private String remoteServer;
	private int remotePort;
	private String login;
	private String password;
	private String testFile;
	
	private String hostKey;
	private boolean checkHostKey;

	// CLIENT
	private ChannelSftp client;
	private Session session;

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getRemotePort() {
		return remotePort;
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	public String getRemoteServer() {
		return remoteServer;
	}

	public void setRemoteServer(String remoteServer) {
		this.remoteServer = remoteServer;
	}

	public String getHostKeyAsString() {
		return hostKey;
	}
	
	public byte[] getHostKeyAsByteArray() {
		return Util.base64Decode(hostKey);
	}

	public void setHostKey(String hostKey) {
		this.hostKey = hostKey;
	}

	public boolean isCheckHostKey() {
		return checkHostKey;
	}

	public void setCheckHostKey(boolean checkHostKey) {
		this.checkHostKey = checkHostKey;
	}

	public SFTPProxy(String testFile) {
		super();
		this.testFile = testFile;
	}

	public static void main(String[] args) {
		try {
			SFTPProxy p = new SFTPProxy("/");
			p.setLogin("admin");
			p.setPassword("Datura31");
			p.setRemotePort(22);
			p.setRemoteServer("192.168.2.100");
			p.connect();
		} catch (SFTPConnectionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Enforce server reconnection (closes the current connection if it is still alive)
	 */
	public synchronized void connect() throws SFTPConnectionException {
		//checkLocked();

		try {
			// Try to disconnect
			this.disconnect();

			// Open new connection			
			Logger.defaultLogger().info("Trying to connect to server : " + this.remoteServer + " ...");
			debug("connect : connect", remoteServer);

			JSch jsch = new JSch();
			if (checkHostKey) {
				HostKeyRepository hkr = jsch.getHostKeyRepository();
				byte[] keybytes = this.getHostKeyAsByteArray();
				if (keybytes == null) {
					throw new SFTPConnectionException("Incorrect hostkey : " + this.getHostKeyAsString());
				}
				HostKey key = new HostKey(remoteServer, keybytes);
				hkr.add(key, null);
				jsch.setHostKeyRepository(hkr);
			}

			session = jsch.getSession(login, remoteServer, remotePort);
			UserInfo ui = new DefaultUserInfo(this.password);
			session.setUserInfo(ui);
			session.setDaemonThread(true);
			session.setConfig("StrictHostKeyChecking", checkHostKey ? "yes":"no");

			Logger.defaultLogger().info("Trying to log in with user : " + this.login +  " ...");
			debug("connect : login", login + "/" + password);
			session.connect();

			client = (ChannelSftp)session.openChannel("sftp");
			client.connect();

			this.connectionId = Util.getRndLong();
			this.updateOpTime();

			Logger.defaultLogger().info("Connected to server : " + this.remoteServer);
		} catch (JSchException e) {
			resetClient(e);
			throw new SFTPConnectionException("Unable to connect to server : " + this.remoteServer + " (" + e.getMessage() + ")");
		} finally {
			clearCache();
		}
	}

	protected void resetClient(Throwable e) {
		debug("Destroying client because of exception.", e);
		Logger.defaultLogger().error("Client reset because of the following error.", e, "AbstractProxy.resetClient()");
		try {
			this.disconnect();
		} catch (Throwable ex) {
			Logger.defaultLogger().warn("Error caucht while trying to disconnect from remote server.", ex, "AbstractProxy.resetClient()");
		}
		this.client = null;
		clearCache();
	}


	public void disconnect() {
		this.resetContextData();
		Logger.defaultLogger().info("Disconnecting from server : " + this.remoteServer + " ...");
		if (this.client != null && this.client.isConnected()) {
			debug("disconnect : client.disconnect");
			this.client.disconnect();
		}

		if (this.session != null && this.session.isConnected()) {
			debug("disconnect : session.disconnect");
			this.session.disconnect();
		}
		Logger.defaultLogger().info("OK : disconnected from server : " + this.remoteServer + ".");
	}

	/**
	 * Checks if the SFTP connection is alive and reconnect to server if needed.
	 */
	private synchronized void checkConnection() throws SFTPConnectionException {
		boolean shallReconnect = true;

		if (client != null && client.isConnected()) {
			try {
				if ((System.currentTimeMillis() - lastOpTime) >= TIME_BETWEEN_OPS) {
					debug("checkConnection : getAttributes");
					client.lstat(testFile);
					this.updateOpTime();
					shallReconnect = false;
					debug("checkConnection : connection successfully checked");
				} else {
					shallReconnect = false;
					debug("checkConnection : no need to check connection");
				}
			} catch (Throwable e) {
				debug("checkConnection", e);
				Logger.defaultLogger().warn("Got an error during connection check (" + e.getMessage() + "). The client will be reconnected.");
			}
		}

		if (shallReconnect) {
			if (client != null) {
				Logger.defaultLogger().info("Disconnected from server : " + this.remoteServer + " ... tyring to reconnect.");
				debug("checkConnection : disconnected ... trying to reconnect");                
			} else {
				debug("checkConnection : not connected ... trying to connect"); 	
			}
			this.connect();
		}
	}

	private void changeWorkingDirectory(String dir) throws SftpException {
		this.client.cd(dir);
	}

	private String resolveRemotePath(String path) {
		return path;
	}

	public boolean deleteFile(String remoteFile)
	throws RemoteConnectionException {
		checkLocked();
		this.checkConnection();
		try {
			this.updateOpTime();
			debug("deleteFile : ", remoteFile);
			client.rm(resolveRemotePath(remoteFile));
			return true;
		} catch (SftpException e) {
			resetClient(e);
			resetContextData();
			throw new SFTPConnectionException(e.getMessage());
		} catch (RuntimeException e) {
			resetContextData();
			throw e;
		} finally {
			removeCachedFileInfos(remoteFile);
		}
	}

	public synchronized boolean deleteDir(String remoteDir) throws SFTPConnectionException {
		checkLocked();
		this.checkConnection();
		try {
			this.updateOpTime();
			debug("deleteDir : ", remoteDir);
			client.rmdir(resolveRemotePath(remoteDir));
			return true;
		} catch (SftpException e) {
			resetClient(e);
			resetContextData();
			throw new SFTPConnectionException(e.getMessage());
		} catch (RuntimeException e) {
			resetContextData();
			throw e;
		} finally {
			removeCachedFileInfos(remoteDir);
		}
	}

	public synchronized boolean mkdir(String remoteFile) throws SFTPConnectionException {
		checkLocked();
		this.checkConnection();
		try {
			File f = new File(remoteFile);

			this.changeWorkingDirectory(AbstractRemoteFileSystemDriver.normalizeIfNeeded(f.getParent()));
			this.updateOpTime();
			debug("mkdir : mkdir", remoteFile);
			client.mkdir(f.getName());
			FictiveFile existing = this.fileInfoCache.getCachedFileInfos(remoteFile);
			if (existing != null) {
				existing.init(0, true, true, 0);
			} else {
				this.fileInfoCache.registerFileInfo(remoteFile, new FictiveFile(remoteFile, remoteFile, 0, true, true, 0));
			}
			return true;
		} catch (SftpException e) {
			removeCachedFileInfos(remoteFile);
			resetClient(e);
			throw new SFTPConnectionException(e.getMessage());
		} finally {
			resetContextData();
		}
	}

	public synchronized boolean renameTo(String source, String destination) throws SFTPConnectionException {
		checkLocked();
		this.checkConnection();

		try {
			debug("renameTo : rename", source + "->" + destination);
			client.rename(source, destination);
			this.updateOpTime();
			return true;
		} catch (SftpException e) {
			resetClient(e);
			throw new SFTPConnectionException(e.getMessage());
		} finally {
			resetContextData();

			FictiveFile file = fileInfoCache.getCachedFileInfos(destination);
			if (file != null && file.exists() && file.isFile()) {
				// Case 1 : "source" is a file -> selectively remove the entries from the cache
				removeCachedFileInfos(source);
				removeCachedFileInfos(destination);
			} else {
				// Case 2 : We don't know whether "source" is a file or a directory -> destroy all the cache
				clearCache();
			}
		}
	}

	public synchronized InputStream getFileInputStream(String file) throws SFTPConnectionException {
		checkLocked();
		this.checkConnection();
		try {
			debug("getFileInputStream : retrieveFileStream", file);
			InputStream result = this.client.get(file);

			this.updateOpTime();
			return new RemoteFileInputStream(this, result, ownerId);
		} catch (SftpException e) {
			resetClient(e);
			throw new SFTPConnectionException(e.getMessage());
		}
	}

	public synchronized OutputStream getFileOutputStream(final String file, boolean append) throws RemoteConnectionException {
		if (append) {
			throw new UnsupportedOperationException("Append mode not supported on the SFTP implementation.");
		}

		checkLocked();
		this.checkConnection();
		OutputStream result = null;
		try {
			debug("getFileOutputStream : put", file);
			result = client.put(file, ChannelSftp.OVERWRITE);

			this.updateOpTime();
			return new RemoteFileOutputStream(this, result, ownerId, file);
		} catch (SftpException e) {
			resetClient(e);
			throw new SFTPConnectionException(e.getMessage());
		}  finally {
			removeCachedFileInfos(file);
		}
	}	
	
	public synchronized FictiveFile[] listFiles(String parentFile) throws SFTPConnectionException {
		checkLocked();
		this.checkConnection();

		try {
			debug("listFiles : getAttrs", parentFile);
			client.lstat(parentFile);
		} catch (SftpException e) {
			return new FictiveFile[0];
		}
		
		try {
			// File lookup on server
			debug("listFiles : listFiles", parentFile);
			Iterator iter = client.ls(parentFile).iterator();
			this.updateOpTime();
			ArrayList returned = new ArrayList();
			while(iter.hasNext()) {
				LsEntry file = (LsEntry)iter.next();
				if (acceptListedFile(file)) {
					String remotePath = FileNameUtil.normalizeSlashes(parentFile + "/" + file.getFilename(), false);
					returned.add(
							new FictiveFile(
									remotePath, 
									remotePath, 
									file.getAttrs().getSize(), 
									file.getAttrs().isDir(),
									true, 
									file.getAttrs().getMTime()
							)
					);
				}
			}
			return (FictiveFile[])returned.toArray(new FictiveFile[0]);
		} catch (SftpException e) {
			resetClient(e);
			throw new SFTPConnectionException(e.getMessage());
		}  
	}	
	
	/**
	 * Filters the "." and ".." directories 
	 */
	private boolean acceptListedFile(LsEntry file) {
		String name = file.getFilename().trim().toLowerCase();
		return (
				! (
						name.endsWith("/..") 
						|| name.endsWith("\\..")
						|| name.endsWith("/.")
						|| name.endsWith("\\.")
						|| name.equals("..")
						|| name.equals(".")
				)
		);
	}
	
	public synchronized FictiveFile getRemoteFileInfos(String remoteFile) throws SFTPConnectionException {
		checkLocked();
		this.nbGetRemoteFileInfos++;        

		debug("getRemoteFileInfos : getCachedFileInfos", remoteFile);
		FictiveFile info = fileInfoCache.getCachedFileInfos(remoteFile);
		if (info != null) {
			debug("getRemoteFileInfos : Cached data were found", info);
			this.nbCacheRetrieval++;
			debug("Cache Efficiency", new Double(this.nbCacheRetrieval / this.nbGetRemoteFileInfos));
			return info;
		}

		this.checkConnection();
		try {    
			debug("getRemoteFileInfos : getAttributes", remoteFile);
			try {
				SftpATTRS attributes = this.client.lstat(remoteFile);
				this.updateOpTime();
				
				// File lookup on server
				// The local path is initialized to the remote path --> not relevant
				info = new FictiveFile(
						remoteFile, 
						remoteFile, 
						attributes.getSize(),
						attributes.isDir(), 
						true, 
						attributes.getMTime()
				);
			} catch (SftpException e) {
				info = new FictiveFile(
						remoteFile, 
						remoteFile, 
						0,
						false, 
						false, 
						0
				);
			} 

			registerFileInfo(remoteFile, info);
			return info;
		} catch (Throwable e) {
			resetClient(e);
			return null;
		}
	}

	public synchronized void completePendingCommand(boolean blocking) throws IOException, RemoteConnectionException {
	}

	protected void resetContextData() {
	} 

	public AbstractProxy cloneProxy() {
		SFTPProxy proxy = new SFTPProxy(testFile);
		proxy.setLogin(login);
		proxy.setPassword(password);
		proxy.setRemotePort(remotePort);
		proxy.setHostKey(hostKey);
		proxy.setRemoteServer(remoteServer);
		proxy.setFileInfoCache(fileInfoCache);
		proxy.setCheckHostKey(checkHostKey);

		return proxy;
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else if (obj instanceof SFTPProxy) {
			SFTPProxy o = (SFTPProxy)obj;
			return
			EqualsHelper.equals(this.remotePort, o.remotePort)
			&& EqualsHelper.equals(this.login, o.login)
			&& EqualsHelper.equals(this.hostKey, o.hostKey)
			&& EqualsHelper.equals(this.password, o.password)
			&& EqualsHelper.equals(this.checkHostKey, o.checkHostKey)
			&& EqualsHelper.equals(this.remoteServer, o.remoteServer);
		} else {
			return false;
		}
	}

	public int hashCode() {
		int h = HashHelper.initHash(this);
		h = HashHelper.hash(h, this.remotePort);
		h = HashHelper.hash(h, this.login);
		h = HashHelper.hash(h, this.password);
		h = HashHelper.hash(h, this.hostKey);
		h = HashHelper.hash(h, this.checkHostKey);
		h = HashHelper.hash(h, this.remoteServer);
		return h;
	}    

	public String toString() {
		StringBuffer sb = ToStringHelper.init(this);
		ToStringHelper.append("Login", login, sb);
		ToStringHelper.append("Host", remoteServer, sb);
		ToStringHelper.append("Port", remotePort, sb);
		return ToStringHelper.close(sb);
	}
}