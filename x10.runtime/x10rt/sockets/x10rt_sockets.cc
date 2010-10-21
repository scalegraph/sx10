/********************************************************************************************
 * (c) Copyright IBM Corporation 2010
 * Written by Ben Herta for IBM, bherta@us.ibm.com, Septemer 2010
 * This code supports multi-place x10 programs running on one or more machines.
 * It uses SSH to spawn processes on remote machines.  A user should set up any hosts to have
 * an SSH daemon, and public/private keys configured so that there isn't any password prompt.
 * As long as you have SSH daemons, you shouldn't need any other system dependencies.
 *
 * The pgas_socket backend has its own buffers and it spawns threads to populate those buffers,
 * getting data out of the network as quickly as possible.  This implementation does not do this.
 * The network is read/written directly.
 **********************************************************************************************/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h> // for close() and sleep()
#include <errno.h> // for the strerror function
#include <sys/socket.h> // for sockets
#include <pthread.h> // for locks on the sockets
#include <poll.h> // for poll()
#include <alloca.h> // for alloca()

#include <x10rt_net.h>
#include "Launcher.h"
#include "TCP.h"

// mechanisms for the callback functions used in the register and probe methods
typedef void (*handlerCallback)(const x10rt_msg_params *);
typedef void *(*finderCallback)(const x10rt_msg_params *, x10rt_copy_sz);
typedef void (*notifierCallback)(const x10rt_msg_params *, x10rt_copy_sz);

enum MSGTYPE {STANDARD, PUT, GET, GET_COMPLETED};
//#define DEBUG_MESSAGING 1

struct x10SocketCallback
{
	handlerCallback handler;
	finderCallback finder;
	notifierCallback notifier;
};

struct x10SocketState
{
	x10rt_place numPlaces; // how many places we have.
	x10rt_place myPlaceId; // which place we're at.  Also used as the index to the array below. Local per-place memory is used.
	x10SocketCallback* callBackTable; // I'm told message ID's run from 0 to n, so a simple array using message indexes is the best solution for this table.  Local per-place memory is used.
	x10rt_msg_type callBackTableSize; // length of the above array
	char* myhost; // my own hostname, so I can detect places that are on the same machine and use localhost instead.
	bool yieldAfterProbe; // a little flag that adds sched_yield() after probe, for better performance when there are more workers than processors on a machine (or when debugging).
	pthread_mutex_t readLock; // a lock to prevent overlapping reads on each socket
	uint32_t nextSocketToCheck; // this is used in the socket read loop so that we don't give preference to the low-numbered places
	struct pollfd* socketLinks; // the file descriptors for each socket to other places
	pthread_mutex_t* writeLocks; // a lock to prevent overlapping writes on each socket
	// special case for index=myPlaceId on the above three.  The socket link is the local listen socket,
	// the read lock is used for listen socket handling and write lock for launcher communication
} state;

void probe (bool onlyProcessAccept);

/*********************************************
 *  utility methods
*********************************************/

void error(const char* message)
{
	fprintf(stderr, "Fatal Error: %s: %s\n", message, strerror(errno));
	fflush(stderr);
	abort();
}

void handleConnectionRequest()
{
	int newFD = TCP::accept(state.socketLinks[state.myPlaceId].fd, false);
	if (newFD > 0)
	{
		struct ctrl_msg m;
		int r = TCP::read(newFD, &m, sizeof(struct ctrl_msg));
		if (r == sizeof(struct ctrl_msg))
		{
			uint32_t from = m.from;
			// the higher-numbered place always decides if this connection goes or stays
			if (from < state.myPlaceId)
			{
				if (state.socketLinks[from].fd > 0) // already connected.
				{
					m.type = GOODBYE;
					m.to = from;
					m.from = state.myPlaceId;
					m.datalen = 0;
					r = TCP::write(newFD, &m, sizeof(struct ctrl_msg));
					close(newFD);
					#ifdef DEBUG
						printf("X10rt.Sockets: place %u got a redundant connection from place %u\n", state.myPlaceId, from);
					#endif
					return;
				}
				else
				{
					m.type = HELLO;
					m.to = from;
					m.from = state.myPlaceId;
					m.datalen = 0;
					r = TCP::write(newFD, &m, sizeof(struct ctrl_msg));
				}
			}
			#ifdef DEBUG
				printf("X10rt.Sockets: place %u got a new connection from place %u\n", state.myPlaceId, from);
			#endif
			pthread_mutex_init(&state.writeLocks[from], NULL);
	    	state.socketLinks[from].fd = newFD;
			state.socketLinks[from].events = POLLIN | POLLPRI;
			return;
		}
	}
	#ifdef DEBUG
		printf("X10rt.Sockets: place %u got a bad connection request\n", state.myPlaceId);
	#endif
}

int initLink(uint32_t remotePlace)
{
	if (remotePlace > state.numPlaces)
		return -1;

	if (state.socketLinks[remotePlace].fd <= 0)
		probe(true); // handle any incoming connection requests - we may be able to skip a lookup.

	if (state.socketLinks[remotePlace].fd <= 0)
	{
		#ifdef DEBUG
			printf("X10rt.Sockets: Place %u looking up place %u for a new connection\n", state.myPlaceId, remotePlace);
		#endif
		char link[1024];
		pthread_mutex_lock(&state.writeLocks[state.myPlaceId]); // because the lookup isn't currently thread-safe
		int r = Launcher::lookupPlace(state.myPlaceId, remotePlace, link, sizeof(link));
		if (r <= 0)
		{
			pthread_mutex_unlock(&state.writeLocks[state.myPlaceId]);
			return -1;
		}

		// check that the other end didn't connect to us while we were waiting for our lookup to complete.
		if (state.socketLinks[remotePlace].fd > 0)
		{
			pthread_mutex_unlock(&state.writeLocks[state.myPlaceId]);
			return state.socketLinks[remotePlace].fd;
		}

		// break apart the link into host and port
		char * c = strchr(link, ':');
		if (c == NULL)
			error("Malformed host:port");
		c[0] = '\0';
		int port = atoi(c + 1);

		// check to see if the host is our host, and if so, change it to "localhost"
		// to take advantage of any localhost OS efficiencies
		bool noDelay;
		if (strcmp(state.myhost, link) == 0)
		{
			strcpy(link, "localhost\0");
			#ifdef DEBUG
				printf("X10rt.Sockets: Place %u changed hostname for place %u to %s\n", state.myPlaceId, remotePlace, link);
			#endif
			noDelay = true;
		}
		else if (strcmp("localhost", link) == 0)
			noDelay = true;
		else
			noDelay = false;

		int newFD;
		if ((newFD = TCP::connect(link, port, 10, noDelay)) > 0)
		{
			struct ctrl_msg m;
			m.type = HELLO;
			m.to = remotePlace;
			m.from = state.myPlaceId;
			m.datalen = 0;
			int r = TCP::write(newFD, &m, sizeof(m));
			if (r != sizeof(m))
			{
				pthread_mutex_unlock(&state.writeLocks[state.myPlaceId]);
				return -1;
			}
			// both ends may reach this same point at the same time, so we can't always block on a read.
			// if we are connecting to a *higher* number place, we block, waiting to hear if this is
			// redundant or not.  Otherwise, we set the FD and continue on, assuming that the connection is good.
			if (m.to > state.myPlaceId)
			{
				r = TCP::read(newFD, &m, sizeof(m));
				if (r != sizeof(m))
				{
					pthread_mutex_unlock(&state.writeLocks[state.myPlaceId]);
					return -1;
				}
			}

			if (m.type == HELLO)
			{
				pthread_mutex_init(&state.writeLocks[remotePlace], NULL);
				state.socketLinks[remotePlace].fd = newFD;
				state.socketLinks[remotePlace].events = POLLIN | POLLPRI;
				#ifdef DEBUG
					printf("X10rt.Sockets: Place %u established a link to place %u\n", state.myPlaceId, remotePlace);
				#endif
				pthread_mutex_unlock(&state.writeLocks[state.myPlaceId]);
			}
			else
			{
				pthread_mutex_unlock(&state.writeLocks[state.myPlaceId]);
				#ifdef DEBUG
					printf("X10rt.Sockets: Place %u did NOT establish a link to place %u\n", state.myPlaceId, remotePlace);
				#endif
				while (state.socketLinks[remotePlace].fd < 0) // there is a pending connection coming in.
					probe(true);
			}
		}
		else
		{ // failed to connect to the other end.
			pthread_mutex_unlock(&state.writeLocks[state.myPlaceId]);
			return -1;
		}
	}
	return state.socketLinks[remotePlace].fd;
}

/******************************************************
 *  Main API calls.  See x10rt_net.h for documentation
*******************************************************/
void x10rt_net_init (int * argc, char ***argv, x10rt_msg_type *counter)
{
	// If this is to be a launcher process, this method will not return.
	Launcher::Setup(*argc, *argv);

	// determine the number of places
	char* NPROCS = getenv(X10LAUNCHER_NPROCS);
	if (NPROCS == NULL)
	{
		fprintf(stderr, "%s not set.  Assuming 1 place, running locally\n", X10LAUNCHER_NPROCS);
		state.numPlaces = 1;
	}
	else
	{
		state.numPlaces = atol(NPROCS);
		if (state.numPlaces <= 0) // atol failed
			error("X10LAUNCHER_NPROCS is not set to a valid number of places!");
	}

	if (state.numPlaces == 1)
	{
		state.myPlaceId = 0;
		return; // If there is only 1 place, then there are no sockets to set up.
	}

	// determine my place ID
	char* ID = getenv(X10LAUNCHER_MYID);
	if (ID == NULL)
		error("X10LAUNCHER_MYID not set!");
	else
		state.myPlaceId = atol(ID);

	if (getenv("X10RT_YIELDAFTERPROBE"))
		state.yieldAfterProbe = true;
	else
		state.yieldAfterProbe = false;

	state.nextSocketToCheck = 0;
	pthread_mutex_init(&state.readLock, NULL);
	state.socketLinks = new struct pollfd[state.numPlaces];
	state.writeLocks = new pthread_mutex_t[state.numPlaces];
	for (unsigned int i=0; i<state.numPlaces; i++)
	{
		state.socketLinks[i].fd = -1;
		state.socketLinks[i].events = 0;
	}

	// open local listen port.
	unsigned listenPort = 0;
	state.socketLinks[state.myPlaceId].fd = TCP::listen(&listenPort, 10);
	if (state.socketLinks[state.myPlaceId].fd < 0)
		error("cannot create listener port");
	pthread_mutex_init(&state.writeLocks[state.myPlaceId], NULL);
	state.socketLinks[state.myPlaceId].events = POLLIN | POLLPRI;

	// Tell our launcher our communication port number
	char portname[1024];
	TCP::getname(state.socketLinks[state.myPlaceId].fd, portname, sizeof(portname));
	pthread_mutex_lock(&state.writeLocks[state.myPlaceId]);
	if (Launcher::setPort(state.myPlaceId, portname) < 0)
		error("failed to connect to the local runtime");
	pthread_mutex_unlock(&state.writeLocks[state.myPlaceId]);

	// save our hostname for later
	char * c = strchr(portname, ':');
	c[0] = '\0';
	state.myhost = (char*)malloc(strlen(portname)+1);
	strcpy(state.myhost, portname);
	#ifdef DEBUG
		printf("X10rt.Sockets: place %u running on %s\n", state.myPlaceId, state.myhost);
	#endif
}

void x10rt_net_register_msg_receiver (x10rt_msg_type msg_type, x10rt_handler *callback)
{
	// register a pointer to methods that will handle specific message types.
	// add an entry to our type/handler table

	// there are more efficient ways to do this, but this is not in our critical path of execution, so we do it the easy way
	if (msg_type >= state.callBackTableSize)
	{
		state.callBackTable = (x10SocketCallback*)realloc(state.callBackTable, sizeof(struct x10SocketCallback)*(msg_type+1));
		if (state.callBackTable == NULL) error("Unable to allocate space for the callback table");
		state.callBackTableSize = msg_type+1;
	}

	state.callBackTable[msg_type].handler = callback;
	state.callBackTable[msg_type].finder = NULL;
	state.callBackTable[msg_type].notifier = NULL;

	#ifdef DEBUG_MESSAGING
		printf("X10rt.Sockets: place %u registered standard message %u\n", state.myPlaceId, msg_type);
	#endif
}

void x10rt_net_register_put_receiver (x10rt_msg_type msg_type, x10rt_finder *finderCallback, x10rt_notifier *notifierCallback)
{
	// register a pointer to methods that will handle specific message types.
	// add an entry to our type/handler table

	// there are more efficient ways to do this, but this is not in our critical path of execution, so we do it the easy way
	if (msg_type >= state.callBackTableSize)
	{
		state.callBackTable = (x10SocketCallback*)realloc(state.callBackTable, sizeof(struct x10SocketCallback)*(msg_type+1));
		if (state.callBackTable == NULL) error("Unable to allocate space for the callback table");
	}

	state.callBackTable[msg_type].handler = NULL;
	state.callBackTable[msg_type].finder = finderCallback;
	state.callBackTable[msg_type].notifier = notifierCallback;

	#ifdef DEBUG_MESSAGING
		printf("X10rt.Sockets: place %u registered put message %u\n", state.myPlaceId, msg_type);
	#endif
}

void x10rt_net_register_get_receiver (x10rt_msg_type msg_type, x10rt_finder *finderCallback, x10rt_notifier *notifierCallback)
{
	// register a pointer to methods that will handle specific message types.
	// add an entry to our type/handler table

	// there are more efficient ways to do this, but this is not in our critical path of execution, so we do it the easy way
	if (msg_type >= state.callBackTableSize)
	{
		state.callBackTable = (x10SocketCallback*)realloc(state.callBackTable, sizeof(struct x10SocketCallback)*(msg_type+1));
		if (state.callBackTable == NULL) error("Unable to allocate space for the callback table");
	}

	state.callBackTable[msg_type].handler = NULL;
	state.callBackTable[msg_type].finder = finderCallback;
	state.callBackTable[msg_type].notifier = notifierCallback;

	#ifdef DEBUG_MESSAGING
		printf("X10rt.Sockets: place %u registered get message %u\n", state.myPlaceId, msg_type);
	#endif
}

x10rt_place x10rt_net_nhosts (void)
{
	// return the number of places that exist.
	return state.numPlaces;
}

x10rt_place x10rt_net_here (void)
{
	// return which place this is
	return state.myPlaceId;
}

void x10rt_net_send_msg (x10rt_msg_params *parameters)
{
	if (initLink(parameters->dest_place) < 0)
		error("establishing a connection");
	#ifdef DEBUG_MESSAGING
		printf("X10rt.Sockets: place %u sending a %d byte message to place %u\n", state.myPlaceId, parameters->len, parameters->dest_place);
	#endif
	pthread_mutex_lock(&state.writeLocks[parameters->dest_place]);

	// write out the x10SocketMessage data
	// Format: type, p.type, p.len, p.msg
	enum MSGTYPE m = STANDARD;
	if (TCP::write(state.socketLinks[parameters->dest_place].fd, &m, sizeof(m)) < (int)sizeof(m))
		error("sending STANDARD type");
	if (TCP::write(state.socketLinks[parameters->dest_place].fd, &parameters->type, sizeof(parameters->type)) < (int)sizeof(parameters->type))
		error("sending STANDARD x10rt_msg_params.type");
	if (TCP::write(state.socketLinks[parameters->dest_place].fd, &parameters->len, sizeof(parameters->len)) < (int)sizeof(parameters->len))
		error("sending STANDARD x10rt_msg_params.len");
	if (parameters->len > 0)
		if (TCP::write(state.socketLinks[parameters->dest_place].fd, parameters->msg, parameters->len) < (int)parameters->len)
			error("sending STANDARD msg");
	pthread_mutex_unlock(&state.writeLocks[parameters->dest_place]);
}

void x10rt_net_send_get (x10rt_msg_params *parameters, void *buffer, x10rt_copy_sz bufferLen)
{
	if (initLink(parameters->dest_place) < 0)
		error("establishing a connection");
	#ifdef DEBUG_MESSAGING
		printf("X10rt.Sockets: place %u sending a %d byte GET message with %d byte payload to place %u\n", state.myPlaceId, parameters->len, bufferLen, parameters->dest_place);
	#endif
	pthread_mutex_lock(&state.writeLocks[parameters->dest_place]);

	// write out the x10SocketMessage data
	// Format: type, p.type, p.len, p.msg, bufferlen, bufferADDRESS
	enum MSGTYPE m = GET;
	if (TCP::write(state.socketLinks[parameters->dest_place].fd, &m, sizeof(m)) < (int)sizeof(m))
		error("sending GET MSGTYPE");
	if (TCP::write(state.socketLinks[parameters->dest_place].fd, &parameters->type, sizeof(parameters->type)) < (int)sizeof(parameters->type))
		error("sending GET x10rt_msg_params.type");
	if (TCP::write(state.socketLinks[parameters->dest_place].fd, &parameters->len, sizeof(parameters->len)) < (int)sizeof(parameters->len))
		error("sending GET x10rt_msg_params.len");
	if (parameters->len > 0)
		if (TCP::write(state.socketLinks[parameters->dest_place].fd, parameters->msg, parameters->len) < (int)parameters->len)
			error("sending GET x10rt_msg_params.msg");
	if (TCP::write(state.socketLinks[parameters->dest_place].fd, &bufferLen, sizeof(x10rt_copy_sz)) < (int)sizeof(x10rt_copy_sz))
		error("sending GET bufferLen");
	if (bufferLen > 0)
		if (TCP::write(state.socketLinks[parameters->dest_place].fd, &buffer, sizeof(void*)) < (int)sizeof(void*))
			error("sending GET buffer pointer");
	pthread_mutex_unlock(&state.writeLocks[parameters->dest_place]);
}

void x10rt_net_send_put (x10rt_msg_params *parameters, void *buffer, x10rt_copy_sz bufferLen)
{
	if (initLink(parameters->dest_place) < 0)
		error("establishing a connection");
	pthread_mutex_lock(&state.writeLocks[parameters->dest_place]);

	#ifdef DEBUG_MESSAGING
		printf("X10rt.Sockets: place %u sending a %d byte PUT message with %d byte payload to place %u\n", state.myPlaceId, parameters->len, bufferLen, parameters->dest_place);
	#endif
	// write out the x10SocketMessage data
	// Format: type, p.type, p.len, p.msg, bufferlen, buffer contents
	enum MSGTYPE m = PUT;
	if (TCP::write(state.socketLinks[parameters->dest_place].fd, &m, sizeof(m)) < (int)sizeof(m))
		error("sending PUT MSGTYPE");
	if (TCP::write(state.socketLinks[parameters->dest_place].fd, &parameters->type, sizeof(parameters->type)) < (int)sizeof(parameters->type))
		error("sending PUT x10rt_msg_params.type");
	if (TCP::write(state.socketLinks[parameters->dest_place].fd, &parameters->len, sizeof(parameters->len)) < (int)sizeof(parameters->len))
		error("sending PUT x10rt_msg_params.len");
	if (parameters->len > 0)
		if (TCP::write(state.socketLinks[parameters->dest_place].fd, parameters->msg, parameters->len) < (int)parameters->len)
			error("sending PUT x10rt_msg_params.len");
	if (TCP::write(state.socketLinks[parameters->dest_place].fd, &bufferLen, sizeof(x10rt_copy_sz)) < (int)sizeof(x10rt_copy_sz))
		error("sending PUT bufferLen");
	if (bufferLen > 0)
		if (TCP::write(state.socketLinks[parameters->dest_place].fd, buffer, bufferLen) < (int)bufferLen)
			error("sending PUT buffer");
	pthread_mutex_unlock(&state.writeLocks[parameters->dest_place]);
}

void x10rt_net_probe ()
{
	probe(false);
}

void probe (bool onlyProcessAccept)
{
	if (pthread_mutex_lock(&state.readLock) < 0)
		return;
	uint32_t whichPlaceToHandle = state.nextSocketToCheck;
	int ret = poll(state.socketLinks, state.numPlaces, 0);
	if (ret > 0)
	{ // There is at least one socket with something interesting to look at
		if (onlyProcessAccept)
		{
			if ((state.socketLinks[state.myPlaceId].revents & POLLIN) || (state.socketLinks[state.myPlaceId].revents & POLLPRI))
				whichPlaceToHandle = state.myPlaceId;
			else
			{
				pthread_mutex_unlock(&state.readLock);
				return;
			}
		}
		else
		{
			while(true)
			{
				if (state.socketLinks[whichPlaceToHandle].fd != -1 && state.socketLinks[whichPlaceToHandle].revents)
					break;

				whichPlaceToHandle++;
				if (whichPlaceToHandle == state.numPlaces)
					whichPlaceToHandle = 0;
			}

			// Set nextSocketToCheck
			if (whichPlaceToHandle == state.numPlaces-1)
				state.nextSocketToCheck = 0;
			else
				state.nextSocketToCheck = whichPlaceToHandle+1;

			state.socketLinks[whichPlaceToHandle].events = 0; // disable any further polls on this socket
		}
		pthread_mutex_unlock(&state.readLock);

		if ((state.socketLinks[whichPlaceToHandle].revents & POLLHUP) || (state.socketLinks[whichPlaceToHandle].revents & POLLERR) || (state.socketLinks[whichPlaceToHandle].revents & POLLNVAL))
		{
			#ifdef DEBUG
				printf("X10rt.Sockets: place %u detected a broken link to place %u!\n", state.myPlaceId, whichPlaceToHandle);
			#endif

			// link is broken.  Close it down.
			close(state.socketLinks[whichPlaceToHandle].fd);
			state.socketLinks[whichPlaceToHandle].fd = -1;
			// TODO - notify the runtime of this?
		}
		else if ((state.socketLinks[whichPlaceToHandle].revents & POLLIN) || (state.socketLinks[whichPlaceToHandle].revents & POLLPRI))
		{
			#ifdef DEBUG_MESSAGING
				printf("X10rt.Sockets: place %u probe processing a message from place %u\n", state.myPlaceId, whichPlaceToHandle);
			#endif

			if (whichPlaceToHandle == state.myPlaceId) // special case.  This is an incoming connection request.
				handleConnectionRequest();
			else
			{
				// Format: type, p.type, p.len, p.msg
				enum MSGTYPE t;
				int r = TCP::read(state.socketLinks[whichPlaceToHandle].fd, &t, sizeof(enum MSGTYPE));
				if (r < (int)sizeof(enum MSGTYPE) || t > GET_COMPLETED) // closed connection
				{
					#ifdef DEBUG_MESSAGING
						printf("X10rt.Sockets: Place %u detected a bad message from place %u (likely a closed socket)\n", state.myPlaceId, whichPlaceToHandle);
					#endif
					close(state.socketLinks[whichPlaceToHandle].fd);
					state.socketLinks[whichPlaceToHandle].fd = -1;
					return;
				}
				#ifdef DEBUG_MESSAGING
					printf("X10rt.Sockets: place %u picked up a message from place %u\n", state.myPlaceId, whichPlaceToHandle);
				#endif

				x10rt_msg_params mp;
				mp.dest_place = state.myPlaceId;
				if (TCP::read(state.socketLinks[whichPlaceToHandle].fd, &mp.type, sizeof(x10rt_msg_type)) < (int)sizeof(x10rt_msg_type))
					error("reading x10rt_msg_params.type");
				if (TCP::read(state.socketLinks[whichPlaceToHandle].fd, &mp.len, sizeof(uint32_t)) < (int)sizeof(uint32_t))
					error("reading x10rt_msg_params.len");
				bool heapAllocated = false;
				if (mp.len > 0)
				{
					mp.msg = alloca(mp.len);
					if (mp.msg == NULL) // stack allocation failed... try heap allocation
					{
						mp.msg = malloc(mp.len);
						if (mp.msg == NULL)
							error("unable to allocate memory for an incoming message");
						heapAllocated = true;
					}
					if (TCP::read(state.socketLinks[whichPlaceToHandle].fd, mp.msg, mp.len) < (int)mp.len)
						error("reading x10rt_msg_params.msg");
				}
				else
					mp.msg = NULL;

				switch (t)
				{
					case STANDARD:
					{
						pthread_mutex_lock(&state.readLock);
						state.socketLinks[whichPlaceToHandle].events = POLLIN | POLLPRI;
						pthread_mutex_unlock(&state.readLock);
						handlerCallback hcb = state.callBackTable[mp.type].handler;
						hcb(&mp);
					}
					break;
					case PUT:
					{
						x10rt_copy_sz dataLen;
						if (TCP::read(state.socketLinks[whichPlaceToHandle].fd, &dataLen, sizeof(x10rt_copy_sz)) < (int)sizeof(x10rt_copy_sz))
							error("reading PUT datalen");

						finderCallback fcb = state.callBackTable[mp.type].finder;
						void* dest = fcb(&mp, dataLen); // get the pointer to the destination location
						if (dest == NULL)
							error("invalid buffer provided for a PUT");
						if (TCP::read(state.socketLinks[whichPlaceToHandle].fd, dest, dataLen) < (int)dataLen)
							error("reading PUT data");
						pthread_mutex_lock(&state.readLock);
						state.socketLinks[whichPlaceToHandle].events = POLLIN | POLLPRI;
						pthread_mutex_unlock(&state.readLock);
						notifierCallback ncb = state.callBackTable[mp.type].notifier;
						ncb(&mp, dataLen);
					}
					break;
					case GET:
					{
						// this is the request for data.
						// Format: type, p.type, p.len, p.msg, bufferlen, bufferADDRESS
						x10rt_copy_sz dataLen;
						void* remotePtr; // THIS IS A POINTER ON A REMOTE MACHINE.  NOT VALID HERE
						if (TCP::read(state.socketLinks[whichPlaceToHandle].fd, &dataLen, sizeof(x10rt_copy_sz)) < (int)sizeof(x10rt_copy_sz))
							error("reading GET dataLen");
						if (dataLen > 0)
							if (TCP::read(state.socketLinks[whichPlaceToHandle].fd, &remotePtr, sizeof(void*)) < (int)sizeof(void*))
								error("reading GET pointer");

						pthread_mutex_lock(&state.readLock);
						state.socketLinks[whichPlaceToHandle].events = POLLIN | POLLPRI;
						pthread_mutex_unlock(&state.readLock);

						finderCallback fcb = state.callBackTable[mp.type].finder;
						void* src = fcb(&mp, dataLen);

						// send the data to the other side (the link is good, because we just read from it)
						pthread_mutex_lock(&state.writeLocks[whichPlaceToHandle]);
						// Format: type, p.type, p.len, p.msg, bufferlen, bufferADDRESS, buffer
						enum MSGTYPE m = GET_COMPLETED;
						if (TCP::write(state.socketLinks[whichPlaceToHandle].fd, &m, sizeof(m)) < (int)sizeof(m))
							error("sending GET_COMPLETED MSGTYPE");
						if (TCP::write(state.socketLinks[whichPlaceToHandle].fd, &mp.type, sizeof(mp.type)) < (int)sizeof(mp.type))
							error("sending GET_COMPLETED x10rt_msg_params.type");
						if (TCP::write(state.socketLinks[whichPlaceToHandle].fd, &mp.len, sizeof(mp.len)) < (int)sizeof(mp.len))
							error("sending GET_COMPLETED x10rt_msg_params.len");
						if (mp.len > 0)
							if (TCP::write(state.socketLinks[whichPlaceToHandle].fd, mp.msg, mp.len) < (int)mp.len)
								error("sending GET_COMPLETED x10rt_msg_params.msg");
						if (TCP::write(state.socketLinks[whichPlaceToHandle].fd, &dataLen, sizeof(x10rt_copy_sz)) < (int)sizeof(x10rt_copy_sz))
							error("sending GET_COMPLETED dataLen");
						if (dataLen > 0)
						{
							if (TCP::write(state.socketLinks[whichPlaceToHandle].fd, &remotePtr, sizeof(void*)) < (int)sizeof(void*))
								error("sending GET_COMPLETED remotePtr");
							if (TCP::write(state.socketLinks[whichPlaceToHandle].fd, src, dataLen) < (int)dataLen)
								error("sending GET_COMPLETED data");
						}
						pthread_mutex_unlock(&state.writeLocks[whichPlaceToHandle]);
					}
					break;
					case GET_COMPLETED:
					{
						x10rt_copy_sz dataLen;
						void* buffer;

						if (TCP::read(state.socketLinks[whichPlaceToHandle].fd, &dataLen, sizeof(x10rt_copy_sz)) < (int)sizeof(x10rt_copy_sz))
							error("reading GET_COMPLETED dataLen");
						if (dataLen > 0)
						{
							if (TCP::read(state.socketLinks[whichPlaceToHandle].fd, &buffer, sizeof(void*)) < (int)sizeof(void*))
								error("reading GET_COMPLETED pointer");
							if (TCP::read(state.socketLinks[whichPlaceToHandle].fd, buffer, dataLen) < (int)dataLen)
								error("reading GET_COMPLETED data");
						}
						pthread_mutex_lock(&state.readLock);
						state.socketLinks[whichPlaceToHandle].events = POLLIN | POLLPRI;
						pthread_mutex_unlock(&state.readLock);

						mp.dest_place = whichPlaceToHandle;
						notifierCallback ncb = state.callBackTable[mp.type].notifier;
						ncb(&mp, dataLen);
					}
					break;
					default: // this should never happen
						error("Unknown message type found");
					break;
				}
				if (heapAllocated)
					free(mp.msg);
			}
		}
		else
		{
			// when the socket gets closed, we might get into this code here.
			#ifdef DEBUG_MESSAGING
				printf("X10rt.Sockets: place %u got a dud message from place %u\n", state.myPlaceId, whichPlaceToHandle);
			#endif
			pthread_mutex_lock(&state.readLock);
			state.socketLinks[whichPlaceToHandle].events = POLLIN | POLLPRI;
			pthread_mutex_unlock(&state.readLock);
		}
	}
	else
	{
		pthread_mutex_unlock(&state.readLock);
		if (state.yieldAfterProbe) // This would be a good time for a yield in some systems.
			sched_yield();
	}
}

void x10rt_net_finalize (void)
{
	if (state.numPlaces == 1)
		return;

	#ifdef DEBUG
		printf("X10rt.Sockets: shutting down place %u\n", state.myPlaceId);
	#endif

	for (unsigned int i=0; i<state.numPlaces; i++)
	{
		if (state.socketLinks[i].fd != -1)
		{
			pthread_mutex_lock(&state.writeLocks[i]);
			close(state.socketLinks[i].fd);
			pthread_mutex_unlock(&state.writeLocks[i]);
			pthread_mutex_destroy(&state.writeLocks[i]);
		}
	}

	if (Launcher::_parentLauncherControlLink != -1)
		close(Launcher::_parentLauncherControlLink);
	pthread_mutex_destroy(&state.readLock);
	free(state.myhost);
	free(state.socketLinks);
	free(state.writeLocks);
}

/*************************************************
 * All of the stuff below is not used in the socket
 * backend, and we rely on the emulation layer to
 * convert these into messages for us.
 *************************************************/


void x10rt_net_internal_barrier (void)
{
	#ifdef DEBUG
		printf("X10rt.Sockets internal barrier called at place %u\n", state.myPlaceId);
	#endif
}

int x10rt_net_supports (x10rt_opt o)
{
    return 0;
}

void x10rt_net_remote_op (x10rt_place place, x10rt_remote_ptr victim, x10rt_op_type type, unsigned long long value)
{
	error("x10rt_net_remote_op not implemented");
}

x10rt_remote_ptr x10rt_net_register_mem (void *ptr, size_t len)
{
	error("x10rt_net_register_mem not implemented");
	return NULL;
}

void x10rt_net_team_new (x10rt_place placec, x10rt_place *placev,
                         x10rt_completion_handler2 *ch, void *arg)
{
	error("x10rt_net_team_new not implemented");
}

void x10rt_net_team_del (x10rt_team team, x10rt_place role,
                         x10rt_completion_handler *ch, void *arg)
{
	error("x10rt_net_team_del not implemented");
}

x10rt_place x10rt_net_team_sz (x10rt_team team)
{
	error("x10rt_net_team_sz not implemented");
    return 0;
}

void x10rt_net_team_split (x10rt_team parent, x10rt_place parent_role, x10rt_place color,
		x10rt_place new_role, x10rt_completion_handler2 *ch, void *arg)
{
	error("x10rt_net_team_split not implemented");
}

void x10rt_net_barrier (x10rt_team team, x10rt_place role, x10rt_completion_handler *ch, void *arg)
{
	error("x10rt_net_barrier not implemented");
}

void x10rt_net_bcast (x10rt_team team, x10rt_place role, x10rt_place root, const void *sbuf,
		void *dbuf, size_t el, size_t count, x10rt_completion_handler *ch, void *arg)
{
	error("x10rt_net_bcast not implemented");
}

void x10rt_net_scatter (x10rt_team team, x10rt_place role, x10rt_place root, const void *sbuf,
		void *dbuf, size_t el, size_t count, x10rt_completion_handler *ch, void *arg)
{
	error("x10rt_net_scatter not implemented");
}

void x10rt_net_alltoall (x10rt_team team, x10rt_place role, const void *sbuf, void *dbuf,
		size_t el, size_t count, x10rt_completion_handler *ch, void *arg)
{
	error("x10rt_net_alltoall not implemented");
}

void x10rt_net_allreduce (x10rt_team team, x10rt_place role, const void *sbuf, void *dbuf,
		x10rt_red_op_type op, x10rt_red_type dtype, size_t count, x10rt_completion_handler *ch, void *arg)
{
	error("x10rt_net_allreduce not implemented");
}
