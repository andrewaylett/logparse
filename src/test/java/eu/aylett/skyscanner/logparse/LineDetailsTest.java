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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for the {@link LineDetails} class.
 */
public class LineDetailsTest {
    @Test
    public void canParseFirstLine() {
        String logLine = "127.0.0.1 - - [30/Mar/2015:05:04:20 +0100] \"GET /render/?from=-11minutes&until=-5mins&uniq=1427688307512&format=json&target=alias%28movingAverage%28divideSeries%28sum%28nonNegativeDerivative%28collector.uk1.rou.*rou*.svc.*.RoutesService.routedate.total.processingLatency.totalMillis.count%29%29%2Csum%28nonNegativeDerivative%28collector.uk1.rou.*rou*.svc.*.RoutesService.routedate.total.processingLatency.totalCalls.count%29%29%29%2C%275minutes%27%29%2C%22Latency%22%29 HTTP/1.1\" 200 157 165169";
        Optional<LineDetails> maybeLineDetails = LineDetails.parseLogLine(logLine);
        assertThat(maybeLineDetails, isPresent());
        LineDetails lineDetails = maybeLineDetails.get();

        assertThat(lineDetails.timestamp.toInstant(), equalTo(DateTime.parse("2015-03-30T05:04:20+0100").toInstant()));
        assertThat(lineDetails.bytesTransferred, equalTo(157l));
        assertThat(lineDetails.status, equalTo(StatusClass.SUCCESS));
        assertThat(lineDetails.timeTaken, equalTo(165169l));
    }

    private Matcher<? super Optional<?>> isPresent() {
        return new BaseMatcher<Optional<?>>() {
            @Override
            public boolean matches(Object item) {
                return item instanceof Optional<?> && ((Optional<?>) item).isPresent();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("is a present Optional<?>");
            }
        };
    }
}
