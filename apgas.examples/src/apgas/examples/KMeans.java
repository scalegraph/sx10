/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2014.
 */

package apgas.examples;

import static apgas.Constructs.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

import apgas.Configuration;
import apgas.Place;
import apgas.util.GlobalRef;

/**
 * A formulation of distributed KMeans using coarse-grained asyncs to implement
 * an allreduce pattern for cluster centers and counts.
 *
 * For a highly optimized and scalable, version of this benchmark see KMeans.x10
 * in the X10 Benchmarks (separate download from x10-lang.org)
 */
public class KMeans {
  static int DIM = 4;
  static int CLUSTERS = 5;
  static final int DEFAULT_PLACES = 4;
  static final int DEFAULT_THREADS = 2;

  static class ClusterState implements Serializable {
    private static final long serialVersionUID = 1862268388246760008L;

    float[][] clusters = new float[CLUSTERS][DIM];
    int[] clusterCounts = new int[CLUSTERS];
  }

  public static void main(String[] args) {
    if (System.getProperty(Configuration.APGAS_PLACES) == null) {
      System.setProperty(Configuration.APGAS_PLACES,
          String.valueOf(DEFAULT_PLACES));
    }

    if (System.getProperty(Configuration.APGAS_THREADS) == null) {
      System.setProperty(Configuration.APGAS_THREADS,
          String.valueOf(DEFAULT_THREADS));
    }

    final int iterations = args.length > 1 ? Integer.parseInt(args[1]) : 50;
    final int numPoints = args.length > 0 ? Integer.parseInt(args[0]) : 2000000;

    System.out.println("Warmup...");

    KMeans.run(numPoints, iterations, true);
    KMeans.run(numPoints, iterations, false);
  }

  public static void run(int numPoints, int iterations, boolean warmup) {
    if (!warmup) {
      System.out
          .printf(
              "Resilient K-Means: %d clusters, %d points, %d dimensions, %d places, %d threads\n",
              CLUSTERS, numPoints, DIM,
              Integer.valueOf(System.getProperty(Configuration.APGAS_PLACES)),
              Integer.valueOf(System.getProperty(Configuration.APGAS_THREADS)));
    }

    final GlobalRef<ClusterState> globalClusterState = new GlobalRef<ClusterState>(
        places(), () -> {
          return new ClusterState();
        });
    final GlobalRef<float[][]> globalCurrentClusters = new GlobalRef<float[][]>(
        places(), () -> {
          return new float[CLUSTERS][DIM];
        });
    final GlobalRef<float[][]> globalPoints = new GlobalRef<float[][]>(
        places(),
        () -> {
          final Random rand = new Random(here().id);
          final float[][] localPoints = new float[numPoints / places().size()][DIM];
          for (int i = 0; i < numPoints / places().size(); i++) {
            for (int j = 0; j < DIM; j++) {
              localPoints[i][j] = rand.nextFloat();
            }
          }

          return localPoints;
        });

    final ClusterState centralClusterState = new ClusterState();
    final GlobalRef<ClusterState> centralClusterStateGr = new GlobalRef<ClusterState>(
        centralClusterState);
    final float[][] centralCurrentClusters = new float[CLUSTERS][DIM];

    // arbitrarily initialize central clusters to first few points
    for (int i = 0; i < CLUSTERS; i++) {
      for (int j = 0; j < DIM; j++) {
        centralCurrentClusters[i][j] = globalPoints.get()[i][j];
      }
    }

    long time = System.nanoTime();
    int iter;
    for (iter = 1; iter <= iterations; iter++) {
      if (!warmup) {
        System.out.print(".");
      }

      finish(() -> {
        for (final Place place : places()) {
          asyncAt(
              place,
              () -> {

                final float[][] currentClusters = globalCurrentClusters.get();
                for (int i = 0; i < CLUSTERS; i++) {
                  for (int j = 0; j < DIM; j++) {
                    currentClusters[i][j] = centralCurrentClusters[i][j];
                  }
                }

                final ClusterState clusterState = globalClusterState.get();
                final float[][] newClusters = clusterState.clusters;
                for (int i = 0; i < CLUSTERS; i++) {
                  Arrays.fill(newClusters[i], 0.0f);
                }
                final int[] clusterCounts = clusterState.clusterCounts;
                Arrays.fill(clusterCounts, 0);

                /* compute new clusters and counters */
                final float[][] points = globalPoints.get();

                for (int p = 0; p < points.length; p++) {
                  int closest = -1;
                  float closestDist = Float.MAX_VALUE;
                  for (int k = 0; k < CLUSTERS; k++) {
                    float dist = 0;
                    for (int d = 0; d < DIM; d++) {
                      final double tmp = points[p][d] - currentClusters[k][d];
                      dist += tmp * tmp;
                    }
                    if (dist < closestDist) {
                      closestDist = dist;
                      closest = k;
                    }
                  }

                  for (int d = 0; d < DIM; d++) {
                    newClusters[closest][d] += points[p][d];
                  }
                  clusterCounts[closest]++;
                }

                asyncAt(
                    centralClusterStateGr.home(),
                    () -> {
                      // combine place clusters to central
                      final float[][] centralNewClusters = centralClusterStateGr
                          .get().clusters;
                      synchronized (centralNewClusters) {
                        for (int i = 0; i < CLUSTERS; i++) {
                          for (int j = 0; j < DIM; j++) {
                            centralNewClusters[i][j] += newClusters[i][j];
                          }
                        }
                      }
                      final int[] centralClusterCounts = centralClusterStateGr
                          .get().clusterCounts;
                      synchronized (centralClusterCounts) {
                        for (int j = 0; j < CLUSTERS; j++) {
                          centralClusterCounts[j] += clusterCounts[j];
                        }
                      }
                    });
              });
        }
      });

      for (int k = 0; k < CLUSTERS; k++) {
        for (int d = 0; d < DIM; d++) {
          centralClusterState.clusters[k][d] /= centralClusterState.clusterCounts[k];
        }
      }

      // TEST FOR CONVERGENCE
      boolean b = true;
      for (int i = 0; i < CLUSTERS; i++) {
        for (int j = 0; j < DIM; j++) {
          if (Math.abs(centralCurrentClusters[i][j]
              - centralClusterState.clusters[i][j]) > 0.0001) {
            b = false;
            break;
          }
        }
      }

      for (int i = 0; i < CLUSTERS; i++) {
        for (int j = 0; j < DIM; j++) {
          centralCurrentClusters[i][j] = centralClusterState.clusters[i][j];
        }
      }

      if (b) {
        break;
      }

      for (int i = 0; i < CLUSTERS; i++) {
        Arrays.fill(centralClusterState.clusters[i], 0.0f);
      }
      Arrays.fill(centralClusterState.clusterCounts, 0);
    }
    time = System.nanoTime() - time;

    if (!warmup) {
      System.out.println();
      for (int d = 0; d < DIM; d++) {
        for (int k = 0; k < CLUSTERS; k++) {
          if (k > 0) {
            System.out.print(" ");
          }
          System.out.print(centralCurrentClusters[k][d]);
        }
        System.out.println();
      }

      System.out.printf("time per iteration %.3f ms\n", time / 1e6 / iter);
    }
  }
}
