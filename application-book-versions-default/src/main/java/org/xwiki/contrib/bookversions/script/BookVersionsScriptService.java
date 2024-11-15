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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiException;

import org.xwiki.contrib.bookversions.BookVersionsManager;
import org.xwiki.model.reference.DocumentReference;

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
}
