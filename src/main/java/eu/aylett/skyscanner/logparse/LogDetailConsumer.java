/*
 * Copyright 2015 Andrew Aylett
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

package eu.aylett.skyscanner.logparse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Collects {@link LineDetails}, feeding them onwards when we have a minute's worth.
 */
public class LogDetailConsumer implements Consumer<LineDetails> {
    private static final Logger LOG = LoggerFactory.getLogger(LogDetailConsumer.class);

    LoadingCache<DateTime, LogMinuteAggregator> minutes = CacheBuilder.newBuilder().build(new CacheLoader<DateTime, LogMinuteAggregator>() {
        @Override
        public LogMinuteAggregator load(DateTime key) throws Exception {
            return new LogMinuteAggregator(key);
        }
    });

    @Override
    public void accept(LineDetails lineDetails) {
        DateTime lineMinute = lineDetails.timestamp.withSecondOfMinute(0);
        try {
            minutes.get(lineMinute).accept(lineDetails);
        } catch (ExecutionException e) {
            LOG.error("Failed to get the aggregator for a minute: {}", lineMinute, e);
        }
    }

    public void generateOutput(Writer w, ObjectMapper mapper) throws IOException {
        w.write(mapper.writeValueAsString(new TreeMap<>(minutes.asMap())));
    }

    public void generateAggregateOutput(OutputStreamWriter writer, ObjectMapper mapper) throws IOException {
        Set<DateTime> times = minutes.asMap().keySet();
        if (times.isEmpty()) {
            writer.write("No data\n");
            return;
        }

        LogGlobalAggregator aggregator = new LogGlobalAggregator();
        minutes.asMap().values().forEach(aggregator::accept);

        writer.write(mapper.writeValueAsString(aggregator));
    }
}
