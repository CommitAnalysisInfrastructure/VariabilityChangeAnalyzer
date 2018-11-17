# The Commit Visualization (ComVi) script for calculating and visualizing the numbers of:
#     - Commit Changing Artifact-specific Information in Code artifacts (CCAIC)
#     - Commit Changing Artifact-specific Information in Code artifacts exclusively (CCAICe)
#     - Commit Changing Variability Information in Code artifacts (CCVIC)
#     - Commit Changing Variability Information in Code artifacts exclusively (CCVICe)
#     - Commit Changing Artifact-specific Information in Build artifacts (CCAIB)
#     - Commit Changing Artifact-specific Information in Build artifacts exclusively (CCAIBe)
#     - Commit Changing Variability Information in Build artifacts (CCVIB)
#     - Commit Changing Variability Information in Build artifacts exclusively (CCVIBe)
#     - Commit Changing Artifact-specific Information in Model artifacts (CCAIM)
#     - Commit Changing Artifact-specific Information in Model artifacts exclusively (CCAIMe)
#     - Commit Changing Variability Information in Model artifacts (CCVIM)
#     - Commit Changing Variability Information in Model artifacts exclusively (CCVIMe)
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

# Count number of analyzed commits
commits <- length(resultData$Commit)

# Calculate the numbers of changes to artifact-specific and variability information in code artifacts
ccaic <- length(resultData$CCLAI [ ! resultData$CCLAI %in% 0]) # artifact-specific information (non-exclusive; includes ccaice)
ccaice <- length(resultData[resultData$CMLAI == 0 & resultData$CMLVI == 0 & resultData$CCLAI > 0
    & resultData$CCLVI == 0 & resultData$CBLAI == 0 & resultData$CBLVI == 0,]$Commit) # artifact-specific information (exclusive)
ccvic <- length(resultData$CCLVI [ ! resultData$CCLVI %in% 0]) # variability information (non-exclusive; includes ccvice)
ccvice <- length(resultData[resultData$CMLAI == 0 & resultData$CMLVI == 0 & resultData$CCLAI == 0
    & resultData$CCLVI > 0 & resultData$CBLAI == 0 & resultData$CBLVI == 0,]$Commit) # variability information (exclusive)

# Calculate the numbers of changes to artifact-specific and variability information in build artifacts
ccaib <- length(resultData$CBLAI [ ! resultData$CBLAI %in% 0]) # artifact-specific information (non-exclusive; includes ccaibe)
ccaibe <- length(resultData[resultData$CMLAI == 0 & resultData$CMLVI == 0 & resultData$CCLAI == 0
    & resultData$CCLVI == 0 & resultData$CBLAI > 0 & resultData$CBLVI == 0,]$Commit) # artifact-specific information (exclusive)
ccvib <- length(resultData$CBLVI [ ! resultData$CBLVI %in% 0]) # variability information (non-exclusive; includes ccvibe)
ccvibe <- length(resultData[resultData$CMLAI == 0 & resultData$CMLVI == 0 & resultData$CCLAI == 0
    & resultData$CCLVI == 0 & resultData$CBLAI == 0 & resultData$CBLVI > 0,]$Commit) # variability information (exclusive)

# Calculate the numbers of changes to artifact-specific and variability information in model artifacts
ccaim <- length(resultData$CMLAI [ ! resultData$CMLAI %in% 0]) # artifact-specific information (non-exclusive; includes ccaime)
ccaime <- length(resultData[resultData$CMLAI > 0 & resultData$CMLVI == 0 & resultData$CCLAI == 0
                            & resultData$CCLVI == 0 & resultData$CBLAI == 0 & resultData$CBLVI == 0,]$Commit) # artifact-specific information (exclusive)
ccvim <- length(resultData$CMLVI [ ! resultData$CMLVI %in% 0]) # variability information (non-exclusive; includes ccvime)
ccvime <- length(resultData[resultData$CMLAI == 0 & resultData$CMLVI > 0 & resultData$CCLAI == 0
                            & resultData$CCLVI == 0 & resultData$CBLAI == 0 & resultData$CBLVI == 0,]$Commit) # variability information (exclusive)

# Create file for saving plot
if (os == "Windows") {
  # We are on Windows, use EMF
  plotFile <- paste(analyzedSpl, "CommitDistributionPerArtifactInformationType.emf", sep = "_")
  plotFile <- paste(saveDir, plotFile, sep="/")
  win.metafile(file = plotFile, width = 12, height = 7)
} else {
  # We are on Linux, use PDF
  plotFile <- paste(analyzedSpl, "CommitDistributionPerArtifactInformationType.pdf", sep = "_")
  plotFile <- paste(saveDir, plotFile, sep="/")
  pdf(file = plotFile, width = 12, height = 7)
}

# Create bar values and labels and plot the bars
commitDistribution <- c(commits, ccaic, ccaice, ccvic, ccvice, ccaib, ccaibe, ccvib, ccvibe, ccaim, ccaime, ccvim, ccvime)
commitDistributionPercentages <- round((commitDistribution/commits)*100, digits = 2)
commitTypeLabels <- c("All", "CCAIC", "CCAICe", "CCVIC", "CCVICe",
                      "CCAIB", "CCAIBe", "CCVIB", "CCVIBe",
                      "CCAIM", "CCAIMe", "CCVIM", "CCVIMe")
commitTypeLabels <- paste(commitTypeLabels, commitDistributionPercentages, sep = "\n")
commitTypeLabels <- paste(commitTypeLabels, "%", sep = "")
yMaxValue <- getMaxY(splName = analyzedSpl, visualizedData = "CDPAIT") # use function and "analyzedSpl" defined in ComVi.R
oldPar <- par(no.readonly = TRUE) # save default parameter settings for reset
par(mar = c(5, 4, 3, 1) + 0.1, xpd = TRUE)
# Define custom colors for bars
commit_col <- rgb(95/255, 95/255, 95/255) # dark gray
ccaim_col <- rgb(104/255, 9/255, 11/255) # dark dark red
ccaime_col <- rgb(170/255, 14/255, 18/255) # dark red
ccvim_col <- rgb(236/255, 32/255, 37/255) # red
ccvime_col <- rgb(245/255, 137/255, 140/255) # light red
ccaic_col <- rgb(31/255, 31/255, 95/255) # dark indigo
ccaice_col <- rgb(51/255, 51/255, 153/255) # indigo
ccvic_col <- rgb(105/255, 105/255, 205/255) # light indigo
ccvice_col <- rgb(177/255, 177/255, 228/255) # light light indigo
ccaib_col <- rgb(35/255, 70/255, 0/255) # dark dark green
ccaibe_col <- rgb(64/255, 128/255, 0/255) # dark green
ccvib_col <- rgb(99/255, 198/255, 0/255) # green
ccvibe_col <- rgb(121/255, 242/255, 0/255) # light green
barColors <- c(commit_col, ccaic_col, ccaice_col, ccvic_col, ccvice_col,
               ccaib_col, ccaibe_col, ccvib_col, ccvibe_col,
               ccaim_col, ccaime_col, ccvim_col, ccvime_col)
bar <- barplot(commitDistribution, col = barColors, ylim = c(0, yMaxValue), xlab = "Type of Commits", ylab = "Number of Commits") # yaxp = c(0, yMaxValue, 10)
# Add the sum and percetage of commits for each type as numbers (text) to bars
text(x = bar, y = commitDistribution, pos = 3, labels = commitDistribution)
text(x = bar, y = 0, pos = 1, labels = commitTypeLabels)
legend("topright", inset = c(0.03, 0), ncol = 1, bty = "n",
       title = expression(bold("Legend")), title.adj = 0,
       legend = c("All = Full number of analyzed commits",
                  "CCAIC = Commit Changing Artifact-specific Information in Code artifacts",
                  "CCAICe = Commit Changing Artifact-specific Information in Code artifacts exclusively",
                  "CCVIC = Commit Changing Variability Information in Code artifacts",
                  "CCVICe = Commit Changing Variability Information in Code artifacts exclusively",
                  "CCAIB = Commit Changing Artifact-specific Information in Build artifacts",
                  "CCAIBe = Commit Changing Artifact-specific Information in Build artifacts exclusively",
                  "CCVIB = Commit Changing Variability Information in Build artifacts",
                  "CCVIBe = Commit Changing Variability Information in Build artifacts exclusively",
                  "CCAIM = Commit Changing Artifact-specific Information in Model artifacts",
                  "CCAIMe = Commit Changing Artifact-specific Information in Model artifacts exclusively",
                  "CCVIM = Commit Changing Variability Information in Model artifacts",
                  "CCVIMe = Commit Changing Variability Information in Model artifacts exclusively"),
       fill = barColors)
dev.off()
par(oldPar) # reset parameters to default