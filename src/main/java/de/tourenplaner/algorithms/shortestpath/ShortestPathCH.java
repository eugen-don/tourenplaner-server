/*
 * Copyright 2012 ToureNPlaner
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package de.tourenplaner.algorithms.shortestpath;

import com.carrotsearch.hppc.BitSet;
import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.cursors.IntCursor;
import de.tourenplaner.algorithms.ComputeException;
import de.tourenplaner.algorithms.DijkstraStructs;
import de.tourenplaner.algorithms.Heap;
import de.tourenplaner.computecore.RequestPoints;
import de.tourenplaner.computecore.Way;
import de.tourenplaner.graphrep.GraphRep;

import java.util.List;
import java.util.logging.Logger;

/**
 * Shortest Path Algorithm taking advantage of Contraction Hierarchies
 *
 * @author Christoph Haag, Sascha Meusel, Niklas Schnelle, Peter Vollmer
 */
public class ShortestPathCH extends ShortestPath {

    private static Logger log = Logger.getLogger("de.tourenplaner.algorithms");

    // Variables used for debugging/diagnosing
    int bfsNodes = 0;
    int bfsEdges = 0;

    // DijkstraStructs used by the ShortestPathCH
    private final DijkstraStructs ds;

    public ShortestPathCH(GraphRep graph, DijkstraStructs resourceSharer) {
        super(graph);
        ds = resourceSharer;
    }

    /**
     * Marks the G_down edges by doing a BFS from the target node for
     * consideration !G_down edges are always before G_up edges! That's why we
     * can break the inner loop early
     *
     * @param markedEdges
     * @param targetId
     * @throws IllegalAccessException
     */
    public final void bfsMark(BitSet markedEdges, int targetId) throws IllegalAccessException {
        int edgeId;
        int currNode;
        int sourceNode;
        int targetRank;
        IntArrayDeque deque = ds.borrowDeque();
        BitSet visited = ds.borrowVisitedSet();
        deque.addLast(targetId);
        visited.set(targetId);
        while (!deque.isEmpty()) {
            currNode = deque.removeLast();
            targetRank = graph.getRank(currNode);
            bfsNodes++;
            Inner:
            for (int i = 0; i < graph.getInEdgeCount(currNode); i++) {
                edgeId = graph.getInEdgeId(currNode, i);
                sourceNode = graph.getSource(edgeId);
                // Check if G_down
                if (targetRank <= graph.getRank(sourceNode)) {
                    bfsEdges++;
                    // Mark the edge
                    markedEdges.set(edgeId);
                    if (!visited.get(sourceNode)) {
                        visited.set(sourceNode);
                        // Add source for exploration
                        deque.addFirst(sourceNode);
                    }
                } else {
                    break Inner;
                }
            }
        }
        ds.returnDeque();
        ds.returnVisitedSet();
    }

    /**
     * Marks the G_down edges by doing a BFS like search for G_down edges from all target nodes
     * consideration !G_down edges are always before G_up edges! That's why we
     * can break the inner loop early
     *
     * @param markedEdges
     * @param deque       filled with the target nodes
     * @throws IllegalAccessException
     */
    public final void bfsMarkAll(BitSet markedEdges, IntArrayDeque deque) throws IllegalAccessException {
        int edgeId;
        int currNode;
        int sourceNode;
        int targetRank;
        BitSet visited = ds.borrowVisitedSet();
        // Set all nodes already in the deque as visited
        for (IntCursor nodeId : deque) {
            visited.set(nodeId.value);
        }
        while (!deque.isEmpty()) {
            currNode = deque.removeLast();
            targetRank = graph.getRank(currNode);
            bfsNodes++;
            Inner:
            for (int i = 0; i < graph.getInEdgeCount(currNode); i++) {
                edgeId = graph.getInEdgeId(currNode, i);
                sourceNode = graph.getSource(edgeId);
                // Check if G_down
                if (graph.getRank(sourceNode) >= targetRank) {
                    bfsEdges++;
                    // Mark the edge
                    markedEdges.set(edgeId);
                    if (!visited.get(sourceNode)) {
                        visited.set(sourceNode);
                        // Add source for exploration
                        deque.addFirst(sourceNode);
                    }
                } else {
                    break Inner;
                }
            }
        }
        ds.returnVisitedSet();
    }


    /**
     * Performs the Dijkstra Search on E_up U E_marked stopping when
     * the destination point is removed from the pq
     *
     * @param markedEdges
     * @param dists
     * @param srcId
     * @param destId
     * @return
     * @throws IllegalAccessException
     */
    public final boolean dijkstraStopAtDest(int[] dists, int[] prevEdges, BitSet markedEdges, int srcId, int destId) throws IllegalAccessException {
        dists[srcId] = 0;
        Heap heap = ds.borrowHeap();
        heap.insert(srcId, dists[srcId]);

        int nodeDist;
        int edgeId;
        int tempDist;
        int targetNode;
        int nodeId = srcId;
        int sourceRank;
        DIJKSTRA:
        while (!heap.isEmpty()) {
            nodeId = heap.peekMinId();
            nodeDist = heap.peekMinDist();
            sourceRank = graph.getRank(nodeId);
            heap.removeMin();
            if (nodeId == destId) {
                break DIJKSTRA;
            } else if (nodeDist > dists[nodeId]) {
                continue;
            }
            for (int i = 0; i < graph.getOutEdgeCount(nodeId); i++) {
                edgeId = graph.getOutEdgeId(nodeId, i);
                targetNode = graph.getTarget(edgeId);
                // Either marked (by BFS) or G_up edge
                if (markedEdges.get(edgeId) || (sourceRank <= graph.getRank(targetNode))) {

                    tempDist = dists[nodeId] + graph.getDist(edgeId);

                    if (tempDist < dists[targetNode]) {
                        dists[targetNode] = tempDist;

                        prevEdges[targetNode] = edgeId;
                        heap.insert(targetNode, tempDist);
                    }

                }
            }
        }
        ds.returnHeap();
        return nodeId == destId;
    }

    /**
     * Performs the Dijkstra Search on E_up U E_marked stopping when the pq is
     * empty without setting predecessor edges
     *
     * @param markedEdges
     * @param dists
     * @param srcId
     * @throws IllegalAccessException
     */
    public final void dijkstraStopAtEmptyDistOnly(int[] dists, BitSet markedEdges, int srcId) throws IllegalAccessException {
        dists[srcId] = 0;
        Heap heap = ds.borrowHeap();
        heap.insert(srcId, dists[srcId]);

        int nodeDist;
        int edgeId;
        int tempDist;
        int targetNode;
        int sourceRank;
        int nodeId = srcId;
        while (!heap.isEmpty()) {
            nodeId = heap.peekMinId();
            nodeDist = heap.peekMinDist();
            sourceRank = graph.getRank(nodeId);
            heap.removeMin();
            if (nodeDist > dists[nodeId]) {
                continue;
            }
            int edgeCount = graph.getOutEdgeCount(nodeId);
            for (int i = 0; i < edgeCount; i++) {
                edgeId = graph.getOutEdgeId(nodeId, i);
                targetNode = graph.getTarget(edgeId);

                // Either marked (by BFS) or G_up edge
                if (markedEdges.get(edgeId) || (sourceRank <= graph.getRank(targetNode))) {

                    tempDist = dists[nodeId] + graph.getDist(edgeId);

                    if (tempDist < dists[targetNode]) {
                        dists[targetNode] = tempDist;
                        heap.insert(targetNode, tempDist);
                    }

                }
            }
        }
        ds.returnHeap();
        return;
    }

    /**
     * Backtracks the prevEdges Array and calculates the actual path length
     * returns the length of the found path in meters
     *
     * @param prevEdges
     * @param resultWay
     * @param srcId
     * @param destId
     * @throws IllegalAccessException
     */
    public final void backtrack(int[] prevEdges, Way resultWay, int srcId, int destId) throws IllegalAccessException {
        int nodeLat;
        int nodeLon;
        int edgeId;
        int length = 0;
        // backtracking and shortcut unpacking use dequeue as stack
        IntArrayDeque deque = ds.borrowDeque();

        // Add the edges to our unpacking stack we
        // go from destination to start so add at the end of the deque
        int currNode = destId;
        int shortedEdge1, shortedEdge2;

        while (currNode != srcId) {
            edgeId = prevEdges[currNode];
            deque.addFirst(edgeId);
            currNode = graph.getSource(edgeId);
        }

        // Unpack shortcuts "recursively"
        log.finer("Start backtrack with " + deque.size() + " edges");
        while (!deque.isEmpty()) {
            // Get the top edge and check if it's a shortcut that needs
            // further
            // unpacking
            edgeId = deque.removeFirst();
            shortedEdge1 = graph.getFirstShortcuttedEdge(edgeId);
            if (shortedEdge1 >= 0) {
                // We have a shortcut unpack it
                shortedEdge2 = graph.getSecondShortcuttedEdge(edgeId);
                deque.addFirst(shortedEdge2);
                deque.addFirst(shortedEdge1);
            } else {
                // No shortcut remember it
                currNode = graph.getSource(edgeId);
                nodeLat = graph.getLat(currNode);
                nodeLon = graph.getLon(currNode);
                resultWay.addPoint(nodeLat, nodeLon);
                length += graph.getEuclidianDist(edgeId);
            }
        }
        // Add destination node
        nodeLat = graph.getLat(destId);
        nodeLon = graph.getLon(destId);
        resultWay.addPoint(nodeLat, nodeLon);

        resultWay.setDistance(length);
        ds.returnDeque();
        return;
    }

    /**
     * Computes the shortest path over points[0] -> points[1] -> points[2]...
     * and stores all points on the path in resultWays
     *
     * @param points     The points the route should span, the id's must have been set
     * @param resultWays
     * @param tour       Specifies whether this is a tour and points[n]->points[0]
     * @return
     * @throws IllegalAccessException
     * @throws Exception
     */
    public int shortestPath(RequestPoints points, List<Way> resultWays, boolean tour) throws ComputeException, IllegalAccessException {

        int srcId = 0;
        int destId = 0;

        int distance = 0;
        int totalDistance = 0;
        // in meters
        double directDistance = 0.0;

        for (int pointIndex = 0; pointIndex < points.size(); pointIndex++) {

            // New Dijkstra need to reset
            long starttime = System.nanoTime();
            srcId = points.getPointId(pointIndex);
            if (pointIndex < points.size() - 1) {
                destId = points.getPointId(pointIndex + 1);
            } else if (tour) {
                destId = points.getPointId(0);
            } else {
                break;
            }

            //New point -> new subway
            resultWays.add(new Way());

            // get data structures used by Dijkstra
            int[] dists = ds.borrowDistArray();
            int[] prevEdges = ds.borrowPrevArray();
            BitSet markedEdges = ds.borrowMarkedSet();

            directDistance += calcDirectDistance(graph.getLat(srcId) / 10000000.0, (double) graph.getLon(srcId) / 10000000, (double) graph.getLat(destId) / 10000000, (double) graph.getLon(destId) / 10000000);

            // Do our BFS marking at the destination
            bfsMark(markedEdges, destId);
            long bfsdonetime = System.nanoTime();

            // Run Dijkstra stopping when destId is removed from the pq
            boolean found = dijkstraStopAtDest(dists, prevEdges, markedEdges, srcId, destId);
            long dijkstratime = System.nanoTime();

            if (!found) {
                // Return/Reset the data structures
                ds.returnDistArray(false);
                ds.returnPrevArray();
                ds.returnMarkedSet();
                log.info("There is no path from src to trgt (" + srcId + " to " + destId + ")");
                throw new ComputeException("No Path found");
            }
            // Backtrack to get the actual path
            backtrack(prevEdges, resultWays.get(pointIndex), srcId, destId);
            resultWays.get(pointIndex).setTravelTime(dists[destId] * graph.travelTimeConstant);
            distance = resultWays.get(pointIndex).getDistance();
            totalDistance += distance;

            long backtracktime = System.nanoTime();

            // Save the distance to the last point at the target
            // wrap around at tour
            points.getConstraints((pointIndex + 1) % points.size()).put("distToPrev", distance);
            points.getConstraints((pointIndex + 1) % points.size()).put("timeToPrev", resultWays.get(pointIndex).getTravelTime());


            log.info("found sp with dist = " + resultWays.get(pointIndex).getDistance() / 1000.0 + " km (direct distance: " + directDistance / 1000.0 + " dist[destid] = " + dists[destId] + "\n" +
                    "BFS: " + (bfsdonetime - starttime) / 1000000.0 + " ms with " + bfsNodes + " nodes and " + bfsEdges + " edges\n" + "Dijkstra: " + (dijkstratime - bfsdonetime) / 1000000.0 + " ms\n" + "Backtracking: " + (backtracktime - dijkstratime) / 1000000.0 + " ms");


            // Return/Reset the data structures
            ds.returnDistArray(false);
            ds.returnPrevArray();
            ds.returnMarkedSet();
        }

        return totalDistance;
    }

}
