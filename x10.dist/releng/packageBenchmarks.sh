#!/bin/bash

# exit if anything goes wrong
set -e

workdir=/tmp/x10-bench-dist

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
    echo "usage: $0 must give X10 tag as -tag <git tag>"
    exit 1
fi

date

distdir=$workdir/x10-benchmarks-$X10_VERSION
repodir_bench=$workdir/x10-bench-git
repodir_apps=$workdir/x10-apps-git

echo
echo cleaning $workdir
rm -rf $workdir
mkdir -p $workdir || exit 1
mkdir -p $workdir/x10-benchmarks-$X10_VERSION

echo
echo cloning x10-benchmarks git repo
cd $workdir
git clone --depth 1 https://github.com/x10-lang/x10-benchmarks.git $repodir_bench

echo
echo extracting PERCS benchmarks from repo
cd $repodir_bench/PERCS
git archive --format=tar $X10_TAG FT KMEANS LU RA SSCA1 SSCA2 STREAM UTS | (cd $distdir && tar xf - )

echo
echo cloning x10-applications git repo
cd $workdir
git clone --depth 1 https://github.com/x10-lang/x10-applications.git $repodir_apps

echo
echo extracting ProxyApps from app repo
cd $repodir_apps
git archive --format=tar $X10_TAG lulesh2 | (cd $distdir && tar xf - )

tarfile="x10-benchmarks-$X10_VERSION"".tar.bz2"
echo "The benchmarks are now exported to the directory $workdir"

cd $workdir
eval tar -cjf "$tarfile" x10-benchmarks-$X10_VERSION

