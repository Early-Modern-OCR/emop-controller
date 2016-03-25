PAGE_EVALUATOR_VERSION := 1.1.0
PAGE_CORRECTOR_VERSION := 1.10.0
ROOT_DIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
SRC_DIR := $(ROOT_DIR)/src
LIB_DIR := $(ROOT_DIR)/lib
SEASR_HOME ?= $(LIB_DIR)/seasr
JUXTA_HOME ?= $(LIB_DIR)/juxta-cl
RETAS_HOME ?= $(LIB_DIR)/retas

.PHONY: help all docs

help:
	@echo "Please use 'make <target>' where <target> is one of"
	@echo "  all                to run 'build' and 'install' targets"
	@echo "  docs               to build documentation"
	@echo "  ocular				to build and install Ocular"
	@echo "  build              to build all dependencies"
	@echo "  build_seasr        to build SEASR"
	@echo "  build_juxta_cl     to build Juxta-CL"
	@echo "  build_retas        to build RETAS"
	@echo "  build_ocular		to build Ocular"
	@echo "  install            to install all dependencies"
	@echo "  install_seasr      to build SEASR"
	@echo "  install_juxta_cl   to build Juxta-CL"
	@echo "  install_retas      to build RETAS"
	@echo "  install_ocular		to install Ocular"
	@echo "  clean              to clean the build of all dependencies"
	@echo "  clean_seasr        to clean the build of SEASR"
	@echo "  clean_juxta_cl     to clean the build of Juxta-CL"
	@echo "  clean_retas        to clean the build of RETAS"
	@echo "  uninstall          to uninstall all dependencies"
	@echo "  uninstall_seasr    to uninstall SEASR"
	@echo "  uninstall_juxta_cl to uninstall Juxta-CL"
	@echo "  uninstall_retas    to uninstall RETAS"

_default: help

all: build install

docs:
	cd docs && make clean && make html

ocular: build_ocular install_ocular

build: build_seasr build_juxta_cl build_retas

build_seasr:
	mvn -f $(SRC_DIR)/seasr/PageEvaluator package
	mvn -f $(SRC_DIR)/seasr/PageCorrector package

build_juxta_cl:
	mvn -f $(SRC_DIR)/Juxta-cl package

build_retas:
	cd $(SRC_DIR)/RETAS && javac *.java

build_ocular:
ifneq ($(wildcard src/ocular/.*),)
	cd $(SRC_DIR)/ocular && git pull origin emop
else
	git clone -b emop https://github.com/Early-Modern-OCR/ocular.git $(SRC_DIR)/ocular
endif
	cd $(SRC_DIR)/ocular && _JAVA_OPTIONS="-Xmx512m" ./make_jar.sh

install: build install_seasr install_juxta_cl install_retas

install_seasr:
	install -d $(LIB_DIR)/seasr
	install -m 0664 $(SRC_DIR)/seasr/PageEvaluator/target/PageEvaluator-$(PAGE_EVALUATOR_VERSION)-SNAPSHOT.jar $(SEASR_HOME)/PageEvaluator.jar
	install -m 0664 $(SRC_DIR)/seasr/PageCorrector/target/PageCorrector-$(PAGE_CORRECTOR_VERSION)-SNAPSHOT.jar $(SEASR_HOME)/PageCorrector.jar

install_juxta_cl:
	install -d $(JUXTA_HOME)
	install -d $(JUXTA_HOME)/lib
	install -m 0664 $(SRC_DIR)/Juxta-cl/target/juxta-cl.jar $(JUXTA_HOME)/juxta-cl.jar
	install $(SRC_DIR)/Juxta-cl/scripts/*.sh $(JUXTA_HOME)/
	install -m 0664 $(SRC_DIR)/Juxta-cl/target/lib/*.jar $(JUXTA_HOME)/lib/

install_retas:
	install -d $(RETAS_HOME)
	cd $(SRC_DIR)/RETAS && jar cfe $(RETAS_HOME)/retas.jar RecursiveAlignmentTool *.class
	install -m 0664 $(SRC_DIR)/RETAS/config.txt $(RETAS_HOME)/config.txt

install_ocular:
	install -d $(LIB_DIR)/ocular
	install -d $(LIB_DIR)/ocular/conf
	install $(SRC_DIR)/ocular/ocular-*-SNAPSHOT-with_dependencies.jar $(LIB_DIR)/ocular/ocular.jar
	install -m 0644 $(SRC_DIR)/ocular/conf/* $(LIB_DIR)/ocular/conf/
	install -m 0644 $(SRC_DIR)/ocular/LICENSE.txt $(LIB_DIR)/ocular/
	install -m 0644 $(SRC_DIR)/ocular/README.md $(LIB_DIR)/ocular/
	install -m 0644 $(SRC_DIR)/ocular/README.txt $(LIB_DIR)/ocular/

clean: clean_seasr clean_juxta_cl clean_retas

clean_seasr:
	rm -r $(SRC_DIR)/seasr/PageCorrector/target/*
	rm -r $(SRC_DIR)/seasr/PageEvaluator/target/*

clean_juxta_cl:
	rm -r $(SRC_DIR)/Juxta-cl/target/*

clean_retas:
	rm $(SRC_DIR)/RETAS/*.class

uninstall: uninstall_seasr uninstall_juxta_cl uninstall_retas

uninstall_seasr:
	rm $(SEASR_HOME)/PageEvaluator.jar
	rm $(SEASR_HOME)/PageCorrector.jar

uninstall_juxta_cl:
	rm $(JUXTA_HOME)/juxta-cl.jar
	rm $(JUXTA_HOME)/*.sh
	rm $(JUXTA_HOME)/lib/*.jar

uninstall_retas:
	rm $(RETAS_HOME)/retas.jar
	rm $(RETAS_HOME)/config.txt
