package com.mcmagic.omnira.alchemy;

import java.util.ArrayList;
import java.util.List;

/** Shared logical screen coordinates for fluid drawing and selection. */
public final class ReactionPoolFluidLayout {
    public record Layer(int row,int top,int bottom){}
    public static List<Layer> layers(PoolFluids fluids,int capacity){
        var layers=new ArrayList<Layer>();int bottom=100;
        for(int row=fluids.count()-1;row>=0;row--){
            int depth=Math.max(1,70*fluids.getFluidInTank(row).getAmount()/capacity);
            int top=Math.max(30,bottom-depth);
            if(top<bottom)layers.add(new Layer(row,top,bottom));bottom=top;
        }
        return layers;
    }
    public static int rowAt(PoolFluids fluids,int capacity,double x,double y){
        if(x>=71&&x<144&&y>=27&&y<103){
            int row=(int)(y-27)/19;
            return row<fluids.count()&&y<27+row*19+17?row:-1;
        }
        if(x>=22&&x<63)for(var layer:layers(fluids,capacity))if(y>=layer.top&&y<layer.bottom)return layer.row;
        return -1;
    }
    private ReactionPoolFluidLayout(){}
}
