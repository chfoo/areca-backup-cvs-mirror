package com.application.areca.search;

import java.util.regex.Pattern;

/**
 * Utility class for file searches
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
public class SearchMatcher {

    private DefaultSearchCriteria criteria = null;
    private Pattern cachedRegex = null;
    private String cachedPattern = null;
    
    public SearchMatcher(DefaultSearchCriteria criteria) {
        this.criteria = criteria;
        this.cachedPattern = criteria.isMatchCase() ? criteria.getPattern() : criteria.getPattern().toLowerCase();
        if (criteria.isRegularExpression()) {
            this.cachedRegex = Pattern.compile(this.cachedPattern);
        }
    }
    
    public boolean matches(String entry) {
        String toCheck = this.criteria.isMatchCase() ? entry : entry.toLowerCase();
        if (this.criteria.isRegularExpression()) {
            return cachedRegex.matcher(toCheck).find();
        } else {
            return toCheck.indexOf(this.cachedPattern) != -1;
        }
    }
}
