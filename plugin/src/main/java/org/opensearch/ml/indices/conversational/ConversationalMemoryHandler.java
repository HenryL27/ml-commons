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
package org.opensearch.ml.indices.conversational;

import java.util.List;

import org.opensearch.action.ActionListener;

public interface ConversationalMemoryHandler {
    
    /**
     * Create a new conversation
     * @param listener listener to wait for this op to finish, gets unique id of new conversation
     */
    public void createConversation(ActionListener<String> listener);

    /**
     * Create a new conversation
     * @param name the name of the new conversation
     * @param listener listener to wait for this op to finish, gets unique id of new conversation
     */
    public void createConversation(String name, ActionListener<String> listener);

    /**
     * Adds an interaction to the conversation indicated, updating the conversational metadata
     * @param conversationId the conversation to add the interaction to
     * @param input the human input for the interaction
     * @param prompt the prompt template used in this interaction
     * @param response the Gen AI response for this interaction
     * @param agent the name of the GenAI agent in this interaction
     * @param metadata arbitrary JSON string of extra stuff
     * @param listener gets the ID of the new interaction
     */
    public void createInteraction(
        String conversationId, 
        String input,
        String prompt,
        String response,
        String agent,
        String metadata,
        ActionListener<String> listener
    );

    /**
     * Get the interactions associate with this conversation, sorted by recency
     * @param conversationId the conversation whose interactions to get
     * @param from where to start listiing from
     * @param maxResults how many interactions to get
     * @param listener gets the list of interactions in this conversation, sorted by recency
     */
    public void getInteractions(String conversationId, int from, int maxResults, ActionListener<List<Interaction>> listener);

    /**
     * Get all conversations (not the interactions in them, just the headers)
     * @param from where to start listing from
     * @param maxResults how many conversations to list
     * @param listener gets the list of all conversations, sorted by recency
     */
    public void getConversations(int from, int maxResults, ActionListener<List<ConvoMeta>> listener);

    /**
     * Get all conversations (not the interactions in them, just the headers)
     * @param maxResults how many conversations to get
     * @param listener receives the list of conversations, sorted by recency
     */
    public void getConversations(int maxResults, ActionListener<List<ConvoMeta>> listener);

    /**
     * Delete a conversation and all of its interactions
     * @param conversationId the id of the conversation to delete
     * @param listener receives whether the convoMeta object and all of its interactions were deleted. i.e. false => there's something still in an index somewhere
     */
    public void deleteConversation(String conversationId, ActionListener<Boolean> listener);
}
