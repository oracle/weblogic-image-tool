// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;

public class AdditionalBuildCommands {
    private static final LoggingFacade logger = LoggingFactory.getLogger(AdditionalBuildCommands.class);

    public static final String PACKAGES = "package-manager-packages";
    public static final String INITIAL_BLD = "initial-build-commands";
    public static final String BEFORE_JDK = "before-jdk-install";
    public static final String AFTER_JDK = "after-jdk-install";
    public static final String BEFORE_FMW = "before-fmw-install";
    public static final String AFTER_FMW = "after-fmw-install";
    public static final String BEFORE_WDT = "before-wdt-command";
    public static final String AFTER_WDT = "after-wdt-command";
    public static final String FINAL_BLD = "final-build-commands";

    private final List<NamedPattern> sections;
    private final Map<String, List<String>> contents;

    /**
     * Load the contents of the additional build commands file into this object.
     * Existing data in this instance are replaced.
     * @param file Path to the additional build commands file that should be loaded.
     * @throws IOException if an issue occurs locating or opening the file provided
     */
    public AdditionalBuildCommands(Path file) throws IOException {
        sections = new ArrayList<>();
        sections.add(getPattern(PACKAGES));
        sections.add(getPattern(INITIAL_BLD));
        sections.add(getPattern(BEFORE_JDK));
        sections.add(getPattern(AFTER_JDK));
        sections.add(getPattern(BEFORE_FMW));
        sections.add(getPattern(AFTER_FMW));
        sections.add(getPattern(BEFORE_WDT));
        sections.add(getPattern(AFTER_WDT));
        sections.add(getPattern(FINAL_BLD));
        contents = new HashMap<>();
        load(file);
    }

    private static NamedPattern getPattern(String key) {
        return new NamedPattern(key, getSectionHeaderString(key));
    }

    private static String getSectionHeaderString(String key) {
        return MessageFormat.format("\\s*\\[\\s*{0}\\s*\\]\\s*", key);
    }

    /**
     * Once a file is loaded, size returns the number of sections found in the file.
     * @return the count of sections found in the file.
     */
    public int size() {
        return contents.size();
    }

    /**
     * Get the contents of the file provided on the command line as additional build commands.
     * The file is parsed into a map based on the sections in the file. The returned map
     * will have one entry per section found in the provided file (such as "final-build-commands").
     * The Callable list value for each entry is for late resolution of the contents.  If the user
     * provides mustache placeholders in the file, those placeholders will get resolved when the call
     * method is invoked and not when getContents is returned.  This allows getContents to be called
     * before DockerfileOptions is completely populated.
     *
     * @param options The backing file that will be used to resolve any mustache placeholders in the file
     * @return a map by section name of the additional build file contents
     */
    public Map<String, Callable<List<String>>> getContents(DockerfileOptions options) {
        Map<String, Callable<List<String>>> callableResult = new HashMap<>();
        for (Map.Entry<String, List<String>> entry: contents.entrySet()) {
            // implements the "call" method so that the contents are resolved at the time they are retrieved
            Callable<List<String>> value = () -> {
                List<String> resolvedLines = new ArrayList<>();
                MustacheFactory mf = new DefaultMustacheFactory();
                // Parse each line in the file contents using mustache and the provided "options"
                for (String line: entry.getValue()) {
                    StringWriter writer = new StringWriter();
                    Mustache mustache = mf.compile(new StringReader(line), "x");
                    mustache.execute(writer, options);
                    writer.flush();
                    resolvedLines.add(writer.toString());
                }
                return resolvedLines;
            };
            callableResult.put(entry.getKey(), value);
        }
        return callableResult;
    }

    /**
     * Once a file is loaded, getSection should return the contents of a single section, by name.
     * @param name the name of the section to return
     * @return the contents of the section requested or null if the section was not found.
     */
    public List<String> getSection(String name) {
        return contents.get(name);
    }

    /**
     * Load the contents of the additional build commands file into this object.
     * Existing data in this instance are replaced.
     * @param file Path to the additional build commands file that should be loaded.
     * @throws IOException if an issue occurs locating or opening the file provided
     */
    private void load(Path file) throws IOException {
        logger.entering(file);
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            List<String> buffer = new ArrayList<>();
            String currentSection = null;
            String line;
            while ((line = reader.readLine()) != null) {
                logger.finest(line);
                String sectionStart = checkForSectionHeader(line);
                //skip any lines that come before the first section header
                if (currentSection != null && sectionStart == null) {
                    //collect lines inside current section
                    buffer.add(line);
                } else if (currentSection != null) {
                    //while in a section, found next section start (save last section, and start new section)
                    logger.fine("IMG-0015", buffer.size(), currentSection);
                    contents.put(currentSection, buffer);
                    buffer = new ArrayList<>();
                    currentSection = sectionStart;
                } else if (sectionStart != null) {
                    //current section was null, but found new section start
                    currentSection = sectionStart;
                }
            }

            if (currentSection != null && !buffer.isEmpty()) {
                //finished reading file, store the remaining lines that were read for the section
                logger.fine("IMG-0015", buffer.size(), currentSection);
                contents.put(currentSection, buffer);
            }
        } catch (IOException ioe) {
            logger.severe("IMG-0013", file.getFileName());
            throw ioe;
        }

        logger.exiting();
    }

    private String checkForSectionHeader(String line) {
        //Pattern: looks like something in square brackets, ignoring whitespace
        Pattern sectionLike = Pattern.compile(getSectionHeaderString(".*"));
        if (!sectionLike.matcher(line).matches()) {
            //line does not look like a header "[something]"
            return null;
        }

        for (NamedPattern section : sections) {
            if (section.matches(line)) {
                return section.getName();
            }
        }
        throw new IllegalArgumentException(Utils.getMessage("IMG-0014", line, validSectionNames()));
    }

    /**
     * Returns a list of valid section names for the additional build commands file.
     * @return comma separated list of section names
     */
    public String validSectionNames() {
        return sections.stream()
            .map(NamedPattern::getName)
            .collect(Collectors.joining(", "));
    }
}
