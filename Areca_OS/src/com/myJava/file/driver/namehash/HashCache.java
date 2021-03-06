package com.myJava.file.driver.namehash;

import java.util.HashMap;
import java.util.Map;

import com.myJava.configuration.FrameworkConfiguration;
import com.myJava.util.log.Logger;

import sun.misc.Queue;

/**
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
public class HashCache {

    private static final int CACHE_SIZE;
    
    static {
        if (FrameworkConfiguration.getInstance().isHashCacheMode()) {
            CACHE_SIZE= FrameworkConfiguration.getInstance().getHashCacheSize();
        } else {
            CACHE_SIZE = 0;
        }
    }
    
    // DATA CACHE
    private Map hashCache = new HashMap();
    private Queue hashOrder = new Queue();

    public HashCache() {
    }
    
    protected synchronized void registerFullName(String hash, String fullName) {
        if (CACHE_SIZE != 0) {
	        try {
	            while (this.hashCache.size() >= CACHE_SIZE) {
	                this.hashCache.remove(this.hashOrder.dequeue());
	            }
	            
	            this.hashCache.put(hash, fullName);
	            this.hashOrder.enqueue(hash);
	        } catch (InterruptedException e) {
	            Logger.defaultLogger().error(e);
	        }
        }
    }
    
    protected synchronized String getFullName(String hash) {
        return (String)this.hashCache.get(hash);
    }
    
    protected synchronized void removeFullName(String hash) {
        this.hashCache.remove(hash);
    }
    
    protected synchronized void clearCache() {
        this.hashCache.clear();
        this.hashOrder = new Queue();
    }
    
    public int size() {
        return this.hashCache.size();
    }
}
