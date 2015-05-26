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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;

import java.util.function.Consumer;

/**
 * Takes a collection of {@link LineDetails} and aggregates their stats.
 */
public class LogMinuteAggregator implements Consumer<LineDetails> {
    private final DateTime minute;
    private long count;
    private long bytes;
    private long time;
    private long successful;
    private long failures;

    public LogMinuteAggregator(DateTime minute) {
        this.minute = minute;
        count = 0;
        bytes = 0;
        time = 0;
        successful = 0;
        failures = 0;
    }

    @Override
    public void accept(LineDetails line) {
        Preconditions.checkState(line.timestamp.withSecondOfMinute(0).equals(minute), "Passed a line with a different minute");
        count++;
        bytes += line.bytesTransferred;
        time += line.timeTaken;
        switch (line.status) {
            case SUCCESS:
                successful++;
                break;
            case FAILURE:
                failures++;
                break;
            default:
                throw new IllegalStateException("Switch should have been exhaustive");
        }
    }

    @JsonProperty
    public long getSuccessful() {
        return successful;
    }

    @JsonProperty
    public long getFailures() {
        return failures;
    }

    @JsonIgnore
    public long getCount() {
        return count;
    }

    @JsonIgnore
    public long getBytes() {
        return bytes;
    }

    @JsonIgnore
    public long getTime() {
        return time;
    }

    @JsonProperty
    public long getMeanResponseTime() {
        return time/count;
    }

    @JsonProperty
    public double getMbSent() {
        return (double)bytes/(1024*1024);
    }

    @JsonIgnore
    public DateTime getMinute() {
        return minute;
    }
}
