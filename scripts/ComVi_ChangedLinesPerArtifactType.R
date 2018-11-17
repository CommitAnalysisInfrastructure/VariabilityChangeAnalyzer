# The Commit Visualization (ComVi) script for calculating and visualizing the sums over all commits of:
#     - Changed Code Lines containing Artifact-specific Information (CCLAI)
#     - Changed Code Lines containing Variability Information (CCLVI)
#     - Changed Build Lines containing Artifact-specific Information (CBLAI)
#     - Changed Build Lines containing Variability Information (CBLVI)
#     - Changed Model Lines containing Artifact-specific Information (CMLAI)
#     - Changed Model Lines containing Variability Information (CMLVI)
#
# Further, this script creates a frequency distribution for the above information types
#
# Required input (provided by main ComVi script):
#     - os: the operating system for determining the type of file to save the plots
#     - resultData: data frame containing the result data from ComAn_Results.tsv
#     - saveDir: the absolute path to the output directory (for saving results of this script)
#     - analyzedSpl: the name of the SPl the "resulData" belongs to (for naming result files of this
#           script and to pass the value as parameter to "getMaxY" below)
#     - getMaxY: function for determination of the maximum y-axis value of the graph
#
# Author: Christian Kröher


# Adjust y-axis numbers (no scientific)
options(scipen=1000)

# Calculate the sums of changed lines for each artifact type over all commits (years)
cclai <- sum(resultData["CCLAI"])
cclvi <- sum(resultData["CCLVI"])
cblai <- sum(resultData["CBLAI"])
cblvi <- sum(resultData["CBLVI"])
cmlai <- sum(resultData["CMLAI"])
cmlvi <- sum(resultData["CMLVI"])
all <- sum(cclai, cclvi, cblai, cblvi, cmlai, cmlvi)

# Create file for saving plot
if (os == "Windows") {
  # We are on Windows, use EMF
  plotFile <- paste(analyzedSpl, "ChangedLinesSumsPerArtifactType.emf", sep = "_")
  plotFile <- paste(saveDir, plotFile, sep="/")
  win.metafile(file = plotFile, width = 11, height = 6)
} else {
  # We are on Linux, use PDF
  plotFile <- paste(analyzedSpl, "ChangedLinesSumsPerArtifactType.pdf", sep = "_")
  plotFile <- paste(saveDir, plotFile, sep="/")
  pdf(file = plotFile, width = 11, height = 6)
}

# Create bar values and their labels and plot the bars
barNumbers <- c(all, cclai, cclvi, cblai, cblvi, cmlai, cmlvi)
barLabels <- c("All", "CCLAI", "CCLVI", "CBLAI", "CBLVI", "CMLAI", "CMLVI")
barPercentages <- round(barNumbers/sum(cclai, cclvi, cblai, cblvi, cmlai, cmlvi)*100, 2)
barTexts <- barNumbers
barTexts <- paste(barTexts, "(", sep = " ")
barTexts <- paste(barTexts, barPercentages, sep = "")
barTexts <- paste(barTexts, "%", sep = "")
barTexts <- paste(barTexts, ")", sep = "")
yMaxValue <- getMaxY(splName = analyzedSpl, visualizedData = "CLSPAT") # use function and "analyzedSpl" defined in ComVi.R
# Define custom colors for bars
all_col <- rgb(95/255, 95/255, 95/255) # dark gray
cclai_col <- rgb(31/255, 31/255, 95/255) # dark indigo
cclvi_col <- rgb(105/255, 105/255, 205/255) # light indigo
cblai_col <- rgb(35/255, 70/255, 0/255) # dark dark green
cblvi_col <- rgb(99/255, 198/255, 0/255) # green
cmlai_col <- rgb(104/255, 9/255, 11/255) # dark dark red
cmlvi_col <- rgb(236/255, 32/255, 37/255) # red
barColors <- c(all_col, cclai_col, cclvi_col, cblai_col, cblvi_col, cmlai_col, cmlvi_col)
bars <- barplot(barNumbers, col = barColors , names.arg = barLabels, ylim = c(0, yMaxValue), xlab = "Line Type", ylab = "Number of Lines")
# Add the bar labels created above
text(x = bars, y = barNumbers, pos = 3, labels = barTexts)
# Add the legend to the plot
legend("topright", inset = c(0.01, -0.01), ncol = 1, bty = "n",
       title = expression(bold("Legend")), title.adj = 0,
       legend = c("All = Full number of changed lines",
                  "CCLAI = Changed Code Lines containing Artifact-specific Information",
                  "CCLVI = Changed Code Lines containing Variability Information",
                  "CBLAI = Changed Build Lines containing Artifact-specific Information",
                  "CBLVI = Changed Build Lines containing Variability Information",
                  "CMLAI = Changed Model Lines containing Artifact-specific Information",
                  "CMLVI = Changed Model Lines containing Variability Information"),
       fill = barColors)
dev.off()


# Create frequency distribution
allCommits <- length(resultData$Commit)
changedLinesIntervalsMaxValues <- c(0, 10, 50, 100, 200, 300, 400, 500, 1000, 10000, 100000, 1000000, 10000000, 100000000)
table <- as.data.frame(changedLinesIntervalsMaxValues)

cclaiFreq <- length(resultData$CCLAI [resultData$CCLAI %in% table$changedLinesIntervalsMaxValues[1]])
for (i in 2:length(changedLinesIntervalsMaxValues)) {
  min <- changedLinesIntervalsMaxValues[i - 1]
  max <- changedLinesIntervalsMaxValues[i]
  cclaiFreq <- c(cclaiFreq, length(resultData$CCLAI [resultData$CCLAI > min & resultData$CCLAI <= max]))
}
table <- cbind(table, cclaiFreq)
cclaiFreqPerc <- round(cclaiFreq/allCommits*100, 2)
table <- cbind(table, cclaiFreqPerc)

cclviFreq <- length(resultData$CCLVI [resultData$CCLVI %in% table$changedLinesIntervalsMaxValues[1]])
for (i in 2:length(changedLinesIntervalsMaxValues)) {
  min <- changedLinesIntervalsMaxValues[i - 1]
  max <- changedLinesIntervalsMaxValues[i]
  cclviFreq <- c(cclviFreq, length(resultData$CCLVI [resultData$CCLVI > min & resultData$CCLVI <= max]))
}
table <- cbind(table, cclviFreq)
cclviFreqPerc <- round(cclviFreq/allCommits*100, 2)
table <- cbind(table, cclviFreqPerc)

cblaiFreq <- length(resultData$CBLAI [resultData$CBLAI %in% table$changedLinesIntervalsMaxValues[1]])
for (i in 2:length(changedLinesIntervalsMaxValues)) {
  min <- changedLinesIntervalsMaxValues[i - 1]
  max <- changedLinesIntervalsMaxValues[i]
  cblaiFreq <- c(cblaiFreq, length(resultData$CBLAI [resultData$CBLAI > min & resultData$CBLAI <= max]))
}
table <- cbind(table, cblaiFreq)
cblaiFreqPerc <- round(cblaiFreq/allCommits*100, 2)
table <- cbind(table, cblaiFreqPerc)

cblviFreq <- length(resultData$CBLVI [resultData$CBLVI %in% table$changedLinesIntervalsMaxValues[1]])
for (i in 2:length(changedLinesIntervalsMaxValues)) {
  min <- changedLinesIntervalsMaxValues[i - 1]
  max <- changedLinesIntervalsMaxValues[i]
  cblviFreq <- c(cblviFreq, length(resultData$CBLVI [resultData$CBLVI > min & resultData$CBLVI <= max]))
}
table <- cbind(table, cblviFreq)
cblviFreqPerc <- round(cblviFreq/allCommits*100, 2)
table <- cbind(table, cblviFreqPerc)

cmlaiFreq <- length(resultData$CMLAI [resultData$CMLAI %in% table$changedLinesIntervalsMaxValues[1]])
for (i in 2:length(changedLinesIntervalsMaxValues)) {
  min <- changedLinesIntervalsMaxValues[i - 1]
  max <- changedLinesIntervalsMaxValues[i]
  cmlaiFreq <- c(cmlaiFreq, length(resultData$CMLAI [resultData$CMLAI > min & resultData$CMLAI <= max]))
}
table <- cbind(table, cmlaiFreq)
cmlaiFreqPerc <- round(cmlaiFreq/allCommits*100, 2)
table <- cbind(table, cmlaiFreqPerc)

cmlviFreq <- length(resultData$CMLVI [resultData$CMLVI %in% table$changedLinesIntervalsMaxValues[1]])
for (i in 2:length(changedLinesIntervalsMaxValues)) {
  min <- changedLinesIntervalsMaxValues[i - 1]
  max <- changedLinesIntervalsMaxValues[i]
  cmlviFreq <- c(cmlviFreq, length(resultData$CMLVI [resultData$CMLVI > min & resultData$CMLVI <= max]))
}
table <- cbind(table, cmlviFreq)
cmlviFreqPerc <- round(cmlviFreq/allCommits*100, 2)
table <- cbind(table, cmlviFreqPerc)

frequDistrCsvFile <- paste(analyzedSpl, "ChangedLinesPerArtifactTypeFrequencyDistribution.csv", sep="_")
frequDistrCsvFile <- paste(saveDir, frequDistrCsvFile, sep="/")
write.csv(table, file = frequDistrCsvFile, row.names = FALSE)

# Create file for saving plot
if (os == "Windows") {
  # We are on Windows, use EMF
  plotFile <- paste(analyzedSpl, "ChangedLinesPerArtifactTypeFrequencyDistribution.emf", sep = "_")
  plotFile <- paste(saveDir, plotFile, sep="/")
  win.metafile(file = plotFile, width = 11, height = 6)
} else {
  # We are on Linux, use PDF
  plotFile <- paste(analyzedSpl, "ChangedLinesPerArtifactTypeFrequencyDistribution.pdf", sep = "_")
  plotFile <- paste(saveDir, plotFile, sep="/")
  pdf(file = plotFile, width = 11, height = 6)
}

plotTable <- rbind(cclaiFreq, cclviFreq, cblaiFreq, cblviFreq, cmlaiFreq, cmlviFreq)
xLabels <- c("I01", "I02", "I03", "I04", "I05", "I06", "I07", "I08", "I09", "I10", "I11", "I12", "I13", "I14")
yMaxValue <- getMaxY(splName = analyzedSpl, visualizedData = "CLPATFD") # use function and "analyzedSpl" defined in ComVi.R
barplot(plotTable,
        beside = TRUE,
        xlab = "Interval of Changed Lines", ylab = "Frequency of Commits",
        ylim = c(0, yMaxValue),
        col = c(cclai_col, cclvi_col, cblai_col, cblvi_col, cmlai_col, cmlvi_col),
        names.arg = xLabels)
# Add the legend to the plot
mainLegend <- legend("topright", inset = c(0.01, -0.01), ncol = 1, bty = "n",
       title = expression(bold("Legend")), title.adj = 0,
       legend = c("CCLAI = Changed Code Lines containing Artifact-specific Information",
                  "CCLVI = Changed Code Lines containing Variability Information",
                  "CBLAI = Changed Build Lines containing Artifact-specific Information",
                  "CBLVI = Changed Build Lines containing Variability Information",
                  "CMLAI = Changed Model Lines containing Artifact-specific Information",
                  "CMLVI = Changed Model Lines containing Variability Information"),
       fill = c(cclai_col,
                cclvi_col,
                cblai_col,
                cblvi_col,
                cmlai_col,
                cmlvi_col))
legend(x = mainLegend$rect$left, y = mainLegend$rect$top - mainLegend$rect$h, ncol = 2, bty = "n", x.intersp = -0.05,
       legend = c("I01 = 0",
                  "I02 = 1-10",
                  "I03 = 11-50",
                  "I04 = 51-100",
                  "I05 = 101-200",
                  "I06 = 201-300",
                  "I07 = 301-400",
                  "I08 = 401-500",
                  "I09 = 501-1000",
                  "I10 = 1001-10000",
                  "I11 = 10001-100000",
                  "I12 = 100001-1000000",
                  "I13 = 1000001-10000000",
                  "I14 = 10000001-100000000"))
dev.off()