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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;

/**
 * Main log-parsing class.  Puts everything together and runs it.
 */
public class LogParse {
    private static final Logger LOG = LoggerFactory.getLogger(LogParse.class);
    private final boolean aggregate;
    private final boolean detail;
    private final List<BufferedReader> inputs;
    private final ObjectMapper mapper;

    public LogParse(boolean aggregate, boolean detail, List<BufferedReader> inputs, ObjectMapper mapper) {
        this.aggregate = aggregate;
        this.detail = detail;
        this.inputs = inputs;
        this.mapper = mapper;
    }

    public static void main(String args[]) throws IOException {
        boolean aggregate = true;
        boolean detail = true;
        boolean verbose = false;
        List<BufferedReader> inputs = newArrayList();
        for (String arg : args) {
            switch (arg) {
                case "--no-aggregate":
                    aggregate = false;
                    break;
                case "--no-detail":
                    detail = false;
                    break;
                case "--verbose":
                case "-v":
                    verbose = true;
                    break;
                case "-":
                    inputs.add(new BufferedReader(new InputStreamReader(System.in)));
                    break;
                case "--help":
                    System.out.println("Options: [--no-aggregate] [--no-detail] [files...]");
                    System.out.println("StdIn can be represented by '-' or by not providing any files");
                    return;
                default:
                    inputs.add(Files.newBufferedReader(Paths.get(arg)));
                    break;
            }
        }

        // Set up logging
        Level loggerLevel = verbose ? Level.DEBUG : Level.ERROR;
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger(ROOT_LOGGER_NAME).setLevel(loggerLevel);

        LOG.debug("Verbose: {}", verbose);
        LOG.debug("Detail: {}", detail);
        LOG.debug("Aggregate: {}", aggregate);

        if (inputs.isEmpty()) {
            LOG.info("No files given: using std input");
            inputs.add(new BufferedReader(new InputStreamReader(System.in)));
        }

        if (!(detail || aggregate)) {
            LOG.error("No detail or aggregate makes no output");
            return;
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        LogParse app = new LogParse(aggregate, detail, inputs, mapper);
        app.run();
    }

    public void run() throws IOException {
        LogDetailConsumer logDetailConsumer = new LogDetailConsumer();
        for (BufferedReader in: inputs) {
            while (true) {
                String logLine = in.readLine();
                if (logLine == null) {
                    // Done
                    in.close();
                    break;
                }
                if (logLine.trim().isEmpty()) {
                    continue;
                }
                Optional<LineDetails> details = LineDetails.parseLogLine(logLine);
                if (details.isPresent()) {
                    logDetailConsumer.accept(details.get());
                }
                // Continue if we don't get a line: the parser will log why.
            }
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(System.out)) {
            if (detail && aggregate) {
                mapper.writeValue(writer, logDetailConsumer);
            } else if (detail) {
                mapper.writeValue(writer, logDetailConsumer.detail());
            } else if (aggregate) {
                mapper.writeValue(writer, logDetailConsumer.aggregate());
            }
        }
    }
}
