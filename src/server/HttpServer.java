/**
 * $$\\ToureNPlaner\\$$
 */
package server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import algorithms.AlgorithmFactory;
import algorithms.DummyFactory;
import algorithms.GraphAlgorithmFactory;
import computecore.AlgorithmRegistry;
import computecore.ComputeCore;
import config.ConfigManager;

/**
 * ToureNPlaner Event Based Server
 * 
 * This is the main class used to start the server and construct 
 * utility objects like the Graphrep. and ConfigManager and ComputeCore
 * 
 * @author Niklas Schnelle
 * @version 0.1 Prototype
 * 
 *  Initially based on: 
 * 	http://docs.jboss.org/netty/3.2/xref/org/jboss/netty/example/http/snoop/package-summary.html
 */
public class HttpServer {
	
	
	private static Map<String, Object> getServerInfo(AlgorithmRegistry reg){
		Map<String, Object> info = new HashMap<String,Object>(4);
		info.put("version", new Float(0.1));
		info.put("servertype", "public");
		info.put("sslport", ConfigManager.getInstance().getEntryLong("sslport", 8081));
		// Enumerate Algorithms
		Collection<AlgorithmFactory> algs = reg.getAlgorithms();
		Map<String, Object> algInfo;
		List<Map<String, Object>> algList= new ArrayList<Map<String,Object>>();
		for(AlgorithmFactory alg: algs){
			algInfo = new HashMap<String, Object>(5);
			algInfo.put("version", alg.getVersion());
			algInfo.put("name", alg.getAlgName());
			algInfo.put("urlsuffix", alg.getURLSuffix());
			if(alg instanceof GraphAlgorithmFactory){
				algInfo.put("pointconstraints", ((GraphAlgorithmFactory)alg).getPointConstraints());
				algInfo.put("constraints", ((GraphAlgorithmFactory)alg).getConstraints());
			}
			algList.add(algInfo);
		}
		info.put("algorithms", algList);
		
		return info;
	}
	/**
	 * Main method for the ToureNPlaner Server
	 * @param args
	 */
    public static void main(String[] args) {
        // Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory( // Change to Oio* if you want OIO
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        ServerBootstrap infoBootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory( // Change to Oio* if you want OIO
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));
        
        // Load Config
        if(args.length == 1){
        	try {
				ConfigManager.Init(args[0]);
			} catch (Exception e) {
				System.err.println("Couldn't load configuration File: "+e.getMessage());
				return;
			}
        } else {
        	System.err.println("Missing config path parameter");
        	return;
        }
        // Register Algorithms
        AlgorithmRegistry reg = new AlgorithmRegistry();
        reg.registerAlgorithm(new DummyFactory());
        
        ConfigManager cm = ConfigManager.getInstance();
        // Create our ComputeCore that manages all ComputeThreads
        ComputeCore comCore = new ComputeCore(reg, (int)cm.getEntryLong("threads",2),(int)cm.getEntryLong("queuelength", 32));
        
        // Create ServerInfo object
        Map<String, Object> serverInfo = getServerInfo(reg);
        
        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory(new ServerPipelineFactory(comCore, false, serverInfo));
        infoBootstrap.setPipelineFactory(new ServerInfoOnlyPipelineFactory(serverInfo));
        
        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress((int)cm.getEntryLong("sslport", 8081)));
        infoBootstrap.bind(new InetSocketAddress((int)cm.getEntryLong("httpport", 8080)));
    }
}