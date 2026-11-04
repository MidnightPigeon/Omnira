package com.mcmagic.omnira.visual;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import com.mcmagic.omnira.space.anchor.*;
import com.mcmagic.omnira.space.sea.*;
import com.mcmagic.omnira.codex.MyriadCodexEntity;
import com.mcmagic.omnira.time.WonderlandPokerStandBlock;

/** Visual-only, on-demand range descriptions. No ticking server task or world mutation. */
@EventBusSubscriber(modid="omnira")
public final class MachineRangePreview {
    public record Range(BlockPos source,int radius,boolean chunk){}
    private record Trigger(BlockPos source,long until){}
    private static final java.util.Map<net.minecraft.world.entity.player.Player,Trigger> RECENT=new java.util.WeakHashMap<>();
    public static final float SPEED=3F/20, HOLD=100, FADE=20;
    public static float extent(float age,float limit){return Math.min(limit,Math.max(0,age)*SPEED);}
    public static float alpha(float age,float limit){return Math.clamp(1-(age-limit/SPEED-HOLD)/FADE,0,1);}
    /** Opacity along the full build height; shared by walls, corners and markings. */
    public static float verticalAlpha(float y,float bottom,float top){return top<=bottom?0:Math.clamp((top-y)/(top-bottom),0,1);}
    public static Range find(Level level,BlockPos pos){
        var state=level.getBlockState(pos);
        if(state.getBlock() instanceof WonderlandPokerStandBlock){
            if(state.getValue(WonderlandPokerStandBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)pos=pos.below();
            if(level.getBlockEntity(pos) instanceof com.mcmagic.omnira.time.WonderlandPokerStandBlockEntity
                    &&level.getBlockState(pos.above()).getBlock()==state.getBlock())return new Range(pos.immutable(),8,true);
            return null;
        }
        if(state.is(AnchorContent.ACTIVE.get()))return new Range(pos.immutable(),StabilityField.RADIUS,false);
        var be=level.getBlockEntity(pos);
        if(be instanceof com.mcmagic.omnira.block.entity.CrystalBallBlockEntity ball&&ball.upgrades.has(com.mcmagic.omnira.item.OrbUpgradeItem.Kind.INFUSED_INTAKE))return new Range(pos.immutable(),com.mcmagic.omnira.storage.InfusedIntake.RADIUS,false);
        if(be instanceof StarClusterOrbEntity orb&&orb.stabilized())return new Range(pos.immutable(),32,false);
        if(be instanceof StarlightEntity&&state.is(StarlightContent.SOURCE.get()))return new Range(pos.immutable(),64,false);
        if(MyriadCodexEntity.from(be)!=null)return new Range(pos.immutable(),32,false);
        if(be instanceof com.mcmagic.omnira.fate.SuitcaseEntity suitcase&&suitcase.musical())return new Range(pos.immutable(),10,false);
        var workstation=com.mcmagic.omnira.workstation.CrystalWorkstations.work(be);
        if(workstation!=null&&workstation.kind()==com.mcmagic.omnira.workstation.CrystalWorkstations.Kind.ANVIL&&!workstation.infused())return new Range(pos.immutable(),5,false);
        if(state.getBlock() instanceof com.mcmagic.omnira.block.LightCrystalCoreBlock&&state.getValue(com.mcmagic.omnira.block.LightCrystalCoreBlock.GROWING))return new Range(pos.immutable(),5,false);
        if(state.getBlock() instanceof com.mcmagic.omnira.spacetime.TimeWarpPointBlock)return new Range(pos.immutable(),com.mcmagic.omnira.spacetime.TimeWarpPointBlock.RADIUS,false);
        if(state.is(com.mcmagic.omnira.registry.ModBlocks.SPACE_WARP_POINT.get())||state.is(com.mcmagic.omnira.registry.ModBlocks.SPATIAL_REMNANT.get())||state.is(com.mcmagic.omnira.space.abyss.AbyssContent.MASS.get()))return new Range(pos.immutable(),5,false);
        int deflection=com.mcmagic.omnira.space.geodes.DeflectionContent.radius(state);
        if(deflection>0)return new Range(pos.immutable(),deflection,false);
        return null;
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void use(PlayerInteractEvent.RightClickBlock event){
        var player=event.getEntity();
        if(event.getHand()!=InteractionHand.MAIN_HAND||!player.isShiftKeyDown()||!player.getMainHandItem().isEmpty()||player.isSpectator())return;
        var range=find(event.getLevel(),event.getPos());if(range==null)return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if(event.getLevel() instanceof net.minecraft.server.level.ServerLevel server){
            long now=server.getGameTime();var previous=RECENT.get(player);
            if(previous!=null&&previous.source().equals(range.source())&&now<previous.until())return;
            RECENT.put(player,new Trigger(range.source(),now+(long)Math.ceil(range.radius()/SPEED+HOLD+FADE)));
            var center=range.source().getCenter();
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(server,null,center.x,center.y,center.z,range.radius()+32,new RangePreviewPayload(range.source(),range.radius(),range.chunk()));
        }
    }
    private MachineRangePreview(){}
}
