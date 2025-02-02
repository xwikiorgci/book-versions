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

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * Book versions constants.
 * 
 * @version $Id$
 * @since 0.1
 */
public interface BookVersionsConstants
{
    /**
     * The reference of the code location.
     */
    EntityReference BOOKVERSIONS_CODE_REFERENCE = new EntityReference("Code", EntityType.SPACE,
        new EntityReference("BookVersions", org.xwiki.model.EntityType.SPACE));

    /**
     * The reference of the Book class.
     */
    EntityReference BOOK_CLASS_REFERENCE =
        new EntityReference("BookClass", EntityType.DOCUMENT, BOOKVERSIONS_CODE_REFERENCE);

    /**
     * The reference of the library class.
     */
    EntityReference LIBRARY_CLASS_REFERENCE =
        new EntityReference("LibraryClass", EntityType.DOCUMENT, BOOKVERSIONS_CODE_REFERENCE);

    /**
     * The reference of the version class.
     */
    EntityReference VERSION_CLASS_REFERENCE =
        new EntityReference("VersionClass", EntityType.DOCUMENT, BOOKVERSIONS_CODE_REFERENCE);

    /**
     * The reference of the variant class.
     */
    EntityReference VARIANT_CLASS_REFERENCE =
        new EntityReference("VariantClass", EntityType.DOCUMENT, BOOKVERSIONS_CODE_REFERENCE);

    /**
     * The reference of the book page class.
     */
    EntityReference BOOKPAGE_CLASS_REFERENCE =
        new EntityReference("BookPageClass", EntityType.DOCUMENT, BOOKVERSIONS_CODE_REFERENCE);

    /**
     * The reference of the book versioned content class.
     */
    EntityReference BOOKVERSIONEDCONTENT_CLASS_REFERENCE =
        new EntityReference("BookVersionedContentClass", EntityType.DOCUMENT, BOOKVERSIONS_CODE_REFERENCE);

    /**
     * The reference of the library reference class.
     */
    EntityReference BOOKLIBRARYREFERENCE_CLASS_REFERENCE =
        new EntityReference("LibraryReferenceClass", EntityType.DOCUMENT, BOOKVERSIONS_CODE_REFERENCE);

    /**
     * The unversioned property of a book page.
     */
    String BOOKPAGE_PROP_UNVERSIONED = "unversioned";

    /**
     * The status property of a book page.
     */
    String BOOKVERSIONEDCONTENT_PROP_STATUS = "status";

    /**
     * The status property of a book page.
     */
    String BOOKVERSIONEDCONTENT_PROP_STATUS_DRAFT = "draft";

    /**
     * The status property of a book page.
     */
    String BOOKVERSIONEDCONTENT_PROP_STATUS_REVIEW = "review";

    /**
     * The status property of a book page.
     */
    String BOOKVERSIONEDCONTENT_PROP_STATUS_COMPLETE = "complete";

    /**
     * The slug validator hint.
     */
    String SLUGVALIDATION_HINT = "SlugEntityNameValidation";

    /**
     * The selected version that is stored in the session.
     */
    String SESSION_SELECTEDVERSION = "BookVersions.selectedVersion";

    /**
     * The selected variant that is stored in the session.
     */
    String SESSION_SELECTEDVARIANT = "BookVersions.selectedVariant";

    /**
     * The selected version that is stored in the session.
     */
    String VERSION_PROP_PRECEDINGVERSION = "precedingVersionReference";

    /**
     * The library reference of a book library reference.
     */
    String BOOKLIBRARYREFERENCE_PROP_LIBRARY = "libraryReference";

    /**
     * The library version reference of a book library reference.
     */
    String BOOKLIBRARYREFERENCE_PROP_LIBRARYVERSION = "libraryVersionReference";

    /**
     * The location of version definitions, in a book / library.
     */
    String VERSIONS_LOCATION = "Versions";

    /**
     * The location of variant definitions, in a book / library.
     */
    String VARIANTS_LOCATION = "Variants";
}
