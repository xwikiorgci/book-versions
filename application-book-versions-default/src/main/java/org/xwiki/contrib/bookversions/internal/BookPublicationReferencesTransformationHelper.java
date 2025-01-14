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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;

/**
 * This component focuses on transforming references between Book Version pages at publication time.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = BookPublicationReferencesTransformationHelper.class)
@Singleton
public class BookPublicationReferencesTransformationHelper
{
    // Note that we don't support interwiki links, as books cannot span over multiple wikis
    // TODO: Handle space references
    private static final List<ResourceType> SUPPORTED_DOCUMENT_RESOURCES = Arrays.asList(ResourceType.DOCUMENT,
        ResourceType.PAGE);

    private static final List<ResourceType> SUPPORTED_ATTACHMENT_RESOURCES = Arrays.asList(ResourceType.ATTACHMENT,
        ResourceType.PAGE_ATTACHMENT);

    @Inject
    private Logger logger;

    @Inject
    @Named("current")
    private EntityReferenceResolver<String> currentEntityReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    public boolean transform(XDOM xdom, DocumentReference originalReference,
        Map<String, Object> publicationConfiguration)
    {
        // Extract information about the master spaces and the publication space
        SpaceReference publishedSpaceReference =
            ((DocumentReference) publicationConfiguration.get(
                BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_DESTINATIONSPACE)).getLastSpaceReference();
        // Update the published space reference to also take into account that the top space will be also included in
        // there.

        // TODO: Add support for getting libraries
        List<SpaceReference> masterSpaceReferences = Arrays.asList(
            ((DocumentReference) publicationConfiguration.get(
                BookVersionsConstants.PUBLICATIONCONFIGURATION_PROP_SOURCE)).getLastSpaceReference()
        );

        return transform(xdom, originalReference, masterSpaceReferences, publishedSpaceReference);
    }

    public boolean transform(XDOM xdom, DocumentReference originalReference,
        List<SpaceReference> masterSpaceReferences, SpaceReference publishedSpaceReference)
    {
        boolean hasXDOMChanged = false;

        // Handle the transformation of links
        for (Block block : xdom.getBlocks(new ClassBlockMatcher(LinkBlock.class), Block.Axes.DESCENDANT_OR_SELF)) {
            LinkBlock linkBlock = (LinkBlock) block;
            ResourceType resourceType = linkBlock.getReference().getType();
            if (SUPPORTED_DOCUMENT_RESOURCES.contains(resourceType)) {
                ResourceReference equivalentResourceReference = getEquivalentDocumentResourceReference(
                    linkBlock.getReference(), originalReference, masterSpaceReferences, publishedSpaceReference);

                if (equivalentResourceReference != null) {
                    LinkBlock newLinkBlock = new LinkBlock(linkBlock.getChildren(), equivalentResourceReference,
                        linkBlock.isFreeStandingURI());
                    linkBlock.getParent().replaceChild(newLinkBlock, linkBlock);
                    hasXDOMChanged = true;
                }
            } else if (SUPPORTED_ATTACHMENT_RESOURCES.contains(resourceType)) {
                ResourceReference equivalentResourceReference = getEquivalentAttachmentResourceReference(
                    linkBlock.getReference(), originalReference, masterSpaceReferences, publishedSpaceReference);

                if (equivalentResourceReference != null) {
                    LinkBlock newLinkBlock = new LinkBlock(linkBlock.getChildren(), equivalentResourceReference,
                        linkBlock.isFreeStandingURI());
                    linkBlock.getParent().replaceChild(newLinkBlock, linkBlock);
                    hasXDOMChanged = true;
                }
            }
        }

        // Handle the transformation of images
        for (Block block : xdom.getBlocks(new ClassBlockMatcher(ImageBlock.class), Block.Axes.DESCENDANT_OR_SELF)) {
            ImageBlock imageBlock = (ImageBlock) block;
            ResourceType resourceType = imageBlock.getReference().getType();

            if (SUPPORTED_ATTACHMENT_RESOURCES.contains(resourceType)) {
                ResourceReference equivalentResourceReference = getEquivalentAttachmentResourceReference(
                    imageBlock.getReference(), originalReference, masterSpaceReferences, publishedSpaceReference);

                if (equivalentResourceReference != null) {
                    ImageBlock newImageBlock = new ImageBlock(equivalentResourceReference,
                        imageBlock.isFreeStandingURI(), imageBlock.getParameters());
                    imageBlock.getParent().replaceChild(newImageBlock, imageBlock);
                    hasXDOMChanged = true;
                }
            }
        }

        // We assume that transformation of macro content is already handled through calls in #transformXDOM.
        // Here we only care about updating the macro parameters which are declared as document references
        /*for (Block block : xdom.getBlocks(new ClassBlockMatcher(MacroBlock.class), Block.Axes.DESCENDANT_OR_SELF)) {
            MacroBlock macroBlock = (MacroBlock) block;

            // Try to load the macro definition
            if (componentManagerProvider.get().hasComponent(Macro.class, macroBlock.getId())) {
                try {
                    Macro macro = componentManagerProvider.get().getInstance(Macro.class, macroBlock.getId());
                    Map<String, ParameterDescriptor> parameterDescriptors =
                        macro.getDescriptor().getParameterDescriptorMap();
                    for (Map.Entry<String, ParameterDescriptor> parameterDescriptorEntry :
                        parameterDescriptors.entrySet()) {
                        if (DocumentReference.class.equals(parameterDescriptorEntry.getValue().getParameterType()) {
                            // Try to transform the parameter
                            macroBlock.getParameter("")
                        }
                    }
                } catch (ComponentLookupException e) {
                    // Should never happen
                    logger.error("Failed to lookup macro definition for [{}]", macroBlock.getId(), e);
                }
            }
        }*/

        return hasXDOMChanged;
    }

    private ResourceReference getEquivalentDocumentResourceReference(ResourceReference resourceReference,
        DocumentReference originalReference, List<SpaceReference> masterSpaceReferences,
        SpaceReference publishedSpaceReference)
    {
        DocumentReference reference = convertDocumentResourceReference(resourceReference, originalReference);
        DocumentReference equivalentReference =
            getEquivalentReference(reference, masterSpaceReferences, publishedSpaceReference);
        if (!reference.equals(equivalentReference)) {
            // Update the link with the new reference
            return new ResourceReference(
                entityReferenceSerializer.serialize(equivalentReference), ResourceType.DOCUMENT);
        } else {
            return null;
        }
    }

    private ResourceReference getEquivalentAttachmentResourceReference(ResourceReference resourceReference,
        DocumentReference originalReference, List<SpaceReference> masterSpaceReferences,
        SpaceReference publishedSpaceReference)
    {
        AttachmentReference reference = convertAttachmentResourceReference(resourceReference, originalReference);
        DocumentReference equivalentReference = getEquivalentReference(reference.getDocumentReference(),
                masterSpaceReferences, publishedSpaceReference);

        if (!reference.getDocumentReference().equals(equivalentReference)) {
            // Update the link with the new reference
            AttachmentReference equivalentAttachmentReference =
                new AttachmentReference(reference.getName(), equivalentReference);
            return new ResourceReference(
                entityReferenceSerializer.serialize(equivalentAttachmentReference), ResourceType.ATTACHMENT);
        } else {
            return null;
        }
    }

    private DocumentReference convertDocumentResourceReference(ResourceReference reference,
        DocumentReference originalReference)
    {
        if (ResourceType.DOCUMENT.equals(reference.getType())) {
            return new DocumentReference(currentEntityReferenceResolver.resolve(reference.getReference(),
                EntityType.DOCUMENT, originalReference));
        } else if (reference.getType().equals(ResourceType.PAGE)) {
            // XXX
            // TODO
            return null;
        } else {
            logger.error("Unsupported resource type for converting to a document reference : [{}]",
                reference.getType());
            return null;
        }
    }

    private AttachmentReference convertAttachmentResourceReference(ResourceReference reference,
        DocumentReference originalReference)
    {
        if (ResourceType.ATTACHMENT.equals(reference.getType())) {
            return new AttachmentReference(currentEntityReferenceResolver.resolve(reference.getReference(),
                EntityType.ATTACHMENT, originalReference));
        } else if (reference.getType().equals(ResourceType.PAGE_ATTACHMENT)) {
            // XXX
            // TODO
            return null;
        } else {
            logger.error("Unsupported resource type for converting to an attachment reference : [{}]",
                reference.getType());
            return null;
        }
    }

    private DocumentReference getEquivalentReference(DocumentReference reference,
        List<SpaceReference> masterSpaceReferences, SpaceReference publishedSpaceReference)
    {
        SpaceReference foundSpaceReference = null;
        for (EntityReference entityReference : reference.getReversedReferenceChain()) {
            if (EntityType.SPACE.equals(entityReference.getType()) && masterSpaceReferences.contains(entityReference)) {
                foundSpaceReference = (SpaceReference) entityReference;
            }
        }

        if (foundSpaceReference != null) {
            // The page will then be located within the newly published space. We need to re-compute its reference
            // chain.
            return reference.replaceParent(foundSpaceReference, publishedSpaceReference);
        } else {
            // If no space reference is found, it means that we are in the case where the document reference actually
            // points to a page outside any book or library involved in this publication. We then don't need to
            // update it.
            return reference;
        }
    }
}
