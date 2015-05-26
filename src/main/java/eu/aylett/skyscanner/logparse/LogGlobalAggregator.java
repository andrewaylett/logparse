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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.Duration;
import org.joda.time.Instant;

import java.util.function.Consumer;

/**
 * When fed {@link LogMinuteAggregator} instances, can output global aggregated
 * statistics.
 */
public class LogGlobalAggregator implements Consumer<LogMinuteAggregator> {

    private long totalSuccessful = 0;
    private long totalFailures = 0;
    private long totalTime = 0;
    private long totalBytes = 0;
    private long totalCount = 0;

    private Instant earliest = null;
    private Instant latest = null;

    @Override
    public void accept(LogMinuteAggregator minuteAggregation) {
        totalSuccessful += minuteAggregation.getSuccessful();
        totalFailures += minuteAggregation.getFailures();
        totalTime += minuteAggregation.getTime();
        totalBytes += minuteAggregation.getBytes();
        totalCount += minuteAggregation.getCount();

        Instant i = minuteAggregation.getMinute().toInstant();
        if (earliest == null || earliest.compareTo(i) > 0) {
            earliest = i;
        }
        if (latest == null || latest.compareTo(i) < 0) {
            latest = i;
        }
    }

    @JsonProperty
    public long getDurationInMinutes() {
        return new Duration(earliest, latest.plus(Duration.standardMinutes(1))).getStandardMinutes();
    }

    @JsonProperty
    public double getSuccessfulPerMinute() {
        return (double)totalSuccessful/getDurationInMinutes();
    }

    @JsonProperty
    public double getFailuresPerMinute() {
        return (double)totalFailures/getDurationInMinutes();
    }

    @JsonProperty
    public long getMeanResponseTime() {
        return totalTime/totalCount;
    }

    @JsonProperty
    public long getTimeSpentRespondingPerMinute() {
        return totalTime/getDurationInMinutes();
    }

    @JsonProperty
    public double getMbSentPerMinute() {
        return (double)totalBytes/(1024*1024*getDurationInMinutes());
    }
}
