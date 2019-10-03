// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;

public class AdditionalBuildCommands {
    private static final LoggingFacade logger = LoggingFactory.getLogger(AdditionalBuildCommands.class);

    public static final String BEFORE_JDK = "before-jdk-install";
    public static final String AFTER_JDK = "after-jdk-install";
    public static final String BEFORE_FMW = "before-fmw-install";
    public static final String AFTER_FMW = "after-fmw-install";
    public static final String BEFORE_WDT = "before-wdt-command";
    public static final String AFTER_WDT = "after-wdt-command";
    public static final String FINAL_BLD = "final-build-commands";

    private List<NamedPattern> sections;
    private Map<String, List<String>> contents;

    private AdditionalBuildCommands() {
        sections = new ArrayList<>();
        sections.add(getPattern(BEFORE_JDK));
        sections.add(getPattern(AFTER_JDK));
        sections.add(getPattern(BEFORE_FMW));
        sections.add(getPattern(AFTER_FMW));
        sections.add(getPattern(BEFORE_WDT));
        sections.add(getPattern(AFTER_WDT));
        sections.add(getPattern(FINAL_BLD));
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

    public Map<String,List<String>> getContents() {
        return contents;
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
    public static AdditionalBuildCommands load(Path file) throws IOException {
        logger.entering(file);
        AdditionalBuildCommands result = new AdditionalBuildCommands();
        result.contents = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            List<String> buffer = new ArrayList<>();
            String currentSection = null;
            String line;
            while ((line = reader.readLine()) != null) {
                logger.finest(line);
                String sectionStart = result.checkForSectionHeader(line);
                //skip any lines that come before the first section header
                if (currentSection != null && sectionStart == null) {
                    //collect lines inside current section
                    buffer.add(line);
                } else if (currentSection != null) {
                    //while in a section, found next section start (save last section, and start new section)
                    logger.fine("IMG-0015", buffer.size(), currentSection);
                    result.contents.put(currentSection, buffer);
                    buffer = new ArrayList<>();
                    currentSection = sectionStart;
                } else if (sectionStart != null) {
                    //current section was null, but found new section start
                    currentSection = sectionStart;
                }
            }

            if (currentSection != null && buffer.size() > 0) {
                //finished reading file, store the remaining lines that were read for the section
                logger.fine("IMG-0015", buffer.size(), currentSection);
                result.contents.put(currentSection, buffer);
            }
        } catch (IOException ioe) {
            logger.severe("IMG-0013", file.getFileName());
            throw ioe;
        }

        logger.exiting();
        return result;
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
