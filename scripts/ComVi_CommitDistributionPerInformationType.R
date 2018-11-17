# The Commit Visualization (ComVi) script for calculating and visualizing the numbers of:
#     - Commits Changing Artifact-specific Information exclusively (CCAIe)
#     - Commits Changing Variability Information exclusively (CCVIe)
#     - Commits Changing Artifact-specific and Variability Information (CCAVI)
#     - Commits Changing Other Artifacts exclusively (CCOAe)
#       (these are commits introducing changes to the documentation or to files out of scope of the report)
#
# Required input (provided by main ComVi script):
#     - os: the operating system for determining the type of file to save the plots
#     - resultData: data frame containing the result data from ComAn_Results.tsv
#     - saveDir: the absolute path to the output directory (for saving results of this script)
#     - analyzedSpl: the name of the SPl the "resulData" belongs to (for naming result files of this script)
#
# Author: Christian Kröher


# Calculate the numbers for the four different categories (CCAIe, CCVIe, CCAVI, CCOAe)
ccaie <- length(resultData[(resultData$CMLAI > 0 | resultData$CCLAI > 0 | resultData$CBLAI > 0)
    & resultData$CMLVI == 0 & resultData$CCLVI == 0 & resultData$CBLVI == 0,]$Commit)
ccvie <- length(resultData[(resultData$CMLVI > 0 | resultData$CCLVI > 0 | resultData$CBLVI > 0)
    & resultData$CMLAI == 0 & resultData$CCLAI == 0 & resultData$CBLAI == 0,]$Commit)
ccavi <- length(resultData[(resultData$CMLVI > 0 | resultData$CCLVI > 0 | resultData$CBLVI > 0)
    & (resultData$CMLAI > 0 | resultData$CCLAI > 0 | resultData$CBLAI > 0),]$Commit)
ccoae <- length(resultData[resultData$CMLVI == 0 & resultData$CCLVI == 0 & resultData$CBLVI == 0
                            & resultData$CMLAI == 0 & resultData$CCLAI == 0 & resultData$CBLAI == 0,]$Commit)

# Define custom colors for pie parts
ccaie_col <- rgb(121/255, 92/255, 95/255) # brown
ccvie_col <- rgb(166/255, 150/255, 88/255) # tan
ccavi_col <- rgb(217/255, 178/255, 111/255) # tan (darker)
ccoae_col <- rgb(95/255, 95/255, 95/255) # dark gray
colors <- c(ccaie_col, ccvie_col, ccavi_col, ccoae_col)

# Create file for saving plot
if (os == "Windows") {
  # We are on Windows, use EMF
  plotFile <- paste(analyzedSpl, "CommitDistributionPerInformationType.emf", sep = "_")
  plotFile <- paste(saveDir, plotFile, sep="/")
  win.metafile(file = plotFile)
} else {
  # We are on Linux, use PDF
  plotFile <- paste(analyzedSpl, "CommitDistributionPerInformationType.pdf", sep = "_")
  plotFile <- paste(saveDir, plotFile, sep="/")
  pdf(file = plotFile)
}

# Create pie parts and labels and plot the pie
pieParts <- c(ccaie, ccvie, ccavi, ccoae)
piePartsPercentages <- round(pieParts/sum(pieParts)*100, digits = 2)
pieLabels <- c("CCAIe", "CCVIe", "CCAVI", "CCOAe")
pieLabels <- paste(pieLabels, ": ", pieParts, "\n~", piePartsPercentages, "%", sep = "")
oldPar <- par(no.readonly = TRUE) # save default parameter settings for reset
par(mar = c(7, 1, 1, 1) + 0.1, xpd = TRUE)
pie(pieParts, labels = pieLabels, radius = 0.8, cex = 1, col = colors)
legend("bottomleft", inset = c(0, -0.2), ncol = 1, bty = "n",
       title = expression(bold("Legend")), title.adj = 0,
       legend = c("CCAIe = Commits Changing Artifact-specific Information exclusively",
                  "CCVIe = Commits Changing Variability Information exclusively",
                  "CCAVI = Commits Changing Artifact-specific and Variability Information",
                  "CCOAe = Commits Changing Other Artifacts exclusively"),
       fill = colors)
if (os == "Windows") {
  # Only required if EMF-file is created; PDF is closed automatically by pie-command
  # as this is the standard plotting device for that plot type
  dev.off()
}
par(oldPar) # reset parameters to default