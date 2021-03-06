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

package de.tourenplaner.graphrep;

import com.carrotsearch.hppc.IntArrayList;

import java.util.logging.Logger;

/**
 * @author Christoph Haag, Sascha Meusel, Niklas Schnelle, Peter Vollmer
 */
public class GridNN implements NNSearcher {
    private static Logger log = Logger.getLogger("de.tourenplaner.graphrep");
    private static final long serialVersionUID = 1L;
    private static final int numberOfColumns = 400;
    private int numRows;
    private int numCols;

    private final DumbNN fallback;
    private final GraphRep graphRep;
    private final IntArrayList[] grid;
    private final int latMin, latDiff;
    private final int lonMin, lonDiff;


    private int mapLat(int value) {
        return (int)((long)(value - latMin) * (long)(numRows - 1) / (long) latDiff);
    }

    private int mapLon(int value) {
        return (int) ((long) (value - lonMin) * (long) (numCols - 1) / (long) lonDiff);
    }

    private int coordsToIndex(int lat, int lon) {
        return mapLat(lat) * numCols + mapLon(lon);
    }

    private int toIndex(int row, int col) {
        return row * numCols + col;
    }

    public GridNN(GraphRep graph) {
        this.graphRep = graph;
        fallback = new DumbNN(graphRep);

        // Find biggest and smallest elements
        // needed for mapping values into the grid
        int latMin = Integer.MAX_VALUE;
        int lonMin = Integer.MAX_VALUE;
        int latMax = Integer.MIN_VALUE;
        int lonMax = Integer.MIN_VALUE;

        int numNodes = graphRep.getNodeCount();
        int curr;

        // Go lat's and lon's one after the other to access more
        // sequentially
        for (int i = 0; i < numNodes; i++) {
            curr = graphRep.getLat(i);
            if (curr < latMin) {
                latMin = curr;
            } else if (curr > latMax) {
                latMax = curr;
            }
        }
        for (int i = 0; i < numNodes; i++) {
            curr = graphRep.getLon(i);
            if (curr < lonMin) {
                lonMin = curr;
            } else if (curr > lonMax) {
                lonMax = curr;
            }
        }

        this.latMin = latMin;
        this.latDiff = latMax - latMin;
        this.lonMin = lonMin;
        this.lonDiff = lonMax - lonMin;
        log.info("Min latitude: "+latMin+" max latitude: "+latMax+
        "\nmin longitude: "+lonMin+" max longitude: "+lonMax);

        // Calculate numRows and numCols to match the geometry of our map
        // only an optimization not needed for correctness
        this.numCols = numberOfColumns;
        this.numRows = (int)(((double)numCols)*((((double)latDiff))/((double)lonDiff)));

        log.info("Using Grid of "+numRows+" * "+numCols);

        grid = new IntArrayList[numRows * numCols];

        // Insert all nodes
        int lat, lon, index;
        IntArrayList list;
        for (int i = 0; i < numNodes; i++) {
            lat = graphRep.getLat(i);
            lon = graphRep.getLon(i);
            index = coordsToIndex(lat, lon);
            list = grid[index];

            if (list == null) {
                list = new IntArrayList(10);
                grid[index] = list;
            }
            list.add(i);
        }

        // Let's trim the Lists to their internal size
        for(int i = 0; i < grid.length; i++){
            list = grid[i];
            if (list != null)
                list.trimToSize();
        }
    }

    /**
     * @param lat
     * @param lon
     * @return
     * @see de.tourenplaner.graphrep.NNSearcher
     */
    @Override
    public int getIDForCoordinates(int lat, int lon) {
        final int row = mapLat(lat);
        final int col = mapLon(lon);
        // Need to search the exact cell and all around it
        // because lat,lon can be out of the range of stored coordinates
        // make sure we search a cell at all
        final int upper = (row - 1 > 0) ? row - 1 : 0;
        final int lower = (row + 2 < numRows) ? upper + 3 : numRows;
        final int left = (col - 1 > 0) ? col - 1 : 0;
        final int right = (col + 2 < numCols) ? left + 3 : numCols;

        long minDist = Long.MAX_VALUE;
        long dist;
        int nodeId, minNodeId=0;
        IntArrayList list;
        for (int i = upper; i < lower; i++) {
            Inner: for (int j = left; j < right; j++) {
                list = grid[toIndex(i,j)];
                if (list == null)
                    continue Inner;

                for (int index = 0; index < list.size(); index++){
                    nodeId = list.get(index);
                    dist = sqDistToCoords(nodeId, lat, lon);
                    if (dist < minDist){
                        minDist = dist;
                        minNodeId = nodeId;
                    }
                }
            }
        }
        // If every list was null fallback
        if (minDist == Long.MAX_VALUE){
            log.warning("Fell back to dumbNN for "+lat+","+lon);
            minNodeId = fallback.getIDForCoordinates(lat, lon);
        }
        return minNodeId;

    }

    private final long sqDistToCoords(int nodeID, int lat, int lon) {
        return ((long) (graphRep.getLat(nodeID) - lat)) * ((long) (graphRep.getLat(nodeID) - lat)) + ((long) (graphRep.getLon(nodeID) - lon)) * ((long) (graphRep.getLon(nodeID) - lon));
    }

}
