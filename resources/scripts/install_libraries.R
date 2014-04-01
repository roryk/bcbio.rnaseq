# adapted from http://stackoverflow.com/questions/4090169/elegant-way-to-check-for-missing-packages-and-install-them
mirror = "http://cran.at.r-project.org"
update.packages(checkBuilt = TRUE, ask = FALSE, repos=mirror)
is_package_installed = function(package) {
    available = suppressMessages(suppressWarnings(sapply(package, require,
        quietly = TRUE, character.only = TRUE, warn.conflicts = FALSE)))
    missing = package[!available]
    if (length(missing) > 0) return(FALSE)
    return(TRUE)
}

install.cran = function(packages, default_mirror="http://cran.at.r-project.org") {
  for(i in packages) {
    #  require returns TRUE invisibly if it was able to load package
    if(!is_package_installed(i)) {
      options(repos = c(CRAN = default_mirror))
      suppressMessages(suppressWarnings(install.packages(i, dependencies = TRUE, repo=default_mirror)))
    }
  }
}

install.bioconductor = function(packages, default_mirror="http://cran.at.r-project.org") {
  for(i in packages) {
    #  require returns TRUE invisibly if it was able to load package
    if(!is_package_installed(i)) {
      suppressMessages(suppressWarnings(biocLite(i, ask=FALSE)))
    }
  }
}

install.github = function(packages, username) {
    install.cran(c("devtools"))
    library(devtools)
    for(i in packages) {
        if(!is_package_installed(i)) {
            suppressMessages(suppressWarnings(install_github(i, username)))
        }
    }
}

cran_packages = c("devtools", "ggplot2", "reshape", "pROC", "plyr", "knitr")
install.cran(cran_packages)

bioconductor_packages = c("edgeR", "HTSFilter", "DESeq2", "baySeq", "DESeq")
source("http://bioconductor.org/biocLite.R")
install.bioconductor(bioconductor_packages)

install.github(c("CHBUtils"), "hbc")
