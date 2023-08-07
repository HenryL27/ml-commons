/*
 * Copyright Aryn, Inc 2023
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensearch.conversational.index;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.opensearch.action.ActionListener;
import org.opensearch.action.LatchedActionListener;
import org.opensearch.action.StepListener;
import org.opensearch.client.Client;
import org.opensearch.client.Requests;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.test.OpenSearchIntegTestCase;

@OpenSearchIntegTestCase.ClusterScope(scope = OpenSearchIntegTestCase.Scope.TEST, numDataNodes = 2)
public class ConvoMetaIndexTests extends OpenSearchIntegTestCase {
    private final static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(ConvoMetaIndexTests.class);

    private ClusterService clusterService;
    private Client client;
    private ConvoMetaIndex index;

    private void refreshIndex() {
        client.admin().indices().refresh(Requests.refreshRequest(ConvoIndexConstants.META_INDEX_NAME));
    }

    @Before
    public void setup() {
        log.info("Setting up test");
        this.client = client();
        this.clusterService = clusterService();
        this.index = new ConvoMetaIndex(client, clusterService);
    }

    /**
     * Can the index be initialized?
     */
    public void testConvoMetaIndexCanBeInitialized() {
        CountDownLatch cdl = new CountDownLatch(1);
        index.initConvoMetaIndexIfAbsent(new LatchedActionListener<Boolean>( ActionListener.wrap(r->{
            assert(r);
        }, e -> {
            log.error(e);
            assert(false);
        }), cdl));
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    /**
     * If the index tries to be initialized more than once does it break something
     * Also make sure that only one initialization happens
     */
    public void testConvoMetaIndexCanBeInitializedTwice() {
        CountDownLatch cdl = new CountDownLatch(2);
        index.initConvoMetaIndexIfAbsent(new LatchedActionListener<Boolean>(ActionListener.wrap(r->{
            assert(r);
        }, e -> {
            log.error(e);
            assert(false);
        }), cdl));
        index.initConvoMetaIndexIfAbsent(new LatchedActionListener<Boolean>(ActionListener.wrap(r->{
            assert(r);
        }, e -> {
            log.error(e);
            assert(false);
        }), cdl));
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    /**
     * If the index tries to be initialized by different objects does it break anything
     * Also make sure that only one initialization happens
     */
    public void testConvoMetaIndexCanBeInitializedByDifferentObjects() {
        CountDownLatch cdl = new CountDownLatch(2);
        index.initConvoMetaIndexIfAbsent(new LatchedActionListener<Boolean>(ActionListener.wrap(r->{
            assert(r);
        }, e -> {
            log.error(e);
            assert(false);
        }), cdl));
        ConvoMetaIndex otherIndex = new ConvoMetaIndex(client, clusterService);
        otherIndex.initConvoMetaIndexIfAbsent(new LatchedActionListener<Boolean>(ActionListener.wrap(r->{
            assert(r);
        }, e -> {
            log.error(e);
            assert(false);
        }), cdl));
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    /**
     * Can I add a new conversation to the index without crashong?
     */
    public void testCanAddNewConversation() {
        CountDownLatch cdl = new CountDownLatch(1);
        index.createConversation(new LatchedActionListener<String>(ActionListener.wrap(r->{
            assert(r != null && r.length() > 0);
        }, e->{
            log.error(e);
            assert(false);
        }), cdl));
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    /**
     * Are conversation ids unique?
     */
    public void testConversationIDsAreUnique() {
        int numTries = 100;
        CountDownLatch cdl = new CountDownLatch(numTries);
        Set<String> seenIds = Collections.synchronizedSet(new HashSet<String>(numTries));
        for(int i = 0; i < numTries; i++){
            index.createConversation(new LatchedActionListener<String>(ActionListener.wrap(r-> {
                assert(!seenIds.contains(r));
                seenIds.add(r);
            }, e-> {
                log.error(e);
                assert(false);
            }), cdl));
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    /**
     * If I add a conversation, that id shows up in the list of conversations
     */
    public void testConversationsCanBeListed() {
        CountDownLatch cdl = new CountDownLatch(1);
        StepListener<String> addConvoListener = new StepListener<>();
        index.createConversation(addConvoListener);

        StepListener<List<ConvoMeta>> listConvoListener = new StepListener<>();
        addConvoListener.whenComplete(cid -> {
            refreshIndex();
            refreshIndex();
            index.getConversations(10, listConvoListener);
        }, e -> {
            cdl.countDown();
            log.error(e);
        });

        LatchedActionListener<List<ConvoMeta>> finishAndAssert = new LatchedActionListener<>(ActionListener.wrap(
            convos -> {
                boolean foundConvo = false;
                log.info("FINDME");
                log.info(addConvoListener.result());
                log.info(convos);
                for(ConvoMeta c: convos) {
                    if(c.getId().equals(addConvoListener.result())) {
                        foundConvo = true;
                    }
                }
                assert(foundConvo);
            }, e -> {
                log.error(e);
            }
        ), cdl);
        listConvoListener.whenComplete(finishAndAssert::onResponse, finishAndAssert::onFailure);
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    /**
     * When I touch a conversation, it should update the metadata
     */
    public void testConversationsCanGetHitStepped() {
        CountDownLatch cdl = new CountDownLatch(1);
        StepListener<String> addConvoListener = new StepListener<>();
        index.createConversation(addConvoListener);
        
        Instant pit = Instant.now().plus(423, ChronoUnit.MINUTES);

        StepListener<Boolean> hitConvoListener = new StepListener<>();
        addConvoListener.whenComplete(cid -> {
            refreshIndex();
            index.hitConversation(cid, pit, hitConvoListener);
        }, e -> {
            cdl.countDown();
            log.error(e);
        });

        StepListener<List<ConvoMeta>> listConvoListener = new StepListener<>();
        hitConvoListener.whenComplete(b -> {
            refreshIndex();
            index.getConversations(1, listConvoListener);
        }, e -> {
            cdl.countDown();
            log.error(e);
        });

        LatchedActionListener<List<ConvoMeta>> finishAndAssert = new LatchedActionListener<>(ActionListener.wrap(
            convos -> {
                ConvoMeta convo = convos.get(0);
                assert(convo.getId().equals(addConvoListener.result()));
                assert(convo.getLastHit().equals(pit));
                assert(convo.getLength() == 1);
            }, e -> {
                log.error(e);
            }
        ), cdl);
        listConvoListener.whenComplete(finishAndAssert::onResponse, finishAndAssert::onFailure);
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    public void testConversationsCanBeListedPaginated() {
        CountDownLatch cdl = new CountDownLatch(1);
        StepListener<String> addConvoListener1 = new StepListener<>();
        index.createConversation(addConvoListener1);

        StepListener<String> addConvoListener2 = new StepListener<>();
        addConvoListener1.whenComplete( cid -> {
            index.createConversation(addConvoListener2);
        }, e -> {cdl.countDown(); assert(false);});

        StepListener<List<ConvoMeta>> listConvoListener1 = new StepListener<>();
        addConvoListener2.whenComplete(cid2 -> {
            index.getConversations(1, listConvoListener1);
        }, e -> { cdl.countDown(); assert(false); });

        StepListener<List<ConvoMeta>> listConvoListener2 = new StepListener<>();
        listConvoListener1.whenComplete(convos1 -> {
            index.getConversations(1,1,listConvoListener2);
        }, e -> {
            cdl.countDown();
            assert(false);
        });

        LatchedActionListener<List<ConvoMeta>> finishAndAssert = new LatchedActionListener<>( ActionListener.wrap(
            convos2 -> {
                List<ConvoMeta> convos1 = listConvoListener1.result();
                String cid1 = addConvoListener1.result();
                String cid2 = addConvoListener2.result();
                assert(convos1.get(0).getId().equals(cid2));
                assert(convos2.get(0).getId().equals(cid1));
            }, e -> { 
                assert(false); 
            }
        ), cdl);
        listConvoListener2.whenComplete(finishAndAssert::onResponse, finishAndAssert::onFailure);
        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }

    }

    public void testConversationsCanBeDeleted() {
        CountDownLatch cdl = new CountDownLatch(1);
        StepListener<String> addConvoListener = new StepListener<>();
        index.createConversation(addConvoListener);

        StepListener<Boolean> deleteConvoListener = new StepListener<>();
        addConvoListener.whenComplete(cid -> {
            index.deleteConversation(cid, deleteConvoListener);
        }, e -> {
            cdl.countDown();
            assert(false);
        });

        LatchedActionListener<List<ConvoMeta>> finishAndAssert = new LatchedActionListener<>(ActionListener.wrap(
            conversations -> {
                assert(conversations.size() == 0);
            }, e -> {
                cdl.countDown();
                assert(false);
            }
        ), cdl);
        deleteConvoListener.whenComplete(success -> {
            if(success) {
                index.getConversations(10, finishAndAssert);
            } else {
                cdl.countDown();
                assert(false);
            }
        }, e -> {
            cdl.countDown();
            assert(false); 
        });

        try {
            cdl.await();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }


}