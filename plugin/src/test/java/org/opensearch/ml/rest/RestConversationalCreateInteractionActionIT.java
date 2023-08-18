/*
 * Copyright 2023 Aryn
 * Copyright OpenSearch Contributors
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
package org.opensearch.ml.rest;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.opensearch.client.Response;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.ml.common.conversational.ActionConstants;
import org.opensearch.ml.utils.TestHelper;

public class RestConversationalCreateInteractionActionIT extends MLCommonsRestTestCase {

    public void testCreateInteraction() throws IOException {
        Response ccresponse = TestHelper.makeRequest(client(), "POST", ActionConstants.CREATE_CONVERSATION_PATH, null, "", null);
        assert (ccresponse != null);
        assert (TestHelper.restStatus(ccresponse) == RestStatus.OK);
        HttpEntity cchttpEntity = ccresponse.getEntity();
        String ccentityString = TestHelper.httpEntityToString(cchttpEntity);
        Map ccmap = gson.fromJson(ccentityString, Map.class);
        assert (ccmap.containsKey("conversation_id"));
        String id = (String) ccmap.get("conversation_id");

        Map<String, String> params = Map.of(
            ActionConstants.INPUT_FIELD, "input", 
            ActionConstants.PROMPT_FIELD, "prompt", 
            ActionConstants.AI_RESPONSE_FIELD, "response", 
            ActionConstants.AI_AGENT_FIELD, "agent", 
            ActionConstants.INTER_ATTRIBUTES_FIELD, "attributes"
        );
        Response response = TestHelper.makeRequest(client(), "POST", "_plugins/_ml/conversational/memory/" + id, params, "", null);
        assert (response != null);
        assert (TestHelper.restStatus(response) == RestStatus.OK);
        HttpEntity httpEntity = response.getEntity();
        String entityString = TestHelper.httpEntityToString(httpEntity);
        Map map = gson.fromJson(entityString, Map.class);
        assert (map.containsKey("interaction_id"));
    }
}