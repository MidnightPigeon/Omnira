package com.mcmagic.omnira.pulse;

import java.util.*;

/** Lower-bound circulation reserves all base loads, then residual flow grants ordered extras. */
public final class PulseAllocation {
    public record Node(double supply,double throughput,double base,double maximum,long order) {
        public Node {
            if(!Double.isFinite(supply)||!Double.isFinite(throughput)||!Double.isFinite(base)
                    ||!Double.isFinite(maximum)||supply<0||throughput<0||base<0||maximum<base)
                throw new IllegalArgumentException("Invalid pulse node");
        }
    }
    public record Edge(int from,int to,double loss) {
        public Edge {
            if(from==to||from<0||to<0||loss!=1)
                throw new IllegalArgumentException("Transport resolves chain loss before sparse allocation");
        }
    }
    public record Result(boolean running,double[] demand,double[] generation,double[] flow) {}
    public static double efficiency(int unprotectedWires) {
        if(unprotectedWires<0)throw new IllegalArgumentException("Negative cable count");
        return Math.max(0,1-.05*unprotectedWires);
    }
    public static Result solve(List<Node> nodes,List<Edge> edges) {
        int n=nodes.size(),source=2*n,sink=source+1,feasibilitySource=source+2,feasibilitySink=source+3;
        double supply=nodes.stream().mapToDouble(Node::supply).sum(),base=nodes.stream().mapToDouble(Node::base).sum();
        if(base>supply+Flow.EPS)return stopped(n,edges.size());
        var graph=new Flow(2*n+4);double infinity=Math.max(1,supply+base);
        var generation=new Flow.Arc[n];var demand=new Flow.Arc[n];var links=new Flow.Arc[edges.size()];
        var temporary=new ArrayList<Flow.Arc>();
        for(int i=0;i<n;i++){
            var node=nodes.get(i);int in=2*i,out=in+1;
            graph.add(in,out,node.throughput>0?node.throughput:infinity);
            generation[i]=graph.add(source,in,node.supply);
            demand[i]=graph.add(out,sink,0);
            // Each base load is a lower bound, not a competing optional load.
            if(node.base>0)temporary.add(graph.add(out,feasibilitySink,node.base));
        }
        for(int i=0;i<edges.size();i++){
            var edge=edges.get(i);if(edge.from>=n||edge.to>=n)throw new IllegalArgumentException("Missing edge endpoint");
            links[i]=graph.add(2*edge.from+1,2*edge.to,infinity);
        }
        temporary.add(graph.add(feasibilitySource,sink,base));
        temporary.add(graph.add(sink,source,infinity));
        if(graph.send(feasibilitySource,feasibilitySink,base)<base-Flow.EPS)return stopped(n,edges.size());
        for(var arc:temporary)arc.disable();
        double[] allocated=new double[n];for(int i=0;i<n;i++)allocated[i]=nodes.get(i).base;
        var order=new ArrayList<Integer>();for(int i=0;i<n;i++)if(nodes.get(i).maximum>nodes.get(i).base)order.add(i);
        order.sort(Comparator.comparingLong((Integer i)->nodes.get(i).order).thenComparingInt(i->i));
        for(int i:order){
            double extra=nodes.get(i).maximum-nodes.get(i).base;demand[i].remaining=extra;
            graph.send(source,sink,extra);allocated[i]+=demand[i].reverse.remaining;
            // Later consumers may reroute paths, but cannot take back an earlier optimum.
            demand[i].disable();
        }
        double[] produced=new double[n],transferred=new double[edges.size()];
        for(int i=0;i<n;i++)produced[i]=generation[i].reverse.remaining;
        for(int i=0;i<edges.size();i++)transferred[i]=links[i].reverse.remaining;
        return new Result(true,allocated,produced,transferred);
    }
    private static Result stopped(int nodes,int edges){return new Result(false,new double[nodes],new double[nodes],new double[edges]);}
    /** Dinic residual graph; memory scales with topology instead of a dense constraint matrix. */
    private static final class Flow {
        static final double EPS=1e-7;
        private final List<List<Arc>> adjacency;private final int[] distance,cursor,queue;
        private final int[] pathNodes;private final double[] pathAmounts;private final Arc[] pathArcs;
        private static final class Arc {
            final int to;double remaining;Arc reverse;
            Arc(int to,double capacity){this.to=to;remaining=capacity;}
            void disable(){remaining=0;reverse.remaining=0;}
        }
        Flow(int count){adjacency=new ArrayList<>(count);for(int i=0;i<count;i++)adjacency.add(new ArrayList<>());distance=new int[count];cursor=new int[count];queue=new int[count];pathNodes=new int[count];pathAmounts=new double[count];pathArcs=new Arc[count];}
        Arc add(int from,int to,double capacity){var arc=new Arc(to,capacity);var reverse=new Arc(from,0);arc.reverse=reverse;reverse.reverse=arc;adjacency.get(from).add(arc);adjacency.get(to).add(reverse);return arc;}
        boolean levels(int source,int sink){
            Arrays.fill(distance,-1);distance[source]=0;int first=0,last=0;queue[last++]=source;
            while(first<last){int at=queue[first++];for(var arc:adjacency.get(at))if(arc.remaining>EPS&&distance[arc.to]<0){distance[arc.to]=distance[at]+1;queue[last++]=arc.to;}}
            return distance[sink]>=0;
        }
        double push(int at,int sink,double amount){
            int depth=0;pathNodes[0]=at;pathAmounts[0]=amount;
            while(depth>=0){
                at=pathNodes[depth];
                if(at==sink){double sent=pathAmounts[depth];for(int i=0;i<depth;i++){var arc=pathArcs[i];arc.remaining-=sent;arc.reverse.remaining+=sent;}return sent;}
                var arcs=adjacency.get(at);boolean advanced=false;
                while(cursor[at]<arcs.size()){
                    var arc=arcs.get(cursor[at]);
                    if(arc.remaining>EPS&&distance[arc.to]==distance[at]+1){pathArcs[depth]=arc;pathAmounts[depth+1]=Math.min(pathAmounts[depth],arc.remaining);pathNodes[++depth]=arc.to;advanced=true;break;}
                    cursor[at]++;
                }
                if(!advanced){distance[at]=-1;depth--;if(depth>=0)cursor[pathNodes[depth]]++;}
            }return 0;
        }
        double send(int source,int sink,double limit){
            double sent=0;while(sent<limit-EPS&&levels(source,sink)){
                Arrays.fill(cursor,0);double next;while(sent<limit-EPS&&(next=push(source,sink,limit-sent))>EPS)sent+=next;
            }return sent;
        }
    }
    private PulseAllocation(){}
}
