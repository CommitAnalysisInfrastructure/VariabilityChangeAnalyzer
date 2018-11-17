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
 * This class is used to count changes to other files than model, source code, or build files. As these files
 * are not specified in detail, there is no explicit count of variability changes. Currently, objects of this
 * class and the corresponding results are not part of the overall analysis results.
 * 
 * @author Christian Kroeher
 *
 */
public class OtherFileDiff extends FileDiff {
    
    /**
     * Construct a new {@link OtherFileDiff}.<br><br>
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
    protected OtherFileDiff(String[] diffLines, int changesStartLineNum) {
        super(FileType.OTHER, diffLines, changesStartLineNum);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected String normalize(String diffLine, int diffLinePosition) {
        return "";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isVariabilityChange(String cleanDiffLine, int cleanDiffLinePosition) {
        return false;
    }
    
}
