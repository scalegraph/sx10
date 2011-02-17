#!/bin/bash

svn_command=export
workdir=/tmp/x10-distribution

while [ $# != 0 ]; do

  case $1 in
    -version)
	export X10_VERSION=$2
	shift
    ;;

    -tag)
	export X10_TAG=$2
	shift
    ;;

    -dir)
        workdir=$2
	shift
    ;;
   esac
   shift
done

if [[ -z "$X10_VERSION" ]]; then
    echo "usage: $0 must give X10 version as -version <version>"
    exit 1
fi

if [[ -z "$X10_TAG" ]]; then
    echo "usage: $0 must give X10 tag as -tag <svn tag>"
    exit 1
fi

date

distdir=$workdir/x10-benchmarks-$X10_VERSION

echo
echo cleaning $workdir
rm -rf $workdir
mkdir -p $workdir || exit 1
mkdir -p $workdir/x10-benchmarks-$X10_VERSION

(
cd $workdir/x10-benchmarks-$X10_VERSION

echo
echo "getting benchmarks "
for i in \
        BC \
        GLB \
        hpcc \
        kmeans \
        SSCA1 \
        UTS
do
    svn $svn_command https://x10.svn.sourceforge.net/svnroot/x10/benchmarks/tags/$X10_TAG/$i
done
)

tarfile="x10-benchmarks-$X10_VERSION"".tgz"
echo "The benchmarks are now exported to the directory $workdir"

cd $workdir
eval tar -cvzf "$tarfile" x10-benchmarks-$X10_VERSION

