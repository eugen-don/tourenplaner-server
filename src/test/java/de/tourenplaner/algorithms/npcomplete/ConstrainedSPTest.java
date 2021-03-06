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

package de.tourenplaner.algorithms.npcomplete;

import de.tourenplaner.algorithms.GraphAlgorithmFactory;
import de.tourenplaner.computecore.RequestPoints;
import de.tourenplaner.computecore.Way;
import de.tourenplaner.graphrep.GraphRep;
import de.tourenplaner.graphrep.GraphRepReader;
import de.tourenplaner.graphrep.GraphRepTextReader;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Christoph Haag, Sascha Meusel, Niklas Schnelle, Peter Vollmer
 */
public class ConstrainedSPTest {
    @Test
    public void testCompute() throws Exception {
        GraphRepReader graphRepTextReader = new GraphRepTextReader();

      String testFile = new String("8\n18\n10000000 10000000 20 0\n20000000 20000000 5 0\n20000000 10000000 10 0\n10000000 20000000 100 0\n30000000 10000000 40 0\n30000000 20000000 45 0\n20000000 30000000 30 0\n10000000 30000000 50 0\n0 1 0 4 -1 -1\n0 2 0 2 -1 -1\n0 3 0 3 -1 -1\n1 0 0 4 -1 -1\n1 6 0 2 -1 -1\n2 0 0 2 -1 -1\n2 4 0 3 -1 -1\n3 0 0 3 -1 -1\n3 7 0 4 -1 -1\n4 2 0 2 -1 -1\n4 5 0 4 -1 -1\n5 4 0 4 -1 -1\n5 7 0 5 -1 -1\n6 1 0 2 -1 -1\n6 7 0 3 -1 -1\n7 6 0 3 -1 -1\n7 5 0 5 -1 -1\n7 3 0 4 -1 -1");
        byte[] testFileBytes = testFile.getBytes();
        ByteArrayInputStream testFileByteArrayStream = new ByteArrayInputStream(testFileBytes);
        GraphRep graphRep = graphRepTextReader.createGraphRep(testFileByteArrayStream);
        GraphAlgorithmFactory fac = new ConstrainedSPFactory(graphRep);
        ConstrainedSP constrainedSP = (ConstrainedSP)fac.createAlgorithm();

        RequestPoints points = new RequestPoints();
        points.addPoint(10000000,10000000);
        points.addPoint(10000000,30000000);

        //shortest path is the best fittest

        Way resultWay = new Way();
        int altitudeDiff = constrainedSP.cSP(points,resultWay,90);
        assertEquals(80,altitudeDiff);
        assertEquals(7, resultWay.getDistance());
        assertEquals(10000000,resultWay.getPointLat(0));
        assertEquals(10000000,resultWay.getPointLon(0));
        assertEquals(10000000,resultWay.getPointLat(1));
        assertEquals(20000000,resultWay.getPointLon(1));
        assertEquals(10000000,resultWay.getPointLat(2));
        assertEquals(30000000,resultWay.getPointLon(2));

        //middle way

        resultWay = new Way();
        altitudeDiff = constrainedSP.cSP(points,resultWay,60);
        assertEquals(45,altitudeDiff);
        assertEquals(9, resultWay.getDistance());
        assertEquals(10000000,resultWay.getPointLat(0));
        assertEquals(10000000,resultWay.getPointLon(0));
        assertEquals(20000000,resultWay.getPointLat(1));
        assertEquals(20000000,resultWay.getPointLon(1));
        assertEquals(20000000,resultWay.getPointLat(2));
        assertEquals(30000000,resultWay.getPointLon(2));
        assertEquals(10000000,resultWay.getPointLat(3));
        assertEquals(30000000,resultWay.getPointLon(3));

        //longest possible way

        resultWay = new Way();
        altitudeDiff = constrainedSP.cSP(points,resultWay,44);
        assertEquals(40,altitudeDiff);
        assertEquals(14, resultWay.getDistance());
        assertEquals(10000000,resultWay.getPointLat(0));
        assertEquals(10000000,resultWay.getPointLon(0));
        assertEquals(20000000,resultWay.getPointLat(1));
        assertEquals(10000000,resultWay.getPointLon(1));
        assertEquals(30000000,resultWay.getPointLat(2));
        assertEquals(10000000,resultWay.getPointLon(2));
        assertEquals(30000000,resultWay.getPointLat(3));
        assertEquals(20000000,resultWay.getPointLon(3));
        assertEquals(10000000,resultWay.getPointLat(4));
        assertEquals(30000000,resultWay.getPointLon(4));

        //no possible way with a constraint. There should be the way with the least difference of altitude
        resultWay = new Way();
        altitudeDiff = constrainedSP.cSP(points,resultWay,30);
        assertEquals(40,altitudeDiff);
        assertEquals(14, resultWay.getDistance());
        assertEquals(10000000,resultWay.getPointLat(0));
        assertEquals(10000000,resultWay.getPointLon(0));
        assertEquals(20000000,resultWay.getPointLat(1));
        assertEquals(10000000,resultWay.getPointLon(1));
        assertEquals(30000000,resultWay.getPointLat(2));
        assertEquals(10000000,resultWay.getPointLon(2));
        assertEquals(30000000,resultWay.getPointLat(3));
        assertEquals(20000000,resultWay.getPointLon(3));
        assertEquals(10000000,resultWay.getPointLat(4));
        assertEquals(30000000,resultWay.getPointLon(4));

    }
}
                            