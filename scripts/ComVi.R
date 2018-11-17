# The main Commit Visualization (ComVi) script
#
# This script calls the individual ComVi scripts of which each produces different statistics and plots
# based on the input data defined in this script. This input data has to be the ComAn_Results.tsv file
# created by the Commit Analysis (ComAn) tool.
#
# Author: Christian Kröher

# Function for defining optimal y-axis maximum values for different plots depending on the product line
# param splName = the name of the software product line the result data belongs to, e.g. "coreboot" or "linux"
# param visualitedData = the capital letters of the calling ComVi_* script
getMaxY <- function(splName, visualizedData) {
  maxYValue <- 0
  if (splName == "coreboot" | splName == "Coreboot") {
    if (visualizedData == "CDPAIT") {
      # return max y value for CommitDistributionPerArtifactInformationType for Coreboot
      maxYValue <- 25000
    } else if (visualizedData == "CLSPAT") {
      # return max y value for ChangedLinesSumsPerArtifactType for Coreboot
      maxYValue <- 5000000
    } else if (visualizedData == "CLPATFD") {
      # return max y value for ChangedLinesPerArtifactTypeFrequencyDistribution for Coreboot
      maxYValue <- 20000
    } else if (visualizedData == "FE") {
      # return max y value for FullEvolution for Coreboot
      maxYValue <- 100000000
    }
  }
  if (splName == "linux" | splName == "Linux") {
    if (visualizedData == "CDPAIT") {
      # return max y value for CommitDistributionPerArtifactInformationType for Linux
      maxYValue <- 700000
    } else if (visualizedData == "CLSPAT") {
      # return max y value for ChangedLinesSumsPerArtifactType for Linux
      maxYValue <- 60000000
    } else if (visualizedData == "CLPATFD") {
      # return max y value for ChangedLinesPerArtifactTypeFrequencyDistribution for Coreboot
      maxYValue <- 700000
    }  else if (visualizedData == "FE") {
      # return max y value for FullEvolution for Linux
      maxYValue <- 10000000000
    }
  }
  return(maxYValue)
}

# Get commandline arguments specifying:
#     1. The absolute path to the result data file (ComAn_Results.tsv)
#     2. The absolute path to the output directory
# Example: args <- c("C:/Kroeher/ComAn_Results.tsv", "C:/Kroeher", "Coreboot")
#args <- c("C:/Users/kroeher/Desktop/ComAn-Test/output/coreboot/results/ComAn_Results.tsv", "C:/Users/kroeher/Desktop/ComAn-Test/output/coreboot/results", "Coreboot")
args <- commandArgs(trailingOnly = TRUE)
if (length(args) != 3) {
  stop("Missing absolute paths to:\n\t1. The result data file (ComAn_Results.tsv)\n\t2. The output directory\nOr missing definition of analyzed software product line: 'coreboot' or 'linux'")
}
resultData <- read.delim(args[1], header = TRUE)
saveDir <- args[2]
analyzedSpl <- args[3]
# Further, get operting system to switch between:
#     - EMF-files for saving plots (Windows)
#     - PDF-files for saving plots (Linux)
os <- Sys.info()['sysname']

# Determine path to directory in which this script (and the other scripts) is located. This path is reuqired
# for the source()-commands executing the other scripts. Typically, sourcing does not work if the (full) path
# to the sourced files is missing.
argsNoTrailing <- commandArgs(trailingOnly = FALSE)
fileNameArg <- "--file="
thisScriptName <- sub(fileNameArg, "", argsNoTrailing[grep(fileNameArg, argsNoTrailing)])
scriptPath <- dirname(thisScriptName)

# For the linux kernel, we exclude the initial commit, which introduces the entire Linux kernel version 2.6.12-rc2
# in a single commit. This is the only "real" outlier commit we could identify in both SPLs under analysis
if (analyzedSpl == "linux" | analyzedSpl == "Linux") {
  resultData <- resultData[resultData$Commit!="1da177e", ]
}

if(length(scriptPath) > 0) {
  # (full) path to this script available; prepend this path to sourced script names
  source(paste(scriptPath, "ComVi_CommitDistributionPerInformationType.R", sep = "/"), echo = TRUE)
  source(paste(scriptPath, "ComVi_CommitDistributionPerArtifactInformationType.R", sep = "/"), echo = TRUE)
  source(paste(scriptPath, "ComVi_ChangedLinesPerArtifactType.R", sep = "/"), echo = TRUE)
  source(paste(scriptPath, "ComVi_FullEvolution.R", sep = "/"), echo = TRUE)
  source(paste(scriptPath, "ComVi_ChangedLinesCommitSizesRelation.R", sep = "/"), echo = TRUE)
} else {
  # No (full) path to this script available; execute further script by script names only
  source("ComVi_CommitDistributionPerInformationType.R", echo = TRUE)
  source("ComVi_CommitDistributionPerArtifactInformationType.R", echo = TRUE)
  source("ComVi_ChangedLinesPerArtifactType.R", echo = TRUE)
  source("ComVi_FullEvolution.R", echo = TRUE)
  source("ComVi_ChangedLinesCommitSizesRelation.R", echo = TRUE)
}
