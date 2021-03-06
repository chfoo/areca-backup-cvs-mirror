package com.application.areca.filter;

import java.io.File;

import com.myJava.object.Duplicable;

/**
 * Archive filter interface.
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
public interface ArchiveFilter 
extends Duplicable {
	
    public boolean acceptIteration(File f);
    
    public boolean acceptStorage(File f);
    
    /**
     * Indique si le filtre fonctionne en inclusion ou exclusion
     * @return
     */
    public boolean isExclude();
    
    /**
     * Indique si le filtre fonctionne en inclusion ou exclusion
     * @param exclude
     */
    public void setExclude(boolean exclude);
    
    /**
     * Parses the string provided as argument and inits the filter
     */
    public void acceptParameters(String parameters);
    
    /**
     * Returns the filter's parameters as a String
     */
    public String getStringParameters();
    
    /**
     * Tells wether the filter needs to be parametrized or not 
     */
    public boolean requiresParameters();
}