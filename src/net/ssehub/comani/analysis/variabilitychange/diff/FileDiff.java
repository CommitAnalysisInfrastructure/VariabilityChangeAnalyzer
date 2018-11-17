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

/**
 * This abstract class represents a general file diff. It implements common attributes and methods for the following
 * sub-classes:<br>
 * <ul>
 * <li>{@link ModelFileDiff}</li>
 * <li>{@link BuildFileDiff}</li>
 * <li>{@link SourceFileDiff}</li>
 * <li>{@link OtherFileDiff}</li>
 * </ul>
 * 
 * @author Christian Kroeher
 *
 */
public abstract class FileDiff {
    
    /**
     * Enumeration for defining the type of file diff for a specific object. This eases the
     * identification of the type of file diff during the calculation of all changed lines
     * among a given commit.
     * 
     * @author Christian Kroeher
     *
     */
    public enum FileType { MODEL, BUILD, SOURCE, OTHER };
    
    /**
     * String identifying an added line in a diff text, if found at the start of the line.
     */
    protected static final String LINE_ADDED_MARKER = "+";
    
    /**
     * String identifying a deleted line in a diff text, if found at the start of the line.
     */
    protected static final String LINE_DELETED_MARKER = "-";
    
    /**
     * The {@link FileType} of this file diff.
     */
    protected FileType fileType;
    
    /**
     * The full, line-wise diff description of a commit.
     */
    protected String[] diffLines;
    
    /**
     * The line number at which the first change of a commit is described. This typically is
     * the first occurrence of a line starting with "@@".
     */
    protected int changesStartLine;
    
    /**
     * The number of lines added by a commit.<br><br>
     * 
     * This number includes {@link #addedVarLinesNum}.
     */
    private int addedLinesNum;
    
    /**
     * The number of lines deleted by a commit.<br><br>
     * 
     * This number includes {@link #deletedVarLinesNum}.
     */
    private int deletedLinesNum;
    
    /**
     * The number of lines containing variability information added by a commit.
     */
    private int addedVarLinesNum;
    
    /**
     * The number of lines containing variability information deleted by a commit.
     */
    private int deletedVarLinesNum;
    
    /**
     * Construct a new {@link FileDiff}.<br><br>
     * 
     * This constructor will start a line-wise analysis of the given diff lines calling the abstract
     * method {@link #isVariabilityChange(String, int)}. This method is implemented in the specific file diff
     * classes to detect changes to the variability information available in the specific type of file diff.
     * 
     * @param fileType the {@link FileType} of this file diff
     * @param diffLines the full, line-wise diff description of a commit
     * @param changesStartLineNum the line number at which the first change of a commit is described (typically
     * the first occurrence of a line staring with "@@")
     */
    protected FileDiff(FileType fileType, String[] diffLines, int changesStartLineNum) {
        this.fileType = fileType;
        this.diffLines = diffLines;
        this.changesStartLine = changesStartLineNum;
        
        this.addedLinesNum = 0;
        this.deletedLinesNum = 0;
        this.addedVarLinesNum = 0;
        this.deletedVarLinesNum = 0;
        
        analyzeDiff();
    }
    
    /**
     * Analyze the given {@link #diffLines} by counting the general lines added or removed by a commit as well
     * as counting the lines containing variability information added or removed by the commit.
     */
    private void analyzeDiff() {
        String diffLine = null;
        String diffLineNoMarker = null;
        for (int i = changesStartLine; i < diffLines.length; i++) {
            diffLine = diffLines[i];
            if (diffLine.startsWith(LINE_ADDED_MARKER)) {
                // Up-front check if the added line is an empty line (do not count such lines)
                diffLineNoMarker = diffLine.substring(1, diffLine.length());
                if (!diffLineNoMarker.trim().isEmpty()) {
                    // Now, actually delete the marker and additional comments from that line
                    diffLine = normalize(diffLine, i);
                    if (!diffLine.trim().isEmpty()) {
                        if (isVariabilityChange(diffLine, i)) {
                            addedVarLinesNum++;
                        } else {
                            addedLinesNum++;
                        }
                    } // else {
                        // Not counted as artifact-specific information anymore!
                        /*
                         * Line is empty after normalization. Happens if line only contains a
                         * comment. No further checks needed, but this line has to be counted
                         * as a general change.
                         */
                        //addedLinesNum++;
                    //}
                }
            } else if (diffLine.startsWith(LINE_DELETED_MARKER)) {
                // Up-front check if the added line is an empty line (do not count such lines)
                diffLineNoMarker = diffLine.substring(1, diffLine.length());
                if (!diffLineNoMarker.trim().isEmpty()) {
                    // Now, actually delete the marker and additional comments from that line
                    diffLine = normalize(diffLine, i);
                    if (!diffLine.trim().isEmpty()) {
                        if (isVariabilityChange(diffLine, i)) {
                            deletedVarLinesNum++;
                        } else {
                            deletedLinesNum++;
                        }
                    } //else {
                        // Not counted as artifact-specific information anymore!
                        /*
                         * Line is empty after normalization. Happens if line only contains a
                         * comment. No further checks needed, but this line has to be counted
                         * as a general change.
                         */
                        //deletedLinesNum++;
                    //}
                }
            }
        }
    }
    
    /**
     * Normalize the given diff line, e.g. remove leading "+" or "-", check if the line is a general comment or
     * contains general comments as part of the line.
     * 
     * @param diffLine the line of a diff description to be normalized
     * @param diffLinePosition the index of the given diff line in {@link #diffLines} used, e.g., for backtracking
     * @return the normalized diff line without leading "+" or "-"; in case of comments, only the non-comment part
     * will be return, which may lead to an empty string
     */
    protected abstract String normalize(String diffLine, int diffLinePosition);
    
    /**
     * Check if the given diff line (without leading "+" or "-") describes a change to variability information.
     * The result of this check depends on the possible variability definitions in the specific file type and, thus,
     * has to be implemented in each class derived from this class (see {@link FileDiff}.<br><br>
     * 
     * <b>Please note</b> that returning <code>true</code> results in increasing the added or removed variability line
     * number (counter).
     *  
     * @param cleanDiffLine the line of a diff description to be checked for variability information without leading "+"
     * or "-"
     * @param cleanDiffLinePosition the index of the given diff line in {@link #diffLines} used, e.g., for backtracking 
     * @return <code>true</code> if the given diff line contains variability information, <code>false</code> otherwise
     */
    protected abstract boolean isVariabilityChange(String cleanDiffLine, int cleanDiffLinePosition);
    
    /**
     * Return the {@link FileType} of this file diff.
     * 
     * @return the {@link FileType} of this file diff
     */
    public FileType getFileType() {
        return this.fileType;
    }
    
    /**
     * Return the full, line-wise diff description analyzed by this file diff.
     * 
     * @return the full, line-wise diff description analyzed by this file diff
     */
    public String[] getDiffText() {
        return this.diffLines;
    }
    
    /**
     * Return the number of lines added by the given commit (diff lines).<br><br>
     * 
     * This number includes {@link #addedVarLinesNum}.
     * 
     * @return the number of lines added by the given commit (diff lines)
     */
    public int getAddedLinesNum() {
        return this.addedLinesNum;
    }
    
    /**
     * Return the number of lines deleted by the given commit (diff lines).<br><br>
     * 
     * This number includes {@link #deletedVarLinesNum}.
     * 
     * @return the number of lines deleted by the given commit (diff lines)
     */
    public int getDeletedLinesNum() {
        return this.deletedLinesNum;
    }
    
    /**
     * Return the number of lines containing variability information added by
     * the given commit (diff lines).
     * 
     * @return the number of lines containing variability information added by
     * the given commit (diff lines)
     */
    public int getAddedVarLinesNum() {
        return this.addedVarLinesNum;
    }
    
    /**
     * Return the number of lines containing variability information deleted by
     * the given commit (diff lines).
     * 
     * @return the number of lines containing variability information deleted by
     * the given commit (diff lines)
     */
    public int getDeletedVarLinesNum() {
        return this.deletedVarLinesNum;
    }
}
