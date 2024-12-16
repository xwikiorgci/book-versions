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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.bookversions.BookVersionsManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.validation.EntityNameValidation;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiRequest;

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
    @Named("SlugEntityNameValidation")
    private Provider<EntityNameValidation> slugEntityNameValidationProvider;

    @Inject
    private Logger logger;

    @Override
    public boolean isBook(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext xcontext = this.getXWikiContext();

        return isBook(this.getXWikiContext().getWiki().getDocument(documentReference, xcontext));
    }

    @Override
    public boolean isBook(XWikiDocument document) throws XWikiException
    {
        return new DefaultBook(document).isDefined();
    }

    @Override
    public boolean isPage(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext xcontext = this.getXWikiContext();

        return isPage(xcontext.getWiki().getDocument(documentReference, xcontext));
    }

    @Override
    public boolean isPage(XWikiDocument document) throws XWikiException
    {
        return new DefaultPage(document).isDefined();
    }

    @Override
    public boolean isVersionedPage(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext xcontext = this.getXWikiContext();

        return isVersionedPage(xcontext.getWiki().getDocument(documentReference, xcontext));
    }

    @Override
    public boolean isVersionedPage(XWikiDocument document) throws XWikiException
    {
        return new DefaultPage(document).isVersioned();
    }

    @Override
    public boolean isVersionedContent(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext xcontext = this.getXWikiContext();

        return isVersionedContent(xcontext.getWiki().getDocument(documentReference, xcontext));
    }

    @Override
    public boolean isVersionedContent(XWikiDocument document) throws XWikiException
    {
        return new DefaultVersionedContent(document).isDefined();
    }

    @Override
    public boolean isPossibleVersionedContentReference(DocumentReference collectionReference,
        DocumentReference documentReference) throws XWikiException
    {
        if (collectionReference != null && documentReference != null) {
            DocumentReference versionRef = getVersionReference(collectionReference, documentReference.getName());
            if (versionRef != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isVersion(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext xcontext = this.getXWikiContext();

        return isVersion(xcontext.getWiki().getDocument(documentReference, xcontext));
    }

    @Override
    public boolean isVersion(XWikiDocument document) throws XWikiException
    {
        return new DefaultVersion(document).isDefined();
    }

    @Override
    public boolean isVariant(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext xcontext = this.getXWikiContext();

        return isVariant(xcontext.getWiki().getDocument(documentReference, xcontext));
    }

    private boolean isVariant(XWikiDocument document) throws XWikiException
    {
        return new DefaultVariant(document).isDefined();
    }

    @Override
    public boolean isLibrary(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext xcontext = this.getXWikiContext();

        return isLibrary(xcontext.getWiki().getDocument(documentReference, xcontext));
    }

    private boolean isLibrary(XWikiDocument document) throws XWikiException
    {
        return new DefaultLibrary(document).isDefined();
    }

    @Override
    public boolean isFromLibrary(DocumentReference libraryReference, DocumentReference libraryVersionReference)
        throws QueryException, XWikiException
    {
        for (String versionStringRef : getCollectionVersions(libraryReference)) {
            if (libraryVersionReference.equals(referenceResolver.resolve(versionStringRef, libraryVersionReference))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isFromLibrary(XWikiDocument library, XWikiDocument libraryVersion)
        throws QueryException, XWikiException
    {
        return isFromLibrary(library.getDocumentReference(), libraryVersion.getDocumentReference());
    }

    @Override
    public boolean isMarkedDeleted(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext xcontext = this.getXWikiContext();

        return isMarkedDeleted(xcontext.getWiki().getDocument(documentReference, xcontext));
    }

    @Override
    public boolean isMarkedDeleted(XWikiDocument document)
    {
        if (document.getXObject(BookVersionsConstants.MARKEDDELETED_CLASS_REFERENCE) != null)
        {
            return true;
        }
        return false;
    }

    @Override
    public String transformUsingSlugValidation(String name)
    {
        EntityNameValidation modelValidationScriptService = this.slugEntityNameValidationProvider.get();

        return modelValidationScriptService.transform(name);
    }

    @Override
    public String getSelectedVersion(DocumentReference documentReference) throws XWikiException, QueryException
    {
        if (documentReference == null) {
            return null;
        }

        DocumentReference versionedCollectionReference = getVersionedCollectionReference(documentReference);
        Map<String, String> versionsMap = new HashMap<String, String>();
        XWikiRequest request = getXWikiContext().getRequest();
        HttpSession session = request.getSession();
        if (session != null) {
            versionsMap = (Map<String, String>) session.getAttribute(BookVersionsConstants.SESSION_SELECTEDVERSION);

            if (versionsMap != null) {
                Iterator<?> it = versionsMap.entrySet().iterator();

                while (it.hasNext()) {
                    Map.Entry<String, String> collectionVersion = (Map.Entry<String, String>) it.next();
                    String collectionReference = collectionVersion.getKey();
                    if (!collectionReference.isBlank()
                        && collectionReference.equals(localSerializer.serialize(versionedCollectionReference))) {
                        return collectionVersion.getValue();
                    }

                }
            }
        }

        List<String> collectionVersions = getCollectionVersions(documentReference);
        if (collectionVersions.size() > 0) {
            return collectionVersions.get(0);
        }

        return null;
    }

    @Override
    public void setSelectedVersion(DocumentReference documentReference, String version)
    {
        if (documentReference == null) {
            return;
        }

        Map<String, String> versionsMap = new HashMap<String, String>();
        XWikiRequest request = getXWikiContext().getRequest();
        HttpSession session = request.getSession();
        if (session != null) {
            Object sessionAttribute = session.getAttribute(BookVersionsConstants.SESSION_SELECTEDVERSION);
            versionsMap = sessionAttribute != null ? (Map<String, String>) sessionAttribute : versionsMap;
            String collectionReferenceSerialized = localSerializer.serialize(documentReference);
            versionsMap.put(collectionReferenceSerialized, version);
            session.setAttribute(BookVersionsConstants.SESSION_SELECTEDVERSION, versionsMap);
        }
    }

    @Override
    public String getSelectedVariant(DocumentReference documentReference) throws XWikiException, QueryException
    {
        if (documentReference == null) {
            return null;
        }

        Map<String, String> variantsMap = new HashMap<String, String>();
        XWikiRequest request = getXWikiContext().getRequest();
        HttpSession session = request.getSession();
        if (session != null) {
            variantsMap = (Map<String, String>) session.getAttribute(BookVersionsConstants.SESSION_SELECTEDVARIANT);

            if (variantsMap != null) {
                Iterator<?> it = variantsMap.entrySet().iterator();
                DocumentReference versionedCollectionReference = getVersionedCollectionReference(documentReference);

                while (it.hasNext()) {
                    Map.Entry<String, String> collectionVariant = (Map.Entry<String, String>) it.next();
                    String collectionReference = collectionVariant.getKey();
                    if (!collectionReference.isBlank()
                        && collectionReference.equals(localSerializer.serialize(versionedCollectionReference))) {
                        return collectionVariant.getValue();
                    }

                }
            }
        }

        return null;
    }

    @Override
    public void setSelectedVariant(DocumentReference documentReference, String variant)
    {
        if (documentReference == null) {
            return;
        }

        Map<String, String> variantsMap = new HashMap<String, String>();
        XWikiRequest request = getXWikiContext().getRequest();
        HttpSession session = request.getSession();
        if (session != null) {
            Object sessionAttribute = session.getAttribute(BookVersionsConstants.SESSION_SELECTEDVARIANT);
            variantsMap = sessionAttribute != null ? (Map<String, String>) sessionAttribute : variantsMap;
            String collectionReferenceSerialized = localSerializer.serialize(documentReference);
            variantsMap.put(collectionReferenceSerialized, variant);
            session.setAttribute(BookVersionsConstants.SESSION_SELECTEDVARIANT, variantsMap);
        }
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
    private String getEscapedName(DocumentReference documentReference)
    {
        return getEscapedName(documentReference.getName());
    }

    /**
     * Get the name of the page, but escaped for dot character.
     * 
     * @param name the name to escape
     * @return the escaped name of the page
     */
    private String getEscapedName(String name)
    {
        String sequence = "\\.";
        String escapedName = name.replaceAll(sequence, sequence);

        logger.debug("[getEscapedName] escapedName : [{}]", escapedName);

        return escapedName;
    }

    @Override
    public DocumentReference getVersionReference(DocumentReference collectionReference, String version)
        throws XWikiException
    {
        // Search first for the terminal document : Book.Versions.MyVersion
        SpaceReference versionParentSpaceReference =
            new SpaceReference(new EntityReference(BookVersionsConstants.VERSIONS_LOCATION, EntityType.SPACE,
                collectionReference.getParent()));
        DocumentReference versionDocumentReference =
            new DocumentReference(new EntityReference(version, EntityType.DOCUMENT, versionParentSpaceReference));

        // Search for the non-terminal document : Book.Versions.MyVersion.WebHome
        if (!this.isVersion(versionDocumentReference)) {
            SpaceReference versionNonTerminalParentSpaceReference =
                new SpaceReference(new EntityReference(version, EntityType.SPACE, versionParentSpaceReference));
            versionDocumentReference =
                new DocumentReference(new EntityReference(this.getXWikiContext().getWiki().DEFAULT_SPACE_HOMEPAGE,
                    EntityType.DOCUMENT, versionNonTerminalParentSpaceReference));
        }
        return this.isVersion(versionDocumentReference) ? versionDocumentReference : null;
    }

    @Override
    public DocumentReference getVariantReference(DocumentReference collectionReference, String variant)
        throws XWikiException
    {
        // Search first for the terminal document : Book.Versions.MyVersion
        SpaceReference variantParentSpaceReference =
            new SpaceReference(new EntityReference(BookVersionsConstants.VARIANTS_LOCATION, EntityType.SPACE,
                collectionReference.getParent()));
        DocumentReference variantDocumentReference =
            new DocumentReference(new EntityReference(variant, EntityType.DOCUMENT, variantParentSpaceReference));

        // Search for the non-terminal document : Book.Versions.MyVersion.WebHome
        if (!this.isVariant(variantDocumentReference)) {
            SpaceReference versionNonTerminalParentSpaceReference =
                new SpaceReference(new EntityReference(variant, EntityType.SPACE, variantParentSpaceReference));
            variantDocumentReference =
                new DocumentReference(new EntityReference(this.getXWikiContext().getWiki().DEFAULT_SPACE_HOMEPAGE,
                    EntityType.DOCUMENT, versionNonTerminalParentSpaceReference));
        }
        return this.isVariant(variantDocumentReference) ? variantDocumentReference : null;
    }

    @Override
    public List<String> getCollectionVersions(DocumentReference collectionReference)
        throws QueryException, XWikiException
    {
        if (collectionReference != null) {
            DocumentReference versionedCollectionReference = getVersionedCollectionReference(collectionReference);

            if (versionedCollectionReference != null) {

                SpaceReference collectionSpace = versionedCollectionReference.getLastSpaceReference();
                String collectionSpaceSerialized = localSerializer.serialize(collectionSpace);
                String spacePrefix = collectionSpaceSerialized.replaceAll("([%_/])", "/$1").concat(".%");

                logger.debug("[getCollectionVersions] collectionSpaceSerialized : [{}]", collectionSpaceSerialized);
                logger.debug("[getCollectionVersions] spacePrefix : [{}]", spacePrefix);

                // Query inspired from getDocumentReferences of DefaultModelBridge.java in xwiki-platform
                List<String> result = this.queryManagerProvider.get()
                    .createQuery(", BaseObject as obj where doc.fullName = obj.name and obj.className = :versionClass "
                        + "and doc.space like :space escape '/' order by doc.creationDate desc", Query.HQL)
                    .bindValue("versionClass", localSerializer.serialize(BookVersionsConstants.VERSION_CLASS_REFERENCE))
                    .bindValue("space", spacePrefix).execute();

                logger.debug("[getCollectionVersions] result : [{}]", result);

                return result;
            }
        }

        return Collections.emptyList();
    }

    @Override
    public List<String> getCollectionVariants(DocumentReference collectionReference)
        throws QueryException, XWikiException
    {
        if (collectionReference != null) {
            DocumentReference versionedCollectionReference = getVersionedCollectionReference(collectionReference);

            if (versionedCollectionReference != null) {

                SpaceReference collectionSpace = versionedCollectionReference.getLastSpaceReference();
                String collectionSpaceSerialized = localSerializer.serialize(collectionSpace);
                String spacePrefix = collectionSpaceSerialized.replaceAll("([%_/])", "/$1").concat(".%");

                logger.debug("[getCollectionVersions] collectionSpaceSerialized : [{}]", collectionSpaceSerialized);
                logger.debug("[getCollectionVersions] spacePrefix : [{}]", spacePrefix);

                // Query inspired from getDocumentReferences of DefaultModelBridge.java in xwiki-platform
                List<String> result = this.queryManagerProvider.get()
                    .createQuery(", BaseObject as obj where doc.fullName = obj.name and obj.className = :variantClass "
                        + "and doc.space like :space escape '/' order by doc.creationDate desc", Query.HQL)
                    .bindValue("variantClass", localSerializer.serialize(BookVersionsConstants.VARIANT_CLASS_REFERENCE))
                    .bindValue("space", spacePrefix).execute();

                logger.debug("[getCollectionVersions] result : [{}]", result);

                return result;
            }
        }

        return Collections.emptyList();
    }

    /**
     * Search for the parent storing the collection type (book or library).
     */
    @Override
    public DocumentReference getVersionedCollectionReference(DocumentReference documentReference)
        throws XWikiException, QueryException
    {
        if (documentReference == null) {
            return null;
        }

        if (isBook(documentReference) || isLibrary(documentReference)) {
            return documentReference;
        }

        EntityReference entityReference = documentReference.getParent();

        if (entityReference != null) {

            // Check if the parent is a document.
            EntityReference documentEntityReference = entityReference.extractReference(EntityType.DOCUMENT);
            if (documentEntityReference != null) {
                DocumentReference parentDocumentReference = documentEntityReference instanceof DocumentReference
                    ? (DocumentReference) documentEntityReference : new DocumentReference(documentEntityReference);

                // Verify recursively if the parent is storing the collection definition.
                return getVersionedCollectionReference(parentDocumentReference);
            } else {
                // Check if the parent is a space.
                SpaceReference parentSpaceReference = getSpaceReference(entityReference);

                // Check if the parent itself is a collection
                DocumentReference parentDocumentReference = null;
                if (parentSpaceReference != null) {
                    parentDocumentReference = new DocumentReference(
                        this.getXWikiContext().getWiki().DEFAULT_SPACE_HOMEPAGE, parentSpaceReference);
                    if (isBook(parentDocumentReference) || isLibrary(parentDocumentReference)) {
                        return parentDocumentReference;
                    }
                }

                // If the parent is a space, but it's the last space of the given reference,
                // then go upper with one level, to the parent of the parent to avoid Stack Overflow.
                if (parentSpaceReference != null
                    && parentSpaceReference.equals(documentReference.getLastSpaceReference())) {
                    parentSpaceReference = getSpaceReference(parentSpaceReference.getParent());
                }

                // Get the document reference of the root for the parent space.
                parentDocumentReference = parentSpaceReference != null ? new DocumentReference(
                    this.getXWikiContext().getWiki().DEFAULT_SPACE_HOMEPAGE, parentSpaceReference) : null;

                // Verify recursively if this document is storing the collection definition.
                return getVersionedCollectionReference(parentDocumentReference);
            }
        }

        return null;
    }

    @Override
    public boolean hasContentForVersion(DocumentReference documentReference, String version)
        throws QueryException, XWikiException
    {
        if (version.isBlank() || documentReference == null) {
            return false;
        }
        XWikiContext xcontext = this.getXWikiContext();
        DocumentReference pageReference = documentReference;

        // If the reference is not a page, go to its parent, assuming that the current ref is a versioned content page.
        if (!isPage(documentReference)) {

            SpaceReference parentSpaceReference = getSpaceReference(documentReference);
            if (parentSpaceReference != null
                && parentSpaceReference.equals(documentReference.getLastSpaceReference())) {
                parentSpaceReference = getSpaceReference(parentSpaceReference.getParent());
            }
            pageReference =
                new DocumentReference(this.getXWikiContext().getWiki().DEFAULT_SPACE_HOMEPAGE, parentSpaceReference);

            // The parent is not a page either.
            if (!isPage(pageReference)) {
                return false;
            }
        }

        DocumentReference versionedContentReference = this.getVersionedContentReference(pageReference, version);
        return versionedContentReference != null && xcontext.getWiki().exists(versionedContentReference, xcontext);
    }

    @Override
    public String getVersionName(DocumentReference versionReference)
    {
        String versionName = getEscapedName(versionReference);
        if (versionName != null && versionName.equals(this.getXWikiContext().getWiki().DEFAULT_SPACE_HOMEPAGE)) {
            versionName = getEscapedName(versionReference.getLastSpaceReference().getName());
        }

        return versionName;
    }

    @Override
    public String getVariantName(DocumentReference variantReference)
    {
        String variantName = getEscapedName(variantReference);
        if (variantName != null && variantName.equals(this.getXWikiContext().getWiki().DEFAULT_SPACE_HOMEPAGE)) {
            variantName = getEscapedName(variantReference.getLastSpaceReference().getName());
        }

        return variantName;
    }

    @Override
    public DocumentReference getVersionedContentReference(DocumentReference documentReference)
        throws XWikiException, QueryException
    {
        return isVersionedPage(documentReference)
            ? getVersionedContentReference(documentReference, getSelectedVersion(documentReference)) : null;
    }

    @Override
    public DocumentReference getVersionedContentReference(XWikiDocument document) throws XWikiException, QueryException
    {
        if (this.isVersionedPage(document)) {
            DocumentReference versionedCollectionReference =
                getVersionedCollectionReference(document.getDocumentReference());

            return getVersionedContentReference(document.getDocumentReference(),
                getSelectedVersion(versionedCollectionReference));
        }

        return null;
    }

    @Override
    public DocumentReference getVersionedContentReference(DocumentReference documentReference, String version)
        throws QueryException, XWikiException
    {
        if (!version.isBlank()) {
            DocumentReference versionDocumentReference =
                referenceResolver.resolve(version).setWikiReference(this.getXWikiContext().getWikiReference());
            return getVersionedContentReference(documentReference, versionDocumentReference);
        }

        List<String> collectionVersions = getCollectionVersions(documentReference);
        if (collectionVersions.size() > 0) {
            return referenceResolver.resolve(collectionVersions.get(0))
                .setWikiReference(this.getXWikiContext().getWikiReference());
        }

        return null;
    }

    @Override
    public DocumentReference getVersionedContentReference(DocumentReference pageReference,
        DocumentReference versionReference)
    {
        String versionName = getVersionName(versionReference);

        logger.debug("[getVersionedContentReference] versionName : [{}]", versionName);

        return new DocumentReference(new EntityReference(versionName, EntityType.DOCUMENT, pageReference.getParent()));
    }

    @Override
    public DocumentReference getInheritedVersionedContentReference(DocumentReference documentReference)
        throws XWikiException, QueryException
    {
        DocumentReference versionDocumentReference = getVersionedContentReference(documentReference);

        if (versionDocumentReference != null) {
            return getInheritedContentVersionReference(documentReference, versionDocumentReference);
        }

        return documentReference;
    }

    @Override
    public DocumentReference getInheritedContentVersionReference(DocumentReference pageReference,
        DocumentReference versionReference) throws QueryException, XWikiException
    {
        if (pageReference != null && versionReference != null) {
            // TO DO: check if the page is unversioned, or not
            XWikiContext xcontext = this.getXWikiContext();
            XWiki xwiki = xcontext.getWiki();

            String versionName = getVersionName(versionReference);

            logger.debug("[getInheritedContentVersionReference] versionName : [{}]", versionName);

            while (!versionName.isEmpty() && versionName != null) {
                DocumentReference versionedContentRef = new DocumentReference(
                    new EntityReference(versionName, EntityType.DOCUMENT, pageReference.getParent()));

                logger.debug("[getInheritedContentVersionReference] versionedContentRef : [{}]", versionedContentRef);

                if (xwiki.exists(versionedContentRef, xcontext)) {
                    // Content exists for this version of the page
                    return versionedContentRef;
                } else {
                    // Content does not exists for this version. Lets check if there is content in a version to be
                    // inherited
                    return getPrecedingContentVersionReference(pageReference, versionReference);
                }
            }
        }

        return null;
    }

    private DocumentReference getPrecedingContentVersionReference(DocumentReference pageReference,
        DocumentReference versionReference) throws QueryException, XWikiException
    {
        XWikiContext xcontext = this.getXWikiContext();
        XWiki xwiki = xcontext.getWiki();

        if (versionReference == null) {
            // This is the first version in the tree, there's nothing to inherit from
            return null;
        }

        DocumentReference versionedContentReference = new DocumentReference(
            new EntityReference(getVersionName(versionReference), EntityType.DOCUMENT, pageReference.getParent()));

        if (xwiki.exists(versionedContentReference, xcontext)) {
            // Found the content corresponding to the given version
            return versionedContentReference;
        } else {
            // No versioned content found, so search in the preceding version
            return xwiki.exists(versionedContentReference, xcontext) ? versionedContentReference
                : getPrecedingContentVersionReference(pageReference, getPreviousVersion(versionReference));
        }
    }

    private DocumentReference getPreviousVersion(DocumentReference versionReference) throws XWikiException
    {
        XWikiContext xcontext = this.getXWikiContext();

        XWikiDocument versionDocument = xcontext.getWiki().getDocument(versionReference, xcontext);
        BaseObject versionObject = versionDocument.getXObject(BookVersionsConstants.VERSION_CLASS_REFERENCE);
        if (versionObject == null) {
            logger.warn("Version [{}] is missing the object [{}].", versionDocument.getDocumentReference().toString(),
                BookVersionsConstants.VERSION_CLASS_REFERENCE.toString());
            return null;
        }

        String previousVersion = versionObject.getStringValue(BookVersionsConstants.VERSION_PROP_PRECEDINGVERSION);
        return !previousVersion.isBlank()
            ? referenceResolver.resolve(previousVersion).setWikiReference(this.getXWikiContext().getWikiReference())
            : null;
    }

    @Override
    public DocumentReference getInheritedContentReference(DocumentReference pageReference,
        DocumentReference versionReference) throws QueryException, XWikiException
    {
        if (pageReference != null && versionReference != null) {
            XWikiContext xcontext = this.getXWikiContext();

            DocumentReference versionPageReference =
                getInheritedContentVersionReference(pageReference, versionReference);

            logger.debug("[getInheritedContentReference] versionPageReference : [{}]", versionPageReference);

            if (versionPageReference == null) {
                return null;
            }
            String versionName = getVersionName(versionPageReference);

            logger.debug("[getInheritedContentReference] versionName : [{}]", versionName);

            DocumentReference versionedContentRef =
                new DocumentReference(new EntityReference(versionName, EntityType.DOCUMENT, pageReference.getParent()));

            logger.debug("[getInheritedContentReference] versionedContentRef : [{}]", versionedContentRef);

            if (xcontext.getWiki().exists(versionedContentRef, xcontext)) {
                // Content exists for this version of the page
                return versionedContentRef;
            }
        }

        return null;
    }

    private SpaceReference getSpaceReference(EntityReference entityReference)
    {
        EntityReference spaceEntityReference = entityReference.extractReference(EntityType.SPACE);

        return entityReference != null && spaceEntityReference != null ? spaceEntityReference instanceof SpaceReference
            ? (SpaceReference) spaceEntityReference : new SpaceReference(spaceEntityReference) : null;
    }

    @Override
    public void setLibrary(DocumentReference bookReference, DocumentReference libraryReference)
        throws QueryException, XWikiException
    {
        setLibrary(bookReference, libraryReference,
            referenceResolver.resolve(getCollectionVersions(libraryReference).get(0),libraryReference));
    }

    @Override
    public void setLibrary(DocumentReference bookReference, DocumentReference libraryReference,
        DocumentReference libraryVersionReference)
        throws QueryException, XWikiException
    {
        if (isBook(bookReference) && isLibrary(libraryReference) && isFromLibrary(libraryReference, libraryVersionReference)) {
            List<String> versionsLocalRef = getCollectionVersions(bookReference);
            for (String versionLocalRef : versionsLocalRef) {
                DocumentReference versionRef = referenceResolver.resolve(versionLocalRef);
                setVersionLibrary(versionRef, libraryReference, libraryVersionReference);
            }
        }
    }

    private void setVersionLibrary(DocumentReference versionReference, DocumentReference libraryReference,
        DocumentReference libraryVersionReference)
        throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();
        XWiki xwiki = xcontext.getWiki();

        XWikiDocument versionDoc = xwiki.getDocument(versionReference, xcontext).clone();
        List<BaseObject> libRefObjects = versionDoc.getXObjects(BookVersionsConstants.BOOKLIBRARYREFERENCE_CLASS_REFERENCE);
        boolean createObject = true;
        for (BaseObject libRefObject : libRefObjects) {
            if (libRefObject != null) {
                if (libraryReference.equals(referenceResolver.resolve(libRefObject.getStringValue(BookVersionsConstants.BOOKLIBRARYREFERENCE_PROP_LIBRARY),
                    libraryReference))) {
                    if (libraryVersionReference.equals(referenceResolver.resolve(libRefObject.getStringValue(
                        BookVersionsConstants.BOOKLIBRARYREFERENCE_PROP_LIBRARYVERSION), libraryVersionReference)))
                    {
                        // The version library is already set with the proper value, nothing to do
                        return;
                    } else {
                        // The library configuration object already exists but without the proper value
                        libRefObject.set(BookVersionsConstants.BOOKLIBRARYREFERENCE_PROP_LIBRARYVERSION,
                            libraryVersionReference, xcontext);
                        createObject = false;
                    }
                }
            }
        }
        if (createObject) {
            // No object already existing for the library configuration, add one
            BaseObject newObject = versionDoc.newXObject(BookVersionsConstants.BOOKLIBRARYREFERENCE_CLASS_REFERENCE,
                xcontext);
            newObject.set(BookVersionsConstants.BOOKLIBRARYREFERENCE_PROP_LIBRARY, libraryReference, xcontext);
            newObject.set(BookVersionsConstants.BOOKLIBRARYREFERENCE_PROP_LIBRARYVERSION,libraryVersionReference, xcontext);
        }
        xwiki.saveDocument(versionDoc, "Setting version configuration for library ["+libraryReference.getParent().toString()+
                "]: ["+libraryVersionReference.toString()+"]." , xcontext);
    }

    @Override
    public DocumentReference getConfiguredLibraryVersion(DocumentReference bookReference, DocumentReference libraryReference)
        throws XWikiException, QueryException
    {
        if(isBook(bookReference) && isLibrary(libraryReference)) {
            String selectedVersionStringRef = getSelectedVersion(bookReference);
            if (selectedVersionStringRef == null || selectedVersionStringRef.isEmpty()) {
                // in case no version has been selected yet, get the most recent one
                selectedVersionStringRef = getCollectionVersions(bookReference).get(0);
            }
            DocumentReference selectedVersionRef = referenceResolver.resolve(selectedVersionStringRef, bookReference);
            XWikiContext xcontext = getXWikiContext();
            XWiki xwiki = xcontext.getWiki();
            XWikiDocument selectedVersionDoc = xwiki.getDocument(selectedVersionRef, xcontext);
            List<BaseObject> libRefObjects =
                selectedVersionDoc.getXObjects(BookVersionsConstants.BOOKLIBRARYREFERENCE_CLASS_REFERENCE);
            for (BaseObject libRefObject : libRefObjects) {
                if (libraryReference.equals(referenceResolver.resolve(libRefObject.getStringValue(BookVersionsConstants.BOOKLIBRARYREFERENCE_PROP_LIBRARY),
                    libraryReference))) {
                    return referenceResolver.resolve(libRefObject.getStringValue(
                        BookVersionsConstants.BOOKLIBRARYREFERENCE_PROP_LIBRARYVERSION));
                }
            }
        }
        return null;
    }

    @Override
    public DocumentReference getLinkedLibraryContentReference(DocumentReference bookReference, DocumentReference keyReference)
        throws XWikiException, QueryException
    {
        DocumentReference libraryRef = getVersionedCollectionReference(keyReference);
        if (keyReference != null && libraryRef != null && isLibrary(libraryRef) && isPage(keyReference)) {
            // the passed reference is part of a library
            if (isVersionedPage(keyReference)) {
                // versioned page => get the content depending on the book configuration
                DocumentReference libraryVersionRef = getConfiguredLibraryVersion(bookReference,libraryRef);
                return getInheritedContentReference(keyReference,libraryVersionRef);
            } else {
                // unversioned page
                return keyReference;
            }
        }
        return null;
    }

    @Override
    public void switchDeletedMark(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext xcontext = this.getXWikiContext();
        XWiki xwiki = xcontext.getWiki();

        XWikiDocument document = xwiki.getDocument(documentReference, xcontext).clone();

        if (isMarkedDeleted(document)) {
            BaseObject object = document.getXObject(BookVersionsConstants.MARKEDDELETED_CLASS_REFERENCE);
            document.removeXObject(object);
            xwiki.saveDocument(document,"Unmarked document as \"Deleted\"",xcontext);
        } else {
            document.newXObject(BookVersionsConstants.MARKEDDELETED_CLASS_REFERENCE,xcontext);
            xwiki.saveDocument(document,"Marked document as \"Deleted\"",xcontext);
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
