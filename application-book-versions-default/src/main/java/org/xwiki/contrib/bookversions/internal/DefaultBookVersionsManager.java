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
import org.xwiki.model.validation.script.ModelValidationScriptService;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiRequest;
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
        XWikiContext xcontext = this.getXWikiContext();

        return isBook(this.getXWikiContext().getWiki().getDocument(documentReference, xcontext));
    }

    private boolean isBook(XWikiDocument document) throws XWikiException
    {
        return new DefaultBook(document).isDefined();
    }

    @Override
    public boolean isVersionedBook(DocumentReference documentReference) throws XWikiException, QueryException
    {
        XWikiContext xcontext = this.getXWikiContext();
        XWiki xwiki = xcontext.getWiki();

        return (new DefaultPage(xwiki.getDocument(documentReference, xcontext)).isVersioned()
            && !getCollectionVersions(documentReference).isEmpty());
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
    public boolean isVersion(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext xcontext = this.getXWikiContext();

        return isVersion(xcontext.getWiki().getDocument(documentReference, xcontext));
    }

    private boolean isVersion(XWikiDocument document) throws XWikiException
    {
        return new DefaultBook(document).isDefined();
    }

    @Override
    public boolean isVariant(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext xcontext = this.getXWikiContext();

        return isVariant(xcontext.getWiki().getDocument(documentReference, xcontext));
    }

    private boolean isVariant(XWikiDocument document) throws XWikiException
    {
        return new DefaultBook(document).isDefined();
    }

    @Override
    public boolean isLibrary(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext xcontext = this.getXWikiContext();

        return isLibrary(xcontext.getWiki().getDocument(documentReference, xcontext));
    }

    private boolean isLibrary(XWikiDocument document) throws XWikiException
    {
        return new DefaultBook(document).isDefined();
    }

    @Override
    public String transformUsingSlugValidation(String name)
    {
        ModelValidationScriptService modelValidationScriptService = this.modelValidationScriptServiceProvider.get();

        return modelValidationScriptService.transformName(name, BookVersionsConstants.SLUGVALIDATION_HINT);
    }

    @Override
    public String getSelectedVersion(DocumentReference documentReference)
    {
        if (documentReference == null) {
            return null;
        }

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
                        && collectionReference.equals(localSerializer.serialize(documentReference))) {
                        return collectionVersion.getValue();
                    }

                }
            }
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
        String sequence = "\\.";
        String escapedName = documentReference.getName().replaceAll(sequence, sequence);

        logger.debug("[getEscapedName] escapedName : [{}]", escapedName);

        return escapedName;
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

                // If so and but it's the last space of the given reference,
                // then go upper with one level, to the parent of the parent to avoid Stack Overflow.
                if (parentSpaceReference != null
                    && parentSpaceReference.equals(documentReference.getLastSpaceReference())) {
                    parentSpaceReference = getSpaceReference(parentSpaceReference.getParent());
                }

                // Get the document reference of the root for the parent space.
                DocumentReference parentDocumentReference = parentSpaceReference != null ? new DocumentReference(
                    this.getXWikiContext().getWiki().DEFAULT_SPACE_HOMEPAGE, parentSpaceReference) : null;

                // Verify recursively if this document is storing the collection definition.
                return getVersionedCollectionReference(parentDocumentReference);
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
    public DocumentReference getVersionedContentReference(DocumentReference documentReference)
        throws XWikiException, QueryException
    {
        if (this.isVersionedPage(documentReference)) {
            DocumentReference versionDocumentReference = null;
            String selectedVersion = getSelectedVersion(documentReference);

            if (selectedVersion == null) {
                // If no version has been selected, then start with the latest version
                List<String> collectionVersions = getCollectionVersions(documentReference);
                if (collectionVersions.size() > 0) {
                    versionDocumentReference = referenceResolver.resolve(collectionVersions.get(0))
                        .setWikiReference(this.getXWikiContext().getWikiReference());
                    selectedVersion = this.getEscapedName(versionDocumentReference);
                }
            } else {
                // Start with the selected version
                DocumentReference collectionReference = this.getVersionedCollectionReference(documentReference);
                versionDocumentReference = new DocumentReference(
                    new EntityReference(selectedVersion, EntityType.DOCUMENT, collectionReference.getParent()));
            }

            if (versionDocumentReference != null) {
                return getInheritedContentVersionReference(documentReference, versionDocumentReference);
            }
        }

        return documentReference;
    }

    @Override
    public DocumentReference getVersionedContentReference(DocumentReference pageReference,
        DocumentReference versionReference)
    {
        String versionName = getEscapedName(versionReference);

        logger.debug("[getVersionedContentReference] versionName : [{}]", versionName);

        return new DocumentReference(new EntityReference(versionName, EntityType.DOCUMENT, pageReference.getParent()));
    }

    @Override
    public DocumentReference getInheritedContentVersionReference(DocumentReference pageReference,
        DocumentReference versionReference) throws QueryException, XWikiException
    {
        if (pageReference != null && versionReference != null) {
            // TO DO: check if the page is unversioned, or not
            XWikiContext xcontext = this.getXWikiContext();
            XWiki xwiki = xcontext.getWiki();

            String versionName = getEscapedName(versionReference);

            logger.debug("[getInheritedContentVersionReference] versionName : [{}]", versionName);

            while (!versionName.isEmpty() && versionName != null) {
                DocumentReference versionedContentRef = new DocumentReference(
                    new EntityReference(versionName, EntityType.DOCUMENT, pageReference.getParent()));

                logger.debug("[getInheritedContentVersionReference] versionedContentRef : [{}]", versionedContentRef);

                if (xwiki.exists(versionedContentRef, xcontext)) {
                    // Content exists for this version of the page
                    return versionedContentRef;
                } else {
                    XWikiDocument versionDoc = xwiki.getDocument(versionReference, xcontext);
                    BaseObject versionObject = versionDoc.getXObject(BookVersionsConstants.VERSION_CLASS_REFERENCE);
                    if (versionObject == null) {
                        logger.warn("Could not find [{}] object in version document [{}].",
                            BookVersionsConstants.VERSION_CLASS_REFERENCE.toString(), versionReference);
                        return null;
                    }
                    // Content does not exists for this version. Lets check if there is content in a version to be
                    // inherited
                    String precedingVersion =
                        versionObject.getStringValue(BookVersionsConstants.VERSION_PROP_PRECEDINGVERSION);
                    return this.getPrecedingContentVersionReference(pageReference, versionReference, versionName,
                        precedingVersion);
                }
            }
        }

        return null;
    }

    private DocumentReference getPrecedingContentVersionReference(DocumentReference pageReference,
        DocumentReference versionReference, String currentVersion, String precedingVersion)
        throws QueryException, XWikiException
    {
        if (precedingVersion.isEmpty()) {
            // This is the first version in the tree, there's nothing to inherit from
            return null;
        }
        DocumentReference precedingVersionRef =
            this.referenceResolver.resolve(precedingVersion, versionReference.getParent())
                .setWikiReference(this.getXWikiContext().getWikiReference());
        String precedingVersionName = getEscapedName(precedingVersionRef);
        if (!precedingVersionName.equals(currentVersion)) {
            // The preceding version exists, so inherit from it.
            return new DocumentReference(
                new EntityReference(precedingVersionName, EntityType.DOCUMENT, pageReference.getParent()));
        } else {
            // Search in the existing versions, in reverse order, to check for the one to inherit from.
            List<String> getCollectionVersions = getCollectionVersions(pageReference);

            for (String collectionVersion : getCollectionVersions) {
                if (collectionVersion != currentVersion) {
                    XWikiContext xcontext = this.getXWikiContext();
                    XWikiDocument versionDoc = xcontext.getWiki().getDocument(versionReference, xcontext);
                    BaseObject versionObject = versionDoc.getXObject(BookVersionsConstants.VERSION_CLASS_REFERENCE);
                    if (versionObject == null) {
                        logger.warn("Version [{}] is missing the object [{}].",
                            versionDoc.getDocumentReference().toString(),
                            BookVersionsConstants.VERSION_CLASS_REFERENCE.toString());
                        return null;
                    }
                    return this.getPrecedingContentVersionReference(pageReference, versionDoc.getDocumentReference(),
                        precedingVersion,
                        versionObject.getStringValue(BookVersionsConstants.VERSION_PROP_PRECEDINGVERSION));
                }
            }

            // Avoiding an infinite loop
            logger.warn("Version page [" + precedingVersion + "] is referencing itself as a preceding " + "version");
        }

        return null;
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
            String versionName = getEscapedName(versionPageReference);

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
