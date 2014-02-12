# bcbio.rnaseq -- Create executable script

all:
	rm -f target/*.jar
	lein uberjar
	cat bin/bcbio-rnaseq.template target/*-standalone.jar > bin/bcbio-rnaseq
	chmod +x bin/bcbio-rnaseq
