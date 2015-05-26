Log Parsing demo
----------------

Build by running `mvn verify`.

Run either directly as the built executable JAR or by using the helper script
`./logparse`.

`./logparse` (or the JAR) will process standard input if no files are given,
and will up its logging level if passed `-v` or `--verbose`.  This doesn't add
much extra output.  If you want to process files as well as standard input, you
can use `-` to represent standard input.

---

I didn't find the requirements especially clear, so there are two sets of data
output, either of which may be suppressed by a command-line option.

The first output prints an entry for each minute that passes.  This is to
satisfy the requirement of "mean response time per minute".  You can suppress
this data by using the flag `--no-detail`.

The second output prints aggregate data for the whole time period.  It prints
out both the overall mean response time and the per-minute mean of the total
time taken to respond in each minute.  You can suppress this data using the
flag `--no-aggregate`.

Both output options print the number of successful and failing requests
(treating a 2xx or 3xx response as a success and anything else as a failure)
either for the minute in question or the average per-minute as appropriate, and
similarly for the number of megabytes transferred (where 1MB = 2^20 bytes).
The aggregate output also shows how many minutes of logs were processed.
