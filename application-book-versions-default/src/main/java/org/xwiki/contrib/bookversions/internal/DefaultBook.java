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

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.contrib.bookversions.Book;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Book.
 *
 * @version $Id$
 * @since 0.1
 */
public class DefaultBook implements Book
{
    private DocumentReference documentReference;

    private XWikiDocument document;

    private BaseObject object;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    /**
     * Constructor.
     * 
     * @param documentReference The reference of the book
     */
    public DefaultBook(DocumentReference documentReference)
    {
        this.documentReference = documentReference;
        setMetadata();
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    @Override
    public boolean isDefined()
    {
        return this.object != null;
    }

    /**
     * Setting book metadata.
     */
    private void setMetadata()
    {
        XWikiContext xcontext = getXWikiContext();
        try {
            this.document = xcontext.getWiki().getDocument(this.documentReference, xcontext);
            this.object = this.document.getXObject(BookVersionsConstants.BOOK_CLASS_REFERENCE);
        } catch (XWikiException e) {
            logger.error("Could not handle the page metadata from reference [{}]", this.documentReference);
        }
    }

    /**
     * Get the XWiki context.
     *
     * @return the xwiki context.
     */
    protected XWikiContext getXWikiContext()
    {
        return contextProvider.get();
    }
}
