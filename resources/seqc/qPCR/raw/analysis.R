# tidy the taqman data
library(reshape)
library(stringr)
library(methods)

fix_excel_symbols = function(symbols) {
    return(unlist(lapply(as.character(symbols), fix_string)))
}

fix_string = function(x) {
    regex_str = "([0-9])-(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)"
    return(if(any(grep(regex_str, x))) date_fixer(x) else x)
}

date_fixer = function(x) {
    tokens = str_split(x, "-")
    return(paste(toupper(tokens[[1]][2]), tokens[[1]][1], sep=""))
}

t.test.robust <- function(...) {
    obj<-try(t.test(...), silent=TRUE)
    if (is(obj, "try-error")) return(NA) else return(obj$p.value)
}

description = data.frame(group=c("A", "B"), reference=c("universal", "brain"),
    mix=c(1, 2))
x = read.table("TAQ.txt", sep="\t", header=TRUE)
x$id = fix_excel_symbols(x$X)
x = x[!duplicated(x$id), ]
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
foldchange$pvalue =  unlist(dlply(x, "id", function(y) t.test.robust(y[,c("A1", "A2", "A3", "A4")], y[,c("B1", "B2", "B3", "B4")])))
foldchange$padj = p.adjust(foldchange$pvalue)
foldchange = foldchange[, c("id", "logFC", "pvalue", "padj")]
foldchange$method = "qPCR"

write.table(foldchange, "foldchange_qpcr.tidy", row.names=FALSE, quote=FALSE, col.names=TRUE,
            sep="\t")
write.table(casted, "qpcr.tidy", row.names=FALSE, quote=FALSE, col.names=TRUE, sep="\t")
