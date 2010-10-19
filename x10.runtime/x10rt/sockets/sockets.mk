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

TESTS += $(patsubst test/%,test/%.sockets,$(BASE_TESTS))

SOCKETS_DYNLIB = lib/$(LIBPREFIX)x10rt_sockets$(LIBSUFFIX)
LIBS += $(SOCKETS_DYNLIB)

PROPERTIES += etc/x10rt_sockets.properties
LAUNCHER_OBJS = sockets/Launcher_Init.o sockets/Launcher.o sockets/tcp.o

ifeq ($(X10RT_PLATFORM), sunos)
  SOLARIS_LDLIBS += -lresolv -lnsl -lsocket -lrt
endif

%.sockets: %.cc $(SOCKETS_DYNLIB)
	$(CXX) $(CXXFLAGS) $< -o $@ $(LDFLAGS) -lx10rt_sockets $(SOLARIS_LDLIBS) $(X10RT_TEST_LDFLAGS)

ifdef X10_STATIC_LIB
$(SOCKETS_DYNLIB): sockets/x10rt_sockets.o $(LAUNCHER_OBJS) $(COMMON_OBJS)
	$(AR) $(ARFLAGS) $@ $^
else
$(SOCKETS_DYNLIB): sockets/x10rt_sockets.o $(LAUNCHER_OBJS) $(COMMON_OBJS) 
ifeq ($(X10RT_PLATFORM),aix_xlc)
	$(SHLINK) $(CXXFLAGS) $(CXXFLAGS_SHARED) $(LDFLAGS_SHARED) -o $@ $^
else
	$(CXX) $(CXXFLAGS) $(CXXFLAGS_SHARED) $(LDFLAGS_SHARED) -o $@ $^
endif
endif

etc/x10rt_sockets.properties:
	@echo "CXX=$(CXX)" > $@
	@echo "CXXFLAGS=" >> $@
	@echo "LDFLAGS=$(CUDA_LDFLAGS)" >> $@
	@echo "LDLIBS=-lx10rt_sockets $(SOLARIS_LDLIBS) $(CUDA_LDLIBS)" >> $@

.PRECIOUS: etc/x10rt_sockets.properties

# vim: ts=8:sw=8:noet
