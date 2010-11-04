/*
 * This file is here to allow for a stand-alone build of the launcher, which can be used to launch anything.
 */

#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include "Launcher.h"

int main (int argc, char ** argv)
{
	if (argc < 2 || strcmp(argv[1], "-help")==0)
	{
		fprintf(stderr, "Usage: X10Launcher [-np NUM_OF_PLACES] [-hostlist HOST1,HOST2,ETC] [-hostfile FILENAME] COMMAND_TO_LAUNCH [ARG1 ARG2 ...]\n");
		fprintf(stderr, "    You can set the environment variable \"X10LAUNCHER_NPROCS\" or use -np to specify the number of places to run\n");
		fprintf(stderr, "    Set the environment variable \"X10LAUNCHER_HOSTFILE\" or use -hostfile to specify a file containing what machines to run on\n");
		fprintf(stderr, "    Set the environment variable \"X10LAUNCHER_HOSTLIST\" or use -hostlist if you don't want to use a file\n");
		fprintf(stderr, "    Specify the executable to run after the above aguments.  Any arguments after this will be passed to that program.\n");

		return 1;
	}

	int commandPosition = 1;
	for (int i = 1; i < argc; i+=2)
	{
		if (strcmp(argv[i], "-np") == 0)
			setenv(X10LAUNCHER_NPROCS, argv[i+1], 1);
		else if (strcmp(argv[i], "-hostlist") == 0)
			setenv(X10LAUNCHER_HOSTLIST, argv[i+1], 1);
		else if (strcmp(argv[i], "-hostfile") == 0)
			setenv(X10LAUNCHER_HOSTFILE, argv[i+1], 1);
		else
		{
			commandPosition = i;
			break;
		}
	}

	// run the launcher code.  This will cause more launchers to be spawned.
	// this method will not return for launcher processes.
	Launcher::Setup(argc, argv);

	// if we're here, then this is a runtime process.  We simply want to exec whatever was passed at the command line after our executable.
	if (execvp(argv[commandPosition], &argv[commandPosition]))
		return 1; // can't get here, if the exec succeeded
}
