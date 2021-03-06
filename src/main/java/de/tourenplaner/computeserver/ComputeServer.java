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

package de.tourenplaner.computeserver;

import de.tourenplaner.algorithms.AlgorithmFactory;
import de.tourenplaner.algorithms.GraphAlgorithmFactory;
import de.tourenplaner.algorithms.NNSearchFactory;
import de.tourenplaner.algorithms.bbbundle.BBBundleFactory;
import de.tourenplaner.algorithms.coregraph.CoreGraphFactory;
import de.tourenplaner.algorithms.coregraph.UpDownFactory;
import de.tourenplaner.algorithms.coregraph.WayByNodeIdsFactory;
import de.tourenplaner.algorithms.drawcore.DrawCoreFactory;
import de.tourenplaner.algorithms.npcomplete.TravelingSalesmenFactory;
import de.tourenplaner.algorithms.shortestpath.ShortestPathBDCHFactory;
import de.tourenplaner.computecore.AlgorithmManagerFactory;
import de.tourenplaner.computecore.AlgorithmRegistry;
import de.tourenplaner.computecore.ComputeCore;
import de.tourenplaner.computecore.SharingAMFactory;
import de.tourenplaner.config.ConfigManager;
import de.tourenplaner.graphrep.*;

import java.io.*;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.XMLFormatter;

/**
 * @author Christoph Haag, Sascha Meusel, Niklas Schnelle, Peter Vollmer
 */
public class ComputeServer {

    private static void registerAlgorithms(AlgorithmRegistry reg, GraphRep graph) {
        // reg.registerAlgorithm(new ShortestPathFactory(graph));
        reg.registerAlgorithm(new TravelingSalesmenFactory(graph));
        reg.registerAlgorithm(new ShortestPathBDCHFactory(graph));
        //reg.registerAlgorithm(new ShortestPathCHFactory(graph));
        reg.registerAlgorithm(new NNSearchFactory(graph));
        //reg.registerAlgorithm(new ConstrainedSPFactory(graph));
        reg.registerAlgorithm(new UpDownFactory(graph));
        reg.registerAlgorithm(new WayByNodeIdsFactory(graph));
        reg.registerAlgorithm(new CoreGraphFactory(graph));
        reg.registerAlgorithm(new BBBundleFactory(graph));
        reg.registerAlgorithm(new DrawCoreFactory(graph));
    }

    private static Logger log = Logger.getLogger("de.tourenplaner");

    /**
     * @param graphName Original file name of the graph
     * @return The filename of the dumped graph
     */
    private static String dumpName(String graphName) {
        return graphName + ".dat";
    }

    private static Map<String, Object> getServerInfo(AlgorithmRegistry reg) {
        Map<String, Object> info = new HashMap<String, Object>(4);
        info.put("version", new Float(1.0));
        info.put("servertype", ConfigManager.getInstance().getEntryBool("private", false) ? "private" : "public");

        // when serverinfosslport is available then put it in the serverinfo, instead of the sslport we really use
        int sslport = ConfigManager.getInstance().isEntryAvailable("serverinfosslport") ?
                ConfigManager.getInstance().getEntryInt("sslport", 8081) :
                ConfigManager.getInstance().getEntryInt("serverinfosslport", 8081);

        info.put("sslport", sslport);
        // Enumerate Algorithms
        Collection<AlgorithmFactory> algs = reg.getAlgorithms();
        Map<String, Object> algInfo;
        List<Map<String, Object>> algList = new ArrayList<Map<String, Object>>();
        for (AlgorithmFactory alg : algs) {
            algInfo = new HashMap<String, Object>(5);
            algInfo.put("version", alg.getVersion());
            algInfo.put("name", alg.getAlgName());
            algInfo.put("description", alg.getDescription());
            algInfo.put("urlsuffix", alg.getURLSuffix());

            // stuff every alg has (should have):
            algInfo.put("constraints", alg.getConstraints());
            algInfo.put("details", alg.getDetails());

            // if the alg is a graph algorithm it may additionally have pointconstraints
            if (alg instanceof GraphAlgorithmFactory) {
                algInfo.put("pointconstraints", ((GraphAlgorithmFactory) alg).getPointConstraints());
            }
            algList.add(algInfo);
        }
        info.put("algorithms", algList);
        return info;
    }

    /**
     * This is the main class of ToureNPlaner. It passes CLI parameters to the
     * handler and creates the httpserver
     */
    public static void main(String[] args) {
        GraphRep graph = null;
        String graphFilename;
        String logFilename;
        CLIParser cliParser = new CLIParser(args);
        if (cliParser.getConfigFilePath() != null) {
            try {
                ConfigManager.init(new FileInputStream(cliParser.getConfigFilePath()));
            } catch (Exception e) {
                // ConfigManager either didn't like the path or the .config file at the path
                log.severe("Error reading configuration file from file: " + cliParser.getConfigFilePath() + '\n' +
                        e.getMessage() + '\n' +
                        "Using builtin configuration...");
            }
        } else {
            log.severe("Usage: \n\tjava -jar tourenplaner-server.jar -c \"config file\" " +
                    "[-f dump|text] [dumpgraph]\nDefaults are: builtin configuration, -f text");
        }
        ConfigManager cm = ConfigManager.getInstance();
        graphFilename = cm.getEntryString("graphfilepath", System.getProperty("user.home") + "/germany.txt");
        logFilename = cm.getEntryString("logfilepath", System.getProperty("user.home") + "/tourenplaner.log");

        // Add log file as loggind handler
        try {
            FileHandler fh = new FileHandler(logFilename, true);
            fh.setFormatter(new XMLFormatter());
            log.addHandler(fh);
            log.setLevel(Level.parse(cm.getEntryString("loglevel", "info").toUpperCase()));
        } catch (IOException ex) {
            log.log(Level.WARNING, "Couldn't open log file " + logFilename, ex);
        }
        GraphRepWriter gWriter = new GraphRepBinaryWriter();

        // now that we have a config (or not) we look if we only need to dump our graph and then exit
        // TODO WARNING Properly handle graph formats
        if (cliParser.dumpgraph()) {
            log.info("Dumping Graph...");
            try {
                graph = new GraphRepStandardReader(false).createGraphRep(new FileInputStream(graphFilename));
                gWriter.writeGraphRep(new FileOutputStream(dumpName(graphFilename)), graph);
            } catch (IOException e) {
                log.severe("IOError dumping graph to file: " + dumpName(graphFilename) + '\n' + e.getMessage());
            } finally {
                System.exit(0);
            }
        }

        //TODO there's an awful lot of duplicate logic and three layers of exception throwing code wtf
        try {
            if (cliParser.loadTextGraph()) {
                graph = new GraphRepStandardReader(false).createGraphRep(new FileInputStream(graphFilename));
            } else {
                try {
                    graph = new GraphRepBinaryReader().createGraphRep(new FileInputStream(dumpName(graphFilename)));
                } catch (InvalidClassException e) {
                    log.warning("Dumped Graph version does not match the required version: " + e.getMessage());
                    log.info("Falling back to text reading from file: " + graphFilename + " (path provided by config file)");
                    graph = new GraphRepStandardReader(false).createGraphRep(new FileInputStream(graphFilename));


                    if (graph != null && new File(dumpName(graphFilename)).delete()) {
                        log.info("Graph successfully read. Now replacing old dumped graph");
                        try {
                            gWriter.writeGraphRep(new FileOutputStream(dumpName(graphFilename)), graph);
                        } catch (IOException e1) {
                            log.warning("writing dump failed (but graph loaded):\n" + e1.getMessage());
                        }
                    }
                } catch (IOException e) {
                    log.log(Level.WARNING, "loading dumped graph failed", e);
                    log.info("Falling back to text reading from file " + graphFilename + " (path provided by config file)");
                    graph = new GraphRepStandardReader(false).createGraphRep(new FileInputStream(graphFilename));
                    log.info("Graph successfully read. Now writing new dump");
                    gWriter.writeGraphRep(new FileOutputStream(dumpName(graphFilename)), graph);
                }
            }


            if (graph == null) {
                log.severe("Reading graph failed");
                System.exit(1);
            }

            // The GraphRep uses a BoundingBoxPriorityTree for NNSearch
            // now which should be fast enough but we can change the searcher if
            // we want to.
            // choose the NNSearcher here
            // DumbNN uses linear search and is slow.
            // HashNN should be faster but needs more RAM
            // GridNN is even faster and uses less RAM
            //log.info("Start creating NNSearcher");
            //graph.setNNSearcher(new GridNN(graph));//new HashNN(graphRep);

            //System.gc();


            // Register Algorithms
            AlgorithmRegistry reg = new AlgorithmRegistry();
            registerAlgorithms(reg, graph);


            // Create our ComputeCore that manages all ComputeThreads
            ComputeCore comCore = new ComputeCore(reg, cm.getEntryInt("threads", 16), cm.getEntryInt("queuelength", 32));
            AlgorithmManagerFactory amFac = new SharingAMFactory(graph);
            log.info("Graph loaded rank range is 0-" + graph.getMaxRank());
            comCore.start(amFac);

            // Create ServerInfo object
            Map<String, Object> serverInfo = getServerInfo(reg);

            new HttpComputeServer(cm, serverInfo, comCore);

        } catch (IOException e) {
            log.log(Level.SEVERE, "loading text graph failed", e);
        } catch (InterruptedException e) {
            log.log(Level.SEVERE, "Main Thread interrupted", e);
        }
    }
}
