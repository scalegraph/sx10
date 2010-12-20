#!/bin/bash

# Dave Grove

hosts="condor.watson.ibm.com triloka3.watson.ibm.com nashira.watson.ibm.com rlsedx10.watson.ibm.com"

# TODO: we should get svn info by parsing svn info URL and extracting revision from there.
while [ $# != 0 ]; do
  case $1 in
    -version)
        version=$2
	shift
    ;;

    -tag)
        tag=$2
	shift
    ;;

    -hosts)
        hosts=$2
	shift
    ;;

   esac
   shift
done

if [[ -z "$version" ]]; then
    echo "usage: $0 must give version name -version <version name>"
    exit 1
fi

if [[ -z "$tag" ]]; then
    echo "usage: $0 must give SF tag name -tag <tag>"
    exit 1
fi


ssh orquesta.watson.ibm.com "mkdir -p /var/www/localhost/htdocs/x10dt/x10-rc-builds/$version"

for host in $hosts
do
    echo "Launching buildRelease.sh on $host"
    scp buildRelease.sh $host:/tmp 
    ssh $host "bash -l -c 'cd /tmp; ./buildRelease.sh -dir /tmp/x10-rc-$USER -version $version -tag $tag'"
    ssh $host "(cd /tmp; rm ./buildRelease.sh)"
    echo "transfering file from $host to localhost"
    scp "$host:/tmp/x10-rc-$USER/x10-$version/x10.dist/x10-$version*.tgz" .
    echo "transfering from localhost to orquesta"
    scp x10-$version*.tgz orquesta.watson.ibm.com:/var/www/localhost/htdocs/x10dt/x10-rc-builds/$version
    rm x10-$version*.tgz
    #ssh $host rm -rf /tmp/x10-rc-$USER 
done

