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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests that aggregating a single minute works correctly.
 *
 * It's a fairly simple class, not worth testing exhaustively.
 */
public class LogMinuteAggregatorTest {
    DateTime testTime = DateTime.parse("2015-05-26T12:00:00+0100");
    LogMinuteAggregator aggregator = new LogMinuteAggregator(testTime);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void canNotSupplyWrongMinute() {
        expectedException.expect(IllegalStateException.class);
        aggregator.accept(new LineDetails(testTime.plusMinutes(1), StatusClass.SUCCESS, 10l, 10l));
    }

    @Test
    public void addingOneLineGivesRightResult() {
        aggregator.accept(new LineDetails(testTime.plusSeconds(5), StatusClass.SUCCESS, 10l, 200l));
        assertEquals("Should return the value in MB", aggregator.getMbSent(), 0.0000095367431640625, 0.00000001);
        assertThat(aggregator.getSuccessful(), equalTo(1l));
    }
}
