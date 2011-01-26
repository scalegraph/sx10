#
#  This file is part of the X10 project (http://x10-lang.org).
#
#  This file is licensed to You under the Eclipse Public License (EPL);
#  You may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#      http://www.opensource.org/licenses/eclipse-1.0.php
#
#  (C) Copyright IBM Corporation 2006-2010.
#

TESTS += $(patsubst test/%,test/%.mpi,$(BASE_TESTS))

MPI_DYNLIB = lib/$(LIBPREFIX)x10rt_mpi$(LIBSUFFIX)
LIBS += $(MPI_DYNLIB)

PROPERTIES += etc/x10rt_mpi.properties

%.mpi: %.cc $(MPI_DYNLIB)
	$(MPICXX) $(CXXFLAGS) $< -o $@ $(LDFLAGS) -lx10rt_mpi $(X10RT_TEST_LDFLAGS)

mpi/x10rt_mpi.o: mpi/x10rt_mpi.cc
	$(MPICXX) $(CXXFLAGS) $(CXXFLAGS_SHARED) -c $< -o $@

ifdef X10_STATIC_LIB
$(MPI_DYNLIB): mpi/x10rt_mpi.o $(COMMON_OBJS)
	$(AR) $(ARFLAGS) $@ $^
else
$(MPI_DYNLIB): mpi/x10rt_mpi.o $(COMMON_OBJS)
	$(MPICXX) $(CXXFLAGS) $(CXXFLAGS_SHARED) $(LDFLAGS_SHARED) -o $@ $^
endif

etc/x10rt_mpi.properties:
	@echo "PLATFORM=$(X10RT_PLATFORM)" > $@
	@echo "CXX=$(MPICXX)" >> $@
	@echo "CXXFLAGS=" >> $@
	@echo "LDFLAGS=$(CUDA_LDFLAGS)" >> $@
	@echo "LDLIBS=-lx10rt_mpi $(CUDA_LDLIBS)" >> $@

.PRECIOUS: etc/x10rt_mpi.properties

# vim: ts=8:sw=8:noet
