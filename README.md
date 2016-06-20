## Overview
[![Build Status](https://secure.travis-ci.org/roryk/bcbio.rnaseq.png)](http://travis-ci.org/roryk/bcbio.rnaseq) ![DOI](https://zenodo.org/badge/3658/roryk/bcbio.rnaseq.png)


There are a large number of algorithms designed to call differential
events from RNA-seq data at the gene, isoform and splicing-event
level. It would be great if when a new caller comes out, there was a
way to automatically place it in the context of the other
callers. Every now and then a review article comes out which compares
a subset of the tools against each other. This project aims to do provide
a tool to do that automatically.

## Quickstart

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

This will make an executable in bin/bcbio-rnaseq that you can move to wherever
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
