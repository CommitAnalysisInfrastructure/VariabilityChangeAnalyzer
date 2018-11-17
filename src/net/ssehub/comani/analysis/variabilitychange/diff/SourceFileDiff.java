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
 * This class implements analysis methods and required attributes for counting the changes made to a source file
 * (C-family).<br><br>
 * 
 * In particular, this class differentiates between changed lines containing general source code 
 * and lines that contain variability information, e.g. ifdef-statements in combination with configuration symbols.
 * <br><br>
 * 
 * A changed line is counted as variability change if that particular line contains a reference to a
 * Kconfig-symbol, e.g. "$CONFIG_X". Concatenations will not be counted as variability change, for example:<br><br>
 * 
 * #if IS_ENABLED(CONFIG_X) \ <br>
 * +    someInt != 5 <br><br>
 * 
 * The last line is a general change to a source file but is not counted as variability change.<br><br>
 * 
 * Further elements of conditional statements like:<br><br>
 * 
 * if (IS_ENABLED(CONFIG_X)) <br>
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
public class SourceFileDiff extends FileDiff {
    
    /**
     * String identifying the start of a single line comment in a source file.
     */
    private static final String SOURCE_SL_COMMENT_MARKER = "//";
    
    /**
     * String identifying the start of a multi line comment in a source file.
     */
    private static final String SOURCE_ML_COMMENT_START_MARKER = "/*";
    
    /**
     * Regex identifying the start of a multi line comment in a source file.<br><br>
     * 
     * Value: {@value #SOURCE_ML_COMMENT_START_PATTERN};
     */
    private static final String SOURCE_ML_COMMENT_START_PATTERN = "/\\*";
    
    /**
     * String identifying the end of a multi line comment in a source file.
     */
    private static final String SOURCE_ML_COMMENT_END_MARKER = "*/";
    
    /**
     * Regex identifying the end of a multi line comment in a source file.<br><br>
     * 
     * Value: {@value #SOURCE_ML_COMMENT_END_PATTERN};
     */
    private static final String SOURCE_ML_COMMENT_END_PATTERN = "\\*/";
    
    /**
     * Regex identifying lines that contain a reference to a model symbol "CONFIG_" in a source code file.<br><br>
     * 
     * Value: {@value #SOURCE_VAR_PATTERN};
     */
    private static final String SOURCE_VAR_PATTERN = ".*(\\s+|\\(|\\[|\\{|\\<|\\)|\\]|\\}|\\>)\\!?CONFIG_.*";
    
    /**
     * Regex identifying lines that contain "#ifdef" or "#ifndef" statements in a source code file
     * and refer to a Kconfig symbol.<br><br>
     * 
     * Value: {@value #SOURCE_VAR_IFDEF_PATTERN};
     */
    private static final String SOURCE_VAR_IFDEF_PATTERN = "#\\s*(ifdef|ifndef)" + SOURCE_VAR_PATTERN;
    
    /**
     * Regex identifying lines that contain help methods (macros) in a source code file.<br><br>
     * 
     * Value: {@value #SOURCE_VAR_HELP_METHODS_PATTERN};
     */
    private static final String SOURCE_VAR_HELP_METHODS_PATTERN = "\\!?(defined|IS_BUILTIN|"
            + "IS_MODULE|IS_REACHABLE|IS_ENABLED)\\(";
    
    /**
     * Regex identifying lines that contain conditions with help methods (macros) in a source code file
     * and refer to a Kconfig symbol.<br><br>
     * 
     * Value: {@value #SOURCE_VAR_IF_CONDITION_PATTERN};
     */
    private static final String SOURCE_VAR_IF_CONDITION_PATTERN = "\\!?.*(" + SOURCE_VAR_HELP_METHODS_PATTERN + ")?"
            + SOURCE_VAR_PATTERN;
    
    /**
     * Regex identifying lines that contain "#if" or "#elif" statements in a source code file
     * and refer to help methods (macros), which in turn refer to a Kconfig symbol.<br><br>
     * 
     * Value: {@value #SOURCE_VAR_IF_PATTERN};
     */
    private static final String SOURCE_VAR_IF_PATTERN = "#\\s*(if|elif).*" + SOURCE_VAR_IF_CONDITION_PATTERN;
    
    /**
     * Regex identifying lines that contain variability information in a source code file and
     * indicate the start of a conditional block.<br><br>
     * 
     * Value: {@value #SOURCE_VAR_IF_START_PATTERN};
     */
    private static final String SOURCE_VAR_IF_START_PATTERN = ".*((" + SOURCE_VAR_IFDEF_PATTERN + ")"
            + "|(" + SOURCE_VAR_IF_PATTERN + "))";
    
    /**
     * Regex identifying lines that indicate the end of a conditional block in a source code file. If these
     * lines can be interpreted as variability related depends on the corresponding start of the block (the
     * condition): see {@link #isVariabilityChange(String, int)}.<br><br>
     * 
     * Value: {@value #SOURCE_VAR_IF_END_PATTERN};
     */
    private static final String SOURCE_VAR_IF_END_PATTERN = ".*#\\s*(else|endif).*";
    
    /**
     * Construct a new {@link SourceFileDiff}.<br><br>
     * 
     * This constructor will call the super constructor of {@link FileDiff}, which will start a
     * line-wise analysis of the given diff lines calling the inherited methods {@link #normalize(String, int)} and
     * {@link #isVariabilityChange(String, int)} defined in this class.<br>
     * Counting the number of changed lines (regardless of variability information) is done in {@link FileDiff}.
     * 
     * @param diffLines the lines of a source code diff
     * @param changesStartLineNum the index of the line in the given <code>diffLines</code> that
     * marks the starting point of the change details in terms of added and removed lines
     */
    public SourceFileDiff(String[] diffLines, int changesStartLineNum) {
        super(FileType.SOURCE, diffLines, changesStartLineNum);
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
        if (normalizedDiffLine.contains(SOURCE_SL_COMMENT_MARKER)) {
            // Single line comment included, thus only use the part before the comment.
            String[] normalizedDiffLineParts = normalizedDiffLine.split(SOURCE_SL_COMMENT_MARKER);
            if (normalizedDiffLineParts.length > 0) {
                normalizedDiffLine = normalizedDiffLineParts[0];
            } else {
                normalizedDiffLine = "";
            }
        } else if (normalizedDiffLine.contains(SOURCE_ML_COMMENT_START_MARKER)) {
            if (normalizedDiffLine.contains(SOURCE_ML_COMMENT_END_MARKER)) {
                /*
                 * Multi line comment start and end in single line, thus the part before the start and the part after
                 * the end of the comment should be used for further analysis.
                 */
                String beforeCommentStart = "";
                String afterCommentEnd = "";
                String[] normalizedDiffLineParts = normalizedDiffLine.split(SOURCE_ML_COMMENT_START_PATTERN);
                if (normalizedDiffLineParts.length > 0) {
                    beforeCommentStart = normalizedDiffLineParts[0];
                }
                normalizedDiffLineParts = normalizedDiffLine.split(SOURCE_ML_COMMENT_END_PATTERN);
                if (normalizedDiffLineParts.length > 1) {
                    afterCommentEnd = normalizedDiffLineParts[1];
                }
                normalizedDiffLine = beforeCommentStart + " " + afterCommentEnd;
            } else {
                // Multi line comment start only in this line, thus only use the part before the start.
                String[] normalizedDiffLineParts = normalizedDiffLine.split(SOURCE_ML_COMMENT_START_PATTERN);
                if (normalizedDiffLineParts.length > 0) {
                    normalizedDiffLine = normalizedDiffLineParts[0];
                } else {
                    normalizedDiffLine = "";
                }
            }
        } else if (normalizedDiffLine.contains(SOURCE_ML_COMMENT_END_MARKER)) {
            // Multi line comment end included, thus only use the part after the end of the comment (if available)
            String[] normalizedDiffLineParts = normalizedDiffLine.split(SOURCE_ML_COMMENT_END_PATTERN);
            if (normalizedDiffLineParts.length > 1) {
                normalizedDiffLine = normalizedDiffLineParts[1];
            } else {
                normalizedDiffLine = "";
            }
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
         * See isPartOfComment-method.
         */
        boolean isVariabilityChange = false;
        if (!isPartOfComment(cleanDiffLinePosition)) {
            if (Pattern.matches(SOURCE_VAR_PATTERN, cleanDiffLine)) {
                isVariabilityChange = true;
            } else if ((Pattern.matches(SOURCE_VAR_IF_END_PATTERN, cleanDiffLine)
                    && backtrackPreprocessorCondition(cleanDiffLinePosition)) 
                    || (cleanDiffLine.contains("}") && backtrackRuntimeCondition(cleanDiffLinePosition))) {
                isVariabilityChange = true;
            }
        }
        return isVariabilityChange;
    }
    
    /**
     * Check if the diff line at the given position is part of a multi line comment.
     * 
     * @param diffLinePosition the index of the diff line which should be checked for being
     * part of a multi line comment
     * @return <code>true</code> if the diff line at the given index is part of a multi line
     * comment, <code>false</code> otherwise
     */
    private boolean isPartOfComment(int diffLinePosition) {
        boolean isPartOfComment = false;
        int diffLineCounter = diffLinePosition - 1;
        boolean commentMarkerFound = false;
        String previousDiffLine = null;
        while (diffLineCounter >= 0 && !commentMarkerFound) {
            previousDiffLine = diffLines[diffLineCounter];
            if (previousDiffLine.contains(SOURCE_ML_COMMENT_END_MARKER)) {
                /*
                 * Closing multi line found, thus the diff line at the given position
                 * cannot be part of a multi line comment.
                 */
                commentMarkerFound = true;
            } else if (previousDiffLine.contains(SOURCE_ML_COMMENT_START_MARKER)) {
                /*
                 * Opening multi line found, thus the diff line at the given position
                 * must be part of a multi line comment. 
                 */
                commentMarkerFound = true;
                isPartOfComment = true;
            }
            diffLineCounter--;
        }
        return isPartOfComment;
    }
    
    /**
     * Find the condition for the "#endif" or "#else" statement at the given index of the diff
     * lines and return <code>true</code> if this condition is variability related, which should
     * lead to an increment of the respective variability lines counter (added or deleted).
     *  
     * @param blockEndIndex the index of the diff line where the "#endif" or "#else" statement was found
     * @return <code>true</code> if the found condition is variability related, <code>false</code> otherwise
     */
    private boolean backtrackPreprocessorCondition(int blockEndIndex) {
        /*
         * Determine the change type of the block end ('+' or '-') and the inverted type for checking
         * whether possible previous block end statements are part of a nested block or if the given block end
         * substitutes the previous block end statement, e.g. like in this case:
         *
         * #if !CONFIG_X
         *     ...
         * #else
         *     ...
         * -#endif
         *     ...
         * +#endif
         */
        String blockEndLine = diffLines[blockEndIndex];
        char blockEndChangeType = blockEndLine.charAt(0);
        char invertedblockEndChangeType = invertChangeType(blockEndChangeType);
        boolean conditionIsVariabilityRelated = false;
        boolean conditionFound = false;
        int nestedEndifCounter = 0; // Counts the nested #endif-statements
        int diffLinesCounter = blockEndIndex - 1;
        String diffLine = null;
        while (!conditionFound && diffLinesCounter >= 0) {
            diffLine = diffLines[diffLinesCounter]; // normalize(diffLines[diffLinesCounter])
            if (nestedEndifCounter == 0 && !Pattern.matches(SOURCE_VAR_IF_END_PATTERN, diffLine)
                    && Pattern.matches(".*#if.*", diffLine)) {
                // No nested blocks and not an #endif or #else and line indicates block start
                conditionFound = true;
                if (Pattern.matches(SOURCE_VAR_IF_START_PATTERN, diffLine)) {
                    // Current diff line contains variability information
                    conditionIsVariabilityRelated = true;
                } else if (diffLine.trim().endsWith("\\")) {
                    /*
                     * Current diffLine contains #if-statement but does not include a CONFIG_ symbol.
                     * If this line ends with continuation ("\"), we have to check the following lines until
                     * there is no continuation anymore for CONFIG_ symbols.
                     */
                    int blockLinesCounter = diffLinesCounter + 1;
                    String blockLine = "";
                    do {
                        blockLine = normalize(diffLines[blockLinesCounter], blockLinesCounter);
                        if (Pattern.matches(SOURCE_VAR_PATTERN, blockLine)) {
                            conditionIsVariabilityRelated = true;
                        }
                        blockLinesCounter++;
                    } while (blockLinesCounter < blockEndIndex && blockLine.trim().endsWith("\\"));
                }
            } else {
                if (!diffLine.isEmpty() && diffLine.charAt(0) != invertedblockEndChangeType
                        && Pattern.matches(".*#endif.*", diffLine)) {
                    // Nested block end found
                    nestedEndifCounter++;
                } else if (nestedEndifCounter > 0 && Pattern.matches(".*#if.*", diffLine)) {
                    // Nested block start found
                    nestedEndifCounter--;
                }
            }
            diffLinesCounter--;
        }
        return conditionIsVariabilityRelated;
    }
    
    /**
     * Find the possible condition for a closing curly bracket "}" at the given index of the diff
     * lines and return <code>true</code> if this condition is variability related, which should
     * lead to an increment of the respective variability lines counter (added or deleted).
     *  
     * @param blockEndIndex the index of the diff line where the closing curly bracket "}" was found
     * @return <code>true</code> if the found condition is variability related, <code>false</code> otherwise
     */
    private boolean backtrackRuntimeCondition(int blockEndIndex) {
        /*
         * Determine the change type of the block end ('+' or '-') and the inverted type for checking
         * whether possible previous block end statements are part of a nested block or if the given block end
         * substitutes the previous block end statement, e.g. like in this case:
         * 
         * if (CONFIG_SYMBOL) {
         *     ...
         * -}
         *     ...
         * +}
         */
        String blockEndLine = diffLines[blockEndIndex];
        char blockEndChangeType = blockEndLine.charAt(0);
        char invertedblockEndChangeType = invertChangeType(blockEndChangeType);
        boolean conditionIsVariabilityRelated = false;
        boolean conditionFound = false;
        int nestedBlocksCounter = 0; // Counts the nested blocks like if... or loops
        int diffLinesCounter = blockEndIndex - 1;
        String diffLine = null;
        while (!conditionFound && diffLinesCounter >= 0) {
            diffLine = diffLines[diffLinesCounter]; // normalize(diffLines[diffLinesCounter])
            if (nestedBlocksCounter == 0 && diffLine.contains("{")
                    && !Pattern.matches(".*(\\}\\s*else\\s*\\{).*", diffLine)) {
                // No nested blocks and not an else-statement indicates block start
                conditionFound = true;
                String[] diffLineParts = diffLine.split("\\{");
                if (diffLineParts.length >= 1 && Pattern.matches(SOURCE_VAR_PATTERN, diffLineParts[0])) {
                    // Current line really includes the condition and there is a reference to a model symbol
                    conditionIsVariabilityRelated = true;
                }
                if (diffLineParts.length == 0) {
                    /*
                     * Current line only contains the opening curly bracket, thus, check the lines before
                     * for the corresponding condition. Do this check until no unclosed brackets ")" are
                     * found to cover multi line statements like
                     * 
                     *  if (x == 0
                     *     && CONFIG_Y == 1)
                     *  { ...
                     * 
                     * and to reject statements like
                     * 
                     *  struct name
                     *  { ...
                     */
                    int blockLinesCounter = diffLinesCounter - 1;
                    String blockLine = "";
                    do {
                        blockLine = normalize(diffLines[blockLinesCounter], blockLinesCounter);
                        if (Pattern.matches(SOURCE_VAR_PATTERN, blockLine)) {
                            conditionIsVariabilityRelated = true;
                        }
                        blockLinesCounter--;
                    } while (blockLinesCounter >= 0 && hasUnclosedBrackets(blockLine));
                }
            } else {
                if (!diffLine.isEmpty() && diffLine.charAt(0) != invertedblockEndChangeType && diffLine.contains("}")) {
                    // Nested block end found
                    nestedBlocksCounter++;
                }
                if (nestedBlocksCounter > 0 && diffLine.contains("{")) {
                    // Nested block start found
                    nestedBlocksCounter--;
                }
            }
            diffLinesCounter--;
        }
        return conditionIsVariabilityRelated;
    }
    
    /**
     * Invert the given change type ('+' or '-').
     * 
     * @param changeType the character defining the change type ('+' or '-')
     * @return if the given change type is '+' then '-'; '+' in all other cases
     */
    private char invertChangeType(char changeType) {
        char invertedChangeType = '+';
        if (changeType == '+') {
            invertedChangeType = '-';
        }
        return invertedChangeType;
    }
    
    /**
     * Count the number of brackets "(" and ")" and return <code>true</code> if the difference of
     * opening and closing brackets is zero, meaning that there are no unclosed brackets in the
     * given line.
     *  
     * @param sourceCodeLine the {@link String} in which the number of brackets should be counted
     * @return <code>true</code> if the difference of opening and closing brackets is zero,
     * <code>false</code> otherwise
     */
    private boolean hasUnclosedBrackets(String sourceCodeLine) {
        boolean hasUnclosedBrackets = false;
        int bracketsCounter = 0;
        for (int i = 0; i < sourceCodeLine.length(); i++) {
            if (sourceCodeLine.charAt(i) == '(') {
                bracketsCounter++;
            } else if (sourceCodeLine.charAt(i) == ')') {
                bracketsCounter--;
            }
        }
        if (bracketsCounter != 0) {
            hasUnclosedBrackets = true;
        }
        return hasUnclosedBrackets;
    }
}
