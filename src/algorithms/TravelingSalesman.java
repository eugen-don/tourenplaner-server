package algorithms;

import com.carrotsearch.hppc.BitSet;
import com.carrotsearch.hppc.IntOpenHashSet;
import computecore.ComputeRequest;
import computecore.RequestPoint;
import computecore.RequestPoints;
import graphrep.GraphRep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Niklas Schnelle
 * Date: 12/26/11
 * Time: 4:31 PM
 */
public class TravelingSalesman extends GraphAlgorithm {

    private final DijkstraStructs ds;
    private final ShortestPathCH chdijks;

    public TravelingSalesman(GraphRep graphrep, DijkstraStructs ds) {
        super(graphrep);
        this.ds = ds;
        this.chdijks = new ShortestPathCH(graph, ds);
    }


    private int[][] computeDistMatrix(RequestPoints points) throws IllegalAccessException {
        int[][] distmat = new int[points.size()][points.size()];
        BitSet markedEdges = ds.borrowMarkedSet();

        // BfsMark all points
        for (int i = 0; i < points.size(); i++) {
            chdijks.bfsMark(markedEdges, points.getPointId(i));
        }

        // Calculate the distance matrix rows
        for (int i = 0; i < points.size(); i++) {
            int[] dists = ds.borrowDistArray();
            // Dijkstra sets dist labels
            chdijks.dijkstraStopAtEmptyDistOnly(dists, markedEdges, points.getPointId(i));
            for (int j = 0; j < points.size(); j++) {
                distmat[i][j] = dists[points.getPointId(j)];
            }
            ds.returnDistArray(false);
        }
        ds.returnMarkedSet();

        return distmat;
    }

    @Override
    public void compute(ComputeRequest req) throws ComputeException {
        // Map points to ids
        RequestPoints points = req.getPoints();
        int[][] distmat;

        // DEBUG:
        Map<String, Object> debugMat = new HashMap<String, Object>();


        try {
            // Map our requested points to ids
            points.setIdsFromGraph(graph);

            // Looks cheap but computes the n^2 matrix of distances for the given points
            distmat = computeDistMatrix(points);



            IntOpenHashSet visited = new IntOpenHashSet(points.size());
            int currIndex = 0;
            int minValue;
            int minIndex;
            List<RequestPoint> requestPointList = req.getPoints().getStore();
            List<RequestPoint> pointStore = new ArrayList<RequestPoint>(points.size());
            // add the initial point
            pointStore.add(requestPointList.get(currIndex));
            
            for (int nextIndex = 1; nextIndex < points.size(); nextIndex++) {
                visited.add(currIndex);

                minValue = Integer.MAX_VALUE;
                minIndex = 0;
                for (int i = 0; i < points.size(); i++) {
                    if (minValue > distmat[currIndex][i] && i != currIndex && !visited.contains(i)) {
                        minValue = distmat[currIndex][i];
                        minIndex = i;
                    }
                }
                currIndex = minIndex;
                pointStore.add(requestPointList.get(currIndex));
            }
            
            req.getPoints().setStore(pointStore);
            // Now build real paths
            chdijks.shortestPath(points, req.getResulWay(), true);


            debugMat.put("distMat", distmat);
            req.setMisc(debugMat);
        } catch (IllegalAccessException e) {
            throw new ComputeException("Illegal Access: " + e.getMessage());
        }
    }
}