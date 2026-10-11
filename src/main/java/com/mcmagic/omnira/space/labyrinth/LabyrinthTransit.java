package com.mcmagic.omnira.space.labyrinth;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid="omnira")
public final class LabyrinthTransit {
    public static final int RANGE=2;
    public record Contact(BlockPos pos,Direction outward){}
    private record Mapping(LabyrinthTerrain.Room room,Direction outward){}
    private static final ThreadLocal<Map<Mapping,List<LabyrinthTerrain.Room>>> CANDIDATES=ThreadLocal.withInitial(()->new LinkedHashMap<>(256,.75F,true){
        @Override protected boolean removeEldestEntry(Map.Entry<Mapping,List<LabyrinthTerrain.Room>> e){return size()>256;}
    });
    public static Contact contact(ServerLevel level,AABB box){
        var scan=box.inflate(.7);
        int minX=net.minecraft.util.Mth.floor(scan.minX),maxX=net.minecraft.util.Mth.floor(scan.maxX);
        int minZ=net.minecraft.util.Mth.floor(scan.minZ),maxZ=net.minecraft.util.Mth.floor(scan.maxZ);
        int minY=Math.max(level.getMinBuildHeight(),net.minecraft.util.Mth.floor(scan.minY));
        int maxY=Math.min(level.getMaxBuildHeight()-1,net.minecraft.util.Mth.floor(scan.maxY));
        for(int cx=minX>>4;cx<=maxX>>4;cx++)for(int cz=minZ>>4;cz<=maxZ>>4;cz++){
            var chunk=level.getChunkSource().getChunkNow(cx,cz);if(chunk==null)continue;
            for(int sy=minY>>4;sy<=maxY>>4;sy++){
                var section=chunk.getSection(chunk.getSectionIndex(sy<<4));
                if(!section.maybeHas(s->s.getBlock() instanceof VoidPortalBlock))continue;
                for(var pos:BlockPos.betweenClosed(Math.max(minX,cx<<4),Math.max(minY,sy<<4),Math.max(minZ,cz<<4),
                        Math.min(maxX,(cx<<4)+15),Math.min(maxY,(sy<<4)+15),Math.min(maxZ,(cz<<4)+15))){
                    var state=section.getBlockState(pos.getX()&15,pos.getY()&15,pos.getZ()&15);
                    if(state.getBlock() instanceof VoidPortalBlock&&VoidPortalBlock.bounds(state,pos).intersects(box))return new Contact(pos.immutable(),state.getValue(VoidPortalBlock.FACING));
                }
            }
        }
        return null;
    }
    public static List<LabyrinthTerrain.Room> candidates(LabyrinthTerrain.Room from,Direction outward,boolean compass){
        var result=new ArrayList<LabyrinthTerrain.Room>();
        if(compass){var next=from.adjacent(outward);if(LabyrinthTerrain.exists(next)&&LabyrinthTerrain.plan(next).doors().containsKey(outward.getOpposite()))result.add(next);return result;}
        return new ArrayList<>(CANDIDATES.get().computeIfAbsent(new Mapping(from,outward),LabyrinthTransit::mapping));
    }
    private static List<LabyrinthTerrain.Room> mapping(Mapping mapping){
        var from=mapping.room();var outward=mapping.outward();var result=new ArrayList<LabyrinthTerrain.Room>();
        for(int x=-RANGE;x<=RANGE;x++)for(int y=-RANGE;y<=RANGE;y++)for(int z=-RANGE;z<=RANGE;z++){
            if(x==0&&y==0&&z==0)continue;
            var room=new LabyrinthTerrain.Room(from.x()+x,from.y()+y,from.z()+z);
            if(LabyrinthTerrain.exists(room)&&LabyrinthTerrain.plan(room).doors().containsKey(outward.getOpposite()))result.add(room);
        }
        return List.copyOf(result);
    }
    public static boolean stable(Entity root){return root.getSelfAndPassengers().anyMatch(e->e instanceof Player player&&player.isHolding(LabyrinthContent.COMPASS.get()));}
    public static Vec3 arrival(ServerLevel level,Entity root,LabyrinthTerrain.Room room,Direction exit){
        var portal=LabyrinthTerrain.plan(room).doors().get(exit);if(portal==null)return null;
        var inward=exit.getOpposite();
        for(int radius=0;radius<=1;radius++)for(int a=-radius;a<=radius;a++)for(int b=-radius;b<=radius;b++){
            if(Math.max(Math.abs(a),Math.abs(b))!=radius)continue;
            double x=portal.getX()+.5+inward.getStepX()*1.1,z=portal.getZ()+.5+inward.getStepZ()*1.1;
            double y=exit==Direction.UP?portal.getY()-root.getBbHeight()-.1:room.floor()+1;
            if(exit.getAxis()==Direction.Axis.Y){x+=a;z+=b;}else if(exit.getAxis()==Direction.Axis.X){z+=a;y+=b;}else{x+=a;y+=b;}
            var point=new Vec3(x,y,z);var o=room.origin();
            if(x<o.getX()+1||x>o.getX()+6||z<o.getZ()+1||z>o.getZ()+6||y<room.floor()+1)continue;
            boolean valid=true;
            for(var member:root.getSelfAndPassengers().toList()){
                var box=member.getBoundingBox().move(point.subtract(root.position()));
                for(int cx=net.minecraft.util.Mth.floor(box.minX)>>4;cx<=net.minecraft.util.Mth.floor(box.maxX)>>4;cx++)for(int cz=net.minecraft.util.Mth.floor(box.minZ)>>4;cz<=net.minecraft.util.Mth.floor(box.maxZ)>>4;cz++)level.getChunk(cx,cz);
                if(box.minX<o.getX()+1||box.maxX>o.getX()+6||box.minZ<o.getZ()+1||box.maxZ>o.getZ()+6||box.minY<room.floor()+1||box.maxY>room.ceiling()||!level.getWorldBorder().isWithinBounds(box)||!level.noCollision(box)){valid=false;break;}
            }
            if(valid)return point;
        }
        return null;
    }
    public static boolean transfer(ServerLevel level,Entity root,BlockPos portal,Direction outward){
        var from=LabyrinthTerrain.room(portal.relative(outward.getOpposite()));
        var choices=candidates(from,outward,stable(root));Collections.shuffle(choices,new Random(level.random.nextLong()));
        for(var room:choices){
            var exit=outward.getOpposite();var door=LabyrinthTerrain.plan(room).doors().get(exit);level.getChunkAt(door);
            if(!level.getBlockState(door).is(LabyrinthContent.PORTAL.get())||level.getBlockState(door).getValue(VoidPortalBlock.FACING)!=exit)continue;
            var point=arrival(level,root,room,exit);if(point==null)continue;
            root.getSelfAndPassengers().forEach(e->{var tag=e.getPersistentData();tag.putLong("OmniraVoidPortalCooldown",level.getGameTime()+20);tag.putLong("OmniraVoidPortalExit",door.asLong());tag.putBoolean("OmniraVoidPortalAtExit",true);});
            return com.mcmagic.omnira.space.anchor.StabilityField.portalTravel(root,new net.minecraft.world.level.portal.DimensionTransition(level,point,Vec3.ZERO,exit.getAxis()==Direction.Axis.Y?root.getYRot():exit.getOpposite().toYRot(),root.getXRot(),moved->moved.getSelfAndPassengers().forEach(e->e.fallDistance=0)))!=null;
        }
        return false;
    }
    @SubscribeEvent public static void tick(EntityTickEvent.Post event){
        var entity=event.getEntity();if(!(entity.level() instanceof ServerLevel level)||entity.isPassenger()||!entity.isAlive()||entity.isSpectator())return;
        var data=entity.getPersistentData();long now=level.getGameTime();
        if(data.getLong("OmniraVoidPortalCooldown")>now)return;
        var contact=contact(level,entity.getBoundingBox());
        if(contact==null){data.putBoolean("OmniraVoidPortalAtExit",false);return;}
        if(level.getBlockState(contact.pos()).is(LabyrinthContent.LONG_RANGE_PORTAL.get())){
            if(!data.getBoolean("OmniraVoidPortalAtExit")||data.getLong("OmniraVoidPortalExit")!=contact.pos().asLong())
                if(!LongRangeTransit.transfer(level,entity,contact.pos()))data.putLong("OmniraVoidPortalCooldown",now+10);
            return;
        }
        var outward=contact.outward();var room=LabyrinthTerrain.room(contact.pos().relative(outward.getOpposite()));var canonical=LabyrinthTerrain.plan(room).doors().get(outward);
        if(canonical==null||data.getBoolean("OmniraVoidPortalAtExit")&&data.getLong("OmniraVoidPortalExit")==canonical.asLong())return;
        if(!transfer(level,entity,canonical,outward))data.putLong("OmniraVoidPortalCooldown",now+10);
    }
    private LabyrinthTransit(){}
}
