###################################################
###################################################
## Name:  	X10 application test
## Created by: 	Juemin Zhang
## Contact:   	zhangj@us.ibm.com
###################################################
###################################################


###################################################
# Source and target file
###################################################
target_list = TestDense TestMultDense TestTrans TestLapack

target		= TestDense

# build path
build_path	= build

# GML path
gml_path    = ../../
gml_scripts = $(gml_path)/scripts

# compile options
X10_FLAG	= -d $(build_path) -report postcompile=2 -v -errors 5 -VERBOSE_CHECKS #-O -NO_CHECKS

# runtime settings
runtime_list = mpi sock java
numplaces = 1
test_args = 

###################################################
# make rules
###################################################
## default run
default	: java

run		: run_java

###################################################
# Including rules
###################################################
include $(gml_path)/scripts/build_common.mk
include $(gml_path)/scripts/build_managed.mk
include $(gml_path)/scripts/build_native.mk
include $(gml_path)/scripts/build_native_mpi.mk
include $(gml_path)/scripts/test_run.mk

###################################################

help	::
	@echo "-------------------- build/test dense matrix ----------------";
	@echo " make          : build managed backend";
	@echo " make run      : run dense matrix tests on managed backend";
	@echo "" 
