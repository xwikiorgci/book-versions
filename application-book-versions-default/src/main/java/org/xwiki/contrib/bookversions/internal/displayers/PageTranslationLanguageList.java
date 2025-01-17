package org.xwiki.contrib.bookversions.internal.displayers;
/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

import java.util.List;

import org.xwiki.properties.annotation.PropertyDescription;

/**
 * Class representing a list of language ids. This class is needed to declare a valid
 * {@link org.xwiki.properties.annotation.PropertyDisplayType}.
 *
 * @version $Id$
 * @since 1.0
 */

public class PageTranslationLanguageList
{
    /**
     * @see #getLanguages()
     */
    private List<String> languageIds;

    /**
     * Constructor.
     * 
     * @param languageIds the list of language ids.
     */
    public PageTranslationLanguageList(List<String> languageIds)
    {
        setLanguages(languageIds);
    }

    /**
     * @param languageIds see {@link #getLanguages()}
     */
    @PropertyDescription("the language ids")
    public void setLanguages(List<String> languageIds)
    {
        this.languageIds = languageIds;
    }

    /**
     * @return the list of language ids.
     */
    public List<String> getLanguages()
    {
        return languageIds;
    }

    @Override
    public String toString()
    {
        return languageIds.toString();
    }
}
