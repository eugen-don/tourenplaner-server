package de.tourenplaner.algorithms.bbbundle;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tourenplaner.computecore.StreamJsonWriter;
import de.tourenplaner.graphrep.GraphRep;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * This class is used to store and send back
 * subgraphs for client side computation
 */
public final class BBBundleResult implements StreamJsonWriter {

    //private final GraphRep graph;
    // TODO use IntArrayList
    private final ArrayList<BBBundleEdge> upEdges;
    private final ArrayList<BBBundleEdge> downEdges;
    private final BBBundleRequestData request;
    private final int nodeCount;
    private final GraphRep graph;

    public BBBundleResult(GraphRep graph, int nodeCount, ArrayList<BBBundleEdge> upEdges, ArrayList<BBBundleEdge> downEdges, BBBundleRequestData request) {
        this.upEdges = upEdges;
        this.downEdges = downEdges;
        this.nodeCount= nodeCount;
        this.graph = graph;
        this.request = request;
    }

    @Override
    public void writeToStream(ObjectMapper mapper, OutputStream stream) throws IOException {
        JsonGenerator gen = mapper.getFactory().createGenerator(stream);
        gen.writeStartObject();
        // Head
        gen.writeObjectFieldStart("head");
        gen.writeNumberField("nodeCount", nodeCount);
        gen.writeNumberField("upEdgeCount", upEdges.size());
        gen.writeNumberField("downEdgeCount", downEdges.size());
        gen.writeNumberField("coreSize", request.getCoreSize());
        gen.writeNumberField("level", request.getLevel());
        gen.writeEndObject();
        // Edges
        gen.writeObjectFieldStart("edges");
        gen.writeArrayFieldStart("upEdges");
        for (BBBundleEdge e : upEdges) {
            gen.writeStartObject();
            gen.writeNumberField("src", e.srcId);
            gen.writeNumberField("trgt", e.trgtId);
            gen.writeNumberField("cost", e.cost);
            gen.writeArrayFieldStart("draw");
            for (int i = 0; i < e.unpacked.size(); ++i){
                writeDrawEdge(gen, e, i);
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
        gen.writeEndArray();

        gen.writeArrayFieldStart("downEdges");
        for (BBBundleEdge e : downEdges) {
            gen.writeStartObject();
            gen.writeNumberField("src", e.srcId);
            gen.writeNumberField("trgt", e.trgtId);
            gen.writeNumberField("cost", e.cost);
            gen.writeArrayFieldStart("draw");
            for (int i = 0; i < e.unpacked.size(); ++i){
                writeDrawEdge(gen, e, i);
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
        gen.writeEndArray();
        gen.writeEndObject();
        gen.writeEndObject();
        gen.flush();
    }

    public final void writeDrawEdge(JsonGenerator gen, BBBundleEdge e, int i) throws IOException {
        int edgeId = e.unpacked.get(i);
        int s = graph.getSource(edgeId);
        int t = graph.getTarget(edgeId);
        // TODO we need to save the real type
        int type = 0;
        gen.writeNumber(graph.getXPos(s));
        gen.writeNumber(graph.getYPos(s));
        gen.writeNumber(graph.getXPos(t));
        gen.writeNumber(graph.getYPos(t));
        gen.writeNumber(type);
    }
}
