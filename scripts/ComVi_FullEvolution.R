# The Commit Visualization (ComVi) script for calculating and visualizing the full evolution of
# one of the target software product lines (SPLs). This includes:
#     - Creation of a graphical "per year"-representation of the numbers of:
#         * Analyzed commits
#         * Changed Code Lines containing Artifact-specific Information (CCLAI)
#         * Changed Code Lines containing Variability Information (CCLVI)
#         * Changed Build Lines containing Artifact-specific Information (CBLAI)
#         * Changed Build Lines containing Variability Information (CBLVI)
#         * Changed Model Lines containing Artifact-specific Information (CMLAI)
#         * Changed Model Lines containing Variability Information (CMLVI)
#     - Creation of a table (saved as *.csv file) containing:
#         * The numbers used for (but not included in) the graphical represention
#         * Further statistical evalutions for concluding on the evolution of the respective SPL
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

# Create additional column for "per year"-summary of data
resultData$Year <- format(as.Date(resultData$Date, format = "%Y/%m/%d"), "%Y")

# Create tables containing "per year"-numbers of:
#     - Analyzed commits
#     - Changed Code Lines containing Artifact-specific Information (CCLAI)
#     - Changed Code Lines containing Variability Information (CCLVI)
#     - Changed Build Lines containing Artifact-specific Information (CBLAI)
#     - Changed Build Lines containing Variability Information (CBLVI)
#     - Changed Model Lines containing Artifact-specific Information (CMLAI)
#     - Changed Model Lines containing Variability Information (CMLVI)
commitsPerYearTable <- table(resultData$Year)
cclaiPerYearTable <- aggregate(x = resultData["CCLAI"], by = list(Year = resultData$Year), sum)
cclviPerYearTable <- aggregate(x = resultData["CCLVI"], by = list(Year = resultData$Year), sum)
cblaiPerYearTable <- aggregate(x = resultData["CBLAI"], by = list(Year = resultData$Year), sum)
cblviPerYearTable <- aggregate(x = resultData["CBLVI"], by = list(Year = resultData$Year), sum)
cmlaiPerYearTable <- aggregate(x = resultData["CMLAI"], by = list(Year = resultData$Year), sum)
cmlviPerYearTable <- aggregate(x = resultData["CMLVI"], by = list(Year = resultData$Year), sum)

# -------------------------------------------------------------------------------------------------------------------- #
# --------------------------------------- Creation of graphical representation --------------------------------------- #
# Create file for saving plot
if (os == "Windows") {
  # We are on Windows, use EMF
  plotFile <- paste(analyzedSpl, "FullEvolution.emf", sep = "_")
  plotFile <- paste(saveDir, plotFile, sep="/")
  win.metafile(file = plotFile, width = 12, height = 7)
} else {
  # We are on Linux, use PDF
  plotFile <- paste(analyzedSpl, "FullEvolution.pdf", sep = "_")
  plotFile <- paste(saveDir, plotFile, sep="/")
  pdf(file = plotFile, width = 12, height = 7)
}

yMaxValue <- getMaxY(splName = analyzedSpl, visualizedData = "FE") # use function and "analyzedSpl" defined in ComVi.R

# Define custome colors, line size, and point types for the different plot elements
lineWidth <- 2 # Common line width for all lines
barFill <- rgb(217/255, 217/255, 217/255) # Analyzed commits bars fill color = light gray
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
# Plot bars illustrating the sum of commits for each year
bar <- barplot(commitsPerYearTable, col = barFill, ylim = c(1, yMaxValue), log = "y",
               xlab = "Year", ylab = "Number of Commits (bars) / Changed Lines (lines)")
# Add the sum of commits for each year as number (text) to bars
text(x = bar, y = 1, pos = 3, labels = commitsPerYearTable)
# Add lines illustrating the sum of changed lines of each category for each year
lines(x = bar, y = cclaiPerYearTable$CCLAI, type = "b", lty = ai_lty, lwd = lineWidth,
      pch = cclai_pch, col = c_col, xaxt = "n", yaxt = "n", xlab = "", ylab = "")
lines(x = bar, y = cclviPerYearTable$CCLVI, type = "b", lty = vi_lty, lwd = lineWidth,
      pch = cclvi_pch, col = c_col, xaxt = "n", yaxt = "n", xlab = "", ylab = "")
lines(x = bar, y = cblaiPerYearTable$CBLAI, type = "b", lty = ai_lty, lwd = lineWidth,
      pch = cblai_pch, col = b_col, xaxt = "n", yaxt = "n", xlab = "", ylab = "")
lines(x = bar, y = cblviPerYearTable$CBLVI, type = "b", lty = vi_lty, lwd = lineWidth,
      pch = cblvi_pch, col = b_col, xaxt = "n", yaxt = "n", xlab = "", ylab = "")
lines(x = bar, y = cmlaiPerYearTable$CMLAI, type = "b", lty = ai_lty, lwd = lineWidth,
      pch = cmlai_pch, col = m_col, xaxt = "n", yaxt = "n", xlab = "", ylab = "")
lines(x = bar, y = cmlviPerYearTable$CMLVI, type = "b", lty = vi_lty, lwd = lineWidth,
      pch = cmlvi_pch, col = m_col, xaxt = "n", yaxt = "n", xlab = "", ylab = "")
# Add the legend to the plot
legend("topleft", inset = c(0.01, -0.01), ncol = 2, bty = "n",
       title = expression(bold("Legend")), title.adj = 0,
       legend = c("Analyzed commits",
                  "Changed code lines (artifact-specific information)",
                  "Changed code lines (variability information)",
                  "Changed build lines (artifact-specific information)",
                  "Changed build lines (variability information)",
                  "Changed model lines (artifact-specific information)",
                  "Changed model lines (variability information)"),
       col = c("white", c_col, c_col, b_col, b_col, m_col, m_col),
       fill = c(barFill, rgb(0,0,0,alpha=0), rgb(0,0,0,alpha=0), rgb(0,0,0,alpha=0),
                rgb(0,0,0,alpha=0), rgb(0,0,0,alpha=0), rgb(0,0,0,alpha=0)), 
       border = c("black", rgb(0,0,0,alpha=0), rgb(0,0,0,alpha=0), rgb(0,0,0,alpha=0),
                  rgb(0,0,0,alpha=0), rgb(0,0,0,alpha=0), rgb(0,0,0,alpha=0)),
       lty = c(-1, ai_lty, vi_lty, ai_lty, vi_lty, ai_lty, vi_lty),
       lwd = c(1, lineWidth, lineWidth, lineWidth, lineWidth, lineWidth, lineWidth),
       pch = c(22, cclai_pch, cclvi_pch, cblai_pch, cblvi_pch, cmlai_pch, cmlvi_pch))
dev.off()
# --------------------------------------- Creation of graphical representation --------------------------------------- #
# -------------------------------------------------------------------------------------------------------------------- #


# -------------------------------------------------------------------------------------------------------------------- #
# ---------------------------------------- Creation of statistics and tables ----------------------------------------- #
commitsPerYearTable <- as.data.frame(commitsPerYearTable)
names(commitsPerYearTable) <- c("Year", "Commits")

evolutionPerYearTable <- merge(commitsPerYearTable, cclaiPerYearTable, by = "Year")
evolutionPerYearTable <- merge(evolutionPerYearTable, cclviPerYearTable, by = "Year")
evolutionPerYearTable <- merge(evolutionPerYearTable, cblaiPerYearTable, by = "Year")
evolutionPerYearTable <- merge(evolutionPerYearTable, cblviPerYearTable, by = "Year")
evolutionPerYearTable <- merge(evolutionPerYearTable, cmlaiPerYearTable, by = "Year")
evolutionPerYearTable <- merge(evolutionPerYearTable, cmlviPerYearTable, by = "Year")

evoNumbersCsvFile <- paste(analyzedSpl, "FullEvolutionNumbers.csv", sep="_")
evoNumbersCsvFile <- paste(saveDir, evoNumbersCsvFile, sep="/")
write.csv(evolutionPerYearTable, file = evoNumbersCsvFile, row.names = FALSE)


# In "resultData":
# Column No    Category
#     4         CCLAI
#     5         CCLVI
#     7         CBLAI
#     8         CBLVI
#    10         CMLAI
#    11         CMLVI

# ---- Calculations based on analyzed data (unmodified) --- #
# Calculate sum for each category (except for "Year" and "Commits")
sums <- sapply(resultData[, c(4, 5, 7, 8, 10, 11)], sum)
# Calculate minimum value for each category (except for "Year" and "Commits")
mins <- sapply(resultData[, c(4, 5, 7, 8, 10, 11)], min)
# Calculate maximum value for each category (except for "Year" and "Commits")
maxs <- sapply(resultData[, c(4, 5, 7, 8, 10, 11)], max)
# Calculate mean for each category (except for "Year" and "Commits") over full commit set (not per year)
means <- sapply(resultData[, c(4, 5, 7, 8, 10, 11)], mean)
# Calculate median for each category (except for "Year" and "Commits") over full commit set (not per year)
medians <- sapply(resultData[, c(4, 5, 7, 8, 10, 11)], median)
# Standard deviation for each category (except for "Year" and "Commits") over full commit set (not per year)
sds  <- sapply(resultData[, c(4, 5, 7, 8, 10, 11)], sd)
# Create complete table for writing .csv-file
evolutionStatisticsTable <- data.frame(type = names(resultData)[c(4, 5, 7, 8, 10, 11)])
evolutionStatisticsTable <- cbind(evolutionStatisticsTable, sums, mins, maxs, means, medians, sds)
# Write complete table to .csv-file
evoStatsCsvFile <- paste(analyzedSpl, "FullEvolutionStatistics_unmodified.csv", sep="_")
evoStatsCsvFile <- paste(saveDir, evoStatsCsvFile, sep="/")
write.csv(evolutionStatisticsTable, file = evoStatsCsvFile, row.names = FALSE)
# ---- Calculations based on analyzed data (unmodified) --- #

# ---- Calculations based on analyzed data excluding "0"s (non-affecting) commits for each category --- #
# Calculate sum for each category not required as exclusion of non-affecting commits does not change these values
#     Reuse "sums"-vector from above
# Calculate minimum value for each category (except for "Year" and "Commits")
cclaiMin <- min(resultData$CCLAI [resultData$CCLAI > 0])
cclviMin <- min(resultData$CCLVI [resultData$CCLVI > 0])
cblaiMin <- min(resultData$CBLAI [resultData$CBLAI > 0])
cblviMin <- min(resultData$CBLVI [resultData$CBLVI > 0])
cmlaiMin <- min(resultData$CMLAI [resultData$CMLAI > 0])
cmlviMin <- min(resultData$CMLVI [resultData$CMLVI > 0])
mins <- c(cclaiMin, cclviMin, cblaiMin, cblviMin, cmlaiMin, cmlviMin)
# Calculate maximum value for each category not required as exclusion of non-affecting commits does not change these values
#     Reuse "maxs"-vector from above
# Calculate mean for each category (except for "Year" and "Commits") over full commit set (not per year)
cclaiMean <- mean(resultData$CCLAI [resultData$CCLAI > 0])
cclviMean <- mean(resultData$CCLVI [resultData$CCLVI > 0])
cblaiMean <- mean(resultData$CBLAI [resultData$CBLAI > 0])
cblviMean <- mean(resultData$CBLVI [resultData$CBLVI > 0])
cmlaiMean <- mean(resultData$CMLAI [resultData$CMLAI > 0])
cmlviMean <- mean(resultData$CMLVI [resultData$CMLVI > 0])
means <- c(cclaiMean, cclviMean, cblaiMean, cblviMean, cmlaiMean, cmlviMean)
# Calculate median for each category (except for "Year" and "Commits") over full commit set (not per year)
cclaiMedian <- median(resultData$CCLAI [resultData$CCLAI > 0])
cclviMedian <- median(resultData$CCLVI [resultData$CCLVI > 0])
cblaiMedian <- median(resultData$CBLAI [resultData$CBLAI > 0])
cblviMedian <- median(resultData$CBLVI [resultData$CBLVI > 0])
cmlaiMedian <- median(resultData$CMLAI [resultData$CMLAI > 0])
cmlviMedian <- median(resultData$CMLVI [resultData$CMLVI > 0])
medians <- c(cclaiMedian, cclviMedian, cblaiMedian, cblviMedian, cmlaiMedian, cmlviMedian)
# Standard deviation for each category (except for "Year" and "Commits") over full commit set (not per year)
cclaiSd <- sd(resultData$CCLAI [resultData$CCLAI > 0])
cclviSd <- sd(resultData$CCLVI [resultData$CCLVI > 0])
cblaiSd <- sd(resultData$CBLAI [resultData$CBLAI > 0])
cblviSd <- sd(resultData$CBLVI [resultData$CBLVI > 0])
cmlaiSd <- sd(resultData$CMLAI [resultData$CMLAI > 0])
cmlviSd <- sd(resultData$CMLVI [resultData$CMLVI > 0])
sds <- c(cclaiSd, cclviSd, cblaiSd, cblviSd, cmlaiSd, cmlviSd)
# Create complete table for writing .csv-file
evolutionStatisticsTable <- data.frame(type = names(resultData)[c(4, 5, 7, 8, 10, 11)])
evolutionStatisticsTable <- cbind(evolutionStatisticsTable, sums, mins, maxs, means, medians, sds)
# Write complete table to .csv-file
evoStatsCsvFile <- paste(analyzedSpl, "FullEvolutionStatistics_excluded-non-affecting.csv", sep="_")
evoStatsCsvFile <- paste(saveDir, evoStatsCsvFile, sep="/")
write.csv(evolutionStatisticsTable, file = evoStatsCsvFile, row.names = FALSE)
# ---- Calculations based on analyzed data excluding "0"s (non-affecting) commits for each category --- #

# ---------------------------------------- Creation of statistics and tables ----------------------------------------- #
# -------------------------------------------------------------------------------------------------------------------- #