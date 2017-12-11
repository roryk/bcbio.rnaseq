## Deprecation
This is maintained, and should work, but is deprecated in favor of https://github.com/hbc/bcbioRNASeq for most use cases

## Overview
[![Build Status](https://travis-ci.org/roryk/bcbio.rnaseq.png)](http://travis-ci.org/roryk/bcbio.rnaseq) [![bioconda-badge](https://img.shields.io/badge/install%20with-bioconda-brightgreen.svg?style=flat-square)](http://bioconda.github.io)


This repository generates a Rmarkdown report from a bcbio-nextgen RNA-seq run.
The report isn't a finalized analysis and definitely needs a trained person with
exerpience to interpret and tweak it, but it does a lot of the annoying plumbing
work when setting up an analysis. It generates a large amount of useful quality
control information, runs a differential expression analysis with DESeq2 and
sets up using Sleuth or doing a pathway analysis if those options are flagged
on.

## Installation

### Conda install (UNIX only)

```
conda install -c bioconda bcbio-rnaseq
```

### Manual installation

1. Install [leiningen](https://github.com/technomancy/leiningen)
2. Install [pandoc](http://johnmacfarlane.net/pandoc/) (at least v1.12.3)
3. Download the bcbio.repository

```
git clone https://github.com/roryk/bcbio.rnaseq
```

4. Install the necessary libraries

```
cd bcbio.rnaseq
Rscript resources/scripts/install_libraries.R
```

5. Make an executable.

```bash
make
```

This will make an executable in `bin/bcbio-rnaseq` that you can move to wherever
you want and run.


## Generating a bcbio-nextgen report
This produces a Rmd file with a report of a bcbio-nextgen run. If you run it
without passing a formula, it will just generate a quality-control report. If
you pass a formula like this:

```
bcbio-rnaseq summarize path-to-project-summary-yaml -f "~batch+panel"
```

It will run a DESeq2 analysis with the specified model and prepare reports
of all pairwise comparisons for the last factor in the model. An example
report is available [here][example-summary].

Other options you can pass are `--sleuth` which will set up a
[sleuth](https://github.com/pachterlab/sleuth) object to perform transcript
level differential expression. Passing the `--organism` parameter with either
`mouse` or `human` will set up functions to run clusterProfiler GSEA and gene
enrichment analyses using the results of DESeq2 objects.


[bcbio-nextgen]: https://github.com/chapmanb/bcbio-nextgen
[metadata]: https://github.com/chapmanb/bcbio-nextgen/blob/master/docs/contents/configuration.rst#sample-information
[ERCC]: http://www.lifetechnologies.com/order/catalog/product/4456740
[example-summary]: https://rawgit.com/roryk/bcbio.rnaseq/master/docs/qc-summary.html

