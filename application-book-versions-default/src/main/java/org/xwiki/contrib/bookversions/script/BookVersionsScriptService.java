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

package org.xwiki.contrib.bookversions.script;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiException;

import org.xwiki.contrib.bookversions.BookVersionsManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;

/**
 * Book versions script service.
 * 
 * @version $Id$
 * @since 0.1
 */
@Component
@Named(BookVersionsScriptService.ID)
@Singleton
public class BookVersionsScriptService implements ScriptService
{
    /**
     * The id of this script service.
     */
    public static final String ID = "bookversions";

    @Inject
    private Provider<BookVersionsManager> bookVersionsManagerProvider;

    /**
     * Check if the given reference is a book.
     * 
     * @param documentReference The document reference.
     * @return True, if the given reference is a book.
     * @throws XWikiException In case the system can't provide an answer.
     */
    public boolean isBook(DocumentReference documentReference) throws XWikiException
    {
        return bookVersionsManagerProvider.get().isBook(documentReference);
    }

    /**
     * Check if the given reference is a versioned book (has at least one version).
     * 
     * @param documentReference The document reference.
     * @return true if the given reference is versioned book.
     * @throws QueryException
     * @throws XWikiException
     */
    boolean isVersionedBook(DocumentReference documentReference) throws XWikiException, QueryException
    {
        return bookVersionsManagerProvider.get().isVersionedBook(documentReference);
    }

    /**
     * Check if the given reference is a book page.
     * 
     * @param documentReference The document reference.
     * @return True, if the given reference is a book page.
     * @throws XWikiException In case the system can't provide an answer.
     */
    public boolean isPage(DocumentReference documentReference) throws XWikiException
    {
        return bookVersionsManagerProvider.get().isPage(documentReference);
    }

    /**
     * Check if the given reference is a versioned book page.
     * 
     * @param documentReference The document reference.
     * @return True, if the given reference is a versioned book page.
     * @throws XWikiException In case the system can't provide an answer.
     */
    public boolean isVersionedPage(DocumentReference documentReference) throws XWikiException
    {
        return bookVersionsManagerProvider.get().isVersionedPage(documentReference);
    }

    /**
     * Transform the given name by using the slug name validation.
     *
     * @param name The name to be transformed.
     * @return the transformed.
     */
    public String transformUsingSlugValidation(String name)
    {
        return bookVersionsManagerProvider.get().transformUsingSlugValidation(name);
    }

    /**
     * Check if a page is a nested page of another one, recursively.
     * 
     * @param spaceReference the reference of the supposed space
     * @param nestedReference the reference of the supposed nested page
     * @return true if the space contains the nested space in one of its sub-spaces.
     */
    public boolean isAParent(DocumentReference spaceReference, DocumentReference nestedReference)
    {
        return bookVersionsManagerProvider.get().isAParent(spaceReference, nestedReference);
    }

    /**
     * Get the versioned collection (book or library) reference of a given page.
     * 
     * @param pageReference the page from which to take the collection reference
     * @return the versioned collection reference, or null if the page is not part of a collection
     * @throws QueryException
     */
    public DocumentReference getVersionedCollectionReference(DocumentReference pageReference)
        throws XWikiException, QueryException
    {
        return bookVersionsManagerProvider.get().getVersionedCollectionReference(pageReference);
    }

    /**
     * Get the version references from a versioned collection (book or library.
     * 
     * @param collectionReference the reference of the collection to get versions from
     * @return a list of versions references declared in the versioned collection, ordered by descending date. Returns
     *         an empty list if none are found.
     * @throws QueryException
     * @throws XWikiException
     */
    public List<DocumentReference> getCollectionVersions(DocumentReference collectionReference)
        throws QueryException, XWikiException
    {
        return bookVersionsManagerProvider.get().getCollectionVersions(collectionReference);
    }

    /**
     * Get the reference of a versioned page content. This does not check existence or page class.
     * 
     * @param pageReference the reference of the (versioned content) page
     * @param versionReference the reference of the version page
     * @return the reference of the versioned page content
     */
    public DocumentReference getVersionedContentReference(DocumentReference pageReference,
        DocumentReference versionReference)
    {
        return bookVersionsManagerProvider.get().getVersionedContentReference(pageReference, versionReference);
    }

    /**
     * Get the reference of the version of the content to be displayed, be it the required version, or inherited one.
     * 
     * @param pageReference the reference of the page to get the content from
     * @param versionReference the reference of the version from which to get the content from, or inherit
     * @return the reference of the version of the content to be displayed. Null if there's no versioned content for the
     *         page, neither to inherit.
     * @throws QueryException
     * @throws XWikiException
     */
    public DocumentReference getInheritedContentVersionReference(DocumentReference pageReference,
        DocumentReference versionReference) throws QueryException, XWikiException
    {
        return bookVersionsManagerProvider.get().getInheritedContentVersionReference(pageReference, versionReference);
    }

    /**
     * Get the reference of the content to be displayed, be it corresponding to the required version, or inherited from
     * another version.
     * 
     * @param pageReference the reference of the page to get the content from
     * @param versionReference the reference of the version from which to get the content from, or inherit
     * @return the reference of the content to be displayed. Null if there's no versioned content for the page, neither
     *         to inherit.
     * @throws QueryException
     * @throws XWikiException
     */
    public DocumentReference getInheritedContentReference(DocumentReference pageReference,
        DocumentReference versionReference) throws QueryException, XWikiException
    {
        return bookVersionsManagerProvider.get().getInheritedContentReference(pageReference, versionReference);
    }
}
