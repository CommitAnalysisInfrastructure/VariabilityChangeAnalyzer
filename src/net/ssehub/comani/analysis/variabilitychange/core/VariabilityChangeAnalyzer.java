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
package net.ssehub.comani.analysis.variabilitychange.core;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.ssehub.comani.analysis.AbstractCommitAnalyzer;
import net.ssehub.comani.analysis.AnalysisSetupException;
import net.ssehub.comani.analysis.variabilitychange.diff.DiffAnalyzer;
import net.ssehub.comani.core.Logger.MessageType;
import net.ssehub.comani.data.Commit;
import net.ssehub.comani.data.IAnalysisQueue;
import net.ssehub.comani.utility.ProcessUtilities;
import net.ssehub.comani.utility.ProcessUtilities.ExecutionResult;

/**
 * This class represents the main class of this analyzer. In principle, this is a wrapper, which starts the actual
 * analysis by calling {@link DiffAnalyzer} for each received commit.
 * 
 * @author Christian Kroeher
 *
 */
public class VariabilityChangeAnalyzer extends AbstractCommitAnalyzer {
    
    /**
     * The identifier of this class, e.g., for printing messages.
     */
    private static final String ID = "VariabilityChangeAnalyzer";
    
    /**
     * The string representation of the properties' key identifying the output directory to which the analysis results
     * will be stored. The definition of this property is mandatory and has to define an existing directory.
     */
    private static final String PROPERTY_ANALYSIS_OUTPUT = "analysis.output";
    
    /**
     * The string representation of the properties' key identifying the regular expression for identifying variability
     * model files. The definition of this property is mandatory and has to define a valid Java regular expression.
     */
    private static final String PROPERTY_VM_FILES_REGEX = "analysis.variability_change_analyzer.vm_files_regex";
    
    /**
     * The string representation of the properties' key identifying the regular expression for identifying code files.
     * The definition of this property is mandatory and has to define a valid Java regular expression.
     */
    private static final String PROPERTY_CODE_FILES_REGEX = "analysis.variability_change_analyzer.code_files_regex";
    
    /**
     * The string representation of the properties' key identifying the regular expression for identifying build files.
     * The definition of this property is mandatory and has to define a valid Java regular expression.
     */
    private static final String PROPERTY_BUILD_FILES_REGEX = "analysis.variability_change_analyzer.build_files_regex";
    
    /**
     * The string representation of the properties' key identifying the SPL from which the commits are analyzed. 
     * The definition of this property is optional and only necessary, if the visualization of analysis results is
     * desired. In that case, valid values are [Cc]oreboot or [Ll]inux. All other values result in an 
     * {@link AnalysisSetupException}. Hence, this property indirectly controls the visualization.
     */
    private static final String PROPERTY_TARGET_SPL = "analysis.variability_change_analyzer.target_spl";
    
    /**
     * The name of the result file containing a single line for each analyzed commit
     * with the number of changed model files, source code files, build files, and the
     * overall number of changed lines and changed lines containing variability information
     * for each of these file types.
     */
    static final String RESULT_FILE_NAME = ID + "_Results.tsv";
    
    /**
     * The name of the summary file containing the overall numbers of all analyzed commits,
     * e.g. the average number of changed model files, etc. with respect to the number of
     * analyzed commits.
     */
    private static final String SUMMARY_FILE_NAME = ID + "_Summary.tsv";
    
    /**
     * The name of the file containing the commits that were not analyzed,
     * e.g. because they do not contain line-wise changes to files.
     */
    private static final String UNANALYZED_FILE_NAME = ID + "_Unanalyzed.txt";
    
    /**
     * The command for printing the installed R version used to check whether SVN is installed during 
     * {@link #prepare()}.<br>
     * <br>
     * Command: <code>Rscript --version</code>
     */
    private static final String[] R_VERSION_COMMAND = {"Rscript", "--version"};
    
    /**
     * The directory which will contain all result files produced by this analyzer.
     */
    private File resultsDirectory;
    
    /**
     * The file which will contain the full result list of the analysis.
     */
    private File resultFile;
    
    /**
     * The file which will contain the summary of the analysis, e.g. the
     * sum and average of counted lines, etc. 
     */
    private File summaryFile;
    
    /**
     * The file which will contain a list of unanalyzable commits, e.g.
     * as they do not include line-wise changes. This file is only created
     * if such commits are found.
     */
    private File unanalyzedFile;
    
    /**
     * The string denoting the Java regular expression for identifying variability model files. This value is set by
     * {@link #prepare()} based on the value of {@link #PROPERTY_VM_FILES_REGEX}.
     */
    private String vmFilesRegex;
    
    /**
     * The string denoting the Java regular expression for identifying code files. This value is set by
     * {@link #prepare()} based on the value of {@link #PROPERTY_CODE_FILES_REGEX}.
     */
    private String codeFilesRegex;
    
    /**
     * The string denoting the Java regular expression for identifying build files. This value is set by
     * {@link #prepare()} based on the value of {@link #PROPERTY_VM_FILES_REGEX}.
     */
    private String buildFilesRegex;
    
    /**
     * The string denoting the name of the software product line for which the commits will be analyzed. Currently
     * supported values are "Linux" and "Coreboot". This value is set by {@link #prepare()} based on the value of
     * {@link #PROPERTY_TARGET_SPL}.
     */
    private String targetSpl;

    /**
     * Create a new instance of this analyzer.
     *  
     * @param analysisProperties the properties of the properties file defining the analysis process and the
     *        configuration of the analyzer in use; all properties, which start with the prefix "<i>analysis.</i>" 
     * @param commitQueue the {@link IAnalysisQueue} for transferring commits from an extractor to an analyzer
     * @throws AnalysisSetupException if the analyzer is not supporting the current operating or version control
     *         system
     */
    public VariabilityChangeAnalyzer(Properties analysisProperties, IAnalysisQueue commitQueue)
            throws AnalysisSetupException {
        super(analysisProperties, commitQueue);
        prepare(); // throws exceptions if something is missing or not as expected
        logger.log(ID, this.getClass().getName() + " created", null, MessageType.DEBUG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean analyze() {
        logger.log(ID, "Starting analysis", null, MessageType.DEBUG);
        boolean analysisSuccessful = analyzeCommits();
        if (analysisSuccessful && targetSpl != null) {
            // Not null means either "Coreboot" or "Linux", hence, do visualization (see prepare())
            ResultVisualizer resultVisualizer = new ResultVisualizer();
            analysisSuccessful = resultVisualizer.visualize(resultFile, resultsDirectory, targetSpl);
        }
        return analysisSuccessful;
    }
    
    /**
     * Analyzes all commits provided by the {@link net.ssehub.comani.data.CommitQueue} and creates the corresponding
     * entries in the results file as well as the summary file.
     * 
     * @return <code>true</code> if the analysis was successful; <code>false</code> otherwise
     */
    private boolean analyzeCommits() {
        logger.log(ID, "Analyzing commits", null, MessageType.DEBUG);
        DiffAnalyzer diffAnalyzer = null;
        int commitFilesCount = 0;
        while (commitQueue.isOpen()) {
            Commit commit = commitQueue.getCommit();
            if (commit != null) {
                logger.log(ID, "Analyzing commit " + commit.getId(), null, MessageType.DEBUG);
                diffAnalyzer = new DiffAnalyzer(vmFilesRegex, codeFilesRegex, buildFilesRegex, commit);
                if (!diffAnalyzer.getCommitNumber().isEmpty() && diffAnalyzer.analyze()) {
                    logger.log(ID, "Writing analysis results for commit " + commit.getId(), null,
                        MessageType.DEBUG);
                    ResultCollector.getInstance().addResults(diffAnalyzer, resultFile);
                } else {
                    logger.log(ID, "Commit " + commit.getId() + " not analyzed", null, MessageType.DEBUG);
                    ResultCollector.getInstance().addUnanalyzed(commit.getId(), unanalyzedFile);
                }
                commitFilesCount++;
            }
        }
        ResultCollector.getInstance().writeSummary(summaryFile, commitFilesCount);
        /*
         * TODO: we need a new mechanism to check whether the analysis was successful.
         * Possibility: add boolean return value to addResults, addUnanalyzed, and writeSummary
         * of the ResultCollector. For each commit as well as for all commits, all return values must
         * be "true". If a single commit returns "false", print a warning for that commit. If writing
         * summary in the end fails, print an error. If all commits fail, print an error.
         */
        return true;
    }
    
    /**
     * Prepares the analysis in terms of checking and adapting to properties and creating the required output files.
     * 
     * @throws AnalysisSetupException if preparing fails  
     */
    private void prepare() throws AnalysisSetupException {
        // First: check the properties and adapt the analyzer accordingly
        targetSpl = analysisProperties.getProperty(PROPERTY_TARGET_SPL);
        if (targetSpl == null) {
            logger.log(ID, "Visualization of analysis results disabled", null, MessageType.INFO);
        } else {
            if (targetSpl.equalsIgnoreCase("linux") || targetSpl.equalsIgnoreCase("coreboot")) {
                // Check if R and the required packages "Hmisc" and "nortest" are installed
                checkRInstallation();
            } else {
                throw new AnalysisSetupException("Unsupported target SPL specified");
            }
        }
        vmFilesRegex = analysisProperties.getProperty(PROPERTY_VM_FILES_REGEX);
        checkRegex(PROPERTY_VM_FILES_REGEX, vmFilesRegex);
        codeFilesRegex = analysisProperties.getProperty(PROPERTY_CODE_FILES_REGEX);
        checkRegex(PROPERTY_CODE_FILES_REGEX, codeFilesRegex);
        buildFilesRegex = analysisProperties.getProperty(PROPERTY_BUILD_FILES_REGEX);
        checkRegex(PROPERTY_BUILD_FILES_REGEX, buildFilesRegex);
        // Second: check and prepare required directories and files
        // Unavailability of analysis output property or defined directory handled by Setup
        resultsDirectory = new File(analysisProperties.getProperty(PROPERTY_ANALYSIS_OUTPUT));
        // Delete directory first and create new one including files to guarantee new results
        deleteResultDirectory(resultsDirectory);
        if (resultsDirectory.mkdirs()) {
            resultFile = new File(resultsDirectory, RESULT_FILE_NAME);
            summaryFile = new File(resultsDirectory, SUMMARY_FILE_NAME);
            unanalyzedFile = new File(resultsDirectory, UNANALYZED_FILE_NAME);
            try {
                resultFile.createNewFile();
                summaryFile.createNewFile();
                // Do not create unanalyzed commits file here. Create only if unanalyzable commits occur.
            } catch (IOException e) {
                throw new AnalysisSetupException("Creating new output files failed", e);
            }
        } else {
            throw new AnalysisSetupException("Creating new result directory \"" + resultsDirectory.getAbsolutePath() 
                    + "\" failed");
        }
    }
    
    /**
     * Checks if the given regular expression (regex) for the given file identification property (regexProperty) is not
     * empty or undefined and a valid Java regular expression.
     * 
     * @param regexProperty one of {@link #PROPERTY_VM_FILES_REGEX}, {@link #PROPERTY_CODE_FILES_REGEX}, or
     *        {@link #PROPERTY_BUILD_FILES_REGEX}
     * @param regex the Java regular expression as defined by the user for one of the above properties
     * @throws AnalysisSetupException if the given expression is empty, undefined, or is not a valid Java regular
     *         expression
     */
    private void checkRegex(String regexProperty, String regex) throws AnalysisSetupException {
        String exceptionMessage = null;
        if (regex != null && !regex.isEmpty()) {
            try {                
                Pattern.compile(regex);
            } catch (PatternSyntaxException e) {
                exceptionMessage = "Regular expression for \"" + regexProperty + "\" is invalid: " + e.getDescription();
            }
        } else {
            exceptionMessage = "Missing Java regular expression for identifying files; please define \"" 
                    + regexProperty + "\" in the configuration file";
        }
        if (exceptionMessage != null) {
            throw new AnalysisSetupException(exceptionMessage);
        }
    }
    
    /**
     * Checks if R is installed on the current machine and if the required packages "Hmisc" and "nortest" are available.
     */
    private void checkRInstallation() throws AnalysisSetupException {
        ProcessUtilities processUtilities = ProcessUtilities.getInstance();
        ExecutionResult executionResult = processUtilities.executeCommand(R_VERSION_COMMAND, null);
        /*
         * R is installed if the execution was successful and
         *     - on Windows: the standard stream contains text "Rscript (R) [...]"
         *     - on Ubuntu: the standard stream contains text "R [...]"
         */
        if (executionResult.executionSuccessful()) {
        	if (startsWith(executionResult.getStandardOutputData(), "Rscript (R)")
        			|| startsWith(executionResult.getErrorOutputData(), "R ")) {
                // Check for installation of required R packages
                /*
                 * TODO This check does not work on Linux as the process executing the command does not include the
                 * package list (neither on standard output nor on error output). As long as we do not have a solution
                 * for this, excluded the check.
                 */
//                executionResult = processUtilities.executeCommand("R -q -e \"installed.packages()[,1]\"", null);
//                if (executionResult.executionSuccessful()) {
//                    String standardOutputData = executionResult.getStandardOutputData();
//                    if (standardOutputData != null && !standardOutputData.isEmpty()) {                        
//                        if (!standardOutputData.contains("Hmisc") || !standardOutputData.contains("nortest")) {
//                            throw new AnalysisSetupException("Missing R packages" + System.lineSeperator()
//                                    + "Please install packages \"Hmisc\" and \"nortest\" as part of the R installation");
//                        }
//                    } else {
//                        throw new AnalysisSetupException("Executing command \"R -q -e \"installed.packages()[,1]\"\" returned no output"
//                                + System.lineSeperator()
//                                + "Cannot determine installed R packages");
//                    }
//                } else {
//                    throw new AnalysisSetupException("Executing command \"R -q -e \"installed.packages()[,1]\"\" failed"
//                            + System.lineSeperator()
//                            + "Cannot determine installed R packages");
//                }
            } else {
                throw new AnalysisSetupException("Missing R-environment for visualizing results");
            }
        } else {
            throw new AnalysisSetupException("Missing R-environment for visualizing results");
        }
    }
    
    /**
     * Checks whether the given test string starts with the given prefix. This method checks both strings for being not
     * <code>null</code> and that the given test string is not empty before the actual check.
     * 
     * @param testString the string to check for starting with the given prefix
     * @param prefix the prefix the given test string should start with
     * @return <code>true</code>, if the given test string starts with the given prefix; <code>false</code> otherwise
     */
    private boolean startsWith(String testString, String prefix) {
    	boolean startsWith = false;
    	if (testString != null && !testString.isEmpty() && prefix != null) {
    		startsWith = testString.startsWith(prefix);
    	}
    	return startsWith;
    }
    
    /**
     * Deletes the given directory including contained sub-directories and files from the file system.
     * 
     * @param resultsDirectory the directory to be deleted
     * @throws AnalysisSetupException if deleting the directory or the contained files failed
     */
    private void deleteResultDirectory(File resultsDirectory) throws AnalysisSetupException {
        if (resultsDirectory != null && resultsDirectory.exists() && resultsDirectory.isDirectory()) {
            logger.log(ID, "Deleting directory \"" + resultsDirectory.getAbsolutePath() + "\"", null,
                    MessageType.DEBUG);
            File[] containedElements = resultsDirectory.listFiles();
            if (containedElements != null) {
                File containedElement;
                for (int i = 0; i < containedElements.length; i++) {
                    containedElement = containedElements[i]; 
                    if (containedElement.isDirectory()) {
                        deleteResultDirectory(containedElement);
                    } else {
                        if (!containedElement.delete()) {
                            throw new AnalysisSetupException("Deleting \"" + containedElement.getAbsolutePath() 
                                    + "\" failed");
                        }
                    }
                }
            }
            if (!resultsDirectory.delete()) {
                throw new AnalysisSetupException("Deleting \"" + resultsDirectory.getAbsolutePath() 
                        + "\" failed");
            }
        } else {
            throw new AnalysisSetupException("Deleting results directory failed as given directory is \"null\"");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean operatingSystemSupported(String operatingSystem) {
        // This analyzer is OS-independent
        logger.log(ID, "Supported operating systems: all", null, MessageType.DEBUG);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean versionControlSystemSupported(String versionControlSystem) {
        logger.log(ID, "Supported version control systems: Git and SVN", null, MessageType.DEBUG);
        return versionControlSystem.equalsIgnoreCase("Git") || versionControlSystem.equalsIgnoreCase("SVN");
    }

}
