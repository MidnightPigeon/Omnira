package com.mcmagic.omnira.aggregation;

import net.minecraft.core.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;

/** Clockwise order is shared by world nodes, UI slots and assembly animation. */
public final class AggregationLayout {
    public static double buttonDepth(double elapsed){
        if(elapsed<0||elapsed>=8)return 0;
        return .04*(elapsed<2?elapsed/2:elapsed<4?1:(8-elapsed)/4);
    }
    public static final int SUBSTRATE=8,INK=9,CORE=10,OUTPUT=11,SLOTS=12,DURATION=160;
    public static final double[][] NODES={{-.48,1.16},{.48,1.16},{1.16,.48},{1.16,-.48},
            {.48,-1.16},{-.48,-1.16},{-1.16,-.48},{-1.16,.48}};
    public static final double BUTTON_Y=-.83,BUTTON_Z=.35,BUTTON_TILT=-18;
    public static final int[] COLORS={0x7CAEFF,0x7CAEFF,0x70DBBE,0x70DBBE,0x70DBBE,
            0xC29CEB,0xC29CEB,0xC29CEB,0xEDF4FF,0xA7A0EC,0xEBD49D,0xBFF4DB};
    public static final int[][] UI={{63,24},{111,24},{145,58},{145,106},{111,140},
            {63,140},{29,106},{29,58},{69,68},{105,68},{87,94},{87,119}};
    public static BlockPos part(BlockPos center,Direction facing,int index){
        return center.relative(facing.getCounterClockWise(),index%3-1).above(index/3-1);
    }
    public static BlockPos center(BlockPos pos,BlockState state){
        int index=state.getValue(AggregationRingBlock.PART);
        return pos.relative(state.getValue(AggregationRingBlock.FACING).getCounterClockWise(),1-index%3).above(1-index/3);
    }
    public static Vec3 local(Vec3 world,BlockPos center,Direction facing){
        var delta=world.subtract(Vec3.atCenterOf(center));var right=facing.getCounterClockWise();
        return new Vec3(delta.x*right.getStepX()+delta.z*right.getStepZ(),delta.y,
                delta.x*facing.getStepX()+delta.z*facing.getStepZ());
    }
    public static double bob(double time,boolean powered){return powered?Math.sin(time*.045)*.045:0;}
    public static Vec3 orb(double time,int index,boolean powered){
        double angle=(powered?time*.025:0)+(index+1)*Math.PI;
        return new Vec3(Math.cos(angle)*.5,Math.sin(angle)*.5,0);
    }
    public static int node(Vec3 local,double bob){
        for(int i=0;i<8;i++)if(Math.hypot(local.x-NODES[i][0],local.y-bob-NODES[i][1])<.24)return i;
        return -1;
    }
    public static boolean button(Vec3 local,double bob){
        return Math.hypot(local.x,local.y-bob-BUTTON_Y)<.25;
    }
    /** Nodes release inward one by one and join a clockwise inner-ring stack. */
    public static Vec3 ingredient(double progress,int slot){
        double elapsed=progress-slot*.085;
        if(elapsed<0)return null;
        double along=Math.min(7,progress/.82*7);
        int index=Math.min(6,(int)along);
        double f=along-index;
        var a=point(Math.max(0,index-1));var b=point(index);
        var c=point(index+1);var d=point(Math.min(7,index+2));
        double release=Math.min(1,elapsed/.045);
        release=release*release*(3-2*release);
        double radius=1-.38*release;
        double join=Math.max(0,Math.min(1,(elapsed-.045)/.075));
        join=join*join*(3-2*join);
        double x=NODES[slot][0]*radius*(1-join)+curve(a.x,b.x,c.x,d.x,f)*.62*join;
        double y=NODES[slot][1]*radius*(1-join)+curve(a.y,b.y,c.y,d.y,f)*.62*join;
        double inward=Math.max(0,Math.min(1,(progress-.82)/.18));
        inward=inward*inward*(3-2*inward);
        double angle=inward*Math.PI*.65;
        return new Vec3((x*Math.cos(angle)-y*Math.sin(angle))*(1-inward),
                (x*Math.sin(angle)+y*Math.cos(angle))*(1-inward),
                release*.32+join*(.07+slot*.018)*(1-inward)-inward*.12+Math.sin(inward*Math.PI)*.22);
    }
    public static Vec3 decoration(double time,int index){
        double along=(time*.012+index*.8)%8;
        int segment=(int)along;double f=along-segment;
        var a=point(segment);var b=point((segment+1)%8);
        double x=(a.x+(b.x-a.x)*f)*1.055,y=(a.y+(b.y-a.y)*f)*1.055;
        double helix=time*.08+index*2.4,r=Math.hypot(x,y);
        return new Vec3(x+x/r*Math.cos(helix)*.085,y+y/r*Math.cos(helix)*.085,
                .04+Math.sin(helix)*.14);
    }
    private static Vec3 point(int index){return new Vec3(NODES[index][0],NODES[index][1],0);}
    private static double curve(double a,double b,double c,double d,double t){
        return .5*((2*b)+(-a+c)*t+(2*a-5*b+4*c-d)*t*t+(-a+3*b-3*c+d)*t*t*t);
    }
    public static String role(int slot){return slot==0?"target":slot==1?"shape":slot<5?"element":slot<8?"modifier":
            slot==SUBSTRATE?"substrate":slot==INK?"ink":slot==CORE?"core":"output";}
    private AggregationLayout(){}
}
