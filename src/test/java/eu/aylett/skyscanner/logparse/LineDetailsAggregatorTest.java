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

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link LineDetailsAggregator}.
 */
public class LineDetailsAggregatorTest {
    DateTime testTime = DateTime.parse("2015-05-26T12:00:00+0100");
    LineDetailsAggregator aggregator = new LineDetailsAggregator();

    @Test
    public void oneLineTakesOneMinute() {
        aggregator.accept(new LineDetails(testTime, StatusClass.SUCCESS, 10l, 200l));
        assertThat(aggregator.aggregate().getDurationInMinutes(), equalTo(1l));
    }

    @Test
    public void twoLinesOverTwoMinutesTakesTwoMinutes() {
        aggregator.accept(new LineDetails(testTime, StatusClass.SUCCESS, 10l, 200l));
        aggregator.accept(new LineDetails(testTime.plusSeconds(90), StatusClass.SUCCESS, 10l, 200l));
        LogGlobalAggregator aggregate = aggregator.aggregate();
        assertThat("Duration in minutes", aggregate.getDurationInMinutes(), equalTo(2l));
        assertThat("Successful per minute", aggregate.getSuccessfulPerMinute(), equalTo(1.0));
        assertThat("Failed per minute", aggregate.getFailuresPerMinute(), equalTo(0.0));
        assertThat("Mean response time", aggregate.getMeanResponseTime(), equalTo(200l));
        assertThat("Time responding per minute", aggregate.getTimeSpentRespondingPerMinute(), equalTo(200l));
    }

    @Test
    public void threeLinesOverTwoMinutesTakesTwoMinutes() {
        aggregator.accept(new LineDetails(testTime, StatusClass.SUCCESS, 10l, 200l));
        aggregator.accept(new LineDetails(testTime.plusSeconds(30), StatusClass.FAILURE, 10l, 200l));
        aggregator.accept(new LineDetails(testTime.plusSeconds(90), StatusClass.SUCCESS, 10l, 200l));
        LogGlobalAggregator aggregate = aggregator.aggregate();
        assertThat("Duration in minutes", aggregate.getDurationInMinutes(), equalTo(2l));
        assertThat("Successful per minute", aggregate.getSuccessfulPerMinute(), equalTo(1.0));
        assertThat("Failed per minute", aggregate.getFailuresPerMinute(), equalTo(0.5));
        assertThat("Mean response time", aggregate.getMeanResponseTime(), equalTo(200l));
        assertThat("Time responding per minute", aggregate.getTimeSpentRespondingPerMinute(), equalTo(300l));
        assertThat("MB transferred per minute", aggregate.getMbSentPerMinute(), equalTo(15.0/(1024*1024)));
    }
}
