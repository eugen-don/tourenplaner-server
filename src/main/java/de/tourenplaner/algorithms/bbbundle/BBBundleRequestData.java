package de.tourenplaner.algorithms.bbbundle;

import de.tourenplaner.algorithms.bbprioclassic.BoundingBox;
import de.tourenplaner.computecore.RequestData;

/**
 * Created by niklas on 19.03.15.
 */
public final class BBBundleRequestData extends RequestData {


    public enum LevelMode {
        EXACT,
        AUTO,
        HINTED
    }
    private BoundingBox bbox;

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getCoreSize() {return coreSize;}

    public int getNodeCount() {return nodeCount;}

    public LevelMode getMode() {return mode;}

    public BoundingBox getBbox() {
        return bbox;
    }

    private int level;
    private final int coreSize;
    private final int nodeCount;
    private final LevelMode mode;
    private final double minLen;
    private final double maxLen;
    private final double maxRatio;

    public double getMinLen(){ return minLen;}

    public double getMaxLen(){ return maxLen;}

    public double getMaxRatio(){ return maxRatio;}

    public BBBundleRequestData(String algSuffix, BoundingBox bbox, LevelMode mode, double minLen, double maxLen, double maxRatio, int nodeCount, int level, int coreSize){
        super(algSuffix);
        this.bbox = bbox;
        this.nodeCount = nodeCount;
        this.minLen = minLen;
        this.maxLen = maxLen;
        this.maxRatio = maxRatio;
        this.level = level;
        this.coreSize = coreSize;
        this.mode = mode;
    }

}
