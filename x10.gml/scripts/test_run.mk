##---------------
#Input settings
##---------------
#$(gml_path)    ## gml installation path
#$(gml_inc)     ## gml include path
#$(gml_lib)     ## gml library path
#$(build_path)  ## application target build path
#$(target)      ## application target name
#$(target_list) ## list of targets

#$(server)      ## Server name for testing. If not on server, it uses default settings
#$(test_args)   ## test run arges
#$(numplaces)   ## Number of places used in testing
#$(runtime_list)## backend and runtime transport
#$(GML_ELEM_TYPE) ## float or double

#X10_FLAG += -sourcepath $(X10_HOME)/x10.tests/tests/x10lib

###################################################
## execution settings
###################################################

#Following settings are used in Triloka4 batch job headnode

ifeq ($(server), $(findstring $(server), $(shell hostname)))
	Srun  = mpirun
	Srsv  = salloc
else
	Srun  = mpirun
	Srsv  = mpirun
endif

##--------------------------------
Xrun  = $(shell which X10Launcher)
Xjvm  = $(shell which x10)

###-----------------------------------------------
# run tests short-keys

run_java	: java
		GML_ELEM_TYPE=$(GML_ELEM_TYPE) X10_NPLACES=$(numplaces) $(Xjvm) -classpath $(build_path):$(gml_lib)/managed_gml_$(GML_ELEM_TYPE).jar -libpath $(base_dir_elem)/lib $(target) $(test_args)
#		$(Xjvm) -np $(numplaces) -classpath $(build_path):$(gml_lib)/managed_gml.jar -libpath $(build_path):$(gml_lib) $(target) $(test_args)

run_mpi		: mpi
			$(Srun) -n $(numplaces) ./$(target)_mpi_$(GML_ELEM_TYPE) $(test_args)

run_sock	: sock
			X10_NPLACES=$(numplaces) ./$(target)_sock_$(GML_ELEM_TYPE) $(test_args)

run_pami	: pami
			MP_PROCS=$(numplaces) MP_EUILIB=ip ./$(target)_pami_$(GML_ELEM_TYPE) $(test_args)

## resilience tests
HAMMER_FILE ?= 

# run with two spare places, replace any failed place with a spare
run_redundant	: java
		GML_ELEM_TYPE=$(GML_ELEM_TYPE) X10_RESILIENT_MODE=1 X10_PLACE_GROUP_RESTORE_MODE=1 X10_GML_HAMMER_FILE=$(HAMMER_FILE) X10_NPLACES=$(numplaces) $(Xjvm) -classpath $(build_path):$(gml_lib)/managed_gml_$(GML_ELEM_TYPE).jar -libpath $(build_path):$(gml_lib):$(base_dir_elem)/lib $(target) $(test_args) --skip 2 --checkpointFreq 2

run_elastic	: java
		GML_ELEM_TYPE=$(GML_ELEM_TYPE) X10_RESILIENT_MODE=12 X10_PLACE_GROUP_RESTORE_MODE=2 X10_GML_HAMMER_FILE=$(HAMMER_FILE) X10_NPLACES=$(numplaces) $(Xjvm) -DX10RT_DATASTORE=Hazelcast -classpath $(build_path):$(gml_lib)/managed_gml_$(GML_ELEM_TYPE).jar -libpath $(build_path):$(gml_lib):$(base_dir_elem)/lib $(target) $(test_args) --checkpointFreq 2


##----- Run all tests in one transport or java backend

runall_java :
			$(foreach src, $(target_list), $(MAKE) target=$(src) run_java; )

runall_mpi	:
			$(foreach src, $(target_list), $(MAKE) target=$(src) run_mpi; )

runall_sock	:
			$(foreach src, $(target_list), $(MAKE) target=$(src) run_sock; )

cleanall :
			$(foreach src, $(target_list), $(MAKE) target=$(src) clean; )

##---- Run all tests in all transports and java backend

runall		:
			$(foreach rt, $(runtime_list), $(MAKE) runall_$(rt) ; )

###----

################################################
#.PHONY	: run_java run_mpi run_sock runalltrans runalltests
###########################################

help	::
	@echo "-------------------------- launch test run -----------------------";
	@echo " make run_mpi      : run $(target) test built for MPI transport";
	@echo " make run_sock     : run $(target) test built for socket transport" ;
	@echo " make run_java     : run $(target) test built for managed backend";
	@echo " make runall_mpi   : run all tests built for MPI transport";
	@echo " make runall_sock  : run all tests built for socket transport";
	@echo " make runall_java  : run all tests built for managed backend";
	@echo " make runall       : run all tests built for all backends: $(runtime_list)";
	@echo "";
