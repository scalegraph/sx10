#!/bin/bash

#
# (c) Copyright IBM Corporation 2009-2013
#
# $Id$
# Harness to execute a set of tests with Native X10
# and generate .xml files for each test case in JUnit format
# 


# display command-line help
# usage: printUsage excode detail
function printUsage {
	printf "\n=====> X10/C++ Test Harness\n\n"
	printf "Usage: xtestScript++ [-t|-timeOut [secs]] [-f|-force]\n"
	printf "[-l|-list \"test1 test2 ... testn\"]]\n"
	printf "    [-v|-verbose] [-h|-help]\n\n"
	if [[ $2 > 0 ]]; then
		printf "The xtestScript++ runs the pre-validated *.x10 test cases"
		printf " in the current\ndirectory and its subdirectories, and"
		printf " places the test results in a log\nfile under the"
		printf " specified log directory.\n\n"
		printf -- "-t | -timeOut [secs]\n"
		printf "  Enable timeout option for test case execution. This"
		printf " overrides\nthe default timeout value of 60 seconds.\n\n"
		printf -- "-f | -force\n"
		printf "  Run this script in the top-level directory where the *.x10"
		printf " \ntest cases can be located.  The current directory must"
		printf " have a magic\nfile \".ThisIsAnX10TestDirectory\" present"
		printf " in the directory to launch\nthe harness.  Use this option,"
		printf " to force launch the harness, even\nif the magic file is"
		printf " not present.\n\n"
		printf -- "-logPath dir\n"
		printf "  Specify the directory path where generated log files"
		printf " will be\nstored.  If not specified, the current path's"
		printf " \"log\" entry\nwill be used for this purpose.\n\n"
		printf -- "-listFile file\n"
		printf "  Test cases are found recursively starting at the top"
		printf " test\ndirectory, and are executed in the same order as"
		printf " they are found.\nThis option indicates a file containing"
		printf " a list of shell file\npatterns denoting the tests to run."
		printf "  The test list file can\nalso contain comment lines"
		printf " beginning with a \"#\" character,\nand blank lines for"
		printf " clarity.\n\n"
		printf -- "-l | -list test1 test2 ... testn\n"
		printf "  Alternatively, use this option to indicate the file"
		printf " pattern(s)\nfor the tests to run.  With these options,"
		printf " only the specified tests\nare run.\n\n"
		printf -- "-runFile file\n"
		printf "  Additional information about test cases during execution"
		printf " can be\nprovided through this data file. Please look into"
		printf " the sample\nruntime data file provided for details.\n\n"
		printf -- "-h | -help\n"
		printf "  Print this help message.\n\n"
	fi
	exit $1
}

# parse command line options and arguments
# exit code: 1
# usage: parseCmdLine args
function parseCmdLine {
	while (( $# > 0 )); do
		if [[ "$1" == "-timeOut" || "$1" == "-t" ]]; then
			tctimeout=1
			shift
			if (( $# >= 1 )); then
				typeset tmp=$(echo $1 | cut -c 1)
				if [[ "$tmp" != "-" ]]; then
					# this takes care of non-integer argument
					(( tctoutval = $1 ))
					if (( $? != 0 || $tctoutval <= 0 )); then
						printf -- "\n[${prog}: err]: Timeout value $tctoutval is invalid\n\n"
						printUsage 1 0
					fi
					shift
				fi
			fi
		elif [[ "$1" == "-force" || "$1" == "-f" ]]; then
			tcforce=1
			shift
		elif [[ "$1" == "-junit_report" && $# -ge 2 ]]; then
		        TLOGF=$2
			shift 2
		elif [[ "$1" == "-logPath" && $# -ge 2 ]]; then
			if [[ ! -d "$2" ]]; then
				printf "\n[${prog}: err]: Log directory $2 must exist\n"
				cleanUpExitHarness 1
			fi
			tclogpath=$2
			shift 2
		elif [[ "$1" == "-listFile" && $# -ge 2 ]]; then
			if [[ ! -r "$2" ]]; then
				printf "\n[${prog}: err]: List file $2 must exist & be readable\n"
				cleanUpExitHarness 1
			fi
			tcpatfile=$2
			shift 2
		elif [[ "$1" == "-runFile" && $# -ge 2 ]]; then
			if [[ ! -r "$2" ]]; then
				printf "\n[${prog}: err]: Run file $2 must exist & be readable\n"
				cleanUpExitHarness 1
			fi
			tcrunfile=$2
			shift 2
		elif [[ "$1" == "-list" || "$1" == "-l" ]]; then
			if (( $# >= 2 )); then
				tcpatlist="$2"
				shift 2
			fi
		elif [[ "$1" == "-help" || "$1" == "-h" ]]; then
			printUsage 0 1
		elif [[ "$1" == "-logPath" || "$1" == "-listFile" || "$1" == "-runFile" || "$1" == "-l" || "$1" == "-list" ]]; then
			printf "\n[${prog}: err]: Option $1 needs argument\n\n"
			printUsage 1 0
		elif [[ "$1" == -* ]]; then
			printf "\n[${prog}: err]: Unrecognized option $1\n\n"
			printUsage 1 0
		else
			printf "\n[${prog}: err]: Extraneous argument(s) $1\n\n"
			printUsage 1 0
		fi
	done
	
	# need either test pattern list or file
	if [[ ! -z "$tcpatlist" && ! -z "$tcpatfile" ]]; then
		printf "\n[${prog}: err]: Conflicting test pattern specifications...\n\n"
		printUsage 1 0
	fi
	
	# extract pattern list from file
	if [[ ! -z "$tcpatfile" ]]; then
		cat $tcpatfile | while read -r line; do
			line=$(echo $line | sed -e 's;^[ \t]+;;')
			if [[ ! -z "$line" && "$line" != \#* ]]; then
				tcpatlist="$tcpatlist $line"
			fi
		done
	fi
	
	# neither pattern file or list present
	# choose every valid x10 file under current directory
	if [[ -z "$tcpatlist" ]]; then
		tcpatlist="."
	fi
}

# generate list of tests for the specified pattern(s)
# usage: findTests testpat(s)
function findTests {
	local testPat="$1"
	local findpat=""
	local dirlist

	#eval "find $1 -type d -name '.svn' -prune -o -type f -name '*.x10' -print"
	if [[ -n "$testPat" && "${testPat}" != "." ]]; then
		#findpat="$(printf "%q " "${testPat}" | sed 's/\\\\\([\*\?]\)/\1/g')"
		eval "find $testPat -type d -name '.svn' -prune -o -type f -name '*.x10'  -print | sort"
	else
		dirlist="$(eval "find -type d -name '.svn' -prune -o -type d -name '*' -print | sort")"
		dirlist="$(echo $dirlist | tr '\n' ' ')"
		eval "find $dirlist -maxdepth 1 -type f -name '*.x10' -print"
	fi
}

# check whether the specified test case file is valid
# usage: isTestCase file.x10
function isTestCase {
	typeset file=$(basename $1)
	
	if [[ "${file#${file%\.*}}" != ".x10" ]]; then
		printf "\n[$prog: err]: ${file} need an .x10 extension\n"
		return 1
	fi

	if [[ ! -r $1 ]]; then
		printf "\n[$prog: err]: ${file} must exist & is readable\n"
		return 1
	fi

	printf "\n===> ${EGREP} -q 'public[ ]+static[ ]+def[ ]+main' $1\n\n" 1>&2
	${EGREP} -q 'public[ ]+static[ ]+def[ ]+main' $1
	if [[ $? != 0 ]]; then
		printf "\n[$prog: err]: no \"public static def main\" method in ${file}\n"
		return 1
	fi

	printf "\n===> ${EGREP} -q 'MANAGED_X10_ONLY' $1\n\n" 1>&2
	${EGREP} -q 'MANAGED_X10_ONLY' $1
	if [[ $? == 0 ]]; then
		printf "\n[$prog: err]: ${file} contains MANAGED_X10_ONLY directive\n"
		return 1
	fi

	return 0
}

# usage: resolveParams
function resolveParams {
	# validation code
	case "${tctarget}" in
		*_MustFailCompile)
			tcvcode=FAIL_COMPILE
			;;
		*_MustFailTimeout)
			tcvcode=FAIL_TIMEOUT
			;;
		*)
			tcvcode=SUCCEED
			;;
	esac

	# update expected counters
	case "${tcvcode}" in
		"SUCCEED")
			let 'xtcpasscnt += 1'
			;;
		"FAIL_COMPILE")
			let 'xtcfcompcnt += 1'
			;;
		"FAIL_TIMEOUT")
			let 'xtcftoutcnt += 1'
			;;
	esac
}

# execute the given command line with timelimit
# usage: execTimeOut val cmd
function execTimeOut {
	typeset timeout=$1
	shift
	typeset outfile=$1
	shift
	printf "\n===> $@ >> $outfile &\n\n" 1>&2
	printf "\n" >> $outfile
	"$MYDIR"/xnewpgrp "$@" >> $outfile 2>&1 &
	typeset cmd_pid=$!
	printf "\n===> sleep $timeout && kill -9 -$cmd_pid >/dev/null 2>&1 &\n\n" 1>&2
	"$MYDIR"/xnewpgrp "sleep $timeout && kill -9 -$cmd_pid && echo 'Timeout' >> $outfile" >/dev/null 2>&1 &
	typeset sleep_pid=$!
	printf "\n===> wait $cmd_pid 2>/dev/null\n\n" 1>&2
	wait $cmd_pid >/dev/null 2>&1
	typeset rc=$?
	printf "\n===> kill -9 -$sleep_pid 2>/dev/null\n\n" 1>&2
	kill -9 -$sleep_pid >/dev/null 2>&1
	return $rc
}

# the following needs to be defined outside main
# program name
MYDIR=$(cd $(dirname $0) && pwd)
X10_HOME=$X10_HOME
if [[ -z "$X10_HOME" ]]; then
	X10_HOME=$(cd $MYDIR/../..; pwd)
fi
X10TEST_PATH=$X10_HOME/x10.tests/examples
prog=x10c++th

# platform independent abstraction for certain commands
EGREP=egrep
egrep --version 2>/dev/null 1>/dev/null
if [[ $? == 0 && $(uname -s) != Sun* && $(uname -s) != CYGWIN* && $(uname -s) != Linux* ]]; then
	EGREP="egrep -E"
fi

# generate unique timestamp (tctimestamp)
tcdate=$(date '+%Y-%m-%d')
tctime=$(date '+%H:%M:%S')
tctimestamp="$(echo $tcdate | sed -e 's;-;;g').$(echo $tctime | sed -e 's;:;;g')"

# unique name for temporary directory (tctmpdir)
if [[ -z "${TMPDIR}" ]]; then
	TMPDIR=/tmp
fi
tctmpdir=${TMPDIR}/${prog}.$$.${tctimestamp}
mkdir -p $tctmpdir

# default values
DEFAULT_TIMEOUT=360
DEFAULT_LOGPATH="log"
DEFAULT_NPLACES=2
MAX_NPLACES=4

# the value will be set later
export X10CPP_LOGLOC=""
export X10CPP_RUNLOG="${prog}.run.log"
export X10CPP_ERRLOG="${prog}.err.log"

# test case globals

# test harness current run state
# could be one of:
# PARSING_CMDLINE, LIST_PREPARATION, TEST_PROCESSING,
# REPORT_GENERATION, UNKNOWN_STATE
thrunstate="UNKNOWN_STATE"

# debug variable
tctestrun=0

# enable/disable timeout option
# default: enable
typeset -i tctimeout=1

# default timeout value, if timeout is enabled
typeset -i tctoutval=$DEFAULT_TIMEOUT
typeset -i tccomptout=420
# run tests even if the script is launched in a non-test dir
# default: no
typeset -i tcforce=0

# default log path, where log file will be created
typeset tclogpath=$DEFAULT_LOGPATH

# test case pattern file
# default: none
typeset tcpatfile=""

# test case run data file
# default: none
typeset tcrunfile=""

# test pattern list
# default: none
typeset tcpatlist=""

# path to temporary log file(s)
# run and error logs
# need to capture text even before command-line parsing is done
tctmprlog=${tctmpdir}/${prog}.run.${tctimestamp}.log
tctmprhlog=${tctmpdir}/${prog}.run.${tctimestamp}.head.log
tctmpelog=${tctmpdir}/${prog}.err.${tctimestamp}.log
tctmpreport=${tctmpdir}/${prog}.${tctimestamp}.report
# final log(s) will be available here
tcrlogfile=""
tcelogfile=""

# various micro counters
# total number of test case files considered
tctotalcnt=0
# number of test cases passed
tcpasscnt=0
# number of test cases failed
tcfailcnt=0
# number of test case files processed so far
tcproccnt=0
# number of test cases having invalid validation code
tcfvcodecnt=0

# total number of valid test cases
tcvalidcnt=0

# number of test cases successfully compiled
tccompcnt=0
# number of expected compilation failures
xtcfcompcnt=0
# number of actual compilation failures
tcfcompcnt=0

# number of test cases successfully executed
tcexeccnt=0
# number of actual execution failures
tcfexeccnt=0

# number of expected timeout failures
xtcftoutcnt=0
# number of actual timeout failures
tcftoutcnt=0
# number of expected successes
xtcpasscnt=0

# following are needed for webfication
# start & end timings
tcstime=""
tcetime=""

# initialize test case globals
# usage: init args
function init {

	# parse command-line arguments
	thrunstate=PARSING_CMDLINE
	parseCmdLine "$@"

	# validate the test launch directory
	# exit code: 1
	if [[ $tcforce != 1 && ! -e .ThisIsAnX10TestDirectory ]]; then
		printf "\n[$prog: err]: $(basename $(pwd)) not a valid X10 launch dir\n"
		exit 1
	fi

	# set final log destination(s)
	tcrlogfile=${tclogpath}/${prog}.run.${tctimestamp}.log
	tcelogfile=${tclogpath}/${prog}.err.${tctimestamp}.log
}

function xmlLog {
	TOUTF=$TLOGF
	if [[ -z "$TLOGF" ]]; then TLOGF=/dev/null; TOUTF=/dev/stderr; fi
	__cat_test_end_time=$(perl -e 'print time;')
	let '__cat_test_duration = __cat_test_end_time - __cat_test_start_time'
	printf "\n\t<testcase>\n" >> $TLOGF
	printf "\t\t<name>${__cat_test_name}</name>\n" >> $TLOGF
	printf "\t\t\t<time>${__cat_test_duration}</time>\n" >> $TLOGF
	if [[ "${__cat_test_result}" != "SUCCESS" ]]; then
	    printf "\t\t<failure>\n" >> $TLOGF
	    printf "\t\t\t<type>${__cat_test_result}</type>\n" >> $TLOGF
	    printf "\t\t\t<message>${__cat_test_result_explanation}</message>\n" >> $TLOGF
	    printf "\t\t</failure>\n" >> $TLOGF
	fi
	printf "\t\t\t<system-out>\n" >> $TLOGF
	perl -pe 's/&/\&amp;/g;
	          s/</\&lt;/g;
	          s/>/\&gt;/g;
	          s/"/\&quot;/g;
	          s/'"'"'/\&apos;/g;
	          s/([^[:print:]\t\n\r])/sprintf("\&#%04x;", ord($1))/eg' $1 >> $TOUTF
	printf "\t\t\t</system-out>\n" >> $TLOGF
	printf "\t\t</testcase>\n" >> $TLOGF
}

# main routine that invokes the rest
function main {

	# log invocation options
	printf "\n<<Invocation Options>>\n"
	printf "\nGlobal Timeout: "
	if [[ $tctimeout == 1 ]]; then
		printf "Enabled\n"
	else
		printf "Disabled\n"
	fi
	printf "Global Timeout Value: $tctoutval\n"
	printf "Run Log File: $tcrlogfile\n"
	printf "Error Log File: $tcelogfile\n"
	printf "\nTestcase Pattern List: $tcpatlist\n"

	# set test case build environment
	X10CPP=$X10CPP
	if [[ -z "$X10CPP" ]]; then
		X10CPP=$X10_HOME/x10.dist/bin/x10c++
	fi
	if [[ ! -f $X10CPP ]]; then
		printf "\n[$prog: err]: unable to locate x10c++ compiler!\n"
		exit 2
	fi
	if [[ "$(uname -s)" == "AIX" ]]; then
		X10CPP="$X10CPP -x10rt sockets"
	elif [[ "$(uname -s)" == "Linux" && "$(uname -m)" == "x86_64" ]]; then
	    X10CPP="$X10CPP -x10rt sockets"
	elif [[ "$(uname -s)" == "Linux" && "$(uname -m)" == "ppc64" ]]; then
	    X10CPP="$X10CPP -x10rt sockets"
	elif [[ "$(uname -s)" == "SunOS" && "$(uname -p)" == "sparc" ]]; then
	    X10CPP="$X10CPP -x10rt sockets"
	elif [[ "$(uname -s)" == CYGWIN* && "$(uname -m)" == "i686" ]]; then
	    X10CPP="$X10CPP -x10rt sockets"
	elif [[ "$(uname -s)" == "Darwin" ]]; then
	    X10CPP="$X10CPP -x10rt sockets"
	fi

	RUN_X10=$RUN_X10
	if [[ -z "$RUN_X10" ]]; then
		RUN_X10=$X10_HOME/x10.dist/bin/runx10
	fi
	if [[ ! -f $RUN_X10 ]]; then
		printf "\n[$prog: err]: unable to locate runx10 script!\n"
		exit 2
	fi

	if [[ ! -f $X10_HOME/x10.dist/stdlib/libx10.properties ]]; then
		printf "\n[$prog: err]: unable to libx10.properties!\n"
		exit 2
	fi

	# prepare test case list
	# exit code: 3
	thrunstate=LIST_PREPARATION
	printf "\n<<Testcase List Preparation>>\n"
	declare -a tclist=($(findTests "$tcpatlist"))
	if [[ ${#tclist[*]} == 0 ]]; then
		printf "\n[$prog: err]: zero tests found under $(basename $(pwd))\n"
		exit 3
	fi
	printf "\n===> ${tclist[*]}\n\n"
	tctotalcnt=${#tclist[*]}


	thrunstate=TEST_PROCESSING
	printf "\n<<Testcase Processing>>\n"
	printf "\n# Legend:\n"
	printf "#\tC - compilation step\n"
	printf "#\tE - execution step\n\n"
	printf "#\tX - test failed\n"
	printf "#\tY - test passed\n"

	for tc in ${tclist[*]}; do
		let 'tcproccnt += 1'
		printf "\n((${tcproccnt})) [$(basename $(dirname $tc)):$(basename $tc)]"
		printf "\n===>((${tcproccnt})) [$(basename $tc)]\n\n" 1>&2
		# pre-validate the test case
		isTestCase $tc
		if [[ $? != 0 ]]; then
			continue
		fi
		let 'tcvalidcnt += 1'

		__cat_test_start_time=$(perl -e 'print time;')

		# create the test root
		tctarget=$(basename $tc | sed -e 's;.x10;;')
		local tPkg=$(sed -ne 's|\.|/|g' -e 's|^\s*package \([^;]*\);|\1|p' "$tc")

		local className="${tPkg}/${tctarget}"
		className=${className#\.\/}
		className=`echo "$className" | sed -e 's/\//\./g'`
		className=${className#\.}
		__cat_test_name="$className"
		local testDir=$(dirname $tc)
		local tDirSlash=${testDir%/$tPkg}
		local tDir=${tDirSlash#\.\/}
		tDir=`echo "$tDir" | sed -e 's/\//\./g'`
		__cat_current_group="$tDir"
		if [[ -d ${tcroot} ]]; then
			rm -rf ${tcroot}
		fi
		tcroot=$tctmpdir/$tctarget
		printf "\n===> mkdir -p $tcroot\n\n" 1>&2
		mkdir -p $tcroot

		# resolve test case parameters
		# tcvcode
		resolveParams
		
		# try & generate sources
		printf " +C [COMPILATION]"
		extends="$(sed -ne 's|^.*extends\s*\(.*\)\s*[{]|\1|p' $tc | sed -n '1,1p')"
		if [[ -n "$extends" && "$extends" != *x10Test* ]]; then
			extendList=$(echo "$(dirname $tc)/$extends.x10" | tr -d ' \r')
		else
			extendList=""
		fi
		extra_opts="$(sed -ne 's|^\s*//\s*OPTIONS*\:\s*\(.*\)|\1|p' $tc)"
		extra_sourcepath="$(sed -ne 's|^\s*//\s*SOURCEPATH*\:\s*\(.*\)|\1|p' $tc)"
                [ -n "$extra_sourcepath" ] && extra_sourcepath="-sourcepath \"$extra_sourcepath\""
		__cat_test_x10c_sourcepath="$tcroot"
		__cat_test_x10c_classpath="${EXTRA_CLASSPATH}"
		__cat_test_x10c_directory="$testDir"
		if [[ "$(uname -s)" == CYGWIN* ]]; then
		    comp_cmd="${X10CPP} $extra_opts $extra_sourcepath $X10CPP_EXTRA_OPTS -t -v -report postcompile=1 -CHECK_INVARIANTS=true -MAIN_CLASS=$className -o \"$(cygpath -am $tcroot)/$tctarget\" -sourcepath \"$(cygpath -am $X10_HOME/x10.tests/examples/$tDirSlash)\" -sourcepath \"$(cygpath -am $X10_HOME/x10.tests/examples/$testDir)\" -sourcepath \"$(cygpath -am $X10_HOME/x10.tests/examples/x10lib)\" -sourcepath \"$(cygpath -am $tcroot)\" -sourcepath \"$(cygpath -am $X10_HOME/x10.dist/samples)\"  -sourcepath \"$(cygpath -am $X10_HOME/x10.dist/samples/tutorial)\" -sourcepath \"$(cygpath -am $X10_HOME/x10.dist/samples/CUDA)\" -sourcepath \"$(cygpath -am $X10_HOME/x10.dist/samples/work-stealing)\" -d \"$(cygpath -am $tcroot)\" $tc $extendList \"$(cygpath -am ${X10TEST_PATH}/x10lib/harness/x10Test.x10)\""
		else
		    comp_cmd="${X10CPP} $extra_opts $extra_sourcepath $X10CPP_EXTRA_OPTS -t -v -report postcompile=1 -CHECK_INVARIANTS=true -MAIN_CLASS=$className -o $tcroot/$tctarget -sourcepath $X10_HOME/x10.tests/examples/$tDirSlash -sourcepath $X10_HOME/x10.tests/examples/$testDir -sourcepath $X10_HOME/x10.tests/examples/x10lib -sourcepath $tcroot -sourcepath $X10_HOME/x10.dist/samples -sourcepath $X10_HOME/x10.dist/samples/tutorial -sourcepath $X10_HOME/x10.dist/samples/CUDA -sourcepath $X10_HOME/x10.dist/samples/work-stealing -d $tcroot $tc $extendList ${X10TEST_PATH}/x10lib/harness/x10Test.x10"
		fi
		tccompdat=${tcroot}/${tctarget}.comp
		printf "\n****** $tDir $className ******\n\n" >> $tccompdat

		__cat_test_x10_command=""
		__cat_test_x10c_command="$(echo execTimeOut $tccomptout $tccompdat \"${comp_cmd}\")"
		execTimeOut $tccomptout $tccompdat "${comp_cmd}"
		rc=$?
		cat ${tccompdat} 1>&2
		if [[ $rc != 0 && "$tcvcode" == "FAIL_COMPILE" ]]; then
			let 'tcfcompcnt += 1'
			__cat_test_exit_code=$rc
            ${EGREP} "Exception in thread" $tccompdat >/dev/null 2>&1
            if [[ $? == 0 ]]; then
                printf " *** X ***"
                let 'tcfailcnt += 1'
                printf "\n[$prog: err]: compile time exception for ${className}\n"
                __cat_test_result_explanation="${className} did not meet expectation: expected=MustFailCompile actual=FailCompileWithException (exception in thread main)."
                __cat_test_result="FAILURE"
                printf "\n****** $tDir $className failed: compile time exception\n" >> $tccompdat
            else
			    printf " *** Y ***"
			    let 'tcpasscnt += 1'
			    __cat_test_result_explanation="${className} met expectation: MustFailCompile."
			    __cat_test_result="SUCCESS"
			    printf "\n****** $tDir $className succeeded.\n" >> $tccompdat
            fi
			xmlLog $tccompdat
			continue
		elif [[ $rc == 0 && "$tcvcode" == "FAIL_COMPILE" ]]; then
			printf " *** X ***"
			let 'tcfvcodecnt += 1'
			let 'tcfailcnt += 1'
			printf "\n[$prog: err]: invalid validation code for ${className}\n"
			__cat_test_result_explanation="${className} did not meet expectation: expected=MustFailCompile actual=Succeed (invalid validation code)."
			__cat_test_result="FAILURE"
			__cat_test_exit_code=42
			printf "\n****** $tDir $className failed: compile\n" >> $tccompdat
			xmlLog $tccompdat
			continue
		elif [[ $rc != 0 && "$tcvcode" != "FAIL_COMPILE" ]]; then
			let 'tcfcompcnt += 1'
			let 'tcfailcnt += 1'
			printf " *** X ***"
			printf "\n[$prog: err]: can't generate c++ sources for ${className}\n"
			__cat_test_result_explanation="${className} did not meet expectation: expected=Succeed actual=FailCompile (failed to generate c++ sources)."
			__cat_test_result="FAILURE"
			__cat_test_exit_code=$rc
			printf "\n****** $tDir $className failed: compile\n" >> $tccompdat
			xmlLog $tccompdat
			continue
		fi
		printf "\n++++++ Compilation succeeded.\n" >> $tccompdat
		let 'tccompcnt += 1'

		# try & run the target	
		# the actual output will be logged here
		#tcoutdat=${tcroot}/${tctarget}.out
		tcoutdat=${tccompdat}
		# extract additional execution details, if available
		my_nplaces=$DEFAULT_NPLACES
		if [[ -n "$tcrunfile" ]]; then
			${EGREP} ${tctarget}\.x10 $tcrunfile >/dev/null 2>&1
			if (( $? == 0 )); then
				my_nplaces=$MAX_NPLACES
			fi
		fi

		if [[ "$(uname -s)" == "Darwin" ]]; then
                    pid_list=`/usr/sbin/lsof -t -i ':21053'`
		    if (( $? == 0 )); then
			for pid in $pid_list
			do
			    printf "\nkill -9 $pid 2>/dev/null\n\n"
			    kill -9 $pid 2>/dev/null
			done
		    fi
		fi

		if [[ "$(uname -s)" == CYGWIN* ]]; then
		    run_cmd="X10_NPLACES=${my_nplaces} X10_HOSTLIST=localhost /usr/bin/time -p $RUN_X10 ./${tctarget}.exe"
		else
		    run_cmd="X10_NPLACES=${my_nplaces} X10_HOSTLIST=localhost /usr/bin/time -p ./${tctarget}"
		fi
		printf "\n${run_cmd}\n" >> $tcoutdat

		__cat_test_x10_classpath="$EXTRA_CLASSPATH"
		__cat_test_x10_timeout="$tctoutval"
		if [[ $tctimeout == 0 ]]; then
			__cat_test_x10_command="$(echo $run_cmd >> $tcoutdat)"
		else
			__cat_test_x10_command="$(echo execTimeOut $tctoutval $tcoutdat \"${run_cmd}\")"
		fi
		printf " ++ E [EXECUTION]"
		( \
			printf "\n===> cd $tcroot\n" 1>&2; \
			cd $tcroot; \
			if [[ $tctimeout == 0 ]]; then \
				printf "\n===> $run_cmd >> $tcoutdat\n\n" 1>&2; \
				$run_cmd >> $tcoutdat; \
			else \
				execTimeOut $tctoutval $tcoutdat "${run_cmd}"; \
			fi;
		)
		rc=$?
		if [[ $rc == 0 && $tcvcode == "SUCCEED" ]]; then
			let 'tcexeccnt += 1'
			let 'tcpasscnt += 1'
			printf " *** Y ***"
			#printf "\n<<==========OUTPUT=======\n"
			#cat ${tcoutdat}
			#printf "=======================>>\n"
			__cat_test_result_explanation="${className} met expectation: Succeed."
			__cat_test_result="SUCCESS"
			__cat_test_exit_code=$rc
			printf "\n****** $tDir $className succeeded.\n" >> $tcoutdat
			xmlLog $tcoutdat
			continue
		fi
		if [[ $tctimeout == 0 ]]; then
			if [[ $rc > 0 && $tcvcode == "SUCCEED" ]]; then
				let 'tcfailcnt += 1'
				let 'tcfexeccnt += 1'
				printf " *** X ***"
				printf "\n[$prog: err]: failed to execute ${className}\n"
				__cat_test_result_explanation="${className} did not meet expectation: expected=Succeed actual=FailRun."
				__cat_test_result="FAILURE"
				__cat_test_exit_code=$rc
				printf "\n****** $tDir $className failed: run\n" >> $tcoutdat
				xmlLog $tcoutdat
				continue
			else
				printf " *** X ***"
				let 'tcfailcnt += 1'
				let 'tcfvcodecnt += 1'
				printf "\n[$prog: err]: invalid validation code ${className}\n"
				__cat_test_result_explanation="${className} did not meet expectation: expected=Succeed actual=FailRun (invalid validation code)."
				__cat_test_result="FAILURE"
				__cat_test_exit_code=42
				printf "\n****** $tDir $className failed: run\n" >> $tcoutdat
				xmlLog $tcoutdat
				continue
			fi
		else
			if [[ $rc > 128 && $tcvcode == "SUCCEED" ]]; then
				printf " *** X ***"
				let 'tcfailcnt += 1'
				let 'tcftoutcnt += 1'
				let 'tcfexeccnt += 1'
				printf "\n[$prog: err]: ${className} is killed due to timeout\n"
				__cat_test_result_explanation="${className} did not meet expectation: expected=Succeed actual=TimeOut (killed due to timeout)."
				__cat_test_result="FAILURE"
				__cat_test_exit_code=$rc
				printf "\n****** $tDir $className failed: timeout\n" >> $tcoutdat
				xmlLog $tcoutdat
				continue
			elif [[ $rc > 0 && $tcvcode == "SUCCEED" ]]; then
				printf " *** X ***"
				let 'tcfailcnt += 1'
				let 'tcfexeccnt += 1'
				printf "\n[$prog: err]: failed to execute ${className}\n"
				__cat_test_result_explanation="${className} did not meet expectation: expected=Succeed actual=FailRun (execution failed)."
				__cat_test_result="FAILURE"
				__cat_test_exit_code=$rc
				printf "\n****** $tDir $className failed: run\n" >> $tcoutdat
				xmlLog $tcoutdat
				continue
			elif [[ $rc == 0 && $tcvcode == "FAIL_TIMEOUT" ]]; then
				printf " *** X ***"
				let 'tcfailcnt += 1'
				let 'tcfvcodecnt += 1'
				printf "\n[$prog: err]: invalid validation code ${tcvcode}\n"
				__cat_test_result_explanation="${className} did not meet expectation: expected=MustFailTimeOut actual=Succeed (invalid validation code)."
				__cat_test_result="FAILURE"
				__cat_test_exit_code=42
				printf "\n****** $tDir $className failed: run\n" >> $tcoutdat
				xmlLog $tcoutdat
				continue
			elif [[ $rc > 128 && $tcvcode == "FAIL_TIMEOUT" ]]; then
				printf " *** Y ***"
				let 'tcftoutcnt += 1'
				let 'tcfexeccnt += 1'
				let 'tcpasscnt += 1'
				__cat_test_result_explanation="${className} met expectation: MustFailTimeOut."
				__cat_test_result="SUCCESS"
				__cat_test_exit_code=0
				printf "\n****** $tDir $className succeeded.\n" >> $tcoutdat
				xmlLog $tcoutdat
				continue
			elif [[ $rc > 0 && $tcvcode == "FAIL_TIMEOUT" ]]; then
				printf " *** X ***"
				let 'tcfailcnt += 1'
				let 'tcftoutcnt += 1'
				let 'tcfexeccnt += 1'
				printf "\n[$prog: err]: failed to execute ${className}\n"
				__cat_test_result_explanation="${className} did not meet expectation: expected=MustFailTimeOut actual=FailRun (execution failed)."
				__cat_test_result="FAILURE"
				__cat_test_exit_code=$rc
				printf "\n****** $tDir $className failed: run\n" >> $tcoutdat
				xmlLog $tcoutdat
				continue
			fi
		fi
	done

	# prepare the report
	# simple summary for the moment
	thrunstate=REPORT_GENERATION
	printf "\n\n<<Report Generation>>\n" > ${tctmpreport}
	printf "\n\n======================================================================\n\n" >> ${tctmpreport}
	printf "                     X10/C++ Test Harness :: Run Report\n\n" >> ${tctmpreport}
	printf "\n**QUEUE          : " >> ${tctmpreport}
	printf "${tcvalidcnt}\n" >> ${tctmpreport}

	printf "\n**COMPILATION    : " >> ${tctmpreport}
	printf "${tccompcnt} Successes / ${xtcfcompcnt} Expected Failures" >> ${tctmpreport}
	printf " / ${tcfcompcnt} Actual Failures\n" >> ${tctmpreport}

	printf "\n**EXECUTION      : " >> ${tctmpreport}
	printf "${tcexeccnt} Successes" >> ${tctmpreport}
	printf " / ${tcfexeccnt} Actual Failures\n" >> ${tctmpreport}

	printf "\n**TIME-OUT       : " >> ${tctmpreport}
	printf "${xtcftoutcnt} Expected Failures / ${tcftoutcnt} Actual" >> ${tctmpreport}
	printf " Failures\n" >> ${tctmpreport}

	printf "\n**MISCELLANEOUS  : " >> ${tctmpreport}
	printf "${tcfvcodecnt} Invalid Validation Codes\n" >> ${tctmpreport}

	printf "\n**CONCLUSION     : " >> ${tctmpreport}
	printf "${tcpasscnt} Successes / ${tcfailcnt} Failures\n" >> ${tctmpreport}
	printf "\n======================================================================\n\n" >> ${tctmpreport}
}

__cat_opened_group="invalid"
__cat_current_group=""
__cat_test_name=""
__cat_test_x10c_command=""
__cat_test_x10_command=""
__cat_test_parameters=""
__cat_test_exit_code=""
__cat_test_start_time=""
__cat_test_end_time=""
__cat_test_duration=""
__cat_test_result=""
__cat_test_result_explanation=""
__cat_test_output=""
__cat_test_x10c_sourcepath=""
__cat_test_x10c_classpath=""
__cat_test_x10c_directory=""
__cat_test_x10_classpath=""
__cat_test_x10_timeout=""

init "$@"
main 
exit 0

# vim:tabstop=4:shiftwidth=4:expandtab
