/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2015.
 */

package apgas.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apgas.NoSuchPlaceException;
import apgas.util.GlobalID;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MapEvent;
import com.hazelcast.map.AbstractEntryProcessor;
import com.hazelcast.query.Predicate;

@SuppressWarnings("javadoc")
final class ResilientFinish implements Serializable, Finish {
  private static final long serialVersionUID = -8238404708052769991L;

  /**
   * A factory producing {@link ResilientFinish} instances.
   */
  static class Factory extends Finish.Factory {
    @Override
    ResilientFinish make(Finish parent) {
      return new ResilientFinish(parent);
    }
  }

  private static final IMap<GlobalID, State> map = GlobalRuntimeImpl
      .getRuntime().transport.<GlobalID, State> getMap("apgas:finish");

  final GlobalID id;

  static final class State implements Serializable {
    private static final long serialVersionUID = 4155719029376056951L;

    Set<Integer> deads; // places that have died during the finish
    final GlobalID pid; // parent
    Set<GlobalID> cids; // children
    Set<GlobalID> dids; // dead children
    List<SerializableThrowable> exceptions;
    final Map<Long, Integer> counts = new HashMap<Long, Integer>();
    int count; // non-zero counts
    int max; // max place encountered

    static long index(int p, int q) {
      return (((long) p) << 32) + q;
    }

    void clear(int p, int q) {
      final Integer v = counts.remove(index(p, q));
      count += (v == null ? 0 : (v == 0 ? 0 : -1));
    }

    void incr(int p, int q) {
      if (p > max) {
        max = p;
      }
      if (q > max) {
        max = q;
      }
      final int v = counts.getOrDefault(index(p, q), 0);
      counts.put(index(p, q), v + 1);
      count += (v == 0 ? 1 : (v == -1 ? -1 : 0));
    }

    void decr(int p, int q) {
      if (p > max) {
        max = p;
      }
      if (q > max) {
        max = q;
      }
      final int v = counts.getOrDefault(index(p, q), 0);
      counts.put(index(p, q), v - 1);
      count += (v == 0 ? 1 : (v == 1 ? -1 : 0));
    }

    State(GlobalID pid, int p) {
      max = p;
      this.pid = pid;
      counts.put(index(p, p), 1);
      count = 1;
    }
  }

  ResilientFinish(Finish parent) {
    final GlobalID id = new GlobalID();
    this.id = id;
    final GlobalID pid = parent instanceof ResilientFinish ? ((ResilientFinish) parent).id
        : null;
    final int here = GlobalRuntimeImpl.getRuntime().here;
    // map.set(id, new State(pid, here, p));
    executeOnKey(id, state -> {
      return new State(pid, here);
    });
    if (pid == null) {
      return;
    }
    executeOnKey(pid, state -> {
      if (state == null || state.deads != null && state.deads.contains(here)) {
        // parent finish thinks this place is dead, exit
        throw new DeadPlaceError();
      }
      if (state.dids == null || !state.dids.contains(id)) {
        if (state.cids == null) {
          state.cids = new HashSet<GlobalID>();
        }
        state.cids.add(id);
      }
      return state;
    });
  }

  static void purge(int p) {
    final int here = GlobalRuntimeImpl.getRuntime().here;
    // only process finish states for the current place and the dead place
    final Predicate<GlobalID, State> predicate = entry -> {
      return entry.getKey().home.id == here || entry.getKey().home.id == p;
    };
    for (final GlobalID id : map.keySet(predicate)) {
      executeOnKey(id, state -> {
        if (state == null) {
          // entry has been removed already, ignore
          return null;
        }
        if (state.deads == null) {
          state.deads = new HashSet<Integer>();
        }
        if (state.deads.contains(p)) {
          // death of p has already been processed
          return null;
        }
        state.deads.add(p);
        for (int i = 0; i <= state.max; i++) {
          state.clear(p, i);
          state.clear(i, p);
        }
        return state;
      });
    }
  }

  @Override
  public void submit(int p) {
    final int here = GlobalRuntimeImpl.getRuntime().here;
    if (p == here) {
      // task originated here, no transit stage
      return;
    }
    executeOnKey(id, state -> {
      if (state == null || state.deads != null && state.deads.contains(here)) {
        // finish thinks this place is dead, exit
        throw new DeadPlaceError();
      }
      if (state.deads != null && state.deads.contains(p)) {
        // source place has died, refuse task but keep place alive
        throw new NoSuchPlaceException();
      }
      state.decr(p, here);
      state.incr(here, here);
      return state;
    });
  }

  @Override
  public void spawn(int p) {
    final int here = GlobalRuntimeImpl.getRuntime().here;
    executeOnKey(id, state -> {
      if (state == null || state.deads != null && state.deads.contains(here)) {
        // finish thinks this place is dead, exit
        throw new DeadPlaceError();
      }
      if (state.deads != null && state.deads.contains(p)) {
        // destination place has died, reject task
        throw new NoSuchPlaceException();
      }
      state.incr(here, p);
      return state;
    });
  }

  @Override
  public void unspawn(int p) {
    final int here = GlobalRuntimeImpl.getRuntime().here;
    executeOnKey(id, state -> {
      if (state == null || state.deads != null && state.deads.contains(here)) {
        // finish thinks this place is dead, exit
        throw new DeadPlaceError();
      }
      if (state.deads != null && state.deads.contains(p)) {
        // destination place has died, reject task
        throw new NoSuchPlaceException();
      }
      state.decr(here, p);
      return state;
    });
  }

  @Override
  public void tell(int p) {
    final int here = GlobalRuntimeImpl.getRuntime().here;
    executeOnKey(id, state -> {
      if (state == null || state.deads != null && state.deads.contains(here)) {
        // finish thinks this place is dead, exit
        throw new DeadPlaceError();
      }
      state.decr(here, here);
      return state;
    });
  }

  @Override
  public void addSuppressed(Throwable exception) {
    final int here = GlobalRuntimeImpl.getRuntime().here;
    final SerializableThrowable t = new SerializableThrowable(exception);
    executeOnKey(id, state -> {
      if (state == null || state.deads != null && state.deads.contains(here)) {
        // finish thinks this place is dead, exit
        throw new DeadPlaceError();
      }
      if (state.exceptions == null) {
        state.exceptions = new ArrayList<SerializableThrowable>();
      }
      state.exceptions.add(t);
      return state;
    });
  }

  @FunctionalInterface
  static interface Processor extends Serializable {
    State process(State state);
  }

  static void executeOnKey(GlobalID id, Processor f) {
    try {
      final GlobalID pid = (GlobalID) map.executeOnKey(id,
          new AbstractEntryProcessor<GlobalID, State>() {
            private static final long serialVersionUID = 6777775768226692449L;

            @Override
            public GlobalID process(Map.Entry<GlobalID, State> entry) {
              final State state = f.process(entry.getValue());
              if (state == null) {
                return null;
              }
              if (state.count > 0 || state.cids != null
                  && !state.cids.isEmpty() || state.deads == null
                  || !state.deads.contains(id.home.id)) {
                // state is still useful:
                // finish is incomplete or we need to preserve its exceptions
                entry.setValue(state);
              } else {
                // finish is complete and place of finish has died, remove entry
                entry.setValue(null);
              }
              if (state.count > 0 || state.cids != null
                  && !state.cids.isEmpty()) {
                return null;
              } else {
                return state.pid;
              }
            }
          });
      if (pid == null) {
        return;
      }
      executeOnKey(pid, state -> {
        if (state == null) {
          // parent has been purged already
          // stop propagating termination
          return null;
        }
        if (state.cids != null && state.cids.contains(id)) {
          state.cids.remove(id);
        } else {
          if (state.dids == null) {
            state.dids = new HashSet<GlobalID>();
          }
          if (!state.dids.contains(id)) {
            state.dids.add(id);
          }
        }
        return state;
      });
    } catch (final DeadPlaceError | HazelcastInstanceNotActiveException e) {
      // this place is dead for the world
      System.exit(42);
    }
  }

  @Override
  public boolean isReleasable() {
    try {
      return (boolean) map.executeOnKey(id,
          new AbstractEntryProcessor<GlobalID, State>(false) {
            private static final long serialVersionUID = -7314315521004813385L;

            @Override
            public Boolean process(Map.Entry<GlobalID, State> entry) {
              final State state = entry.getValue();
              if (state == null) {
                throw new DeadPlaceError();
              }
              return state.count == 0
                  && (state.cids == null || state.cids.isEmpty());
            }
          });
    } catch (final DeadPlaceError | HazelcastInstanceNotActiveException e) {
      // this place is dead for the world
      System.exit(42);
      throw e;
    }
  }

  @Override
  public boolean block() {
    final String reg = map.addEntryListener(
        new EntryListener<GlobalID, State>() {

          @Override
          public void entryAdded(EntryEvent<GlobalID, State> event) {
          }

          @Override
          public void entryRemoved(EntryEvent<GlobalID, State> event) {
            synchronized (ResilientFinish.this) {
              ResilientFinish.this.notifyAll();
            }
          }

          @Override
          public void entryUpdated(EntryEvent<GlobalID, State> event) {
            synchronized (ResilientFinish.this) {
              ResilientFinish.this.notifyAll();
            }
          }

          @Override
          public void entryEvicted(EntryEvent<GlobalID, State> event) {
          }

          @Override
          public void mapEvicted(MapEvent event) {
          }

          @Override
          public void mapCleared(MapEvent event) {
          }
        }, id, false);
    synchronized (this) {
      while (!isReleasable()) {
        try {
          wait(1000);
        } catch (final InterruptedException e) {
        }
      }
    }
    map.removeEntryListener(reg);
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Throwable> exceptions() {
    try {
      final List<SerializableThrowable> exceptions = (List<SerializableThrowable>) map
          .executeOnKey(id, new AbstractEntryProcessor<GlobalID, State>() {
            private static final long serialVersionUID = -6913737556384372242L;

            @Override
            public List<SerializableThrowable> process(
                Map.Entry<GlobalID, State> entry) {
              final State state = entry.getValue();
              if (state == null) {
                throw new DeadPlaceError();
              }
              entry.setValue(null);
              return state.exceptions;
            }
          });
      if (exceptions == null) {
        return null;
      }
      final List<Throwable> list = new ArrayList<Throwable>();
      for (final SerializableThrowable t : exceptions) {
        list.add(t.t);
      }
      return list;
    } catch (final DeadPlaceError | HazelcastInstanceNotActiveException e) {
      // this place is dead for the world
      System.exit(42);
      throw e;
    }
  }
}
