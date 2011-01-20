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

#X10_VERSION=svn head
X10_VERSION=2.1.1
VERSION=20100916
SOCKETS_TGZ = pgas-$(VERSION)-$(WPLATFORM)-sockets.tgz
LAPI_TGZ = pgas-$(VERSION)-$(WPLATFORM)-lapi.tgz
BGP_TGZ = pgas-$(VERSION)-$(WPLATFORM)-bgp.tgz

# defaults
PLATFORM_SUPPORTS_SOCKETS := no
PLATFORM_SUPPORTS_PANE := no
PLATFORM_SUPPORTS_LAPI := no
PLATFORM_SUPPORTS_BGP := no

LAPI_LDFLAGS    = $(CUDA_LDFLAGS)
BGP_LDFLAGS     = $(CUDA_LDFLAGS)
SOCKETS_LDFLAGS = $(CUDA_LDFLAGS)
PANE_LDFLAGS    = $(CUDA_LDFLAGS) -btextpsize:64K -bdatapsize:64K -bstackpsize:64K

LAPI_LDLIBS     = -lx10rt_pgas_lapi $(CUDA_LDLIBS)
BGP_LDLIBS      = -lx10rt_pgas_bgp $(CUDA_LDLIBS)
SOCKETS_LDLIBS  = -lx10rt_pgas_sockets -lpthread $(CUDA_LDLIBS)
PANE_LDLIBS     = -lx10rt_pgas_pane $(CUDA_LDLIBS)

ifeq ($(X10RT_PLATFORM), bgp)
  WPLATFORM      := bgp_g++4
  PLATFORM_SUPPORTS_BGP        := yes
  BGP_LDFLAGS    += -L/bgsys/drivers/ppcfloor/comm/lib -L/bgsys/drivers/ppcfloor/runtime/SPI
  BGP_LDLIBS     += -ldcmf.cnk -ldcmfcoll.cnk -lSPI.cna -lpthread -lrt -lm
endif
ifeq ($(X10RT_PLATFORM), aix_xlc)
  LAPI_LDFLAGS   += -Wl,-binitfini:poe_remote_main 
  LAPI_LDDEPS    := -L/usr/lpp/ppe.poe/lib -lmpi_r -lvtd_r -llapi_r -lpthread -lm
  LAPI_LDLIBS    += $(LAPI_LDDEPS)
  PANE_LDFLAGS   += -Wl,-binitfini:poe_remote_main -L/usr/lpp/ppe.poe/lib
  PANE_ARLIBS     = -llapi_r -lpthread -lm
  PANE_LDLIBS    += -lmpi_r -lvtd_r $(PANE_ARLIBS)
  WPLATFORM      := aix_xlc
  PLATFORM_SUPPORTS_LAPI       := yes
  # PLATFORM_SUPPORTS_SOCKETS    := yes
  PLATFORM_SUPPORTS_PANE       := yes
endif
ifeq ($(X10RT_PLATFORM), aix_gcc)
  WPLATFORM      := aix_g++4
  PLATFORM_SUPPORTS_LAPI       := yes
  LAPI_LDFLAGS   += -Wl,-binitfini:poe_remote_main 
  LAPI_LDDEPS    := -L/usr/lpp/ppe.poe/lib -lmpi_r -lvtd_r -llapi_r -lpthread -lm
  LAPI_LDLIBS    += $(LAPI_LDDEPS)
  PANE_LDFLAGS   += -Wl,-binitfini:poe_remote_main -L/usr/lpp/ppe.poe/lib
  PANE_ARLIBS     = -llapi_r -lpthread -lm
  PANE_LDLIBS    += -lmpi_r -lvtd_r $(PANE_ARLIBS)
  #PLATFORM_SUPPORTS_SOCKETS    := yes
  PLATFORM_SUPPORTS_PANE       := yes
endif
ifeq ($(X10RT_PLATFORM), linux_ppc_64_gcc)
  WPLATFORM      := linux_ppc_64_g++4
  PLATFORM_SUPPORTS_LAPI       := yes
  LAPI_LDDEPS    := 
  LAPI_LDLIBS    += $(LAPI_LDDEPS) -L/opt/ibmhpc/ppe.poe/lib -lpoe -lmpi_ibm -llapi
  PLATFORM_SUPPORTS_SOCKETS    := yes
endif
ifeq ($(X10RT_PLATFORM), linux_ppc_64_xlc)
  WPLATFORM      := linux_ppc_64_xlc
  PLATFORM_SUPPORTS_LAPI       := yes
  LAPI_LDDEPS    := 
  LAPI_LDLIBS    += $(LAPI_LDDEPS) -L/opt/ibmhpc/ppe.poe/lib -lpoe -lmpi_ibm -llapi
  PLATFORM_SUPPORTS_SOCKETS    := yes
endif
ifeq ($(X10RT_PLATFORM), linux_x86_64)
  WPLATFORM      := linux_x86_64_g++4
  PLATFORM_SUPPORTS_LAPI       := yes
  LAPI_LDDEPS    := 
  LAPI_LDLIBS    += $(LAPI_LDDEPS) -L/opt/ibmhpc/ppe.poe/lib -lpoe -lmpi_ibm -llapi
  PLATFORM_SUPPORTS_SOCKETS    := yes
endif
ifeq ($(X10RT_PLATFORM), linux_x86_32)
  WPLATFORM      := linux_x86_g++4
# TODO: re-enable when we build the 32 bit lapi version of pgas and post it.
#  PLATFORM_SUPPORTS_LAPI       := yes
  LAPI_LDDEPS    := 
  LAPI_LDLIBS    += $(LAPI_LDDEPS) -L/opt/ibmhpc/ppe.poe/lib -lpoe -lmpi_ibm -llapi
  PLATFORM_SUPPORTS_SOCKETS    := yes
endif
ifeq ($(X10RT_PLATFORM), cygwin)
  WPLATFORM      := cygwin_x86_g++4
  PLATFORM_SUPPORTS_SOCKETS    := yes
endif
ifeq ($(X10RT_PLATFORM), darwin)
  WPLATFORM      := macos_x86_g++4
  PLATFORM_SUPPORTS_SOCKETS    := yes
endif
ifeq ($(X10RT_PLATFORM), darwin64)
  WPLATFORM      := macos_x86_g++4
  PLATFORM_SUPPORTS_SOCKETS    := yes
endif
ifeq ($(X10RT_PLATFORM), sunos)
  WPLATFORM      := sunos_sparc_g++4
  PLATFORM_SUPPORTS_SOCKETS    := yes
  SOCKETS_LDLIBS += -lresolv -lnsl -lsocket -lrt
endif

ifdef CUSTOM_PGAS
include/pgasrt.h: $(CUSTOM_PGAS)/include/pgasrt.h
	$(CP) $(CUSTOM_PGAS)/include/*.h include

  ifeq ($(shell test -r $(CUSTOM_PGAS)/lib/libxlpgas_pane.a && printf hi),hi)
    XLPGAS_PANE_EXISTS := yes
  else
    XLPGAS_PANE_EXISTS := no
  endif
  ifeq ($(shell test -r $(CUSTOM_PGAS)/lib/libxlpgas_lapi.a && printf hi),hi)
    XLPGAS_LAPI_EXISTS := yes
  else
    XLPGAS_LAPI_EXISTS := no
  endif
  ifeq ($(shell test -r $(CUSTOM_PGAS)/lib/libxlpgas_sockets.a && printf hi),hi)
    XLPGAS_SOCKETS_EXISTS := yes
  else
    XLPGAS_SOCKETS_EXISTS := no
  endif
  ifeq ($(shell test -r $(CUSTOM_PGAS)/lib/libxlpgas_bgp.a && printf hi),hi)
    XLPGAS_BGP_EXISTS := yes
  else
    XLPGAS_BGP_EXISTS := no
  endif
else
  # if the platform supports it, it can be found in the website tarball for that platform
  XLPGAS_PANE_EXISTS := no
  XLPGAS_LAPI_EXISTS := $(PLATFORM_SUPPORTS_LAPI)
  XLPGAS_SOCKETS_EXISTS := $(PLATFORM_SUPPORTS_SOCKETS)
  XLPGAS_BGP_EXISTS := $(PLATFORM_SUPPORTS_BGP)
endif

#Assume that if poe is installed then `which poe` will print its full path to
#stdout.  Since we don't know what the full path is, we can't run it because it
#will fail, and we can't trust the error messages or exit code of `which`, we
#instead test if the path is an executable file.
ifeq ($(shell test -x "`which poe 2>/dev/null`" && printf hi),hi)
  POE_EXISTS := yes
else
  POE_EXISTS := no
endif

ifeq ($(PLATFORM_SUPPORTS_SOCKETS), yes)

TESTS += $(patsubst test/%,test/%.pgas_sockets,$(BASE_TESTS))
PGAS_DYNLIB_SOCKETS = lib/$(LIBPREFIX)x10rt_pgas_sockets$(LIBSUFFIX)
LIBS += $(PGAS_DYNLIB_SOCKETS)
PROPERTIES += etc/x10rt_pgas_sockets.properties
PGAS_EXECUTABLES = bin/launcher bin/manager bin/daemon
EXECUTABLES += $(PGAS_EXECUTABLES)

%.pgas_sockets: %.cc $(PGAS_DYNLIB_SOCKETS)
	$(CXX) $(CXXFLAGS) $< -o $@ $(LDFLAGS) $(SOCKETS_LDFLAGS) $(SOCKETS_LDLIBS) $(X10RT_TEST_LDFLAGS)

ifdef CUSTOM_PGAS
lib/libxlpgas_sockets.a: $(COMMON_OBJS) $(CUSTOM_PGAS)/lib/libxlpgas_sockets.a include/pgasrt.h
	$(CP) $(CUSTOM_PGAS)/lib/libxlpgas_sockets.a lib/libxlpgas_sockets.a

$(PGAS_EXECUTABLES): $(PGAS_EXECUTABLES:%=$(CUSTOM_PGAS)/%)
	-$(CP) $^ bin/
else
$(SOCKETS_TGZ).phony:
	-$(WGET) -q -N  "http://dist.codehaus.org/x10/binaryReleases/$(X10_VERSION)/$(SOCKETS_TGZ)"

$(SOCKETS_TGZ): $(SOCKETS_TGZ).phony

lib/libxlpgas_sockets.a: $(COMMON_OBJS) $(SOCKETS_TGZ)
	$(GZIP) -cd $(SOCKETS_TGZ) | $(TAR) -xf -
endif

ifdef X10_STATIC_LIB
# On the Mac, AR=libtool, and the target library is overwritten, so the initial $(CP) is harmless.
# However, we do need to link in the original archive.
ifeq ($(subst 64,,$(X10RT_PLATFORM)),darwin)
DARWIN_EXTRA_LIB:=lib/libxlpgas_sockets.a
endif
$(PGAS_DYNLIB_SOCKETS): $(COMMON_OBJS) lib/libxlpgas_sockets.a
	$(CP) lib/libxlpgas_sockets.a $@
	$(AR) $(ARFLAGS) $@ $(DARWIN_EXTRA_LIB) $(COMMON_OBJS)
else
$(PGAS_DYNLIB_SOCKETS): $(COMMON_OBJS) lib/libxlpgas_sockets.a
	$(CXX) $(CXXFLAGS) $(CXXFLAGS_SHARED) $(LDFLAGS_SHARED) -o $@ $^
endif


etc/x10rt_pgas_sockets.properties:
	@echo "CXX=$(CXX)" > $@
	@echo "LDFLAGS=$(SOCKETS_LDFLAGS)" >> $@
	@echo "LDLIBS=$(SOCKETS_LDLIBS)" >> $@

.PRECIOUS: etc/x10rt_pgas_sockets.properties
.PHONY: $(SOCKETS_TGZ).phony
TGZ += $(SOCKETS_TGZ).phony

endif


ifeq ($(PLATFORM_SUPPORTS_PANE),yes)
ifeq ($(XLPGAS_PANE_EXISTS),yes)
TESTS += $(patsubst test/%,test/%.pgas_pane,$(BASE_TESTS))

PGAS_DYNLIB_PANE = lib/$(LIBPREFIX)x10rt_pgas_pane$(LIBSUFFIX)
LIBS += $(PGAS_DYNLIB_PANE)
PROPERTIES += etc/x10rt_pgas_pane.properties

%.pgas_pane: %.cc $(PGAS_DYNLIB_PANE)
	$(CXX) $(CXXFLAGS) $< -o $@ $(LDFLAGS) -DX10RT_PANE_HACK $(PANE_LDFLAGS) $(PANE_LDLIBS) $(X10RT_TEST_LDFLAGS)

ifdef CUSTOM_PGAS
lib/libxlpgas_pane.a: $(COMMON_OBJS) $(CUSTOM_PGAS)/lib/libxlpgas_pane.a include/pgasrt.h
	$(CP) $(CUSTOM_PGAS)/lib/libxlpgas_pane.a lib/libxlpgas_pane.a
else
HACK=$(shell echo "Your platform has no prebuilt PGAS available.  You must export CUSTOM_PGAS=pgas2/common/work">2)
endif

ifdef X10_STATIC_LIB
$(PGAS_DYNLIB_PANE): $(COMMON_OBJS) lib/libxlpgas_pane.a
	$(CP) lib/libxlpgas_pane.a $@
	$(AR) $(ARFLAGS) $@ $(COMMON_OBJS)
else
$(PGAS_DYNLIB_PANE): $(COMMON_OBJS) lib/libxlpgas_pane.a
ifeq ($(X10RT_PLATFORM),aix_xlc)
	$(SHLINK) $(CXXFLAGS) $(CXXFLAGS_SHARED) $(LDFLAGS_SHARED) $(PANE_ARLIBS) -o $@ $(COMMON_OBJS) -Wl,-bexpfull lib/libxlpgas_pane.a 
else
	$(CXX) $(CXXFLAGS) $(CXXFLAGS_SHARED) $(LDFLAGS_SHARED) $(PANE_ARLIBS) -o $@ $(COMMON_OBJS) -Wl,-bexpfull lib/libxlpgas_pane.a 
endif
endif

etc/x10rt_pgas_pane.properties:
	echo "CXX=$(CXX)" > $@
	echo "LDFLAGS=$(PANE_LDFLAGS)" >> $@
	echo "LDLIBS=$(PANE_LDLIBS)" >> $@

.PRECIOUS: etc/x10rt_pgas_pane.properties
.PHONY: $(PANE_TGZ).phony
TGZ += $(PANE_TGZ).phony

endif #XLPGAS_PANE_EXISTS
endif #PLATFORM_SUPPORTS_PANE


ifeq ($(PLATFORM_SUPPORTS_LAPI),yes)
ifeq ($(XLPGAS_LAPI_EXISTS),yes)
ifeq ($(POE_EXISTS),yes)
TESTS += $(patsubst test/%,test/%.pgas_lapi,$(BASE_TESTS))
else
HACK=$(shell echo "Your platform supports LAPI but we could not find the poe executable so not building LAPI tests">2)
endif

PGAS_DYNLIB_LAPI = lib/$(LIBPREFIX)x10rt_pgas_lapi$(LIBSUFFIX)
LIBS += $(PGAS_DYNLIB_LAPI)
PROPERTIES += etc/x10rt_pgas_lapi.properties

%.pgas_lapi: %.cc $(PGAS_DYNLIB_LAPI)
	$(CXX) $(CXXFLAGS) $< -o $@ $(LDFLAGS) $(LAPI_LDFLAGS) $(LAPI_LDLIBS) $(X10RT_TEST_LDFLAGS)

ifdef CUSTOM_PGAS
lib/libxlpgas_lapi.a: $(COMMON_OBJS) $(CUSTOM_PGAS)/lib/libxlpgas_lapi.a include/pgasrt.h
	$(CP) $(CUSTOM_PGAS)/lib/libxlpgas_lapi.a lib/libxlpgas_lapi.a
else
$(LAPI_TGZ).phony:
	-$(WGET) -q -N  "http://dist.codehaus.org/x10/binaryReleases/$(X10_VERSION)/$(LAPI_TGZ)"

$(LAPI_TGZ): $(LAPI_TGZ).phony

lib/libxlpgas_lapi.a: $(COMMON_OBJS) $(LAPI_TGZ)
	$(GZIP) -cd $(LAPI_TGZ) | $(TAR) -xf -
endif

ifdef X10_STATIC_LIB
$(PGAS_DYNLIB_LAPI): $(COMMON_OBJS) lib/libxlpgas_lapi.a
	$(CP) lib/libxlpgas_lapi.a $@
	$(AR) $(ARFLAGS) $@ $(COMMON_OBJS)
else
$(PGAS_DYNLIB_LAPI): $(COMMON_OBJS) lib/libxlpgas_lapi.a
ifeq ($(X10RT_PLATFORM),aix_xlc)
	$(SHLINK) $(CXXFLAGS) $(CXXFLAGS_SHARED) $(LDFLAGS_SHARED) $(LAPI_LDDEPS) -o $@ $^
else
	$(CXX) $(CXXFLAGS) $(CXXFLAGS_SHARED) $(LDFLAGS_SHARED) $(LAPI_LDDEPS) -o $@ $^
endif
endif

etc/x10rt_pgas_lapi.properties:
	echo "CXX=$(CXX)" > $@
	echo "LDFLAGS=$(LAPI_LDFLAGS)" >> $@
	echo "LDLIBS=$(LAPI_LDLIBS)" >> $@

.PRECIOUS: etc/x10rt_pgas_lapi.properties
.PHONY: $(LAPI_TGZ).phony
TGZ += $(LAPI_TGZ).phony

endif #XLPGAS_LAPI_EXISTS
endif #PLATFORM_SUPPORTS_LAPI


ifeq ($(PLATFORM_SUPPORTS_BGP),yes)

TESTS += $(patsubst test/%,test/%.pgas_bgp,$(BASE_TESTS))
LIBS += lib/libx10rt_pgas_bgp.a
PROPERTIES += etc/x10rt_pgas_bgp.properties

%.pgas_bgp: %.cc lib/libx10rt_pgas_bgp.a
	$(CXX) $(CXXFLAGS) $< -o $@ $(LDFLAGS) $(BGP_LDFLAGS) $(BGP_LDLIBS) $(X10RT_TEST_LDFLAGS)

ifdef CUSTOM_PGAS
lib/libxlpgas_bgp.a: $(COMMON_OBJS) $(CUSTOM_PGAS)/lib/libxlpgas_bgp.a include/pgasrt.h
	$(CP) $(CUSTOM_PGAS)/lib/libxlpgas_bgp.a lib/libxlpgas_bgp.a
else
$(BGP_TGZ).phony:
	-$(WGET) -q -N  "http://dist.codehaus.org/x10/binaryReleases/$(X10_VERSION)/$(BGP_TGZ)"

$(BGP_TGZ): $(BGP_TGZ).phony

lib/libxlpgas_bgp.a: $(COMMON_OBJS) $(BGP_TGZ)
	$(GZIP) -cd $(BGP_TGZ) | $(TAR) -xf -
endif

lib/libx10rt_pgas_bgp.a: $(COMMON_OBJS) lib/libxlpgas_bgp.a
	$(CP) lib/libxlpgas_bgp.a lib/libx10rt_pgas_bgp.a
	$(AR) $(ARFLAGS) $@ $(COMMON_OBJS)

etc/x10rt_pgas_bgp.properties:
	@echo "CXX=$(CXX)" > $@
	@echo "LDFLAGS=$(BGP_LDFLAGS)" >> $@
	@echo "LDLIBS=$(BGP_LDLIBS)" >> $@

.PRECIOUS: etc/x10rt_pgas_bgp.properties
.PHONY: $(BGP_TGZ).phony
TGZ += $(BGP_TGZ).phony

endif


debug::
	@echo pgas.mk X10RT_PLATFORM = $(X10RT_PLATFORM)
	@echo pgas.mk ENABLE_X10RT_CUDA = $(ENABLE_X10RT_CUDA)
	@echo pgas.mk DISABLE_X10RT_CUDA = $(DISABLE_X10RT_CUDA)
	@echo pgas.mk ENABLE_X10RT_MPI = $(ENABLE_X10RT_MPI)
	@echo pgas.mk DISABLE_X10RT_MPI = $(DISABLE_X10RT_MPI)
	@echo pgas.mk ENABLE_X10RT_PGAS = $(ENABLE_X10RT_PGAS)
	@echo pgas.mk DISABLE_X10RT_PGAS = $(DISABLE_X10RT_PGAS)
	@echo pgas.mk PLATFORM_SUPPORTS_LAPI = $(PLATFORM_SUPPORTS_LAPI)
	@echo pgas.mk PLATFORM_SUPPORTS_SOCKETS = $(PLATFORM_SUPPORTS_SOCKETS)
	@echo pgas.mk PLATFORM_SUPPORTS_BGP = $(PLATFORM_SUPPORTS_BGP)
	@echo pgas.mk CUSTOM_PGAS = $(CUSTOM_PGAS)
	@echo pgas.mk XLPGAS_LAPI_EXISTS = $(XLPGAS_LAPI_EXISTS)
	@echo pgas.mk XLPGAS_SOCKETS_EXISTS = $(XLPGAS_SOCKETS_EXISTS)
	@echo pgas.mk XLPGAS_BGP_EXISTS = $(XLPGAS_BGP_EXISTS)
	@echo pgas.mk LIBS = $(LIBS)
	@echo pgas.mk PROPERTIES = $(PROPERTIES)
	@echo pgas.mk TESTS = $(TESTS)

# vim: ts=8:sw=8:noet
