/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2010.
 */

#include <x10aux/config.h>

#include <cstdlib>
#include <cstring>

bool getBoolEnvVar(const char* name) {
    char* value = getenv(name);
    return (value && !(strcasecmp("false", value) == 0) && !(strcasecmp("0", value) == 0) && !(strcasecmp("f", value) == 0));
}

const bool x10aux::trace_ansi_colors = getBoolEnvVar("X10_TRACE_ANSI_COLORS");
const bool x10aux::trace_static_init = getBoolEnvVar("X10_TRACE_STATIC_INIT") || getBoolEnvVar("X10_TRACE_ALL");
const bool x10aux::trace_x10rt = getBoolEnvVar("X10_TRACE_X10RT") || getBoolEnvVar("X10_TRACE_NET") || getenv("X10_TRACE_ALL");
const bool x10aux::trace_ser = getBoolEnvVar("X10_TRACE_SER") || getBoolEnvVar("X10_TRACE_NET") || getBoolEnvVar("X10_TRACE_ALL");
const bool x10aux::trace_rxtx = getBoolEnvVar("X10_TRACE_RXTX") || getBoolEnvVar("X10_TRACE_NET") || getBoolEnvVar("X10_TRACE_ALL");

const bool x10aux::disable_dealloc = getBoolEnvVar("X10_DISABLE_DEALLOC");

const bool x10aux::x10__assertions_enabled = !getBoolEnvVar("X10_DISABLE_ASSERTIONS");

bool x10aux::get_congruent_huge() {
    return getBoolEnvVar(ENV_CONGRUENT_HUGE);
}

char* x10aux::get_congruent_base() {
    return getenv(ENV_CONGRUENT_BASE);
}

char* x10aux::get_congruent_size() {
    return getenv(ENV_CONGRUENT_SIZE);
}
