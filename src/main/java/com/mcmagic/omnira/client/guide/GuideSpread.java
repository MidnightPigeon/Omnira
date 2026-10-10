package com.mcmagic.omnira.client.guide;

/** Leaf indexing shared by rendering, widgets and navigation. */
public final class GuideSpread {
    private GuideSpread(){}
    public static int leaf(int first,int side){return first+side;}
    public static int turn(int first,boolean forward){return Math.max(0,first+(forward?2:-2));}
    public static boolean hasNext(int first,int total){return first+2<total;}
    public static int directoryStart(int introductionLeaves){return introductionLeaves;}
    public static int directoryLeaf(int leaf,int introductionLeaves){return leaf-directoryStart(introductionLeaves);}
    public static int directoryTotal(int entries,int introductionLeaves){return directoryStart(introductionLeaves)+Math.max(1,(entries+5)/6);}
}
