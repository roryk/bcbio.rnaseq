# tidy the taqman data
library(reshape)
library(stringr)
library(CHButils)
description = data.frame(group=c("A", "B"), reference=c("universal", "brain"),
    mix=c(1, 2))
x = read.table("combined/TAQ.txt", sep="\t", header=TRUE)
x$id = fix_excel_symbols(x$X)
x$X = NULL
melted = melt(x)
melted$group = apply(melted, 1, function (q) substring(q["variable"], 1, 1))
melted$replicate = apply(melted, 1, function (q) substring(q["variable"], 2, 2))
melted$variable = NULL
melted = merge(melted, description, by=c("group"), all=TRUE)
melted$method = "qPCR"

casted = cast(melted, id +  reference + mix + group ~ ., mean)
colnames(casted) = c("id", "reference", "mix", "group", "expression")

foldchange = cast(casted, id ~ group)
foldchange$logFC = log2(foldchange$B/foldchange$A)
foldchange = foldchange[, c("id", "logFC")]
foldchange$method = "qPCR"

write.table(foldchange, "foldchange_qpcr.tidy", row.names=FALSE, quote=FALSE, col.names=TRUE,
            sep="\t")
write.table(casted, "qpcr.tidy", row.names=FALSE, quote=FALSE, col.names=TRUE, sep="\t")

# write these both out, the tidy table and the foldchange table

## y = read.table("combined/taqman.txt", sep="\t", header=TRUE)
## y$id = rownames(y)
## casted$logFC = log2(casted$B/casted$A)

## x = read.table("combined/combined.counts", sep="\t", header=TRUE)
## groups = c("A", "B", "A", "B", "A", "B", "A", "B", "A", "B")
## x$id = fix_excel_symbols(x$id)
## melted2 = melt(x)
## melted2$reference = apply(melted2, 1, function (q) substring(q["variable"], 1, 4))
## melted2$reference = apply(melted2, 1, function(q) if(q["reference"] == "UHRR") "universal" else "brain")
## melted2$replicate = apply(melted2, 1, function (q) substring(q["variable"], 9, 9))
## melted2$variable = NULL
## melted2 = merge(melted2, description, by="reference", all=TRUE)
## melted2$method = "rnaseq"

## combined = rbind(melted, melted2)

## keep = subset(combined, group %in% c("A", "B"))

## casted = cast(keep, id ~ group + method, mean)
## complete = casted[complete.cases(casted),]
## complete$rnaseq_logFC = log2(complete$B_rnaseq / complete$A_rnaseq)
## complete$qPCR_logFC = liog2(complete$B_qPCR / complete$A_qPCR)

## ggplot(complete, aes(complete$qPCR_logFC, complete$rnaseq_logFC)) + geom_point()

#y = read.table("../../de-analysis/edgeR_A_vs_B.tsv", header=TRUE, sep="\t")
foldchange = read.table("foldchange_qpcr.tidy", header=TRUE, sep="\t")
analysis_dir = "../../de-analysis"
in_files = list.files(analysis_dir, "*.tsv", full.names=TRUE)
ldf = lapply(in_files, read.table, header=TRUE, stringsAsFactors=FALSE)
df = do.call(rbind, ldf)

df = df[, c("id", "logFC", "algorithm")]
colnames(df) = c("id", "logFC", "method")
qpcr_foldchange = foldchange[, c("id", "logFC")]
df = merge(df, qpcr_foldchange, by="id")
colnames(df) = c("id", "seq_logFC", "method", "qPCR_logFC")
ggplot(df, aes(seq_logFC, qPCR_logFC)) + geom_point() + facet_grid(. ~ method) +
    xlab(expression(paste(log["2"],  " fold change via RNA-seq"))) +
    ylab(expression(paste(log["2"], " fold change via qPCR")))
         

