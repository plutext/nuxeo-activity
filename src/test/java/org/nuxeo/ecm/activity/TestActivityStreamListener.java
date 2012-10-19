/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.activity;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@RunWith(FeaturesRunner.class)
@Features(ActivityFeature.class)
@Ignore
public class TestActivityStreamListener {

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Inject
    protected ActivityStreamService activityStreamService;

    @Test
    public void shouldAddNewActivitiesThroughListener() throws ClientException {
        DocumentModel doc1 = session.createDocumentModel("/", "firstDocument",
                "File");
        doc1 = session.createDocument(doc1);
        commitAndWaitForAsyncCompletion();

        DocumentModel doc2 = session.createDocumentModel("/", "secondDocument",
                "File");
        doc2 = session.createDocument(doc2);
        commitAndWaitForAsyncCompletion();

        doc1.setPropertyValue("dc:title", "A new Title");
        session.saveDocument(doc1);
        commitAndWaitForAsyncCompletion();

        List<Activity> activities = activityStreamService.query(
                ActivityStreamService.ALL_ACTIVITIES, null);
        assertNotNull(activities);
        assertEquals(3, activities.size());

        String currentUser = ActivityHelper.createUserActivityObject(session.getPrincipal());
        Activity storedActivity = activities.get(0);
        assertEquals(1L, storedActivity.getId());
        assertEquals(currentUser, storedActivity.getActor());
        assertEquals(DOCUMENT_CREATED, storedActivity.getVerb());
        assertEquals(ActivityHelper.createDocumentActivityObject(doc1),
                storedActivity.getObject());
        assertEquals("firstDocument", storedActivity.getDisplayObject());
        assertEquals(
                ActivityHelper.createDocumentActivityObject(session.getRootDocument()),
                storedActivity.getTarget());

        storedActivity = activities.get(1);
        assertEquals(2L, storedActivity.getId());
        assertEquals(currentUser, storedActivity.getActor());
        assertEquals(DOCUMENT_CREATED, storedActivity.getVerb());
        assertEquals(ActivityHelper.createDocumentActivityObject(doc2),
                storedActivity.getObject());
        assertEquals("secondDocument", storedActivity.getDisplayObject());
        assertEquals(
                ActivityHelper.createDocumentActivityObject(session.getRootDocument()),
                storedActivity.getTarget());

        storedActivity = activities.get(2);
        assertEquals(3L, storedActivity.getId());
        assertEquals(currentUser, storedActivity.getActor());
        assertEquals(DocumentEventTypes.DOCUMENT_UPDATED,
                storedActivity.getVerb());
        assertEquals(ActivityHelper.createDocumentActivityObject(doc1),
                storedActivity.getObject());
        assertEquals("A new Title", storedActivity.getDisplayObject());
        assertEquals(
                ActivityHelper.createDocumentActivityObject(session.getRootDocument()),
                storedActivity.getTarget());
    }

    private void commitAndWaitForAsyncCompletion() {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        eventService.waitForAsyncCompletion();
    }

}
