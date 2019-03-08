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

import java.util.List;
import java.util.regex.Pattern;

import net.ssehub.comani.data.ChangedArtifact;
import net.ssehub.comani.data.Commit;

/**
 * This class implements a general diff analyzer.<br><br>
 * 
 * This class is used to provide the numbers of changed lines in different file types of a specific commit.
 * In turn, this class uses the different file diff classes depending on the type of file changed by the
 * given commit.
 * 
 * 
 * @author Christian Kroeher
 *
 */
public class DiffAnalyzer {
    
    /**
     * This array contains file extensions (without the ".") for identifying files
     * that should not be analyzed.<br><br>
     * 
     * Although regular expressions for identifying files for analysis exist, there
     * are certain combinations that lead to wrong results, e.g. "Config.lb"
     * (found in coreboot), where the name of the file seems to define a Kconfig-file,
     * but the content is not.
     */
    private static final String[] FILE_EXTENSION_BLACKLIST = {"lb"};
    
    /**
     * Regex identifying directories containing documentation.<br><br>
     * 
     * Value: {@value #DOC_DIR_PATTERN};
     */
    private static final String DOC_DIR_PATTERN = "[dD]ocumentation(s)?";
    
    /**
     * Regex identifying directories containing scripts.<br><br>
     * 
     * Value: {@value #SCRIPT_DIR_PATTERN};
     */
    private static final String SCRIPT_DIR_PATTERN = "[sS]cript(s)?";
    
    /**
     * Regex identifying files to be excluded from analysis,
     * in particular documentation files or scripts.<br><br>
     * 
     * Value: {@value #FILE_EXCLUDE_PATTERN};<br>
     * 
     * See {@link #DOC_DIR_PATTERN} and {@link #SCRIPT_DIR_PATTERN}
     */
    private static final String FILE_EXCLUDE_PATTERN = "(.*/((" + DOC_DIR_PATTERN + ")|(" 
                                                           + SCRIPT_DIR_PATTERN + "))/.*)|(.*\\.txt)";
    
    /**
     * Regex identifying variability model files.
     */
    private String vmFilePattern;
    
    /**
     * Regex identifying code files.
     */
    private String codeFilePattern;
    
    /**
     * Regex identifying build files.
     */
    private String buildFilePattern;
    
    /**
     * The {@link Commit} to analyze given via the constructor of this class.
     * 
     * @see #DiffAnalyzer(Commit)
     */
    private Commit commit;
    
    /**
     * The number of model files changed by the commit.
     */
    private int changedModelFilesCounter = 0;
    
    /**
     * The number of all lines of all model file changed by the commit. 
     */
    private int changedModelLinesCounter = 0;
    
    /**
     * The number of lines containing variability information of all model files
     * changed by the commit.
     */
    private int changedModelVarLinesCounter = 0;
    
    /**
     * The number of source code files changed by the commit.
     */
    private int changedSourceFilesCounter = 0;
    
    /**
     * The number of all lines of all source code file changed by the commit. 
     */
    private int changedSourceLinesCounter = 0;
    
    /**
     * The number of lines containing variability information of all source
     * code files changed by the commit.
     */
    private int changedSourceVarLinesCounter = 0;
    
    /**
     * The number of build files changed by the commit.
     */
    private int changedBuildFilesCounter = 0;
    
    /**
     * The number of all lines of all build file changed by the commit. 
     */
    private int changedBuildLinesCounter = 0;
    
    /**
     * The number of lines containing variability information of all build files
     * changed by the commit.
     */
    private int changedBuildVarLinesCounter = 0;
    
    /**
     * Construct a new {@link DiffAnalyzer}.
     * 
     * @param vmFilesRegex the regular expression identifying variability model files
     * @param codeFilesRegex the regular expression identifying code files
     * @param buildFilesRegex the regular expression identifying build files
     * @param commit the {@link Commit} containing diff information
     */
    public DiffAnalyzer(String vmFilesRegex, String codeFilesRegex, String buildFilesRegex, Commit commit) {
        this.vmFilePattern = vmFilesRegex;
        this.codeFilePattern = codeFilesRegex;
        this.buildFilePattern = buildFilesRegex;
        this.commit = commit;
    }
    
    /**
     * Analyze the artifacts changed by the given commit.
     * 
     * @return <code>true</code> if the analysis of the given commit (changed artifacts) was successful,
     *         <code>false</code> otherwise
     */
    public boolean analyze() {
        boolean analyzedSuccessful = false;
        if (!commit.getId().isEmpty()) {
            List<ChangedArtifact> changedArtifactList = commit.getChangedArtifacts();
            FileDiff fileDiff = null;
            for (ChangedArtifact changedArtifact : changedArtifactList) {
                fileDiff = createFileDiff(changedArtifact);
                if (fileDiff != null) {
                    switch(fileDiff.getFileType()) {
                    case MODEL:
                        changedModelLinesCounter = changedModelLinesCounter + getChangedLines(fileDiff, false);
                        changedModelVarLinesCounter = changedModelVarLinesCounter + getChangedLines(fileDiff, true);
                        break;
                    case SOURCE:
                        changedSourceLinesCounter = changedSourceLinesCounter + getChangedLines(fileDiff, false);
                        changedSourceVarLinesCounter = changedSourceVarLinesCounter + getChangedLines(fileDiff, true);
                        break;
                    case BUILD:
                        changedBuildLinesCounter = changedBuildLinesCounter + getChangedLines(fileDiff, false);
                        changedBuildVarLinesCounter = changedBuildVarLinesCounter + getChangedLines(fileDiff, true);
                        break;
                    default:
                        // like OTHER, do nothing
                        break;
                    }
                    analyzedSuccessful = true;
                }
            }
        }
        return analyzedSuccessful;
    }
    
    /**
     * Return the sum of changed lines of the given {@link FileDiff}; either all changed lines or only those lines
     * that contain variability information (see <code>varLinesOnly</code>).
     * 
     * @param fileDiff the {@link FileDiff} for which the sum of changed lines should be calculated
     * @param varLinesOnly returns only those lines that contain variability information if set to <code>true</code>
     * or all changed lines if set to <code>false</code>
     * @return the sum of changed lines (all or variability only) of the given file diff
     */
    private int getChangedLines(FileDiff fileDiff, boolean varLinesOnly) {
        int changedLines = 0;
        if (varLinesOnly) {
            // Return sum of changed lines holding variability information only
            changedLines = fileDiff.getAddedVarLinesNum() + fileDiff.getDeletedVarLinesNum();
        } else {
            // Return sum of all changed lines
            changedLines = fileDiff.getAddedLinesNum() + fileDiff.getDeletedLinesNum();
        }
        return changedLines;
    }
    
    /**
     * Create a new {@link FileDiff} based on the given {@link ChangedArtifact}. The actual type
     * of the returned <code>FileDiff</code> depends on the type of file under change as provided by the
     * changed artifact information:<br>
     * <ul>
     * <li>{@link SourceFileDiff}</li>
     * <li>{@link BuidFileDiff}</li>
     * <li>{@link ModelFileDiff}</li>
     * </ul><br>
     * 
     * @param changedArtifact the {@link ChangedArtifact} describing the changes of a specific file
     * @return a {@link FileDiff} object holding detailed information about the diff, e.g. number of changed lines
     */
    private FileDiff createFileDiff(ChangedArtifact changedArtifact) {
        FileDiff fileDiff = null;
        if (!changedArtifact.getArtifactPath().isEmpty()) {
            String[] diffLines = changedArtifact.getContent().toArray(new String[0]);
            if (Pattern.matches(FILE_EXCLUDE_PATTERN, changedArtifact.getArtifactPath())
                    || isBlacklisted(changedArtifact.getArtifactPath())) {
                // Either excluded or blacklisted file changed, thus use OtherFileDiff
                fileDiff = new OtherFileDiff(diffLines, 0);
            } else if (Pattern.matches(codeFilePattern, changedArtifact.getArtifactPath())) {
                // Diff affects source code file
                changedSourceFilesCounter++;
                fileDiff = new SourceFileDiff(diffLines, 0);
            } else if (Pattern.matches(buildFilePattern, changedArtifact.getArtifactPath())) {
                // Diff affects build file
                changedBuildFilesCounter++;
                fileDiff = new BuildFileDiff(diffLines, 0);
            } else if (Pattern.matches(vmFilePattern, changedArtifact.getArtifactPath())) {
                // Diff affects model file
                changedModelFilesCounter++;
                fileDiff = new ModelFileDiff(diffLines, 0);
            } else {
                /*
                 * As this method should only return null if no changes start line can be identified,
                 * we need another way of excluding files not of interest. This is done by creating an
                 * OtherFileDiff-object, which is actually doing nothing and does not influence further
                 * analysis. 
                 */
                fileDiff = new OtherFileDiff(diffLines, 0);
            }
        }
        return fileDiff;
    }
    
    /**
     * Check the name of the changed file defined in the given changed file description line
     * against the blacklisted file extensions defined in {@link #FILE_EXTENSION_BLACKLIST}.
     *  
     * @param changedFileDescriptionLine the first line of a diff containing the path and the
     * name of the changed file, e.g. "diff --git a/include/libbb.h b/include/libbb.h"
     * @return <code>true</code> if the extension of the file in the given changed file description
     * line matches on of the blacklisted file extensions, <code>false</code> otherwise
     */
    private boolean isBlacklisted(String changedFileDescriptionLine) {
        boolean isBlacklisted = false;
        int blacklistCounter = 0;
        while (blacklistCounter < FILE_EXTENSION_BLACKLIST.length && !isBlacklisted) {
            /*
             * The given line always contains a string similar to "diff --git a/include/libbb.h b/include/libbb.h".
             * Thus, remove leading and trailing whitespace and check if one of the blacklist entries prepended by
             * a "." matched the end of the given line. 
             */
            String fileExtension = "." + FILE_EXTENSION_BLACKLIST[blacklistCounter];
            if (changedFileDescriptionLine.trim().endsWith(fileExtension)) {
                isBlacklisted = true;
            }
            blacklistCounter++;
        }
        return isBlacklisted;
    }
    
    /**
     * Parse the given content line to a reduced date format.
     * 
     * @param commitFileFirstLine the first line in the {@link #commitFile} containing the date and
     * time the commit was created, e.g. <i>2011-06-10 06:01:30 +0200</i>
     * @return the parsed commit date
     */
    private String parseCommitDate(String commitFileFirstLine) {
        String commitDate = null;
        if (commitFileFirstLine != null && !commitFileFirstLine.isEmpty()) {
            // The line contains date and time like "2011-06-10 06:01:30 +0200"
            String[] dateAndTimeParts = commitFileFirstLine.split("\\s+");
            if (dateAndTimeParts.length > 0) {
                // Here, we only need the first part "2011-06-10" split into year, month, and day
                String[] dateParts = dateAndTimeParts[0].split("-");
                if (dateParts.length == 3) {
                    commitDate = dateParts[0] + "/" + dateParts[1] + "/" + dateParts[2]; 
                }
            }
        }
        return commitDate;
    }
    
    /**
     * Return the commit SHA of the analyzed commit.
     * 
     * @return the commit SHA of the analyzed commit
     */
    public String getCommitNumber() {
        return commit.getId();
    }
    
    /**
     * Return the date the analyzed commit was created
     * in the format <i>dd/mm/yyyy</i>.
     * 
     * @return the date the analyzed commit was created
     */
    public String getCommitDate() {
        return parseCommitDate(commit.getDate());
    }
    
    /**
     * Return the number of model files changed by analyzed commit.<br><br>
     * 
     * <b>Note</b> that calling {@link #analyze()} before calling this method is
     * required to return the correct number.
     * 
     * @return the number of model files changed by analyzed commit
     */
    public int getChangedModelFilesCount() {
        return changedModelFilesCounter;
    }
    
    /**
     * Return the number of source code files changed by analyzed commit.<br><br>
     * 
     * <b>Note</b> that calling {@link #analyze()} before calling this method is
     * required to return the correct number.
     * 
     * @return the number of source code files changed by analyzed commit
     */
    public int getChangedSourceFilesCount() {
        return changedSourceFilesCounter;
    }
    
    /**
     * Return the number of build files changed by analyzed commit.<br><br>
     * 
     * <b>Note</b> that calling {@link #analyze()} before calling this method is
     * required to return the correct number.
     * 
     * @return the number of build files changed by analyzed commit
     */
    public int getChangedBuildFilesCount() {
        return changedBuildFilesCounter;
    }
    
    /**
     * Return the number of lines of all model files changed by analyzed commit.<br><br>
     * 
     * <b>Note</b> that calling {@link #analyze()} before calling this method is
     * required to return the correct number.
     * 
     * @return the number of lines of all model files changed by analyzed commit
     */
    public int getChangedModelLinesCount() {
        return changedModelLinesCounter;
    }
    
    /**
     * Return the number of lines of all source code files changed by analyzed commit.<br><br>
     * 
     * <b>Note</b> that calling {@link #analyze()} before calling this method is
     * required to return the correct number.
     * 
     * @return the number of lines of all source code files changed by analyzed commit
     */
    public int getChangedSourceLinesCount() {
        return changedSourceLinesCounter;
    }
    
    /**
     * Return the number of lines of all build files changed by analyzed commit.<br><br>
     * 
     * <b>Note</b> that calling {@link #analyze()} before calling this method is
     * required to return the correct number.
     * 
     * @return the number of lines of all build files changed by analyzed commit
     */
    public int getChangedBuildLinesCount() {
        return changedBuildLinesCounter;
    }
    
    /**
     * Return the number of lines containing variability information of all model files changed by analyzed
     * commit.<br><br>
     * 
     * <b>Note</b> that calling {@link #analyze()} before calling this method is
     * required to return the correct number.
     * 
     * @return the number of lines containing variability information of all model files changed by analyzed commit
     */
    public int getChangedModelVarLinesCount() {
        return changedModelVarLinesCounter;
    }
    
    /**
     * Return the number of lines containing variability information of all source code files changed by analyzed
     * commit.<br><br>
     * 
     * <b>Note</b> that calling {@link #analyze()} before calling this method is
     * required to return the correct number.
     * 
     * @return the number of lines containing variability information of all source code files changed by analyzed
     *         commit
     */
    public int getChangedSourceVarLinesCount() {
        return changedSourceVarLinesCounter;
    }
    
    /**
     * Return the number of lines containing variability information of all build files changed by analyzed
     * commit.<br><br>
     * 
     * <b>Note</b> that calling {@link #analyze()} before calling this method is
     * required to return the correct number.
     * 
     * @return the number of lines containing variability information of all build files changed by analyzed commit
     */
    public int getChangedBuildVarLinesCount() {
        return changedBuildVarLinesCounter;
    }
}
