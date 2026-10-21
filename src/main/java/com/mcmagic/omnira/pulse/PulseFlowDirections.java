package com.mcmagic.omnira.pulse;

import java.util.*;

/** Signal propagates away from live sources; equal-distance joins have no fabricated direction. */
public final class PulseFlowDirections {
    public static int[] distances(List<PulseTransport.Node> nodes,List<PulseTransport.Link> links){
        int[] distance=new int[nodes.size()];Arrays.fill(distance,Integer.MAX_VALUE);
        var adjacent=new ArrayList<List<Integer>>();for(int i=0;i<nodes.size();i++)adjacent.add(new ArrayList<>());
        for(var edge:links){if(edge.aToB())adjacent.get(edge.a()).add(edge.b());if(edge.bToA())adjacent.get(edge.b()).add(edge.a());}
        var queue=new ArrayDeque<Integer>();
        for(int i=0;i<nodes.size();i++)if(nodes.get(i).source()&&nodes.get(i).supply()>0){distance[i]=0;queue.add(i);}
        while(!queue.isEmpty()){
            int from=queue.remove();if(nodes.get(from).base()>0)continue;
            for(int to:adjacent.get(from))if(!nodes.get(to).source()&&distance[to]>distance[from]+1){distance[to]=distance[from]+1;queue.add(to);}
        }return distance;
    }
    private PulseFlowDirections(){}
}
