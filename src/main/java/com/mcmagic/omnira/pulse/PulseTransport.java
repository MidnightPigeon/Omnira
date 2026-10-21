package com.mcmagic.omnira.pulse;

import java.util.*;

/** One authoritative settlement: source budgets, useful loads, local losses and spare quotas. */
public final class PulseTransport {
    private static final double EPS=1e-7;
    public record Node(double supply,double capacity,double base,double maximum,long order,boolean source,boolean lossy) {
        public Node {if(!Double.isFinite(supply)||!Double.isFinite(capacity)||!Double.isFinite(base)||!Double.isFinite(maximum)
                ||supply<0||capacity<0||base<0||maximum<base||supply>0&&!source||source&&(capacity>0||base>0||lossy))throw new IllegalArgumentException("Invalid pulse transport node");}
    }
    public record Link(int a,int b,boolean aToB,boolean bToA) {
        public Link(int a,int b){this(a,b,true,true);}
        public Link {if(a<0||b<0||a==b||!aToB&&!bToA)throw new IllegalArgumentException("Invalid pulse connection");}
    }
    public record Transfer(int source,int from,int to,double amount) {}
    public record Result(boolean running,double[] demand,double[] generation,double[] used,double[] free,double[] loss,double[] absorbed,List<Transfer> transfers) {
        public Result(boolean running,double[] demand,double[] generation,double[] used,double[] free,double[] loss,double[] absorbed){
            this(running,demand,generation,used,free,loss,absorbed,new ArrayList<>());
        }
        public Result(boolean running,double[] demand,double[] generation,double[] used,double[] free,double[] loss){
            this(running,demand,generation,used,free,loss,new double[demand.length]);
        }
    }
    private record Route(int source,int from,int to,int[] path) {}
    private record Profile(int source,int[] distance,int[] charges,List<List<Integer>> next) {
        double efficiency(int at){return PulseAllocation.efficiency(charges[at]);}
    }
    public static Result solve(List<Node> nodes,List<Link> links){
        return solve(nodes,links,new boolean[nodes.size()]);
    }
    public static Result solve(List<Node> nodes,List<Link> links,boolean[] absorbing){
        if(absorbing.length!=nodes.size())throw new IllegalArgumentException("Missing source absorption flags");
        int n=nodes.size();var adjacent=new ArrayList<List<Integer>>();
        for(int i=0;i<n;i++)adjacent.add(new ArrayList<>());
        for(var link:links){if(link.aToB)adjacent.get(link.a).add(link.b);if(link.bToA)adjacent.get(link.b).add(link.a);}
        adjacent.forEach(Collections::sort);
        var grouped=groupLoads(nodes,links,adjacent,absorbing);if(grouped!=null)return grouped;
        var profiles=new ArrayList<Profile>();
        for(int i=0;i<n;i++)if(nodes.get(i).source&&nodes.get(i).supply>0)profiles.add(profile(nodes,adjacent,i));
        var result=new Result(true,new double[n],new double[n],new double[n],new double[n],new double[n]);
        if(nodes.stream().anyMatch(node->node.base>0))result=allocate(nodes,links,profiles);
        // Local player loads receive only the budget left after all machine allocations.
        for(int i=0;i<n;i++)if(absorbing[i]&&nodes.get(i).source){
            double amount=Math.max(0,nodes.get(i).supply-result.generation[i]);
            result.absorbed[i]=amount;result.generation[i]+=amount;
        }
        quotas(nodes,profiles,result);
        return result;
    }
    /** Identical terminal interfaces share one constraint; retain each consumer's priority afterwards. */
    private static Result groupLoads(List<Node> nodes,List<Link> links,List<List<Integer>> adjacent,boolean[] absorbing){
        if(links.stream().anyMatch(link->!link.aToB||!link.bToA))return null;
        var loads=new ArrayList<Integer>();for(int i=0;i<nodes.size();i++)if(nodes.get(i).base>0)loads.add(i);
        if(loads.size()<2)return null;int first=loads.getFirst();
        for(int i:loads)if(nodes.get(i).capacity>0||nodes.get(i).lossy||!adjacent.get(i).equals(adjacent.get(first)))return null;
        var reduced=new ArrayList<Node>();int[] map=new int[nodes.size()];Arrays.fill(map,-1);
        double base=0,max=0;for(int i:loads){base+=nodes.get(i).base;max+=nodes.get(i).maximum;}
        for(int i=0;i<nodes.size();i++){
            if(nodes.get(i).base>0&&i!=first)continue;map[i]=reduced.size();
            reduced.add(i==first?new Node(0,0,base,max,0,false,false):nodes.get(i));
        }
        for(int i:loads)map[i]=map[first];var reducedLinks=new ArrayList<Link>();var seen=new HashSet<Long>();
        for(var link:links){int a=map[link.a],b=map[link.b];long key=((long)Math.min(a,b)<<32)|Math.max(a,b);
            if(a!=b&&seen.add(key))reducedLinks.add(new Link(a,b));}
        var reducedAbsorbing=new boolean[reduced.size()];for(int i=0;i<nodes.size();i++)if(absorbing[i])reducedAbsorbing[map[i]]=true;
        var compact=solve(reduced,reducedLinks,reducedAbsorbing);int n=nodes.size();var result=new Result(compact.running,new double[n],new double[n],new double[n],new double[n],new double[n]);
        for(int i=0;i<n;i++)if(nodes.get(i).base==0){int at=map[i];result.generation[i]=compact.generation[at];result.used[i]=compact.used[at];result.free[i]=compact.free[at];result.loss[i]=compact.loss[at];result.absorbed[i]=compact.absorbed[at];}
        if(compact.running){
            double spare=Math.max(0,compact.demand[map[first]]-base);
            loads.sort(Comparator.comparingLong((Integer i)->nodes.get(i).order).thenComparingInt(i->i));
            for(int i:loads){var node=nodes.get(i);double extra=Math.min(spare,node.maximum-node.base);spare-=extra;result.demand[i]=node.base+extra;result.used[i]=result.demand[i];}
            int[] reverse=new int[reduced.size()];for(int i=0;i<n;i++)if(nodes.get(i).base==0||i==first)reverse[map[i]]=i;
            for(var transfer:compact.transfers){
                if(transfer.to==map[first])for(int i:loads)result.transfers.add(new Transfer(transfer.source<0?-1:reverse[transfer.source],reverse[transfer.from],i,
                        transfer.amount*result.demand[i]/compact.demand[map[first]]));
                else result.transfers.add(new Transfer(transfer.source<0?-1:reverse[transfer.source],reverse[transfer.from],reverse[transfer.to],transfer.amount));
            }
        }
        return result;
    }
    private static Profile profile(List<Node> nodes,List<List<Integer>> adjacent,int source){
        int n=nodes.size();int[] distance=new int[n],charges=new int[n];Arrays.fill(distance,Integer.MAX_VALUE);Arrays.fill(charges,1000);
        distance[source]=0;charges[source]=0;var queue=new ArrayDeque<Integer>();queue.add(source);
        while(!queue.isEmpty()){
            int from=queue.remove();if(nodes.get(from).base>0)continue;
            for(int to:adjacent.get(from)){
                if(nodes.get(to).source)continue;
                int length=distance[from]+1,loss=charges[from]+(nodes.get(to).lossy?1:0);
                if(length<distance[to]||length==distance[to]&&loss<charges[to]){distance[to]=length;charges[to]=loss;queue.add(to);}
            }
        }
        var next=new ArrayList<List<Integer>>();for(int i=0;i<n;i++)next.add(new ArrayList<>());
        for(int from=0;from<n;from++)if(distance[from]<Integer.MAX_VALUE&&nodes.get(from).base==0)
            for(int to:adjacent.get(from))if(!nodes.get(to).source&&distance[to]==distance[from]+1
                    &&charges[to]==charges[from]+(nodes.get(to).lossy?1:0)&&charges[to]<20)next.get(from).add(to);
        return new Profile(source,distance,charges,next);
    }
    private static Result allocate(List<Node> nodes,List<Link> links,List<Profile> profiles){
        int n=nodes.size();
        // Loss-free networks retain the sparse circulation solver, including alternate routes.
        if(nodes.stream().noneMatch(Node::lossy)){
            var graph=new ArrayList<PulseAllocation.Node>();var edges=new ArrayList<PulseAllocation.Edge>();
            for(var node:nodes)graph.add(new PulseAllocation.Node(node.supply,node.capacity,node.base,node.maximum,node.order));
            for(var link:links){if(link.aToB&&!nodes.get(link.b).source&&nodes.get(link.a).base==0)edges.add(new PulseAllocation.Edge(link.a,link.b,1));
                if(link.bToA&&!nodes.get(link.a).source&&nodes.get(link.b).base==0)edges.add(new PulseAllocation.Edge(link.b,link.a,1));}
            var flow=PulseAllocation.solve(graph,edges);var used=new double[n];
            for(int i=0;i<edges.size();i++)used[edges.get(i).to()]+=flow.flow()[i];
            var result=new Result(flow.running(),flow.demand(),flow.generation(),used,new double[n],new double[n]);
            for(int i=0;i<edges.size();i++)if(flow.flow()[i]>EPS)result.transfers.add(new Transfer(-1,edges.get(i).from(),edges.get(i).to(),flow.flow()[i]));
            return result;
        }
        if(profiles.size()==1){
            var p=profiles.getFirst();var graph=new ArrayList<PulseAllocation.Node>();var edges=new ArrayList<PulseAllocation.Edge>();
            for(int i=0;i<n;i++){
                var node=nodes.get(i);double eff=p.efficiency(i);
                if(eff<=0&&node.base>0)return new Result(false,new double[n],new double[n],new double[n],new double[n],new double[n]);
                graph.add(new PulseAllocation.Node(node.supply,node.capacity>0&&eff>0?node.capacity/eff:0,
                        eff>0?node.base/eff:0,eff>0?node.maximum/eff:0,node.order));
                for(int to:p.next.get(i))edges.add(new PulseAllocation.Edge(i,to,1));
            }
            var flow=PulseAllocation.solve(graph,edges);var result=new Result(flow.running(),new double[n],flow.generation(),new double[n],new double[n],new double[n]);
            for(int i=0;i<n;i++)result.demand[i]=flow.demand()[i]*p.efficiency(i);
            for(int j=0;j<edges.size();j++){int to=edges.get(j).to();double raw=flow.flow()[j];result.used[to]+=raw*p.efficiency(to);
                if(raw>EPS)result.transfers.add(new Transfer(p.source,edges.get(j).from(),to,raw));
                if(nodes.get(to).lossy)result.loss[to]+=raw*.05;else if(nodes.get(to).capacity>0)result.loss[to]+=raw*(1-p.efficiency(to));}
            return result;
        }
        var routes=new ArrayList<Route>();
        // Dead-end scenery and idle branches need no optimization variables.
        for(int s=0;s<profiles.size();s++){
            var p=profiles.get(s);boolean[] useful=new boolean[n];
            var order=ordered(p);
            for(int k=order.size()-1;k>=0;k--){int at=order.get(k);useful[at]=nodes.get(at).base>0;
                for(int to:p.next.get(at))useful[at]|=useful[to];}
            int[] incoming=new int[n];for(int from:order)for(int to:p.next.get(from))if(useful[to])incoming[to]++;
            for(int from:order){
                if(from!=p.source&&nodes.get(from).capacity==0&&nodes.get(from).base==0&&incoming[from]==1&&p.next.get(from).size()==1)continue;
                for(int first:p.next.get(from))if(useful[first]){
                    var path=new ArrayList<Integer>();int to=first;path.add(to);
                    while(nodes.get(to).capacity==0&&nodes.get(to).base==0&&incoming[to]==1&&p.next.get(to).size()==1){to=p.next.get(to).getFirst();path.add(to);}
                    routes.add(new Route(s,from,to,path.stream().mapToInt(Integer::intValue).toArray()));
                }
            }
        }
        int v=routes.size();var rows=new ArrayList<double[]>();var bounds=new ArrayList<Double>();
        double[][] demand=new double[n][v],received=new double[n][v];
        for(int j=0;j<v;j++){var r=routes.get(j);double eff=profiles.get(r.source).efficiency(r.to);received[r.to][j]=eff;if(nodes.get(r.to).base>0)demand[r.to][j]=eff;}
        for(int s=0;s<profiles.size();s++){
            var p=profiles.get(s);var supply=new double[v];
            for(int j=0;j<v;j++)if(routes.get(j).source==s&&routes.get(j).from==p.source)supply[j]=1;
            add(rows,bounds,supply,nodes.get(p.source).supply);
            for(int at:ordered(p))if(at!=p.source&&nodes.get(at).base==0){
                var balance=new double[v];boolean present=false;
                for(int j=0;j<v;j++)if(routes.get(j).source==s){var r=routes.get(j);if(r.to==at){balance[j]++;present=true;}if(r.from==at){balance[j]--;present=true;}}
                if(present){add(rows,bounds,balance,0);add(rows,bounds,negative(balance),0);}
            }
        }
        var consumers=new ArrayList<Integer>();
        for(int i=0;i<n;i++){
            var node=nodes.get(i);if(node.capacity>0)add(rows,bounds,received[i],node.capacity);
            if(node.base>0){add(rows,bounds,demand[i],node.maximum);add(rows,bounds,negative(demand[i]),-node.base);if(node.maximum>node.base)consumers.add(i);}
        }
        consumers.sort(Comparator.comparingLong((Integer i)->nodes.get(i).order).thenComparingInt(i->i));
        double[] solution=null;PulseLinearProgram lp=null;
        for(int step=0;step<=consumers.size();step++){
            double[] objective=new double[v];
            if(step<consumers.size())objective=demand[consumers.get(step)].clone();
            else for(int j=0;j<v;j++)if(routes.get(j).from==profiles.get(routes.get(j).source).source)objective[j]=-1;
            if(lp==null){lp=new PulseLinearProgram(rows,bounds,objective);solution=lp.solve();}else solution=lp.maximize(objective);
            if(solution==null)return new Result(false,new double[n],new double[n],new double[n],new double[n],new double[n]);
            if(step<consumers.size()){double value=dot(objective,solution);lp.constrain(objective,value+EPS);lp.constrain(negative(objective),-value+EPS);}
        }
        var result=new Result(true,new double[n],new double[n],new double[n],new double[n],new double[n]);
        for(int j=0;j<v;j++){
            var r=routes.get(j);var p=profiles.get(r.source);double raw=solution[j];
            if(r.from==p.source)result.generation[p.source]+=raw;
            if(raw>EPS){int from=r.from;for(int to:r.path){result.transfers.add(new Transfer(p.source,from,to,raw));from=to;}}
            for(int at:r.path){result.used[at]+=raw*p.efficiency(at);
                if(nodes.get(at).lossy)result.loss[at]+=raw*.05;else if(nodes.get(at).capacity>0)result.loss[at]+=raw*(1-p.efficiency(at));}
            if(nodes.get(r.to).base>0)result.demand[r.to]+=raw*p.efficiency(r.to);
        }
        return result;
    }
    private static List<Integer> ordered(Profile p){
        var order=new ArrayList<Integer>();for(int i=0;i<p.distance.length;i++)if(p.distance[i]<Integer.MAX_VALUE)order.add(i);
        order.sort(Comparator.comparingInt((Integer i)->p.distance[i]).thenComparingInt(i->i));return order;
    }
    private static void quotas(List<Node> nodes,List<Profile> profiles,Result result){
        int n=nodes.size();double[][] pending=new double[profiles.size()][n];int last=0;
        int[] nearest=new int[n];Arrays.fill(nearest,Integer.MAX_VALUE);
        for(var p:profiles)for(int i=0;i<n;i++)nearest[i]=Math.min(nearest[i],p.distance[i]);
        double[] admitted=new double[n];
        for(int s=0;s<profiles.size();s++){var p=profiles.get(s);pending[s][p.source]=Math.max(0,nodes.get(p.source).supply-result.generation[p.source]);
            for(int d:p.distance)if(d<Integer.MAX_VALUE)last=Math.max(last,d);}
        var layers=new ArrayList<SortedSet<Integer>>();for(int i=0;i<=last;i++)layers.add(new TreeSet<>());
        for(var p:profiles)for(int i=0;i<n;i++)if(p.distance[i]<Integer.MAX_VALUE)layers.get(p.distance[i]).add(i);
        // Budgets move only outward from their origin; cycles can never amplify a signal.
        for(int depth=0;depth<=last;depth++){
            for(int at:layers.get(depth)){
                double incoming=0;for(int s=0;s<profiles.size();s++)if(profiles.get(s).distance[at]==depth)incoming+=pending[s][at]*profiles.get(s).efficiency(at);
                double room=nodes.get(at).capacity>0?Math.max(0,nodes.get(at).capacity-result.used[at]-admitted[at]):Double.POSITIVE_INFINITY;
                double scale=incoming>EPS?Math.min(1,room/incoming):1;
                for(int s=0;s<profiles.size();s++){
                    var p=profiles.get(s);if(p.distance[at]!=depth)continue;
                    double raw=pending[s][at]*scale,eff=p.efficiency(at);pending[s][at]=0;if(raw<EPS||nodes.get(at).base>0)continue;
                    boolean relay=nodes.get(at).capacity>0;
                    if(!nodes.get(at).source){if(!relay)result.free[at]+=raw*eff;else admitted[at]+=raw*eff;
                        if(nodes.get(at).lossy)result.loss[at]+=raw*.05;}
                    var targets=new ArrayList<Integer>();
                    for(int to:p.next.get(at))if(nodes.get(to).base==0&&p.distance[to]==nearest[to])targets.add(to);
                    // A relay reserves one local share; the remainder continues to outward branches.
                    // Spare signals do not cross into a region closer to a different source.
                    if(relay){double local=raw/(targets.size()+1);result.free[at]+=local*eff;result.loss[at]+=local*(1-eff);raw-=local;}
                    if(targets.isEmpty())continue;
                    // Equal idle shares, with redistribution when a directly connected branch is full.
                    double remaining=raw;while(remaining>EPS&&!targets.isEmpty()){
                        double share=remaining/targets.size(),sent=0;var full=new ArrayList<Integer>();
                        for(int to:targets){double targetEff=p.efficiency(to);double capacity=nodes.get(to).capacity;
                            double already=0;for(int other=0;other<profiles.size();other++)already+=pending[other][to]*profiles.get(other).efficiency(to);
                            double available=capacity>0?Math.max(0,capacity-result.used[to]-admitted[to]-already)/targetEff:Double.POSITIVE_INFINITY;
                            double amount=Math.min(share,available);pending[s][to]+=amount;sent+=amount;if(available<=share+EPS)full.add(to);}
                        remaining-=sent;targets.removeAll(full);if(sent<EPS)break;
                    }
                    if(relay){result.free[at]+=remaining*eff;result.loss[at]+=remaining*(1-eff);}
                }
            }
        }
    }
    private static void add(List<double[]> rows,List<Double> bounds,double[] row,double value){rows.add(row);bounds.add(value);}
    private static double[] negative(double[] row){var copy=row.clone();for(int i=0;i<copy.length;i++)copy[i]=-copy[i];return copy;}
    private static double dot(double[] a,double[] b){double sum=0;for(int i=0;i<a.length;i++)sum+=a[i]*b[i];return sum;}
    private PulseTransport(){}
}
