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

package org.xwiki.contrib.bookversions.listeners;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.bookversions.BookVersionsManager;
import org.xwiki.contrib.bookversions.internal.BookVersionsConstants;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Updating the language data in book pages.
 * 
 * @version $Id$
 * @since 0.1
 */
@Component
@Named(DocumentUpdatingEventListener.NAME)
@Singleton
public class DocumentUpdatingEventListener extends AbstractLocalEventListener
{

    static final String NAME = "org.xwiki.contrib.bookversions.listeners.DocumentUpdatingEventListener";

    private static final List<Event> EVENT_LIST = List.of(new DocumentUpdatingEvent());

    @Inject
    private Provider<BookVersionsManager> bookVersionsManagerProvider;

    @Inject
    private Logger logger;

    /**
     * Constructor.
     */
    public DocumentUpdatingEventListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        XWikiDocument updatedXDoc = (XWikiDocument) source;
        BookVersionsManager bookVersionsManager = bookVersionsManagerProvider.get();

        try {
            if (bookVersionsManager.isPage(updatedXDoc) || bookVersionsManager.isVersionedContent(updatedXDoc)) {
                Map<String, Map<String, Object>> lanugageData = bookVersionsManager.getLanguageData(updatedXDoc);
                if (lanugageData != null && !lanugageData.isEmpty()) {
                    bookVersionsManager.setLanguageData(updatedXDoc, lanugageData);
                    updatedXDoc.setTitle(BookVersionsConstants.DEFAULT_TRANSLATION_TITLE);
                } else {
                    bookVersionsManager.resetTranslations(updatedXDoc);
                }
            }
        } catch (XWikiException e) {
            logger.error("Could not handle the event listener.", e);
        }
    }
}
