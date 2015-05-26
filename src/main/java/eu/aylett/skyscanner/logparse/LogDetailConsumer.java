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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Collects {@link LineDetails}, feeding them onwards when we have a minute's worth.
 */
public class LogDetailConsumer implements Consumer<LineDetails> {
    private static final Logger LOG = LoggerFactory.getLogger(LogDetailConsumer.class);

    LoadingCache<DateTime, LogAggregator> minutes = CacheBuilder.newBuilder().build(new CacheLoader<DateTime, LogAggregator>() {
        @Override
        public LogAggregator load(DateTime key) throws Exception {
            return new LogAggregator(key);
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

    public void generateOutput(Writer w) throws IOException {
        List<Map.Entry<DateTime, LogAggregator>> entries = newArrayList(minutes.asMap().entrySet());
        entries.sort((a, b) -> a.getKey().compareTo(b.getKey()));
        for (Map.Entry<DateTime, LogAggregator> e : entries) {
            w.write(e.getValue().resultString());
            w.write('\n');
        }
    }

    public void generateAggregateOutput(OutputStreamWriter writer) throws IOException {
        Set<DateTime> times = minutes.asMap().keySet();
        if (times.isEmpty()) {
            writer.write("No data\n");
            return;
        }
        Instant earliest, latest;
        earliest = latest = times.iterator().next().toInstant();

        for (DateTime dt: times) {
            Instant i = dt.toInstant();
            if (earliest.compareTo(dt) > 0) {
                earliest = i;
            }
            if (latest.compareTo(dt) < 0) {
                latest = i;
            }
        }

        Duration duration = new Duration(earliest, latest.plus(Duration.standardMinutes(1)));
        long durationInMinutes = duration.getStandardMinutes();

        long totalSuccessful = 0;
        long totalFailures = 0;
        long totalTime = 0;
        long totalBytes = 0;
        long totalCount = 0;
        for (LogAggregator minuteAggregation : minutes.asMap().values()) {
            totalSuccessful += minuteAggregation.getSuccessful();
            totalFailures += minuteAggregation.getFailures();
            totalTime += minuteAggregation.getTime();
            totalBytes += minuteAggregation.getBytes();
            totalCount += minuteAggregation.getCount();
        }

        writer.write(String.format("Aggregate over %d minutes:\n  successful: %f/min\n  failed: %f/min\n  meanResponseTime: %d us\n  meanTimeEachMinuteSpentResponding: %d us\n  mbSent: %f/min\n",
                durationInMinutes,
                (double) totalSuccessful/durationInMinutes,
                (double) totalFailures/durationInMinutes,
                totalTime/totalCount,
                totalTime/durationInMinutes,
                (double) totalBytes/(1024*1024*durationInMinutes)));
    }
}
