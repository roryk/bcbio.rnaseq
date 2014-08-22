# adapted from "Voom: precision weights unlock linear model analysis tools for
# "RNA-seq read counts"
# 1847344521090019_add1.r
library(edgeR)
nsim = 100
invChisq = TRUE

ngenes = 10000
load(url("http://bioinf.wehi.edu.au/voom/qAbundanceDist.RData"))

# these are from a real dataset. this is the baseline proportion of genes, normalized to
# the total number. so the proportion of the dataset that will be devoted to one gene
# we could have something to estimate this from the data and make simulations of
# the shape of the actual data set
baselineprop = qAbundanceDist((1:ngenes)/(ngenes+1))
baselineprop = baselineprop/sum(baselineprop)
group = factor(c(1,1,1,2,2,2))
design = model.matrix(~ group)
n1 = 3
n2 = 3
nlibs = n1 + n2
expected.lib.size = 20e6 * c(1, 0.1, 1, 0.1, 1, 0.1)
i = sample(1:ngenes, 200)
i1 = i[1:100]
i2 = i[101:200]
fc = 2
baselineprop1 <- baselineprop2 <- baselineprop
baselineprop1[i1] = baselineprop[i1]*fc
baselineprop2[i2] = baselineprop[i2]*fc
# baselineprop1 <- baselineprop1[i1] * fc
# baselineprop2 <- baselineprop2[i2] * fc

mu0.1 = matrix(baselineprop1,ngenes,1) %*% matrix(expected.lib.size[1:n1],1,n1)
mu0.2 = matrix(baselineprop2,ngenes,1) %*% matrix(expected.lib.size[(n1+1):(n1+n2)],1,n2)
mu0 = cbind(mu0.1,mu0.2)
status = rep(0,ngenes)
status[i1] = -1
status[i2] = 1

# biological variation
BCV0 = 0.2 + 1 / sqrt(mu0)

# whats the inv chisq do
if(invChisq){
    df.BCV = 40
	BCV = BCV0*sqrt(df.BCV/rchisq(ngenes,df=df.BCV))
} else {
	BCV = BCV0*exp( rnorm(ngenes,mean=0,sd=0.25)/2 )
}

shape = 1/BCV^2
scale = mu0/shape
mu = matrix(rgamma(ngenes*nlibs,shape=shape,scale=scale),ngenes,nlibs) 

# technical variabtion
counts <- matrix(rpois(ngenes*nlibs,lambda=mu),ngenes,nlibs)

# filter
keep <- rowSums(counts)>=10
nkeep <- sum(keep)
counts2 <- counts[keep,]
counts = counts2


edgeR.dgelist = DGEList(counts = counts, group=group)
edgeR.dgelist = calcNormFactors(edgeR.dgelist, method = "TMM")
edgeR.dgelist = estimateCommonDisp(edgeR.dgelist)
edgeR.dgelist = estimateTrendedDisp(edgeR.dgelist)
edgeR.dgelist = estimateTagwiseDisp(edgeR.dgelist)
edgeR.test = exactTest(edgeR.dgelist)
out_table = edgeR.test$table
out_table$id = rownames(out_table)
out_table$padj = p.adjust(out_table$PValue, method = "BH")
out_table$algorithm = "edgeR"
out_table$expr = 2^out_table$logCPM
out_table = out_table[, c("id", "expr", "logFC", "PValue", "padj", "algorithm")]
colnames(out_table) = c("id", "expr", "logFC", "pval", "padj", "algorithm")
table(out_table$padj < 0.05)
