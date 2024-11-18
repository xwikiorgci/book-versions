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

package org.xwiki.contrib.bookversions;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Book Page.
 *
 * @version $Id$
 * @since 0.1
 */
@Role
public interface Page
{
    /**
     * Get the document storing the definition of the page.
     * 
     * @return the document storing the definition of the page.
     */
    XWikiDocument getDocument();

    /**
     * Check if the current book page is defined (has the required object).
     * 
     * @return true if the current book page is defined.
     */
    boolean isDefined();

    /**
     * Get the status of the page.
     * 
     * @return the status of the page.
     */
    String getStatus();

    /**
     * Check if the page is versioned.
     * 
     * @return the identifier of the variant.
     * @throws XWikiException In case the system can't provide an answer.
     */
    boolean isVersioned() throws XWikiException;

    /**
     * Check if the current book page is a draft.
     * 
     * @return true if the current book page is a draft.
     */
    boolean isDraft();

    /**
     * Check if the current book page is in review.
     * 
     * @return true if the current book page is in review.
     */
    boolean isInReview();

    /**
     * Check if the current book page is complete.
     * 
     * @return true if the current book page is complete.
     */
    boolean isComplete();
}
