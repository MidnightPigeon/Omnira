package com.mcmagic.omnira.space.labyrinth;

import com.mcmagic.omnira.spacetime.CorridorLayout;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;

/** Vertical first, then a small horizontal search; does not carve destination terrain. */
public final class LongRangeTransit {
    public static final int LANDING_RADIUS=4;
    public static Vec3 landing(ServerLevel level,Entity root,boolean time){
        int min=time?CorridorLayout.TIME_BASE+1:CorridorLayout.SPACE_BASE+1;
        int max=time?level.getMaxBuildHeight()-1:CorridorLayout.SPACE_CEILING-1;
        var offsets=new ArrayList<BlockPos>();
        for(int dx=-LANDING_RADIUS;dx<=LANDING_RADIUS;dx++)for(int dz=-LANDING_RADIUS;dz<=LANDING_RADIUS;dz++)
            if(dx*dx+dz*dz<=LANDING_RADIUS*LANDING_RADIUS)offsets.add(new BlockPos(dx,0,dz));
        offsets.sort(Comparator.comparingInt(p->p.getX()*p.getX()+p.getZ()*p.getZ()));
        for(var offset:offsets){
            double x=root.getX()+offset.getX(),z=root.getZ()+offset.getZ();
            int preferred=time?CorridorLayout.timeSurface(net.minecraft.util.Mth.floor(x),net.minecraft.util.Mth.floor(z))+1:41;
            var heights=new ArrayList<Integer>();for(int y=min;y<=max;y++)heights.add(y);
            heights.sort(Comparator.comparingInt(y->Math.abs(y-preferred)));
            for(int y:heights){
                var point=new Vec3(x,y,z);boolean valid=true;
                for(var member:root.getSelfAndPassengers().toList()){
                    var box=member.getBoundingBox().move(point.subtract(root.position()));
                    if(box.minY<min||box.maxY>max||!level.getWorldBorder().isWithinBounds(box)){valid=false;break;}
                    for(int cx=net.minecraft.util.Mth.floor(box.minX)>>4;cx<=net.minecraft.util.Mth.floor(box.maxX)>>4;cx++)for(int cz=net.minecraft.util.Mth.floor(box.minZ)>>4;cz<=net.minecraft.util.Mth.floor(box.maxZ)>>4;cz++)level.getChunk(cx,cz);
                    if(!level.noCollision(box)){valid=false;break;}
                }
                if(!valid)continue;
                var below=BlockPos.containing(x,y-.01,z);
                var rootBox=root.getBoundingBox().move(point.subtract(root.position()));
                if(!level.noCollision(rootBox.move(0,-.05,0))||!level.getFluidState(below).isEmpty())return point;
            }
        }
        return null;
    }
    public static boolean transfer(ServerLevel level,Entity root,BlockPos source){
        if(!level.getBlockState(source).is(LabyrinthContent.LONG_RANGE_PORTAL.get()))return false;
        var point=landing(level,root,source.getY()<CorridorLayout.SEPARATOR_BOTTOM);if(point==null)return false;
        var contact=LabyrinthTransit.contact(level,root.getBoundingBox().move(point.subtract(root.position())));
        root.getSelfAndPassengers().forEach(e->{
            var data=e.getPersistentData();data.putLong("OmniraVoidPortalCooldown",level.getGameTime()+20);
            data.putBoolean("OmniraVoidPortalAtExit",contact!=null);
            if(contact!=null)data.putLong("OmniraVoidPortalExit",contact.pos().asLong());
        });
        return com.mcmagic.omnira.space.anchor.StabilityField.portalTravel(root,new net.minecraft.world.level.portal.DimensionTransition(level,point,Vec3.ZERO,root.getYRot(),root.getXRot(),moved->moved.getSelfAndPassengers().forEach(e->e.fallDistance=0)))!=null;
    }
    private LongRangeTransit(){}
}
