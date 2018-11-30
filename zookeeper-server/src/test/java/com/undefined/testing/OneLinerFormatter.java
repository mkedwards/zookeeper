package com.undefined.testing;

import java.lang.management.ManagementFactory;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitVersionHelper;

import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StringUtils;

/**
 * Prints a single lines of tests to a specified Writer.
 * Inspired by the BriefJUnitResultFormatter and
 * XMLJUnitResultFormatter.
 *
 * @see FormatterElement
 * @see BriefJUnitResultFormatter
 * @see XMLJUnitResultFormatter
 */

public class OneLinerFormatter implements JUnitResultFormatter {

    private final static String TAB_STR = "    ";

    // (\w+\.)+(\w+)\((\w+).(?:\w+):(\d+)\)
    private final static Pattern traceLinePattern = Pattern.compile("(\\w+\\.)+(\\w+)\\((\\w+).(?:\\w+):(\\d+)\\)");

    private final static boolean showCausesLines = true;

    /**
     * Where to write the log to.
     */
    private OutputStream out;

    /**
     * Used for writing the results.
     */
    private PrintWriter output;

    /**
     * Used as part of formatting the results.
     */
    private StringWriter results;

    /**
     * Used for writing formatted results to.
     */
    private PrintWriter resultWriter;

    /**
     * Formatter for timings.
     */
    private NumberFormat numberFormat = NumberFormat.getInstance();

    /**
     * Output suite has written to System.out
     */
    private String systemOutput = null;

    /**
     * Output suite has written to System.err
     */
    private String systemError = null;

    /**
     * tests that failed.
     */
    private Hashtable failedTests = new Hashtable();
    /**
     * Timing helper.
     */
    private Hashtable testStarts = new Hashtable();

    /**
     * Constructor for OneLinerFormatter.
     */
    public OneLinerFormatter() {
        results = new StringWriter();
        resultWriter = new PrintWriter(results);
    }

    /**
     * Sets the stream the formatter is supposed to write its results to.
     * @param out the output stream to write to
     */
    public synchronized void setOutput(OutputStream out) {
        this.out = out;
        output = new PrintWriter(out);
    }

    /**
     * @see JUnitResultFormatter#setSystemOutput(String)
     */
    public synchronized void setSystemOutput(String out) {
        systemOutput = out;
    }

    /**
     * @see JUnitResultFormatter#setSystemError(String)
     */
    public synchronized void setSystemError(String err) {
        systemError = err;
    }

    /**
     * The whole testsuite started.
     * @param suite the test suite
     */
    public synchronized void startTestSuite(JUnitTest suite) {
        if (output == null) {
            return; // Quick return - no output do nothing.
        }
        StringBuffer sb = new StringBuffer(StringUtils.LINE_SEP);
        sb.append(StringUtils.LINE_SEP);
        sb.append("----------------------------------------------------------");
        sb.append(StringUtils.LINE_SEP);
        sb.append("Testsuite: ");
        sb.append(suite.getName());
        sb.append(" In process: ");
        sb.append(ManagementFactory.getRuntimeMXBean().getName());
        sb.append(StringUtils.LINE_SEP);
        output.write(sb.toString());
        output.flush();
    }

    public final static void prefixLines(StringBuffer sb, String prefix, String buffer) {
        String lines[] = buffer.split("\\r?\\n");
        for(int i = 0; i < lines.length; i++) {
            sb.append(prefix)
                .append(lines[i]);
        }
    }


    /**
     * The whole testsuite ended.
     * @param suite the test suite
     */
    public synchronized void endTestSuite(JUnitTest suite) {
        StringBuffer sb = new StringBuffer("Tests run: ");
        String runtimeName = " [" + ManagementFactory.getRuntimeMXBean().getName() + "] ";
        sb.append(suite.runCount());
        sb.append(", Failures: ");
        sb.append(suite.failureCount());
        sb.append(", Errors: ");
        sb.append(suite.errorCount());
        sb.append(", Time elapsed: ");
        sb.append(numberFormat.format(suite.getRunTime() / 1000.0));
        sb.append(" sec");
        sb.append(" In process: ");
        sb.append(ManagementFactory.getRuntimeMXBean().getName());
        sb.append(StringUtils.LINE_SEP);
        sb.append(StringUtils.LINE_SEP);

        output.write(sb.toString());
        output.flush();
        sb = new StringBuffer();

        // append the err and output streams to the log
        if (systemOutput != null && systemOutput.length() > 0) {
            sb.append(runtimeName)
                    .append("------------- Standard Output ---------------")
                    .append(StringUtils.LINE_SEP);

            prefixLines(sb, runtimeName, systemOutput);

            sb.append(StringUtils.LINE_SEP)
                    .append(runtimeName)
                    .append("------------- ---------------- ---------------")
                    .append(StringUtils.LINE_SEP);

            output.write(sb.toString());
            output.flush();
            sb = new StringBuffer();
        }

        if (systemError != null && systemError.length() > 0) {
            sb.append(runtimeName)
                    .append("------------- Standard Error -----------------")
                    .append(StringUtils.LINE_SEP);

            prefixLines(sb, runtimeName, systemError);

            sb.append(StringUtils.LINE_SEP)
                    .append(runtimeName)
                    .append("------------- ---------------- ---------------")
                    .append(StringUtils.LINE_SEP);

            output.write(sb.toString());
            output.flush();
            sb = new StringBuffer();
        }

        if (output != null) {
            try {
                output.write(sb.toString());
                resultWriter.close();
                output.write(results.toString());
                output.flush();
            } finally {
                if (out != System.out && out != System.err) {
                    FileUtils.close(out);
                }
            }
        }
    }

    /**
     * A test started.
     * @param test a test
     */
    public synchronized void startTest(Test test) {
        testStarts.put(test, new Long(System.currentTimeMillis()));
    }

    /**
     * A test ended.
     * @param test a test
     */
    public synchronized void endTest(Test test) {
        // Fix for bug #5637 - if a junit.extensions.TestSetup is
        // used and throws an exception during setUp then startTest
        // would never have been called
        if (!testStarts.containsKey(test)) {
            startTest(test);
        }

        boolean failed = failedTests.containsKey(test);

        Long l = (Long) testStarts.get(test);

        String runtimeName = " [" + ManagementFactory.getRuntimeMXBean().getName() + "] ";
        output.write(runtimeName);
        output.write("Ran [");
        output.write(((System.currentTimeMillis() - l.longValue()) / 1000.0) + "] ");
        output.write(getTestName(test) + " ... " + (failed ? "FAILED" : "OK"));
        output.write(StringUtils.LINE_SEP);
        output.flush();
    }

    /**
     * Interface TestListener for JUnit &gt; 3.4.
     *
     * <p>A Test failed.
     * @param test a test
     * @param t    the assertion failed by the test
     */
    public synchronized void addFailure(Test test, AssertionFailedError t) {
        formatError("\tFAILED", test, (Throwable) t);
    }

    /**
     * A test caused an error.
     * @param test  a test
     * @param error the error thrown by the test
     */
    public synchronized void addError(Test test, Throwable error) {
        formatError("\tCaused an ERROR", test, error);
    }

    /**
     * Get test name
     *
     * @param test a test
     * @return test name
     */
    private final static String getTestName(Test test) {
        if (test == null) {
            return "null";
        } else {
            return JUnitVersionHelper.getTestCaseClassName(test) + ":" +
                JUnitVersionHelper.getTestCaseName(test);
        }
    }

    /**
     * Get test case full class name
     *
     * @param test a test
     * @return test full class name
     */
    private final static String getTestCaseClassName(Test test) {
        if (test == null) {
            return "null";
        } else {
            return JUnitVersionHelper.getTestCaseClassName(test);
        }
    }

    /**
     * Format the test for printing..
     * @param test a test
     * @return the formatted testname
     */
    private final static String formatTest(Test test) {
        if (test == null) {
            return "Null Test: ";
        } else {
            return "Testcase: " + test.toString() + ":";
        }
    }

    /**
     * Format an error and print it.
     * @param type the type of error
     * @param test the test that failed
     * @param error the exception that the test threw
     */
    private final void formatError(String type, Test test,
                                            Throwable error) {
        if (test != null) {
            failedTests.put(test, test);
            endTest(test);
        }

        resultWriter.println(formatTest(test) + type);
        resultWriter.println(TAB_STR + "(" + error.getClass().getSimpleName() + "): " +
                ((error.getMessage() != null) ? error.getMessage() : error));

        if (showCausesLines) {
            // resultWriter.append(StringUtils.LINE_SEP);
            resultWriter.println(filterErrorTrace(test, error));
        }

        resultWriter.println();

        /* String strace = JUnitTestRunner.getFilteredTrace(error);
        resultWriter.println(strace);
        resultWriter.println(); */
    }

    private final static String filterErrorTrace(Test test, Throwable error) {
        String trace = StringUtils.getStackTrace(error);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        StringReader sr = new StringReader(trace);
        BufferedReader br = new BufferedReader(sr);

        String testCaseClassName = getTestCaseClassName(test);
        String line;
        try {
            while ((line = br.readLine()) != null) {
                if (line.indexOf(testCaseClassName) != -1) {
                    Matcher matcher = traceLinePattern.matcher(line);
                    // pw.println(matcher + ": " + matcher.find());
                    if (matcher.find()) {
                        pw.print(TAB_STR);
                        pw.print("(" + matcher.group(3) + ") ");
                        pw.print(matcher.group(2) + ": ");
                        pw.println(matcher.group(4));
                    } else {
                        pw.println(line);
                    }

                }
            }
        } catch (Exception e) {
            return trace; // return the treca unfiltered
        }

        return sw.toString();
    }
}
