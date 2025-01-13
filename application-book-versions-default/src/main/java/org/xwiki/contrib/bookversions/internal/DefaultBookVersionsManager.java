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

import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.bookversions.BookVersionsManager;
import org.xwiki.contrib.bookversions.PageTranslationStatus;
import org.xwiki.job.DefaultRequest;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
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
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
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
    private static final ClassBlockMatcher MACRO_MATCHER = new ClassBlockMatcher(MacroBlock.class);

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Provider<QueryManager> queryManagerProvider;

    @Inject
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentMixedReferenceResolver;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    @Named("SlugEntityNameValidation")
    private Provider<EntityNameValidation> slugEntityNameValidationProvider;

    @Inject
    private Logger logger;

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private JobProgressManager progressManager;

    @Inject
    @Named("context/root")
    private Provider<ComponentManager> contextrootComponentManagerProvider;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

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
        if (document.getXObject(BookVersionsConstants.MARKEDDELETED_CLASS_REFERENCE) != null) {
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
    public List<DocumentReference> getPageVariants(XWikiDocument page)
    {
        List<DocumentReference> result = new ArrayList<DocumentReference>();
        if (page == null) {
            return result;
        }
        BaseObject variantListObj = page.getXObject(BookVersionsConstants.VARIANTLIST_CLASS_REFERENCE);
        if (variantListObj == null) {
            return result;
        }
        for (String referenceString :
            (List<String>) variantListObj.getListValue(BookVersionsConstants.VARIANTLIST_PROP_VARIANTSLIST)) {
            result.add(referenceResolver.resolve(referenceString));
        }
        return result;
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
    public String getSelectedLanguage(DocumentReference documentReference) throws XWikiException, QueryException
    {
        if (documentReference == null) {
            return null;
        }

        Map<String, String> languagesMap = new HashMap<String, String>();
        XWikiRequest request = getXWikiContext().getRequest();
        HttpSession session = request.getSession();
        if (session != null) {
            languagesMap = (Map<String, String>) session.getAttribute(BookVersionsConstants.SESSION_SELECTEDLANGUAGE);

            if (languagesMap != null) {
                Iterator<?> it = languagesMap.entrySet().iterator();
                DocumentReference versionedCollectionReference = getVersionedCollectionReference(documentReference);

                while (it.hasNext()) {
                    Map.Entry<String, String> collectionLanguage = (Map.Entry<String, String>) it.next();
                    String collectionReference = collectionLanguage.getKey();
                    if (!collectionReference.isBlank()
                        && collectionReference.equals(localSerializer.serialize(versionedCollectionReference))) {
                        return collectionLanguage.getValue();
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void setSelectedLanguage(DocumentReference documentReference, String language)
    {
        if (documentReference == null) {
            return;
        }

        Map<String, String> languagesMap = new HashMap<String, String>();
        XWikiRequest request = getXWikiContext().getRequest();
        HttpSession session = request.getSession();
        if (session != null) {
            Object sessionAttribute = session.getAttribute(BookVersionsConstants.SESSION_SELECTEDLANGUAGE);
            languagesMap = sessionAttribute != null ? (Map<String, String>) sessionAttribute : languagesMap;
            String collectionReferenceSerialized = localSerializer.serialize(documentReference);
            languagesMap.put(collectionReferenceSerialized, language);
            session.setAttribute(BookVersionsConstants.SESSION_SELECTEDLANGUAGE, languagesMap);
        }
    }

    @Override
    public String getDefaultTranslation(DocumentReference documentReference) throws XWikiException, QueryException
    {
        XWikiContext xcontext = this.getXWikiContext();

        return getDefaultTranslation(xcontext.getWiki().getDocument(documentReference, xcontext));
    }

    private String getDefaultTranslation(XWikiDocument document) throws XWikiException, QueryException
    {
        Map<String, Map<String, Object>> languageData = getLanguageData(document);

        for (Entry<String, Map<String, Object>> languageDataEntry : languageData.entrySet()) {
            if ((boolean) languageDataEntry.getValue().get(BookVersionsConstants.PAGETRANSLATION_ISDEFAULT)) {
                return (String) languageDataEntry.getKey();
            }
        }

        return null;
    }

    @Override
    public String getTranslatedTitle(DocumentReference documentReference) throws XWikiException, QueryException
    {
        XWikiContext xcontext = this.getXWikiContext();

        return getTranslatedTitle(xcontext.getWiki().getDocument(documentReference, xcontext));
    }

    @Override
    public String getTranslatedTitle(XWikiDocument document) throws XWikiException, QueryException
    {
        DocumentReference collectionRef = getVersionedCollectionReference(document.getDocumentReference());
        String selectedLanguage = getSelectedLanguage(collectionRef);

        if (selectedLanguage == null) {
            selectedLanguage = this.getDefaultTranslation(document);
        }

        return selectedLanguage != null ? getTranslatedTitle(document, selectedLanguage) : null;
    }

    @Override
    public String getTranslatedTitle(DocumentReference documentReference, String language)
        throws XWikiException, QueryException
    {
        XWikiContext xcontext = this.getXWikiContext();

        return getTranslatedTitle(xcontext.getWiki().getDocument(documentReference, xcontext), language);
    }

    @Override
    public String getTranslatedTitle(XWikiDocument document, String language) throws XWikiException, QueryException
    {
        String title = null;

        for (BaseObject tObj : document.getXObjects(BookVersionsConstants.PAGETRANSLATION_CLASS_REFERENCE)) {
            String languageEntry = tObj.getStringValue(BookVersionsConstants.PAGETRANSLATION_LANGUAGE);
            if (languageEntry != null && !languageEntry.isEmpty() && languageEntry.equals(language)) {
                title = tObj.getStringValue(BookVersionsConstants.PAGETRANSLATION_TITLE);

            }
        }

        return title != null && !title.isEmpty() ? title : getInheritedTitle(document);
    }

    private String getInheritedTitle(XWikiDocument document) throws XWikiException
    {
        if (isVersionedContent(document)) {

            DocumentReference documentReference = document.getDocumentReference();
            SpaceReference parentSpaceReference = getSpaceReference(documentReference);
            if (parentSpaceReference != null) {
                XWikiContext xcontext = this.getXWikiContext();

                DocumentReference parentPageReference =
                    new DocumentReference(xcontext.getWiki().DEFAULT_SPACE_HOMEPAGE, parentSpaceReference);
                // The parent is a book page, so use its title.
                if (isPage(parentPageReference)) {
                    return xcontext.getWiki().getDocument(parentPageReference, xcontext).getTitle();
                }
            }

        }

        return document.getDocumentReference().getName();
    }

    @Override
    public String getTranslationStatus(DocumentReference documentReference, String language)
        throws XWikiException, QueryException
    {
        XWikiContext xcontext = this.getXWikiContext();

        return getTranslationStatus(xcontext.getWiki().getDocument(documentReference, xcontext), language);
    }

    @Override
    public String getTranslationStatus(XWikiDocument document) throws XWikiException, QueryException
    {
        DocumentReference collectionRef = getVersionedCollectionReference(document.getDocumentReference());
        String selectedLanguage = getSelectedLanguage(collectionRef);

        BaseObject translationObject = null;
        for (BaseObject tObj : document.getXObjects(BookVersionsConstants.PAGETRANSLATION_CLASS_REFERENCE)) {
            String languageEntry = tObj.getStringValue(BookVersionsConstants.PAGETRANSLATION_LANGUAGE);
            if (!languageEntry.isEmpty() && languageEntry.equals(selectedLanguage)) {
                translationObject = tObj;
                break;
            }
        }

        return translationObject != null
            ? translationObject.getStringValue(BookVersionsConstants.PAGETRANSLATION_STATUS) : null;
    }

    @Override
    public String getTranslationStatus(XWikiDocument document, String language) throws XWikiException, QueryException
    {
        for (BaseObject tObj : document.getXObjects(BookVersionsConstants.PAGETRANSLATION_CLASS_REFERENCE)) {
            String languageEntry = tObj.getStringValue(BookVersionsConstants.PAGETRANSLATION_LANGUAGE);
            if (!languageEntry.isEmpty() && languageEntry.equals(language)) {
                return tObj.getStringValue(BookVersionsConstants.PAGETRANSLATION_STATUS);
            }
        }

        return null;
    }

    @Override
    public boolean isDefaultLanguage(DocumentReference documentReference, String language)
        throws XWikiException, QueryException
    {
        XWikiContext xcontext = this.getXWikiContext();

        return isDefaultLanguage(xcontext.getWiki().getDocument(documentReference, xcontext), language);
    }

    @Override
    public boolean isDefaultLanguage(XWikiDocument document, String language) throws XWikiException, QueryException
    {
        for (BaseObject tObj : document.getXObjects(BookVersionsConstants.PAGETRANSLATION_CLASS_REFERENCE)) {
            String languageEntry = tObj.getStringValue(BookVersionsConstants.PAGETRANSLATION_LANGUAGE);
            if (!languageEntry.isEmpty() && languageEntry.equals(language)) {
                int isDefault = tObj.getIntValue(BookVersionsConstants.PAGETRANSLATION_ISDEFAULT);
                if (isDefault == 1) {
                    return true;
                }
                break;
            }
        }

        return false;
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

            return queryPages(versionedCollectionReference, BookVersionsConstants.VERSION_CLASS_REFERENCE);
        }

        return Collections.emptyList();
    }

    @Override
    public List<String> getCollectionVariants(DocumentReference collectionReference)
        throws QueryException, XWikiException
    {
        if (collectionReference != null) {
            DocumentReference versionedCollectionReference = getVersionedCollectionReference(collectionReference);

            return queryPages(versionedCollectionReference, BookVersionsConstants.VARIANT_CLASS_REFERENCE);
        }

        return Collections.emptyList();
    }

    private List<String> queryPages(DocumentReference documentReference, EntityReference classReference)
        throws QueryException
    {
        if (documentReference != null) {
            logger.debug("[queryPages] Query pages with class [{}] under [{}]", classReference.toString(),
                documentReference.toString());

            SpaceReference spaceReference = documentReference.getLastSpaceReference();
            String spaceSerialized = localSerializer.serialize(spaceReference);
            String spacePrefix = spaceSerialized.replaceAll("([%_/])", "/$1").concat(".%");

            logger.debug("[queryPages] spaceSerialized : [{}]", spaceSerialized);
            logger.debug("[queryPages] spacePrefix : [{}]", spacePrefix);

            // Query inspired from getDocumentReferences of DefaultModelBridge.java in xwiki-platform
            List<String> result = this.queryManagerProvider.get()
                .createQuery(", BaseObject as obj where doc.fullName = obj.name and obj.className = :class "
                    + "and doc.space like :space escape '/' order by doc.creationDate desc", Query.HQL)
                .bindValue("class", localSerializer.serialize(classReference)).bindValue("space",
                    spacePrefix)
                .execute();

            logger.debug("[queryPages] result : [{}]", result);

            return result;
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
            referenceResolver.resolve(getCollectionVersions(libraryReference).get(0), libraryReference));
    }

    @Override
    public void setLibrary(DocumentReference bookReference, DocumentReference libraryReference,
        DocumentReference libraryVersionReference) throws QueryException, XWikiException
    {
        if (isBook(bookReference) && isLibrary(libraryReference)
            && isFromLibrary(libraryReference, libraryVersionReference)) {
            List<String> versionsLocalRef = getCollectionVersions(bookReference);
            for (String versionLocalRef : versionsLocalRef) {
                DocumentReference versionRef = referenceResolver.resolve(versionLocalRef);
                setVersionLibrary(versionRef, libraryReference, libraryVersionReference);
            }
        }
    }

    private void setVersionLibrary(DocumentReference versionReference, DocumentReference libraryReference,
        DocumentReference libraryVersionReference) throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();
        XWiki xwiki = xcontext.getWiki();

        XWikiDocument versionDoc = xwiki.getDocument(versionReference, xcontext).clone();
        List<BaseObject> libRefObjects =
            versionDoc.getXObjects(BookVersionsConstants.BOOKLIBRARYREFERENCE_CLASS_REFERENCE);
        boolean createObject = true;
        for (BaseObject libRefObject : libRefObjects) {
            if (libRefObject != null) {
                if (libraryReference.equals(referenceResolver.resolve(
                    libRefObject.getStringValue(BookVersionsConstants.BOOKLIBRARYREFERENCE_PROP_LIBRARY),
                    libraryReference))) {
                    if (libraryVersionReference.equals(referenceResolver.resolve(
                        libRefObject.getStringValue(BookVersionsConstants.BOOKLIBRARYREFERENCE_PROP_LIBRARYVERSION),
                        libraryVersionReference))) {
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
            BaseObject newObject =
                versionDoc.newXObject(BookVersionsConstants.BOOKLIBRARYREFERENCE_CLASS_REFERENCE, xcontext);
            newObject.set(BookVersionsConstants.BOOKLIBRARYREFERENCE_PROP_LIBRARY, libraryReference, xcontext);
            newObject.set(BookVersionsConstants.BOOKLIBRARYREFERENCE_PROP_LIBRARYVERSION, libraryVersionReference,
                xcontext);
        }
        xwiki.saveDocument(versionDoc, "Setting version configuration for library ["
            + libraryReference.getParent().toString() + "]: [" + libraryVersionReference.toString() + "].", xcontext);
    }

    @Override
    public DocumentReference getConfiguredLibraryVersion(DocumentReference bookReference,
        DocumentReference libraryReference) throws XWikiException, QueryException
    {
        if (isBook(bookReference) && isLibrary(libraryReference)) {
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
                if (libraryReference.equals(referenceResolver.resolve(
                    libRefObject.getStringValue(BookVersionsConstants.BOOKLIBRARYREFERENCE_PROP_LIBRARY),
                    libraryReference))) {
                    return referenceResolver.resolve(
                        libRefObject.getStringValue(BookVersionsConstants.BOOKLIBRARYREFERENCE_PROP_LIBRARYVERSION));
                }
            }
        }
        return null;
    }

    @Override
    public DocumentReference getLinkedLibraryContentReference(DocumentReference bookReference,
        DocumentReference keyReference) throws XWikiException, QueryException
    {
        DocumentReference libraryRef = getVersionedCollectionReference(keyReference);
        if (keyReference != null && libraryRef != null && isLibrary(libraryRef) && isPage(keyReference)) {
            // the passed reference is part of a library
            if (isVersionedPage(keyReference)) {
                // versioned page => get the content depending on the book configuration
                DocumentReference libraryVersionRef = getConfiguredLibraryVersion(bookReference, libraryRef);
                return getInheritedContentReference(keyReference, libraryVersionRef);
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
            xwiki.saveDocument(document, "Unmarked document as \"Deleted\"", xcontext);
        } else {
            document.newXObject(BookVersionsConstants.MARKEDDELETED_CLASS_REFERENCE, xcontext);
            xwiki.saveDocument(document, "Marked document as \"Deleted\"", xcontext);
        }
    }

    @Override
    public String publish(DocumentReference configurationReference) throws JobException
    {
        DefaultRequest jobRequest = new DefaultRequest();
        String jobId = BookVersionsConstants.PUBLICATION_JOBID_PREFIX
            + BookVersionsConstants.PUBLICATION_JOBID_SEPARATOR
            + configurationReference.getName() + BookVersionsConstants.PUBLICATION_JOBID_SEPARATOR
            + Instant.now().toString();
        jobRequest.setId(jobId);
        jobRequest.setProperty("configurationReference", configurationReference);
        jobExecutor.execute(BookVersionsConstants.PUBLICATIONJOB_TYPE, jobRequest);
        return jobId;
    }

    @Override
    public Map<String, Object> loadPublicationConfiguration(DocumentReference configurationReference)
        throws XWikiException
    {
        Map<String, Object> configuration = new HashMap<String, Object>();
        logger.debug("[loadPublicationConfiguration] Loading publication configuration from [{}]",
            configurationReference);
        if (configurationReference == null) {
            logger.error("Configuration reference is null");
            return configuration;
        }

        XWikiContext xcontext = this.getXWikiContext();
        XWiki xwiki = xcontext.getWiki();
        XWikiDocument configurationDoc = xwiki.getDocument(configurationReference, xcontext);
        BaseObject configurationObject =
            configurationDoc.getXObject(BookVersionsConstants.PUBLICATIONCONFIGURATION_CLASS_REFERENCE);

        if (configurationObject == null) {
            logger.error("[loadPublicationConfiguration] Configuration page has no [{}]",
                BookVersionsConstants.PUBLICATIONCONFIGURATION_CLASS_REFERENCE);
            return configuration;
        }

        logger.info("Loading configuration.");

        String sourceReferenceString =
            configurationObject.getStringValue(BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_SOURCE);
        String destinationReferenceString =
            configurationObject.getStringValue(BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_DESTINATIONSPACE);
        String versionReferenceString =
            configurationObject.getStringValue(BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_VERSION);
        if (sourceReferenceString.isBlank() || destinationReferenceString.isBlank()
            || versionReferenceString.isBlank()) {
            logger.error("One of the mandatory element in the configuration (source, destination or version) is "
                + "missing.");
            return configuration;
        }
        configuration.put(BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_SOURCE,
            referenceResolver.resolve(sourceReferenceString, configurationReference.getWikiReference()));
        configuration.put(BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_DESTINATIONSPACE,
            referenceResolver.resolve(destinationReferenceString, configurationReference.getWikiReference()));
        configuration.put(BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_VERSION,
            referenceResolver.resolve(versionReferenceString, configurationReference.getWikiReference()));

        String variantReferenceString =
            configurationObject.getStringValue(BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_VARIANT);
        configuration.put(BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_VARIANT,
            variantReferenceString.isBlank() ? null : referenceResolver.resolve(variantReferenceString,
                configurationReference.getWikiReference()));

        configuration.put(BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_LANGUAGE,
            configurationObject.getStringValue(BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_LANGUAGE));
        configuration.put(BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_PUBLISHONLYCOMPLETE,
            configurationObject.getIntValue(BookVersionsConstants.
                PUBLICATIONCONFIGURATION_PROP_PUBLISHONLYCOMPLETE) != 0);
        configuration.put(BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_PUBLISHPAGEORDER,
            configurationObject.getIntValue(BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_PUBLISHPAGEORDER) != 0);
        configuration.put(BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_PUBLISHBEHAVIOUR,
            configurationObject.getStringValue(BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_PUBLISHBEHAVIOUR));

        logger.debug("[loadPublicationConfiguration] Configuration loaded: [{}].", configuration);
        logger.info("Configuration loaded.");

        return configuration;
    }

    @Override
    public void publishInternal(DocumentReference configurationReference)
        throws XWikiException, QueryException, ComponentLookupException, ParseException
    {
        logger.debug("[publish] Publication required with configuration [{}]", configurationReference);
        logger.info("Starting publication job with configuration [{}].", configurationReference);

        Map<String, Object> configuration = loadPublicationConfiguration(configurationReference);
        if (configuration.isEmpty()) {
            return;
        }
        XWikiContext xcontext = this.getXWikiContext();
        XWiki xwiki = xcontext.getWiki();
        DocumentReference sourceReference = (DocumentReference) configuration.get(
                BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_SOURCE);
        DocumentReference collectionReference = getVersionedCollectionReference(sourceReference);
        XWikiDocument collection = xwiki.getDocument(collectionReference, xcontext);
        DocumentReference versionReference = (DocumentReference) configuration.get(
            BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_VERSION);
        XWikiDocument version = xwiki.getDocument(versionReference,xcontext);
        String publicationComment = "Published from [" + collection.getTitle()+ "], version [" + version.getTitle()
            + "].";

        // Execute publication job
        logger.info("Start publication.");
        List<String> pageReferenceTree = getPageReferenceTree(sourceReference);
        int i = 1;
        int pageQuantity = pageReferenceTree.size();
        progressManager.pushLevelProgress(pageReferenceTree.size(), this);
        DocumentReference targetTopReference = null;
        for (String pageStringReference : pageReferenceTree) {
            progressManager.startStep(this, pageStringReference);
            logger.info("Page publication {}/{}: [{}]", i, pageQuantity, pageStringReference);
            i++;
            DocumentReference pageReference = referenceResolver.resolve(pageStringReference, configurationReference);
            XWikiDocument page = xwiki.getDocument(pageReference, xcontext);

            // Get the relevant content for the page
            DocumentReference contentPageReference = getContentPage(page, configuration);
            logger.debug("[publish] For page [{}], the content will be taken from [{}]", page.getDocumentReference(),
                contentPageReference);
            if (contentPageReference == null) {
                continue;
            }

            // Check if the content should be published
            XWikiDocument contentPage = xwiki.getDocument(contentPageReference, xcontext).clone();
            if (!isToBePublished(contentPage, configuration)) {
                // TODO: page shouldn't be ignored if it contains ordering and publishPageOrder is true
                //TODO: markedAsDeleted shouldn't be ignored if update behaviour
                continue;
            }

            // Get the target reference and keep the top page reference for setting its metadata later
            DocumentReference publishedReference = getPublishedReference(page, configuration);
            if (collectionReference.equals(pageReference)) {
                targetTopReference = publishedReference;
            }

            // Create the published document
            logger.info("Copying page [{}] to [{}].", contentPage.getDocumentReference(),
                publishedReference);
            XWikiDocument publishedDocument = xwiki.getDocument(publishedReference, xcontext);
            copyContentsToNewVersion(contentPage, publishedDocument, xcontext);

            logger.info("Transforming content for publication.");
            prepareForPublication(contentPage, publishedDocument, collection, configuration);

            logger.debug("[publish] Publish page.");
            xwiki.saveDocument(publishedDocument, publicationComment, xcontext);
            logger.debug("[publish] End working on page [{}].", pageStringReference);
            progressManager.endStep(this);
        }

        // Add metadata in the collection page (master) and top page (published space)
        logger.debug("[publish] Adding metadata on master and published space top pages.");
        addMasterPublicationData(collection, configuration);
        addTopPublicationData(targetTopReference, publicationComment, collection, configuration);

        logger.debug("[publish] Publication ended.");
        logger.info("Publication finished");
        progressManager.popLevelProgress(this);
    }

    // Adapted from org.xwiki.workflowpublication.internal.DefaultPublicationWorkflow (Publication Workflow Application)
    protected boolean copyContentsToNewVersion(XWikiDocument fromDocument, XWikiDocument toDocument, XWikiContext xcontext)
        throws XWikiException
    {
        // use a fake 3 way merge: previous is toDocument without comments, rights and wf object
        // current version is current toDocument
        // next version is fromDocument without comments, rights and wf object
        XWikiDocument previousDoc = toDocument.clone();
        this.removeObjectsForPublication(previousDoc);
        // set reference and language

        // make sure that the attachments are properly loaded in memory for the duplicate to work fine, otherwise it's a
        // bit impredictable about attachments
        fromDocument.loadAttachments(xcontext);
        XWikiDocument nextDoc = fromDocument.duplicate(toDocument.getDocumentReference());
        this.removeObjectsForPublication(nextDoc);

        // and now merge. Normally the attachments which are not in the next doc are deleted from the current doc
        MergeResult result = toDocument.merge(previousDoc, nextDoc, new MergeConfiguration(), xcontext);

        // for some reason the creator doesn't seem to be copied if the toDocument is new, so let's put it
        if (toDocument.isNew()) {
            toDocument.setCreatorReference(fromDocument.getCreatorReference());
        }
        // Author does not seem to be merged anymore in the merge function in newer versions, so we'll do it here
        toDocument.setAuthorReference(fromDocument.getAuthorReference());

        List<LogEvent> exception = result.getLog().getLogs(LogLevel.ERROR);
        if (exception.isEmpty()) {
            return true;
        } else {
            StringBuffer exceptions = new StringBuffer();
            for (LogEvent e : exception) {
                if (exceptions.length() == 0) {
                    exceptions.append(";");
                }
                exceptions.append(e.getMessage());
            }
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Could not copy document contents from "
                    + localSerializer.serialize(fromDocument.getDocumentReference()) + " to document "
                    + localSerializer.serialize(toDocument.getDocumentReference()) + ". Caused by: "
                    + exceptions.toString());
        }
    }

    private void addTopPublicationData (DocumentReference targetTopReference,
        String publicationComment, XWikiDocument collection,
        Map<String, Object> configuration) throws XWikiException
    {
        if (targetTopReference != null) {
            logger.debug("[addTopPublicationData] Adding metadata to [{}]", targetTopReference);
            XWikiContext xcontext = this.getXWikiContext();
            XWiki xwiki = xcontext.getWiki();
            DocumentReference versionReference = (DocumentReference) configuration.get(
                BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_VERSION);
            XWikiDocument version = xwiki.getDocument(versionReference, xcontext);
            DocumentReference variantReference = (DocumentReference) configuration.get(
                BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_VARIANT);

            // Set metadata
            XWikiDocument targetTop = xwiki.getDocument(targetTopReference, xcontext).clone();
            BaseObject publicationObject = targetTop.newXObject(
                BookVersionsConstants.PUBLISHEDCOLLECTION_CLASS_REFERENCE, xcontext);
            publicationObject.set(BookVersionsConstants.PUBLISHEDCOLLECTION_PROP_MASTERNAME, collection.getTitle(),
                xcontext);
            publicationObject.set(BookVersionsConstants.PUBLISHEDCOLLECTION_PROP_VERSIONNAME, version.getTitle(),
                xcontext);
            if (variantReference != null) {
                XWikiDocument variant = xwiki.getDocument(variantReference, xcontext);
                publicationObject.set(BookVersionsConstants.PUBLISHEDCOLLECTION_PROP_VARIANTNAME, variant.getTitle(),
                    xcontext);
            }
            xwiki.saveDocument(targetTop, publicationComment, xcontext);

        }
    }

    private void addMasterPublicationData (XWikiDocument collection, Map<String, Object> configuration)
        throws XWikiException
    {
        logger.debug("[addMasterPublicationData] Adding metadata to [{}]", collection.getDocumentReference());
        XWikiContext xcontext = this.getXWikiContext();
        XWiki xwiki = xcontext.getWiki();
        DocumentReference sourceReference = (DocumentReference) configuration.get(
                BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_SOURCE);
        DocumentReference destinationReference = (DocumentReference) configuration.get(
            BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_DESTINATIONSPACE);
        DocumentReference versionReference = (DocumentReference) configuration.get(
            BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_VERSION);
        XWikiDocument version = xwiki.getDocument(versionReference,xcontext);
        DocumentReference variantReference = (DocumentReference) configuration.get(
            BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_VARIANT);
        XWikiDocument collectionClone = collection.clone();

        String publicationId;
        String publicationComment;
        if ( variantReference == null ) {
            publicationId = versionReference.getName();
            publicationComment = "Publication of space [" + sourceReference + "], version [" + version.getTitle()
                + "].";
        } else {
            XWikiDocument variant = xwiki.getDocument(variantReference,xcontext);
            publicationId = versionReference.getName() + "-" + variantReference.getName();
            publicationComment = "Publication of space [" + sourceReference + "], version [" + version.getTitle() +
                "], variant [" + variant.getTitle() + "].";
        }

        // Check existing publication objects
        logger.debug("[addMasterPublicationData] Checking if an object already exists for ID [{}] and source [{}].",
            publicationId, sourceReference);
        BaseObject publicationObject = null;
        for (BaseObject XObject : collectionClone.getXObjects(BookVersionsConstants.PUBLICATION_CLASS_REFERENCE)) {
            String objectId = XObject.getStringValue(BookVersionsConstants.PUBLICATION_PROP_ID);
            String objectSource = XObject.getStringValue(BookVersionsConstants.PUBLICATION_PROP_SOURCE);
            if (publicationId.equals(objectId) && sourceReference.toString().equals(objectSource)) {
                // Previous publication of the same space, with same version and variant found
                logger.debug("[addMasterPublicationData] An object already exists.");
                publicationObject = XObject;
                break;
            }
        }
        if (publicationObject == null) {
            // No previous publication found, create a new one
            logger.debug("[addMasterPublicationData] A new object is added.");
            publicationObject = collectionClone.newXObject(BookVersionsConstants.PUBLICATION_CLASS_REFERENCE,
                 xcontext);
        }

        // Add metadata
        publicationObject.set(BookVersionsConstants.PUBLICATION_PROP_ID, publicationId, xcontext);
        publicationObject.set(BookVersionsConstants.PUBLICATION_PROP_SOURCE, sourceReference.toString(), xcontext);
        publicationObject.set(BookVersionsConstants.PUBLICATION_PROP_PUBLISHEDSPACE, destinationReference.toString(),
            xcontext);
        xwiki.saveDocument(collectionClone, publicationComment, xcontext);

    }

    private DocumentReference getPublishedReference(XWikiDocument originalPage, Map<String, Object> configuration)
        throws QueryException, XWikiException
    {
        DocumentReference targetReference = (DocumentReference) configuration.get(
            BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_DESTINATIONSPACE);
        DocumentReference originalReference = originalPage.getDocumentReference();
        DocumentReference collectionReference = getVersionedCollectionReference(originalReference);
        // the first getParent() gets the collection's space, the second the space above
        DocumentReference publishedReference =
            originalReference.replaceParent(collectionReference.getParent().getParent(),
            targetReference.getLastSpaceReference());
        return  publishedReference;
    }

    private XWikiDocument removeObjectsForPublication(XWikiDocument publishedPage)
    {
        for (EntityReference objectRef : BookVersionsConstants.PUBLICATION_REMOVEDOBJECTS) {
            publishedPage.removeXObjects(objectRef);
        }
        return publishedPage;
    }

    private XWikiDocument prepareForPublication(XWikiDocument originalDocument, XWikiDocument publishedDocument,
        XWikiDocument collection, Map<String, Object> configuration)
        throws XWikiException, ComponentLookupException, ParseException
    {
        // Execute here all transformations on the document: change links, point to published library,
        logger.debug("[prepareForPublication] Apply changes on [{}] for publication.",
            publishedDocument.getDocumentReference());
        // Work directly on the document
        publishedDocument.setTitle(originalDocument.getTitle());
        publishedDocument.setHidden(false);
        // Work on the XDOM
        XDOM xdom = publishedDocument.getXDOM();
        String syntax = publishedDocument.getSyntax().toIdString();
        transformXDOM(xdom, syntax, originalDocument, publishedDocument, collection, configuration);
        // Set the modified XDOM
        publishedDocument.setContent(xdom);
        return publishedDocument;
    }

    // Heavily inspired from "Bulk update links according to a TSV mapping using XDOM" on https://snippets.xwiki.org
    private boolean transformXDOM(XDOM xdom, String syntaxId, XWikiDocument originalDocument,
        XWikiDocument publishedDocument, XWikiDocument collection, Map<String, Object>  configuration)
        throws ComponentLookupException, ParseException
    {
        boolean hasXDOMChanged = false;
        DocumentReference publishedVariantReference = (DocumentReference) configuration.get(
            BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_VARIANT);

        // First, update any document macro that could contain nested content (variant macro)

        for (Block b : xdom.getBlocks(new ClassBlockMatcher(MacroBlock.class), Block.Axes.DESCENDANT_OR_SELF)) {
            MacroBlock block = (MacroBlock) b;
            String id = block.getId();
            String content = block.getContent();
            logger.debug("[transformXDOM] Checking macro [{}] - [{}]", id, block.getClass());
            ComponentManager componentManager = contextrootComponentManagerProvider.get();
            if (!componentManager.hasComponent(Macro.class, id)) {
                continue;
            }

            // Check if the macro content is wiki syntax, in which case we'll also verify the contents of the macro
            Macro<?> macro = componentManager.getInstance(Macro.class, id);
            ContentDescriptor contentDescriptor = macro.getDescriptor().getContentDescriptor();

            // Get the possible variants from the variant macro
            String variants = block.getParameter(BookVersionsConstants.VARIANT_MACRO_PROP_NAME);
            List<DocumentReference> variantReferences = new ArrayList<>();
            if (variants != null) {
                for (String variant : variants.split(",")) {
                    variantReferences.add(referenceResolver.resolve(variant));
                }
            }

            if (id.equals(BookVersionsConstants.VARIANT_MACRO_ID) && publishedVariantReference != null
                && !variantReferences.contains(publishedVariantReference)
            ) {
                logger.debug("[transformXDOM] Variant macro is for [{}], it is removed from content ",
                    variantReferences);
                block.getParent().removeBlock(block);
            } else if (contentDescriptor != null && contentDescriptor.getType().equals(Block.LIST_BLOCK_TYPE)
                && StringUtils.isNotEmpty(content)
            ) {
                // We will take a quick shortcut here and directly parse the macro content with the syntax of the
                // document
                logger.debug("[transformXDOM] Calling parse on [{}] with syntax [{}]", id, syntaxId);
                Parser parser = componentManagerProvider.get().getInstance(Parser.class, syntaxId);
                XDOM contentXDOM = parser.parse(new StringReader(content));
                boolean hasMacroContentChanged = transformXDOM(contentXDOM, syntaxId, originalDocument,
                    publishedDocument, collection, configuration);
                if (hasMacroContentChanged) {
                    logger.debug("[transformXDOM] The content of macro [{}] has changed", id);
                    WikiPrinter printer = new DefaultWikiPrinter();
                    BlockRenderer renderer =
                        this.componentManagerProvider.get().getInstance(BlockRenderer.class, syntaxId);
                    renderer.render(block, printer);
                    String newMacroContent = printer.toString();
                    // Create a new macro block and swap it
                    MacroBlock newMacroBlock =
                        new MacroBlock(id, block.getParameters(), newMacroContent, block.isInline());
                    block.getParent().replaceChild(newMacroBlock, block);
                    hasXDOMChanged = true;
                }
                if (id.equals(BookVersionsConstants.VARIANT_MACRO_ID)) {
                    logger.debug("[transformXDOM] Variant macro is for [{}], it is replaced by its content ",
                        variantReferences);
                    block.getParent().replaceChild(contentXDOM, block);
                    hasXDOMChanged = true;
                }
            }
        }

        //TODO: add here other transformations (links, includeLibrary macro, ...)
        boolean transformedLibrary = transformLibrary(xdom, collection, configuration);
        return hasXDOMChanged || transformedLibrary;
    }

    private boolean transformLibrary(XDOM xdom, XWikiDocument collection, Map<String, Object>  configuration)
    {
        logger.debug("[transformLibrary] Starting to transform includeLibrary macro reference");
        boolean hasChanged = false;
        List<MacroBlock> listBlock = xdom.getBlocks(
            new ClassBlockMatcher(
                new MacroBlock(BookVersionsConstants.INCLUDELIBRARY_MACRO_ID, Collections.emptyMap(), true).getClass()
            ), Block.Axes.DESCENDANT_OR_SELF);
        logger.debug("[transformLibrary] [{}] '{}' macros found in the passed XDOM", listBlock.size(),
            BookVersionsConstants.INCLUDELIBRARY_MACRO_ID);

        for (MacroBlock macroBlock : listBlock) {
            //TODO: finish library transform
            /*
            logger.debug
            DocumentReference library = getLibrary;
            DocumentReference libraryVersion = getVersion(collectionReference, publishedVersion);
            DocumentReference publishedLibrary = getPublishedCollection(library, libraryVersion);
            replaceIncludeLibraryWithInclude(publishedLibrary);
            block.getParent().replace
             */
        }
        return hasChanged;
    }

    private DocumentReference getContentPage(XWikiDocument page, Map<String, Object> configuration)
        throws QueryException, XWikiException
    {
        boolean unversioned = (page.getXObject(BookVersionsConstants.BOOKPAGE_CLASS_REFERENCE)
            .getIntValue(BookVersionsConstants.BOOKPAGE_PROP_UNVERSIONED) == 1);
        if (unversioned) {
            return page.getDocumentReference();
        } else {
            return getInheritedContentReference(page.getDocumentReference(), (DocumentReference) configuration.get(
                BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_VERSION));
        }
    }

    private boolean isToBePublished(XWikiDocument page, Map<String, Object> configuration)
    {
        List<DocumentReference> variants = getPageVariants(page);
        String status = getPageStatus(page);
        boolean publishOnlyComplete =
            (boolean) configuration.get(BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_PUBLISHONLYCOMPLETE);
        DocumentReference variantReference = (DocumentReference) configuration.get(
            BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_VARIANT);
        if (isMarkedDeleted(page)) {
            logger.debug("[isToBePublished] Page is ignored because it is marked as deleted.");
            logger.info("Page is ignored because it is marked as deleted.");
            return false;
        } else if (publishOnlyComplete &&
            !status.equals(BookVersionsConstants.BOOKVERSIONEDCONTENT_PROP_STATUS_COMPLETE)) {
            logger.debug("[isToBePublished] Page is ignored because its status is [{}] and only complete page are "
                + "published.", status);
            logger.info("Page is ignored because its status is [{}] and only complete page are published.", status);
            return false;
        } else if (!variants.isEmpty() && !variants.contains(variantReference)) {
            logger.debug("[isToBePublished] Page is ignored because it is associated with other variants.");
            logger.info("Page is ignored because it is associated with other variants.");
            return false;
        }
        return true;
    }

    @Override
    public String getPageStatus(XWikiDocument page)
    {
        if (page == null) {
            return "";
        }
        BaseObject statusObj = page.getXObject(BookVersionsConstants.BOOKVERSIONEDCONTENT_CLASS_REFERENCE);
        if (statusObj == null) {
            return "";
        }
        return statusObj.getStringValue(BookVersionsConstants.BOOKVERSIONEDCONTENT_PROP_STATUS);
    }

    private List<String> getPageReferenceTree(DocumentReference sourceReference) throws QueryException
    {
        // Can be refactored with queryPages
        if (sourceReference != null) {
            SpaceReference spaceReference = sourceReference.getLastSpaceReference();
            String spaceSerialized = localSerializer.serialize(spaceReference);
            String spacePrefix = spaceSerialized.replaceAll("([%_/])", "/$1").concat(".%");

            logger.debug("[getPageReferenceTree] spaceSerialized : [{}]", spaceSerialized);
            logger.debug("[getPageReferenceTree] spacePrefix : [{}]", spacePrefix);

            // Query inspired from getDocumentReferences of DefaultModelBridge.java in xwiki-platform
            List<String> result = this.queryManagerProvider.get()
                .createQuery(", BaseObject as obj where doc.fullName = obj.name and obj.className = :class "
                    + "and doc.space like :space escape '/' order by doc.fullName asc", Query.HQL)
                .bindValue("class", localSerializer.serialize(BookVersionsConstants.BOOKPAGE_CLASS_REFERENCE))
                .bindValue("space", spacePrefix)
                .setWiki(sourceReference.getWikiReference().getName()).execute();
            // add the source as first element, as it's given by the query
            result.add(0, sourceReference.toString());

            logger.debug("[getPageReferenceTree] result : [{}]", result);

            return result;
        }
        return Collections.emptyList();
    }

    @Override
    public Map<String, Map<String, Object>> getLanguageData(XWikiDocument document)
    {
        Map<String, Map<String, Object>> languageData = new HashMap<String, Map<String, Object>>();
        XDOM xdom = document.getXDOM();

        List<MacroBlock> macros = xdom.getBlocks(MACRO_MATCHER, Block.Axes.DESCENDANT_OR_SELF);
        for (MacroBlock macroBlock : macros) {
            if (macroBlock.getId().equals(BookVersionsConstants.CONTENTTRANSLATION_MACRO_ID)) {
                String language = macroBlock.getParameter(BookVersionsConstants.PAGETRANSLATION_LANGUAGE);

                if (language != null && !language.isEmpty()) {

                    // Title
                    String title = macroBlock.getParameter(BookVersionsConstants.PAGETRANSLATION_TITLE);

                    // Status
                    String statusParameterValue = macroBlock.getParameter(BookVersionsConstants.PAGETRANSLATION_STATUS);
                    PageTranslationStatus status = PageTranslationStatus.NOT_TRANSLATED;
                    if (statusParameterValue != null && !statusParameterValue.isEmpty()
                        && statusParameterValue.equals("TRANSLATED")) {
                        status = PageTranslationStatus.TRANSLATED;
                    }
                    if (statusParameterValue != null && !statusParameterValue.isEmpty()
                        && statusParameterValue.equals("OUTDATED")) {
                        status = PageTranslationStatus.OUTDATED;
                    }

                    // Default language
                    String isDefault = macroBlock.getParameter(BookVersionsConstants.PAGETRANSLATION_ISDEFAULT);

                    Map<String, Object> currentLanguageData = new HashMap<String, Object>();
                    currentLanguageData.put(BookVersionsConstants.PAGETRANSLATION_TITLE,
                        title != null && !title.isEmpty() ? title : "");
                    currentLanguageData.put(BookVersionsConstants.PAGETRANSLATION_STATUS,
                        status != null ? status : PageTranslationStatus.NOT_TRANSLATED);
                    currentLanguageData.put(BookVersionsConstants.PAGETRANSLATION_ISDEFAULT,
                        isDefault != null && !isDefault.isEmpty() ? Boolean.valueOf(isDefault) : false);

                    languageData.put(language, currentLanguageData);
                }
            }
        }

        return languageData;
    }

    @Override
    public void setLanguageData(XWikiDocument document, Map<String, Map<String, Object>> languageData)
    {
        for (Entry<String, Map<String, Object>> languageDataEntry : languageData.entrySet()) {
            String language = languageDataEntry.getKey();

            BaseObject translationObject = null;

            for (BaseObject tObj : document.getXObjects(BookVersionsConstants.PAGETRANSLATION_CLASS_REFERENCE)) {
                String languageEntry = tObj.getStringValue(BookVersionsConstants.PAGETRANSLATION_LANGUAGE);
                if (languageEntry != null && !languageEntry.isEmpty() && languageEntry.equals(language)) {
                    translationObject = tObj;
                    break;
                }
            }

            if (translationObject == null) {
                try {
                    translationObject =
                        document.newXObject(BookVersionsConstants.PAGETRANSLATION_CLASS_REFERENCE, getXWikiContext());
                    translationObject.setStringValue(BookVersionsConstants.PAGETRANSLATION_LANGUAGE, language);
                } catch (XWikiException e) {
                    logger.error("Could not set the translation data in document [{}]", document.getDocumentReference(),
                        e);
                    return;
                }
            }

            String title = (String) languageDataEntry.getValue().get(BookVersionsConstants.PAGETRANSLATION_TITLE);
            translationObject.setStringValue(BookVersionsConstants.PAGETRANSLATION_TITLE, title != null ? title : "");

            PageTranslationStatus status =
                (PageTranslationStatus) languageDataEntry.getValue().get(BookVersionsConstants.PAGETRANSLATION_STATUS);
            translationObject.setStringValue(BookVersionsConstants.PAGETRANSLATION_STATUS,
                status != null ? status.getTranslationStatus() : null);

            boolean isDefault =
                (boolean) languageDataEntry.getValue().get(BookVersionsConstants.PAGETRANSLATION_ISDEFAULT);
            translationObject.setIntValue(BookVersionsConstants.PAGETRANSLATION_ISDEFAULT, isDefault ? 1 : 0);
        }

        // Now, remove translations that were deleted by the user or don't have a language defined
        for (BaseObject tObj : document.getXObjects(BookVersionsConstants.PAGETRANSLATION_CLASS_REFERENCE)) {
            String languageEntry = tObj.getStringValue(BookVersionsConstants.PAGETRANSLATION_LANGUAGE);
            if (languageEntry == null || languageEntry.isEmpty() || !languageData.containsKey(languageEntry)) {
                document.removeXObject(tObj);
            }
        }
    }

    @Override
    public void resetTranslations(XWikiDocument document)
    {
        for (BaseObject tObj : document.getXObjects(BookVersionsConstants.PAGETRANSLATION_CLASS_REFERENCE)) {
            document.removeXObject(tObj);
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
