# VariabilityChangeAnalyzer
ComAnI plug-in for analyzing the intensity of variability information changes in KBuild-based Software Product Lines

## Requirements
If visualizing the analysis results is desired (see configuration paramters below), R needs to be installed and the packages
"Hmisc" and "nortest" need to be available.

## Support
Operating system: all

Version control system: "git" or "svn"

## Plug-in-specific Configuration Parameters
```Properties
The name of the SPL from which the commits are analyzed. The definition of this property is optional and only
necessary, if the visualization of analysis results is desired. In that case, valid values are [Cc]oreboot or
[Ll]inux. All other values result in an AnalysisSetupException. Hence, this property indirectly controls the
visualization.

Type: optional
Default value: none
Related parameters: none
analysis.variability_change_analyzer.target_spl = <[Cc]oreboot|[Ll]inux>
```
