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
    EntityReference BOOK_PAGE_CLASS_REFERENCE =
        new EntityReference("BookPageClass", EntityType.DOCUMENT, BOOKVERSIONS_CODE_REFERENCE);

    /**
     * The unversioned property of a book page.
     */
    String BOOK_PAGE_PROP_UNVERSIONED = "unversioned";

    /**
     * The slug validator hint.
     */
    String SLUGVALIDATION_HINT = "SlugEntityNameValidation";
}
