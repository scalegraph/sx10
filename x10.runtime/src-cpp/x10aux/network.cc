/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2010.
 */

#include <x10aux/config.h>

#include <x10aux/network.h>
#include <x10aux/ref.h>
#include <x10aux/RTT.h>
#include <x10aux/basic_functions.h>

#include <x10aux/serialization.h>
#include <x10aux/deserialization_dispatcher.h>

#include <x10/lang/VoidFun_0_0.h>
#include <x10/lang/String.h> // for debug output

#include <x10/lang/Closure.h> // for x10_runtime_Runtime__closure__6

#include <x10/lang/Runtime.h>

#include <strings.h>
#ifdef __CYGWIN__
extern "C" int strcasecmp(const char *, const char *);
#endif

using namespace x10::lang;
using namespace x10aux;

// caches to avoid repeatedly calling into x10rt for trivial things
x10aux::place x10aux::num_places = 0;
x10aux::place x10aux::num_hosts = 0;
x10aux::place x10aux::here = -1;
bool x10aux::x10rt_initialized = false;

// keep a counter for the session.
volatile x10_long x10aux::asyncs_sent = 0;
volatile x10_long x10aux::asyncs_received = 0;
volatile x10_long x10aux::serialized_bytes = 0;
volatile x10_long x10aux::deserialized_bytes = 0;


const int x10aux::cuda_cfgs[] = {
  /*1024*/ 8, 128,
  /*1024*/ 4, 256,
  /*1024*/ 2, 512,

  /*960*/ 5, 192,
  /*960*/ 3, 320,

  /*896*/ 7, 128,
  /*896*/ 2, 448,

  /*768*/ 6, 128,
  /*768*/ 4, 192,
  /*768*/ 3, 256,
  /*768*/ 2, 384,

  /*640*/ 5, 128,
  /*640*/ 2, 320,

  /*576*/ 3, 192,

  /*512*/ 8, 64,
  /*512*/ 4, 128,
  /*512*/ 2, 256,
  /*512*/ 1, 512,

  /*448*/ 7, 64,
  /*448*/ 1, 448,

  /*384*/ 6, 64,
  /*384*/ 3, 128,
  /*384*/ 2, 192,
  /*384*/ 1, 384,

  /*320*/ 5, 64,
  /*320*/ 1, 320,

  /*256*/ 4, 64,
  /*256*/ 2, 128,
  /*256*/ 1, 256,

  /*192*/ 3, 64,
  /*192*/ 1, 192,

  /*128*/ 2, 64,
  /*128*/ 1, 128,

  /*64*/ 1, 64,

  0 /* terminator */
};

void x10aux::blocks_threads (place p, msg_type t, x10_int shm, x10_ubyte &bs, x10_ubyte &ts, const int *cfgs)
{ x10_int a,b; x10rt_blocks_threads(p,t,shm,&a,&b,cfgs); bs=a,ts=b; }
void x10aux::blocks_threads (place p, msg_type t, x10_int shm, x10_byte &bs, x10_byte &ts, const int *cfgs)
{ x10_int a,b; x10rt_blocks_threads(p,t,shm,&a,&b,cfgs); bs=a,ts=b; }
void x10aux::blocks_threads (place p, msg_type t, x10_int shm, x10_ushort &bs, x10_ushort &ts, const int *cfgs)
{ x10_int a,b; x10rt_blocks_threads(p,t,shm,&a,&b,cfgs); bs=a,ts=b; }
void x10aux::blocks_threads (place p, msg_type t, x10_int shm, x10_short &bs, x10_short &ts, const int *cfgs)
{ x10_int a,b; x10rt_blocks_threads(p,t,shm,&a,&b,cfgs); bs=a,ts=b; }
void x10aux::blocks_threads (place p, msg_type t, x10_int shm, x10_uint &bs, x10_uint &ts, const int *cfgs)
{ x10_int a,b; x10rt_blocks_threads(p,t,shm,&a,&b,cfgs); bs=a,ts=b; }
void x10aux::blocks_threads (place p, msg_type t, x10_int shm, x10_int &bs, x10_int &ts, const int *cfgs)
{ x10rt_blocks_threads(p,t,shm,&bs,&ts,cfgs); }
void x10aux::blocks_threads (place p, msg_type t, x10_int shm, x10_ulong &bs, x10_ulong &ts, const int *cfgs)
{ x10_int a,b; x10rt_blocks_threads(p,t,shm,&a,&b,cfgs); bs=a,ts=b; }
void x10aux::blocks_threads (place p, msg_type t, x10_int shm, x10_long &bs, x10_long &ts, const int *cfgs)
{ x10_int a,b; x10rt_blocks_threads(p,t,shm,&a,&b,cfgs); bs=a,ts=b; }



void *kernel_put_finder (const x10rt_msg_params *p, x10rt_copy_sz)
{
    x10aux::deserialization_buffer buf(static_cast<char*>(p->msg));
    buf.read<x10_ulong>();
    x10_ulong remote_addr = buf.read<x10_ulong>();
    assert(buf.consumed() <= p->len);
    _X_(ANSI_X10RT<<"CUDA kernel populating: "<<remote_addr<<ANSI_RESET);
    return (void*)(size_t)remote_addr;
}

void kernel_put_notifier (const x10rt_msg_params *p, x10rt_copy_sz)
{
    x10aux::deserialization_buffer buf(static_cast<char*>(p->msg));
    bool *finished = (bool*)(size_t)buf.read<x10_ulong>();
    *finished = true;
}

x10aux::msg_type x10aux::kernel_put;

void x10aux::registration_complete (void)
{
    x10aux::kernel_put =
        x10rt_register_put_receiver(NULL, NULL, kernel_put_finder, kernel_put_notifier);
    x10rt_registration_complete();
    x10aux::x10rt_initialized = true;
}

void x10aux::network_init (int ac, char **av) {
    x10rt_init(&ac, &av);
    x10aux::here = x10rt_here();
    x10aux::num_places = x10rt_nplaces();
    x10aux::num_hosts = x10rt_nhosts();
}

void x10aux::run_async_at(x10aux::place p, x10aux::ref<Reference> real_body, x10aux::ref<x10::lang::Reference> fs_) {

    x10aux::ref<x10::lang::FinishState> fs = fs_; // avoid including FinishState in the header

    serialization_id_t real_sid = real_body->_get_serialization_id();
    if (!is_cuda(p)) {
        _X_(ANSI_BOLD<<ANSI_X10RT<<"Transmitting a simple async: "<<ANSI_RESET
            <<ref<Reference>(real_body)->toString()->c_str()
            <<" sid "<<real_sid<<" to place: "<<p);

    } else {

        _X_(ANSI_BOLD<<ANSI_X10RT<<"This is actually a kernel: "<<ANSI_RESET
            <<ref<Reference>(real_body)->toString()->c_str()
            <<" sid "<<real_sid<<" at GPU: "<<p);

    }

    x10aux::msg_type real_id = DeserializationDispatcher::getMsgType(real_sid);
    serialization_buffer buf;

    _X_(ANSI_BOLD<<ANSI_X10RT<<"Async id: "<<ANSI_RESET<<real_id);

    assert(DeserializationDispatcher::getClosureKind(real_sid)!=x10aux::CLOSURE_KIND_NOT_ASYNC);
    assert(DeserializationDispatcher::getClosureKind(real_sid)!=x10aux::CLOSURE_KIND_GENERAL_ASYNC);

    buf.write(fs);
    real_body->_serialize_body(buf);

    unsigned long sz = buf.length();
    serialized_bytes += sz; asyncs_sent++;

    _X_(ANSI_BOLD<<ANSI_X10RT<<"async size: "<<ANSI_RESET<<sz);

    x10rt_msg_params params = {p, real_id, buf.borrow(), sz};
    x10rt_send_msg(&params);
}

void x10aux::run_closure_at(x10aux::place p, x10aux::ref<Reference> body) {

    serialization_id_t sid = body->_get_serialization_id();

    _X_(ANSI_BOLD<<ANSI_X10RT<<"Transmitting a general async: "<<ANSI_RESET
        <<ref<Reference>(body)->toString()->c_str()
        <<" sid "<<sid<<" to place: "<<p);

    assert(p!=here); // this case should be handled earlier
    assert(p<num_places); // this is ensured by XRX runtime

    assert(!is_cuda(p));

    serialization_buffer buf;

    assert(DeserializationDispatcher::getClosureKind(sid)!=x10aux::CLOSURE_KIND_NOT_ASYNC);
    assert(DeserializationDispatcher::getClosureKind(sid)!=x10aux::CLOSURE_KIND_SIMPLE_ASYNC);
    msg_type id = DeserializationDispatcher::getMsgType(sid);

    _X_(ANSI_BOLD<<ANSI_X10RT<<"Async id: "<<ANSI_RESET<<id);

    body->_serialize_body(buf);

    unsigned long sz = buf.length();
    serialized_bytes += sz; asyncs_sent++;

    _X_(ANSI_BOLD<<ANSI_X10RT<<"async size: "<<ANSI_RESET<<sz);

    x10rt_msg_params params = {p, id, buf.borrow(), sz};
    x10rt_send_msg(&params);

}

void x10aux::send_get (x10aux::place place, x10aux::serialization_id_t id_,
                       serialization_buffer &buf, void *data, x10aux::copy_sz len)
{
    msg_type id = DeserializationDispatcher::getMsgType(id_);
    x10rt_msg_params p = { place, id, buf.borrow(), buf.length()};
    _X_(ANSI_BOLD<<ANSI_X10RT<<"Transmitting a get: "<<ANSI_RESET
        <<data<<" sid "<<id_<<" id "<<id<<" size "<<len<<" header "<<buf.length()<<" to place: "<<place);
    x10rt_send_get(&p, data, len);
}

void x10aux::send_put (x10aux::place place, x10aux::serialization_id_t id_,
                       serialization_buffer &buf, void *data, x10aux::copy_sz len)
{
    msg_type id = DeserializationDispatcher::getMsgType(id_);
    x10rt_msg_params p = { place, id, buf.borrow(), buf.length() };
    _X_(ANSI_BOLD<<ANSI_X10RT<<"Transmitting a put: "<<ANSI_RESET
        <<data<<" sid "<<id_<<" id "<<id<<" size "<<len<<" header "<<buf.length()<<" to place: "<<place);
    x10rt_send_put(&p, data, len);
}

x10_int x10aux::num_threads() {
#ifdef __bg__
    x10_int default_nthreads = 1;
#else
    x10_int default_nthreads = 2;
#endif
    const char* env = getenv("X10_NTHREADS");
    if (env==NULL) return default_nthreads;
    x10_int num = strtol(env, NULL, 10);
    assert (num > 0);
    return num;
}

x10_boolean x10aux::no_steals()
{
	char* s = getenv("X10_NO_STEALS");
	if (s && !(strcasecmp("false", s) == 0))
		return true;
	return false;
}

x10_boolean x10aux::static_threads() { 
#ifdef __bg__
    return true;
#else
    char* s = getenv("X10_STATIC_THREADS");
    if (s && !(strcasecmp("false", s) == 0))
    	return true;
    return false;
#endif
}

static void receive_async (const x10rt_msg_params *p) {
    _X_(ANSI_X10RT<<"Receiving an async, id ("<<p->type<<"), deserialising..."<<ANSI_RESET);
    x10aux::deserialization_buffer buf(static_cast<char*>(p->msg));
    // note: high bytes thrown away in implicit conversion
    serialization_id_t sid = x10aux::DeserializationDispatcher::getSerializationId(p->type);
    _X_(ANSI_X10RT<<"async sid: ("<<sid<<ANSI_RESET);
    x10aux::ClosureKind ck = DeserializationDispatcher::getClosureKind(sid);
    switch (ck) {
        case x10aux::CLOSURE_KIND_GENERAL_ASYNC: {
            ref<Reference> body(x10aux::DeserializationDispatcher::create<VoidFun_0_0>(buf, sid));
            assert(buf.consumed() <= p->len);
            _X_("The deserialised general async was: "<<x10aux::safe_to_string(body));
            deserialized_bytes += buf.consumed()  ; asyncs_received++;
            if (body.isNull()) return;
            (body.operator->()->*(findITable<VoidFun_0_0>(body->_getITables())->apply))();
            x10aux::dealloc(body.operator->());
        } break;
        case x10aux::CLOSURE_KIND_SIMPLE_ASYNC: {
            x10aux::ref<x10::lang::FinishState> fs = buf.read<x10aux::ref<x10::lang::FinishState> >();
            ref<Reference> body(x10aux::DeserializationDispatcher::create<VoidFun_0_0>(buf, sid));
            assert(buf.consumed() <= p->len);
            _X_("The deserialised simple async was: "<<x10aux::safe_to_string(body));
            deserialized_bytes += buf.consumed()  ; asyncs_received++;
            if (body.isNull()) return;
            x10::lang::Runtime::execute(body, fs);
        } break;
        default: abort();
    }
}

static void cuda_pre (const x10rt_msg_params *p, size_t *blocks, size_t *threads, size_t *shm,
                      size_t *argc, char **argv, size_t *cmemc, char **cmemv)
{
    _X_(ANSI_X10RT<<"Receiving a kernel pre callback, deserialising..."<<ANSI_RESET);
    x10aux::deserialization_buffer buf(static_cast<char*>(p->msg));
    buf.read<x10aux::ref<x10::lang::FinishState> >();
    // note: high bytes thrown away in implicit conversion
    serialization_id_t sid = x10aux::DeserializationDispatcher::getSerializationId(p->type);
    x10aux::CUDAPre pre = x10aux::DeserializationDispatcher::getCUDAPre(sid);
    pre(buf, p->dest_place, *blocks, *threads, *shm, *argc, *argv, *cmemc, *cmemv);
    assert(buf.consumed() <= p->len);
}

static void cuda_post (const x10rt_msg_params *p, size_t blocks, size_t threads, size_t shm,
                       size_t argc, char *argv, size_t cmemc, char *cmemv)
{
    _X_(ANSI_X10RT<<"Receiving a kernel post callback, deserialising..."<<ANSI_RESET);
    {
        serialization_id_t sid = x10aux::DeserializationDispatcher::getSerializationId(p->type);
        x10aux::deserialization_buffer buf(static_cast<char*>(p->msg));
        x10aux::CUDAPost post = x10aux::DeserializationDispatcher::getCUDAPost(sid);
        post(buf, p->dest_place, blocks, threads, shm, argc, argv, cmemc, cmemv);
    }
    {
        x10aux::deserialization_buffer buf(static_cast<char*>(p->msg));
        x10aux::ref<x10::lang::FinishState> fs = buf.read<x10aux::ref<x10::lang::FinishState> >();
        fs->notifyActivityCreation();
        fs->notifyActivityTermination();
    }
}

x10aux::msg_type x10aux::register_async_handler (const char *cubin, const char *kernel)
{
    if (cubin==NULL && kernel==NULL) {
        return x10rt_register_msg_receiver(receive_async, NULL, NULL, NULL, NULL);
    } else {
        return x10rt_register_msg_receiver(receive_async, cuda_pre, cuda_post, cubin, kernel);
    }
}

static void *receive_put (const x10rt_msg_params *p, x10aux::copy_sz len) {
    _X_(ANSI_X10RT<<"Receiving a put, deserialising for buffer finder..."<<ANSI_RESET);
    x10aux::deserialization_buffer buf(static_cast<char*>(p->msg));
    // note: high bytes thrown away in implicit conversion
    serialization_id_t sid = x10aux::DeserializationDispatcher::getSerializationId(p->type);
    x10aux::BufferFinder bf = x10aux::DeserializationDispatcher::getPutBufferFinder(sid);
    void *dropzone = bf(buf,len);
    assert(buf.consumed() <= p->len);
    return dropzone;
}

static void *cuda_receive_put (const x10rt_msg_params *p, x10aux::copy_sz len) {
    _X_(ANSI_X10RT<<"Receiving a put, deserialising for cuda buffer finder..."<<ANSI_RESET);
    x10aux::deserialization_buffer buf(static_cast<char*>(p->msg));
    // note: high bytes thrown away in implicit conversion
    serialization_id_t sid = x10aux::DeserializationDispatcher::getSerializationId(p->type);
    x10aux::BufferFinder bf = x10aux::DeserializationDispatcher::getCUDAPutBufferFinder(sid);
    void *dropzone = bf(buf,len);
    assert(buf.consumed() <= p->len);
    return dropzone;
}

static void finished_put (const x10rt_msg_params *p, x10aux::copy_sz len) {
    _X_(ANSI_X10RT<<"Receiving a put, deserialising for notifier..."<<ANSI_RESET);
    x10aux::deserialization_buffer buf(static_cast<char*>(p->msg));
    // note: high bytes thrown away in implicit conversion
    serialization_id_t sid = x10aux::DeserializationDispatcher::getSerializationId(p->type);
    x10aux::Notifier n = x10aux::DeserializationDispatcher::getPutNotifier(sid);
    n(buf,len);
    assert(buf.consumed() <= p->len);
    deserialized_bytes += buf.consumed()  ; asyncs_received++;
}

static void cuda_finished_put (const x10rt_msg_params *p, x10aux::copy_sz len) {
    _X_(ANSI_X10RT<<"Receiving a put, deserialising for cuda notifier..."<<ANSI_RESET);
    x10aux::deserialization_buffer buf(static_cast<char*>(p->msg));
    // note: high bytes thrown away in implicit conversion
    serialization_id_t sid = x10aux::DeserializationDispatcher::getSerializationId(p->type);
    x10aux::Notifier n = x10aux::DeserializationDispatcher::getCUDAPutNotifier(sid);
    n(buf,len);
    assert(buf.consumed() <= p->len);
    deserialized_bytes += buf.consumed()  ; asyncs_received++;
}

x10aux::msg_type x10aux::register_put_handler () {
    return x10rt_register_put_receiver(receive_put, finished_put,
                                       cuda_receive_put, cuda_finished_put);
}

static void *receive_get (const x10rt_msg_params *p, x10aux::copy_sz len) {
    _X_(ANSI_X10RT<<"Receiving a get, deserialising for buffer finder..."<<ANSI_RESET);
    x10aux::deserialization_buffer buf(static_cast<char*>(p->msg));
    // note: high bytes thrown away in implicit conversion
    serialization_id_t sid = x10aux::DeserializationDispatcher::getSerializationId(p->type);
    x10aux::BufferFinder bf = x10aux::DeserializationDispatcher::getGetBufferFinder(sid);
    void *dropzone = bf(buf,len);
    assert(buf.consumed() <= p->len);
    deserialized_bytes += buf.consumed()  ; asyncs_received++;
    return dropzone;
}

static void *cuda_receive_get (const x10rt_msg_params *p, x10aux::copy_sz len) {
    _X_(ANSI_X10RT<<"Receiving a get, deserialising for cuda buffer finder..."<<ANSI_RESET);
    x10aux::deserialization_buffer buf(static_cast<char*>(p->msg));
    // note: high bytes thrown away in implicit conversion
    serialization_id_t sid = x10aux::DeserializationDispatcher::getSerializationId(p->type);
    x10aux::BufferFinder bf = x10aux::DeserializationDispatcher::getCUDAGetBufferFinder(sid);
    void *dropzone = bf(buf,len);
    assert(buf.consumed() <= p->len);
    deserialized_bytes += buf.consumed()  ; asyncs_received++;
    return dropzone;
}

static void finished_get (const x10rt_msg_params *p, x10aux::copy_sz len) {
    _X_(ANSI_X10RT<<"Receiving a get, deserialising for notifier..."<<ANSI_RESET);
    x10aux::deserialization_buffer buf(static_cast<char*>(p->msg));
    // note: high bytes thrown away in implicit conversion
    serialization_id_t sid = x10aux::DeserializationDispatcher::getSerializationId(p->type);
    x10aux::Notifier n = x10aux::DeserializationDispatcher::getGetNotifier(sid);
    n(buf,len);
    assert(buf.consumed() <= p->len);
    deserialized_bytes += buf.consumed()  ; asyncs_received++;
}

static void cuda_finished_get (const x10rt_msg_params *p, x10aux::copy_sz len) {
    _X_(ANSI_X10RT<<"Receiving a get, deserialising for cuda notifier..."<<ANSI_RESET);
    x10aux::deserialization_buffer buf(static_cast<char*>(p->msg));
    // note: high bytes thrown away in implicit conversion
    serialization_id_t sid = x10aux::DeserializationDispatcher::getSerializationId(p->type);
    x10aux::Notifier n = x10aux::DeserializationDispatcher::getCUDAGetNotifier(sid);
    n(buf,len);
    assert(buf.consumed() <= p->len);
    deserialized_bytes += buf.consumed()  ; asyncs_received++;
}

x10aux::msg_type x10aux::register_get_handler (void) {
    return x10rt_register_get_receiver(receive_get, finished_get,
                                       cuda_receive_get, cuda_finished_get);
}

void x10aux::cuda_put (place gpu, x10_ulong addr, void *var, size_t sz)
{
    bool finished = false;
    x10aux::serialization_buffer buf;
    buf.write((x10_ulong)(size_t)&finished);
    buf.write(addr);
    size_t len = buf.length();
    x10rt_msg_params p = {gpu, kernel_put, buf.borrow(), len};
    x10rt_send_put(&p, var, sz);
    while (!finished) x10rt_probe();
}


// vim:tabstop=4:shiftwidth=4:expandtab
