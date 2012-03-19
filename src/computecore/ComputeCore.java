/**
 * $$\\ToureNPlaner\\$$
 */
package computecore;

import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * The ComputeCore keeps a thread pool of ComputeThreads and allows new
 * computations to be scheduled for execution by adding them to it's queue
 * 
 * @author Niklas Schnelle, Peter Vollmer
 * 
 */
public class ComputeCore {
    
    private static Logger log = Logger.getLogger(ComputeCore.class.getName());

    private final int numThreads;
	private final BlockingQueue<ComputeRequest> reqQueue;
	private final AlgorithmRegistry registry;

	/**
	 * Constructs a new ComputeCore which uses numThreads threads in it's pool
	 * and has a waiting queue of length queueLength
	 * 
	 * @param algRegistry
	 * @param numThreads
	 * @param queueLength
	 */
	public ComputeCore(AlgorithmRegistry algRegistry, int numThreads,
			int queueLength) {
		this.reqQueue = new LinkedBlockingQueue<ComputeRequest>(queueLength);
		this.registry = algRegistry;
		this.numThreads = numThreads;

	}

	/**
	 * Starts the ComputeThreads
	 * 
	 * @throws SQLException
	 *             Thrown only in private mode, and only if database connection
	 *             could not be established.
	 * 
	 */
	public void start(AlgorithmManagerFactory amFac) {
		ComputeThread curr;
		log.info("Starting " + numThreads + " ComputeThreads");
		for (int i = 0; i < numThreads; i++) {
			curr = new ComputeThread(registry.getAlgorithmManager(amFac),
					reqQueue);
			curr.start();
		}
		log.info(numThreads+" ComputeThreads started");
	}

	/**
	 * Submits a request for computation, returns true if there is still space
	 * in the queue false otherwise
	 * 
	 * @param rq
	 * @return
	 */
	public boolean submit(ComputeRequest rq) {
		return reqQueue.offer(rq);
	}

	/**
	 * Gets the AlgorithmRegistry used by this ComputeCore to create
	 * AlgorithmManagers for it's ComputeThreads
	 * 
	 * @return the AlgorithmRegistry
	 */
	public AlgorithmRegistry getAlgorithmRegistry() {
		return registry;
	}

}
