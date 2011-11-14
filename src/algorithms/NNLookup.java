package algorithms;

import graphrep.GraphRep;

import computecore.ComputeRequest;

public class NNLookup extends GraphAlgorithm {

	public NNLookup(GraphRep graph) {
		super(graph);
	}

	@Override
	public void compute(ComputeRequest req) throws ComputeException {
		assert req != null : "We ended up without a request object in run";

		// TODO: send error messages to client
		Points points = req.getPoints();
		// Check if we have enough points to do something useful
		if (points.size() < 1) {
			throw new ComputeException("Not enough points, need at least 1");
		}
		Points resultPoints = req.getResultPoints();
		nearestNeighbourLookup(points, resultPoints);
	}

	public void nearestNeighbourLookup(Points points, Points resultPoints) {
		int nodeID;

		for (int pointIndex = 0; pointIndex < points.size(); pointIndex++) {
			nodeID = graph.getIDForCoordinates(points.getPointLat(pointIndex),
					points.getPointLon(pointIndex));
			resultPoints.addPoint(graph.getNodeLat(nodeID),
					graph.getNodeLon(nodeID));
		}
	}

}
