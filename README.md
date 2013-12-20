# bcbio.rnaseq

## Overview
There are a bajillion different ways to call differential events from RNA-seq
data at the gene, isoform and splicing-event level, with more coming out
every day. It would be great if when a new caller comes out, there was
a way to automatically place it in the context of the other callers. This
project aims to do that.

## Adding new R-based DE callers
If you make a R-file template called name-of-caller.template and stick
it in the resources directory, it will automatically be added to the
list of callers to run. The templates have to handle three things:
transforming the counts to something the caller can use, resolving the
groups to be compared from a list indicating which column in the table
goes with which group and outputting a table with the DE calls in the
right format. It is pretty simple to do.

1) count-file: a file of counts of reads mapping to genes, with the rows the IDs
and the columns the sample names. This will be provided to your script.
2) class: this is a list which pairs each column with each condition to run.
Right now this only does simple two-factor analyses.
3) out-file: In your R script the results should be transformed to have these columns:

    id, expr, logFC, pval, padj, algorithm

where ``id`` is the gene ID, ``expr`` is the normalized average counts
for all of the conditions for that gene, ``logFC`` is the log2 fold
change, ``pval`` the p-value that condition 1 is different than
condition 2, ``padj`` the Benjamini-Hochberg adjusted p-value and
``algorithm`` the name for this caller that you want to show up on the
graphs.

## Why Clojure?
It was time to learn a new language and Clojure is really fun.
