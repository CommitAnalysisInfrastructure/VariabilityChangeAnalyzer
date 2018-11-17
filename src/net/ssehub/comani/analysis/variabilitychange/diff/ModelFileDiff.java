/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.ssehub.comani.analysis.variabilitychange.diff;

import java.util.regex.Pattern;

/**
 * This class implements analysis methods and required attributes for counting the changes made to a model file
 * (Kconfig).<br><br>
 * 
 * In particular, this class differentiates between changed lines containing general model elements, e.g.
 * help texts or comments, and lines that contain variability information, e.g. configuration symbols.<br><br>
 * 
 * @author Christian Kroeher
 *
 */
public class ModelFileDiff extends FileDiff {
    
    /**
     * String identifying the start of a comment in a model file.
     */
    private static final String MODEL_COMMENT_MARKER = "#";
    
    /**
     * Regex identifying lines containing the start of a comment for a configuration option.<br><br>
     * 
     * Value: {@value #MODEL_CONFIG_COMMENT_PATTERN};
     */
    private static final String MODEL_CONFIG_COMMENT_PATTERN = "^\\s*comment\\s+\\\".*";
    
    /**
     * Regex identifying lines containing configuration definitions (variability information).<br><br>
     * 
     * Value: {@value #MODEL_CONFIG_DEF_PATTERN};
     */
    private static final String MODEL_CONFIG_DEF_PATTERN = "^\\s*(config|menuconfig|choice|endchoice|menu|endmenu"
            + "|if|endif|bool|tristate|string|hex|int|default"
            + "|def_bool|def_tristate|prompt|select|visible if|range)(\\s+.*)?";
    
    /**
     * Regex identifying lines containing a "source"-statement (variability information).<br><br>
     * 
     * Value: {@value #MODEL_FILE_INCLUDE_PATTERN};
     */
    private static final String MODEL_FILE_INCLUDE_PATTERN = "^\\s*source\\s+((\\\".*\\\".*)|(.*\\/.*))";
    
    /**
     * Regex identifying lines containing a "depends on"-statement.<br><br>
     * 
     * If such a statement is found, the previous line has to be checked for variability information as
     * this statement can also be used for comments. Thus, only if the previous line does not contain a
     * comment, the identified line has to be counted as variability information.<br><br>
     * 
     * Value: {@value #MODEL_DEPENDS_ON_PATTERN}
     */
    private static final String MODEL_DEPENDS_ON_PATTERN = "^\\s*depends on\\s+.*";
    
    /**
     * Construct a new {@link ModelFileDiff}.<br><br>
     * 
     * This constructor will call the super constructor of {@link FileDiff}, which will start a
     * line-wise analysis of the given diff lines calling the inherited methods {@link #normalize(String, int)} and
     * {@link #isVariabilityChange(String, int)} defined in this class.<br>
     * Counting the number of changed lines (regardless of variability information) is done in {@link FileDiff}.
     * 
     * @param diffLines the lines of a model diff
     * @param changesStartLineNum the index of the line in the given <code>diffLines</code> that
     * marks the starting point of the change details in terms of added and removed lines
     */
    public ModelFileDiff(String[] diffLines, int changesStartLineNum) {
        super(FileType.MODEL, diffLines, changesStartLineNum);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected String normalize(String diffLine, int diffLinePosition) {
        // 1. Remove "+" or "-"
        String normalizedDiffLine = diffLine;
        /*
         * This method is also used by isVariabilityChange-method and
         * may receive unchanged diff lines (no leading "+" or "-") from
         * there. Thus, check before removing the first character although
         * this is not needed if called from parent-class. 
         */
        if (diffLine.startsWith(LINE_ADDED_MARKER) || diffLine.startsWith(LINE_DELETED_MARKER)) {
            normalizedDiffLine = diffLine.substring(1, diffLine.length());
        }
        // 2. Split around comment-token
        String[] splittedNormalizedDiffLine = normalizedDiffLine.split(MODEL_COMMENT_MARKER);
        if (splittedNormalizedDiffLine.length > 0) {
            normalizedDiffLine = splittedNormalizedDiffLine[0];
        } else {
            normalizedDiffLine = "";
        }
        // 3. Return part before comment token (index 0)
        return normalizedDiffLine;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isVariabilityChange(String cleanDiffLine, int cleanDiffLinePosition) {
        /*
         * Clean diff line means, that there is no leading "+" or "-" anymore and
         * comments that were part of this line are removed (only the part before
         * the comment is passed).
         */
        boolean isVariabilityChange = false;
        if (!isPartOfHelp(cleanDiffLine, cleanDiffLinePosition)) {
            if (Pattern.matches(MODEL_CONFIG_DEF_PATTERN, cleanDiffLine) 
                    || Pattern.matches(MODEL_FILE_INCLUDE_PATTERN, cleanDiffLine)) {
                isVariabilityChange = true;
            } else if (Pattern.matches(MODEL_DEPENDS_ON_PATTERN, cleanDiffLine)) {
                /*
                 * "depends on"-statements can also be defined for comments.
                 * Thus, we need to check the previous diff line(s) for containing
                 * variability information. If this is not the case, it must be
                 * a comment and "false" will be returned.
                 */
                int diffLineCounter = cleanDiffLinePosition - 1;
                boolean previousModelElementFound = false;
                String previousDiffLine = null;
                while (diffLineCounter >= 0 && !previousModelElementFound) {
                    previousDiffLine = normalize(diffLines[diffLineCounter], diffLineCounter);
                    if (Pattern.matches(MODEL_CONFIG_COMMENT_PATTERN, previousDiffLine)) {
                        /*
                         * Comment-statement found, thus only model element found but
                         * changed "depend on" is not a variability change. 
                         */
                        previousModelElementFound = true;
                    } else if (isVariabilityChange(previousDiffLine, diffLineCounter)) {
                        /*
                         * Current line includes variability information, thus model
                         * element found and changed "depend on" is a variability change. 
                         */
                        previousModelElementFound = true;
                        isVariabilityChange = true;
                    }
                    diffLineCounter--;
                }
            }
        }
        return isVariabilityChange;
    }
    
    /**
     * Check if the given diff line belongs to a help text defined by, e.g., "help" or "--help--".
     * 
     * @param diffLine the line of a diff to be checked for being part of a help text
     * @param diffLinePosition the index of the given diff line in {@link #diffLines}
     * @return <code>true</code> if the given diff line belongs to a help text, <code>false</code> otherwise
     */
    private boolean isPartOfHelp(String diffLine, int diffLinePosition) {
        boolean isPartOfHelp = false;
        boolean parentElementFound = false;
        int diffLineIndentation = getIndentation(diffLine);
        if (diffLineIndentation > 0) {
            int diffLineCounter = diffLinePosition - 1;
            String previousDiffLine = null;
            do {
                previousDiffLine = normalize(diffLines[diffLineCounter], diffLineCounter);
                if (!previousDiffLine.isEmpty()) {
                    int previousDiffLineIndentation = getIndentation(previousDiffLine);
                    if (previousDiffLineIndentation < diffLineIndentation) {
                        parentElementFound = true;
                        previousDiffLine = previousDiffLine.trim();
                        if (previousDiffLine.startsWith("help") 
                                || previousDiffLine.startsWith("--help--")
                                || previousDiffLine.startsWith("comment")) {
                            isPartOfHelp = true;
                        }
                    }
                }
                diffLineCounter--;
            } while (diffLineCounter >= 0 && !parentElementFound);
        }
        return isPartOfHelp;
    }
    
    /**
     * Count the number of whitespace before the first non-whitespace character in the given diff line.
     *  
     * @param diffLine the line of a diff for which the leading whitespace should be counted
     * @return the number of leading whitespace in the given diff line; returns <code>0</code> if no
     * whitespace was found
     */
    private int getIndentation(String diffLine) {
        int indentation = 0;
        while (indentation < diffLine.length() && Character.isWhitespace(diffLine.charAt(indentation))) {
            indentation++;
        }
        return indentation;
    }
}
