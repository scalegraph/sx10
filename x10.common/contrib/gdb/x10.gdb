# a few things that are useful for debugging x10 programs with gdb

set print static-members off
break x10aux::throwNPE
break x10aux::throwArrayIndexOutOfBoundsException
handle SIGPWR SIGXCPU nostop noprint
