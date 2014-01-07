<head>
    <script type="text/javascript"
            src="http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML">
    </script>
</head>

## Overview

There are a huge number of algorithms designed to call differential
events from RNA-seq data at the gene, isoform and splicing-event
level, with more coming out every day. It would be great if when a new
caller comes out, there was a way to automatically place it in the
context of the other callers. This project aims to do that, at least
for the output of [bcbio-nextgen][bcbio-nextgen] runs.

``bcbio.rnaseq`` can be run in two different modes.  The first mode is
``compare-bcbio-run`` which takes the output from a
[bcbio-nextgen][bcbio-nextgen] RNA-seq analysis and runs several
different DE callers and compares the results to each other. If your
run includes [ERCC spike-in][ERCC] data ``bcbio.rnaseq`` will detect
this automatically and run a concordance analysis on the ERCC data.
The second mode is a ``seqc-comparisons`` which is a diagnostic mode
for determining how well DE callers work against a reference set of ~
1000 qPCR assayed genes from the [SEQC][SEQC] project. The true set of
DE calls were made on the qPCR data via the hacky method of performing
a t-test and BH correcting the p-values, and calling anything with a
FDR < 0.05 as differentially expressed. To run in this mode you would
first download and prepare the [SEQC][SEQC] data, align it with
[bcbio-nextgen][bcbio-nextgen] and run ``bcbio.rnaseq`` on the
results.

## Status
[![Build Status](https://secure.travis-ci.org/roryk/bcbio.rnaseq.png)](http://travis-ci.org/roryk/bcbio.rnaseq)

If you have ``cufflinks`` and ``R`` installed and in your path, this
should work for you.  If it doesn't please open an issue and we'll fix
it.

## Installation
The latest release is 0.1.0a (4 January 2013): [download][dl].

## Quickstart

At the end of your [bcbio-nextgen][bcbio-nextgen] run, point
``bcbio.rnaseq`` at the project-summary.yaml file in your
``upload`` directory:

    java -jar bcbio.rnaseq-0.1.0a.jar compare-bcbio-run /path/to/project_summary.yaml key

where ``key`` is the field in the [metadata][metadata] entry you want
to use as the two groups to compare to each other.

To run against the [SEQC][SEQC] data, you would download the [SEQC][SEQC]
files, align them with [bcbio-nextgen][bcbio-nextgen] and point
the ``bcbio.rnaseq`` to the results:

    java -jar bcbio.rnaseq-0.1.0a.jar seqc-comparisons /path/to/project_summary.yaml key

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

## TODO

* Ability to compare different bcbio-nextgen runs to each other, for
  determining the effect of pipeline tweaks like [trimming][trimming].
* Ability to compare non-bcbio nextgen runs. This will require a
  little bit of work but should be doable.
* Add SEQC downloader and prepper script.
* Better comparison metrics overall.
* Inclusion of more callers.
* Try to put expression values on the same scale somehow; hard to do
  with RPKM/count based methods though. Maybe convert everything
  to RPKM?

[SEQC]: http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE49712
[deseq]: http://raw.github.com/roryk/bcbio.rnaseq/master/resources/templates/deseq.template
[sample-output]: http://raw.github.com/roryk/bcbio.rnaseq/master/resources/test-analysis/deseq_A_vs_B.tsv
[trimming]: http://biorxiv.org/content/early/2013/12/23/000422
[bcbio-nextgen]: https://github.com/chapmanb/bcbio-nextgen
[metadata]: https://github.com/chapmanb/bcbio-nextgen/blob/master/docs/contents/configuration.rst#sample-information
[ERCC]: http://www.lifetechnologies.com/order/catalog/product/4456740
[dl]: http://github.com/roryk/bcbio.rnaseq/releases/download/v.0.1.0a/bcbio.rnaseq-0.1.0a.jar
