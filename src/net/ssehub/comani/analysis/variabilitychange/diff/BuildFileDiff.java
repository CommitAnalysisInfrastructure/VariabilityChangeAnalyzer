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
 * This class implements analysis methods and required attributes for counting the changes made to a build file
 * (Make).<br><br>
 * 
 * In particular, this class differentiates between changed lines containing general build information and
 * lines that contain variability information, e.g. conditional build targets.<br><br>
 * 
 * A changed line is counted as variability change if that particular line contains a reference to a
 * Kconfig-symbol, e.g. "$CONFIG_X". Concatenations will not be counted as variability change, for example:<br><br>
 * 
 * dtb-$(CONFIG_MACH_KIRKWOOD) += \ <br>
 *   kirkwood-b3.dtb \ <br>
 * +  kirkwood-blackarmor-nas220.dtb<br><br>
 * 
 * The last line is a general change to a build file but is not counted as variability change.<br><br>
 * 
 * Further elements of conditional statements like:<br><br>
 * 
 * ifeq ($(CONFIG_PAYLOAD_ELF),y) <br>
 *    ... <br>
 * -else <br>
 *     ... <br>
 * -endif <br><br>
 * 
 * are counted as variability change (here "else" and "endif") as they belong to a condition that
 * references a Kconfig-symbol.
 * 
 * @author Christian Kroeher
 *
 */
public class BuildFileDiff extends FileDiff {
    
    /**
     * String identifying the start of a comment in a build file.
     */
    private static final String BUILD_COMMENT_MARKER = "#";
    
    /**
     * Regex identifying lines that contain variability information in a build file.<br><br>
     * 
     * Value: {@value #BUILD_VAR_PATTERN};
     */
    private static final String BUILD_VAR_PATTERN = ".*\\$\\(CONFIG_.*";
    
    /**
     * Regex identifying lines that contain the start of a conditional block.<br><br>
     * 
     * Value: {@value #BUILD_CONDITION_START_PATTERN};
     */
    private static final String BUILD_CONDITION_START_PATTERN = ".*(ifeq|ifneq|ifdef|ifndef).*";
    
    /**
     * String identifying the end of an entire conditional block.
     */
    private static final String BUILD_CONDITION_END_MARKER = "endif";
    
    /**
     * Regex identifying lines that contain the end of an entire conditional block.<br><br>
     * 
     * Value: {@value #BUILD_CONDITION_END_PATTERN};
     */
    private static final String BUILD_CONDITION_END_PATTERN = ".*" + BUILD_CONDITION_END_MARKER + ".*";
    
    /**
     * Regex identifying lines that contain the end of a conditional block (either a part or the entire block).<br><br>
     * 
     * Value: {@value #BUILD_CONDITION_BLOCK_END_PATTERN};
     */
    private static final String BUILD_CONDITION_BLOCK_END_PATTERN = ".*(else|" + BUILD_CONDITION_END_MARKER + ").*";
    
    /**
     * Construct a new {@link BuildFileDiff}.<br><br>
     * 
     * This constructor will call the super constructor of {@link FileDiff}, which will start a
     * line-wise analysis of the given diff lines calling the inherited methods {@link #normalize(String, int)} and
     * {@link #isVariabilityChange(String, int)} defined in this class.<br>
     * Counting the number of changed lines (regardless of variability information) is done in {@link FileDiff}.
     * 
     * @param diffLines the lines of a build diff
     * @param changesStartLineNum the index of the line in the given <code>diffLines</code> that
     * marks the starting point of the change details in terms of added and removed lines
     */
    public BuildFileDiff(String[] diffLines, int changesStartLineNum) {
        super(FileType.BUILD, diffLines, changesStartLineNum);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected String normalize(String diffLine, int diffLinePosition) {
        // 1. Remove "+" or "-"
        String normalizedDiffLine = diffLine;
        /*
         * This method is also used by backtrackCondition-method and
         * may receive unchanged diff lines (no leading "+" or "-") from
         * there. Thus, check before removing the first character although
         * this is not needed if called from parent-class. 
         */
        if (diffLine.startsWith(LINE_ADDED_MARKER) || diffLine.startsWith(LINE_DELETED_MARKER)) {
            normalizedDiffLine = diffLine.substring(1, diffLine.length());
        }
        // 2. Split around comment-token
        String[] normalizedDiffLineParts = normalizedDiffLine.split(BUILD_COMMENT_MARKER);
        if (normalizedDiffLineParts.length > 0) {
            normalizedDiffLine = normalizedDiffLineParts[0];
        } else {
            normalizedDiffLine = "";
        }
        // 3. Check if is part of comment
        if (!normalizedDiffLine.trim().isEmpty() && isPartOfComment(diffLinePosition)) {
            normalizedDiffLine = "";
        }
        // 4. Return part of the diff line without comments
        return normalizedDiffLine;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isVariabilityChange(String cleanDiffLine, int cleanDiffLinePosition) {
        /*
         * Clean diff line means, that there is no leading "+" or "-" anymore and
         * comments that were part of this line are removed. In case of a multi line
         * comment, the inner part if this comment needs additional checks here.
         */
        boolean isVariabilityChange = false;
        if (!isPartOfComment(cleanDiffLinePosition)
                && (Pattern.matches(BUILD_VAR_PATTERN, cleanDiffLine) 
                        || (Pattern.matches(BUILD_CONDITION_BLOCK_END_PATTERN, cleanDiffLine)
                                && backtrackCondition(cleanDiffLinePosition)))) {
            isVariabilityChange = true;
        }
        return isVariabilityChange;
    }
    
    /**
     * Check if the diff line at the given position is part of a multi line comment
     * concatenated by continuation "\".
     * 
     * @param diffLinePosition the index of the diff line in {@link #diffLines} to be checked for being
     * part of a multi line comment
     * @return <code>true</code> if the diff line at the given position is part of a multi
     * line comment, <code>false</code> otherwise
     */
    private boolean isPartOfComment(int diffLinePosition) {
        boolean isPartOfComment = false;
        boolean parentElementFound = false;
        int diffLineCounter = diffLinePosition - 1;
        String previousDiffLine = null;
        while (diffLineCounter >= 0 && !parentElementFound) {
            /*
             * Do not normalize diff line here as we need to find the leading "#"
             * to check if diffLine is part of comment; normalize would return the
             * part before "#" and, thus, we will never find the start of a comment
             * 
             * Further, leading "+" or "-" can be ignored here, as we only need to
             * follow the trailing "\".
             */
            previousDiffLine = diffLines[diffLineCounter];
            if (!previousDiffLine.isEmpty()) {
                previousDiffLine = previousDiffLine.trim();
                if (previousDiffLine.endsWith("\\")) {
                    if (previousDiffLine.contains(BUILD_COMMENT_MARKER)) {
                        /*
                         * We found the start of a comment with a trailing
                         * continuation, thus the given diff line must be
                         * part of a multi line comment. 
                         */
                        parentElementFound = true;
                        isPartOfComment = true;
                    }
                } else {
                    /*
                     * If previous line does not end with continuation, there
                     * is no multi line comment.
                     */
                    parentElementFound = true;
                }
            } else {
                /*
                 * If the previous line is empty, there is no multi line comment
                 * as an empty line would break the continuation.
                 */
                parentElementFound = true;
            }
            diffLineCounter--;
        }
        return isPartOfComment;
    }
    
    /**
     * Find the condition for the "endif" or "else" statement at the given index of the diff
     * lines and return <code>true</code> if this condition is variability related, which should
     * lead to an increment of the respective variability lines counter (added or deleted).
     *  
     * @param blockEndIndex the index of the diff line where the "endif" or "else" statement was found
     * @return <code>true</code> if the found condition is variability related. <code>false</code> otherwise.
     */
    private boolean backtrackCondition(int blockEndIndex) {
        boolean conditionIsVariabilityRelated = false;
        boolean conditionFound = false;
        int nestedEndifCounter = 0; // Counts the nested #endif-statements
        int diffLinesCounter = blockEndIndex - 1;
        String diffLine = null;
        while (!conditionFound && diffLinesCounter >= 0) {
            diffLine = normalize(diffLines[diffLinesCounter], diffLinesCounter);
            if (nestedEndifCounter == 0 && !Pattern.matches(BUILD_CONDITION_BLOCK_END_PATTERN, diffLine)
                    && Pattern.matches(BUILD_CONDITION_START_PATTERN, diffLine)) {
                /*
                 * No nested blocks and not an "endif" or "else" and line indicates block start:
                 *      Either this line directly matches BUILD_VAR_PATTERN
                 *      or expression continuation is used and the following line(s) match BUILD_VAR_PATTERN
                 * 
                 * --> block end change is variability change
                 */
                conditionFound = true;
                if (Pattern.matches(BUILD_VAR_PATTERN, diffLine)) {
                    conditionIsVariabilityRelated = true;
                } else if (diffLine.trim().endsWith("\\")) {
                    // Expression continuation for an "if"-statement, thus, check the next line(s)
                    int blockLinesCounter = diffLinesCounter + 1;
                    String blockLine = "";
                    do {
                        blockLine = normalize(diffLines[blockLinesCounter], diffLinesCounter);
                        if (Pattern.matches(BUILD_VAR_PATTERN, blockLine)) {
                            conditionIsVariabilityRelated = true;
                        }
                        blockLinesCounter++;
                    } while (blockLinesCounter < blockEndIndex
                            && blockLine.trim().endsWith("\\")
                            && !Pattern.matches(BUILD_CONDITION_START_PATTERN, blockLine));
                }
            } else {
                if (Pattern.matches(BUILD_CONDITION_END_PATTERN, diffLine)) {
                    // Nested block end found
                    nestedEndifCounter++;
                } else if (Pattern.matches(BUILD_CONDITION_START_PATTERN, diffLine)) {
                    // Nested block start found
                    nestedEndifCounter--;
                }
            }
            diffLinesCounter--;
        }
        return conditionIsVariabilityRelated;
    }
}
