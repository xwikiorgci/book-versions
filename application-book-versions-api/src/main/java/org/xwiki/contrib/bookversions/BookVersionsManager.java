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
import org.xwiki.job.JobException;
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
     * Check if the given reference is a possible versioned content reference in its collection (not checking if it
     * exists).
     *
     * @param collectionReference The collection reference.
     * @param documentReference The document reference.
     * @return true if the given reference is a possible versioned content reference in its collection.
     * @throws QueryException
     * @throws XWikiException
     */
    boolean isPossibleVersionedContentReference(DocumentReference collectionReference,
        DocumentReference documentReference) throws XWikiException;

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
     * Check if the given library version belongs to the qiven library.
     * @param libraryReference The library reference
     * @param libraryVersionReference The library version reference
     * @return True if the library version belongs to the library
     * @throws QueryException
     * @throws XWikiException
     */
    boolean isFromLibrary(DocumentReference libraryReference, DocumentReference libraryVersionReference)
        throws QueryException, XWikiException;

    /**
     * Check if the given library version belongs to the qiven library.
     * @param library The library document
     * @param libraryVersion The library version document
     * @return True if the library version belongs to the library
     * @throws QueryException
     * @throws XWikiException
     */
    boolean isFromLibrary(XWikiDocument library, XWikiDocument libraryVersion)
        throws QueryException, XWikiException;

    /**
     * Check if the given document is marked as deleted.
     * @param documentReference The document reference
     * @return True if the document is marked as deleted
     * @throws XWikiException could occur if getDocument has an issue
     */
    boolean isMarkedDeleted(DocumentReference documentReference) throws XWikiException;

    /**
     * Check if the given document is marked as deleted.
     * @param document The document
     * @return True if the document is marked as deleted
     */
    boolean isMarkedDeleted(XWikiDocument document);

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
     * Get the selected variant that is stored in the session for the given collection (book / library).
     * 
     * @param documentReference the document reference.
     * @return the selected variant.
     * @throws QueryException
     * @throws XWikiException
     */
    String getSelectedVariant(DocumentReference documentReference) throws XWikiException, QueryException;

    /**
     * Set the selected variant in the session for the given collection (book / library).
     * 
     * @param documentReference the document reference of the collection.
     * @param variant the variant to be stored for the given collection.
     */
    void setSelectedVariant(DocumentReference documentReference, String variant);

    /**
     * Get the selected language that is stored in the session for the given collection (book / library).
     * 
     * @param documentReference the document reference.
     * @return the selected variant.
     * @throws QueryException
     * @throws XWikiException
     */
    String getSelectedLanguage(DocumentReference documentReference) throws XWikiException, QueryException;

    /**
     * Set the selected language in the session for the given collection (book / library).
     * 
     * @param documentReference the document reference of the collection.
     * @param language the language to be stored for the given collection.
     */
    void setSelectedLanguage(DocumentReference documentReference, String language);

    /**
     * Get the translated title for a document based on the selected language.
     * 
     * @param document The document to get the translated title for
     * @return The translated title if found, null otherwise
     * @throws XWikiException
     * @throws QueryException
     */
    String getTranslatedTitle(XWikiDocument document) throws XWikiException, QueryException;


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
     * Get the name of the referenced version.
     * 
     * @param versionReference The version reference.
     * @return the name of the referenced version.
     */
    String getVersionName(DocumentReference versionReference);

    /**
     * Get the name of the referenced variant.
     * 
     * @param variantReference The version reference.
     * @return the name of the referenced version.
     */
    String getVariantName(DocumentReference variantReference);

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
     * Get the reference of a given variant id, in the given referenced collection.
     * 
     * @param collectionReference The reference of the collection (book / library).
     * @param variant The variant id.
     * @return the reference of a given version id, in the given referenced collection.
     * @throws XWikiException
     */
    DocumentReference getVariantReference(DocumentReference collectionReference, String variant) throws XWikiException;

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
     * Get the variant references from a versioned collection (book or library.
     * 
     * @param documentReference the reference of the collection to get variants from
     * @return a list of variants references declared in the versioned collection, ordered by descending date. Returns
     *         an empty list if none are found.
     * @throws QueryException
     * @throws XWikiException
     */
    List<String> getCollectionVariants(DocumentReference documentReference) throws QueryException, XWikiException;

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
     * Get versioned content reference. The method is used at page content creation.
     * 
     * @param documentReference the reference of the book page
     * @return the versioned content reference
     * @throws XWikiException
     * @throws QueryException
     */
    DocumentReference getVersionedContentReference(DocumentReference documentReference)
        throws XWikiException, QueryException;

    /**
     * Get versioned content reference for the given document.
     * 
     * @param document The XWiki document
     * @return the versioned content reference
     * @throws XWikiException
     * @throws QueryException
     */
    DocumentReference getVersionedContentReference(XWikiDocument document) throws XWikiException, QueryException;

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

    /**
     * Set a library configuration to a book if it doesn't exit yet. The last version of the library is set as default.
     * @param bookReference the reference of the book
     * @param libraryReference the reference of the library to add
     * @throws QueryException
     * @throws XWikiException
     */
    void setLibrary(DocumentReference bookReference, DocumentReference libraryReference)
        throws QueryException, XWikiException;

    /**
     * Set a library configuration to a book if it doesn't exit yet.
     * @param bookReference the reference of the book
     * @param libraryReference the reference of the library to add
     * @param libraryVersionReference the reference of the version of the library to add
     * @throws QueryException
     * @throws XWikiException
     */
    void setLibrary(DocumentReference bookReference, DocumentReference libraryReference,
        DocumentReference libraryVersionReference) throws QueryException, XWikiException;

    /**
     * Get the library version reference which is configured in the given book, for the given library, with the
     * current selected book version.
     * @param bookReference the reference of the book
     * @param libraryReference the reference of the library
     * @return the reference of the library version configured in the book for the library
     * @throws XWikiException
     * @throws QueryException
     */
    DocumentReference getConfiguredLibraryVersion(DocumentReference bookReference, DocumentReference libraryReference)
        throws XWikiException, QueryException;

    /**
     * Get the reference of the library content for the given key, depending on the configured library version in
     * the given book and the current selected book version.
     * @param bookReference the reference of the book
     * @param keyReference the reference of the key (library page)
     * @return the reference of the content of the library
     * @throws XWikiException
     * @throws QueryException
     */
    DocumentReference getLinkedLibraryContentReference(DocumentReference bookReference, DocumentReference keyReference)
        throws XWikiException, QueryException;

    /**
     * Switch the document between "Marked as Deleted" or not.
     * @param documentReference The document reference
     * @throws XWikiException could occur if getDocument, newXObject or save have an issue
     */
    void switchDeletedMark(DocumentReference documentReference) throws XWikiException;

    /**
     * Execute the publication job.
     * @param configurationReference the configuration reference
     * @param jobId the job ID
     */
    void executePublicationJob(DocumentReference configurationReference, String jobId) throws JobException;

    /**
     * Execute the publication process with the provided configuration.
     * @param configurationReference The configuration reference
     * @throws XWikiException could occur if loadPublicationConfiguration has an issue
     */
    void publish(DocumentReference configurationReference) throws XWikiException, QueryException;

}
