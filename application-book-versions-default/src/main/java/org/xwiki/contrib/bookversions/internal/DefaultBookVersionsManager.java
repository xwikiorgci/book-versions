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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.bookversions.BookVersionsManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.validation.script.ModelValidationScriptService;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Default implementation of {@link BookVersionsManager}.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Singleton
public class DefaultBookVersionsManager implements BookVersionsManager
{

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Provider<QueryManager> queryManagerProvider;

    @Inject
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    @Named("modelvalidation")
    private Provider<ModelValidationScriptService> modelValidationScriptServiceProvider;

    @Inject
    private Logger logger;

    @Override
    public boolean isBook(DocumentReference documentReference) throws XWikiException
    {
        return new DefaultBook(documentReference).isDefined();
    }

    @Override
    public boolean isPage(DocumentReference documentReference) throws XWikiException
    {
        return new DefaultPage(documentReference).isDefined();
    }

    @Override
    public boolean isVersionedPage(DocumentReference documentReference) throws XWikiException
    {
        return new DefaultPage(documentReference).isVersioned();
    }

    @Override
    public String transformUsingSlugValidation(String name)
    {
        ModelValidationScriptService modelValidationScriptService = this.modelValidationScriptServiceProvider.get();
        return modelValidationScriptService.transformName(name, BookVersionsConstants.SLUGVALIDATION_HINT);
    }

    @Override
    public boolean isAParent(DocumentReference spaceReference, DocumentReference nestedReference)
    {
        if (spaceReference != null && nestedReference != null) {
            List<SpaceReference> childSpaces = nestedReference.getSpaceReferences();
            for (SpaceReference parentSpace : spaceReference.getSpaceReferences()) {
                if (childSpaces.contains(parentSpace)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the name of the page, but escaped for dot character.
     * 
     * @param documentReference the document reference to get the name from
     * @return the escaped name of the page
     */
    private static String getEscapedName(DocumentReference documentReference)
    {
        String sequence = "\\.";
        return documentReference.getName().replaceAll(sequence, sequence);
    }

    @Override
    public DocumentReference getVersionedCollectionReference(DocumentReference pageReference) throws QueryException
    {
        if (pageReference != null) {
            List<Object> collections = this.queryManagerProvider.get()
                .createQuery(", BaseObject as obj where doc.fullName = obj."
                    + "name and (obj.className = :bookClass or obj.className = :libraryClass)", Query.HQL)
                .bindValue("bookClass", localSerializer.serialize(BookVersionsConstants.BOOK_CLASS_REFERENCE))
                .bindValue("libraryClass", localSerializer.serialize(BookVersionsConstants.LIBRARY_CLASS_REFERENCE))
                .execute();
            for (Object collection : collections) {
                DocumentReference collectionReference =
                    this.referenceResolver.resolve((String) collection, pageReference);
                if (isAParent(collectionReference, pageReference)) {
                    return collectionReference;
                }
            }
        }
        return null;
    }

    @Override
    public List<DocumentReference> getCollectionVersions(DocumentReference collectionReference) throws QueryException
    {
        if (collectionReference != null) {
            SpaceReference collectionSpace = collectionReference.getLastSpaceReference();
            String collectionSpaceSerialized = localSerializer.serialize(collectionSpace);
            String spacePrefix = collectionSpaceSerialized.replaceAll("([%_/])", "/$1").concat(".%");

            // Query inspired from getDocumentReferences of DefaultModelBridge.java in xwiki-platform
            List<DocumentReference> result = this.queryManagerProvider.get()
                .createQuery(", BaseObject as obj where doc.fullName = obj.name and obj.className = :versionClass "
                    + "and doc.space like :space escape '/' order by doc.creationDate desc", Query.HQL)
                .bindValue("versionClass", localSerializer.serialize(BookVersionsConstants.VERSION_CLASS_REFERENCE))
                .bindValue("space", spacePrefix).execute();
            return result;
        }
        return Collections.emptyList();
    }

    @Override
    public DocumentReference getVersionedContentReference(DocumentReference pageReference,
        DocumentReference versionReference)
    {
        String versionName = getEscapedName(versionReference);
        return new DocumentReference(new EntityReference(versionName, EntityType.DOCUMENT, pageReference.getParent()));
    }

    @Override
    public DocumentReference getInheritedContentVersionReference(DocumentReference pageReference,
        DocumentReference versionReference) throws QueryException, XWikiException
    {
        if (pageReference != null && versionReference != null) {
            // TODO: check if the page is unversioned, or not
            XWikiContext xcontext = this.contextProvider.get();
            XWiki xwiki = xcontext.getWiki();

            String versionName = getEscapedName(versionReference);
            DocumentReference precedingVersionRef = versionReference;
            while (!versionName.isEmpty() && versionName != null) {
                XWikiDocument versionDoc = xwiki.getDocument(precedingVersionRef, xcontext);
                BaseObject versionObject = versionDoc.getXObject(BookVersionsConstants.VERSION_CLASS_REFERENCE);
                if (versionObject == null) {
                    logger.warn("Page [" + versionDoc.getDocumentReference().toString() + "] doesn't have a ["
                        + BookVersionsConstants.VERSION_CLASS_REFERENCE.toString() + "] object.");
                    return null;
                }
                DocumentReference versionedContentRef = getVersionedContentReference(pageReference, versionReference);
                if (xwiki.exists(versionedContentRef, xcontext)) {
                    // Content exists for this version of the page
                    return precedingVersionRef;
                } else {
                    // Content does not exists for this version. Lets check if there is content in a version to be
                    // inherited
                    String precedingVersion = versionObject.getStringValue("precedingVersionReference");
                    if (precedingVersion.isEmpty()) {
                        // This is the first version in the tree, there's nothing to inherit from
                        return null;
                    }
                    precedingVersionRef = this.referenceResolver.resolve(precedingVersion, precedingVersionRef);
                    String precedingVersionName = getEscapedName(precedingVersionRef);
                    if (precedingVersionName.equals(versionName)) {
                        logger.warn("Version page [" + precedingVersion + "] is referencing itself as a preceding "
                            + "version");
                        return null;
                    }
                    versionName = precedingVersionName;
                }
            }
        }
        return null;
    }

    @Override
    public DocumentReference getInheritedContentReference(DocumentReference pageReference,
        DocumentReference versionReference) throws QueryException, XWikiException
    {
        if (pageReference != null && versionReference != null) {
            XWikiContext xcontext = this.contextProvider.get();
            XWiki xwiki = xcontext.getWiki();

            DocumentReference versionPageReference =
                getInheritedContentVersionReference(pageReference, versionReference);
            if (versionPageReference == null) {
                return null;
            }
            String versionName = getEscapedName(versionPageReference);
            DocumentReference versionedContentRef =
                new DocumentReference(new EntityReference(versionName, EntityType.DOCUMENT, pageReference.getParent()));
            if (xwiki.exists(versionedContentRef, xcontext)) {
                // Content exists for this version of the page
                return versionedContentRef;
            }
        }
        return null;
    }
}
