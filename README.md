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

bcbio.rnaseq can be run in two different modes.  The first mode is
``compare-bcbio-run`` which takes the output from a
[bcbio-nextgen][bcbio-nextgen] RNA-seq analysis and runs several
different DE callers and compares the results to each other. Since
there is no true set of calls this is just a way to assess which
callers are different from each other.  The second mode is a
``seqc-comparisons`` which is a diagnostic mode for determining how
well DE callers work against a reference set of ~ 1000 qPCR assayed
genes from the [SEQC][SEQC] project. The true set of DE calls were
made on the qPCR data via the hacky method of performing a t-test and
BH correcting the p-values, and calling anything with a FDR < 0.05 as
differentially expressed. To run in this mode you would first download
and prepare the [SEQC][SEQC] data, align it with
[bcbio-nextgen][bcbio-nextgen] and run ``bcbio.rnaseq`` on the
results.

## Status
This is in a pre-release state, but theoretically should work for you
assuming you have ``cufflinks`` and ``R`` installed and in your path.
We'd really appreciate an issue being opened with the error if it does
not work for you, it will greatly help in getting this polished up for
an official release. The only current functionality is comparing DE
calls, not splicing events.

## Quickstart

At the end of your [bcbio-nextgen][bcbio-nextgen] run, point
bcbio.rnaseq at the project-summary.yaml file in your
``upload`` directory:

    java -jar bcbio.rnaseq-0.0.1-SNAPSHOT-standalone.jar compare-bcbio-run

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
* Automatic evaluation of ERCC spike-in data if available in non-SEQC
  data sets.
* Better comparison metrics overall.
* Inclusion of more callers.
* Try to put expression values on the same scale somehow; hard to do
  with RPKM/count based methods though. Maybe convert everything
  to RPKM?

[SEQC]: http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE49712
[deseq]: https://raw.github.com/roryk/bcbio.rnaseq/master/resources/deseq.template?token=414586__eyJzY29wZSI6IlJhd0Jsb2I6cm9yeWsvYmNiaW8ucm5hc2VxL21hc3Rlci9yZXNvdXJjZXMvZGVzZXEudGVtcGxhdGUiLCJleHBpcmVzIjoxMzg5MDIzNDQ2fQ%3D%3D--280e317fb477c6bbe3aa89e09ec598f46e7847d3
[sample-output]: https://raw.github.com/roryk/bcbio.rnaseq/master/resources/test-analysis/deseq_A_vs_B.tsv?token=414586__eyJzY29wZSI6IlJhd0Jsb2I6cm9yeWsvYmNiaW8ucm5hc2VxL21hc3Rlci9yZXNvdXJjZXMvdGVzdC1hbmFseXNpcy9kZXNlcV9BX3ZzX0IudHN2IiwiZXhwaXJlcyI6MTM4OTAyMzQ5Nn0%3D--f1e0e3f110d49e0ab961426f4d608bd86d195e3d
[trimming]: http://biorxiv.org/content/early/2013/12/23/000422
[bcbio-nextgen]: https://github.com/chapmanb/bcbio-nextgen
