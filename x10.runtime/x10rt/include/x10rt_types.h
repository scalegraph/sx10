#ifndef X10RT_TYPES_H
#define X10RT_TYPES_H

#ifdef __cplusplus
/** Used to expose c-compatible symbol names */
#define X10RT_C extern "C"
#else
/** Used to expose c-compatible symbol names */
#define X10RT_C
#endif

#include <stdint.h>

/** \file
 * Common types
 *
 * Typedefs and structs used in the X10RT API.
 */

/** An integer type capable of representing any place or quantity of places.
 */
typedef uint32_t x10rt_place;

/** An integer type capable of representing any global team identifier.  Teams are used in the
 * collective operations API.
 */
typedef uint32_t x10rt_team;

/** User callback to signal that non-blocking operations have completed.
 */
typedef void x10rt_completion_handler (void *arg);

/** User callback to signal that non-blocking team construction operations have completed.
 */
typedef void x10rt_completion_handler2 (x10rt_team, void *arg);

/** An integer type capable of representing any message type id.
 */
typedef uint16_t x10rt_msg_type;

/** An integer type capable of representing a remote void* or size_t.
 */
typedef uint64_t x10rt_remote_ptr;

/** User callback to signal that non-blocking memory allocation operation has completed.
 */
typedef void x10rt_completion_handler3 (x10rt_remote_ptr ptr, void *arg);

/** An integer type capable of representing the maximum size (in bytes) of an inter-place data copy.
 * This applies to both get and put transfers.
 */
typedef uint32_t x10rt_copy_sz;


/** A set of parameters common to many of the message sending / handling functions.
 */
struct x10rt_msg_params {

    /** The place where the message will be delivered. */
    x10rt_place dest_place;

    /** The type of the message being sent. */
    x10rt_msg_type type;

    /** The message itself.  May be NULL.  The memory is allocated and managed by the caller.  The
     * caller shall not write to or free the buffer while the call is in progress, and the callee
     * shall not continue to use the buffer after the call has returned.  In the previous sentence,
     * X10RT and the client code take on alternate roles depending on whether the call is
     * x10rt_send_*() or whether the call is a user callback triggered by #x10rt_probe().
     */
    void *msg;

    /** The length of the message in bytes.  If #msg is NULL then #len shall be 0. */
    uint32_t len;
};

/** A callback for processing a newly received message.
 */
typedef void x10rt_handler(const x10rt_msg_params *);

/** A callback that runs on the CPU on behalf of the GPU, just before a kernel.
 * This is used to configure the kernel, providing blocks, threads, and the
 * amount of dynamic ``shared memory'' per block (via the write-back
 * parameters).  It also provides the arguments to the kernel (i.e. the
 * captured state).  \todo work in progress
 */
typedef void x10rt_cuda_pre(const x10rt_msg_params *, size_t *blocks, size_t *threads, size_t *shm,
                            size_t *argc, char **argv, size_t *cmemc, char **cmemv);

/** A callback that runs on the CPU on behalf of the GPU, just after a kernel
 * has completed.  This is used for updating finish states and other things
 * that must be done after a kernel but cannot be done on the GPU.  \todo work
 * in progress
 */
typedef void x10rt_cuda_post(const x10rt_msg_params *, size_t blocks, size_t threads, size_t shm,
                             size_t argc, char *argv, size_t cmemc, char *cmemv);

/** A callback for finding a buffer at a remote place, that will be the subject of a copy operation.
 */
typedef void *x10rt_finder(const x10rt_msg_params *, x10rt_copy_sz);

/** A callback for finishing off a copy operation.  This can be arbitrary code
 * e.g. to inform the requester of the copy that the copy is complete.
 */
typedef void x10rt_notifier(const x10rt_msg_params *, x10rt_copy_sz);

/** The list of remote operations supported.
 */
typedef enum {
    X10RT_OP_ADD = (0x2 << 2),
    X10RT_OP_AND = (0x4 << 2),
    X10RT_OP_OR  = (0x3 << 2),
    X10RT_OP_XOR = (0x5 << 2)
} x10rt_op_type;

/** The list of operations supported when doing a reduction. 
 * \see x10rt_allreduce
 */
typedef enum {
    X10RT_RED_OP_ADD = 0,
    X10RT_RED_OP_MUL = 1,
    X10RT_RED_OP_AND = 3,
    X10RT_RED_OP_OR  = 4,
    X10RT_RED_OP_XOR = 5,
    X10RT_RED_OP_MAX = 6,
    X10RT_RED_OP_MIN = 7
} x10rt_red_op_type;

/** The struct that must be used when using #X10RT_RED_TYPE_DBL_S32.
 * \see #x10rt_allreduce #x10rt_red_type
 */
struct x10rt_dbl_s32 {
    /** The floating point value that will be compared in the operation*/
    double val;
    /** The index identifying the value that will be preserved over the operation*/
    int32_t idx;
};

/** The list of types supported when doing a reduction operation.  \see x10rt_allreduce.
 * Signed/unsigned integer types are available.  Floating point types are supported but may not be
 * used when doing bitwise arithmetic.  Only MIN/MAX can be used when using #X10RT_RED_TYPE_DBL_S32.
 */
typedef enum
  {
    /** Unsigned byte*/ X10RT_RED_TYPE_U8  = 0,
    /** Signed byte*/ X10RT_RED_TYPE_S8  = 1,
    /** Unsigned word*/ X10RT_RED_TYPE_S16 = 2,
    /** Signed word*/ X10RT_RED_TYPE_U16 = 3,
    /** Unsigned dword*/ X10RT_RED_TYPE_S32 = 4,
    /** Signed dword*/ X10RT_RED_TYPE_U32 = 5,
    /** Signed qword*/ X10RT_RED_TYPE_S64 = 6,
    /** Unsigned qword*/ X10RT_RED_TYPE_U64 = 7,
    /** Double precision IEEE float*/ X10RT_RED_TYPE_DBL = 8,
    /** Single precision IEEE float*/ X10RT_RED_TYPE_FLT = 9,
    /** A pair of double and signed dword*/ X10RT_RED_TYPE_DBL_S32 = 10
} x10rt_red_type;

/** The list of optional x10rt_net features.
 */
typedef enum {
    X10RT_OPT_REMOTE_OP = 0,
    X10RT_OPT_COLLECTIVES = 1
} x10rt_opt;

/**
 * Structure to hold a remote update operation
 */
typedef struct {
    uint               dest;            /* Destination place           */
    uint               op;              /* Atomic operation type       */
    unsigned long long dest_buf;        /* buffer on destination place */
    unsigned long long value;  		    /* operand value for operation */
} x10rt_remote_op_params;

#endif

// vim: tabstop=4:shiftwidth=4:expandtab:textwidth=100
