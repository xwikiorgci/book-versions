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

import org.xwiki.properties.annotation.PropertyDescription;

/**
 * Class representing a translation language in a book page. This class is needed to declare a valid
 * {@link org.xwiki.properties.annotation.PropertyDisplayType}.
 *
 * @version $Id$
 * @since 1.0
 */

public class PageTranslationLanguage
{
    /**
     * @see #getLanguage()
     */
    private String language;

    /**
     * Constructor.
     * 
     * @param languageId the language id.
     */
    public PageTranslationLanguage(String languageId)
    {
        setLanguage(languageId);
    }

    /**
     * @param languageId see {@link #getLanguage()}
     */
    @PropertyDescription("the language of the content translation")
    public void setLanguage(String languageId)
    {
        language = languageId;
    }

    /**
     * @return the language of the content translation.
     */
    public String getLanguage()
    {
        return language;
    }
    
    @Override
    public String toString()
    {
        return language;
    }
}
