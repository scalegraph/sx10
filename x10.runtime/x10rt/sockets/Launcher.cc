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

#ifdef __CYGWIN__
#undef __STRICT_ANSI__ // Strict ANSI mode is too strict in Cygwin
#endif

#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <signal.h>
#include <stdio.h>
#include <assert.h>
#include <fcntl.h>
#include <stdarg.h>
#include <alloca.h>
#include <arpa/inet.h>
#include <sched.h>
#include <errno.h>

#ifdef __MACH__
#include <crt_externs.h>
#endif

#include "Launcher.h"
#include "TCP.h"

/* *********************************************************************** */
/*     utility methods, called outside of the launcher					   */
/* *********************************************************************** */
const char* CTRL_MSG_TYPE_STRINGS[] = {"HELLO", "GOODBYE", "PORT_REQUEST", "PORT_RESPONSE"};

int Launcher::setPort(uint32_t place, char* port)
{
	if (port == NULL)
		return -1;

	if (_singleton == NULL)
	{
		// send this out to our local launcher
		if (_parentLauncherControlLink <= 0)
		{
			if (getenv(X10LAUNCHER_PARENT) != NULL)
			{
				#ifdef DEBUG
					fprintf(stderr, "Runtime %u connecting to launcher at \"%s\"\n", place, getenv(X10LAUNCHER_PARENT));
				#endif

				_parentLauncherControlLink = TCP::connect((const char *) getenv(X10LAUNCHER_PARENT), 10, true);
			}
			if (_parentLauncherControlLink <= 0)
				return -1; // connection failed for some reason
		}

		struct ctrl_msg m;
		m.type = HELLO;
		m.from = place;
		m.to = place;
		m.datalen = strlen(port);
		int r = TCP::write(_parentLauncherControlLink, &m, sizeof(struct ctrl_msg));
		if (r <= 0)
			return -1;
		TCP::write(_parentLauncherControlLink, port, m.datalen);
		#ifdef DEBUG
			fprintf(stderr, "Runtime %u: sent port data \"%s\" to launcher\n", place, port);
		#endif
	}
	else
	{
		_singleton->_runtimePort = (char*)malloc(strlen(port)+1);
		strcpy(_singleton->_runtimePort, port);
	}
	return 1;
}

int Launcher::lookupPlace(uint32_t myPlace, uint32_t destPlace, char* response, int responseLen)
{
	// TODO - this isn't reentrant.  Need to fix that.
	struct ctrl_msg m;
	m.type = PORT_REQUEST;
	m.from = myPlace;
	m.to = destPlace;
	m.datalen = 0;

	if (_singleton == NULL)
	{
		// inside this if, we're running within the runtime process, and talking to a launcher
		// send this out to our local launcher
		// parent link was established earlier, at HELLO
		if (_parentLauncherControlLink <= 0)
			DIE("Runtime %u: bad link to launcher", myPlace);

		#ifdef DEBUG
			fprintf(stderr, "Runtime %u: sending %s:%u to launcher %u\n", myPlace, CTRL_MSG_TYPE_STRINGS[m.type], destPlace, myPlace);
		#endif
		int ret = TCP::write(_parentLauncherControlLink, &m, sizeof(struct ctrl_msg));
		if (ret <= 0)
			DIE("Runtime %u: Unable to write port request", myPlace);

		// this should block, until the data is available
		#ifdef DEBUG
			fprintf(stderr, "Runtime %u: waiting for %s:%u message from launcher %u\n", myPlace, CTRL_MSG_TYPE_STRINGS[PORT_RESPONSE], destPlace, myPlace);
		#endif
		ret = TCP::read(_parentLauncherControlLink, &m, sizeof(struct ctrl_msg));
		if (ret <= 0 || m.type != PORT_RESPONSE || m.datalen <= 0)
			DIE("Runtime %u: Invalid port request reply (len=%d, type=%s, datalen=%d)", myPlace, ret, CTRL_MSG_TYPE_STRINGS[m.type], m.datalen);

		if (responseLen < m.datalen+1)
			DIE("Runtime %u: The buffer is too small for the place lookup (data=%d bytes, buffer=%d bytes)", myPlace, m.datalen, responseLen);

		#ifdef DEBUG
			fprintf(stderr, "Runtime %d: waiting for %s data from launcher %u\n", myPlace, CTRL_MSG_TYPE_STRINGS[PORT_RESPONSE], myPlace);
		#endif
		ret = TCP::read(_parentLauncherControlLink, response, m.datalen);
		if (ret <= 0)
			DIE("Runtime %u: Unable to read port response data", myPlace);
		response[m.datalen] = '\0';

		#ifdef DEBUG
			fprintf(stderr, "Runtime %u: determined place %u is at \"%s\"\n", myPlace, destPlace, response);
		#endif

		return m.datalen;
	}

	// if we're here, then this is executing in the launcher process, not in a runtime.
	// we can't handle this request.  Forward it to some launcher that can.
	return _singleton->forwardMessage(&m, NULL);
}

/* *********************************************************************** */
/*     start all children. If there are no children, nothing is done       */
/* *********************************************************************** */

void Launcher::startChildren()
{
	#ifdef DEBUG
		fprintf(stderr, "Launcher %u: starting %d child%s\n", _myproc, _myproc==0xFFFFFFFF?1:_numchildren, (_numchildren==1 || _myproc==0xFFFFFFFF)?"":"ren");
	#endif

	/* -------------------------------------------- */
	/*    create and initialize listener FD         */
	/* -------------------------------------------- */

	unsigned listenPort = 0;
	_listenSocket = TCP::listen(&listenPort, 10);
	if (_listenSocket < 0)
		DIE("Launcher %u: cannot create listener port", _myproc);
	#ifdef DEBUG
		fprintf(stderr, "Launcher %u: opened listen socket at port %d\n", _myproc, listenPort);
	#endif

	// allocate space to hold all the links to the child launchers and the runtime
	// Child launchers (if any) are first in the list, followed by the local runtime in the last slot.
	_pidlst = (int *) malloc(sizeof(int) * (_numchildren+1));
	_childCoutLinks = (int *) malloc(sizeof(int) * (_numchildren+1));
	_childCerrorLinks = (int *) malloc(sizeof(int) * (_numchildren+1));
	_childControlLinks = (int *) malloc(sizeof(int) * (_numchildren+1));

	if (!getenv(X10LAUNCHER_CWD))
	{
		char dir[1024];
		getcwd(dir, sizeof(dir));
		setenv(X10LAUNCHER_CWD, dir, 1);
	}

	if (!_pidlst || !_childControlLinks || !_childCoutLinks || !_childCerrorLinks)
		DIE("%u: failed in alloca()", _myproc);

	for (uint32_t id=0; id <= _numchildren; id++)
	{
		int pid, outpipe[2], errpipe[2];
		if (pipe(outpipe) < 0) DIE("Launcher %u: failed in outpipe()", _myproc);
		if (pipe(errpipe) < 0) DIE("Launcher %u: failed in errpipe()", _myproc);
		if ((pid = fork()) < 0) DIE("Launcher %u: failed in fork()", _myproc);
		else if (pid > 0)
		{
			// parent process
			_pidlst[id] = pid;
			_childControlLinks[id] = -1; // these get set later, when the child connects
			_childCoutLinks[id] = outpipe[0];
			close(outpipe[1]);
			_childCerrorLinks[id] = errpipe[0];
			close(errpipe[1]);
			fcntl(_childCoutLinks[id], F_SETFL, O_NONBLOCK | O_NDELAY);
			fcntl(_childCerrorLinks[id], F_SETFL, O_NONBLOCK | O_NDELAY);
		}
		else
		{
			// child process
			// redirect stderr and stdout over to our pipes
			int z1 = dup2(outpipe[1], fileno(stdout));
			close(outpipe[0]);
			int z2 = dup2(errpipe[1], fileno(stderr));
			close(errpipe[0]);
			if (z1 != 1 || z2 != 2)
				DIE("Launcher %d: DUP failed", _myproc);

			char masterPort[1024];
			TCP::getname(_listenSocket, masterPort, sizeof(masterPort));

			if (id == _numchildren && _myproc != 0xFFFFFFFF)
			{ // start up the local x10 runtime
				unsetenv(X10_HOSTFILE);
				unsetenv(X10_HOSTLIST);
				unsetenv(X10LAUNCHER_SSH);
				setenv(X10LAUNCHER_PARENT, masterPort, 1);
				setenv(X10LAUNCHER_RUNTIME, "1", 1);
				chdir(getenv(X10LAUNCHER_CWD));

				// check to see if we want to launch this in a debugger
				char* which = getenv(X10LAUNCHER_DEBUG);
				if (which != NULL)
				{
					char placenum[64];
					strcpy(placenum, which);
					char * colon = strchr(placenum, ':');
					if (colon != NULL)
						colon[0] = '\0';

					// see if the launcher argument applies to this place
					char* p;
					errno = 0;
					if (strcasecmp("all", placenum) == 0 || ((_myproc == (uint32_t)strtoul(placenum, &p, 10)) && !(errno != 0 || *p != 0 || p == placenum)))
					{
						// launch this runtime in a debugger
						char** newargv;
						if (colon == NULL)
						{
							#ifdef DEBUG
								fprintf(stderr, "Runtime %u forked with gdb in an xterm.  Running exec.\n", _myproc);
							#endif
							newargv = (char**)alloca(8*sizeof(char*));
							if (newargv == NULL)
								DIE("Allocating space for exec-ing gdb runtime %d\n", _myproc);
							char* title = (char*)alloca(32);
							sprintf(title, "Place %u debug", _myproc);
							newargv[0] = (char*)"xterm";
							newargv[1] = (char*)"-T";
							newargv[2] = title;
							newargv[3] = (char*)"-e";
							newargv[4] = (char*)"gdb";
							newargv[5] = _argv[0];
							newargv[6] = (char*)NULL;
						}
						else
						{
							colon[0] = ':';
							#ifdef DEBUG
								fprintf(stderr, "Runtime %u forked with a remote gdbserver at port %s.  Running exec.\n", _myproc, &colon[1]);
							#endif
							int numArgs = 0;
							while (_argv[numArgs] != NULL)
								numArgs++;
							newargv = (char**)alloca((numArgs+3)*sizeof(char*));
							if (newargv == NULL)
								DIE("Allocating space for exec-ing gdb runtime %d\n", _myproc);
							newargv[0] = (char*)"gdbserver";
							newargv[1] = colon;
							for (int i=0; i<numArgs; i++)
								newargv[i+2] = _argv[i];
							newargv[numArgs+2] = (char*)NULL;
						}
						if (execvp(newargv[0], newargv))
							// can't get here, if the exec succeeded
							DIE("Launcher %u: runtime exec with gdb failed", _myproc);
					}
				}

				#ifdef DEBUG
					fprintf(stderr, "Runtime %u forked.  Running exec.\n", _myproc);
				#endif
				if (execvp(_argv[0], _argv))
					// can't get here, if the exec succeeded
					DIE("Launcher %u: runtime exec failed", _myproc);
			}
			else
			{
				/* SSH children: call startSSH client */
				if (_hostlist && strncmp(_hostlist[id], "localhost", 9) != 0)
					startSSHclient(_firstchildproc+id, masterPort, _hostlist[id]);
				else
				{ // if the child is on the localhost, just exec it.  No need for ssh.
					setenv(X10LAUNCHER_PARENT, masterPort, 1);
					char idString[24];
					sprintf(idString, "%d", _firstchildproc+id);
					setenv(X10_PLACE, idString, 1);
					#ifdef DEBUG
						fprintf(stderr, "Launcher %u forked launcher %s on localhost.  Running exec.\n", _myproc, idString);
					#endif
					if (execvp(_argv[0], _argv))
						DIE("Launcher %u: local child launcher exec failed", _myproc);
				}
			}
		}
	}

	/* process manager invokes main loop */
	handleRequestsLoop(false);
}


/* *********************************************************************** */
/* this is the infinite loop that the process manager is executing.        */
/* *********************************************************************** */

void Launcher::handleRequestsLoop(bool onlyCheckForNewConnections)
{
	#ifdef DEBUG
	if (!onlyCheckForNewConnections)
		fprintf(stderr, "Launcher %u: main loop start\n", _myproc);
	#endif
	bool running = true;

	while (running)
	{
		struct timeval timeout = { 0, 100000 };
		fd_set infds, efds;
		int fd_max = makeFDSets(&infds, NULL, &efds);
		if (select(fd_max+1, &infds, NULL, &efds, &timeout) < 0)
			break; // select error.  This can happen when we're in the middle of shutdown

		if (_dieAt > 0)
		{
			time_t now = time(NULL);
			if (now >= _dieAt)
				break;
		}

		/* listener socket (new connections) */
		if (_listenSocket >= 0)
		{
			if (FD_ISSET(_listenSocket, &efds))
			{
				#ifdef DEBUG
					fprintf(stderr, "Launcher %u: listensocket ERROR detected\n", _myproc);
				#endif
				close(_listenSocket);
				_listenSocket = -1;
			}
			else if (FD_ISSET(_listenSocket, &infds))
				handleNewChildConnection();
		}
		if (onlyCheckForNewConnections)
			return;
		/* parent control socket */
		if (_parentLauncherControlLink >= 0)
		{
			if (FD_ISSET(_parentLauncherControlLink, &efds))
				running = handleDeadParent();
			else if (FD_ISSET(_parentLauncherControlLink, &infds))
				if (handleControlMessage(_parentLauncherControlLink) < 0)
					running = handleDeadParent();
		}
		/* runtime and children control, stdout and stderr */
		for (uint32_t i = 0; i <= _numchildren; i++)
		{
			if (_childControlLinks[i] >= 0)
			{
				if (FD_ISSET(_childControlLinks[i], &efds))
					running = handleDeadChild(i, 0);
				else if (FD_ISSET(_childControlLinks[i], &infds))
					if (handleControlMessage(_childControlLinks[i]) < 0)
						running = handleDeadChild(i, 0);
			}

			if (_childCoutLinks[i] >= 0)
			{
				if (FD_ISSET(_childCoutLinks[i], &efds))
					running = handleDeadChild(i, 1);
				else if (FD_ISSET(_childCoutLinks[i], &infds))
					running = handleChildCout(i);
			}

			if (_childCerrorLinks[i] >= 0)
			{
				if (FD_ISSET(_childCerrorLinks[i], &efds))
					running = handleDeadChild(i, 2);
				else if (FD_ISSET(_childCerrorLinks[i], &infds))
					running = handleChildCerror(i);
			}
		}
	}

	/* --------------------------------------------- */
	/* end of main loop. kill & wait every process   */
	/* --------------------------------------------- */

	signal(SIGCHLD, SIG_DFL); // disable the SIGCHLD handler

	// save the return code for place 0.
	int exitcode = _returncode; // exitcode is here to cover up any issues with using a static variable for the exit code, which may happen on Windows (and AIX?).
	if ((_myproc==0 || _myproc==0xFFFFFFFF) && _pidlst[_numchildren] != -1)
	{
	    int status;
 		if (waitpid(_pidlst[_numchildren], &status, 0) == _pidlst[_numchildren])
		{
 			exitcode = WEXITSTATUS(status);
			if (exitcode != 0)
				fprintf(stderr, "Launcher %d: Non-zero return code from local runtime (pid=%d), status=%d (previous stored status=%d)\n", _myproc, _pidlst[_numchildren], exitcode, _returncode);
			_pidlst[_numchildren] = -1;
		}
	}

	// shut down any connections if they still exist
	handleDeadParent();

	// wait for and clean up any leftover launchers (prevent zombies)
	for (uint32_t i = 0; i < _numchildren; i++)
	{
		if (_pidlst[i] != -1)
		{
			waitpid(_pidlst[i], NULL, 0);
			_pidlst[i] = -1;
		}
	}

	// take out the local runtime, if it's still kicking
	if (_pidlst[_numchildren] != -1)
	{
		kill(_pidlst[_numchildren], SIGKILL);
		waitpid(_pidlst[_numchildren], NULL, 0);
		_pidlst[_numchildren] = -1;
	}

	// free up allocated memory (not really needed, since we're about to exit)
	free(_hostlist);
	free(_runtimePort);
	#ifdef DEBUG
		fprintf(stderr, "Launcher %u: cleanup complete.  Goodbye!\n", _myproc);
	#endif
	exit(exitcode);
}

/* *********************************************************************** */
/*     prepare FD sets to listen to in main loop                           */
/* *********************************************************************** */

#define MAX(a,b) (((a)<(b))?(b):(a))
int Launcher::makeFDSets(fd_set * infds, fd_set * outfds, fd_set * efds)
{
	int fd_max = 0;
	FD_ZERO(infds);
	FD_ZERO(efds);

	/* listener socket */
	if (_listenSocket >= 0)
	{
		FD_SET (_listenSocket, infds);
		FD_SET (_listenSocket, efds);
		fd_max = MAX(fd_max, _listenSocket);
	}

	/* control socket to parent */
	if (_parentLauncherControlLink >= 0)
	{
		FD_SET (_parentLauncherControlLink, infds);
		FD_SET (_parentLauncherControlLink, efds);
		fd_max = MAX(fd_max, _parentLauncherControlLink);
	}

	/* control sockets to children */
	for (uint32_t i = 0; i <= _numchildren; i++)
	{
		if (_childControlLinks[i] >= 0)
		{
			FD_SET(_childControlLinks[i], infds);
			FD_SET(_childControlLinks[i], efds);
			fd_max = MAX(fd_max, _childControlLinks[i]);
		}
		if (_childCoutLinks[i] >= 0)
		{
			FD_SET(_childCoutLinks[i], infds);
			FD_SET(_childCoutLinks[i], efds);
			fd_max = MAX(fd_max, _childCoutLinks[i]);
		}
		if (_childCerrorLinks[i] >= 0)
		{
			FD_SET(_childCerrorLinks[i], infds);
			FD_SET(_childCerrorLinks[i], efds);
			fd_max = MAX(fd_max, _childCerrorLinks[i]);
		}
	}

	return fd_max;
}

/* *********************************************************************** */
/* open control connection to parent (if any) and announce ourselves       */
/* *********************************************************************** */

void Launcher::connectToParentLauncher(void)
{
	if (_myproc == 0)
		return; // don't connect - nothing useful for us at the primordial launcher.

	/* case 1: we have inherited the listener FD */
	if (_listenSocket >= 0)
	{
		char masterport[1024];
		TCP::getname(_listenSocket, masterport, sizeof(masterport));
		#ifdef DEBUG
			fprintf(stderr, "Launcher %u: connecting to parent via inherited port: %s\n", _myproc, masterport);
		#endif
		_parentLauncherControlLink = TCP::connect(masterport, 10, true);
	}

	/* case 2: the SOCK_PARENT env. var is set */
	else if (getenv(X10LAUNCHER_PARENT) != NULL)
	{
		#ifdef DEBUG
			fprintf(stderr, "Launcher %u: connecting to parent via: %s\n", _myproc, getenv(X10LAUNCHER_PARENT));
		#endif
		_parentLauncherControlLink = TCP::connect((const char *) getenv(X10LAUNCHER_PARENT), 10, true);
	}

	/* case 3: launcher=-1 has no parent. We don't connect */
	else
	{
		#ifdef DEBUG
			fprintf(stderr, "Launcher %u: has no parent.\n", _myproc);
		#endif
		_parentLauncherControlLink = -1;
		return;
	}

	/* we are hopefully connected. So we announce ourselves */
	if (_parentLauncherControlLink < 0)
		DIE("Launcher %u: failed to connect to parent", _myproc);

	struct ctrl_msg helloMsg;
	helloMsg.type = HELLO;
	helloMsg.from = _myproc;
	helloMsg.to = -1;
	helloMsg.datalen = 0;
	TCP::write(_parentLauncherControlLink, &helloMsg, sizeof(struct ctrl_msg));
}

/* *********************************************************************** */
/*   a new child is announcing itself on the listener socket               */
/* *********************************************************************** */

void Launcher::handleNewChildConnection(void)
{
	#ifdef DEBUG
		fprintf(stderr, "Launcher %u: new connection detected\n", _myproc);
	#endif
	/* accept the new connection and read off the rank */
	int fd = TCP::accept(_listenSocket, true);
	if (fd < 0)
	{
		close(_listenSocket);
		_listenSocket = -1;
	}

	struct ctrl_msg m;
	int size = TCP::read(fd, &m, sizeof(struct ctrl_msg));

	/* find rank in child list */
	if (size == sizeof(struct ctrl_msg) && m.type == HELLO)
	{
		if (m.from == _myproc)
		{
			_childControlLinks[_numchildren] = fd;
			if (m.datalen > 0)
			{
				_runtimePort = (char*)malloc(m.datalen+1);
				_runtimePort[m.datalen] = '\0';
				size = TCP::read(_childControlLinks[_numchildren], _runtimePort, m.datalen);
				if (size < m.datalen)
					DIE("Launcher %u: could not read local runtime data", _myproc);
			}
			#ifdef DEBUG
				fprintf(stderr, "Launcher %u: new ctrl conn %d from local runtime, with runtime port=\"%s\"\n", _myproc, fd, _runtimePort);
			#endif
			return;
		}

		for (uint32_t i = 0; i < _numchildren; i++) // the runtime is not in this loop
		{
			if (m.from == _firstchildproc + i)
			{
				_childControlLinks[i] = fd;
				#ifdef DEBUG
					fprintf(stderr, "Launcher %u: new ctrl conn from child=%d\n", _myproc, m.from);
				#endif
				if (m.datalen > 0)
				{
					char* data = (char*)alloca(m.datalen+1);
					data[m.datalen] = '\0';
					TCP::read(fd, data, m.datalen);
					DIE("Launcher %u: Control message from child launcher came in with datalen of \"%s\"\n", _myproc, data);
				}
				return;
			}
		}
	}

	// something bad came in.  Maybe some other program trying to connect to our port.  Disconnect it.
	close(fd);
	#ifdef DEBUG
		fprintf(stderr, "Launcher %u: Invalid %d byte message came into listen socket.  Closing it down.\n", _myproc, size);
	#endif
}

/* *********************************************************************** */
/*    A child connection has closed                                        */
/* *********************************************************************** */
// returns false if everything should die
bool Launcher::handleDeadChild(uint32_t childNo, int type)
{
	// this is usually called multiple times, for stdin, stderr, etc
	if (type == 0 && _childControlLinks[childNo] >= 0)
	{
		close(_childControlLinks[childNo]);
		_childControlLinks[childNo] = -1;

		#ifdef DEBUG
		if (childNo == _numchildren && _myproc != 0xFFFFFFFF)
			fprintf(stderr, "Launcher %u: local runtime control disconnected\n", _myproc);
		else
			fprintf(stderr, "Launcher %u: child=%d control disconnected\n", _myproc, _firstchildproc+childNo);
		#endif
	}
	else if (type == 1 && _childCoutLinks[childNo] >= 0)
	{
		close(_childCoutLinks[childNo]);
		_childCoutLinks[childNo] = -1;

		#ifdef DEBUG
		if (childNo == _numchildren && _myproc != 0xFFFFFFFF)
			fprintf(stderr, "Launcher %u: local runtime cout disconnected\n", _myproc);
		else
			fprintf(stderr, "Launcher %u: child=%d cout disconnected\n", _myproc, _firstchildproc+childNo);
		#endif
	}
	else if (type == 2 && _childCerrorLinks[childNo] >= 0)
	{
		close(_childCerrorLinks[childNo]);
		_childCerrorLinks[childNo] = -1;

		#ifdef DEBUG
		if (childNo == _numchildren && _myproc != 0xFFFFFFFF)
			fprintf(stderr, "Launcher %u: local runtime cerror disconnected\n", _myproc);
		else
			fprintf(stderr, "Launcher %u: child=%d cerror disconnected\n", _myproc, _firstchildproc+childNo);
		#endif
	}

	// check to see if *all* child links are down
	for (uint32_t i=0; i<=_numchildren; i++)
	{
		if (_childControlLinks[i] >= 0) return true;
		if (_childCoutLinks[i] >= 0) return true;
		if (_childCerrorLinks[i] >= 0) return true;
	}
	return false;
}

/* *********************************************************************** */
/*    Parent connection has closed                                         */
/* *********************************************************************** */

bool Launcher::handleDeadParent()
{
	#ifdef DEBUG
		fprintf(stderr, "Launcher %u: parent disconnected\n", _myproc);
	#endif
	if (_parentLauncherControlLink != -1)
		close(_parentLauncherControlLink);
	_parentLauncherControlLink = -1;

	// take down all the children
	for (uint32_t i=0; i<=_numchildren; i++)
		for (int j=0; j<=2; j++)
			handleDeadChild(i, j);
	return false;
}

/* *********************************************************************** */
/* *********************************************************************** */

int Launcher::handleControlMessage(int fd)
{
	struct ctrl_msg m;
	assert (fd >= 0);
	int ret = TCP::read(fd, &m, sizeof(struct ctrl_msg));
	if (ret < (int)sizeof(struct ctrl_msg))
		return -1;

	#ifdef DEBUG
		fprintf(stderr, "Launcher %u: Incoming %d byte %s message from %u for %u, with datalen=%d\n",
				_myproc, ret, CTRL_MSG_TYPE_STRINGS[m.type], m.from, m.to, m.datalen);
	#endif

	char* data = NULL;
	if (m.datalen > 0)
	{
		data = (char*)alloca(m.datalen);
		if (data == NULL)
			DIE("Launcher %u: cannot allocate %d bytes for a control message", _myproc, m.datalen);
	}

	if (TCP::read(fd, data, m.datalen) < 0)
		DIE("Launcher %u: cannot read %d bytes of control message data", _myproc, m.datalen);

	if (m.to == _myproc)
	{
		switch(m.type)
		{
			case PORT_REQUEST:
			{
				while (_runtimePort == NULL)
				{
					sched_yield();
					handleRequestsLoop(true);
				}
				m.to = m.from;
				m.from = _myproc;
				m.type = PORT_RESPONSE;
				m.datalen = strlen(_runtimePort);
				#ifdef DEBUG
					fprintf(stderr, "Launcher %u preparing %s message for %u.\n", _myproc, CTRL_MSG_TYPE_STRINGS[PORT_RESPONSE], m.to);
				#endif
				TCP::write(fd, &m, sizeof(struct ctrl_msg));
				TCP::write(fd, _runtimePort, m.datalen);
			}
			break;
			case PORT_RESPONSE:
			{
				// forward to connected runtime
				#ifdef DEBUG
					fprintf(stderr, "Launcher %u forwarding %s message to local runtime.\n", _myproc, CTRL_MSG_TYPE_STRINGS[PORT_RESPONSE]);
				#endif
				TCP::write(_childControlLinks[_numchildren], &m, sizeof(struct ctrl_msg));
				TCP::write(_childControlLinks[_numchildren], data, m.datalen);
			}
			break;
			case HELLO:
				DIE("Unexpected HELLO message");
			break;
			case GOODBYE:
				DIE("Unexpected GOODBYE message");
			break;
		}
	}
	else
	{
		ret = forwardMessage(&m, data);
		if (ret < 0 && m.type == PORT_REQUEST)
		{
			// if we're here, then we were unable to forward the PORT_REQUEST message.  Send an error message back as the response.
			#ifdef DEBUG
				fprintf(stderr, "Launcher %u failed to forward a %s message to place %u.  Sending an error response.\n", _myproc, CTRL_MSG_TYPE_STRINGS[PORT_RESPONSE], m.to);
			#endif

			char* dead = (char*)alloca(64);
			sprintf(dead, "LAUNCHER_%u_IS_NOT_RUNNING", m.to);
			m.to = m.from;
			m.from = _myproc;
			m.type = PORT_RESPONSE;
			m.datalen = strlen(dead);

			TCP::write(fd, &m, sizeof(struct ctrl_msg));
			TCP::write(fd, dead, m.datalen);
		}
	}
	return ret;
}


bool Launcher::handleChildCout(int childNo)
{
	#ifdef DEBUG
		//fprintf(stderr, "Launcher %u: cout from %s detected\n", _myproc, childNo==_numchildren?"runtime":"child launcher");
	#endif
	char buf[1024];
	int n = read(_childCoutLinks[childNo], buf, sizeof(buf));
	if (n <= 0)
		return handleDeadChild(childNo, 1);
	else
	{
		write(fileno(stdout), buf, n);
		fflush(stdout);
	}
	return true;
}

bool Launcher::handleChildCerror(int childNo)
{
	#ifdef DEBUG
		//fprintf(stderr, "Launcher %u: cerror from %s detected\n", _myproc, childNo==_numchildren?"runtime":"child launcher");
	#endif
	char buf[1024];
	int n = read(_childCerrorLinks[childNo], buf, sizeof(buf));
	if (n <= 0)
		return handleDeadChild(childNo, 2);
	else
	{
		write(fileno(stderr), buf, n);
		fflush(stderr);
	}
	return true;
}

int Launcher::forwardMessage(struct ctrl_msg* message, char* data)
{
	// this request needs to be forwarded either to the parent launcher, or a child launcher
	// figure out where to send it, by determining if we are on the chain between place 0 and the dest
	uint32_t child=message->to, parent;

	int destID = -1;
	if (child > _singleton->_myproc)
	{
		do
		{
			parent = (child-1)/2;
			if (parent == _singleton->_myproc)
			{
				if (child == _singleton->_firstchildproc)
					destID = 0; //_childControlLinks[0];
				else
					destID = 1; //_childControlLinks[1];
				#ifdef DEBUG
					fprintf(stderr, "Launcher %u: forwarding %s message to child launcher %u.\n", _myproc, CTRL_MSG_TYPE_STRINGS[message->type], child);
				#endif
				break;
			}
			else
				child = parent;
		}
		while (parent > _singleton->_myproc);
	}

	#ifdef DEBUG
	if (destID == -1)
		fprintf(stderr, "Launcher %u: forwarding %s message to parent launcher.\n", _myproc, CTRL_MSG_TYPE_STRINGS[message->type]);
	#endif

	int destFD = -1;
	do
	{
		if (destID == -1) destFD = _parentLauncherControlLink;
		else if (destID == 0) destFD = _childControlLinks[0];
		else if (destID == 1) destFD = _childControlLinks[1];

		// verify that the link is valid.  It may not be, if we're just starting up
		if (destFD == -1)
		{
			if (destID >= 0 && _pidlst[destID] == -1)
				return -1; // this request is for a launcher that has DIED

			sched_yield();
			handleRequestsLoop(true);
		}
	}
	while (destFD == -1);


	int ret = TCP::write(destFD, message, sizeof(struct ctrl_msg));
	if (ret < (int)sizeof(struct ctrl_msg))
		DIE("Launcher %u: Failed to forward message to %s", _myproc,
			destFD==_parentLauncherControlLink?"parent launcher":
			(destFD==_childControlLinks[0]?"child launcher 0":"child launcher 1"));
	if (message->datalen > 0)
		ret = TCP::write(destFD, data, message->datalen);

	return ret;
}

/* *********************************************************************** */
/*            signal handler for PM to handle dying processes              */
/* *********************************************************************** */

void Launcher::cb_sighandler_cld(int signo)
{
	int status;
	int pid=wait(&status);

	for (uint32_t i=0; i<=_singleton->_numchildren; i++)
	{
		if (_singleton->_pidlst[i] == pid)
		{
			_singleton->_pidlst[i] = -1;
			if (i == _singleton->_numchildren)
			{
				#ifdef DEBUG
					fprintf(stderr, "Launcher %d: SIGCHLD from runtime (pid=%d), status=%d\n", _singleton->_myproc, pid, WEXITSTATUS(status));
				#endif
				if (WEXITSTATUS(status) != 0)
					fprintf(stderr, "Launcher %d: non-zero return code from local runtime (pid=%d), status=%d (previous stored status=%d)\n", _singleton->_myproc, pid, WEXITSTATUS(status), WEXITSTATUS(_singleton->_returncode));
				_singleton->_returncode = WEXITSTATUS(status);
				if (_singleton->_runtimePort)
				{
					free(_singleton->_runtimePort);
					_singleton->_runtimePort = (char*)malloc(64);
					sprintf(_singleton->_runtimePort, "PLACE_%u_IS_DEAD", _singleton->_myproc);
				}
			}
			#ifdef DEBUG
			else
				fprintf(stderr, "Launcher %d: SIGCHLD from child launcher for place %d (pid=%d), status=%d\n", _singleton->_myproc, i+_singleton->_firstchildproc, pid, WEXITSTATUS(status));
			#endif

			break;
		}
	}
	// limit our lifetime to a few seconds, to allow any children to shut down on their own. Then kill em' all.
	if (_singleton->_dieAt == 0)
		_singleton->_dieAt = 2+time(NULL);
}

void Launcher::cb_sighandler_term(int signo)
{
	#ifdef DEBUG
		fprintf(stderr, "Launcher %d: got a SIGTERM\n", _singleton->_myproc);
	#endif
	for (uint32_t i = 0; i <= _singleton->_numchildren; i++)
	{
		if (_singleton->_pidlst[i] != -1)
		{
			#ifdef DEBUG
				fprintf(stderr, "Launcher %u: killing pid=%d\n", _singleton->_myproc, _singleton->_pidlst[i]);
			#endif
			kill(_singleton->_pidlst[i], SIGTERM);
		}
	}
	_singleton->_dieAt = 1; // die now.
}


/* *********************************************************************** */
/*          start a new child                                              */
/* *********************************************************************** */

static char *alloc_printf(const char *fmt, ...)
{
    va_list args;
    char try_buf[1];
    va_start(args, fmt);
    size_t sz = vsnprintf(try_buf, 0, fmt, args);
    va_end(args);
    char *r = (char*)malloc(sz+1);
    va_start(args, fmt);
    size_t s1 = vsnprintf(r, sz+1, fmt, args);
    (void) s1;
    assert (s1 == sz);
    va_end(args);
    return r;
}


// this escaping is for BLAH in the case of the following bash script
// echo "${VAR-BLAH}"
// (the double quotes are important)
static char *escape_various_things (const char *in)
{
    size_t sz = strlen(in);
    size_t out_sz = sz+3;
    char *out = static_cast<char*>(malloc(out_sz+1));
    size_t out_cnt = 0;
    for (size_t i=0 ; i<sz ; ++i) {
        if (out_cnt+3 >= out_sz) {
            out_sz = out_cnt+3;
            out = static_cast<char*>(realloc(out, out_sz+1));
        }
        switch (in[i]) {
            case '\'': // ' will break bash, turn it into "'" (note that \' does not work for some reason)
            out[out_cnt++] = '"';
            out[out_cnt++] = in[i];
            out[out_cnt++] = '"';
            break;

            case '$': // escape these with an additional backslash
            case '\"':
            case '\\':
            out[out_cnt++] = '\\';

            default: // everything else (probably) OK
            out[out_cnt++] = in[i];
            break;
        }
    }
    out[out_cnt] = '\0';
    return out;
}

// this escaping is for BLAH in the case of the following bash script
// echo 'BLAH'
static char *escape_various_things2 (const char *in)
{
    size_t sz = strlen(in);
    size_t out_sz = sz+5;
    char *out = static_cast<char*>(malloc(out_sz+1));
    size_t out_cnt = 0;
    for (size_t i=0 ; i<sz ; ++i) {
        if (out_cnt+5 >= out_sz) {
            out_sz = out_cnt+5;
            out = static_cast<char*>(realloc(out, out_sz+1));
        }
        switch (in[i]) {
            case '\'': // ' will break bash, turn it into "'" (but come out of the existing quote first)
            out[out_cnt++] = '\'';
            out[out_cnt++] = '"';
            out[out_cnt++] = '\'';
            out[out_cnt++] = '"';
            out[out_cnt++] = '\'';
            break;

            default: // everything else (probably) OK
            out[out_cnt++] = in[i];
            break;
        }
    }
    out[out_cnt] = '\0';
    return out;
}

static char *alloc_env_assign(const char *var, const char *val)
{
    return alloc_printf("%s\"=${%s-%s}\"", var, var, escape_various_things(val));
}

static char *alloc_env_always_assign(const char *var, const char *val)
{
    return alloc_printf("%s='%s'", var, escape_various_things2(val));
}

// check with the environment variable name contains characters that bash does not allow (e.g. '.')
bool is_env_var_valid (const char *var)
{
    // [a-zA-Z_]([a-zA-Z0-9_]*)

    // number as first character not allowed
    if (*var >= '0' && *var <= '9') return false;
    for (const char *v=var ; *v != '\0' ; ++v) {
        if (*v >= '0' && *v <= '9') continue;
        if (*v >= 'A' && *v <= 'Z') continue;
        if (*v >= 'a' && *v <= 'z') continue;
        if (*v == '_') continue;
        return false;
    }
    return true;
}

void Launcher::startSSHclient(uint32_t id, char* masterPort, char* remotehost)
{
	char * cmd = (char *) _realpath;

    // leak a bunch of memory, we're about to exec anyway and that will clean it up
    // on all OS that we care about

    // get an array of environment variables in the form "VAR=val"
#ifdef __MACH__
    char** environ = *_NSGetEnviron();
#else
    extern char **environ;
#endif
	// find out how many environment variables there are
    unsigned environ_sz = 0;
    while (environ[environ_sz]!=NULL) environ_sz++;

	char ** argv = (char **) alloca (sizeof(char *) * (_argc+environ_sz+32));
	int z = 0;
	argv[z] = _ssh_command;
	argv[++z] = remotehost;
    static char env_string[] = "env";
	argv[++z] = env_string;

    // deal with the environment variables
    for (unsigned i=0 ; i<environ_sz ; ++i)
    {
        if (!is_env_var_valid(environ[i])) continue;
        char *var = strdup(environ[i]);
        *strchr(var,'=') = '\0';
        if (strcmp(var,X10_HOSTFILE)==0) continue;
        if (strcmp(var,X10LAUNCHER_SSH)==0) continue;
        if (strcmp(var,X10LAUNCHER_PARENT)==0) continue;
        if (strcmp(var,X10_PLACE)==0) continue;
		char* val = getenv(var);
        assert(val!=NULL);
        #ifdef DEBUG
            fprintf(stderr, "Launcher %u: copying environment variable %s=%s for child %u.\n", _myproc, var, val, id);
        #endif
        bool x10_var = false;
        if (strncmp(var, "X10_", 4)==0) x10_var = true;
        if (strncmp(var, "X10RT_", 6)==0) x10_var = true;
        if (strncmp(var, "X10LAUNCHER_", 12)==0) x10_var = true;
        argv[++z] = x10_var ? alloc_env_always_assign(var,val) : alloc_env_assign(var, val);
        
	}

	if (_hostfname != '\0')
	{
        argv[++z] = alloc_env_assign(X10_HOSTFILE, _hostfname);
	}
    argv[++z] = alloc_env_always_assign(X10LAUNCHER_SSH, _ssh_command);
    argv[++z] = alloc_env_always_assign(X10LAUNCHER_PARENT, masterPort);
    argv[++z] = alloc_env_always_assign(X10_PLACE, alloc_printf("%d",id));
	argv[++z] = cmd;
	//argv[++z] = "env";
	for (int i = 1; i < _argc; i++)
	{
		if (strchr(_argv[i], '$') != NULL)
		{
			// make sure ssh doesn't turn a $ into an env variable lookup
			int l = strlen(_argv[i]);
			argv[z+i] = (char*)alloca(l+3);
			argv[z+i][0] = '\'';
			strcpy(&argv[z+i][1], _argv[i]);
			argv[z+i][l+1]='\'';
			argv[z+i][l+2]='\0';
		}
		else
			argv[z + i] = _argv[i];
	}
	argv[z + _argc] = NULL;

	#ifdef DEBUG
		fprintf(stderr, "Launcher %u exec-ing SSH process to start up launcher %u on %s.\n", _myproc, id, remotehost);
		for (int i=0; i<z+_argc; i++)
			fprintf (stderr, " %s ", argv[i]);
		fprintf (stderr, "\n");
	#endif

	z = execvp(argv[0], argv);

	if (z)
		DIE("Launcher %u: ssh exec failed", _myproc);
	abort();
}
