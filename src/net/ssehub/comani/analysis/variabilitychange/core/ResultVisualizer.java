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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.ssehub.comani.core.Logger;
import net.ssehub.comani.core.Logger.MessageType;
import net.ssehub.comani.utility.ProcessUtilities;
import net.ssehub.comani.utility.ProcessUtilities.ExecutionResult;

/**
 * This class provides the capabilities for visualizing the analysis results created by the
 * {@link VariabilityChangeAnalyzer} and saved as single result file by the {@link ResultCollector}. As part of these
 * capabilities, it also checks the availability (installation) of the required R-environment and takes care of
 * extracting the R-scripts to a temporary directory as well as deleting it after execution.
 *  
 * 
 * @author Christian Kroeher
 *
 */
public class ResultVisualizer {
    
    /**
     * The identifier of this class, e.g., for printing messages.
     */
    private static final String ID = "ResultVisualizer";

    /**
     * The name of the directory containing all R-scripts for visualizing the results.
     */
    private static final String SCRIPTS_DIRECTORY_NAME = "scripts";
    
    /**
     * The name of the main R-Script, which has to be called solely to visualize the results. This script in turn calls
     * the remaining R-scripts in the scripts directory.
     */
    private static final String MAIN_SCRIPT_NAME = "ComVi.R";
    
    /**
     * The set of names of all R-scripts, which are part of this project.
     */
    private static final String[] SCRIPT_NAMES = {MAIN_SCRIPT_NAME,
        "ComVi_ChangedLinesCommitSizesRelation.R",
        "ComVi_ChangedLinesPerArtifactType.R",
        "ComVi_CommitDistributionPerArtifactInformationType.R",
        "ComVi_CommitDistributionPerInformationType.R",
        "ComVi_FullEvolution.R"};
    
    /**
     * The definition of whether to delete the scripts directory (<code>true</code>) before exiting
     * {@link #visualize(File, File, String)} or not (<code>false</code>). The deletion of the script directory is only
     * necessary, if this directory is a temporary one (when using this project as Jar). Hence, the default value is
     * <code>false</code>. This value is changed in {@link #getScriptsDirectory()}.
     */
    private boolean deleteScriptsOnExit;
    
    /**
     * The internal {@link Logger} provided by the main infrastructure for logging information, warning, error, and
     * exception messages.
     */
    private Logger logger;
    
    /**
     * The {@link ProcessUtilities} for executing the R-scripts and checking the necessary prerequisites for their execution.
     */
    private ProcessUtilities processUtilities;

    /**
     * Creates a new instance of this {@link ResultVisualizer}.
     */
    public ResultVisualizer() {
        deleteScriptsOnExit = false;
        logger = Logger.getInstance();
        processUtilities = ProcessUtilities.getInstance();
    }
    
    /**
     * Visualizes the results of the given result file and saves the resulting visualizations to the given directory.
     * 
     * @param resultFile the result file as created by the {@link ResultCollector} containing the analysis results for
     *        each commit
     * @param outputDirectory the directory to which this visualizer should save its results
     * @param splName the name of the software product line the results belong to; currently only supports "Linux" and
     *        "Coreboot"
     * @return <code>true</code> if visualizing the results was successful; <code>false</code> otherwise
     */
    public boolean visualize(File resultFile, File outputDirectory, String splName) {
        logger.log(ID, "Starting visualizing", null, MessageType.DEBUG);
        boolean visualizationSuccessful = false;
        if (checkFile(resultFile, true) && checkFile(outputDirectory, false)) {
            File scriptsDirectory = getScriptsDirectory();
            if (scriptsDirectory != null) {
                File mainVisualizationScript = new File(scriptsDirectory, MAIN_SCRIPT_NAME);
                logger.log(ID, "Executing main script \"" + mainVisualizationScript.getAbsolutePath() + "\"", null,
                        MessageType.DEBUG);
                String[] command = {
                        "Rscript",
                        mainVisualizationScript.getAbsolutePath(),
                        resultFile.getAbsolutePath(),
                        outputDirectory.getAbsolutePath(),
                        splName
                };
                ExecutionResult executionResult = processUtilities.executeCommand(command, null);
                if (executionResult.executionSuccessful()) {
                    visualizationSuccessful = true;
                } else {
                    String executionErrorInformation = executionResult.getErrorOutputData();
                    if (executionErrorInformation == null || executionErrorInformation.isEmpty()) {
                        executionErrorInformation = executionResult.getStandardOutputData();
                    }
                    logger.log(ID, "Visualizing results failed", executionErrorInformation, MessageType.ERROR);
                }
                // If scripts directory is a temp one, delete it before exiting
                if (deleteScriptsOnExit) {
                    deleteScripts(scriptsDirectory);
                }
            } else {
                logger.log(ID, "Locating the scripts directory failed",
                        "Method getScriptsDirectory() returned \"null\"", MessageType.ERROR);
            }
        }
        return visualizationSuccessful;
    }
    
    /**
     * Retrieves the directory, which contains the R-scripts for visualizing the results. In case of using this project
     * as a Jar-file, it will call subsequent methods for extracting the scripts to a temporary directory and returns
     * that directory. As part of creating the temporary directory, it sets {@link #deleteScriptsOnExit} to
     * <code>true</code> to ensure that this directory will be deleted again.
     * 
     * @return the directory, which contains the R-scripts for visualizing the results; may return <code>null</code>, if
     *         the directory is not accessible or extracting the scripts to a temporary directory failed
     */
    private File getScriptsDirectory() {
        File scriptsDirectory = null;
        try {
            // First: get the current location of this project/jar file
            URI thisClassUri = ResultVisualizer.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File thisClassLocation = new File(thisClassUri);
            if (thisClassLocation.isDirectory()) {
                // Second_A: simply use the directory location in combination with "scripts\ComVi.R" as return
                logger.log(ID, "This class' location is a directory at \"" + thisClassLocation.getAbsolutePath() + "\"",
                        null, MessageType.DEBUG);
                scriptsDirectory = new File(thisClassLocation.getParentFile(), "/scripts");
            } else {
                // Second_B: extract all scripts from jar to temp and return that temp dir
                logger.log(ID, "This class' location is an archive at \"" + thisClassLocation.getAbsolutePath() + "\"",
                        null, MessageType.DEBUG);
                scriptsDirectory = extractScripts(thisClassLocation);
                // Ensure that the temp script directory will be deleted before exiting visualize()
                deleteScriptsOnExit = true;
            }
        } catch (URISyntaxException e) {
            logger.logException(ID, "Retrieving scripts directory failed", e);
        }
        return scriptsDirectory;
    }
    
    /**
     * Deletes the given directory including contained sub-directories and files from the file system. This is only used
     * in case of extracting scripts to a temporary directory for execution. See {@link #visualize(File, File, String)}.
     * 
     * @param scriptsDirectory the directory to be deleted
     */
    private void deleteScripts(File scriptsDirectory) {
        if (scriptsDirectory != null && scriptsDirectory.exists() && scriptsDirectory.isDirectory()) {
            logger.log(ID, "Deleting directory \"" + scriptsDirectory.getAbsolutePath() + "\"", null,
                    MessageType.DEBUG);
            File[] containedElements = scriptsDirectory.listFiles();
            if (containedElements != null) {
                File containedElement;
                for (int i = 0; i < containedElements.length; i++) {
                    containedElement = containedElements[i]; 
                    if (containedElement.isDirectory()) {
                        deleteScripts(containedElement);
                    } else {
                        if (!containedElement.delete()) {
                            logger.log(ID, "Deleting temporary \"" + containedElement.getAbsolutePath() 
                                    + "\" failed", null, MessageType.WARNING);
                        }
                    }
                }
            }
            if (!scriptsDirectory.delete()) {
                logger.log(ID, "Deleting temporary \"" + scriptsDirectory.getAbsolutePath() 
                        + "\" failed", null, MessageType.WARNING);
            }
        } else {
            logger.log(ID, "Deleting scripts directory failed", "Given directory is \"null\"", MessageType.WARNING);
        }
    }
    
    /**
     * Extracts all R-script files from the given archive.
     * 
     * @param jarFile the file representing this project as a compiled archive (*.jar file).
     * @return the temporary script directory to which the R-scripts are extracted; may return <code>null</code> if
     *         extracting failed
     */
    private File extractScripts(File jarFile) {
        File extractedScriptsDirectory = null;
        if (jarFile != null) {
            if (jarFile.exists() && jarFile.isFile() && jarFile.getAbsolutePath().endsWith(".jar")) {
                logger.log(ID, "Extracting scripts from archive \"" + jarFile.getAbsolutePath() + "\"", null,
                        MessageType.DEBUG);
                try {
                    // Create a temporary directory for storing all *.R scripts to
                    File tempDirectory = Files.createTempDirectory("comvi_scripts_").toFile();
                    // Identify the ZipEntries representing the *.R scripts
                    ZipFile zipFile = new ZipFile(jarFile);
                    boolean scriptsAvailable = true;
                    int scriptsCounter = 0;
                    String scriptFileName;
                    File tempScriptFile;
                    while (scriptsAvailable && scriptsCounter < SCRIPT_NAMES.length) {
                        scriptFileName = SCRIPT_NAMES[scriptsCounter];
                        ZipEntry scriptZipEntry = zipFile.getEntry(SCRIPTS_DIRECTORY_NAME + "/" + scriptFileName);
                        if (scriptZipEntry != null) {
                            // Unzip the current script to the temporary directory
                            tempScriptFile = new File(tempDirectory, scriptFileName);
                            extractScript(zipFile, scriptZipEntry, tempScriptFile);
                            logger.log(ID, "Script \"" + scriptFileName + "\" extracted to \"" 
                                    + tempScriptFile.getAbsolutePath() + "\"", null, MessageType.DEBUG);
                        } else {
                            scriptsAvailable = false;
                            logger.log(ID, "Missing visualization script",
                                    "\"" + jarFile.getAbsolutePath() + "\" does not contain \"" + scriptFileName + "\"",
                                    MessageType.ERROR);
                            deleteScriptsOnExit = true;
                        }
                        scriptsCounter++;
                    }
                    if (scriptsAvailable) {
                        extractedScriptsDirectory = tempDirectory;
                    }
                } catch (IOException e) {
                    logger.logException(ID, "Extracting scripts from \"" 
                            + jarFile.getAbsolutePath() + "\" failed", e);
                }
            } else {
                logger.log(ID, "Extracting scripts failed", "The file \"" + jarFile.getAbsolutePath() 
                        + "\" does not exist or is not a (jar) file", MessageType.ERROR);
            }
        } else {
            logger.log(ID, "Extracting scripts failed", "The given archive file is \"null\"", MessageType.ERROR);
        }
        return extractedScriptsDirectory;
    }
    
    /**
     * Extracts the given Zip entry from the given Zip-file as a new temporary file.
     * 
     * Code from https://stackoverflow.com/questions/600146/run-exe-which-is-packaged-inside-jar
     * 
     * @param zipFile the {@link ZipFile}, which contains the given Zip-entry; should never be <code>null</code> and
     *        the availability of the entry in that Zip-file has to be guaranteed (no further checks here)
     * @param scriptZipEntry the {@link ZipEntry} denoting the file to be extracted; should never be <code>null</code>
     *        and the availability of the entry in that Zip-file has to be guaranteed (no further checks here)
     * @param scriptTempFile the file to be filed with the Zip-entry bytes; should never be <code>null</code>;
     */
    private void extractScript(ZipFile zipFile, ZipEntry scriptZipEntry, File scriptTempFile) {
        logger.log(ID, "Extracting script \"" + scriptZipEntry.getName() + "\" from zip file \"" + zipFile.getName() 
                + "\" to file \"" + scriptTempFile.getAbsolutePath() + "\"", null, MessageType.DEBUG);
        InputStream zipInputStream = null;
        OutputStream scriptOutputStream = null;
        try {            
            zipInputStream = zipFile.getInputStream(scriptZipEntry);
            scriptOutputStream = new FileOutputStream(scriptTempFile);
            byte[] zipInputBuffer = new byte[1024];
            int bufferByteCounter = 0;
            while ((bufferByteCounter = zipInputStream.read(zipInputBuffer)) != -1) {
                scriptOutputStream.write(zipInputBuffer, 0, bufferByteCounter);
            }
        } catch (IOException e) {
            logger.logException(ID, "Extracting scipt \"" + scriptZipEntry.getName() + "\" failed", e);
        } finally {
            if (zipInputStream != null) {
                try {
                    zipInputStream.close();
                } catch (IOException e) {
                    logger.logException(ID, "Closing input stream for extracting scipt \"" 
                            + scriptZipEntry.getName() + "\" failed", e);
                }
            }
            if (scriptOutputStream != null) {
                try {
                    scriptOutputStream.flush();
                    scriptOutputStream.close();
                } catch (IOException e) {
                    logger.logException(ID, "Closing output stream for extracting scipt \"" 
                            + scriptZipEntry.getName() + "\" failed", e);
                }
            }
        }
    }
    
    /**
     * Checks whether the given file exists and if it denotes a file or a directory depending on the specification
     * of the check via the <code>asFile</code> parameter.
     * 
     * @param file the {@link File} to check for existence and of it denotes a file or directory
     * @param asFile <code>true</code> if the given file shall be checked as a simple file or <code>false</code> if
     *        it should be checked as a directory; if <code>true</code>, the file name must also equal the 
     *        {@link VariabilityChangeAnalyzer#RESULT_FILE_NAME}
     * @return <code>true</code> if the given file is valid; <code>false</code> otherwise
     */
    private boolean checkFile(File file, boolean asFile) {
        boolean fileValid = false;
        if (file.exists()) {
            if (asFile) {
                if (file.isFile() && file.getName().equals(VariabilityChangeAnalyzer.RESULT_FILE_NAME)) {
                    fileValid = true;
                } else {
                    logger.log(ID, "Invalid input for visualization",
                            "\"" + file.getAbsolutePath() + "\" is not an analysis result file, which should have the name \""
                            + VariabilityChangeAnalyzer.RESULT_FILE_NAME + "\"", MessageType.ERROR);
                }
            } else {
                if (file.isDirectory()) {
                    fileValid = true;
                } else {
                    logger.log(ID, "Invalid input for visualization",
                            "\"" + file.getAbsolutePath() + "\" is not a directory for saving visualizations",
                            MessageType.ERROR);
                }
            }
        } else {
            logger.log(ID, "Invalid input for visualization",
                    "\"" + file.getAbsolutePath() + "\" does not exist",
                    MessageType.ERROR);
        }
        return fileValid;
    }
}
