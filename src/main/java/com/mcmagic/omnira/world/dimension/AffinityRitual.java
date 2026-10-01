package com.mcmagic.omnira.world.dimension;

import com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity;
import com.mcmagic.omnira.mana.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid="omnira")
public final class AffinityRitual {
    public static final int DURATION=120;
    private static final java.util.Map<java.util.UUID,Session> ACTIVE=new java.util.HashMap<>();
    private static final class Session {
        final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        final BlockPos pos;
        final Affinity selected;
        final AffinityState before;
        final InteractionHand hand;
        final ItemStack offering;
        int ticks;
        Session(ServerPlayer player,BlockPos pos,InteractionHand hand,Affinity selected) {
            this.pos=pos.immutable();dimension=player.level().dimension();this.hand=hand;this.selected=selected;
            before=player.getData(ModAttachments.AFFINITY);offering=player.getItemInHand(hand).copyWithCount(1);
        }
        boolean removing(){return before.affinity()==selected;}
    }
    private AffinityRitual() {}
    public static java.util.List<Item> ingredients() {
        return java.util.List.of(ModItems.EARTH_MICROCORE.get(),ModItems.WATER_MICROCORE.get(),ModItems.FIRE_MICROCORE.get(),
                ModItems.AIR_MICROCORE.get(),ModItems.LIGHT_MICROCORE.get(),ModItems.DARK_MICROCORE.get(),
                ModItems.PARADOX_DUST.get(),DreamContent.DREAM_CRYSTAL_BEDROCK.get().asItem());
    }
    private static java.util.List<CrystalPedestalBlockEntity> pedestals(ServerPlayer player,BlockPos pos) {
        var level=player.serverLevel();var missing=new java.util.HashSet<>(ingredients());
        var result=new java.util.ArrayList<CrystalPedestalBlockEntity>();
        for(var offset:DreamRitual.ANCHORS) {
            var p=pos.offset(offset[0],0,offset[1]);
            if(!level.hasChunkAt(p) || !level.mayInteract(player,p)
                    || !(level.getBlockEntity(p) instanceof CrystalPedestalBlockEntity pedestal)
                    || pedestal.getItem(0).isEmpty() || !missing.remove(pedestal.getItem(0).getItem()))return java.util.List.of();
            result.add(pedestal);
        }
        return missing.isEmpty()?result:java.util.List.of();
    }
    public static boolean inside(ServerPlayer player,BlockPos pos) {
        double dx=player.getX()-pos.getX()-.5,dz=player.getZ()-pos.getZ()-.5;
        return dx*dx+dz*dz<9 && player.getY()>=pos.getY()-.5 && player.getY()<=pos.getY()+3;
    }
    public static boolean active(ServerPlayer player){return ACTIVE.containsKey(player.getUUID());}
    private static boolean message(ServerPlayer player,String key) {
        player.displayClientMessage(Component.translatable("message.omnira.affinity."+key),true);return false;
    }
    public static boolean activate(ServerPlayer player,BlockPos pos,InteractionHand hand) {
        var level=player.serverLevel();var selected=Affinity.offering(player.getItemInHand(hand));
        var state=player.getData(ModAttachments.AFFINITY);
        if(selected==Affinity.NONE || !player.isAlive() || player.isSpectator() || player.isPassenger()
                || !level.mayInteract(player,pos))return false;
        if(!level.getBlockState(pos).is(ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get()))return message(player,"advanced_core");
        if(state.penaltyTicks()>0 || (state.affinity()!=Affinity.NONE && state.affinity()!=selected))return message(player,"locked");
        if(!inside(player,pos))return message(player,"inside");
        if(active(player) || ACTIVE.values().stream().anyMatch(s->s.pos.equals(pos)&&s.dimension==level.dimension()))return message(player,"busy");
        if(pedestals(player,pos).size()!=8)return message(player,"invalid");
        var session=new Session(player,pos,hand,selected);
        if(!session.removing() && (!clear(player,session,0) || !clear(player,session,1)))return message(player,"obstructed");
        ACTIVE.put(player.getUUID(),session);
        if(!session.removing())hold(player,session,0);
        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,net.minecraft.sounds.SoundSource.BLOCKS,1,.8F);
        return true;
    }
    private static Vec3 position(Session session,double progress) {
        return Vec3.atBottomCenterOf(session.pos).add(0,1.05+progress,0);
    }
    private static boolean clear(ServerPlayer player,Session session,double progress) {
        return player.level().noCollision(player,player.getBoundingBox().move(position(session,progress).subtract(player.position())));
    }
    private static void hold(ServerPlayer player,Session session,double progress) {
        var p=position(session,progress);player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
        player.setPos(p);
        player.connection.teleport(p.x,p.y,p.z,player.getYRot(),player.getXRot());
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(event.getEntity() instanceof ServerPlayer player)advance(player);
    }
    public static void advance(ServerPlayer player) {
        var session=ACTIVE.get(player.getUUID());if(session==null)return;
        var level=player.serverLevel();double progress=(session.ticks+1)/(double)DURATION;
        if(!player.isAlive() || player.isSpectator() || player.isPassenger() || player.level().dimension()!=session.dimension
                || !level.hasChunkAt(session.pos) || !level.mayInteract(player,session.pos)
                || !level.getBlockState(session.pos).is(ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get())
                || !player.getData(ModAttachments.AFFINITY).equals(session.before)
                || !player.getItemInHand(session.hand).is(session.offering.getItem())
                || (session.removing()?!inside(player,session.pos):!clear(player,session,progress))) {cancel(player);return;}
        var items=pedestals(player,session.pos);
        if(items.size()!=8){cancel(player);return;}
        if(!session.removing())hold(player,session,progress);
        else if(session.selected==Affinity.DREAM)player.setData(ModAttachments.DREAM_SOLIDIFY,(float)progress);
        session.ticks++;
        if(session.ticks%2==0)particles(player,session.selected,session.ticks);
        if(session.ticks<DURATION)return;
        ACTIVE.remove(player.getUUID());
        player.getItemInHand(session.hand).shrink(1);
        boolean removing=session.removing();
        player.setData(ModAttachments.AFFINITY,removing && session.selected==Affinity.DREAM?AffinityState.NONE:
                new AffinityState(session.selected.ordinal(),removing?session.selected.penaltyDuration():0));
        player.setData(ModAttachments.DREAM_SOLIDIFY,0F);
        AffinityEffects.refresh(player);
        player.displayClientMessage(Component.translatable(removing && session.selected==Affinity.DREAM?"message.omnira.affinity.cleared":
                removing?"message.omnira.affinity.removing":"message.omnira.affinity.bound",Component.translatable(session.selected.key())),true);
    }
    public static void cancel(ServerPlayer player){ACTIVE.remove(player.getUUID());player.setData(ModAttachments.DREAM_SOLIDIFY,0F);message(player,"cancelled");}
    private static void particles(ServerPlayer player,Affinity affinity,int ticks) {
        int[] colors=switch(affinity) {
            case ELEMENTAL -> new int[]{0xE5B76B,0x6CBCEC,0xF28D81,0xACDFB4};
            case DREAM -> new int[]{0x85ADFF,0xC093F5};
            default -> new int[]{affinity.color};
        };
        for(int i=0;i<6;i++) {
            double angle=ticks*.12+i*Math.PI/3;int c=colors[(ticks/8+i)%colors.length];
            var dust=new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f((c>>16&255)/255F,(c>>8&255)/255F,(c&255)/255F),.8F);
            player.serverLevel().sendParticles(dust,player.getX()+Math.cos(angle)*.7,player.getY()+.2+(i%3)*.5,
                    player.getZ()+Math.sin(angle)*.7,1,.02,.05,.02,0);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        ACTIVE.remove(event.getEntity().getUUID());event.getEntity().setData(ModAttachments.DREAM_SOLIDIFY,0F);
    }
    @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event){ACTIVE.clear();}
}
