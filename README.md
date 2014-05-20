<head>
    <script type="text/javascript"
            src="http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML">
    </script>
</head>

## Overview
[![Build Status](https://secure.travis-ci.org/roryk/bcbio.rnaseq.png)](http://travis-ci.org/roryk/bcbio.rnaseq) ![DOI](https://zenodo.org/badge/3658/roryk/bcbio.rnaseq.png)


There are a huge number of algorithms designed to call differential
events from RNA-seq data at the gene, isoform and splicing-event
level. It would be great if when a new caller comes out, there was a
way to automatically place it in the context of the other
callers. Every now and then a review article comes out which compares
a subset of the tools against each other. This project aims to do provide
a tool to do that automatically.

## Quickstart

1. Install [leiningen](https://github.com/technomancy/leiningen)
2. Download the bcbio.repository

```
git clone https://github.com/roryk/bcbio.rnaseq
```

3. Install the necessary libraries

```
cd bcbio.rnaseq
Rscript resources/scripts/install_libraries.R
```

4. Run a simulation of sample size 5

```
lein run simulate -s 5 -d output_dir
```

In the **output_dir** you will find plots comparing the output from
the callers against the set of simulated true positives. For example
here are concordance plots vs. the set of true simulated RNA-seq genes for
an experiment with 3 replicates:

![](https://raw.githubusercontent.com/roryk/bcbio.rnaseq/master/docs/images/concordant.png)

vs 30 replicates:

![](https://raw.githubusercontent.com/roryk/bcbio.rnaseq/master/docs/images/concordant_30replicates.png)

The simulator simulates a range of fold changes and the two concordance plots are
faceted on the log2 fold change cutoff listed at the top. The simulated set has
200 additional genes that are DE between each cutoff, for a total of 1000 DE genes.
For three samples you can see almost all DE genes are called at a 4-fold change.
At a 2-fold change only around half of the correct DE genes are called and almost none
for fold changes lower than that. For 30 samples genes with a lower fold change are
called much more accurately.

Other useful plots are the Jaccard index:

![](https://raw.githubusercontent.com/roryk/bcbio.rnaseq/master/docs/images/jaccard.png)

and the area under the ROC curve for each fold change cutoff:
![](https://raw.githubusercontent.com/roryk/bcbio.rnaseq/master/docs/images/logFC-auc-plot.png)


## Modes of operation
### Simulation
``bcbio.rnaseq`` can be used to simulate a RNA-seq experiment of
a given sample size. Running bcbio.rnaseq with:

```
lein run simulate -s 3 -d sample_size_3
```
Will output the results of simulating an experiment of size 3 to the
directory sample_size_3. The simulator simulates an experiment with 20 million
reads with 10,000 total genes, 1000 of which are differentially expressed,
200 at five different fold change levels.

### bcbio-nextgen comparison
This mode is invoked with

```
lein run compare
```

and takes the output from a
[bcbio-nextgen][bcbio-nextgen] RNA-seq analysis and runs several
different DE callers and compares the results to each other. If your
run includes [ERCC spike-in][ERCC] data ``bcbio.rnaseq`` will detect
this automatically and run a concordance analysis on the ERCC data.
Passing the ``--seqc`` flag turns on a
a diagnostic mode
for determining how well DE callers work against a reference set of ~
1000 qPCR assayed genes from the [SEQC][SEQC] project. The true set of
DE calls were made on the qPCR data via the hacky method of performing
a t-test and BH correcting the p-values, and calling anything with a
FDR < 0.05 as differentially expressed. To run in this mode you would
first download and prepare the [SEQC][SEQC] data, align and quantitate it with
[bcbio-nextgen][bcbio-nextgen] and run ``bcbio.rnaseq`` on the
results.

## Status

If you have ``cufflinks`` and ``R`` installed and in your path, this
should work for you.  If it doesn't please open an issue and we'll fix
it.

## Creating a standalone executable

```
git clone https://github.com/roryk/bcbio.rnaseq
cd bcbio.rnaseq
make
```

The executable will be in `bin/bcbio-rnaseq`.

## Adding new R-based DE callers

If you make a R-file template called ``name-of-caller.template`` and
stick it in the ``resources`` directory, it will automatically be
added to the list of callers to run. The templates have to handle
three things: transforming the counts to something the caller can use,
resolving the groups to be compared from a list indicating which
column in the table goes with which group and outputting a table with
the DE calls in the right format. Each file takes three inputs:

1. ``count-file``: a file of counts of reads mapping to genes, with
the rows the IDs and the columns the sample names. This will be
provided to your script.
2. ``class``: this is a list which pairs each column with each
   condition to run.  Right now this only does simple two-factor
   analyses.
3. ``out-file``: In your R script the results should be transformed to
   have these columns::

```
    id, expr, logFC, pval, padj, algorithm
```

where ``id`` is the gene ID, ``expr`` is the normalized average counts
for all of the conditions for that gene, ``logFC`` is the log2 fold
change, ``pval`` the p-value that condition 1 is different than
condition 2, ``padj`` the Benjamini-Hochberg adjusted p-value and
``algorithm`` the name for this caller that you want to show up on the
graphs.

An example is clearer: a proper template, [deseq][deseq]
and the output, [sample-output][sample-output].

[SEQC]: http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE49712
[deseq]: http://raw.github.com/roryk/bcbio.rnaseq/master/resources/templates/deseq.template
[sample-output]: http://raw.github.com/roryk/bcbio.rnaseq/master/resources/test-analysis/deseq_A_vs_B.tsv
[trimming]: http://biorxiv.org/content/early/2013/12/23/000422
[bcbio-nextgen]: https://github.com/chapmanb/bcbio-nextgen
[metadata]: https://github.com/chapmanb/bcbio-nextgen/blob/master/docs/contents/configuration.rst#sample-information
[ERCC]: http://www.lifetechnologies.com/order/catalog/product/4456740
[dl]: http://github.com/roryk/bcbio.rnaseq/releases/download/v.0.1.0a/bcbio.rnaseq-0.1.0a.jar
