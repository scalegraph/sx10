#ifndef X10RT_LOGICAL_H
#define X10RT_LOGICAL_H

#include <cstdlib>

#include <x10rt_types.h>

/** \file
 * Logical Layer.  \see \ref structure
 *
 * The API differences between the Logical Layer and the frontend of the API are as follows:
 *
 * \li Internal message types are determined by incrementing a counter, a pointer to which is
 * provided at initialization time.  All parts of the system will use this counter in order to
 * allocate message ids from the same 'pool'.
 *
 * \li In the callback registration functions, the message type is provided by the caller.  The
 * caller can tell what messages ids were reserved internally by examining the counter object that
 * was given to the Logical Layer at initialisation time, as described above.
 *
 * \li CUDA kernel processing callbacks and normal host callbacks are registered using separate
 * functions.  Note that it is possible to register both normal and CUDA callbacks for the same
 * message type.  This is because the message can be sent to either of the two places and thus
 * callbacks should be provided for both (if required).
 */

/** The kinds of places that are supported. */
enum x10rt_lgl_cat {
/** A host on the network. */
  X10RT_LGL_HOST,
/** An SPE within a CELL. */
  X10RT_LGL_SPE,
/** A CUDA-capable GPU. */
  X10RT_LGL_CUDA
};

/** A structure that is used to specify a child accelerator.
 */
struct x10rt_lgl_cfg_accel {
    /** The kind of accelerator. */
    x10rt_lgl_cat cat;
    /** The identity of the hardware within the system (e.g. device id, SPE id, etc.). */
    unsigned index;
};

/** Initialize the X10RT API logical layer.  This versions uses the X10RT_ACCELS environment
 * variable.
 *
 * \see #x10rt_lgl_init_ex
 *
 * \param argc As in x10rt_init.
 *
 * \param argv As in x10rt_init.
 *
 * \param counter A counter that is used to find the next available message type for any internal
 * message types needed by the various backends.
*/
X10RT_C void x10rt_lgl_init (int *argc, char ***argv, x10rt_msg_type *counter);

/** Initialize the X10RT API logical layer (alternate extended version).  This version configures
 * the accelerators using an explicit list instead of reading the X10RT_ACCELS environment variable.
 *
 * \see #x10rt_lgl_init
 *
 * \param argc As in x10rt_init.
 *
 * \param argv As in x10rt_init.
 *
 * \param cfgv An array of #x10rt_lgl_cfg_accel values used to configure this host.
 *
 * \param cfgc The number of elements in cfgv
 *
 * \param counter As in x10rt_lgl_init.
*/

X10RT_C void x10rt_lgl_init_ex (int *argc, char ***argv, x10rt_lgl_cfg_accel *cfgv,
                                x10rt_place cfgc, x10rt_msg_type *counter);

/** Register handlers for a plain message.
 *
 * \see #x10rt_register_msg_receiver
 *
 * \param msg_type The type of the message to register callbacks for.
 *
 * \param cb As in x10rt_register_msg_receiver
 */
X10RT_C void x10rt_lgl_register_msg_receiver (x10rt_msg_type msg_type, x10rt_handler *cb);

/** Register handlers for a get message.
 *
 * \see #x10rt_register_get_receiver
 *
 * \param msg_type The type of the message to register callbacks for.
 *
 * \param cb1 As in x10rt_register_get_receiver
 *
 * \param cb2 As in x10rt_register_get_receiver
 */
X10RT_C void x10rt_lgl_register_get_receiver (x10rt_msg_type msg_type,
                                              x10rt_finder *cb1, x10rt_notifier *cb2);

/** Register handlers for a put message.
 *
 * \see #x10rt_register_put_receiver
 *
 * \param msg_type The type of the message to register callbacks for.
 *
 * \param cb1 As in x10rt_register_put_receiver
 *
 * \param cb2 As in x10rt_register_put_receiver
 */

X10RT_C void x10rt_lgl_register_put_receiver (x10rt_msg_type msg_type,
                                              x10rt_finder *cb1, x10rt_notifier *cb2);

/** Register handlers for a CUDA plain message (kernel invocation).
 *
 * \see #x10rt_register_msg_receiver
 *
 * \param msg_type The type of the message to register callbacks for.
 *
 * \param pre As in x10rt_register_msg_receiver
 * \param post As in x10rt_register_msg_receiver
 * \param cubin As in x10rt_register_msg_receiver
 * \param kernel_name As in x10rt_register_msg_receiver
 */
X10RT_C void x10rt_lgl_register_msg_receiver_cuda (x10rt_msg_type msg_type,
                                                   x10rt_cuda_pre *pre, x10rt_cuda_post *post,
                                                   const char *cubin, const char *kernel_name);

/** Register handlers for a CUDA get message.
 *
 * \see #x10rt_register_get_receiver
 *
 * \param msg_type The type of the message to register callbacks for.
 *
 * \param cb1 As in x10rt_register_put_receiver
 *
 * \param cb2 As in x10rt_register_put_receiver
 */

X10RT_C void x10rt_lgl_register_get_receiver_cuda (x10rt_msg_type msg_type,
                                                   x10rt_finder *cb1, x10rt_notifier *cb2);

/** Register handlers for a CUDA put message.
 *
 * \see #x10rt_register_put_receiver
 *
 * \param msg_type The type of the message to register callbacks for.
 *
 * \param cb1 As in x10rt_register_put_receiver
 *
 * \param cb2 As in x10rt_register_put_receiver
 */

X10RT_C void x10rt_lgl_register_put_receiver_cuda (x10rt_msg_type msg_type,
                                                   x10rt_finder *cb1, x10rt_notifier *cb2);

/** An SPMD barrier that can only be used when each place has exactly one thread (the main thread).
 * Only the hosts are synchronized by this call (i.e. not accelerators).  This is used internally by
 * #x10rt_lgl_init and #x10rt_lgl_init_ex.  It is also used to implement
 * #x10rt_registration_complete.  It calls #x10rt_net_probe internally. \bug This should be
 * non-blocking instead of calling #x10rt_net_probe.
 */
X10RT_C void x10rt_lgl_registration_complete (void);

/** \see #x10rt_nplaces */
x10rt_place x10rt_lgl_nplaces (void);

/** \see #x10rt_nhosts */
x10rt_place x10rt_lgl_nhosts (void);

/** \see #x10rt_here */
x10rt_place x10rt_lgl_here (void);

/** Find out about a place.
 * \param place The place in question.
 * \returns The kind of place it is.
 */
x10rt_lgl_cat x10rt_lgl_type (x10rt_place place);

/** \see #x10rt_parent
 * \param place The place in question.
 */
x10rt_place x10rt_lgl_parent (x10rt_place place);

/** \see #x10rt_nchildren
 * \param place The place in question.
 */
x10rt_place x10rt_lgl_nchildren (x10rt_place place);

/** \see #x10rt_child
 * \param host As in x10rt_child
 * \param index As in x10rt_child
 */
x10rt_place x10rt_lgl_child (x10rt_place host, x10rt_place index);

/** \see #x10rt_child_index
 * \param child As in x10rt_child_index
 */
x10rt_place x10rt_lgl_child_index (x10rt_place child);


/** \see #x10rt_send_msg
 * \param p As in x10rt_send_msg
 */
X10RT_C void x10rt_lgl_send_msg (x10rt_msg_params *p);


/** \see #x10rt_send_get
 * \param p As in x10rt_send_get
 * \param buf As in x10rt_send_get
 * \param len As in x10rt_send_get
 */
X10RT_C void x10rt_lgl_send_get (x10rt_msg_params *p, void *buf, x10rt_copy_sz len);


/** \see #x10rt_send_put
 * \param p As in x10rt_send_put
 * \param buf As in x10rt_send_put
 * \param len As in x10rt_send_put
 */
X10RT_C void x10rt_lgl_send_put (x10rt_msg_params *p, void *buf, x10rt_copy_sz len);

X10RT_C void x10rt_lgl_get_stats (x10rt_stats *s);
X10RT_C void x10rt_lgl_set_stats (x10rt_stats *s);
X10RT_C void x10rt_lgl_zero_stats (x10rt_stats *s);


/** \see #x10rt_remote_alloc
 * \param place As in x10rt_remote_alloc.
 * \param sz As in x10rt_remote_alloc.
 * \param ch As in x10rt_remote_alloc.
 * \param arg As in x10rt_remote_alloc.
 */
X10RT_C void x10rt_lgl_remote_alloc (x10rt_place place, x10rt_remote_ptr sz,
                                     x10rt_completion_handler3 *ch, void *arg);

/** \see #x10rt_remote_free
 * \param place As in x10rt_remote_free.
 * \param ptr As in x10rt_remote_free.
 */
X10RT_C void x10rt_lgl_remote_free (x10rt_place place, x10rt_remote_ptr ptr);

/** \see #x10rt_remote_op
 * \param place As in #x10rt_remote_op
 * \param remote_addr As in #x10rt_remote_op
 * \param type As in #x10rt_remote_op
 * \param value As in #x10rt_remote_op
 */
X10RT_C void x10rt_lgl_remote_op (x10rt_place place, x10rt_remote_ptr remote_addr,
                                  x10rt_op_type type, unsigned long long value);

/** \see #x10rt_remote_ops
 * \param ops As in #x10rt_remote_ops
 * \param num_ops As in #x10rt_remote_ops
 */
X10RT_C void x10rt_lgl_remote_ops (x10rt_remote_op_params *ops, size_t num_ops);


/** \see #x10rt_register_mem
 * \param ptr As in #x10rt_register_mem
 * \param len As in #x10rt_register_mem
 * \returns As in #x10rt_register_mem
 */
X10RT_C x10rt_remote_ptr x10rt_lgl_register_mem (void *ptr, size_t len);


/** \see #x10rt_blocks_threads
 * \param d as in x10rt_blocks_threads;
 * \param type as in x10rt_blocks_threads;
 * \param dyn_shm as in x10rt_blocks_threads;
 * \param blocks as in x10rt_blocks_threads;
 * \param threads as in x10rt_blocks_threads;
 * \param cfg as in x10rt_blocks_threads;
 */
X10RT_C void x10rt_lgl_blocks_threads (x10rt_place d, x10rt_msg_type type, int dyn_shm,
                                       int *blocks, int *threads, const int *cfg);

/** Probe all the underlying backends. \see #x10rt_probe
 */
X10RT_C void x10rt_lgl_probe (void);


/** Probe all the underlying backends, blocking if nothing is available.  \see #x10rt_blocking_probe
 */
X10RT_C void x10rt_lgl_blocking_probe (void);


/** Clean up the logical layer.  Called by #x10rt_finalize.
 */
X10RT_C void x10rt_lgl_finalize (void); 

/** \see #x10rt_team_new
 * \param placec As in #x10rt_team_new
 * \param placev As in #x10rt_team_new
 * \param ch As in #x10rt_team_new
 * \param arg As in #x10rt_team_new
 */
X10RT_C void x10rt_lgl_team_new (x10rt_place placec, x10rt_place *placev,
                                 x10rt_completion_handler2 *ch, void *arg);

/** \see #x10rt_team_del
 * \param team As in #x10rt_team_del
 * \param role As in #x10rt_team_del
 * \param ch As in #x10rt_team_del
 * \param arg As in #x10rt_team_del
 */
X10RT_C void x10rt_lgl_team_del (x10rt_team team, x10rt_place role,
                                 x10rt_completion_handler *ch, void *arg);

/** \see #x10rt_team_sz
 * \param team As in #x10rt_team_sz
 * \returns As in #x10rt_team_sz
 */
X10RT_C x10rt_place x10rt_lgl_team_sz (x10rt_team team);

/** \see #x10rt_team_split
 * \param parent As in #x10rt_team_split
 * \param parent_role As in #x10rt_team_split
 * \param color As in #x10rt_team_split
 * \param new_role As in #x10rt_team_split
 * \param ch As in #x10rt_team_split
 * \param arg As in #x10rt_team_split
 */
X10RT_C void x10rt_lgl_team_split (x10rt_team parent, x10rt_place parent_role,
                                   x10rt_place color, x10rt_place new_role,
                                   x10rt_completion_handler2 *ch, void *arg);

/** \see #x10rt_barrier
 * \param team As in #x10rt_barrier
 * \param role As in #x10rt_barrier
 * \param ch As in #x10rt_barrier
 * \param arg As in #x10rt_barrier
 */
X10RT_C void x10rt_lgl_barrier (x10rt_team team, x10rt_place role,
                                x10rt_completion_handler *ch, void *arg);

/** \see #x10rt_bcast
 * \param team As in #x10rt_bcast
 * \param role As in #x10rt_bcast
 * \param root As in #x10rt_bcast
 * \param sbuf As in #x10rt_bcast
 * \param dbuf As in #x10rt_bcast
 * \param el As in #x10rt_bcast
 * \param count As in #x10rt_bcast
 * \param ch As in #x10rt_bcast
 * \param arg As in #x10rt_bcast
 */
X10RT_C void x10rt_lgl_bcast (x10rt_team team, x10rt_place role,
                              x10rt_place root, const void *sbuf, void *dbuf,
                              size_t el, size_t count,
                              x10rt_completion_handler *ch, void *arg);

/** \see #x10rt_scatter
 * \param team As in #x10rt_scatter
 * \param role As in #x10rt_scatter
 * \param root As in #x10rt_scatter
 * \param sbuf As in #x10rt_scatter
 * \param dbuf As in #x10rt_scatter
 * \param el As in #x10rt_scatter
 * \param count As in #x10rt_scatter
 * \param ch As in #x10rt_scatter
 * \param arg As in #x10rt_scatter
 */
X10RT_C void x10rt_lgl_scatter (x10rt_team team, x10rt_place role,
                                x10rt_place root, const void *sbuf, void *dbuf,
                                size_t el, size_t count,
                                x10rt_completion_handler *ch, void *arg);

/** \see #x10rt_alltoall
 * \param team As in #x10rt_alltoall
 * \param role As in #x10rt_alltoall
 * \param sbuf As in #x10rt_alltoall
 * \param dbuf As in #x10rt_alltoall
 * \param el As in #x10rt_alltoall
 * \param count As in #x10rt_alltoall
 * \param ch As in #x10rt_alltoall
 * \param arg As in #x10rt_alltoall
 */
X10RT_C void x10rt_lgl_alltoall (x10rt_team team, x10rt_place role,
                                 const void *sbuf, void *dbuf,
                                 size_t el, size_t count,
                                 x10rt_completion_handler *ch, void *arg);

/** \see #x10rt_allreduce
 * \param team As in #x10rt_allreduce
 * \param role As in #x10rt_allreduce
 * \param sbuf As in #x10rt_allreduce
 * \param dbuf As in #x10rt_allreduce
 * \param op As in #x10rt_allreduce
 * \param dtype As in #x10rt_allreduce
 * \param count As in #x10rt_allreduce
 * \param ch As in #x10rt_allreduce
 * \param arg As in #x10rt_allreduce
 */
X10RT_C void x10rt_lgl_allreduce (x10rt_team team, x10rt_place role,
                                  const void *sbuf, void *dbuf,
                                  x10rt_red_op_type op,
                                  x10rt_red_type dtype,
                                  size_t count,
                                  x10rt_completion_handler *ch, void *arg);

/** \see #x10rt_team_members
 * \param team As in #x10rt_team_translate
 * \param members As in #x10rt_team_translate
 * \param ch As in #x10rt_team_translate
 * \param arg As in #x10rt_team_translate
 */
X10RT_C void x10rt_lgl_team_members (x10rt_team team, x10rt_place *members, x10rt_completion_handler *ch, void *arg);

/** \see #x10rt_scatterv
 * \param team As in #x10rt_scatterv
 * \param role As in #x10rt_scatterv
 * \param sbuf As in #x10rt_scatterv
 * \param dbuf As in #x10rt_scatterv
 * \param op As in #x10rt_scatterv
 * \param dtype As in #x10rt_scatterv
 * \param count As in #x10rt_scatterv
 * \param ch As in #x10rt_scatterv
 * \param arg As in #x10rt_scatterv
 */
X10RT_C void x10rt_lgl_scatterv (x10rt_team team, x10rt_place role,
                    x10rt_place root, const void *sbuf, const void *soffsets, const void *scounts,
                    void *dbuf, size_t dcount,
                    size_t el, x10rt_completion_handler *ch, void *arg);

/** \see #x10rt_gather
 * \param team As in #x10rt_gather
 * \param role As in #x10rt_gather
 * \param sbuf As in #x10rt_gather
 * \param dbuf As in #x10rt_gather
 * \param op As in #x10rt_gather
 * \param dtype As in #x10rt_gather
 * \param count As in #x10rt_gather
 * \param ch As in #x10rt_gather
 * \param arg As in #x10rt_gather
 */
X10RT_C void x10rt_lgl_gather (x10rt_team team, x10rt_place role,
                    x10rt_place root, const void *sbuf, void *dbuf,
                    size_t el, size_t count,
                    x10rt_completion_handler *ch, void *arg);

/** \see #x10rt_gatherv
 * \param team As in #x10rt_gatherv
 * \param role As in #x10rt_gatherv
 * \param sbuf As in #x10rt_gatherv
 * \param dbuf As in #x10rt_gatherv
 * \param op As in #x10rt_gatherv
 * \param dtype As in #x10rt_gatherv
 * \param count As in #x10rt_gatherv
 * \param ch As in #x10rt_gatherv
 * \param arg As in #x10rt_gatherv
 */
X10RT_C void x10rt_lgl_gatherv (x10rt_team team, x10rt_place role,
                    x10rt_place root, const void *sbuf, size_t scount,
                    void *dbuf, const void *doffbuf, const void *dcounts,
                    size_t el, x10rt_completion_handler *ch, void *arg);

/** \see #x10rt_allgather
 * \param team As in #x10rt_allgather
 * \param role As in #x10rt_allgather
 * \param sbuf As in #x10rt_allgather
 * \param dbuf As in #x10rt_allgather
 * \param op As in #x10rt_allgather
 * \param dtype As in #x10rt_allgather
 * \param count As in #x10rt_allgather
 * \param ch As in #x10rt_allgather
 * \param arg As in #x10rt_allgather
 */
X10RT_C void x10rt_lgl_allgather (x10rt_team team, x10rt_place role,
		const void *sbuf,
		void *dbuf,
		size_t el, size_t count, x10rt_completion_handler *ch, void *arg);

/** \see #x10rt_allgatherv
 * \param team As in #x10rt_allgatherv
 * \param role As in #x10rt_allgatherv
 * \param sbuf As in #x10rt_allgatherv
 * \param dbuf As in #x10rt_allgatherv
 * \param op As in #x10rt_allgatherv
 * \param dtype As in #x10rt_allgatherv
 * \param count As in #x10rt_allgatherv
 * \param ch As in #x10rt_allgatherv
 * \param arg As in #x10rt_allgatherv
 */
X10RT_C void x10rt_lgl_allgatherv (x10rt_team team, x10rt_place role,
		const void *sbuf, int scount,
		void *dbuf, const void *doffsets, const void *dcounts,
		size_t el, x10rt_completion_handler *ch, void *arg);

/** \see #x10rt_alltoallv
 * \param team As in #x10rt_alltoallv
 * \param role As in #x10rt_alltoallv
 * \param sbuf As in #x10rt_alltoallv
 * \param dbuf As in #x10rt_alltoallv
 * \param op As in #x10rt_alltoallv
 * \param dtype As in #x10rt_alltoallv
 * \param count As in #x10rt_alltoallv
 * \param ch As in #x10rt_alltoallv
 * \param arg As in #x10rt_alltoallv
 */
X10RT_C void x10rt_lgl_alltoallv (x10rt_team team, x10rt_place role,
                    const void *sbuf, const void *soffsets, const void *scounts,
                    void *dbuf, const void *doffsets, const void *dcounts,
                    size_t el, x10rt_completion_handler *ch, void *arg);
#endif

// vim: tabstop=4:shiftwidth=4:expandtab:textwidth=100
