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

import com.google.common.base.MoreObjects;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds the details we need about a log line.
 */
public class LineDetails {
    private static final Logger LOG = LoggerFactory.getLogger(LineDetails.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("dd/MMM/yyyy:HH:mm:ss Z");
    /*
     * Pattern is "%a %l %u %t \"%r\" %>s %b %D” -- that is:
     *  * Address (ignored)
     *  * Logname (ignored)
     *  * User (ignored)
     *  * Timestamp (match group 1)
     *  * Request (ignored)
     *  * Status Code (match group 2)
     *  * Bytes read (match group 3)
     *  * Time taken (match group 4)
     */
    private static final Pattern pattern = Pattern.compile("\\S+ \\S+ \\S+ \\[(\\S+ \\S+)] .* (\\d{3}) (\\d+|-) (\\d+)");
    public final DateTime timestamp;
    public final StatusClass status;
    public final long bytesTransferred;
    public final long timeTaken;

    public LineDetails(DateTime timestamp, StatusClass status, long bytesTransferred, long timeTaken) {
        this.timestamp = timestamp;
        this.status = status;
        this.bytesTransferred = bytesTransferred;
        this.timeTaken = timeTaken;
    }

    static Optional<LineDetails> parseLogLine(String logLine) {
        Matcher matcher = pattern.matcher(logLine);
        if (!matcher.matches()) {
            LOG.error("Failed to match line: {}", logLine);
            return Optional.empty();
        }

        DateTime timestamp;
        String timestampString = matcher.group(1);
        try {
            timestamp = DateTime.parse(timestampString, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            LOG.error("Failed to parse timestamp \"{}\" from {}", timestampString, logLine, e);
            return Optional.empty();
        }

        String statusString = matcher.group(2);
        StatusClass status = statusString.startsWith("2") || statusString.startsWith("3") ? StatusClass.SUCCESS : StatusClass.FAILURE;

        long bytesTransferred;
        String bytesString = matcher.group(3);
        try {
            if (bytesString.equals("-")) {
                bytesTransferred = 0;
            } else {
                bytesTransferred = Long.parseLong(bytesString);
            }
        } catch (NumberFormatException e) {
            LOG.error("Failed to read number of bytes \"{}\" from {}", bytesString, logLine, e);
            return Optional.empty();
        }

        long timeTaken;
        String timeString = matcher.group(4);
        try {
            timeTaken = Long.parseLong(timeString);
        } catch (NumberFormatException e) {
            LOG.error("Failed to read time taken \"{}\" from {}", timeString, logLine, e);
            return Optional.empty();
        }

        return Optional.of(new LineDetails(timestamp, status, bytesTransferred, timeTaken));
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("timestamp", timestamp)
                .add("status", status)
                .add("bytesTransferred", bytesTransferred)
                .add("timeTaken", timeTaken)
                .toString();
    }
}
