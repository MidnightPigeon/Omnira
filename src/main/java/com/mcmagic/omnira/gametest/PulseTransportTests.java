package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.pulse.PulseTransport;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_pulse")
@PrefixGameTestTemplate(false)
public final class PulseTransportTests {
    @GameTest(template="spell_arena") public static void visualSignalLeavesSourcesAndDoesNotCrossConsumers(GameTestHelper h){
        var nodes=List.of(source(8192),wire(false),wire(false),orb(8192),load(256,2048,1),wire(false),source(8192));
        var distances=com.mcmagic.omnira.pulse.PulseFlowDirections.distances(nodes,links(0,1,1,2,2,3,3,4,4,5,6,2));
        h.assertTrue(distances[0]==0&&distances[6]==0&&distances[1]==1&&distances[2]==1&&distances[3]==2&&distances[4]==3&&distances[5]==Integer.MAX_VALUE,"Visual signal reversed source or traversed consumer");h.succeed();
    }
    @GameTest(template="spell_arena") public static void idleSplitHonorsUnequalCaps(GameTestHelper h){
        var r=PulseTransport.solve(List.of(source(1024),orb(128),orb(8192)),links(0,1,0,2));
        close(h,r.free()[1],128);close(h,r.free()[2],896);h.succeed();
    }
    @GameTest(template="spell_arena") public static void lossyRelayBottleneckStopsAllLoads(GameTestHelper h){
        var r=PulseTransport.solve(List.of(source(8192),source(8192),wire(true),orb(511),load(256,2048,1),load(256,2048,2)),links(0,2,1,2,2,3,3,4,3,5));
        h.assertTrue(!r.running(),"Relay bottleneck allowed partial startup");
        close(h,Arrays.stream(r.demand()).sum(),0);close(h,Arrays.stream(r.generation()).sum(),0);
        h.assertTrue(r.free()[3]<=511.001,"Stopped relay exceeded capacity");h.succeed();
    }
    @GameTest(template="spell_arena") public static void lossLimitCannotBecomeNegative(GameTestHelper h){
        var nodes=new ArrayList<PulseTransport.Node>();var edges=new ArrayList<PulseTransport.Link>();nodes.add(source(8192));
        for(int i=1;i<=21;i++){nodes.add(wire(true));edges.add(new PulseTransport.Link(i-1,i));}
        nodes.add(orb(8192));edges.add(new PulseTransport.Link(21,22));nodes.add(load(256,2048,1));edges.add(new PulseTransport.Link(22,23));
        var r=PulseTransport.solve(nodes,edges);h.assertTrue(!r.running(),"Fully attenuated route powered machine");close(h,r.free()[22],0);
        for(double v:r.free())h.assertTrue(Double.isFinite(v)&&v>=0,"Invalid free quota");h.succeed();
    }
    @GameTest(template="spell_arena") public static void shuffledLinksKeepSettlement(GameTestHelper h){
        var nodes=List.of(source(1024),source(512),wire(true),orb(8192),orb(8192),load(256,2048,1),load(256,2048,2));
        var edges=links(0,2,2,3,1,4,3,4,3,5,4,6);var expected=PulseTransport.solve(nodes,edges);
        Collections.shuffle(edges,new Random(815));var actual=PulseTransport.solve(nodes,edges);
        for(int i=0;i<nodes.size();i++){close(h,actual.demand()[i],expected.demand()[i]);close(h,actual.free()[i],expected.free()[i]);}
        h.succeed();
    }
    private static PulseTransport.Node source(double supply){return new PulseTransport.Node(supply,0,0,0,0,true,false);}
    private static PulseTransport.Node orb(double cap){return new PulseTransport.Node(0,cap,0,0,0,false,false);}
    private static PulseTransport.Node wire(boolean loss){return new PulseTransport.Node(0,0,0,0,0,false,loss);}
    private static PulseTransport.Node load(double base,double max,long order){return new PulseTransport.Node(0,0,base,max,order,false,false);}
    private static List<PulseTransport.Link> links(int... pairs){var list=new ArrayList<PulseTransport.Link>();for(int i=0;i<pairs.length;i+=2)list.add(new PulseTransport.Link(pairs[i],pairs[i+1]));return list;}
    private static void close(GameTestHelper h,double actual,double expected){h.assertTrue(Math.abs(actual-expected)<.001,"Expected "+expected+", got "+actual);}
    @GameTest(template="spell_arena") public static void sharedIdleQuotaConservesSources(GameTestHelper h){
        var r=PulseTransport.solve(List.of(source(8192),source(8192),orb(8192),orb(8192),orb(8192)),links(0,2,1,3,2,4,3,4));
        close(h,r.free()[2],4096);close(h,r.free()[3],4096);close(h,r.free()[4],8192);
        close(h,Arrays.stream(r.free()).sum(),16384);close(h,r.used()[0],0);close(h,r.used()[1],0);h.succeed();
    }
    @GameTest(template="spell_arena") public static void workingLoadsBorrowIdleQuota(GameTestHelper h){
        var r=PulseTransport.solve(List.of(source(2048),orb(8192),orb(8192),load(256,2048,1),load(256,2048,2)),links(0,1,0,2,1,3,2,4));
        close(h,r.demand()[3],1792);close(h,r.demand()[4],256);close(h,Arrays.stream(r.free()).sum(),0);
        close(h,r.generation()[0],2048);h.succeed();
    }
    @GameTest(template="spell_arena") public static void groupedLoadsKeepOriginalPriority(GameTestHelper h){
        var r=PulseTransport.solve(List.of(source(512),source(512),wire(true),orb(8192),load(256,2048,2),load(256,2048,1)),links(0,2,1,2,2,3,3,4,3,5));
        close(h,r.demand()[4],256);close(h,r.demand()[5],1024*.95-256);close(h,Arrays.stream(r.generation()).sum(),1024);h.succeed();
    }
    @GameTest(template="spell_arena") public static void shortestLinearLossAndUsefulLoad(GameTestHelper h){
        var r=PulseTransport.solve(List.of(source(1024),wire(false),wire(true),wire(true),orb(8192),load(256,256,1)),links(0,1,1,2,2,3,3,4,4,5));
        h.assertTrue(r.running(),"Linear loss stopped valid load");close(h,r.demand()[5],256);close(h,r.generation()[0],256/.9);
        close(h,r.used()[4],256);close(h,r.loss()[2],1024*.05);close(h,r.loss()[3],1024*.05);
        close(h,r.free()[4]+r.used()[4]+r.loss()[2]+r.loss()[3],1024);h.succeed();
    }
    @GameTest(template="spell_arena") public static void independentBranchesDoNotTaxEachOther(GameTestHelper h){
        var r=PulseTransport.solve(List.of(source(512),orb(8192),load(256,512,1),wire(false),wire(true),wire(true)),links(0,1,1,2,1,3,3,4,4,5));
        close(h,r.demand()[2],512);close(h,r.generation()[0],512);close(h,r.loss()[4],0);close(h,r.loss()[5],0);h.succeed();
    }
    @GameTest(template="spell_arena") public static void mixedSourceLossesConserveRawSupply(GameTestHelper h){
        var r=PulseTransport.solve(List.of(source(512),source(512),wire(false),wire(true),wire(false),orb(8192),load(256,2048,1)),links(0,2,2,3,3,5,1,4,4,5,5,6));
        h.assertTrue(r.running(),"Mixed source transport stopped");close(h,r.demand()[6],998.4);
        close(h,Arrays.stream(r.generation()).sum(),1024);close(h,r.loss()[3],25.6);close(h,r.used()[0],0);close(h,r.used()[1],0);h.succeed();
    }
    @GameTest(template="spell_arena") public static void equalShortestPathsRespectRelayCapacity(GameTestHelper h){
        var r=PulseTransport.solve(List.of(source(1024),source(1024),wire(true),orb(512),orb(2048),load(256,1792,1)),links(0,2,1,2,2,3,2,4,3,5,4,5));
        h.assertTrue(r.running(),"Equal shortest alternate path falsely overloaded");close(h,r.demand()[5],1792);
        h.assertTrue(r.used()[3]<=512.001,"Relay exceeded throughput");close(h,Arrays.stream(r.generation()).sum(),1792/.95);h.succeed();
    }
    @GameTest(template="spell_arena") public static void consumerCannotBeTransitAndSourceCannotReceive(GameTestHelper h){
        var r=PulseTransport.solve(List.of(source(512),load(256,256,1),load(256,256,2)),links(0,1,1,2));
        h.assertTrue(!r.running(),"Consumer carried power through its chassis");close(h,r.used()[0],0);h.succeed();
    }
    @GameTest(template="spell_arena") public static void idleWireLoopDoesNotDuplicateQuota(GameTestHelper h){
        var r=PulseTransport.solve(List.of(source(512),wire(false),wire(true),wire(true),orb(8192)),links(0,1,1,2,1,3,2,4,3,4,2,3));
        close(h,r.free()[4],486.4);h.assertTrue(r.free()[4]<=512,"Loop multiplied potential");h.succeed();
    }
    @GameTest(template="spell_arena") public static void randomizedBudgetsRespectSupplyAndCapacity(GameTestHelper h){
        var random=new Random(417);
        for(int sample=0;sample<50;sample++){
            var nodes=new ArrayList<PulseTransport.Node>();var links=new ArrayList<PulseTransport.Link>();
            int sources=2+random.nextInt(2),relays=3+random.nextInt(5),consumers=2+random.nextInt(5);
            for(int i=0;i<sources;i++)nodes.add(source(256+random.nextInt(8)*128));
            for(int i=0;i<relays;i++)nodes.add(i%2==0?orb(256+random.nextInt(8)*128):wire(true));
            for(int i=0;i<consumers;i++)nodes.add(load(128,256+random.nextInt(8)*128,i));
            for(int i=0;i<sources;i++)links.add(new PulseTransport.Link(i,sources+random.nextInt(relays)));
            for(int i=1;i<relays;i++)links.add(new PulseTransport.Link(sources+i-1,sources+i));
            for(int i=0;i<consumers;i++)links.add(new PulseTransport.Link(sources+random.nextInt(relays),sources+relays+i));
            if(relays>3)links.add(new PulseTransport.Link(sources,sources+relays-1));
            for(boolean absorbing:new boolean[]{false,true}){
            var players=new boolean[nodes.size()];if(absorbing)for(int i=0;i<sources;i++)players[i]=i%2==0;
            var r=PulseTransport.solve(nodes,links,players);double supply=nodes.stream().mapToDouble(PulseTransport.Node::supply).sum(),settled=Arrays.stream(r.demand()).sum()+Arrays.stream(r.absorbed()).sum();
            for(int i=0;i<nodes.size();i++){
                var node=nodes.get(i);
                if(node.source())h.assertTrue(r.used()[i]<.001&&r.generation()[i]<=node.supply()+.001,"Source received or exceeded its budget");
                if(node.capacity()>0){h.assertTrue(r.used()[i]+r.free()[i]<=node.capacity()+.001,"Node throughput exceeded");settled+=r.free()[i];}
                if(node.lossy())settled+=r.loss()[i];
                if(r.running()&&node.base()>0)h.assertTrue(r.demand()[i]>=node.base()-.001&&r.demand()[i]<=node.maximum()+.001,"Load outside base/max limits");
            }
            h.assertTrue(settled<=supply+.005,"Network created potential");
            }
        }
        h.succeed();
    }
}
