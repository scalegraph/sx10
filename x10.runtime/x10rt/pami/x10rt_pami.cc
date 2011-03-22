/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2010-2011.
 *
 *  This file was written by Ben Herta for IBM: bherta@us.ibm.com
 */

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <unistd.h> // sleep()
#include <errno.h> // for the strerror function
#include <sched.h> // for sched_yield()
#include <pthread.h> // for lock on the team mapping table
#include <x10rt_net.h>
#include <pami.h>

//#define DEBUG 1

enum MSGTYPE {STANDARD=1, PUT, GET, GET_COMPLETE, NEW_TEAM}; // PAMI doesn't send messages with type=0... it just silently eats them.
//mechanisms for the callback functions used in the register and probe methods
typedef void (*handlerCallback)(const x10rt_msg_params *);
typedef void *(*finderCallback)(const x10rt_msg_params *, x10rt_copy_sz);
typedef void (*notifierCallback)(const x10rt_msg_params *, x10rt_copy_sz);
typedef void (*teamCallback2)(x10rt_team, void *);
typedef void (*teamCallback)(void *);

// the values for pami_dt are mapped to the indexes of x10rt_red_type
pami_dt DATATYPE_CONVERSION_TABLE[] = {PAMI_UNSIGNED_CHAR, PAMI_SIGNED_CHAR, PAMI_SIGNED_SHORT, PAMI_UNSIGNED_SHORT, PAMI_SIGNED_INT,
		PAMI_UNSIGNED_INT, PAMI_SIGNED_LONG_LONG, PAMI_UNSIGNED_LONG_LONG, PAMI_DOUBLE, PAMI_FLOAT, PAMI_LOC_DOUBLE_INT};
size_t DATATYPE_MULTIPLIER_TABLE[] = {1,1,2,2,4,4,8,8,8,4,12}; // the number of bytes used for each entry in the table above.
// values for pami_op are mapped to indexes of x10rt_red_op_type
pami_op OPERATION_CONVERSION_TABLE[] = {PAMI_SUM, PAMI_PROD, PAMI_UNDEFINED_OP, PAMI_BAND, PAMI_BOR, PAMI_BXOR, PAMI_MAX, PAMI_MIN};

struct x10rtCallback
{
	handlerCallback handler;
	finderCallback finder;
	notifierCallback notifier;
};

struct x10rt_pami_header_data
{
	x10rt_msg_params x10msg;
    uint32_t data_len;
    void* data_ptr;
    void* callbackPtr; // stores the header address for GET_COMPLETE
};

struct x10rt_pami_team_create
{
	teamCallback2 cb2;
	void *arg;
	x10rt_place *colors;
	uint32_t teamIndex;
	x10rt_place parent_role;
};

struct x10rt_pami_team_callback
{
	teamCallback tcb;
	void *arg;
	pami_xfer_t operation;
};


struct x10rt_pami_team
{
	pami_geometry_t geometry; // abstract geometry ID
	uint32_t size; // number of members in the team
	pami_task_t *places; // list of team members
};

struct x10PAMIState
{
	uint32_t numPlaces;
	uint32_t myPlaceId;
	x10rtCallback* callBackTable;
	x10rt_msg_type callBackTableSize;
	pami_client_t client; // the PAMI client instance used for this place
	pthread_t *threadMap; // The thread ID associated with each context
	pami_context_t *context; // PAMI context associated with the client
	x10rt_pami_team *teams;
	uint32_t lastTeamIndex;
	pthread_mutex_t teamLock;
	int numParallelContexts; // When X10_STATIC_THREADS=true, this is set to the value of X10_NTHREADS. Otherwise it's 0.
} state;

static void local_msg_dispatch (pami_context_t context, void* cookie, const void* header_addr, size_t header_size,
		const void * pipe_addr, size_t pipe_size, pami_endpoint_t origin, pami_recv_t* recv);
static void local_put_dispatch (pami_context_t context, void* cookie, const void* header_addr, size_t header_size,
		const void * pipe_addr, size_t pipe_size, pami_endpoint_t origin, pami_recv_t* recv);
static void local_get_dispatch (pami_context_t context, void* cookie, const void* header_addr, size_t header_size,
		const void * pipe_addr, size_t pipe_size, pami_endpoint_t origin, pami_recv_t* recv);
static void get_complete_dispatch (pami_context_t context, void* cookie, const void* header_addr, size_t header_size,
		const void * pipe_addr, size_t pipe_size, pami_endpoint_t origin, pami_recv_t* recv);
static void team_create_dispatch (pami_context_t context, void* cookie, const void* header_addr, size_t header_size,
		const void * pipe_addr, size_t pipe_size, pami_endpoint_t origin, pami_recv_t* recv);


/*
 * The error method prints out serious errors, then immediately exits
 */
void error(const char* msg, ...)
{
	char buffer[1200];
	va_list ap;
	va_start(ap, msg);
	vsnprintf(buffer, sizeof(buffer), msg, ap);
	va_end(ap);
	strcat(buffer, "  ");
	int blen = strlen(buffer);
	PAMI_Error_text(buffer+blen, 1199-blen);
	fprintf(stderr, "X10 PAMI error: %s\n", buffer);
	if (errno != 0)
		fprintf(stderr, "X10 PAMI errno: %s\n", strerror(errno));

	fflush(stderr);
	exit(EXIT_FAILURE);
}

// small bit of code to extend the number of allocated teams.  Returns the original lastTeamIndex
unsigned expandTeams(unsigned numNewTeams)
{
	int r;
	pthread_mutex_lock(&state.teamLock);
		void* oldTeams = state.teams;
		int oldSize = (state.lastTeamIndex+1)*sizeof(x10rt_pami_team);
		int newSize = (state.lastTeamIndex+1+numNewTeams)*sizeof(x10rt_pami_team);
		void* newTeams = malloc(newSize);
		if (newTeams == NULL) error("Unable to allocate memory to add more teams");
		memcpy(newTeams, oldTeams, oldSize);
		memset(((char*)newTeams)+oldSize, 0, newSize-oldSize);
		state.teams = (x10rt_pami_team*)newTeams;
		r = state.lastTeamIndex;
		state.lastTeamIndex+=numNewTeams;
	pthread_mutex_unlock(&state.teamLock);
	free(oldTeams);
	return r;
}

void initializeContext(int index)
{
	pami_result_t status = PAMI_ERROR;

	pami_dispatch_hint_t hints;
	memset(&hints, 0, sizeof(pami_send_hint_t));
	hints.recv_contiguous = PAMI_HINT_ENABLE;

	size_t ncontext = 1;
	if ((status = PAMI_Context_createv(state.client, NULL, 0, &state.context[index], ncontext)) != PAMI_SUCCESS)
		error("Unable to initialize the PAMI context: %i\n", status);

	// set up our callback functions, which will convert PAMI messages to X10 callbacks
	pami_dispatch_callback_function fn;
	fn.p2p = local_msg_dispatch;
	if ((status = PAMI_Dispatch_set(state.context[index], STANDARD, fn, NULL, hints)) != PAMI_SUCCESS)
		error("Unable to register standard dispatch handler");

	pami_dispatch_callback_function fn2;
	fn2.p2p = local_put_dispatch;
	if ((status = PAMI_Dispatch_set(state.context[index], PUT, fn2, NULL, hints)) != PAMI_SUCCESS)
		error("Unable to register put dispatch handler");

	pami_dispatch_callback_function fn3;
	fn3.p2p = local_get_dispatch;
	if ((status = PAMI_Dispatch_set(state.context[index], GET, fn3, NULL, hints)) != PAMI_SUCCESS)
		error("Unable to register get dispatch handler");

	pami_dispatch_callback_function fn4;
	fn4.p2p = get_complete_dispatch;
	if ((status = PAMI_Dispatch_set(state.context[index], GET_COMPLETE, fn4, NULL, hints)) != PAMI_SUCCESS)
		error("Unable to register get_complete_dispatch handler");

	pami_dispatch_callback_function fn5;
	fn5.p2p = team_create_dispatch;
	if ((status = PAMI_Dispatch_set(state.context[index], NEW_TEAM, fn5, NULL, hints)) != PAMI_SUCCESS)
		error("Unable to register team_create_dispatch handler");
}

/*
 * This method returns the context associated with this thread
 */
pami_context_t getContext()
{
	if (state.numParallelContexts == 0)
		return state.context[0];

	pthread_t myId = pthread_self();
	for (int i=0; i<state.numParallelContexts; i++)
	{
		if (state.threadMap[i] == myId)
			return state.context[i];
		else if (state.threadMap[i] == 0)
		{
			state.threadMap[i] = myId;
			//initializeContext(i);
			return state.context[i];
		}
	}

	error("Unknown thread looking for its context");
	return NULL; // to keep the compiler happy
}

void x10rt_local_probe(pami_context_t context)
{
	pami_result_t status = PAMI_ERROR;
	if (state.numParallelContexts>0)
	{
		status = PAMI_Context_advance(context, 100);
		if (status == PAMI_EAGAIN)
			sched_yield();
	}
	else
	{
		status = PAMI_Context_lock(context);
		if (status != PAMI_SUCCESS) error ("Unable to lock the PAMI context");
		status = PAMI_Context_advance(context, 100);
		status = PAMI_Context_unlock(context);
		if (status != PAMI_SUCCESS) error ("Unable to unlock the PAMI context");
	}
}

/*
 * This small method is used to hold-up some calls until the data transmission is complete, by simply decrementing a counter
 */
static void cookie_decrement (pami_context_t   context,
                       void          * cookie,
                       pami_result_t    result)
{
	if (result != PAMI_SUCCESS)
		error("Error detected in cookie_decrement");

	unsigned * value = (unsigned *) cookie;
	#ifdef DEBUG
		fprintf(stderr, "Place %u decrement() cookie = %p, %d => %d\n", state.myPlaceId, cookie, *value, *value-1);
	#endif
	--*value;
}

/*
 * When a standard message is too large to fit in one transmission block, it gets broken up.
 * This method handles the final part of these messages (local_msg_dispatch handles the first part)
 */
static void std_msg_complete (pami_context_t   context,
                       void          * cookie,
                       pami_result_t    result)
{
	if (result != PAMI_SUCCESS)
		error("Error detected in std_msg_complete");

	struct x10rt_msg_params *hdr = (struct x10rt_msg_params*) cookie;
	#ifdef DEBUG
		fprintf(stderr, "Place %u processing delayed standard message %i, len=%u\n", state.myPlaceId, hdr->type, hdr->len);
	#endif
	handlerCallback hcb = state.callBackTable[hdr->type].handler;
	hcb(hdr);

	if (hdr->len > 0) // will always be true, but check anyway
		free(hdr->msg);
	free(hdr);
}

/*
 * This is the standard message handler.  It gets called on the receiving end for all standard messages
 */
static void local_msg_dispatch (
	    pami_context_t        context,      /**< IN: PAMI context */
	    void               * cookie,       /**< IN: dispatch cookie */
	    const void         * header_addr,  /**< IN: header address */
	    size_t               header_size,  /**< IN: header size */
	    const void         * pipe_addr,    /**< IN: address of PAMI pipe buffer */
	    size_t               pipe_size,    /**< IN: size of PAMI pipe buffer */
	    pami_endpoint_t      origin,
	    pami_recv_t         * recv)        /**< OUT: receive message structure */
{
	if (recv) // not all of the data is here yet, so we need to tell PAMI what to run when it's all here.
	{
		struct x10rt_msg_params *hdr = (struct x10rt_msg_params *)malloc(sizeof(struct x10rt_msg_params));
		hdr->dest_place = state.myPlaceId;
		hdr->len = pipe_size; // this is going to be large-ish, otherwise recv would be null
		hdr->msg = malloc(pipe_size);
		if (hdr->msg == NULL)
			error("Unable to allocate a msg_dispatch buffer of size %u", pipe_size);
		hdr->type = *((x10rt_msg_type*)header_addr);
		#ifdef DEBUG
			fprintf(stderr, "Place %u waiting on a partially delivered message %i, len=%lu\n", state.myPlaceId, hdr->type, pipe_size);
		#endif
		recv->local_fn = std_msg_complete;
		recv->cookie   = hdr;
		recv->type     = PAMI_TYPE_CONTIGUOUS;
		recv->addr     = hdr->msg;
		recv->offset   = 0;
		recv->data_fn  = PAMI_DATA_COPY;
	}
	else
	{	// all the data is available, and ready to process
		x10rt_msg_params mp;
		mp.dest_place = state.myPlaceId;
		mp.type = *((x10rt_msg_type*)header_addr);
		mp.len = pipe_size;
		if (mp.len > 0)
		{
			mp.msg = alloca(pipe_size);
			if (mp.msg == NULL)
				error("Unable to alloca a msg buffer of size %u", pipe_size);
			memcpy(mp.msg, pipe_addr, pipe_size);
		}
		else
			mp.msg = NULL;

		#ifdef DEBUG
			fprintf(stderr, "Place %u processing standard message %i, len=%u\n", state.myPlaceId, mp.type, mp.len);
		#endif
		handlerCallback hcb = state.callBackTable[mp.type].handler;
		hcb(&mp);
	}
}

/*
 * This method is called at the receiving end of a PUT message, after all the data has arrived.
 * It sends a PUT_COMPLETE to the other end (if data was copied), and calls the x10 notifier
 */
static void put_handler_complete (pami_context_t   context,
                       void          * cookie,
                       pami_result_t    result)
{
	if (result != PAMI_SUCCESS)
		error("put_handler_complete discovered a communication error");

	struct x10rt_pami_header_data* header = (struct x10rt_pami_header_data*) cookie;
	#ifdef DEBUG
		fprintf(stderr, "Place %u issuing put notifier callback, type=%i, msglen=%u, datalen=%u\n", state.myPlaceId, header->x10msg.type, header->x10msg.len, header->data_len);
	#endif
	notifierCallback ncb = state.callBackTable[header->x10msg.type].notifier;
	ncb(&header->x10msg, header->data_len);

	if (header->x10msg.len > 0)
		free(header->x10msg.msg);
	free(header);
}

/*
 * This method is called at the receiving end of a PUT message, before all the data has arrived.  It finds the buffers
 * that the incoming data should go into, and issues a RDMA get to pull that data in.  It registers put_handler_complete
 * to run after the data has been transmitted.
 */
static void local_put_dispatch (
	    pami_context_t        context,      /**< IN: PAMI context */
	    void               * cookie,       /**< IN: dispatch cookie */
	    const void         * header_addr,  /**< IN: header address */
	    size_t               header_size,  /**< IN: header size */
	    const void         * pipe_addr,    /**< IN: address of PAMI pipe buffer */
	    size_t               pipe_size,    /**< IN: size of PAMI pipe buffer */
	    pami_endpoint_t      origin,
	    pami_recv_t         * recv)        /**< OUT: receive message structure */
{
	pami_result_t status = PAMI_ERROR;

	if (recv) // not all of the data is here yet, so we need to tell PAMI what to run when it's all here.
		error("non-immediate dispatch not yet implemented");

	// else, all the data is available, and ready to process
	struct x10rt_pami_header_data* localParameters = (struct x10rt_pami_header_data*)malloc(sizeof(struct x10rt_pami_header_data));
	struct x10rt_pami_header_data* incomingParameters = (struct x10rt_pami_header_data*) header_addr;
	localParameters->x10msg.dest_place = state.myPlaceId;
	localParameters->data_len = incomingParameters->data_len;
	localParameters->data_ptr = incomingParameters->data_ptr;
	localParameters->x10msg.type = incomingParameters->x10msg.type;
	localParameters->x10msg.len = pipe_size;
	if (pipe_size > 0)
	{
		localParameters->x10msg.msg = malloc(pipe_size);
		if (localParameters->x10msg.msg == NULL)
			error("Unable to malloc a buffer to hold incoming PUT data");
		memcpy(localParameters->x10msg.msg, pipe_addr, pipe_size); // save the message for later
	}
	else
		localParameters->x10msg.msg = NULL;

	#ifdef DEBUG
		fprintf(stderr, "Place %u processing PUT message %i from %u, msglen=%u, len=%u, remote buf=%p, remote cookie=%p\n", state.myPlaceId, localParameters->x10msg.type, origin, localParameters->x10msg.len, localParameters->data_len, incomingParameters->data_ptr, incomingParameters->x10msg.msg);
	#endif

	finderCallback fcb = state.callBackTable[localParameters->x10msg.type].finder;
	void* dest = fcb(&localParameters->x10msg, localParameters->data_len); // get the pointer to the destination location
	if (dest == NULL)
		error("invalid buffer provided for a PUT");

	if (localParameters->data_len > 0) // PAMI doesn't like zero sized messages
	{
		pami_get_simple_t parameters;
		memset(&parameters, 0, sizeof (parameters));
		parameters.rma.dest    = origin;
		parameters.rma.bytes   = localParameters->data_len;
		parameters.rma.cookie  = localParameters;
		parameters.rma.done_fn = put_handler_complete;
		parameters.addr.local  = dest;
		parameters.addr.remote = localParameters->data_ptr;
		localParameters->data_ptr = incomingParameters->x10msg.msg; // the cookie from the other end
		localParameters->x10msg.dest_place = origin;
		if ((status = PAMI_Get (context, &parameters)) != PAMI_SUCCESS)
			error("Error sending data for PUT response");
	}
	else
		put_handler_complete(context, localParameters, PAMI_SUCCESS);
}

/*
 * This method is called at the receiving end of a GET message, after all the data has been sent.
 * It sends a GET_COMPLETE to the originator
 */
static void get_handler_complete (pami_context_t   context,
                       void          * cookie,
                       pami_result_t    result)
{
	if (result != PAMI_SUCCESS)
		error("Error detected in get_handler_complete");

	x10rt_msg_params* header = (x10rt_msg_params*) cookie;

	#ifdef DEBUG
		fprintf(stderr, "Place %u running get_handler_complete, dest=%u type=%i, remote cookie=%p\n", state.myPlaceId, header->dest_place, header->type, header->msg);
	#endif

	// send a GET_COMPLETE to the originator
	pami_result_t   status = PAMI_ERROR;

	volatile unsigned send_active = 1;
	pami_send_t parameters;
	parameters.send.dispatch        = GET_COMPLETE;
	parameters.send.header.iov_base = &header->msg; // sending the origin pointer back
	parameters.send.header.iov_len  = sizeof(void *);
	parameters.send.data.iov_base   = NULL;
	parameters.send.data.iov_len    = 0;
	parameters.send.dest 			= header->dest_place;
	memset(&parameters.send.hints, 0, sizeof(pami_send_hint_t));
	parameters.events.cookie        = (void *) &send_active;
	parameters.events.local_fn      = cookie_decrement;
	parameters.events.remote_fn     = NULL;

	if ((status = PAMI_Send(context, &parameters)) != PAMI_SUCCESS)
		error("Unable to send a GET_COMPLETE message from %u to %u: %i\n", state.myPlaceId, header->dest_place, status);

	#ifdef DEBUG
		fprintf(stderr, "(%u) get_handler_complete before advance\n", state.myPlaceId);
	#endif
	// hold up this thread until the message buffers have been copied out by PAMI
	while (send_active)
		x10rt_local_probe(context);
	#ifdef DEBUG
		fprintf(stderr, "(%u) get_handler_complete after advance\n", state.myPlaceId);
	#endif
	free(header);
}

/*
 * This is called at the receiving end of a GET message.  It finds the data that is requested locally, and issues
 * a RDMA put to push that data over to the requester (the end that sent the GET).  It then sends a GET_COMPLETE to
 * the originator
 */
static void local_get_dispatch (
	    pami_context_t        context,      /**< IN: PAMI context */
	    void               * cookie,       /**< IN: dispatch cookie */
	    const void         * header_addr,  /**< IN: header address */
	    size_t               header_size,  /**< IN: header size */
	    const void         * pipe_addr,    /**< IN: address of PAMI pipe buffer */
	    size_t               pipe_size,    /**< IN: size of PAMI pipe buffer */
	    pami_endpoint_t      origin,
	    pami_recv_t         * recv)        /**< OUT: receive message structure */
{
	pami_result_t status = PAMI_ERROR;

	if (recv) // not all of the data is here yet, so we need to tell PAMI what to run when it's all here.
		error("non-immediate dispatch not yet implemented");

	// else, all the data is available, and ready to process
	x10rt_msg_params* localParameters = (x10rt_msg_params*) malloc(sizeof(x10rt_msg_params));
	struct x10rt_pami_header_data* header = (struct x10rt_pami_header_data*) header_addr;
	localParameters->dest_place = state.myPlaceId;
	localParameters->type = header->x10msg.type;
	localParameters->msg = (void*)pipe_addr;
	localParameters->len = pipe_size;

	// issue a put to the originator
	#ifdef DEBUG
		fprintf(stderr, "Place %u processing GET message %i, datalen=%u, remote cookie=%p\n", state.myPlaceId, header->x10msg.type, header->data_len, header->x10msg.msg);
	#endif

	finderCallback fcb = state.callBackTable[localParameters->type].finder;
	void* src = fcb(localParameters, header->data_len);
	if (src == NULL)
		error("invalid buffer provided for the source of a GET");

	localParameters->dest_place = origin;
	localParameters->msg = header->callbackPtr; // cookie for the other side

	if (header->data_len > 0) // PAMI doesn't like it if we try to RDMA zero sized messages
	{
		pami_put_simple_t parameters;
		memset(&parameters, 0, sizeof (parameters));
		parameters.rma.dest    = origin;
		parameters.rma.bytes   = header->data_len;
		parameters.rma.cookie  = localParameters;
		parameters.rma.done_fn = get_handler_complete;
		parameters.addr.local  = src;
		parameters.addr.remote = header->data_ptr;
		if ((status = PAMI_Put (context, &parameters)) != PAMI_SUCCESS)
			error("Error sending data for GET response");
		#ifdef DEBUG
			fprintf(stderr, "Place %u pushing out %u bytes of GET message data\n", state.myPlaceId, header->data_len);
		#endif
	}
	else
		get_handler_complete(context, localParameters, PAMI_SUCCESS);
}

/*
 * This method runs at the sending end of a GET, after the data has been transferred.  It runs the notifier
 */
static void get_complete_dispatch (
	    pami_context_t        context,      /**< IN: PAMI context */
	    void               * cookie,       /**< IN: dispatch cookie */
	    const void         * header_addr,  /**< IN: header address */
	    size_t               header_size,  /**< IN: header size */
	    const void         * pipe_addr,    /**< IN: address of PAMI pipe buffer */
	    size_t               pipe_size,    /**< IN: size of PAMI pipe buffer */
	    pami_endpoint_t      origin,
	    pami_recv_t         * recv)        /**< OUT: receive message structure */
{
	struct x10rt_pami_header_data* header = *(struct x10rt_pami_header_data**)header_addr;

	#ifdef DEBUG
		fprintf(stderr, "Place %u got GET_COMPLETE from %u, header=%p\n", state.myPlaceId, origin, (void*)header);
		fprintf(stderr, "     type=%i, datalen=%u. Calling notifier\n", header->x10msg.type, header->data_len);
	#endif

	notifierCallback ncb = state.callBackTable[header->x10msg.type].notifier;
	ncb(&header->x10msg, header->data_len);

	#ifdef DEBUG
		fprintf(stderr, "Place %u finished GET message\n", state.myPlaceId);
	#endif
	if (header->x10msg.len > 0)
		free(header->x10msg.msg);
	free(header);
}

static void team_creation_complete (pami_context_t   context,
                       void          * cookie,
                       pami_result_t    result)
{
	if (result != PAMI_SUCCESS)
		error("Error detected in team_creation_complete");

	if (cookie != NULL)
	{
		x10rt_pami_team_create *team = (x10rt_pami_team_create*)cookie;
		#ifdef DEBUG
			fprintf(stderr, "New team %u created at place %u\n", team->teamIndex, state.myPlaceId);
		#endif
		team->cb2(team->teamIndex, team->arg);
		free(cookie);
	}
	#ifdef DEBUG
	else
		fprintf(stderr, "New team created at place %u\n", state.myPlaceId);
	#endif
	#ifdef DEBUG
		fprintf(stderr, "New team callback at place %u complete.\n", state.myPlaceId);
	#endif
}

/*
 * This method is used to create a new team
 */
static void team_create_dispatch (
	    pami_context_t       context,      /**< IN: PAMI context */
	    void               * cookie,       /**< IN: dispatch cookie */
	    const void         * header_addr,  /**< IN: header address */
	    size_t               header_size,  /**< IN: header size */
	    const void         * pipe_addr,    /**< IN: address of PAMI pipe buffer */
	    size_t               pipe_size,    /**< IN: size of PAMI pipe buffer */
	    pami_endpoint_t      origin,
	    pami_recv_t         * recv)        /**< OUT: receive message structure */
{
	uint32_t newTeamId = *((uint32_t*)header_addr);
	if (newTeamId <= state.lastTeamIndex)
		error("Caught an invalid request to put the same place in a team more than once");

	unsigned previousLastTeam = expandTeams(1);
	if (previousLastTeam+1 != newTeamId) error("misalignment detected in team_create_dispatch");

	// save the members of the new team
	state.teams[newTeamId].size = pipe_size/(sizeof(uint32_t));
	state.teams[newTeamId].places = (pami_task_t*)malloc(pipe_size);
	if (state.teams[newTeamId].places == NULL) error("unable to allocate memory for holding the places in team_create_dispatch");
	memcpy(state.teams[newTeamId].places, pipe_addr, pipe_size);

	pami_configuration_t config;
	config.name = PAMI_GEOMETRY_OPTIMIZE;

	#ifdef DEBUG
		fprintf(stderr, "creating a new team %u at place %u of size %u\n", newTeamId, state.myPlaceId, state.teams[newTeamId].size);
	#endif

	pami_result_t   status = PAMI_ERROR;
	status = PAMI_Geometry_create_tasklist(state.client, &config, 1, &state.teams[newTeamId].geometry, state.teams[0].geometry, newTeamId, state.teams[newTeamId].places, pipe_size/(sizeof(uint32_t)), context, team_creation_complete, cookie);
	if (status != PAMI_SUCCESS) error("Unable to create a new team");
}



/** Initialize the X10RT API logical layer.
 *
 * \see #x10rt_lgl_init
 *
 * \param argc As in x10rt_lgl_init.
 *
 * \param argv As in x10rt_lgl_init.
 *
 * \param counter As in x10rt_lgl_init.
 */
void x10rt_net_init (int *argc, char ***argv, x10rt_msg_type *counter)
{
	pami_result_t   status = PAMI_ERROR;
	const char    *name = "X10";
	setenv("MP_MSG_API", name, 1); // workaround for a PAMI issue
	if ((status = PAMI_Client_create(name, &state.client, NULL, 0)) != PAMI_SUCCESS)
		error("Unable to initialize the PAMI client: %i\n", status);

	// determine the level of parallelism we need to support
	char* value = getenv("X10_STATIC_THREADS");
	if (value && !(strcasecmp("false", value) == 0) && !(strcasecmp("0", value) == 0) && !(strcasecmp("f", value) == 0))
	{
		state.numParallelContexts = atoi(getenv("X10_NTHREADS"));
		state.threadMap = (pthread_t *)malloc(state.numParallelContexts*sizeof(pthread_t));
		if (state.threadMap == NULL) error("Unable to allocate memory for the threadMap");
		memset(state.threadMap, 0, state.numParallelContexts*sizeof(pthread_t));
		state.threadMap[0] = pthread_self();
		state.context = (pami_context_t*)malloc(state.numParallelContexts*sizeof(pami_context_t));
		if (state.context == NULL) error("Unable to allocate memory for the context map");
		for (int i=0; i<state.numParallelContexts; i++)
			initializeContext(i);

	}
	else
	{
		state.numParallelContexts = 0;
		state.threadMap = NULL;
		state.context = (pami_context_t*)malloc(sizeof(pami_context_t));
		if (state.context == NULL) error("Unable to allocate memory for the context map");
		initializeContext(0);
	}

	#ifdef DEBUG
		fprintf(stderr, "Initializing contexts for %s worker threads\n", (state.numParallelContexts==0)?"static":"all (non-static)");
	#endif

	pami_configuration_t configuration;
	configuration.name = PAMI_CLIENT_TASK_ID;
	if ((status = PAMI_Client_query(state.client, &configuration, 1)) != PAMI_SUCCESS)
		error("Unable to query the PAMI_CLIENT_TASK_ID: %i\n", status);
	state.myPlaceId = configuration.value.intval;

	configuration.name = PAMI_CLIENT_NUM_TASKS;
	if ((status = PAMI_Client_query(state.client, &configuration, 1)) != PAMI_SUCCESS)
		error("Unable to query PAMI_CLIENT_NUM_TASKS: %i\n", status);
	state.numPlaces = configuration.value.intval;

	#ifdef DEBUG
		fprintf(stderr, "Hello from process %u of %u\n", state.myPlaceId, state.numPlaces); // TODO - deleteme
	#endif
	
	// create the world geometry
	if (pthread_mutex_init(&state.teamLock, NULL) != 0) error("Unable to initialize the team lock");
	state.teams = (x10rt_pami_team*)malloc(sizeof(x10rt_pami_team));
	state.lastTeamIndex = 0;
	state.teams[0].size = state.numPlaces;
	state.teams[0].places = NULL;
	status = PAMI_Geometry_world(state.client, &state.teams[0].geometry);
	if (status != PAMI_SUCCESS) error("Unable to create the world geometry");
}


void x10rt_net_register_msg_receiver (x10rt_msg_type msg_type, x10rt_handler *callback)
{
	// register a pointer to methods that will handle specific message types.
	// add an entry to our type/handler table

	// there are more efficient ways to do this, but this is not in our critical path of execution, so we do it the easy way
	if (msg_type >= state.callBackTableSize)
	{
		state.callBackTable = (x10rtCallback*)realloc(state.callBackTable, sizeof(struct x10rtCallback)*(msg_type+1));
		if (state.callBackTable == NULL) error("Unable to allocate space for the callback table");
		state.callBackTableSize = msg_type+1;
	}

	state.callBackTable[msg_type].handler = callback;
	state.callBackTable[msg_type].finder = NULL;
	state.callBackTable[msg_type].notifier = NULL;

	#ifdef DEBUG
		fprintf(stderr, "Place %u registered standard message handler %u\n", state.myPlaceId, msg_type);
	#endif
}

void x10rt_net_register_put_receiver (x10rt_msg_type msg_type, x10rt_finder *finderCallback, x10rt_notifier *notifierCallback)
{
	// register a pointer to methods that will handle specific message types.
	// add an entry to our type/handler table

	// there are more efficient ways to do this, but this is not in our critical path of execution, so we do it the easy way
	if (msg_type >= state.callBackTableSize)
	{
		state.callBackTable = (x10rtCallback*)realloc(state.callBackTable, sizeof(struct x10rtCallback)*(msg_type+1));
		if (state.callBackTable == NULL) error("Unable to allocate space for the callback table");
	}

	state.callBackTable[msg_type].handler = NULL;
	state.callBackTable[msg_type].finder = finderCallback;
	state.callBackTable[msg_type].notifier = notifierCallback;

	#ifdef DEBUG
		fprintf(stderr, "Place %u registered PUT message handler %u\n", state.myPlaceId, msg_type);
	#endif
}

void x10rt_net_register_get_receiver (x10rt_msg_type msg_type, x10rt_finder *finderCallback, x10rt_notifier *notifierCallback)
{
	// register a pointer to methods that will handle specific message types.
	// add an entry to our type/handler table

	// there are more efficient ways to do this, but this is not in our critical path of execution, so we do it the easy way
	if (msg_type >= state.callBackTableSize)
	{
		state.callBackTable = (x10rtCallback*)realloc(state.callBackTable, sizeof(struct x10rtCallback)*(msg_type+1));
		if (state.callBackTable == NULL) error("Unable to allocate space for the callback table");
	}

	state.callBackTable[msg_type].handler = NULL;
	state.callBackTable[msg_type].finder = finderCallback;
	state.callBackTable[msg_type].notifier = notifierCallback;

	#ifdef DEBUG
		fprintf(stderr, "Place %u registered GET message handler %u\n", state.myPlaceId, msg_type);
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

/** \see #x10rt_lgl_send_msg
 * \param p As in x10rt_lgl_send_msg.
 */
void x10rt_net_send_msg (x10rt_msg_params *p)
{
	pami_endpoint_t target;
	pami_result_t   status = PAMI_ERROR;
	#ifdef DEBUG
		fprintf(stderr, "Preparing to send a message from place %u to %u\n", state.myPlaceId, p->dest_place);
	#endif
	if ((status = PAMI_Endpoint_create(state.client, p->dest_place, 0, &target)) != PAMI_SUCCESS)
		error("Unable to create a target endpoint for sending a message from %u to %u: %i\n", state.myPlaceId, p->dest_place, status);

	volatile unsigned send_active = 1;
	pami_send_t parameters;
	parameters.send.dispatch        = STANDARD;
	parameters.send.header.iov_base = &p->type;
	parameters.send.header.iov_len  = sizeof(p->type);
	parameters.send.data.iov_base   = p->msg;
	parameters.send.data.iov_len    = p->len;
	parameters.send.dest 			= target;
	memset(&parameters.send.hints, 0, sizeof(pami_send_hint_t));
	//parameters.send.hints.buffer_registered = 1;
	parameters.events.cookie        = (void *) &send_active;
	parameters.events.local_fn      = cookie_decrement;
	parameters.events.remote_fn     = NULL;

	pami_context_t context = getContext();
	if ((status = PAMI_Send(context, &parameters)) != PAMI_SUCCESS)
		error("Unable to send a message from %u to %u: %i\n", state.myPlaceId, p->dest_place, status);

	#ifdef DEBUG
		fprintf(stderr, "(%u) send_msg Before advance\n", state.myPlaceId);
	#endif

	// hold up this thread until the message buffers have been copied out by PAMI
	while (send_active)
		x10rt_local_probe(context);
	#ifdef DEBUG
		fprintf(stderr, "(%u) send_msg After advance\n", state.myPlaceId);
	#endif
}

/** \see #x10rt_lgl_send_msg
 * Important: This method returns control back to the caller after the message p has been
 * transmitted out, but BEFORE the data in buf has been transmitted.  Therefore the caller
 * can not delete or modify buf at return time.  It must do this later, through some other
 * callback.
 */
void x10rt_net_send_put (x10rt_msg_params *p, void *buf, x10rt_copy_sz len)
{
	pami_endpoint_t target;
	pami_result_t   status = PAMI_ERROR;
	if ((status = PAMI_Endpoint_create(state.client, p->dest_place, 0, &target)) != PAMI_SUCCESS)
		error("Unable to create a target endpoint for sending a PUT message from %u to %u: %i\n", state.myPlaceId, p->dest_place, status);

	struct x10rt_pami_header_data header;
	header.x10msg.type = p->type;
	header.x10msg.dest_place = p->dest_place;
	header.x10msg.len = p->len;
	header.data_len = len;
	header.data_ptr = buf;

	volatile unsigned put_active = 1;
	pami_send_t parameters;
	parameters.send.dispatch        = PUT;
	parameters.send.header.iov_base = &header;
	parameters.send.header.iov_len  = sizeof(struct x10rt_pami_header_data);
	parameters.send.data.iov_base   = p->msg;
	parameters.send.data.iov_len    = p->len;
	parameters.send.dest 			= target;
	memset(&parameters.send.hints, 0, sizeof(pami_send_hint_t));
	parameters.events.cookie		= (void*)&put_active;
	parameters.events.local_fn		= cookie_decrement;
	parameters.events.remote_fn     = NULL;

	#ifdef DEBUG
		fprintf(stderr, "Preparing to send a PUT message from place %u to %u, type=%i, msglen=%u, len=%u, buf=%p\n", state.myPlaceId, p->dest_place, p->type, p->len, len, buf);
	#endif

	pami_context_t context = getContext();
	if ((status = PAMI_Send(context, &parameters)) != PAMI_SUCCESS)
		error("Unable to send a PUT message from %u to %u: %i\n", state.myPlaceId, p->dest_place, status);

	#ifdef DEBUG
		fprintf(stderr, "(%u) PUT Before advance\n", state.myPlaceId);
	#endif
	// hold up until the message and data has been copied out
	while (put_active)
		x10rt_local_probe(context);
	#ifdef DEBUG
		fprintf(stderr, "(%u) PUT After advance\n", state.myPlaceId);
	#endif
}

/** \see #x10rt_lgl_send_msg
 * \param p As in x10rt_lgl_send_msg.
 * \param buf As in x10rt_lgl_send_msg.
 * \param len As in x10rt_lgl_send_msg.
 */
void x10rt_net_send_get (x10rt_msg_params *p, void *buf, x10rt_copy_sz len)
{
	// GET is implemented as a send msg, followed by a PUT
	pami_endpoint_t target;
	pami_result_t   status = PAMI_ERROR;

	if ((status = PAMI_Endpoint_create(state.client, p->dest_place, 0, &target)) != PAMI_SUCCESS)
		error("Unable to create a target endpoint for sending a GET message from %u to %u: %i\n", state.myPlaceId, p->dest_place, status);

	struct x10rt_pami_header_data* header = (struct x10rt_pami_header_data*)malloc(sizeof(struct x10rt_pami_header_data));
	header->data_len = len;
	header->data_ptr = buf;
	header->x10msg.type = p->type;
	header->x10msg.dest_place = p->dest_place;
	header->x10msg.len = p->len;
	// save the msg data for the notifier
	if (p->len > 0)
	{
		header->x10msg.msg = malloc(p->len);
		if (header->x10msg.msg == NULL)
			error("Unable to malloc msg space for a GET");
		memcpy(header->x10msg.msg, p->msg, p->len);
	}
	else
		header->x10msg.msg = NULL;
	header->callbackPtr = header; // senging this along with the data

	#ifdef DEBUG
		fprintf(stderr, "Preparing to send a GET message from place %u to %u, len=%u, buf=%p, cookie=%p\n", state.myPlaceId, p->dest_place, len, buf, (void*)header);
	#endif

	volatile unsigned get_active = 1;
	pami_send_t parameters;
	parameters.send.dispatch        = GET;
	parameters.send.header.iov_base = header;
	parameters.send.header.iov_len  = sizeof(struct x10rt_pami_header_data);
	parameters.send.data.iov_base   = p->msg;
	parameters.send.data.iov_len    = p->len;
	parameters.send.dest 			= target;
	memset(&parameters.send.hints, 0, sizeof(pami_send_hint_t));
	parameters.events.cookie        = (void*)&get_active;
	parameters.events.local_fn      = cookie_decrement;
	parameters.events.remote_fn     = NULL;

	pami_context_t context = getContext();
	if ((status = PAMI_Send(context, &parameters)) != PAMI_SUCCESS)
		error("Unable to send a GET message from %u to %u: %i\n", state.myPlaceId, p->dest_place, status);

	// hold up until the message has been copied out
	while (get_active)
		x10rt_local_probe(context);
	#ifdef DEBUG
		fprintf(stderr, "GET message sent from place %u to %u, len=%u, buf=%p, cookie=%p\n", state.myPlaceId, p->dest_place, len, buf, (void*)header);
	#endif
}

/** Handle any oustanding message from the network by calling the registered callbacks.  \see #x10rt_lgl_probe
 */
void x10rt_net_probe()
{
	x10rt_local_probe(getContext());
}

/** Shut down the network layer.  \see #x10rt_lgl_finalize
 */
void x10rt_net_finalize()
{
	pami_result_t status = PAMI_ERROR;

	#ifdef DEBUG
		fprintf(stderr, "Place %u shutting down\n", state.myPlaceId);
	#endif

	if (state.numParallelContexts == 0)
	{
		if ((status = PAMI_Context_destroyv(state.context, 1)) != PAMI_SUCCESS)
			fprintf(stderr, "Error closing PAMI context: %i\n", status);
	}
	else
	{
		// TODO - below should be state.numParallelContexts, not state.numParallelContexts-1, but this is a PAMI bug workaround.
		if ((status = PAMI_Context_destroyv(state.context, state.numParallelContexts-1)) != PAMI_SUCCESS)
			fprintf(stderr, "Error closing PAMI context: %i\n", status);
	}
	if ((status = PAMI_Client_destroy(&state.client)) != PAMI_SUCCESS)
		fprintf(stderr, "Error closing PAMI client: %i\n", status);

	// wipe out any leftover teams
	for (unsigned int i=1; i<=state.lastTeamIndex; i++)
		if (state.teams[i].places != NULL)
			free(state.teams[i].places);

	free(state.teams);
}

int x10rt_net_supports (x10rt_opt o)
{
	if (o == X10RT_OPT_COLLECTIVES)
		return 1;
	return 0;
}

void x10rt_net_internal_barrier (){} // DEPRECATED

void x10rt_net_remote_op (x10rt_place place, x10rt_remote_ptr victim, x10rt_op_type type, unsigned long long value)
{
	error("x10rt_net_remote_op not implemented");
}

x10rt_remote_ptr x10rt_net_register_mem (void *ptr, size_t len)
{
	// TODO PAMI_Memregion_create
	// need to call PAMI_Memregion_destroy at shutdown
	error("x10rt_net_register_mem not implemented");
	return NULL;
}

void x10rt_net_team_new (x10rt_place placec, x10rt_place *placev,
                         x10rt_completion_handler2 *ch, void *arg)
{
	pami_result_t status = PAMI_ERROR;

	// This bit of removable code is here to verify that the runtime is NOT requesting a team with the same place in it more than once.
	for (unsigned i=0; i<placec; i++)
		for (unsigned j=i+1; j<placec; j++)
			if (placev[i] == placev[j])
				error("Request to create a team with duplicate members");

	// send the list of team members to all places
	pthread_mutex_lock(&state.teamLock);
	uint32_t newTeamId = state.lastTeamIndex+1;
	pthread_mutex_unlock(&state.teamLock);

	#ifdef DEBUG
		fprintf(stderr, "Place %u preparing to create a new team with %u members\n", state.myPlaceId, placec);
	#endif

	x10rt_pami_team_create *cookie = (x10rt_pami_team_create*)malloc(sizeof(x10rt_pami_team_create));
	cookie->cb2 = ch;
	cookie->arg = arg;
	cookie->teamIndex = newTeamId;

	pami_send_t parameters;
	volatile unsigned send_active = 1;
	parameters.send.dispatch        = NEW_TEAM;
	parameters.send.header.iov_base = &newTeamId;
	parameters.send.header.iov_len  = sizeof(newTeamId);
	parameters.send.data.iov_base   = placev; // team members
	parameters.send.data.iov_len    = placec*sizeof(x10rt_place);
	memset(&parameters.send.hints, 0, sizeof(pami_send_hint_t));
	parameters.events.cookie        = (void *) &send_active;
	parameters.events.local_fn      = cookie_decrement;
	parameters.events.remote_fn     = NULL;

	pami_context_t context = getContext();
	bool inTeam = false;
	for (unsigned i=0; i<placec; i++)
	{
		if (placev[i] != state.myPlaceId)
		{
			if ((status = PAMI_Endpoint_create(state.client, placev[i], 0, &parameters.send.dest)) != PAMI_SUCCESS)
				error("Unable to create an endpoint for team creation");

			if ((status = PAMI_Send(context, &parameters)) != PAMI_SUCCESS)
				error("Unable to send a NEW_TEAM message from %u to %u: %i\n", state.myPlaceId, placev[i], status);

			while (send_active) // hold up until the message has been copied out
				x10rt_local_probe(context);
			#ifdef DEBUG
				fprintf(stderr, "Place %u sent a NEW_TEAM message to place %u\n", state.myPlaceId, placev[i]);
			#endif
			send_active = 1;
		}
		else
			inTeam = true;
	}
	// at this point, all the places that are to be a part of this new team have been sent a message to join it.  We need to join too
	if (!inTeam)
		error("A team was created that did not include the creator");

	team_create_dispatch(context, cookie, &newTeamId, sizeof(newTeamId), placev, placec*sizeof(x10rt_place), (pami_endpoint_t)state.myPlaceId, NULL);
}

static void split_stage2 (pami_context_t   context,
                       void          * cookie,
                       pami_result_t    result)
{
	if (result != PAMI_SUCCESS)
		error("Error detected in split_stage2");

	// at this point, we have completed our all-to-all, and know which members will be in which new teams
	x10rt_pami_team_create *cbd = (x10rt_pami_team_create *)cookie;

	unsigned parentTeamSize = x10rt_net_team_sz(cbd->teamIndex);

	// find how many new teams we're creating
	unsigned numNewTeams = 0;
	unsigned myNewTeamSize = 0;
	for (unsigned i=0; i<parentTeamSize; i++)
	{
		if (cbd->colors[i] > numNewTeams)
			numNewTeams = cbd->colors[i];
		if (cbd->colors[i] == cbd->colors[cbd->parent_role])
			myNewTeamSize++;
	}
	numNewTeams++; // values in colors run from 0 to N

	// save the members of the team that matches my color.  Skip the other teams
	unsigned myNewTeamIndex = expandTeams(numNewTeams)+cbd->colors[cbd->parent_role]+1;
	state.teams[myNewTeamIndex].size = myNewTeamSize;
	state.teams[myNewTeamIndex].places = (pami_task_t*)malloc(myNewTeamSize*sizeof(pami_task_t));
	if (state.teams[myNewTeamIndex].places == NULL) error("Unable to allocate memory to hold the team member list");
	int index = 0;
	for (unsigned i=0; i<parentTeamSize; i++)
	{   // each team member was a member in the parent.  Copy the ID's over, preserving the order.
		if (cbd->colors[i] == cbd->colors[cbd->parent_role]) // if this member is in my new team
		{
			if (cbd->teamIndex == 0) // world geometry doesn't use the places array
				state.teams[myNewTeamIndex].places[index] = i;
			else
				state.teams[myNewTeamIndex].places[index] = state.teams[cbd->teamIndex].places[i];
			index++;
		}
	}
	#ifdef DEBUG
		fprintf(stderr, "Place %u creating new split team %u from team %u, with %u members: %u", state.myPlaceId, myNewTeamIndex, cbd->teamIndex, myNewTeamSize, state.teams[myNewTeamIndex].places[0]);
		for (unsigned i=1; i<myNewTeamSize; i++)
			fprintf(stderr, ",%u", state.teams[myNewTeamIndex].places[i]);
		fprintf(stderr, ".\n");
	#endif

	pami_configuration_t config;
	config.name = PAMI_GEOMETRY_OPTIMIZE;

	pami_result_t   status = PAMI_ERROR;
	pami_geometry_t parentGeometry = state.teams[cbd->teamIndex].geometry;
	cbd->teamIndex = myNewTeamIndex;
	// TODO - interestingly, I need to call getContext() here.  The context that comes in via the method call is null!  Probably a PAMI bug.
	status = PAMI_Geometry_create_tasklist(state.client, &config, 1, &state.teams[myNewTeamIndex].geometry, parentGeometry, myNewTeamIndex, state.teams[myNewTeamIndex].places, myNewTeamSize, getContext(), team_creation_complete, cbd);
	if (status != PAMI_SUCCESS) error("Unable to create a new team");
}

void x10rt_net_team_split (x10rt_team parent, x10rt_place parent_role, x10rt_place color,
		x10rt_place new_role, x10rt_completion_handler2 *ch, void *arg)
{
	// we need to determine how many new teams are getting created (# of colors), and who is in each team.
	// we learn this through an all-to-all

	unsigned parentTeamSize = x10rt_net_team_sz(parent);
	// allocate a buffer to hold the color for each place
	x10rt_place *colors = (x10rt_place*) malloc(sizeof(x10rt_place) * parentTeamSize);
	if (colors == NULL) error("Unable to allocate memory for the team colors buffer");
	colors[parent_role] = color;

	// determine an algorithm for the all-to-all
	pami_result_t status = PAMI_ERROR;
	pami_context_t context = getContext();
	// figure out how many different algorithms are available for the barrier
	size_t num_algorithms[2]; // [0]=always works, and [1]=sometimes works lists
	status = PAMI_Geometry_algorithms_num(context, state.teams[parent].geometry, PAMI_XFER_ALLGATHER, num_algorithms);
	if (status != PAMI_SUCCESS || num_algorithms[0]==0) error("Unable to query the algorithm counts for team %u", parent);
	pami_algorithm_t *always_works_alg = (pami_algorithm_t*)alloca(sizeof(pami_algorithm_t)*num_algorithms[0]);
	pami_metadata_t *always_works_md = (pami_metadata_t*)alloca(sizeof(pami_metadata_t)*num_algorithms[0]);
	pami_algorithm_t *must_query_alg = (pami_algorithm_t*)alloca(sizeof(pami_algorithm_t)*num_algorithms[1]);
	pami_metadata_t *must_query_md = (pami_metadata_t*)alloca(sizeof(pami_metadata_t)*num_algorithms[1]);
	status = PAMI_Geometry_algorithms_query(context, state.teams[parent].geometry, PAMI_XFER_ALLGATHER, always_works_alg,
			always_works_md, num_algorithms[0], must_query_alg, must_query_md, num_algorithms[1]);
	if (status != PAMI_SUCCESS) error("Unable to query the supported algorithms for team %u", parent);

	// select a algorithm, and issue the collective
	x10rt_pami_team_create *cbd = (x10rt_pami_team_create *)malloc(sizeof(x10rt_pami_team_create));
	cbd->cb2 = ch;
	cbd->arg = arg;
	cbd->colors = colors;
	cbd->teamIndex = parent;
	cbd->parent_role = parent_role;

	pami_xfer_t operation;
	operation.cb_done = split_stage2;
	operation.cookie = cbd;
	operation.algorithm = always_works_alg[0];
	operation.cmd.xfer_allgather.rcvbuf = (char*)colors;
	operation.cmd.xfer_allgather.rtype = PAMI_TYPE_CONTIGUOUS;
	operation.cmd.xfer_allgather.rtypecount = sizeof(x10rt_place)*parentTeamSize;
	operation.cmd.xfer_allgather.sndbuf = (char*)&color;
	operation.cmd.xfer_allgather.stype = PAMI_TYPE_CONTIGUOUS;
	operation.cmd.xfer_allgather.stypecount = sizeof(x10rt_place);

	#ifdef DEBUG
		fprintf(stderr, "Splitting team %u\n", parent);
	#endif
	status = PAMI_Collective(context, &operation);
	if (status != PAMI_SUCCESS) error("Unable to issue an all-to-all for team_split");
}

void x10rt_net_team_del (x10rt_team team, x10rt_place role,
                         x10rt_completion_handler *ch, void *arg)
{
	pami_result_t status = PAMI_ERROR;
	status = PAMI_Geometry_destroy(state.client, &state.teams[team].geometry);
	if (status != PAMI_SUCCESS) error("Unable to destroy geometry");
	state.teams[team].size = 0;
	free(state.teams[team].places);
	state.teams[team].places = NULL;
	ch(arg);
}

x10rt_place x10rt_net_team_sz (x10rt_team team)
{
	if (team > state.lastTeamIndex)
		return 0;
	return state.teams[team].size;
}

static void collective_operation_complete (pami_context_t   context,
                       void          * cookie,
                       pami_result_t    result)
{
	if (result != PAMI_SUCCESS)
		error("Error detected in collective_operation_complete");

	x10rt_pami_team_callback *cbd = (x10rt_pami_team_callback*)cookie;
	#ifdef DEBUG
		fprintf(stderr, "Place %u completed collective operation. cookie=%p\n", state.myPlaceId, cookie);
	#endif
	cbd->tcb(cbd->arg);
	free(cookie);
}

void x10rt_net_barrier (x10rt_team team, x10rt_place role, x10rt_completion_handler *ch, void *arg)
{
	pami_result_t status = PAMI_ERROR;
	pami_context_t context = getContext();
	// figure out how many different algorithms are available for the barrier
	size_t num_algorithms[2]; // [0]=always works, and [1]=sometimes works lists
	status = PAMI_Geometry_algorithms_num(context, state.teams[team].geometry, PAMI_XFER_BARRIER, num_algorithms);
	if (status != PAMI_SUCCESS || num_algorithms[0]==0) error("Unable to query the algorithm counts for team %u", team);

	// query what the different algorithms are
	pami_algorithm_t *always_works_alg = (pami_algorithm_t*)alloca(sizeof(pami_algorithm_t)*num_algorithms[0]);
	pami_metadata_t *always_works_md = (pami_metadata_t*)alloca(sizeof(pami_metadata_t)*num_algorithms[0]);
	pami_algorithm_t *must_query_alg = (pami_algorithm_t*)alloca(sizeof(pami_algorithm_t)*num_algorithms[1]);
	pami_metadata_t *must_query_md = (pami_metadata_t*)alloca(sizeof(pami_metadata_t)*num_algorithms[1]);
	status = PAMI_Geometry_algorithms_query(context, state.teams[team].geometry, PAMI_XFER_BARRIER, always_works_alg,
			always_works_md, num_algorithms[0], must_query_alg, must_query_md, num_algorithms[1]);
	if (status != PAMI_SUCCESS) error("Unable to query the supported algorithms for team %u", team);

	// select a algorithm, and issue the collective
	x10rt_pami_team_callback *tcb = (x10rt_pami_team_callback *)malloc(sizeof(x10rt_pami_team_callback));
	tcb->tcb = ch;
	tcb->arg = arg;
	memset(&tcb->operation, 0, sizeof (tcb->operation));
	tcb->operation.cb_done = collective_operation_complete;
	tcb->operation.cookie = tcb;
	// TODO - figure out a better way to choose.  For now, the code just uses the first *known good* algorithm.
	#ifdef DEBUG
		if ((team==0 && state.myPlaceId==0) || (team>0 && state.myPlaceId == state.teams[team].places[0]))
		{
			fprintf(stderr, "Barrier algorithms are always %s", always_works_md[0].name);
			for (size_t i=1; i<num_algorithms[0]; i++)
				fprintf(stderr, ", %s", always_works_md[i].name);
			if (num_algorithms[1] > 0)
			{
				fprintf(stderr, " and sometimes %s", must_query_md[0].name);
				for (size_t i=1; i<num_algorithms[1]; i++)
					fprintf(stderr, ", %s", must_query_md[i].name);
			}
			fprintf(stderr, ".\n");
		}
		fprintf(stderr, "Place %u, role %u executing barrier (%s). cookie=%p\n", state.myPlaceId, role, always_works_md[0].name, (void*)tcb);
	#endif
	tcb->operation.algorithm = always_works_alg[0];

	status = PAMI_Collective(context, &tcb->operation);
	if (status != PAMI_SUCCESS) error("Unable to issue a barrier on team %u", team);
}

void x10rt_net_bcast (x10rt_team team, x10rt_place role, x10rt_place root, const void *sbuf,
		void *dbuf, size_t el, size_t count, x10rt_completion_handler *ch, void *arg)
{
	pami_result_t status = PAMI_ERROR;
	pami_context_t context = getContext();

	#ifdef DEBUG
		fprintf(stderr, "Place %u executing broadcast of %lu %lu-byte elements on team %u, with role=%u, root=%u\n", state.myPlaceId, count, el, team, role, root);
	#endif

	// figure out how many different algorithms are available for the barrier
	size_t num_algorithms[2]; // [0]=always works, and [1]=sometimes works lists
	status = PAMI_Geometry_algorithms_num(context, state.teams[team].geometry, PAMI_XFER_BROADCAST, num_algorithms);
	if (status != PAMI_SUCCESS || num_algorithms[0]==0) error("Unable to query the algorithm counts for team %u", team);

	// query what the different algorithms are
	pami_algorithm_t *always_works_alg = (pami_algorithm_t*)alloca(sizeof(pami_algorithm_t)*num_algorithms[0]);
	pami_metadata_t *always_works_md = (pami_metadata_t*)alloca(sizeof(pami_metadata_t)*num_algorithms[0]);
	pami_algorithm_t *must_query_alg = (pami_algorithm_t*)alloca(sizeof(pami_algorithm_t)*num_algorithms[1]);
	pami_metadata_t *must_query_md = (pami_metadata_t*)alloca(sizeof(pami_metadata_t)*num_algorithms[1]);
	status = PAMI_Geometry_algorithms_query(context, state.teams[team].geometry, PAMI_XFER_BROADCAST, always_works_alg,
			always_works_md, num_algorithms[0], must_query_alg, must_query_md, num_algorithms[1]);
	if (status != PAMI_SUCCESS) error("Unable to query the supported algorithms for team %u", team);

	// select a algorithm, and issue the collective
	x10rt_pami_team_callback *tcb = (x10rt_pami_team_callback *)malloc(sizeof(x10rt_pami_team_callback));
	tcb->tcb = ch;
	tcb->arg = arg;
	memset(&tcb->operation, 0, sizeof (tcb->operation));
	tcb->operation.cb_done = collective_operation_complete;
	tcb->operation.cookie = tcb;
	// TODO - figure out a better way to choose.  For now, the code just uses the first *known good* algorithm.
	#ifdef DEBUG
		if (role==root)
		{
			fprintf(stderr, "Broadcast algorithms are always %s", always_works_md[0].name);
			for (size_t i=1; i<num_algorithms[0]; i++)
				fprintf(stderr, ", %s", always_works_md[i].name);
			if (num_algorithms[1] > 0)
			{
				fprintf(stderr, " and sometimes %s", must_query_md[0].name);
				for (size_t i=1; i<num_algorithms[1]; i++)
					fprintf(stderr, ", %s", must_query_md[i].name);
			}
			fprintf(stderr, ".\n");
		}
		fprintf(stderr, "Place %u, role %u executing broadcast (%s). cookie=%p\n", state.myPlaceId, role, always_works_md[0].name, (void*)tcb);
	#endif

	tcb->operation.algorithm = always_works_alg[0];
	tcb->operation.cmd.xfer_broadcast.type = PAMI_TYPE_CONTIGUOUS;
	tcb->operation.cmd.xfer_broadcast.typecount = count*el;
	if (team == 0)
		tcb->operation.cmd.xfer_broadcast.root = root;
	else
		tcb->operation.cmd.xfer_broadcast.root = state.teams[team].places[root];

	if (role == root)
		tcb->operation.cmd.xfer_broadcast.buf = (char*)sbuf;
	else
		tcb->operation.cmd.xfer_broadcast.buf = (char*)dbuf;

	status = PAMI_Collective(context, &tcb->operation);
	if (status != PAMI_SUCCESS) error("Unable to issue a broadcast on team %u", team);

	// copy the data for the root separately
	if (role == root)
		memcpy(dbuf, sbuf, count*el);
}

void x10rt_net_scatter (x10rt_team team, x10rt_place role, x10rt_place root, const void *sbuf,
		void *dbuf, size_t el, size_t count, x10rt_completion_handler *ch, void *arg)
{
	pami_result_t status = PAMI_ERROR;
	pami_context_t context = getContext();

	// figure out how many different algorithms are available for the barrier
	size_t num_algorithms[2]; // [0]=always works, and [1]=sometimes works lists
	status = PAMI_Geometry_algorithms_num(context, state.teams[team].geometry, PAMI_XFER_SCATTER, num_algorithms);
	if (status != PAMI_SUCCESS || num_algorithms[0]==0) error("Unable to query the algorithm counts for team %u", team);

	// query what the different algorithms are
	pami_algorithm_t *always_works_alg = (pami_algorithm_t*)alloca(sizeof(pami_algorithm_t)*num_algorithms[0]);
	pami_metadata_t *always_works_md = (pami_metadata_t*)alloca(sizeof(pami_metadata_t)*num_algorithms[0]);
	pami_algorithm_t *must_query_alg = (pami_algorithm_t*)alloca(sizeof(pami_algorithm_t)*num_algorithms[1]);
	pami_metadata_t *must_query_md = (pami_metadata_t*)alloca(sizeof(pami_metadata_t)*num_algorithms[1]);
	status = PAMI_Geometry_algorithms_query(context, state.teams[team].geometry, PAMI_XFER_SCATTER, always_works_alg,
			always_works_md, num_algorithms[0], must_query_alg, must_query_md, num_algorithms[1]);
	if (status != PAMI_SUCCESS) error("Unable to query the supported algorithms for team %u", team);

	// select a algorithm, and issue the collective
	x10rt_pami_team_callback *tcb = (x10rt_pami_team_callback *)malloc(sizeof(x10rt_pami_team_callback));
	tcb->tcb = ch;
	tcb->arg = arg;
	memset(&tcb->operation, 0, sizeof (tcb->operation));
	tcb->operation.cb_done = collective_operation_complete;
	tcb->operation.cookie = tcb;
	// TODO - figure out a better way to choose.  For now, the code just uses the first *known good* algorithm.
	tcb->operation.algorithm = always_works_alg[0];
	tcb->operation.cmd.xfer_scatter.rcvbuf = (char*)dbuf;
	if (team == 0)
		tcb->operation.cmd.xfer_scatter.root = root;
	else
		tcb->operation.cmd.xfer_scatter.root = state.teams[team].places[root];
	tcb->operation.cmd.xfer_scatter.rtype = PAMI_TYPE_CONTIGUOUS;
	tcb->operation.cmd.xfer_scatter.rtypecount = el*count;
	tcb->operation.cmd.xfer_scatter.sndbuf = (char*)sbuf;
	tcb->operation.cmd.xfer_scatter.stype = PAMI_TYPE_CONTIGUOUS;
	tcb->operation.cmd.xfer_scatter.stypecount = el*count;

	#ifdef DEBUG
		fprintf(stderr, "Place %u executing scatter (%s): role=%u, root=%u\n", state.myPlaceId, always_works_md[0].name, role, root);
	#endif
	status = PAMI_Collective(context, &tcb->operation);
	if (status != PAMI_SUCCESS) error("Unable to issue a scatter on team %u", team);

	// copy the root data from src to dst locally
	if (role == root)
	{
		int blockSize = el*count;
		memcpy(((char*)dbuf)+(blockSize*role), ((char*)sbuf)+(blockSize*role), blockSize);
	}
}

void x10rt_net_alltoall (x10rt_team team, x10rt_place role, const void *sbuf, void *dbuf,
		size_t el, size_t count, x10rt_completion_handler *ch, void *arg)
{
	pami_result_t status = PAMI_ERROR;
	pami_context_t context = getContext();

	// figure out how many different algorithms are available for the barrier
	size_t num_algorithms[2]; // [0]=always works, and [1]=sometimes works lists
	status = PAMI_Geometry_algorithms_num(context, state.teams[team].geometry, PAMI_XFER_ALLTOALL, num_algorithms);
	if (status != PAMI_SUCCESS || num_algorithms[0]==0) error("Unable to query the algorithm counts for team %u", team);

	// query what the different algorithms are
	pami_algorithm_t *always_works_alg = (pami_algorithm_t*)alloca(sizeof(pami_algorithm_t)*num_algorithms[0]);
	pami_metadata_t *always_works_md = (pami_metadata_t*)alloca(sizeof(pami_metadata_t)*num_algorithms[0]);
	pami_algorithm_t *must_query_alg = (pami_algorithm_t*)alloca(sizeof(pami_algorithm_t)*num_algorithms[1]);
	pami_metadata_t *must_query_md = (pami_metadata_t*)alloca(sizeof(pami_metadata_t)*num_algorithms[1]);
	status = PAMI_Geometry_algorithms_query(context, state.teams[team].geometry, PAMI_XFER_ALLTOALL, always_works_alg,
			always_works_md, num_algorithms[0], must_query_alg, must_query_md, num_algorithms[1]);
	if (status != PAMI_SUCCESS) error("Unable to query the supported algorithms for team %u", team);

	// select a algorithm, and issue the collective
	x10rt_pami_team_callback *tcb = (x10rt_pami_team_callback *)malloc(sizeof(x10rt_pami_team_callback));
	if (tcb == NULL) error("Unable to allocate memory for the all-to-all cookie");
	tcb->tcb = ch;
	tcb->arg = arg;
	memset(&tcb->operation, 0, sizeof (tcb->operation));
	tcb->operation.cb_done = collective_operation_complete;
	tcb->operation.cookie = tcb;
	// TODO - figure out a better way to choose.  For now, the code just uses the first *known good* algorithm.
	tcb->operation.algorithm = always_works_alg[1]; // TODO: Algorithm "I0:Pairwise:P2P:P2P" is buggy.  Using "I0:M2MComposite:P2P:P2P" instead
	tcb->operation.cmd.xfer_alltoall.rcvbuf = (char*)dbuf;
	tcb->operation.cmd.xfer_alltoall.rtype = PAMI_TYPE_CONTIGUOUS;
	tcb->operation.cmd.xfer_alltoall.rtypecount = el*count;
	tcb->operation.cmd.xfer_alltoall.sndbuf = (char*)sbuf;
	tcb->operation.cmd.xfer_alltoall.stype = PAMI_TYPE_CONTIGUOUS;
	tcb->operation.cmd.xfer_alltoall.stypecount = el*count;

	#ifdef DEBUG
		if (role==0)
		{
			fprintf(stderr, "AllToAll algorithms are always %s", always_works_md[0].name);
			for (size_t i=1; i<num_algorithms[0]; i++)
				fprintf(stderr, ", %s", always_works_md[i].name);
			if (num_algorithms[1] > 0)
			{
				fprintf(stderr, " and sometimes %s", must_query_md[0].name);
				for (size_t i=1; i<num_algorithms[1]; i++)
					fprintf(stderr, ", %s", must_query_md[i].name);
			}
			fprintf(stderr, ".\n");
		}
		fprintf(stderr, "Place %u, role %u executing AllToAll (%s) with team %u. cookie=%p\n", state.myPlaceId, role, always_works_md[1].name, team, (void*)tcb);
	#endif
	status = PAMI_Collective(context, &tcb->operation);
	if (status != PAMI_SUCCESS) error("Unable to issue an all-to-all on team %u", team);

	// copy the local section of data from src to dst
	int blockSize = el*count;
	memcpy(((char*)dbuf)+(blockSize*role), ((char*)sbuf)+(blockSize*role), blockSize);
}

void x10rt_net_allreduce (x10rt_team team, x10rt_place role, const void *sbuf, void *dbuf,
		x10rt_red_op_type op, x10rt_red_type dtype, size_t count, x10rt_completion_handler *ch, void *arg)
{
	pami_result_t status = PAMI_ERROR;
	pami_context_t context = getContext();

	// figure out how many different algorithms are available for the barrier
	size_t num_algorithms[2]; // [0]=always works, and [1]=sometimes works lists
	status = PAMI_Geometry_algorithms_num(context, state.teams[team].geometry, PAMI_XFER_ALLREDUCE, num_algorithms);
	if (status != PAMI_SUCCESS || num_algorithms[0]==0) error("Unable to query the algorithm counts for team %u", team);

	// query what the different algorithms are
	pami_algorithm_t *always_works_alg = (pami_algorithm_t*)alloca(sizeof(pami_algorithm_t)*num_algorithms[0]);
	pami_metadata_t *always_works_md = (pami_metadata_t*)alloca(sizeof(pami_metadata_t)*num_algorithms[0]);
	pami_algorithm_t *must_query_alg = (pami_algorithm_t*)alloca(sizeof(pami_algorithm_t)*num_algorithms[1]);
	pami_metadata_t *must_query_md = (pami_metadata_t*)alloca(sizeof(pami_metadata_t)*num_algorithms[1]);
	status = PAMI_Geometry_algorithms_query(context, state.teams[team].geometry, PAMI_XFER_ALLREDUCE, always_works_alg,
			always_works_md, num_algorithms[0], must_query_alg, must_query_md, num_algorithms[1]);
	if (status != PAMI_SUCCESS) error("Unable to query the supported algorithms for team %u", team);

	// select a algorithm, and issue the collective
	x10rt_pami_team_callback *tcb = (x10rt_pami_team_callback *)malloc(sizeof(x10rt_pami_team_callback));
	tcb->tcb = ch;
	tcb->arg = arg;
	memset(&tcb->operation, 0, sizeof (tcb->operation));
	tcb->operation.cb_done = collective_operation_complete;
	tcb->operation.cookie = tcb;
	// TODO - figure out a better way to choose.  For now, the code just uses the first *known good* algorithm.
	tcb->operation.algorithm = always_works_alg[0];
	tcb->operation.cmd.xfer_allreduce.dt = DATATYPE_CONVERSION_TABLE[dtype];
	if (DATATYPE_CONVERSION_TABLE[dtype] >= PAMI_LOC_2INT)
	{   // operations on LOC datatypes are different from regular types
		if (OPERATION_CONVERSION_TABLE[op] == PAMI_MAX)
			tcb->operation.cmd.xfer_allreduce.op = PAMI_MAXLOC;
		else if (OPERATION_CONVERSION_TABLE[op] == PAMI_MIN)
			tcb->operation.cmd.xfer_allreduce.op = PAMI_MINLOC;
	}
	else
		tcb->operation.cmd.xfer_allreduce.op = OPERATION_CONVERSION_TABLE[op];
	tcb->operation.cmd.xfer_allreduce.rcvbuf = (char*)dbuf;
	tcb->operation.cmd.xfer_allreduce.rtype = PAMI_TYPE_CONTIGUOUS;
	tcb->operation.cmd.xfer_allreduce.rtypecount = count*DATATYPE_MULTIPLIER_TABLE[dtype];
	tcb->operation.cmd.xfer_allreduce.sndbuf = (char*)sbuf;
	tcb->operation.cmd.xfer_allreduce.stype = PAMI_TYPE_CONTIGUOUS;
	tcb->operation.cmd.xfer_allreduce.stypecount = count*DATATYPE_MULTIPLIER_TABLE[dtype];

	#ifdef DEBUG
		fprintf(stderr, "Place %u executing allreduce (%s), with type=%u and op=%u\n", state.myPlaceId, always_works_md[0].name, dtype, op);
	#endif
	status = PAMI_Collective(context, &tcb->operation);
	if (status != PAMI_SUCCESS) error("Unable to issue an allreduce on team %u", team);
}
// vim: tabstop=4:shiftwidth=4:expandtab:textwidth=100
