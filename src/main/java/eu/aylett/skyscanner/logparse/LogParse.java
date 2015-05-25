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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Main log-parsing class.  Puts everything together and runs it.
 */
public class LogParse {
    private static final Logger LOG = LoggerFactory.getLogger(LogParse.class);

    public static void main(String args[]) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        Consumer<LineDetails> logConsumer = new LogConsumer();
        while (true) {
            String logLine = in.readLine();
            if (logLine == null) {
                // Done
                return;
            }
            Optional<LineDetails> details = LineDetails.parseLogLine(logLine);
            if (details.isPresent()) {
                logConsumer.accept(details.get());
            } else {
                LOG.error("Didn't get a line back, quitting");
                return;
            }
        }
    }

}
