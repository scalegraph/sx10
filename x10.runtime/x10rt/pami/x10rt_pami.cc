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

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <errno.h> // for the strerror function
#include <sched.h> // for sched_yield()
#include <x10rt_net.h>
#include <pami.h>

#define DEBUG 1

//mechanisms for the callback functions used in the register and probe methods
typedef void (*handlerCallback)(const x10rt_msg_params *);
typedef void *(*finderCallback)(const x10rt_msg_params *, x10rt_copy_sz);
typedef void (*notifierCallback)(const x10rt_msg_params *, x10rt_copy_sz);

struct x10rtCallback
{
	handlerCallback handler;
	finderCallback finder;
	notifierCallback notifier;
};

struct x10PAMIState
{
	uint32_t numPlaces;
	uint32_t myPlaceId;
	x10rtCallback* callBackTable;
	x10rt_msg_type callBackTableSize;
	pami_client_t client; // the PAMI client instance used for this place
	// TODO associate a context with each worker thread
	pami_context_t context[1]; // PAMI context associated with the client (currently only 1 context is used)
	pami_send_hint_t standardHints; // hints that apply to this session
	volatile unsigned send_active;
	volatile unsigned recv_active;
	volatile unsigned recv_iteration;
} state;


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
	fflush(stdout);
	sched_yield();
	exit(1);
}

/*
 * These methods are used to convert from a PAMI callback to an X10 callback
 */
static void pami_decrement (pami_context_t   context,
                       void          * cookie,
                       pami_result_t    result)
{
	unsigned * value = (unsigned *) cookie;
	#ifdef DEBUG
		printf("(%zu) decrement() cookie = %p, %d => %d\n", state.myPlaceId, cookie, *value, *value-1);
	#endif
	--*value;
}

char _recv_buffer[262144] __attribute__ ((__aligned__(16))); // TODO: temporary, replace me

static void local_dispatch (
	    pami_context_t        context,      /**< IN: PAMI context */
	    void               * cookie,       /**< IN: dispatch cookie */
	    const void         * header_addr,  /**< IN: header address */
	    size_t               header_size,  /**< IN: header size */
	    const void         * pipe_addr,    /**< IN: address of PAMI pipe buffer */
	    size_t               pipe_size,    /**< IN: size of PAMI pipe buffer */
	    pami_endpoint_t      origin,
	    pami_recv_t         * recv)        /**< OUT: receive message structure */
{
	if (recv)
	{
		#ifdef DEBUG
			printf("(%zu) test_dispatch() async recv:  cookie = %p, pipe_size = %zu\n", state.myPlaceId, cookie, pipe_size);
		#endif

		recv->local_fn = pami_decrement;
		recv->cookie   = cookie;
		recv->type     = PAMI_BYTE;
		recv->addr     = _recv_buffer;
		recv->offset   = 0;
		recv->data_fn  = PAMI_DATA_COPY;
	}
	else
	{
		//memcpy (_recv_buffer, pipe_addr, pipe_size);
		unsigned * value = (unsigned *) cookie;
		#ifdef DEBUG
			printf("(%zu) test_dispatch() short recv:  cookie = %p, decrement: %d => %d\n", state.myPlaceId, cookie, *value, *value-1);
		#endif
		--*value;
	}
	state.recv_iteration++;

/*
	volatile size_t * active = (volatile size_t *) cookie;
	(*active)--;

	x10rt_msg_params* x10Header = (x10rt_msg_params*)header;

	handlerCallback hcb = state.callBackTable[x10Header->type].handler;
	hcb(x10Header);
*/
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

	size_t ncontext = 1;
	if ((status = PAMI_Context_createv(state.client, NULL, 0, &state.context[0], ncontext)) != PAMI_SUCCESS)
		error("Unable to initialize the PAMI context: %i\n", status);

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
		printf("Hello from process %u of %u\n", state.myPlaceId, state.numPlaces); // TODO - deleteme
	#endif
	
	memset(&state.standardHints, 0, sizeof(state.standardHints));
	state.send_active = 1;
	state.recv_active = 1;
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

	// register the ID with PAMI
	pami_result_t   status = PAMI_ERROR;
	pami_dispatch_callback_function fn;
	fn.p2p = local_dispatch;
	#ifdef DEBUG
		printf("Before PAMI_Dispatch_set() .. &recv_active = %p, recv_active = %zu\n", &state.recv_active, state.recv_active);
	#endif
	if ((status = PAMI_Dispatch_set(state.context[0], msg_type, fn, (void *) &state.recv_active, state.standardHints)) != PAMI_SUCCESS)
		error("Unable to register message %u", msg_type);

	#ifdef DEBUG
		printf("Place %lu registered standard message %u\n", state.myPlaceId, msg_type);
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

	// TODO register the ID with PAMI
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

	// TODO register the ID with PAMI
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
	pami_endpoint_t origin, target;
	pami_result_t   status = PAMI_ERROR;
	#ifdef DEBUG
		printf("Preparing to send a message from place %lu to %lu\n", state.myPlaceId, p->dest_place);
	#endif
	if ((status = PAMI_Endpoint_create(state.client, state.myPlaceId, 0, &origin)) != PAMI_SUCCESS)
		error("Unable to create an origin endpoint for sending a message from %u to %u: %i\n", state.myPlaceId, p->dest_place, status);

	if ((status = PAMI_Endpoint_create(state.client, p->dest_place, 0, &target)) != PAMI_SUCCESS)
		error("Unable to create a target endpoint for sending a message from %u to %u: %i\n", state.myPlaceId, p->dest_place, status);

	// TODO change to pami_send_immediate for small stuff?
	pami_send_t parameters;
	parameters.send.dispatch        = p->type;
	parameters.send.header.iov_base = p->msg;
	parameters.send.header.iov_len  = p->len;
	parameters.send.data.iov_base   = NULL;
	parameters.send.data.iov_len    = 0;
	parameters.send.dest 			= target;
	parameters.events.cookie        = (void *) &state.send_active;
	parameters.events.local_fn      = pami_decrement;
	parameters.events.remote_fn     = NULL;

	if ((status = PAMI_Send(state.context[0], &parameters)) != PAMI_SUCCESS)
		error("Unable to send a message from %u to %u: %i\n", state.myPlaceId, p->dest_place, status);

	#ifdef DEBUG
		printf("(%zu) send_once() Before advance\n", state.myPlaceId);
	#endif
	while (state.send_active)
		PAMI_Context_advance(state.context[0], 100);
	state.send_active = 1;
	#ifdef DEBUG
		printf("(%zu) send_once() After advance\n", state.myPlaceId);
	#endif
}

/** \see #x10rt_lgl_send_msg
 * \param p As in x10rt_lgl_send_msg.
 * \param buf As in x10rt_lgl_send_msg.
 * \param len As in x10rt_lgl_send_msg.
 */
void x10rt_net_send_get (x10rt_msg_params *p, void *buf, x10rt_copy_sz len)
{
	// TODO PAMI_Get
	error("Get not implemented");
}

/** \see #x10rt_lgl_send_msg
 * \param p As in x10rt_lgl_send_msg.
 * \param buf As in x10rt_lgl_send_msg.
 * \param len As in x10rt_lgl_send_msg.
 */
void x10rt_net_send_put (x10rt_msg_params *p, void *buf, x10rt_copy_sz len)
{
	// TODO PAMI_Put
	error("Put not implemented");
}

/** Handle any oustanding message from the network by calling the registered callbacks.  \see #x10rt_lgl_probe
 */
void x10rt_net_probe()
{
	#ifdef DEBUG
		printf("Place %lu trying a probe\n", state.myPlaceId);
	#endif
	pami_result_t status = PAMI_ERROR;
	// TODO remove this lock when we move to endpoints, or when X10_NTHREADS=1
	if ((status = PAMI_Context_lock(&state.context[0])) != PAMI_SUCCESS)
		error("Unable to lock context");

	#ifdef DEBUG
		printf("Place %lu advancing context\n", state.myPlaceId);
	#endif
	while (state.recv_active)
		PAMI_Context_advance (state.context[0], 100);

	state.recv_active = 1;

/*	status = PAMI_Context_advance(&state.context[0], 1);
	if (status == PAMI_EAGAIN)
	{
		if ((status = PAMI_Context_unlock(&state.context[0])) != PAMI_SUCCESS)
			error("Unable to unlock context");
		sched_yield();
	}
	else */
	if ((status = PAMI_Context_unlock(&state.context[0])) != PAMI_SUCCESS)
		error("Unable to unlock context");
}

/** Shut down the network layer.  \see #x10rt_lgl_finalize
 */
void x10rt_net_finalize()
{
	pami_result_t status = PAMI_ERROR;

	if ((status = PAMI_Context_destroyv(&state.context[0], 1)) != PAMI_SUCCESS)
		fprintf(stderr, "Error closing PAMI context: %i\n", status);

	if ((status = PAMI_Client_destroy(&state.client)) != PAMI_SUCCESS)
		fprintf(stderr, "Error closing PAMI client: %i\n", status);
	#ifdef DEBUG
		printf("Place %lu shut down\n", state.myPlaceId);
	#endif
}

int x10rt_net_supports (x10rt_opt o)
{
    return 0;
}


void x10rt_net_internal_barrier (){} // DEPRECATED

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
// vim: tabstop=4:shiftwidth=4:expandtab:textwidth=100
