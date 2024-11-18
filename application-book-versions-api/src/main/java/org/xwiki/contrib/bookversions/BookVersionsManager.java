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

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Book versions manager.
 * 
 * @version $Id$
 * @since 0.1
 */
@Role
public interface BookVersionsManager
{
    /**
     * Check if the given reference is a book.
     * 
     * @param documentReference The document reference.
     * @return True, if the given reference is a book.
     * @throws XWikiException In case the system can't provide an answer.
     */
    boolean isBook(DocumentReference documentReference) throws XWikiException;

    /**
     * Check if the given document is a book.
     * 
     * @param document The document.
     * @return True, if the given document is a book.
     * @throws XWikiException In case the system can't provide an answer.
     */
    boolean isBook(XWikiDocument document) throws XWikiException;

    /**
     * Check if the given reference is a book page.
     * 
     * @param documentReference The document reference.
     * @return True, if the given reference is a book page.
     * @throws XWikiException In case the system can't provide an answer.
     */
    boolean isPage(DocumentReference documentReference) throws XWikiException;

    /**
     * Check if the given document is a book page.
     * 
     * @param document The XWiki document.
     * @return True, if the given reference is a book page.
     * @throws XWikiException In case the system can't provide an answer.
     */
    boolean isPage(XWikiDocument document) throws XWikiException;

    /**
     * Check if the given reference is a versioned book page.
     * 
     * @param documentReference The document reference.
     * @return True, if the given reference is a versioned book page.
     * @throws XWikiException In case the system can't provide an answer.
     */
    boolean isVersionedPage(DocumentReference documentReference) throws XWikiException;

    /**
     * Check if the given document is a versioned book page.
     * 
     * @param document The XWiki document.
     * @return True, if the given document is a versioned book page.
     * @throws XWikiException In case the system can't provide an answer.
     */
    boolean isVersionedPage(XWikiDocument document) throws XWikiException;

    /**
     * Check if the given reference is a versioned content document.
     * 
     * @param documentReference The document reference.
     * @return true if the given reference is a versioned content document.
     * @throws XWikiException
     */
    boolean isVersionedContent(DocumentReference documentReference) throws XWikiException;

    /**
     * Check if the given document is a versioned content one.
     * 
     * @param document The XWiki document.
     * @return true if the given document is a versioned content one.
     * @throws XWikiException
     */
    boolean isVersionedContent(XWikiDocument document) throws XWikiException;

    /**
     * Check if the given reference is a version.
     * 
     * @param documentReference The document reference.
     * @return True, if the given reference is a version.
     * @throws XWikiException In case the system can't provide an answer.
     */
    boolean isVersion(DocumentReference documentReference) throws XWikiException;

    /**
     * Check if the given document is a version.
     * 
     * @param document The document.
     * @return True, if the given document is a version.
     * @throws XWikiException In case the system can't provide an answer.
     */
    boolean isVersion(XWikiDocument document) throws XWikiException;

    /**
     * Check if the given reference is a variant.
     * 
     * @param documentReference The document reference.
     * @return True, if the given reference is a variant.
     * @throws XWikiException In case the system can't provide an answer.
     */
    boolean isVariant(DocumentReference documentReference) throws XWikiException;

    /**
     * Check if the given reference is a library.
     * 
     * @param documentReference The document reference.
     * @return True, if the given reference is a library.
     * @throws XWikiException In case the system can't provide an answer.
     */
    boolean isLibrary(DocumentReference documentReference) throws XWikiException;

    /**
     * Transform the given name by using the slug name validation.
     *
     * @param name The name to be transformed.
     * @return the transformed.
     */
    String transformUsingSlugValidation(String name);

    /**
     * Get the selected version that is stored in the session for the given collection (book / library).
     * 
     * @param documentReference the document reference.
     * @return the selected version.
     * @throws QueryException
     * @throws XWikiException
     */
    String getSelectedVersion(DocumentReference documentReference) throws XWikiException, QueryException;

    /**
     * Set the selected version in the session for the given collection (book / library).
     * 
     * @param documentReference the document reference.
     * @param version the version to be stored for the given collection.
     */
    void setSelectedVersion(DocumentReference documentReference, String version);

    /**
     * Check if a page is a nested page of another one, recursively.
     * 
     * @param documentReference the reference of the supposed space
     * @param nestedReference the reference of the supposed nested page
     * @return true if the space contains the nested space in one of its sub-spaces.
     */
    boolean isAParent(DocumentReference documentReference, DocumentReference nestedReference);

    /**
     * Get the versioned collection (book or library) reference of a given page.
     * 
     * @param documentReference the page from which to take the collection reference
     * @return the versioned collection reference, or null if the page is not part of a collection
     * @throws XWikiException
     * @throws QueryException
     */
    DocumentReference getVersionedCollectionReference(DocumentReference documentReference)
        throws XWikiException, QueryException;

    /**
     * Get the reference of a given version id, in the given referenced collection.
     * 
     * @param collectionReference The reference of the collection (book / library).
     * @param version The version id.
     * @return the reference of a given version id, in the given referenced collection.
     * @throws XWikiException
     */
    DocumentReference getVersionReference(DocumentReference collectionReference, String version) throws XWikiException;

    /**
     * Get the version references from a versioned collection (book or library.
     * 
     * @param documentReference the reference of the collection to get versions from
     * @return a list of versions references declared in the versioned collection, ordered by descending date. Returns
     *         an empty list if none are found.
     * @throws QueryException
     * @throws XWikiException
     */
    List<String> getCollectionVersions(DocumentReference documentReference) throws QueryException, XWikiException;

    /**
     * Get the reference of the page content corresponding to the given version. No inheritance is used for this
     * computation.
     * 
     * @param documentReference The page reference.
     * @param version The version id.
     * @return the reference of the page content corresponding to the given version.
     * @throws QueryException
     * @throws XWikiException
     */
    DocumentReference getVersionedContentReference(DocumentReference documentReference, String version)
        throws QueryException, XWikiException;

    /**
     * Check if the page has content corresponding to the given version.
     * 
     * @param documentReference The page reference.
     * @param version The version id.
     * @return true if the page has content corresponding to the given version.
     * @throws QueryException
     * @throws XWikiException
     */
    boolean hasContentForVersion(DocumentReference documentReference, String version)
        throws QueryException, XWikiException;

    /**
     * Get the reference of a versioned page content. This does not check existence or page class.
     * 
     * @param documentReference the reference of the (versioned content) page
     * @return the reference of the versioned page content
     * @throws XWikiException
     * @throws QueryException
     */
    DocumentReference getInheritedVersionedContentReference(DocumentReference documentReference)
        throws XWikiException, QueryException;

    /**
     * Get the reference of a versioned page content. This does not check existence or page class.
     * 
     * @param documentReference the reference of the (versioned content) page
     * @param versionReference the reference of the version page
     * @return the reference of the versioned page content
     */
    DocumentReference getVersionedContentReference(DocumentReference documentReference,
        DocumentReference versionReference);

    /**
     * Get the reference of the version of the content to be displayed, be it the required version, or inherited one.
     * 
     * @param documentReference the reference of the page to get the content from
     * @param versionReference the reference of the version from which to get the content from, or inherit
     * @return the reference of the version of the content to be displayed. Null if there's no versioned content for the
     *         page, neither to inherit.
     * @throws QueryException
     * @throws XWikiException
     */
    DocumentReference getInheritedContentVersionReference(DocumentReference documentReference,
        DocumentReference versionReference) throws QueryException, XWikiException;

    /**
     * Get the reference of the content to be displayed, be it corresponding to the required version, or inherited from
     * another version.
     * 
     * @param documentReference the reference of the page to get the content from
     * @param versionReference the reference of the version from which to get the content from, or inherit
     * @return the reference of the content to be displayed. Null if there's no versioned content for the page, neither
     *         to inherit.
     * @throws QueryException
     * @throws XWikiException
     */
    DocumentReference getInheritedContentReference(DocumentReference documentReference,
        DocumentReference versionReference) throws QueryException, XWikiException;
}
