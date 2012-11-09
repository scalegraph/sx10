/*
 * (c) Copyright IBM Corporation 2009
 * 
 * $Id$
 *
 * This file is part of X10 Runtime on MPI layer implementation.
 */

/* MPICH2 mpi.h wants to not have SEEK_SET etc defined for C++ bindings */
#include <mpi.h>

#include <new>

#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <cassert>

#include <algorithm>

#include <pthread.h>
#include <errno.h>
#define __STDC_FORMAT_MACROS
#include <stdint.h>
#include <inttypes.h>
#include <typeinfo>
#include <vector>
#include <map>

#include <x10rt_net.h>

#include <x10rt_types.h>
#include <x10rt_internal.h>
#include <x10rt_cpp.h>
#include <x10rt_ser.h>

#if 0
#define X10RT_NET_DEBUG(fmt, ...) fprintf(stderr, "[%s:%d:%s] (%"PRIu32") " fmt "\n", __FILE__, __LINE__, __func__, static_cast<uint32_t>(x10rt_net_here()), __VA_ARGS__)
#define X10RT_NET_DEBUGV(fmt, var) fprintf(stderr, "[%s:%d:%s] (%"PRIu32") " #var " = %"fmt "\n", __FILE__, __LINE__, __func__, static_cast<uint32_t>(x10rt_net_here()), (var))
#else
#define X10RT_NET_DEBUG(fmt, ...)
#define X10RT_NET_DEBUGV(fmt, ...)
#endif

static void x10rt_net_coll_init(int *argc, char ** *argv, x10rt_msg_type *counter);

/* Init time constants */
#define X10RT_REQ_FREELIST_INIT_LEN     (256)
#define X10RT_REQ_BUMP                  (32)
#define X10RT_CB_TBL_SIZE               (128)
#define X10RT_MAX_PEEK_DEPTH            (16)
#define X10RT_MAX_OUTSTANDING_SENDS     (256)

/* Generic utility funcs */
template <class T> T* ChkAlloc(size_t len) {
    if (0 == len) return NULL;
    T * ptr;
    ptr = static_cast <T*> (malloc(len));
    if (NULL == ptr) {
        fprintf(stderr, "[%s:%d] No more memory\n",
                 __FILE__, __LINE__);
        abort();
    }
    return ptr;
}
template <class T> T* ChkRealloc(T * ptr, size_t len) {
    if (0 == len) {
        free(ptr);
        return NULL;
    }
    T * ptr2;
    ptr2 = static_cast <T*> (realloc(ptr, len));
    if (NULL == ptr2) {
        fprintf(stderr, "[%s:%d] No more memory\n", __FILE__, __LINE__);
        abort();
    }
    return ptr2;
}

/**
 * Get count of bytes received from MPI_Status
 */
static inline int get_recvd_bytes(MPI_Status * msg_status) {
    int received_bytes;
    MPI_Get_count(msg_status,
            MPI_BYTE,
            &received_bytes);
    return received_bytes;
}

static inline void get_lock(pthread_mutex_t * lock) {
    if(pthread_mutex_lock(lock)) {
        perror("pthead_mutex_lock");
        abort();
    }
}

static inline void release_lock(pthread_mutex_t * lock) {
    if(pthread_mutex_unlock(lock)) {
        perror("pthread_mutex_unlock");
        abort();
    }
}

#define LOCK_IF_MPI_IS_NOT_MULTITHREADED {  \
    if(!global_state.is_mpi_multithread)    \
        get_lock(&global_state.lock);       \
}

#define UNLOCK_IF_MPI_IS_NOT_MULTITHREADED {    \
    if(!global_state.is_mpi_multithread)        \
        release_lock(&global_state.lock);       \
}

/**
 * Each X10RT API call is broken down into
 * a X10RT request. Each request of either 
 * one of the following types
 */

typedef enum {
    X10RT_REQ_TYPE_SEND                 = 1,
    X10RT_REQ_TYPE_RECV                 = 2,
    X10RT_REQ_TYPE_GET_INCOMING_DATA    = 3,
    X10RT_REQ_TYPE_GET_OUTGOING_REQ     = 4,
    X10RT_REQ_TYPE_GET_INCOMING_REQ     = 5,
    X10RT_REQ_TYPE_GET_OUTGOING_DATA    = 6,
    X10RT_REQ_TYPE_PUT_OUTGOING_DATA    = 7,
    X10RT_REQ_TYPE_PUT_OUTGOING_REQ     = 8,
    X10RT_REQ_TYPE_PUT_INCOMING_REQ     = 9,
    X10RT_REQ_TYPE_PUT_INCOMING_DATA    = 10,
    X10RT_REQ_TYPE_UNDEFINED            = -1
} X10RT_REQ_TYPES;

typedef struct _x10rt_get_req {
    int                       type;
    int                       dest_place;
    void                    * msg;
    int                       msg_len;
    int                       len;
} x10rt_get_req;

typedef struct _x10rt_put_req {
    int                       type;
    void                    * msg;
    int                       msg_len;
    int                       len;
} x10rt_put_req;

/* differentiate from x10rt_{get|put}_req
 * to save precious bytes from packet size
 * for each PUT/GET */
typedef struct _x10rt_nw_req {
    int                       type;
    int                       msg_len;
    int                       len;
} x10rt_nw_req;

class x10rt_req {
        int                   type;
        MPI_Request           mpi_req;
        x10rt_req           * next;
        x10rt_req           * prev;
        void                * buf;
        x10rt_get_req         get_req;
    public:
        x10rt_req()  {
            next = prev = NULL;
            buf = NULL;
            type = X10RT_REQ_TYPE_UNDEFINED;
        }
        ~x10rt_req() {
            next = prev = NULL;
            buf = NULL;
            type = X10RT_REQ_TYPE_UNDEFINED;
        }
        void setType(int t) { this->type = t; }
        int  getType() { return this->type; }
        MPI_Request * getMPIRequest() { return &this->mpi_req; }
        void setBuf(void * buf) { this->buf = buf; }
        void * getBuf() { return this->buf; }
        void setUserGetReq(x10rt_get_req * r) {
            this->get_req.type       = r->type;
            this->get_req.dest_place = r->dest_place;
            this->get_req.msg        = r->msg;
            this->get_req.msg_len    = r->msg_len;
            this->get_req.len        = r->len;
        }
        x10rt_get_req * getUserGetReq() {
            assert(X10RT_REQ_TYPE_GET_INCOMING_DATA == type);
            return &this->get_req;
        }
        friend class x10rt_req_queue;
};

class x10rt_req_queue {
        pthread_mutex_t       lock;
        x10rt_req           * head;
        x10rt_req           * tail;
        int                   len;
    public:
        x10rt_req_queue()  {
            len = 0;
            head = tail = NULL;
            if (pthread_mutex_init(&lock, NULL)) {
                perror("pthread_mutex_init");
                abort();
            }
        }
        ~x10rt_req_queue() {
            while (len > 0) {
                x10rt_req * r = pop();
                r->~x10rt_req();
            }
            assert((NULL == head) && (NULL == tail) && (0 == len));
            if (pthread_mutex_destroy(&lock)) {
                perror("pthread_mutex_destroy");
                abort();
            }
        }
        int length() { return len; }
        x10rt_req * start() {
            x10rt_req * r;
            get_lock(&this->lock);
            {
                r = head;
            }
            release_lock(&this->lock);
            return r;
        }
        x10rt_req * next(x10rt_req * r) {
            x10rt_req * n;
            get_lock(&this->lock);
            {
                n = r->next;
            }
            release_lock(&this->lock);
            return n;
        }
        /**
         * Append a few empty requests to queue
         */
        void addRequests(int num) {
            /* wrap around enqueue (which is thread safe) */
            for (int i = 0; i < num; ++i) {
                char * mem = ChkAlloc<char>(sizeof(x10rt_req));
                x10rt_req * r = new(mem) x10rt_req();
                enqueue(r);
            }
        }
        /**
         * Appends to end of queue
         */
        void enqueue(x10rt_req * r) {
            /* thread safe */

            get_lock(&this->lock);

            r->next     = NULL;
            if (head) {
                assert(NULL != tail);
                tail->next = r;
                r->prev = tail;
                tail = r;
            } else {
                assert(NULL == tail);
                r->prev = NULL;
                head = tail = r;
            }
            len++;

            release_lock(&this->lock);
        }
        /**
         * Removes first element from queue
         */
        x10rt_req * pop() {
            /* thread safe */
            get_lock(&this->lock);

            x10rt_req * r = head;
            if (NULL != head) {
                head = head->next;
                len--;
                if (NULL == head) {
                    tail = NULL;
                    assert(0 == len);
                }
            }

            release_lock(&this->lock);
            return r;
        }
        /**
         * Removes a request from any location
         * in the queue
         */
        void remove(x10rt_req * r) {
            /* thread safe */
            get_lock(&this->lock);

            assert((NULL != head) && (NULL != tail) && (len > 0));
            if (r->prev) r->prev->next = r->next;
            if (r->next) r->next->prev = r->prev;
            if (r == head) head = r->next;
            if (r == tail) tail = r->prev;
            r->next = r->prev = NULL;
            len--;

            release_lock(&this->lock);
        }
        /**
         * Always returns a request from queue.
         * If queue is empty, adds more requests
         * to queue an returns first request
         */
        x10rt_req * popNoFail() {
            /* wrap around pop (which is thread safe) */
            x10rt_req * r = pop();
            if (NULL == r) {
                addRequests(X10RT_REQ_BUMP);
                r = pop();
            }
            return r;
        }
};

typedef x10rt_handler *amSendCb;
typedef x10rt_finder *putCb1;
typedef x10rt_notifier *putCb2;
typedef x10rt_finder *getCb1;
typedef x10rt_notifier *getCb2;

class x10rt_internal_state {
    public:
        bool                init;
        bool                finalized;
        pthread_mutex_t     lock;
        bool                is_mpi_multithread;
        int                 rank;
        int                 nprocs;
        MPI_Comm            mpi_comm;
        amSendCb          * amCbTbl;
        unsigned            amCbTblSize;
        putCb1            * putCb1Tbl;
        putCb2            * putCb2Tbl;
        unsigned            putCbTblSize;
        getCb1            * getCb1Tbl;
        getCb2            * getCb2Tbl;
        unsigned            getCbTblSize;
        int                 _reserved_tag_get_data;
        int                 _reserved_tag_get_req;
        int                 _reserved_tag_put_data;
        int                 _reserved_tag_put_req;
        x10rt_req_queue     free_list;
        x10rt_req_queue     pending_send_list;
        x10rt_req_queue     pending_recv_list;

        x10rt_internal_state() {
            init                = false;
            finalized           = false;
            is_mpi_multithread  = false;
        }
        void Init() {
            init          = true;
            amCbTbl       =
                ChkAlloc<amSendCb>(sizeof(amSendCb) * X10RT_CB_TBL_SIZE);
            amCbTblSize   = X10RT_CB_TBL_SIZE;
            putCb1Tbl     =
                ChkAlloc<putCb1>(sizeof(putCb1) * X10RT_CB_TBL_SIZE);
            putCb2Tbl     =
                ChkAlloc<putCb2>(sizeof(putCb2) * X10RT_CB_TBL_SIZE);
            putCbTblSize  = X10RT_CB_TBL_SIZE;
            getCb1Tbl     =
                ChkAlloc<getCb1>(sizeof(getCb1) * X10RT_CB_TBL_SIZE);
            getCb2Tbl     =
                ChkAlloc<getCb2>(sizeof(getCb2) * X10RT_CB_TBL_SIZE);
            getCbTblSize  = X10RT_CB_TBL_SIZE;

            free_list.addRequests(X10RT_REQ_FREELIST_INIT_LEN);
            if (pthread_mutex_init(&lock, NULL)) {
                perror("pthread_mutex_init");
                abort();
            }
        }
        ~x10rt_internal_state() {
            free(amCbTbl);
            free(putCb1Tbl);
            free(putCb2Tbl);
            free(getCb1Tbl);
            free(getCb2Tbl);
            if (pthread_mutex_destroy(&lock)) {
                perror("pthread_mutex_destroy");
                abort();
            }
        }
};

static x10rt_internal_state     global_state;

void x10rt_net_init(int *argc, char ** *argv, x10rt_msg_type *counter) {
    assert(!global_state.finalized);
    assert(!global_state.init);

    global_state.Init();

    int provided;
    if(NULL != getenv("X10RT_MPI_THREAD_MULTIPLE")) {
        global_state.is_mpi_multithread = true;
        if (MPI_SUCCESS != MPI_Init_thread(argc, argv, 
                    MPI_THREAD_MULTIPLE, &provided)) {
            fprintf(stderr, "[%s:%d] Error in MPI_Init\n", __FILE__, __LINE__);
            abort();
        }
        MPI_Comm_rank(MPI_COMM_WORLD, &global_state.rank);
        if (MPI_THREAD_MULTIPLE != provided) {
            if (0 == global_state.rank) {
                fprintf(stderr, "[%s:%d] Underlying MPI implementation"
                        " needs to provide MPI_THREAD_MULTIPLE threading level\n",
                        __FILE__, __LINE__);
                fprintf(stderr, "[%s:%d] Alternatively, you could unset env var"
                        " X10RT_MPI_THREAD_MULTIPLE from you environment\n",
                        __FILE__, __LINE__);
            }
            if (MPI_SUCCESS != MPI_Finalize()) {
                fprintf(stderr, "[%s:%d] Error in MPI_Finalize\n",
                        __FILE__, __LINE__);
                abort();
            }
        }
    } else {
        global_state.is_mpi_multithread = false;
        if (MPI_SUCCESS != MPI_Init(argc, argv)) {
            fprintf(stderr, "[%s:%d] Error in MPI_Init\n", __FILE__, __LINE__);
            abort();
        }
    }
    if (MPI_SUCCESS != MPI_Comm_size(MPI_COMM_WORLD, &global_state.nprocs)) {
        fprintf(stderr, "[%s:%d] Error in MPI_Comm_size\n",
                __FILE__, __LINE__);
        abort();
    }
    if (MPI_SUCCESS != MPI_Comm_rank(MPI_COMM_WORLD, &global_state.rank)) {
        fprintf(stderr, "[%s:%d] Error in MPI_Comm_rank\n",
                __FILE__, __LINE__);
        abort();
    }

    /* Reserve tags for internal use */
    global_state._reserved_tag_put_req  = (*counter)++;
    global_state._reserved_tag_put_data = (*counter)++;
    global_state._reserved_tag_get_req  = (*counter)++;
    global_state._reserved_tag_get_data = (*counter)++;

    /* X10RT uses its own communicator so user messages don't
     * collide with internal messages (Mixed mode programming,
     * using MPI libraries ...) */
    if (MPI_Comm_split(MPI_COMM_WORLD, 0, global_state.rank,
                &global_state.mpi_comm)) {
        fprintf(stderr, "[%s:%d] Error in MPI_Comm_split\n",
                __FILE__, __LINE__);
        abort();
    }

    if (MPI_Barrier(global_state.mpi_comm)) {
        fprintf(stderr, "[%s:%d] Error in MPI_Barrier\n",
                __FILE__, __LINE__);
        abort();
    }

    x10rt_net_coll_init(argc, argv, counter);
}

void x10rt_net_register_msg_receiver(x10rt_msg_type msg_type, x10rt_handler *cb) {
    assert(global_state.init);
    assert(!global_state.finalized);
    if (msg_type >= global_state.amCbTblSize) {
        global_state.amCbTbl     =
            ChkRealloc<amSendCb>(global_state.amCbTbl,
                    sizeof(amSendCb)*(msg_type+1));
        global_state.amCbTblSize = msg_type+1;
    }

    global_state.amCbTbl[msg_type] = cb;
}

void x10rt_net_register_put_receiver(x10rt_msg_type msg_type,
                                     x10rt_finder *cb1, x10rt_notifier *cb2) {
    assert(global_state.init);
    assert(!global_state.finalized);
    if (msg_type >= global_state.putCbTblSize) {
        global_state.putCb1Tbl     =
            ChkRealloc<putCb1>(global_state.putCb1Tbl,
                    sizeof(putCb1)*(msg_type+1));
        global_state.putCb2Tbl     =
            ChkRealloc<putCb2>(global_state.putCb2Tbl,
                    sizeof(putCb2)*(msg_type+1));
        global_state.putCbTblSize  = msg_type+1;
    }

    global_state.putCb1Tbl[msg_type] = cb1;
    global_state.putCb2Tbl[msg_type] = cb2;
}

void x10rt_net_register_get_receiver(x10rt_msg_type msg_type,
                                     x10rt_finder *cb1, x10rt_notifier *cb2) {
    assert(global_state.init);
    assert(!global_state.finalized);
    if (msg_type >= global_state.getCbTblSize) {
        global_state.getCb1Tbl     =
            ChkRealloc<getCb1>(global_state.getCb1Tbl,
                    sizeof(getCb1)*(msg_type+1));
        global_state.getCb2Tbl     =
            ChkRealloc<getCb2>(global_state.getCb2Tbl,
                    sizeof(getCb2)*(msg_type+1));
        global_state.getCbTblSize  = msg_type+1;
    }

    global_state.getCb1Tbl[msg_type] = cb1;
    global_state.getCb2Tbl[msg_type] = cb2;
}

void x10rt_net_internal_barrier (void)
{
    abort(); // FUNCTION IS ON DEATH ROW
}

x10rt_place x10rt_net_nhosts(void) {
    assert(global_state.init);
    assert(!global_state.finalized);
    return global_state.nprocs;
}

x10rt_place x10rt_net_here(void) {
    assert(global_state.init);
    assert(!global_state.finalized);
    return global_state.rank;
}

static void x10rt_net_probe_ex (bool network_only);

void x10rt_net_send_msg(x10rt_msg_params * p) {
    assert(global_state.init);
    assert(!global_state.finalized);
    assert(p->type > 0);

    x10rt_lgl_stats.msg.messages_sent++ ;
    x10rt_lgl_stats.msg.bytes_sent += p->len;

    x10rt_req * req;
    req = global_state.free_list.popNoFail();
    static bool in_recursion = false;

    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Isend(p->msg,
                p->len, MPI_BYTE,
                p->dest_place,
                p->type,
                global_state.mpi_comm,
                req->getMPIRequest())) {
        fprintf(stderr, "[%s:%d] Error in MPI_Isend\n", __FILE__, __LINE__);
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;

    if (true || ((global_state.pending_send_list.length() > 
            X10RT_MAX_OUTSTANDING_SENDS) && !in_recursion)) {
        /* Block this send until all pending sends
         * and receives have been completed. It is
         * OK as per X10RT semantics to block a send,
         * as long as we don't block x10rt_net_probe() */
        in_recursion = true;
        int complete = 0;
        MPI_Status msg_status;
        do {
            LOCK_IF_MPI_IS_NOT_MULTITHREADED;
            if (MPI_SUCCESS != MPI_Test(req->getMPIRequest(),
                        &complete,
                        &msg_status)) {
            }
            UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;
            x10rt_net_probe_ex(true);
        } while (!complete);
        global_state.free_list.enqueue(req);
        in_recursion = false;
    } else {
        req->setBuf(p->msg);
        req->setType(X10RT_REQ_TYPE_SEND);
        global_state.pending_send_list.enqueue(req);
    }
}

void x10rt_net_send_get(x10rt_msg_params *p, void *buf, x10rt_copy_sz len) {
    x10rt_lgl_stats.get.messages_sent++ ;
    x10rt_lgl_stats.get.bytes_sent += p->len;

    int                 get_msg_len;
    x10rt_req         * req;
    x10rt_nw_req      * get_msg;
    x10rt_get_req       get_req;

    assert(global_state.init);
    assert(!global_state.finalized);
    get_req.type       = p->type;
    get_req.dest_place = p->dest_place;
    get_req.msg        = p->msg;
    get_req.msg_len    = p->len;
    get_req.len        = len;

    /*      GET Message
     * +-------------------------------------+
     * | type | msg_len | len | <- msg ... ->|
     * +-------------------------------------+
     *  <--- x10rt_nw_req --->
     */
    get_msg_len         = sizeof(*get_msg) + p->len;
    get_msg             = ChkAlloc<x10rt_nw_req>(get_msg_len);
    get_msg->type       = p->type;
    get_msg->msg_len    = p->len;
    get_msg->len        = len;

    /* pre-post a recv that matches the GET request */
    req = global_state.free_list.popNoFail();
    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_Irecv(buf, len,
                MPI_BYTE,
                p->dest_place,
                global_state._reserved_tag_get_data,
                global_state.mpi_comm,
                req->getMPIRequest())) {
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;
    req->setBuf(NULL);
    req->setUserGetReq(&get_req);
    req->setType(X10RT_REQ_TYPE_GET_INCOMING_DATA);
    global_state.pending_recv_list.enqueue(req);

    /* send the GET request */
    req = global_state.free_list.popNoFail();
    memcpy(static_cast <void *> (&get_msg[1]), p->msg, p->len);

    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Isend(get_msg,
                get_msg_len,
                MPI_BYTE,
                p->dest_place,
                global_state._reserved_tag_get_req,
                global_state.mpi_comm,
                req->getMPIRequest())) {
        fprintf(stderr, "[%s:%d] Error in MPI_Isend\n", __FILE__, __LINE__);
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;

    /* Block this send until all pending sends
     * and receives have been completed. It is
     * OK as per X10RT semantics to block a send,
     * as long as we don't block x10rt_net_probe() */
    int complete = 0;
    MPI_Status msg_status;
    do {
        LOCK_IF_MPI_IS_NOT_MULTITHREADED;
        if (MPI_SUCCESS != MPI_Test(req->getMPIRequest(),
                    &complete,
                    &msg_status)) {
        }
        UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;
        x10rt_net_probe_ex(true);
    } while (!complete);
    global_state.free_list.enqueue(req);

}


void x10rt_net_send_put(x10rt_msg_params *p, void *buf, x10rt_copy_sz len) {
    x10rt_lgl_stats.put.messages_sent++ ;
    x10rt_lgl_stats.put.bytes_sent += p->len;
    x10rt_lgl_stats.put_copied_bytes_sent += len;

    int put_msg_len;
    x10rt_put_req * put_msg;
    assert(global_state.init);
    assert(!global_state.finalized);

    x10rt_req * req = global_state.free_list.popNoFail();

    /*      PUT Message
     * +-------------------------------------------+
     * | type | msg | msg_len | len | <- msg ... ->|
     * +-------------------------------------------+
     *  <------ x10rt_put_req ----->
     */
    put_msg_len         = sizeof(*put_msg) + p->len;
    put_msg             = ChkAlloc<x10rt_put_req>(put_msg_len);
    put_msg->type       = p->type;
    put_msg->msg        = p->msg;
    put_msg->msg_len    = p->len;
    put_msg->len        = len;
    memcpy(static_cast <void *> (&put_msg[1]), p->msg, p->len);

    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Isend(put_msg,
                put_msg_len,
                MPI_BYTE,
                p->dest_place,
                global_state._reserved_tag_put_req,
                global_state.mpi_comm,
                req->getMPIRequest())) {
        fprintf(stderr, "[%s:%d] Error in MPI_Isend\n", __FILE__, __LINE__);
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;
    req->setBuf(put_msg);
    req->setType(X10RT_REQ_TYPE_PUT_OUTGOING_REQ);
    global_state.pending_send_list.enqueue(req);

    /* Block this send until all pending sends
     * and receives have been completed. It is
     * OK as per X10RT semantics to block a send,
     * as long as we don't block x10rt_net_probe() */
    int complete = 0;
    MPI_Status msg_status;
    do {
        LOCK_IF_MPI_IS_NOT_MULTITHREADED;
        if (MPI_SUCCESS != MPI_Test(req->getMPIRequest(),
                    &complete,
                    &msg_status)) {
        }
        UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;
        x10rt_net_probe_ex(true);
    } while (!complete);

    req = global_state.free_list.popNoFail();
    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Isend(buf,
                len,
                MPI_BYTE,
                p->dest_place,
                global_state._reserved_tag_put_data,
                global_state.mpi_comm,
                req->getMPIRequest())) {
        fprintf(stderr, "[%s:%d] Error in MPI_Isend\n", __FILE__, __LINE__);
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;
    global_state.free_list.enqueue(req);

}

static void send_completion(x10rt_req_queue * q,
        x10rt_req * req) {
    free(req->getBuf());
    q->remove(req);
    global_state.free_list.enqueue(req);
}

static void recv_completion(int ix, int bytes,
        x10rt_req_queue * q, x10rt_req * req) {
    assert(ix>0);
    amSendCb cb = global_state.amCbTbl[ix];
    assert(cb != NULL);
    x10rt_msg_params p = { x10rt_net_here(),
                           ix,
                           req->getBuf(),
                           bytes,
                           0
                         };

    q->remove(req);

    assert(ix > 0);

    x10rt_lgl_stats.msg.messages_received++;
    x10rt_lgl_stats.msg.bytes_received += p.len;

    release_lock(&global_state.lock);
    {
        cb(&p);
    }
    get_lock(&global_state.lock);

    free(req->getBuf());
    global_state.free_list.enqueue(req);
}

static void get_incoming_data_completion(x10rt_req_queue * q,
        x10rt_req * req) {
    x10rt_get_req * get_req = req->getUserGetReq();
    getCb2 cb = global_state.getCb2Tbl[get_req->type];
    x10rt_msg_params p = { get_req->dest_place,
                           get_req->type,
                           get_req->msg,
                           get_req->msg_len,
                           0
                         };
    q->remove(req);
    x10rt_lgl_stats.get_copied_bytes_sent += get_req->len;

    release_lock(&global_state.lock);
    cb(&p, get_req->len);
    get_lock(&global_state.lock);

    free(get_req->msg);
    global_state.free_list.enqueue(req);
}

static void get_outgoing_req_completion(x10rt_req_queue * q, x10rt_req * req) {
    free(req->getBuf());
    q->remove(req);
    global_state.free_list.enqueue(req);
}

static void get_incoming_req_completion(int dest_place,
        x10rt_req_queue * q, x10rt_req * req) {
    /*      GET Message
     * +-------------------------------------+
     * | type | msg_len | len | <- msg ... ->|
     * +-------------------------------------+
     *  <--- x10rt_nw_req --->
     */
    x10rt_nw_req * get_nw_req = static_cast <x10rt_nw_req *> (req->getBuf());
    int len = get_nw_req->len;
    getCb1 cb = global_state.getCb1Tbl[get_nw_req->type];
    x10rt_msg_params p = { x10rt_net_here(),
                           get_nw_req->type,
                           static_cast <void *> (&get_nw_req[1]),
                           get_nw_req->msg_len,
                           0
                         };
    q->remove(req);
    x10rt_lgl_stats.get.messages_received++;
    x10rt_lgl_stats.get.bytes_received += p.len;
    release_lock(&global_state.lock);
    void * local = cb(&p, len);
    get_lock(&global_state.lock);
    x10rt_lgl_stats.get_copied_bytes_received += len;

    free(req->getBuf());

    /* reuse request for sending reply */
    if (MPI_SUCCESS != MPI_Isend(local,
                len,
                MPI_BYTE,
                dest_place,
                global_state._reserved_tag_get_data,
                global_state.mpi_comm,
                req->getMPIRequest())) {
        fprintf(stderr, "[%s:%d] Error in MPI_Isend\n", __FILE__, __LINE__);
        abort();
    }
    req->setBuf(NULL);
    req->setType(X10RT_REQ_TYPE_GET_OUTGOING_DATA);

    global_state.pending_send_list.enqueue(req);
}

static void get_outgoing_data_completion(x10rt_req_queue * q,
        x10rt_req * req) {
    assert(NULL == req->getBuf());
    q->remove(req);
    global_state.free_list.enqueue(req);
}

static void put_outgoing_req_completion(x10rt_req_queue * q,
        x10rt_req * req) {
    free(req->getBuf());
    q->remove(req);
    global_state.free_list.enqueue(req);
}

static void put_outgoing_data_completion(x10rt_req_queue * q,
        x10rt_req * req) {
    assert(NULL == req->getBuf());
    q->remove(req);
    global_state.free_list.enqueue(req);
}

static void put_incoming_req_completion(int src_place,
        x10rt_req_queue * q,
        x10rt_req * req) {
    /*      PUT Message
     * +-------------------------------------------+
     * | type | msg | msg_len | len | <- msg ... ->|
     * +-------------------------------------------+
     *  <------ x10rt_put_req ----->
     */
    x10rt_put_req * put_req = static_cast <x10rt_put_req *> (req->getBuf());
    int len = put_req->len;
    putCb1 cb = global_state.putCb1Tbl[put_req->type];
    x10rt_msg_params p = { x10rt_net_here(),
                           put_req->type,
                           static_cast <void *> (&put_req[1]),
                           put_req->msg_len,
                           0
                         };
    q->remove(req);
    x10rt_lgl_stats.put.messages_received++;
    x10rt_lgl_stats.put.bytes_received += p.len;

    release_lock(&global_state.lock);
    void * local = cb(&p, len);
    get_lock(&global_state.lock);

    /* reuse request for posting recv */
    if (MPI_SUCCESS != MPI_Irecv(local,
                len,
                MPI_BYTE,
                src_place,
                global_state._reserved_tag_put_data,
                global_state.mpi_comm,
                req->getMPIRequest())) {
        fprintf(stderr, "[%s:%d] Error in posting Irecv\n", __FILE__, __LINE__);
        abort();
    }
    req->setType(X10RT_REQ_TYPE_PUT_INCOMING_DATA);
    global_state.pending_recv_list.enqueue(req);
}

static void put_incoming_data_completion(x10rt_req_queue * q, x10rt_req * req) {
    x10rt_put_req   * put_req = static_cast <x10rt_put_req *> (req->getBuf());
    putCb2 cb = global_state.putCb2Tbl[put_req->type];
    x10rt_msg_params p = { x10rt_net_here(),
                           put_req->type,
                           static_cast <void *> (&put_req[1]),
                           put_req->msg_len,
                           0
                         };
    q->remove(req);
    x10rt_lgl_stats.put_copied_bytes_received += put_req->len;
    release_lock(&global_state.lock);
    cb(&p, put_req->len);
    get_lock(&global_state.lock);
    free(req->getBuf());
    global_state.free_list.enqueue(req);
}

/**
 * Checks pending sends to see if any completed.
 *
 * NOTE: This must be called with global_state.lock held
 */
static void check_pending_sends() {
    int num_checked = 0;
    MPI_Status msg_status;
    x10rt_req_queue * q = &global_state.pending_send_list;

    if (NULL == q->start()) return;

    x10rt_req * req = q->start();
    while ((NULL != req) &&
            num_checked < X10RT_MAX_PEEK_DEPTH) {
        int complete = 0;
        x10rt_req * req_copy = req;
        if (MPI_SUCCESS != MPI_Test(req->getMPIRequest(),
                    &complete,
                    &msg_status)) {
            fprintf(stderr, "[%s:%d] Error in MPI_Test\n", __FILE__, __LINE__);
            abort();
        }
        req = q->next(req);
        if (complete) {
            switch (req_copy->getType()) {
                case X10RT_REQ_TYPE_SEND:
                    send_completion(q, req_copy);
                    break;
                case X10RT_REQ_TYPE_GET_OUTGOING_REQ:
                    get_outgoing_req_completion(q, req_copy);
                    break;
                case X10RT_REQ_TYPE_GET_OUTGOING_DATA:
                    get_outgoing_data_completion(q, req_copy);
                    break;
                case X10RT_REQ_TYPE_PUT_OUTGOING_REQ:
                    put_outgoing_req_completion(q, req_copy);
                    break;
                case X10RT_REQ_TYPE_PUT_OUTGOING_DATA:
                    put_outgoing_data_completion(q, req_copy);
                    break;
                default:
                    fprintf(stderr, "[%s:%d] Unknown completion of type %d, exiting\n",
                            __FILE__, __LINE__, req_copy->getType());
                    abort();
                    break;
            };
            req = q->start();
        } else {
            num_checked++;
        }
    }
}

/**
 * Checks pending receives to see if any completed.
 *
 * NOTE: This must be called with global_state.lock held
 */
static void check_pending_receives() {
    MPI_Status msg_status;
    x10rt_req_queue * q = &global_state.pending_recv_list;

    if (NULL == q->start()) return;

    x10rt_req * req = q->start();
    while (NULL != req) {
        int complete = 0;
        x10rt_req * req_copy = req;
        if (MPI_SUCCESS != MPI_Test(req->getMPIRequest(),
                    &complete,
                    &msg_status)) {
            fprintf(stderr, "[%s:%d] Error in MPI_Test\n", __FILE__, __LINE__);
            abort();
        }
        req = q->next(req);
        if (complete) {
            switch (req_copy->getType()) {
                case X10RT_REQ_TYPE_RECV:
                    recv_completion(msg_status.MPI_TAG, get_recvd_bytes(&msg_status), q, req_copy);
                    break;
                case X10RT_REQ_TYPE_GET_INCOMING_DATA:
                    get_incoming_data_completion(q, req_copy);
                    break;
                case X10RT_REQ_TYPE_GET_INCOMING_REQ:
                    get_incoming_req_completion(msg_status.MPI_SOURCE, q, req_copy);
                    break;
                case X10RT_REQ_TYPE_PUT_INCOMING_REQ:
                    put_incoming_req_completion(msg_status.MPI_SOURCE, q, req_copy);
                    break;
                case X10RT_REQ_TYPE_PUT_INCOMING_DATA:
                    put_incoming_data_completion(q, req_copy);
                    break;
                default:
                    fprintf(stderr, "[%s:%d] Unknown completion of type %d, exiting\n",
                            __FILE__, __LINE__, req_copy->getType());
                    abort();
                    break;
            };
            req = q->start();
        }
    }
}

x10rt_remote_ptr x10rt_net_register_mem (void *ptr, size_t)
{ return (x10rt_remote_ptr)(size_t)ptr; }

void x10rt_register_thread (void) { }

void x10rt_net_probe (void) {
    x10rt_net_probe_ex(false);
}

void x10rt_net_blocking_probe (void)
{
	// TODO: make this blocking.  For now, just call probe.
	x10rt_net_probe_ex(false);
}

static void x10rt_net_probe_ex (bool network_only) {
    int arrived;
    MPI_Status msg_status;

    assert(global_state.init);
    assert(!global_state.finalized);

    get_lock(&global_state.lock);

    do {
        arrived = 0;
        if (MPI_SUCCESS != MPI_Iprobe(MPI_ANY_SOURCE,
                    MPI_ANY_TAG, global_state.mpi_comm,
                    &arrived, &msg_status)) {
            fprintf(stderr, "[%s:%d] Error probing MPI\n", __FILE__, __LINE__);
            abort();
        }

        /* Post recv for incoming message */
        if (arrived) {
            if (global_state._reserved_tag_put_data == msg_status.MPI_TAG) {
                /* Break out of loop, give up lock. At some point we have
                 * discovered the PUT request, and the thread that has
                 * processed it, will post the corresponding receive.
                 */
                check_pending_sends();
                if (!network_only) check_pending_receives();
                break;
            } else {
                /* Don't need to post recv for incoming puts, they
                 * will be matched by X10RT_PUT_INCOMING_REQ handler */
                void * recv_buf = ChkAlloc<char>(get_recvd_bytes(&msg_status));
                int tag = msg_status.MPI_TAG;
                x10rt_req * req = global_state.free_list.popNoFail();
                req->setBuf(recv_buf);
                if (MPI_SUCCESS != MPI_Irecv(recv_buf,
                            get_recvd_bytes(&msg_status),
                            MPI_BYTE,
                            msg_status.MPI_SOURCE,
                            msg_status.MPI_TAG,
                            global_state.mpi_comm,
                            req->getMPIRequest())) {
                    fprintf(stderr, "[%s:%d] Error in posting Irecv\n",
                            __FILE__, __LINE__);
                    abort();
                }
                if (tag == global_state._reserved_tag_get_req) {
                    req->setType(X10RT_REQ_TYPE_GET_INCOMING_REQ);
                } else if (tag == global_state._reserved_tag_put_req) {
                    req->setType(X10RT_REQ_TYPE_PUT_INCOMING_REQ);
                } else {
                    req->setType(X10RT_REQ_TYPE_RECV);
                }
                global_state.pending_recv_list.enqueue(req);
                if (!network_only) check_pending_receives();
            }
        } else {
            check_pending_sends();
            if (!network_only) check_pending_receives();
        }
    } while (arrived);

    release_lock(&global_state.lock);
}

void x10rt_net_finalize(void) {
    assert(global_state.init);
    assert(!global_state.finalized);

    while (global_state.pending_send_list.length() > 0 ||
            global_state.pending_recv_list.length() > 0) {
        x10rt_net_probe();
    }
    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Barrier(global_state.mpi_comm)) {
        fprintf(stderr, "[%s:%d] Error in MPI_Barrier\n", __FILE__, __LINE__);
        abort();
    }
    if (MPI_SUCCESS != MPI_Finalize()) {
        fprintf(stderr, "[%s:%d] Error in MPI_Finalize\n", __FILE__, __LINE__);
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;
    global_state.finalized = true;
}

int x10rt_net_supports (x10rt_opt o) {
    X10RT_NET_DEBUG("o = %d", o);
    switch (o) {
        case X10RT_OPT_COLLECTIVES:
        case X10RT_OPT_COLLECTIVES_APPEND:
             return 1;
             break;
        default:
            return 0;
    }
}

void x10rt_net_remote_op (x10rt_place place, x10rt_remote_ptr victim,
                          x10rt_op_type type, unsigned long long value)
{
    abort();
}

void x10rt_net_remote_ops (x10rt_remote_op_params *ops, size_t numOps)
{
    abort();
}


/** \name MPI Implementations of Collective Operations
 * There functions are implementation of collective operations with MPI.
 * There are some restrictions.
 *
 * Int in X10 must be equal to int in C++.
 *
 * More than one member (role) must not be in the same place when team is created.
 * This restriction is based on that collective communication in MPI is communication that involves a group of processes, i.e., a role corresponds to a process.
 * In this implementation, a process corresponds to a place, therefore a role corresponds to a place.
 * .
 * All collective operation functions are blocking.
 * This restriction is based on that there is no nonblocking collective communication in MPI 2.2 or older versions.
 * This means x10rt_probe() will not called during the blocking, and it will go into deadlock when collective operations and other communications are mixed.
 * For example, following code may cause a deadlock:
 * \code

val team = Team(new Array[Place](Place.MAX_PLACES, (i:int) => Place.places()(i)));
for (p in Place.places()) async at (p) {
    if (here.id() == 0) {
        // it takes long time
        var x : Int = 0;
        for (i in 0..1000000L)
            x = at(here.next()) 0; // place 0 sends the message to place 1 and waits for the reply from place 1
    }
    team.barrier(here.id()); // place 1 waits to do collective operation
}

 * \endcode
 */
/** \{ */
struct MPIGroup {
    MPIGroup (void) : group(MPI_GROUP_NULL) { }

    ~MPIGroup (void) {
        MPI_Group_free(&group);
    }

private:
    MPI_Group group;
};
    // global team database: stores the teams currently in use
struct TeamDB {

    //        Fifo<CollOp> fifo; // where we record incomplete collectives

    TeamDB (void) : teamc(0), team_next(0), teamv(NULL) { }

    ~TeamDB (void) { delete[] teamv; }

    MPI_Comm &operator[] (x10rt_team t) { assert(t<teamc); return teamv[t]; }

    // must be called with global_lock taken
    void allocTeam (x10rt_team t, x10rt_place members, x10rt_place *placev)
    {
        allocTeam_(t, members, placev);
    }

    x10rt_team allocTeam ()
    {
    	get_lock(&this->lock);
        x10rt_team t = team_next;
        team_next++;
    	release_lock(&this->lock);
        X10RT_NET_DEBUG("new id t = %d", t);
        return t;
    }

    x10rt_team allocTeam (MPI_Comm comm)
    {
        x10rt_team t = allocTeam();;
        allocTeam(t, comm);
        return t;
    }


    x10rt_team allocTeam (x10rt_place members, x10rt_place *placev)
    {
        x10rt_team t = allocTeam();;
        allocTeam_(t, members, placev);
        team_next++;
        return t;
    }

    void releaseTeam (x10rt_team t)
    {
        X10RT_NET_DEBUG("t = %d", t);
        LOCK_IF_MPI_IS_NOT_MULTITHREADED;
        MPI_Comm_free(&(this->teamv[t]));
        UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;
    }

    bool isValidTeam (x10rt_team t)
    {
        return (t < teamc);
    }

    void allocTeam (x10rt_team t, MPI_Comm comm)
    {
        X10RT_NET_DEBUG("t = %d", t);
        ensureIndex(t, true);

       MPI_Comm c;
        LOCK_IF_MPI_IS_NOT_MULTITHREADED;
       MPI_Comm_dup(comm, &c);
        UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;
        X10RT_NET_DEBUG("%s", "duped");
       this->teamv[t] = c;
    }

//            this->teamv[t] = new (safe_malloc<TeamObj>()) TeamObj(t, members, placev);

    MPI_Comm comm (x10rt_team t)
    {
        return this->teamv[t];
    }


private:

    pthread_mutex_t       lock;
    x10rt_team teamc; // size of teamv buffer
    x10rt_team team_next; // the next new team gets this id
    MPI_Comm *teamv;

        void ensureIndex (x10rt_team i, bool iscleared)
        {
            get_lock(&this->lock);
            if (i>=teamc) {
                x10rt_team new_teamc = i+1;
                teamv = safe_realloc(teamv, new_teamc);
                if (iscleared) {
                	for (x10rt_team j = 0; j < new_teamc - teamc; ++j)
                		teamv[teamc + j] = MPI_COMM_NULL;
                }
                teamc = new_teamc;
            }
            release_lock(&this->lock);
        }

        void allocTeam_ (x10rt_team t, x10rt_place members, x10rt_place *placev)
        {
            X10RT_NET_DEBUG("t = %d, members = %d", t, members);
            ensureIndex(t, true);
//            this->teamv[t] = new (safe_malloc<TeamObj>()) TeamObj(t, members, placev);

            int *ranks = new int[members];
            for (x10rt_place i = 0; i < members; ++i) {
                ranks[i] = placev[i];
                X10RT_NET_DEBUG("placev[%d} = %d", i, placev[i]);
            }
            MPI_Group grp, MPI_GROUP_WORLD;
            LOCK_IF_MPI_IS_NOT_MULTITHREADED;
            MPI_Comm_group(MPI_COMM_WORLD, &MPI_GROUP_WORLD);
            if (MPI_SUCCESS != MPI_Group_incl(MPI_GROUP_WORLD, members, ranks, &grp)) {
            	fprintf(stderr, "[%s:%d] %s\n",
            			__FILE__, __LINE__, "Error in MPI_Group_incl");
            	delete[] ranks;
            	abort();
            }
            delete[] ranks;
            MPI_Comm comm;
            if (MPI_SUCCESS != MPI_Comm_create(MPI_COMM_WORLD, grp, &comm)) {
            	fprintf(stderr, "[%s:%d] %s\n",
            			__FILE__, __LINE__, "Error in MPI_Comm_create");
            	abort();
            }
            MPI_Group_free(&MPI_GROUP_WORLD);
            UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;

            this->teamv[t] = comm;
        }

} mpi_tdb;

struct CollState {

    int TEAM_NEW_ALLOCATE_TEAM_ID;
    int TEAM_NEW_ID;
    int TEAM_NEW_FINISHED_ID;
    int TEAM_SPLIT_ALLOCATE_TEAM_ID;
    int TEAM_ALLOCATE_NEW_TEAMS_ID;
} coll_state;

struct CounterWithLock {
    int counter;
    pthread_mutex_t lock;
};

static void x10rt_net_one_setter (void *arg)
{ *((int*)arg) = 1; }

static CounterWithLock *new_counter(int count) {
	CounterWithLock *c = safe_malloc<CounterWithLock>();
	c->counter = count;
	if (pthread_mutex_init(&c->lock, NULL)) {
		perror("pthread_mutex_init");
		abort();
	}
	return c;
}

static void decrement_counter(CounterWithLock *c) {
	if (pthread_mutex_lock(&c->lock)) {
		perror("pthread_mutex_lock");
		abort();
	}
	c->counter--;
    if(pthread_mutex_unlock(&c->lock)) {
		perror("pthread_mutex_unlock");
		abort();
    }
}

static void destroy_counter(CounterWithLock *c) {
	if (pthread_mutex_destroy(&c->lock)) {
		perror("pthread_mutex_destroy");
		abort();
	}
	free(c);
}

static void team_new_decrement_counter (CounterWithLock *counter, x10rt_completion_handler2 *ch,
                                        x10rt_team t, void *arg)
{
    X10RT_NET_DEBUG("%s", "called");
    X10RT_NET_DEBUGV("d", counter->counter);
    X10RT_NET_DEBUGV("lx",*ch);

    decrement_counter(counter);
    if (counter->counter == 0) {
        ch(t, arg);
        destroy_counter(counter);
    }
}

static void team_new_finished_recv (const x10rt_msg_params *p)
{
    X10RT_NET_DEBUG("%s", "called");

    x10rt_deserbuf b;
    x10rt_deserbuf_init(&b, p);
    x10rt_team t; x10rt_deserbuf_read(&b, &t);
    x10rt_remote_ptr ch_; x10rt_deserbuf_read(&b, &ch_);
    x10rt_remote_ptr arg_; x10rt_deserbuf_read(&b, &arg_);
    x10rt_remote_ptr counter_; x10rt_deserbuf_read(&b, &counter_);

    CounterWithLock *counter = (CounterWithLock*)(size_t)counter_;
    x10rt_completion_handler2 *ch = (x10rt_completion_handler2*)(size_t)ch_;
    void *arg = (void*)(size_t)arg_;

    team_new_decrement_counter(counter, ch, t, arg);
}

static void send_team_new_finished (x10rt_place home, x10rt_team t, x10rt_remote_ptr ch_,
                                    x10rt_remote_ptr arg_, x10rt_remote_ptr counter_)
{
    X10RT_NET_DEBUG("%s", "called");

    if (x10rt_net_here()==home) {
        CounterWithLock *counter = (CounterWithLock*)(size_t)counter_;
        x10rt_completion_handler2 *ch = (x10rt_completion_handler2*)(size_t)ch_;
        void *arg = (void*)(size_t)arg_;
        team_new_decrement_counter(counter, ch, t, arg);
    } else {
        x10rt_serbuf b2;
        x10rt_serbuf_init(&b2, home, coll_state.TEAM_NEW_FINISHED_ID);
        x10rt_serbuf_write(&b2, &t);
        x10rt_serbuf_write(&b2, &ch_);
        x10rt_serbuf_write(&b2, &arg_);
        x10rt_serbuf_write(&b2, &counter_);
        x10rt_net_send_msg(&b2.p);
        x10rt_serbuf_free(&b2);
    }
}
static void team_new_recv (const x10rt_msg_params *p)
{
    X10RT_NET_DEBUG("%s", "called");

    x10rt_deserbuf b;
    x10rt_deserbuf_init(&b, p);
    x10rt_team t; x10rt_deserbuf_read(&b, &t);
    x10rt_place members; x10rt_deserbuf_read(&b, &members);
    x10rt_place *placev = safe_malloc<x10rt_place>(members);
    x10rt_deserbuf_read_ex(&b, placev, sizeof(*placev), members);
    x10rt_remote_ptr counter_; x10rt_deserbuf_read(&b, &counter_);
    x10rt_place home; x10rt_deserbuf_read(&b, &home);
    x10rt_remote_ptr ch_; x10rt_deserbuf_read(&b, &ch_);
    x10rt_remote_ptr arg_; x10rt_deserbuf_read(&b, &arg_);

    X10RT_NET_DEBUG("%s", "team allocate");
    mpi_tdb.allocTeam(t, members, placev);
    X10RT_NET_DEBUG("%s", "team allocated");

    send_team_new_finished(home,t,ch_,arg_,counter_);
    free(placev);
}

void send_team_new (x10rt_team teamc, x10rt_team *teamv, x10rt_place placec, x10rt_place *placev,
		x10rt_remote_ptr ch_, x10rt_remote_ptr arg_)
{
    X10RT_NET_DEBUG("called: teamc=%d placec=%d", teamc, placec);

    x10rt_place home = x10rt_net_here();

    CounterWithLock *counter = new_counter(x10rt_net_nhosts());
    x10rt_remote_ptr counter_ = reinterpret_cast<x10rt_remote_ptr>(counter);

    x10rt_team t = teamv[0];

//    for (x10rt_place i=0 ; i<placec; ++i) {
    for (x10rt_place place=0 ; place<x10rt_net_nhosts() ; ++place) {
//    	x10rt_place place = placev[i];
        if (place != home) {
    	X10RT_NET_DEBUGV("d",place);

            x10rt_serbuf b;
            x10rt_serbuf_init(&b, place, coll_state.TEAM_NEW_ID);
            x10rt_serbuf_write(&b, &t);
            x10rt_serbuf_write(&b, &placec);
            x10rt_serbuf_write_ex(&b, placev, sizeof(*placev), placec);
            x10rt_serbuf_write(&b, &counter_);
            x10rt_serbuf_write(&b, &home);
            x10rt_serbuf_write(&b, &ch_);
            x10rt_serbuf_write(&b, &arg_);
            x10rt_net_send_msg(&b.p);
            x10rt_serbuf_free(&b);
       }
    }
    mpi_tdb.allocTeam(t, placec, placev);
    send_team_new_finished(home,t,ch_,arg_,counter_);

    //x10rt_team t = mpi_tdb.allocTeam(placec, placev);

//    x10rt_completion_handler2 *ch_ = (x10rt_completion_handler2*)(size_t)ch;

//   ch(t, arg);
}

static void team_new_allocate_teams_recv (const x10rt_msg_params *p)
{
    X10RT_NET_DEBUG("%s", "called");

    x10rt_deserbuf b;
    x10rt_deserbuf_init(&b, p);
    x10rt_team teamc; x10rt_deserbuf_read(&b, &teamc);
    x10rt_team *teamv = safe_malloc<x10rt_team>(teamc);
    x10rt_deserbuf_read_ex(&b, teamv, sizeof(*teamv), teamc);
    size_t cont_len ; x10rt_deserbuf_read(&b, &cont_len);
    char *cont = safe_malloc<char>(cont_len);
    x10rt_deserbuf_read_ex(&b, cont, sizeof(*cont), cont_len);
    x10rt_remote_ptr ch_; x10rt_deserbuf_read(&b, &ch_);
    x10rt_remote_ptr arg_; x10rt_deserbuf_read(&b, &arg_);

    x10rt_place *places = reinterpret_cast<x10rt_place *>(cont);

    send_team_new(teamc,teamv,places[0],&places[1],ch_,arg_);
    free(cont);
    free(teamv);
}



static void send_split_new_team(x10rt_team teamc, x10rt_team *teamv,
		MPI_Comm new_comm,
		x10rt_team parent, x10rt_place parent_role,
		 x10rt_place *colorv,
		 x10rt_completion_handler2 *ch, void *arg)
{
    X10RT_NET_DEBUG("teamc=%d, parent=%d, parent_role=%d", teamc, parent, parent_role);

    int gsize = x10rt_net_team_sz(parent);

    int *new_team_ids = new int[gsize];
    for (int i = 0; i < gsize; ++i) {
    	new_team_ids[i] = teamv[colorv[i]];
    }

    int new_team_id;
    {
    	int finished = 0;
    	x10rt_net_scatter(parent, parent_role, 0, new_team_ids, &new_team_id, sizeof(x10rt_team), 1, x10rt_net_one_setter, &finished);
    	while (!finished) x10rt_net_probe_ex(true);
    }
    if (new_team_ids != NULL)
    	delete[] new_team_ids;


    mpi_tdb.allocTeam(new_team_id, new_comm);
    ch(new_team_id, arg);
}

static void team_split_allocate_teams_recv (const x10rt_msg_params *p)
{
    X10RT_NET_DEBUG("%s", "called");

    x10rt_deserbuf b;
    x10rt_deserbuf_init(&b, p);
    x10rt_team teamc; x10rt_deserbuf_read(&b, &teamc);
    x10rt_team *teamv = safe_malloc<x10rt_team>(teamc);
    x10rt_deserbuf_read_ex(&b, teamv, sizeof(*teamv), teamc);
    size_t cont_len ; x10rt_deserbuf_read(&b, &cont_len);
    char *cont = safe_malloc<char>(cont_len);
    x10rt_deserbuf_read_ex(&b, cont, sizeof(*cont), cont_len);
    x10rt_remote_ptr ch_; x10rt_deserbuf_read(&b, &ch_);
    x10rt_remote_ptr arg_; x10rt_deserbuf_read(&b, &arg_);

    MPI_Comm *comm1 = reinterpret_cast<MPI_Comm *>(cont);
    x10rt_team *team1 = reinterpret_cast<x10rt_team *>(&comm1[1]);
    x10rt_place *places = reinterpret_cast<x10rt_place *>(&team1[1]);
    x10rt_completion_handler2 *ch = (x10rt_completion_handler2*)(size_t)ch_;
    void *arg = (void*)(size_t)arg_;

    send_split_new_team(teamc,teamv,comm1[0],team1[0],places[0],&places[1],ch,arg);
    free(cont);
    free(teamv);
}

static void send_new_teams (x10rt_place home, int teamc, x10rt_team *teamv,
		int msg_id, size_t cont_len, char  *cont, x10rt_remote_ptr ch_,
                                    x10rt_remote_ptr arg_)
{
    X10RT_NET_DEBUG("%s", "called");
    X10RT_NET_DEBUGV("d",msg_id);

    x10rt_serbuf b;
    x10rt_serbuf_init(&b, home, msg_id);
    x10rt_serbuf_write(&b, &teamc);
    x10rt_serbuf_write_ex(&b, teamv, sizeof(*teamv), teamc);
    x10rt_serbuf_write(&b, &cont_len);
    x10rt_serbuf_write_ex(&b, cont, sizeof(*cont), cont_len);
    x10rt_serbuf_write(&b, &ch_);
    x10rt_serbuf_write(&b, &arg_);
    x10rt_net_send_msg(&b.p);
    x10rt_serbuf_free(&b);
}

static void team_allocate_new_teams_recv (const x10rt_msg_params *p)
{
    X10RT_NET_DEBUG("%s", "called");

    x10rt_deserbuf b;
    x10rt_deserbuf_init(&b, p);
    X10RT_NET_DEBUG("%s", "buffer init");

    x10rt_place teamc; x10rt_deserbuf_read(&b, &teamc);
    int msg_id; x10rt_deserbuf_read(&b, &msg_id);
    x10rt_place home; x10rt_deserbuf_read(&b, &home);
    size_t cont_len ; x10rt_deserbuf_read(&b, &cont_len);
    char *cont = safe_malloc<char>(cont_len);
    x10rt_deserbuf_read_ex(&b, cont, sizeof(*cont), cont_len);
    x10rt_remote_ptr ch_; x10rt_deserbuf_read(&b, &ch_);
    x10rt_remote_ptr arg_; x10rt_deserbuf_read(&b, &arg_);

    x10rt_team *new_team_ids = new x10rt_team[teamc];
    for (x10rt_team i = 0; i < teamc; ++i) {
    	new_team_ids[i] = mpi_tdb.allocTeam();
    }
    X10RT_NET_DEBUG("%s", "team id allocated");

    send_new_teams(home,teamc,new_team_ids,msg_id,cont_len,cont,ch_,arg_);
    free(cont);
}

static void x10rt_net_coll_init(int *argc, char ** *argv, x10rt_msg_type *counter) {

    mpi_tdb.allocTeam(MPI_COMM_WORLD); // t = 0
    coll_state.TEAM_NEW_ALLOCATE_TEAM_ID = (*counter)++;
    coll_state.TEAM_NEW_ID = (*counter)++;
    coll_state.TEAM_NEW_FINISHED_ID = (*counter)++;
    coll_state.TEAM_SPLIT_ALLOCATE_TEAM_ID = (*counter)++;
    coll_state.TEAM_ALLOCATE_NEW_TEAMS_ID = (*counter)++;

    X10RT_NET_DEBUGV("d",coll_state.TEAM_NEW_ALLOCATE_TEAM_ID);
    X10RT_NET_DEBUGV("d",coll_state.TEAM_NEW_ID);
    X10RT_NET_DEBUGV("d",coll_state.TEAM_NEW_FINISHED_ID);
    X10RT_NET_DEBUGV("d",coll_state.TEAM_SPLIT_ALLOCATE_TEAM_ID);
    X10RT_NET_DEBUGV("d",coll_state.TEAM_ALLOCATE_NEW_TEAMS_ID);

    x10rt_net_register_msg_receiver(coll_state.TEAM_NEW_ALLOCATE_TEAM_ID, team_new_allocate_teams_recv);
    x10rt_net_register_msg_receiver(coll_state.TEAM_NEW_ID, team_new_recv);
    x10rt_net_register_msg_receiver(coll_state.TEAM_NEW_FINISHED_ID, team_new_finished_recv);
    x10rt_net_register_msg_receiver(coll_state.TEAM_SPLIT_ALLOCATE_TEAM_ID, team_split_allocate_teams_recv);
    x10rt_net_register_msg_receiver(coll_state.TEAM_ALLOCATE_NEW_TEAMS_ID, team_allocate_new_teams_recv);
}

/** \see #x10rt_team_new
 *
 * \param placec As in #x10rt_team_new
 * \param placev As in #x10rt_team_new , but each place must appear zero or one time.
 * \param ch As in #x10rt_team_new
 * \param arg As in #x10rt_team_new
 */
void x10rt_net_team_new (x10rt_place placec, x10rt_place *placev,
		x10rt_completion_handler2 *ch, void *arg)
{
    X10RT_NET_DEBUG("%s", "called");

    x10rt_place home = x10rt_net_here();
    x10rt_remote_ptr ch_ = reinterpret_cast<x10rt_remote_ptr>(ch);
    x10rt_remote_ptr arg_ = reinterpret_cast<x10rt_remote_ptr>(arg);

    x10rt_team teamc = 1;
    int msg_id = coll_state.TEAM_NEW_ALLOCATE_TEAM_ID;
    x10rt_place *places = safe_malloc<x10rt_place>(placec + 1);
    places[0] = placec;
    memcpy(&places[1], placev, placec * sizeof(x10rt_place));
    size_t cont_len = (placec + 1) * sizeof(x10rt_place);
    char *cont = reinterpret_cast<char *>(places);

    x10rt_serbuf b;
    x10rt_serbuf_init(&b, 0, coll_state.TEAM_ALLOCATE_NEW_TEAMS_ID);
    x10rt_serbuf_write(&b, &teamc);
    x10rt_serbuf_write(&b, &msg_id);
    x10rt_serbuf_write(&b, &home);
    x10rt_serbuf_write(&b, &cont_len);
    x10rt_serbuf_write_ex(&b, cont, sizeof(*cont), cont_len);
    x10rt_serbuf_write(&b, &ch_);
    x10rt_serbuf_write(&b, &arg_);
    x10rt_net_send_msg(&b.p);
    x10rt_serbuf_free(&b);

    free(places);
}

void x10rt_net_team_del (x10rt_team team, x10rt_place role,
                         x10rt_completion_handler *ch, void *arg)
{
    X10RT_NET_DEBUG("team=%d, role=%d", team, role);
    mpi_tdb.releaseTeam(team);
   ch(arg);
    return;
}

x10rt_place x10rt_net_team_sz (x10rt_team team)
{
    int sz;

    MPI_Comm comm = mpi_tdb[team];
    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Comm_size(comm, &sz)) {
        fprintf(stderr, "[%s:%d] %s\n",
                __FILE__, __LINE__, "Error in MPI_GroupSize");
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;
    return sz;
}

void x10rt_net_team_split (x10rt_team parent, x10rt_place parent_role,
                           x10rt_place color, x10rt_place new_role,
                           x10rt_completion_handler2 *ch, void *arg)
{
    X10RT_NET_DEBUG("parent=%d, parent_role=%d, color=%d, new_role=%d", parent, parent_role, color, new_role);

    MPI_Comm comm = mpi_tdb[parent];
    int gsize = x10rt_net_team_sz(parent);

    MPI_Comm new_comm;
    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Comm_split(comm, color, new_role, &new_comm)) {
        fprintf(stderr, "[%s:%d] %s\n",
                __FILE__, __LINE__, "Error in MPI_GroupSize");
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;

    // role 0 must exist
    int *colors = new int[gsize];
    {
    	int finished = 0;
    	x10rt_net_gather(parent, parent_role, 0, &color, colors, sizeof(x10rt_place), 1, x10rt_net_one_setter, &finished);
    	while (!finished) x10rt_net_probe_ex(true);
    }

    int *new_team_ids = NULL;
    if (parent_role == 0) {
    	std::sort(colors, colors+gsize);
    	std::map<x10rt_place, int> color_mapping;
    	assert(gsize > 0);
    	int prev = colors[0]; // one or more roles exist
    	int count = 0;
    	for (int i = 0; i < gsize; ++i) {
    		if (prev != colors[i]) {
    			prev = colors[i];
    			++count;
    		}
    		color_mapping[colors[i]] = count;
    	}
    	int new_team_count = count + 1;

    	std::vector<x10rt_place> new_team_ids(gsize);
    	for (int i = 0; i < gsize; ++i) {
    		new_team_ids[i] = color_mapping[colors[i]];
    	}
    	delete[] colors;

    x10rt_place home = x10rt_net_here();
    	int msg_id = coll_state.TEAM_SPLIT_ALLOCATE_TEAM_ID;
    	const size_t cont_len = sizeof(MPI_Comm) + sizeof(x10rt_team) + (1 + gsize)  * sizeof(x10rt_place);
    	char *cont = static_cast<char *>(malloc(cont_len));
    	MPI_Comm *comm1 = reinterpret_cast<MPI_Comm *>(cont);
    	x10rt_team *team1 = reinterpret_cast<x10rt_team *>(&comm1[1]);
    	x10rt_place *places = reinterpret_cast<x10rt_place *>(&team1[1]);
    	comm1[0] = new_comm;
    	team1[0] = parent;
    	places[0] = parent_role;
    	memcpy(&places[1], &new_team_ids[0], gsize * sizeof(x10rt_place));
    	x10rt_remote_ptr ch_ = reinterpret_cast<x10rt_remote_ptr>(ch);
    	x10rt_remote_ptr arg_ = reinterpret_cast<x10rt_remote_ptr>(arg);

    	x10rt_serbuf b;
    	x10rt_serbuf_init(&b, 0, coll_state.TEAM_ALLOCATE_NEW_TEAMS_ID);
    	x10rt_serbuf_write(&b, &new_team_count);
    	x10rt_serbuf_write(&b, &msg_id);
    	x10rt_serbuf_write(&b, &home);
    	x10rt_serbuf_write(&b, &cont_len);
    	x10rt_serbuf_write_ex(&b, cont, sizeof(*cont), cont_len);
    	x10rt_serbuf_write(&b, &ch_);
    	x10rt_serbuf_write(&b, &arg_);
    	x10rt_net_send_msg(&b.p);
    	x10rt_serbuf_free(&b);

    	free(cont);
    } else {
    	int new_team_id;
    	{
    		int finished = 0;
    		x10rt_net_scatter(parent, parent_role, 0, new_team_ids, &new_team_id, sizeof(x10rt_team), 1, x10rt_net_one_setter, &finished);
    		while (!finished) x10rt_net_probe_ex(true);
    	}
    	if (new_team_ids != NULL)
    		delete[] new_team_ids;


    	mpi_tdb.allocTeam(new_team_id, new_comm);
    	ch(new_team_id, arg);
    }
}

int x10rt_red_type_length(x10rt_red_type dtype) {
    switch (dtype) {
#define BORING(x) case x: return sizeof (x10rt_red_type_info<x>::Type);
    BORING(X10RT_RED_TYPE_U8)
    BORING(X10RT_RED_TYPE_S8)
    BORING(X10RT_RED_TYPE_S16)
    BORING(X10RT_RED_TYPE_U16)
    BORING(X10RT_RED_TYPE_S32)
    BORING(X10RT_RED_TYPE_U32)
    BORING(X10RT_RED_TYPE_S64)
    BORING(X10RT_RED_TYPE_U64)
    BORING(X10RT_RED_TYPE_DBL)
    BORING(X10RT_RED_TYPE_FLT)
    BORING(X10RT_RED_TYPE_DBL_S32)
#undef BORING
    default:
        fprintf(stderr, "[%s:%d] unexpected argument. got: %d\n",
                __FILE__, __LINE__, dtype);
        abort();
    }
}

template <typename T> struct MpiTypeOf { static const MPI_Datatype value; };
template <typename T> const MPI_Datatype MpiTypeOf<T>::value = MPI_DATATYPE_NULL;
#if MPI_VERSION >= 3 || (MPI_VERSION == 2 &&MPI_SUBVERSION >=2)
template <> struct MpiTypeOf<int8_t> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<int8_t>::value = MPI_INT8_T;
template <> struct MpiTypeOf<uint8_t> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<uint8_t>::value = MPI_UINT8_T;
template <> struct MpiTypeOf<int16_t> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<int16_t>::value = MPI_INT16_T;
template <> struct MpiTypeOf<uint16_t> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<uint16_t>::value = MPI_UINT16_T;
template <> struct MpiTypeOf<int32_t> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<int32_t>::value = MPI_INT32_T;
template <> struct MpiTypeOf<uint32_t> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<uint32_t>::value = MPI_UINT32_T;
template <> struct MpiTypeOf<int64_t> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<int64_t>::value = MPI_INT64_T;
template <> struct MpiTypeOf<uint64_t> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<uint64_t>::value = MPI_UINT64_T;

#else
// There must be error when each any two of char, short, int, long, and long long have same size.
template <> struct MpiTypeOf<char> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<char>::value = MPI_CHAR;
template <> struct MpiTypeOf<signed char> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<signed char>::value = MPI_SIGNED_CHAR;
template <> struct MpiTypeOf<unsigned char> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<unsigned char>::value = MPI_UNSIGNED_CHAR;
template <> struct MpiTypeOf<short> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<short>::value = MPI_SHORT;
template <> struct MpiTypeOf<unsigned short> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<unsigned short>::value = MPI_UNSIGNED_SHORT;
template <> struct MpiTypeOf<int> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<int>::value = MPI_INT;
template <> struct MpiTypeOf<unsigned int> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<unsigned int>::value = MPI_UNSIGNED;
template <> struct MpiTypeOf<long> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<long>::value = MPI_LONG;
template <> struct MpiTypeOf<unsigned long> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<unsigned long>::value = MPI_UNSIGNED_LONG;
template <> struct MpiTypeOf<long long> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<long long>::value = MPI_LONG_LONG;
template <> struct MpiTypeOf<unsigned long long> {static const MPI_Datatype value; };
const MPI_Datatype MpiTypeOf<unsigned long long>::value = MPI_UNSIGNED_LONG_LONG;
#endif

MPI_Datatype mpi_red_type(x10rt_red_type dtype) {
#define RED_TYPE_SIZE(x) (sizeof (x10rt_red_type_info<x>::Type))
#define MPI_CORR_TYPE_IF(dtype, type, mpitype, els) ((RED_TYPE_SIZE(dtype) == sizeof(type)) ? mpitype : (els))
#define MPI_CORR_TYPE_TUPLED_WITH_DBL(dtype,inttype) \
	(MPI_CORR_TYPE_IF(inttype, int, MPI_DOUBLE_INT, \
        (fprintf(stderr, "[%s:%d] unsuported data type: %s.\n", __FILE__, __LINE__, #dtype), abort(), MPI_DATATYPE_NULL)))
#define BORING_TUPLED(dtype,inttype) CLAUSE(dtype,MPI_CORR_TYPE_TUPLED_WITH_DBL(dtype,inttype))
#define CLAUSE(cond,body) case (cond): return (body);
#define BORING(dtype) CLAUSE(dtype,MpiTypeOf<x10rt_red_type_info<dtype>::Type>::value)
    switch (dtype) {
    BORING(X10RT_RED_TYPE_U8)
    BORING(X10RT_RED_TYPE_S8)
    BORING(X10RT_RED_TYPE_S16)
    BORING(X10RT_RED_TYPE_U16)
    BORING(X10RT_RED_TYPE_S32)
    BORING(X10RT_RED_TYPE_U32)
    BORING(X10RT_RED_TYPE_S64)
    BORING(X10RT_RED_TYPE_U64)
    BORING_TUPLED(X10RT_RED_TYPE_DBL_S32,X10RT_RED_TYPE_S32)
    case X10RT_RED_TYPE_DBL:
        return MPI_DOUBLE;
    case X10RT_RED_TYPE_FLT:
        return MPI_FLOAT;
    default:
        fprintf(stderr, "[%s:%d] unexpected argument. got: %d\n",
                __FILE__, __LINE__, dtype);
        abort();
    }
#undef RED_TYPE_SIZE
#undef MPI_CORR_TYPE_IF
#undef MPI_CORR_TYPE_SUB
#undef MPI_CORR_TYPE_SIGNED
#undef MPI_CORR_TYPE_UNSIGNED
#undef MPI_CORR_TYPE_TUPLED_WITH_DBL
#undef CLAUSE
#undef BORING_SIGNED
#undef BORING_UNSIGNED
#undef BORING_TUPLED
}

MPI_Op mpi_red_arith_op_type(x10rt_red_op_type op) {
    switch (op) {
    case X10RT_RED_OP_ADD:
        return MPI_SUM;
    case X10RT_RED_OP_MUL:
        return MPI_PROD;
    case X10RT_RED_OP_AND:
        return MPI_LAND;
    case X10RT_RED_OP_OR:
        return MPI_LOR;
    case X10RT_RED_OP_XOR:
        return MPI_LXOR;
    case X10RT_RED_OP_MAX:
        return MPI_MAX;
    case X10RT_RED_OP_MIN:
        return MPI_MIN;
    default:
        fprintf(stderr, "[%s:%d] unexpected argument. got: %d\n",
                __FILE__, __LINE__, op);
        abort();
    }
}

MPI_Op mpi_red_loc_op_type(x10rt_red_op_type op) {
    switch (op) {
    case X10RT_RED_OP_MAX:
        return MPI_MAXLOC;
    case X10RT_RED_OP_MIN:
        return MPI_MINLOC;
    default:
        fprintf(stderr, "[%s:%d] unexpected argument. got: %d\n",
                __FILE__, __LINE__, op);
        abort();
    }
}

MPI_Op mpi_red_op_type(x10rt_red_type dtype, x10rt_red_op_type op) {
    switch (dtype) {
    case X10RT_RED_TYPE_U8:
    case X10RT_RED_TYPE_S8:
    case X10RT_RED_TYPE_S16:
    case X10RT_RED_TYPE_U16:
    case X10RT_RED_TYPE_S32:
    case X10RT_RED_TYPE_U32:
    case X10RT_RED_TYPE_S64:
    case X10RT_RED_TYPE_U64:
    case X10RT_RED_TYPE_DBL:
    case X10RT_RED_TYPE_FLT:
        return mpi_red_arith_op_type(op);
    case X10RT_RED_TYPE_DBL_S32:
        return mpi_red_loc_op_type(op);
    default:
        fprintf(stderr, "[%s:%d] unexpected argument. got: %d\n",
                __FILE__, __LINE__, dtype);
        abort();
    }
}

void x10rt_net_barrier (x10rt_team team, x10rt_place role,
                        x10rt_completion_handler *ch, void *arg)
{
    X10RT_NET_DEBUG("team=%d, role=%d", team, role);
    if (!mpi_tdb.isValidTeam(team)) {
        fprintf(stderr, "[%s:%d] %d is not valid team!)\n",
                __FILE__, __LINE__, team);
        return;
    }
    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Barrier(mpi_tdb.comm(team))) {
        fprintf(stderr, "[%s:%d] %s\n",
                __FILE__, __LINE__, "Error in MPI_Barrier");
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;

    ch(arg);
    return;
}

void x10rt_net_bcast (x10rt_team team, x10rt_place role,
                      x10rt_place root, const void *sbuf, void *dbuf,
                      size_t el, size_t count,
                      x10rt_completion_handler *ch, void *arg)
{
    X10RT_NET_DEBUG("team=%d, role=%d, count=%d", team, role, count);
    X10RT_NET_DEBUG("sbuf=%"PRIxPTR" dbuf=%"PRIxPTR, sbuf, dbuf);

    void *buf = (role == root) ? (void *)sbuf : dbuf;
    MPI_Comm comm = mpi_tdb.comm(team);

    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Bcast(buf, count * el, MPI_BYTE, root, comm)){
        fprintf(stderr, "[%s:%d] %s\n",
                __FILE__, __LINE__, "Error in MPI_Bcast");
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (role == root) {
    	memcpy(dbuf, sbuf, count  * el);
    }
    ch(arg);
}

void x10rt_net_scatter (x10rt_team team, x10rt_place role,
                        x10rt_place root, const void *sbuf, void *dbuf,
                        size_t el, size_t count,
                        x10rt_completion_handler *ch, void *arg)
{
    X10RT_NET_DEBUG("team=%d, role=%d, count=%d", team, role, count);
    X10RT_NET_DEBUG("sbuf=%"PRIxPTR" dbuf=%"PRIxPTR, sbuf, dbuf);

    MPI_Comm comm = mpi_tdb.comm(team);
    void *buf = (sbuf == dbuf) ? ChkAlloc<void>(count * el) : dbuf;

    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Scatter((void *)sbuf, count * el, MPI_BYTE, buf, count * el, MPI_BYTE, root, comm)){
        fprintf(stderr, "[%s:%d] %s\n",
                __FILE__, __LINE__, "Error in MPI_Scatter");
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;

    if (sbuf == dbuf) {
    	memcpy(dbuf, buf, count * el);
    	free(buf);
    }
    ch(arg);
}

void x10rt_net_alltoall (x10rt_team team, x10rt_place role,
                         const void *sbuf, void *dbuf,
                         size_t el, size_t count,
                         x10rt_completion_handler *ch, void *arg)
{
    X10RT_NET_DEBUG("team=%d, role=%d, count=%d", team, role, count);
    X10RT_NET_DEBUG("sbuf=%"PRIxPTR" dbuf=%"PRIxPTR, sbuf, dbuf);

    MPI_Comm comm = mpi_tdb.comm(team);
    void *buf = (sbuf == dbuf) ? ChkAlloc<void>(count * el) : dbuf;

    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Alltoall((void*)sbuf, count * el, MPI_BYTE, buf, count * el, MPI_BYTE, comm)){
        fprintf(stderr, "[%s:%d] %s\n",
                __FILE__, __LINE__, "Error in MPI_Alltoall");
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;

    if (sbuf == dbuf) {
    	memcpy(dbuf, buf, count * el);
    	free(buf);
    }
    ch(arg);
}

void x10rt_net_allreduce (x10rt_team team, x10rt_place role,
                          const void *sbuf, void *dbuf,
                          x10rt_red_op_type op, 
                          x10rt_red_type dtype,
                          size_t count,
                          x10rt_completion_handler *ch, void *arg)
{
    int el = x10rt_red_type_length(dtype);
    X10RT_NET_DEBUG("team=%d, role=%d, count=%d", team, role, count);
    X10RT_NET_DEBUG("sbuf=%"PRIxPTR" dbuf=%"PRIxPTR, sbuf, dbuf);
    X10RT_NET_DEBUG("dtype=%d sizeof(dtype)=%d", dtype, el);

    MPI_Comm comm = mpi_tdb.comm(team);
    void *buf = (sbuf == dbuf) ? ChkAlloc<void>(count * el) : dbuf;

    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Allreduce((void*)sbuf, buf, count, mpi_red_type(dtype), mpi_red_op_type(dtype, op), comm)){
        X10RT_NET_DEBUG("%s", "Error in MPI_Allreduce");
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;

    if (sbuf == dbuf) {
    	memcpy(dbuf, buf, count * el);
    	free(buf);
    }
    ch(arg);
}

void x10rt_net_scatterv (x10rt_team team, x10rt_place role, x10rt_place root, const void *sbuf, const void *soffsets, const void *scounts,
		void *dbuf, size_t dcount, size_t el, x10rt_completion_handler *ch, void *arg)
{
    X10RT_NET_DEBUG("team=%d, role=%d, root=%d,", team, role, root);
    X10RT_NET_DEBUG("sbuf=%"PRIxPTR" dbuf=%"PRIxPTR, sbuf, dbuf);
    X10RT_NET_DEBUG("dcount=%d", dcount);
    MPI_Comm comm = mpi_tdb.comm(team);
    int gsize = x10rt_net_team_sz(team);
    void *buf = dbuf;
    int *counts = (role == root) ? ChkAlloc<int>(gsize * el) : NULL;
    int *displs = (role == root) ? ChkAlloc<int>(gsize * el) : NULL;
    X10RT_NET_DEBUG("buf=%x, counts=%x, displs=%x", buf, counts, displs);
    if (role == root) {
    	for (int i = 0; i < gsize; ++i) {
    		counts[i] = static_cast<const int*>(scounts)[i] * el;
    		displs[i] = static_cast<const int*>(soffsets)[i] * el;
    	}
    }

    X10RT_NET_DEBUG("%s", "pre scatterv");
    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Scatterv((void *)sbuf, counts, displs, MPI_BYTE, buf, dcount * el, MPI_BYTE, root, comm)){
        fprintf(stderr, "[%s:%d] %s\n",
                __FILE__, __LINE__, "Error in MPI_Scatter");
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;
    X10RT_NET_DEBUG("%s", "pro scatterv");

    if (sbuf == dbuf) {
    	memcpy(dbuf, buf, dcount * el);
    	free(buf);
    }
    free(counts);
    free(displs);
    ch(arg);
}

void x10rt_net_gather (x10rt_team team, x10rt_place role, x10rt_place root, const void *sbuf,
		void *dbuf, size_t el, size_t count, x10rt_completion_handler *ch, void *arg)
{
    X10RT_NET_DEBUG("team=%d, role=%d, count=%d", team, role, count);
    X10RT_NET_DEBUG("sbuf=%"PRIxPTR" dbuf=%"PRIxPTR, sbuf, dbuf);

    MPI_Comm comm = mpi_tdb.comm(team);
    void *buf = (sbuf == dbuf) ? ChkAlloc<void>(count * el) : dbuf;

    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Gather((void *)sbuf, count * el, MPI_BYTE, buf, count * el, MPI_BYTE, root, comm)){
        fprintf(stderr, "[%s:%d] %s\n",
                __FILE__, __LINE__, "Error in MPI_Scatter");
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;

    X10RT_NET_DEBUG("%s", "done");
    if (sbuf == dbuf) {
    	memcpy(dbuf, buf, count * el);
    	free(buf);
    }
    ch(arg);
}

void x10rt_net_gatherv (x10rt_team team, x10rt_place role, x10rt_place root, const void *sbuf, size_t scount,
		void *dbuf, const void *doffsets, const void *dcounts, size_t el, x10rt_completion_handler *ch, void *arg)
{
    MPI_Comm comm = mpi_tdb.comm(team);
    int gsize = x10rt_net_team_sz(team);
    void *buf = dbuf;
    int *counts = (role == root) ? ChkAlloc<int>(gsize * el) : NULL;
    int *displs = (role == root) ? ChkAlloc<int>(gsize * el) : NULL;
    if (role == root) {
    	for (int i = 0; i < gsize; ++i) {
    		counts[i] = static_cast<const int*>(dcounts)[i] * el;
    		displs[i] = static_cast<const int*>(doffsets)[i] * el;
    	}
    }

    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Gatherv((void *)sbuf, scount * el, MPI_BYTE, buf, counts, displs, MPI_BYTE, root, comm)){
        fprintf(stderr, "[%s:%d] %s\n",
                __FILE__, __LINE__, "Error in MPI_Scatter");
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;

    if (sbuf == dbuf) {
    	memcpy(dbuf, buf, scount * el);
    	free(buf);
    }
    free(counts);
    free(displs);
    ch(arg);
}


void x10rt_net_allgather (x10rt_team team, x10rt_place role, const void *sbuf,
		void *dbuf, size_t el, size_t count, x10rt_completion_handler *ch, void *arg)
{
    X10RT_NET_DEBUG("team=%d, role=%d, count=%d", team, role, count);
    X10RT_NET_DEBUG("sbuf=%"PRIxPTR" dbuf=%"PRIxPTR, sbuf, dbuf);

    MPI_Comm comm = mpi_tdb.comm(team);
    void *buf = (sbuf == dbuf) ? ChkAlloc<void>(count * el) : dbuf;

    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Allgather((void *)sbuf, count * el, MPI_BYTE, buf, count * el, MPI_BYTE, comm)){
        fprintf(stderr, "[%s:%d] %s\n",
                __FILE__, __LINE__, "Error in MPI_Scatter");
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;

    if (sbuf == dbuf) {
        memcpy(dbuf, buf, count * el);
        free(buf);
    }
    ch(arg);
}
void x10rt_net_allgatherv (x10rt_team team, x10rt_place role, const void *sbuf, int scount,
		void *dbuf, const void *doffsets, const void *dcounts, size_t el, x10rt_completion_handler *ch, void *arg)
{
    MPI_Comm comm = mpi_tdb.comm(team);
    int gsize = x10rt_net_team_sz(team);
//    void *buf = (sbuf == dbuf) ? ChkAlloc<void>(scount * el) : dbuf;
    void *buf = dbuf;
    int *dcounts_ = ChkAlloc<int>(gsize * el);
    int *doffsets_ = ChkAlloc<int>(gsize * el);
    for (int i = 0; i < gsize; ++i) {
        dcounts_[i] = static_cast<const int*>(dcounts)[i] * el;
        doffsets_[i] = static_cast<const int*>(doffsets)[i] * el;
    }

    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Allgatherv((void *)sbuf, scount * el, MPI_BYTE, buf, dcounts_, doffsets_, MPI_BYTE, comm)){
        fprintf(stderr, "[%s:%d] %s\n",
                __FILE__, __LINE__, "Error in MPI_Scatter");
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;

    free(dcounts_);
    free(doffsets_);
    ch(arg);
}

void x10rt_net_alltoallv (x10rt_team team, x10rt_place role, const void *sbuf, const void *soffsets, const void *scounts,
		void *dbuf, const void *doffsets, const void *dcounts, size_t el, x10rt_completion_handler *ch, void *arg)
{
    MPI_Comm comm = mpi_tdb.comm(team);
    int gsize = x10rt_net_team_sz(team);
//    void *buf = (sbuf == dbuf) ? ChkAlloc<void>(scount * el) : dbuf;
    void *buf = dbuf;
    int *scounts_ = ChkAlloc<int>(gsize * el);
    int *soffsets_ = ChkAlloc<int>(gsize * el);
    int *dcounts_ = ChkAlloc<int>(gsize * el);
    int *doffsets_ = ChkAlloc<int>(gsize * el);
    for (int i = 0; i < gsize; ++i) {
    	scounts_[i] = static_cast<const int*>(scounts)[i] * el;
    	soffsets_[i] = static_cast<const int*>(soffsets)[i] * el;
    	dcounts_[i] = static_cast<const int*>(dcounts)[i] * el;
    	doffsets_[i] = static_cast<const int*>(doffsets)[i] * el;
    }

    LOCK_IF_MPI_IS_NOT_MULTITHREADED;
    if (MPI_SUCCESS != MPI_Alltoallv((void *)sbuf, scounts_, soffsets_, MPI_BYTE, buf, dcounts_, doffsets_, MPI_BYTE, comm)){
        fprintf(stderr, "[%s:%d] %s\n",
                __FILE__, __LINE__, "Error in MPI_Scatter");
        abort();
    }
    UNLOCK_IF_MPI_IS_NOT_MULTITHREADED;

    /*
    if (sbuf == dbuf) {
    	memcpy(dbuf, buf, scount * el);
    	free(buf);
    }
    */
    free(scounts_);
    free(soffsets_);
    free(dcounts_);
    free(doffsets_);
    ch(arg);
}


/** \} */
