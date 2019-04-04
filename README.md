# VariabilityChangeAnalyzer
This [ComAnI](https://github.com/CommitAnalysisInfrastructure/ComAnI) plug-in realizes an analyzer for identifying the intensity of variability information changes in KBuild-based Software Product Lines [1]. It actually wraps the analysis algorithm of the [ComAn tool set](https://github.com/SSE-LinuxAnalysis/ComAn), which is able to identify this intensity for the [Linux kernel](https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git) [1,2,3] and the [Coreboot firmware](https://www.coreboot.org/downloads.html) [3]. Wrapping this algorithm as ComAnI plug-in enables its application to other software product lines, e.g., those hosted in a SVN repository instead of a Git-based one.

*Main class name:* `net.ssehub.comani.analysis.variabilitychange.core.VariabilityChangeAnalyzer`

*Support:*
- Operating system: all
- Version control system: “git” or “svn”

For more information on how to use ComAnI and its plug-ins, we recommend reading the [ComAnI Guide](https://github.com/CommitAnalysisInfrastructure/ComAnI/blob/master/guide/ComAnI_Guide.pdf).

## Installation
Download the [VariabilityChangeAnalyzer.jar](/release/VariabilityChangeAnalyzer.jar) file from the release directory and save it to the ComAnI plug-ins directory on your machine. This directory is the one specified as `core.plugins_dir` in the configuration file of a particular ComAnI instance.

*Requirements:*
- The [ComAnI infrastructure](https://github.com/CommitAnalysisInfrastructure/ComAnI) has to be installed to execute this plug-in as the analyzer of a particular ComAnI instance
- If the visualization of the analysis results is desired (see configuration parameter below):
  - The [R environment](https://www.r-project.org/) has to be installed and globally executable
  - The R packages [Hmisc](https://cran.r-project.org/web/packages/Hmisc/index.html) and [nortest](https://cran.r-project.org/web/packages/nortest/index.html) have to be installed as part of the R environment

## Execution
This plug-in is not a standalone tool, but only executable as the analyzer of a particular ComAnI instance. Therefore, it has to be defined in the configuration file via its fully qualified main class name as follows:

`analysis.analyzer = net.ssehub.comani.analysis.variabilitychange.core.VariabilityChangeAnalyzer`

*Plug-in-specific configuration parameter(s):*

The name of the SPL from which the commits are analyzed. The definition of this property is optional and only necessary, if the visualization of analysis results is desired. In that case, valid values are "[Cc]oreboot" or "[Ll]inux". All other values result in an `AnalysisSetupException`. Hence, this property indirectly controls the visualization.
```Properties
Type: optional
Default value: none
Related parameters: none
analysis.variability_change_analyzer.target_spl = <[Cc]oreboot|[Ll]inux>
```

The regular expressions for identifying variability model, code, and build files. The definition of all three properties is mandatory and must follow the definition of [Java regular expressions](https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html). The absence of these definitions causes an `AnalysisSetupException`. An example is given below for each of the parameters.
```Properties
Type: mandatory
Default value: none
Related parameters: none
analysis.variability_change_analyzer.vm_files_regex = <Java-Regex>
```
Example: `analysis.variability_change_analyzer.vm_files_regex = .*/Kconfig((\\.|\\-|\\_|\\+|\\~).*)?`
```Properties
Type: mandatory
Default value: none
Related parameters: none
analysis.variability_change_analyzer.code_files_regex = <Java-Regex>
```
Example: `analysis.variability_change_analyzer.code_files_regex = .*/.*\\.[hcS]((\\.|\\-|\\_|\\+|\\~).*)?`
```Properties
Type: mandatory
Default value: none
Related parameters: none
analysis.variability_change_analyzer.build_files_regex = <Java-Regex>
```
Example: `analysis.variability_change_analyzer.build_files_regex = .*/(Makefile|Kbuild)((\\.|\\-|\\_|\\+|\\~).*)?`


## License
This project is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).

## Acknowledgments
This work is partially supported by the ITEA3 project [REVaMP²](http://www.revamp2-project.eu/), funded by the [BMBF (German Ministry of Research and Education)](https://www.bmbf.de/) under grant 01IS16042H.

A special thanks goes to the developers of [KernelHaven](https://github.com/KernelHaven/): Adam Krafczyk, Sascha El-Sharkawy, Moritz Flöter, Alice Schwarz, Kevin Stahr, Johannes Ude, Manuel Nedde, Malek Boukhari, and Marvin Forstreuter. Their architecture and core concepts significantly inspired the development of this project. In particular, the mechanisms for file-based configuration of the infrastructure and the plug-ins as well as loading and executing individual plug-ins are adopted in this work.
## References
[1] Christian Kröher, Lea Gerling and Klaus Schmid. [Identifying the Intensity of Variability Changes in Software Product Line Evolution](https://sse.uni-hildesheim.de/forschung/publikationen/publikation-einzelansicht/?lsfid=11762&cHash=c331c5cf449cd662ce89c638ca2e5e69). In Proceedings of the 22nd International Systems and Software Product Line Conference, Vol. 1, pp. 54-64, ACM, 2018.

[2] Christian Kröher and Klaus Schmid. [A Commit-Bases Analysis of Software Product Line Evolution: Two Case Studies](https://sse.uni-hildesheim.de/en/research/projects/revamp2/spl-evolution/). Report No. 2/2017, SSE 2/17/E, 2017.

[3] Christian Kröher and Klaus Schmid. [Towards a Better Understanding of Software Product Line Evolution](https://dl.gi.de/handle/20.500.12116/4685). In Softwaretechnik-Trends, Vol. 37:2. Gesellschaft für Informatik e.V., Fachgruppe PARS, Berlin, Germany, pp. 40–41, 2017.
