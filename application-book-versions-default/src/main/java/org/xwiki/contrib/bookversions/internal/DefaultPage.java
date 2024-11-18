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

package org.xwiki.contrib.bookversions.internal;

import org.xwiki.contrib.bookversions.Page;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Book Page.
 *
 * @version $Id$
 * @since 0.1
 */
public class DefaultPage implements Page
{
    private XWikiDocument document;

    private BaseObject object;

    private String status;

    private boolean defined;

    private boolean unversioned;

    /**
     * Constructor.
     * 
     * @param document The document storing the object.
     */
    public DefaultPage(XWikiDocument document)
    {
        this.document = document;
        this.object = document.getXObject(BookVersionsConstants.BOOKPAGE_CLASS_REFERENCE);
        this.defined = this.object != null;
        this.status = this.defined ? this.object.getStringValue(BookVersionsConstants.BOOKPAGE_PROP_STATUS) : null;
        this.unversioned =
            this.defined ? this.object.getIntValue(BookVersionsConstants.BOOKPAGE_PROP_UNVERSIONED) == 1 : false;
    }

    @Override
    public XWikiDocument getDocument()
    {
        return this.document;
    }

    @Override
    public boolean isDefined()
    {
        return this.defined;
    }

    @Override
    public String getStatus()
    {
        return this.status;
    }

    @Override
    public boolean isVersioned() throws XWikiException
    {
        return isDefined() && !unversioned;
    }

    @Override
    public boolean isDraft()
    {
        return this.status != null && this.status == BookVersionsConstants.BOOKPAGE_PROP_STATUS_DRAFT;
    }

    @Override
    public boolean isInReview()
    {
        return this.status != null && this.status == BookVersionsConstants.BOOKPAGE_PROP_STATUS_REVIEW;
    }

    @Override
    public boolean isComplete()
    {
        return this.status != null && this.status == BookVersionsConstants.BOOKPAGE_PROP_STATUS_COMPLETE;
    }
}
