library(Hmisc) # Package for calculating correlations
library(nortest) # Package for performing Anderson-Darling normality tests

# The Commit Visualization (ComVi) script for calculating and visualizing the relation between the numbers
# of changed lines and the sizes of commits. The size of a commit is the sum of all changed lines, while the
# number of changed lines to be related to this sum is one of the following:
#     - Changed Code Lines containing Artifact-specific Information (CCLAI)
#     - Changed Code Lines containing Variability Information (CCLVI)
#     - Changed Build Lines containing Artifact-specific Information (CBLAI)
#     - Changed Build Lines containing Variability Information (CBLVI)
#     - Changed Model Lines containing Artifact-specific Information (CMLAI)
#     - Changed Model Lines containing Variability Information (CMLVI)
#
# Required input (provided by main ComVi script):
#     - os: the operating system for determining the type of file to save the plots
#     - resultData: data frame containing the result data from ComAn_Results.tsv
#     - saveDir: the absolute path to the output directory (for saving results of this script)
#     - analyzedSpl: the name of the SPl the "resulData" belongs to (for naming result files of this script)
#
# Author: Christian Kröher


# Adjust y-axis numbers (no scientific)
options(scipen=1000)

# Calculate sum of all changed lines for each commit
summedResultData <- transform(resultData, sum = rowSums(resultData[, c(4, 5, 7, 8, 10, 11)]))
# Order commits by ascending sums of changed lines
summedOrderedResultData <- summedResultData[order(summedResultData$sum),]
# Create vector of commit sizes (sum of all changed lines) for x-axis
commitSizes <- summedOrderedResultData$sum

# Create file for saving plot
if (os == "Windows") {
  # We are on Windows, use EMF
  plotFile <- paste(analyzedSpl, "ChangedLinesCommitSizesRelation.emf", sep = "_")
  plotFile <- paste(saveDir, plotFile, sep="/")
  win.metafile(file = plotFile, width = 9, height = 10)
} else {
  # We are on Linux, use PDF
  plotFile <- paste(analyzedSpl, "ChangedLinesCommitSizesRelation.pdf", sep = "_")
  plotFile <- paste(saveDir, plotFile, sep="/")
  pdf(file = plotFile, width = 9, height = 10)
}

# Combine the following plots into a single one with 3 rows and 2 columns
oldPar <- par(no.readonly = TRUE) # save default parameter settings for reset
par(mfrow = c(3, 2))

# Define custome colors, line size, and point types for the different plot elements
lineWidth <- 2 # Common line width for all lines
ai_lty <- 1 # Changed model, code, and build lines containing artifact-specific information line type = solid
vi_lty <- 2 # Changed model, code, and build lines containing variability information line type = dashed
cclai_pch <- 1 # Changed code lines (artifact-specific) point character = empty circle
cclvi_pch <- 16 # Changed code lines (variability) point character = filled circle
c_col <- rgb(51/255, 51/255, 153/255) # Code color = indigo
cblai_pch <- 2 # Changed build lines (artifact-specific) point character = empty triangle
cblvi_pch <- 17 # Changed build lines (variability) point character = filled triangle
b_col <- rgb(64/255, 128/255, 0/255) # Build color = dark green
cmlai_pch <- 0 # Changed model lines (artifact-specific) point character = empty square
cmlvi_pch <- 15 # Changed model lines (variability) point character = filled square
m_col <- rgb(170/255, 14/255, 18/255) # Model color = dark red

# Plot the CCLAI against the commit sizes (sum of all changed lines)
p <- plot(x = commitSizes, xlim = c(1, max(commitSizes)), ylim = c(1, max(summedOrderedResultData$CCLAI)),
          log = "xy", type = "n", xlab = "Size of Commit (sum of changed lines)", ylab = "Number of Lines", main = "Changed Code Lines (artifact-specific information)")
lines(x = commitSizes, y = summedOrderedResultData$CCLAI, type = "p", lty = ai_lty, lwd = lineWidth, pch = cclai_pch,
      col = c_col, xaxt = "n", yaxt = "n", xlab = "", ylab = "")
corr <- rcorr(commitSizes, summedOrderedResultData$CCLAI, type="spearman")
r <- "r:"
r <- paste(r, round(as.numeric(corr$r[,1])[2], digits = 2), sep = " ")
p <- "p:"
p <- paste(p, round(as.numeric(corr$P[,1])[2], digits = 2), sep = " ")
legend("right", inset = c(0.01, -0.01), ncol = 1, bty = "n", legend = c(r, p))

# Plot the CCLVI against the commit sizes (sum of all changed lines)
p <- plot(x = commitSizes, xlim = c(1, max(commitSizes)), ylim = c(1, max(summedOrderedResultData$CCLVI)),
          log = "xy", type = "n", xlab = "Size of Commit (sum of changed lines)", ylab = "Number of Lines", main = "Changed Code Lines (variability information)")
lines(x = commitSizes, y = summedOrderedResultData$CCLVI, type = "p", lty = vi_lty, lwd = lineWidth, pch = cclvi_pch,
      col = c_col, xaxt = "n", yaxt = "n", xlab = "", ylab = "")
corr <- rcorr(commitSizes, summedOrderedResultData$CCLVI, type="spearman")
r <- "r:"
r <- paste(r, round(as.numeric(corr$r[,1])[2], digits = 2), sep = " ")
p <- "p:"
p <- paste(p, round(as.numeric(corr$P[,1])[2], digits = 2), sep = " ")
legend("right", inset = c(0.01, -0.01), ncol = 1, bty = "n", legend = c(r, p))

# Plot the CBLAI against the commit sizes (sum of all changed lines)
p <- plot(x = commitSizes, xlim = c(1, max(commitSizes)), ylim = c(1, max(summedOrderedResultData$CBLAI)),
          log = "xy", type = "n", xlab = "Size of Commit (sum of changed lines)", ylab = "Number of Lines", main = "Changed Build Lines (artifact-specific information)")
lines(x = commitSizes, y = summedOrderedResultData$CBLAI, type = "p", lty = ai_lty, lwd = lineWidth, pch = cblai_pch,
      col = b_col, xaxt = "n", yaxt = "n", xlab = "", ylab = "")
corr <- rcorr(commitSizes, summedOrderedResultData$CBLAI, type="spearman")
r <- "r:"
r <- paste(r, round(as.numeric(corr$r[,1])[2], digits = 2), sep = " ")
p <- "p:"
p <- paste(p, round(as.numeric(corr$P[,1])[2], digits = 2), sep = " ")
legend("right", inset = c(0.01, -0.01), ncol = 1, bty = "n", legend = c(r, p))

# Plot the CBLVI against the commit sizes (sum of all changed lines)
p <- plot(x = commitSizes, xlim = c(1, max(commitSizes)), ylim = c(1, max(summedOrderedResultData$CBLVI)),
          log = "xy", type = "n", xlab = "Size of Commit (sum of changed lines)", ylab = "Number of Lines", main = "Changed Build Lines (variability information)")
lines(x = commitSizes, y = summedOrderedResultData$CBLVI, type = "p", lty = vi_lty, lwd = lineWidth, pch = cblvi_pch,
      col = b_col, xaxt = "n", yaxt = "n", xlab = "", ylab = "")
corr <- rcorr(commitSizes, summedOrderedResultData$CBLVI, type="spearman")
r <- "r:"
r <- paste(r, round(as.numeric(corr$r[,1])[2], digits = 2), sep = " ")
p <- "p:"
p <- paste(p, round(as.numeric(corr$P[,1])[2], digits = 2), sep = " ")
legend("right", inset = c(0.01, -0.01), ncol = 1, bty = "n", legend = c(r, p))

# Plot the CMLAI against the commit sizes (sum of all changed lines)
plot(x = commitSizes, xlim = c(1, max(commitSizes)), ylim = c(1, max(summedOrderedResultData$CMLAI)),
     log = "xy", type = "n", xlab = "Size of Commit (sum of changed lines)", ylab = "Number of Lines", main = "Changed Model Lines (artifact-specific information)")
lines(x = commitSizes, y = summedOrderedResultData$CMLAI, type = "p", lty = ai_lty, lwd = lineWidth, pch = cmlai_pch,
      col = m_col, xaxt = "n", yaxt = "n", xlab = "", ylab = "")
corr <- rcorr(commitSizes, summedOrderedResultData$CMLAI, type="spearman")
r <- "r:"
r <- paste(r, round(as.numeric(corr$r[,1])[2], digits = 2), sep = " ")
p <- "p:"
p <- paste(p, round(as.numeric(corr$P[,1])[2], digits = 2), sep = " ")
legend("right", inset = c(0.01, -0.01), ncol = 1, bty = "n", legend = c(r, p))

# Plot the CMLVI against the commit sizes (sum of all changed lines)
p <- plot(x = commitSizes, xlim = c(1, max(commitSizes)), ylim = c(1, max(summedOrderedResultData$CMLVI)),
          log = "xy", type = "n", xlab = "Size of Commit (sum of changed lines)", ylab = "Number of Lines", main = "Changed Model Lines (variability information)")
lines(x = commitSizes, y = summedOrderedResultData$CMLVI, type = "p", lty = vi_lty, lwd = lineWidth, pch = cmlvi_pch,
      col = m_col, xaxt = "n", yaxt = "n", xlab = "", ylab = "")
corr <- rcorr(commitSizes, summedOrderedResultData$CMLVI, type="spearman")
r <- "r:"
r <- paste(r, round(as.numeric(corr$r[,1])[2], digits = 2), sep = " ")
p <- "p:"
p <- paste(p, round(as.numeric(corr$P[,1])[2], digits = 2), sep = " ")
legend("right", inset = c(0.01, -0.01), ncol = 1, bty = "n", legend = c(r, p))

dev.off()
par(oldPar) # reset parameters to default

# Perform Anderson-Darling normality test for each type of changed lines
ad.test(summedOrderedResultData$CCLAI)
ad.test(summedOrderedResultData$CCLVI)
ad.test(summedOrderedResultData$CBLAI)
ad.test(summedOrderedResultData$CBLVI)
ad.test(summedOrderedResultData$CMLAI)
ad.test(summedOrderedResultData$CMLVI)