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
package org.opensearch.searchpipelines.questionanswering.generative.ext;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.XContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentGenerator;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GenerativeQAParametersTests extends OpenSearchTestCase {

    public void testGenerativeQAParameters() {
        GenerativeQAParameters params = new GenerativeQAParameters("conversation_id", "llm_model", "llm_question", 2);
        GenerativeQAParamExtBuilder extBuilder = new GenerativeQAParamExtBuilder();
        extBuilder.setParams(params);
        SearchSourceBuilder srcBulder = SearchSourceBuilder.searchSource().ext(List.of(extBuilder));
        SearchRequest request = new SearchRequest("my_index").source(srcBulder);
        GenerativeQAParameters actual = GenerativeQAParamUtil.getGenerativeQAParameters(request);
        assertEquals(params, actual);
    }

    static class DummyStreamOutput extends StreamOutput {

        List<String> list = new ArrayList<>();

        @Override
        public void writeString(String str) {
            list.add(str);
        }

        public List<String> getList() {
            return list;
        }

        @Override
        public void writeByte(byte b) throws IOException {

        }

        @Override
        public void writeBytes(byte[] b, int offset, int length) throws IOException {

        }

        @Override
        public void flush() throws IOException {

        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public void reset() throws IOException {

        }
    }
    public void testWriteTo() throws IOException {
        String conversationId = "a";
        String llmModel = "b";
        String llmQuestion = "c";
        int maxPassages = 2;
        GenerativeQAParameters parameters = new GenerativeQAParameters(conversationId, llmModel, llmQuestion, maxPassages);
        StreamOutput output = new DummyStreamOutput();
        parameters.writeTo(output);
        List<String> actual = ((DummyStreamOutput) output).getList();
        assertEquals(3, actual.size());
        assertEquals(conversationId, actual.get(0));
        assertEquals(llmModel, actual.get(1));
        assertEquals(llmQuestion, actual.get(2));
    }

    public void testMisc() {
        String conversationId = "a";
        String llmModel = "b";
        String llmQuestion = "c";
        int maxPassages = 3;
        GenerativeQAParameters parameters = new GenerativeQAParameters(conversationId, llmModel, llmQuestion, maxPassages);
        assertNotEquals(parameters, null);
        assertNotEquals(parameters, "foo");
        assertEquals(parameters, new GenerativeQAParameters(conversationId, llmModel, llmQuestion, maxPassages));
        assertNotEquals(parameters, new GenerativeQAParameters("", llmModel, llmQuestion, maxPassages));
        assertNotEquals(parameters, new GenerativeQAParameters(conversationId, "", llmQuestion, maxPassages));
        assertNotEquals(parameters, new GenerativeQAParameters(conversationId, llmModel, "", maxPassages));
    }

    public void testToXConent() throws IOException {
        String conversationId = "a";
        String llmModel = "b";
        String llmQuestion = "c";
        int maxPassages = 4;
        GenerativeQAParameters parameters = new GenerativeQAParameters(conversationId, llmModel, llmQuestion, maxPassages);
        XContent xc = mock(XContent.class);
        OutputStream os = mock(OutputStream.class);
        XContentGenerator generator = mock(XContentGenerator.class);
        when(xc.createGenerator(any(), any(), any())).thenReturn(generator);
        XContentBuilder builder = new XContentBuilder(xc, os);
        assertNotNull(parameters.toXContent(builder, null));
    }
}
