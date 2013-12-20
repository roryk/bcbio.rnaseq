# tidy the taqman data
library(reshape)
library(stringr)
library(CHBUtils)
description = data.frame(group=c("A", "B"), reference=c("universal", "brain"),
    mix=c(1, 2))
x = read.table("raw/TAQ.txt", sep="\t", header=TRUE)
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
foldchange$logFC = log2(foldchange$A/foldchange$B)
foldchange = foldchange[, c("id", "logFC")]
foldchange$method = "qPCR"

write.table(foldchange, "qpcr_HBRR_vs_UHRR.tidy", row.names=FALSE, quote=FALSE, col.names=TRUE,
            sep="\t")
write.table(casted, "qpcr.tidy", row.names=FALSE, quote=FALSE, col.names=TRUE, sep="\t")
