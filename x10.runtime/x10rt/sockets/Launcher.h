/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2010.
 *
 *  This file was written by Ben Herta for IBM: bherta@us.ibm.com
 */
#include <stdlib.h>
#include <stdarg.h>
#include <limits.h>
#include <sys/select.h>
#include <stdint.h>

/* ************************************************************************ */
/*            some explicit declarations for strict ANSI mode               */
/* ************************************************************************ */
#ifdef __CYGWIN__
extern "C" int vsnprintf(char *, size_t, const char *, va_list); 
extern "C" char *realpath (const char *, char *);
extern "C" int setenv (const char *, const char *, int);
extern "C" int unsetenv (const char *);
extern "C" int fileno (FILE *__stream);
extern "C" int strcasecmp(const char *, const char *);
#endif

#ifndef __sock_launcher_h__
#define __sock_launcher_h__

/* ************************************************************************ */
/*               public ProcManager interface                               */
/* ************************************************************************ */

// Environment variable names
#define X10_PLACE "X10_PLACE" // a number for the "place" of this process.  Set by the launcher.
#define X10_NPLACES "X10_NPLACES" // the number of places in this process
#define X10_HOSTFILE "X10_HOSTFILE" // full path name of a file containing a list of hostnames
#define X10_HOSTLIST "X10_HOSTLIST" // an alternative to HOSTFILE above.  This is a comma-separated list of hostnames
#define X10LAUNCHER_SSH "X10LAUNCHER_SSH" // the ssh command.  This doesn't normally need to be set.
#define X10LAUNCHER_PARENT "X10LAUNCHER_PARENT" // the hostname:port of the parent launcher.  This is set by the launcher.
#define X10LAUNCHER_RUNTIME "X10LAUNCHER_RUNTIME" // this is a flag to run as a runtime or a launcher.  Set by the launcher.
#define X10LAUNCHER_CWD "X10LAUNCHER_CWD" // the working directory of the program
#define X10LAUNCHER_DEBUG "X10LAUNCHER_DEBUG" // This flag causes the runtime to launch under gdb in a new xterm
#define X10RT_NOYIELD "X10RT_NOYIELD"

// Enable/disable debug information
// Enabled for MULTIVM
#define DEBUG 1

enum CTRL_MSG_TYPE {HELLO, GOODBYE, PORT_REQUEST, PORT_RESPONSE};
struct ctrl_msg
{
	CTRL_MSG_TYPE type;
	uint32_t to;
	uint32_t from;
	int datalen;
	// followed by the data
};

/* ************************************************************************ */
/*                ProcManager class definition                              */
/* ************************************************************************ */

class Launcher
{
	public:
		static int _parentLauncherControlLink; /* parent control connection */
		static void Setup(int argc, char ** argv);
		static void cb_sighandler_cld(int signo);
		static int lookupPlace(uint32_t myPlace, uint32_t destPlace, char* response, int responseLen);
		static int setPort(uint32_t place, char* port);

	protected:
		/* SockProcManager_Init.cc */
		Launcher();
		void initialize(int argc, char ** argv);
		void readHostFile(void);
		void startSSHclient(uint32_t childrank, char* masterPort, char* remotehost);

		/* SockProcManager.cc */
		void startChildren(void);
		void handleRequestsLoop(bool onlyCheckForNewConnections);
		int makeFDSets(fd_set *, fd_set *, fd_set *);
		void connectToParentLauncher(void); /* connect to parent */
		void handleNewChildConnection(void); /* new child */
		bool handleDeadChild(uint32_t childno, int type); /* child disconnected */
		bool handleDeadParent(void); /* parent disconnected */
		bool handleChildCout(int childno); /* console from child */
		bool handleChildCerror(int childno); /* stderr from child */
		int handleControlMessage(int fd); /* incoming ctrl msg */
		int forwardMessage(struct ctrl_msg* message, char* data); /* move data around */
		static void DIE(const char * message, ...);
		void * operator new(size_t, void * addr){return addr;}

	private:
		static Launcher * _singleton;

		/* startup parameters */
		char ** _argv; /* argv copied from init */
		int _argc; /* argc copied from init */
		char _realpath[PATH_MAX]; /* real path of executable */
		char _ssh_command[64]; /* the SSH command. */
		char _hostfname[512]; /* host file name */
		uint32_t _nplaces; /* number of processors in job */
		uint32_t _myproc; /* my processor ID */
		int _returncode; // the return code from the local runtime

		/* parent child structure */
		char ** _hostlist; /* child host list */
		char* _runtimePort; /* the host:port number of the associated x10 runtime's listen port */
		uint32_t _firstchildproc; /* the ID of the first child launcher */
		uint32_t _numchildren; /* number of launcher children in this node */
		int * _pidlst; /* list of all spawned pids */
		int _listenSocket; /* listener - for children */
		int * _childControlLinks; /* children's control connections */
		int * _childCoutLinks; /* children's cout connections */
		int * _childCerrorLinks; /* children's cerror connections */
};
#endif /* #ifdef */

