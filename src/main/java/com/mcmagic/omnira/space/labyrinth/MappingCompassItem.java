package com.mcmagic.omnira.space.labyrinth;

import com.mcmagic.omnira.mixin.CompassRespawnAccessor;
import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class MappingCompassItem extends Item {
    public static final int DURATION=60;
    private static final String BINDING="OmniraCompassRespawn";
    public MappingCompassItem(Properties properties){super(properties.stacksTo(1));}
    public static boolean bound(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().contains(BINDING);}
    public static boolean bind(ServerPlayer player,ItemStack stack){
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.remove(BINDING);
        var pos=player.getRespawnPosition();var level=player.server.getLevel(player.getRespawnDimension());
        if(pos!=null&&level!=null){
            level.getChunkAt(pos);var tag=new CompoundTag();tag.putString("Dimension",level.dimension().location().toString());
            tag.putLong("Pos",pos.asLong());tag.putFloat("Angle",player.getRespawnAngle());tag.putBoolean("Forced",player.isRespawnForced());
            tag.putString("Block",net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());data.put(BINDING,tag);
        }
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return data.contains(BINDING);
    }
    public static DimensionTransition destination(ServerPlayer player,ItemStack stack){
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(BINDING);
        var id=ResourceLocation.tryParse(tag.getString("Dimension"));
        if(id!=null&&tag.contains("Pos")){
            var level=player.server.getLevel(ResourceKey.create(Registries.DIMENSION,id));
            if(level!=null){
                var pos=net.minecraft.core.BlockPos.of(tag.getLong("Pos"));level.getChunkAt(pos);
                if(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString().equals(tag.getString("Block"))){
                    // Native resolver includes modded respawn blocks; keepInventory prevents anchor-charge consumption.
                    var resolved=CompassRespawnAccessor.omnira$respawn(level,pos,tag.getFloat("Angle"),tag.getBoolean("Forced"),true);
                    if(resolved.isPresent()){var target=resolved.get();return new DimensionTransition(level,target.position(),Vec3.ZERO,target.yaw(),0,DimensionTransition.DO_NOTHING);}
                }
            }
        }
        return new DimensionTransition(player.server.overworld(),player,DimensionTransition.DO_NOTHING);
    }
    public static boolean returnHome(ServerPlayer player,ItemStack stack){
        var target=destination(player,stack);
        if(com.mcmagic.omnira.space.anchor.StabilityField.blocks(player,target.newLevel(),target.pos()))return false;
        var source=player.serverLevel();var origin=player.position();
        if(!player.teleportTo(target.newLevel(),target.pos().x,target.pos().y,target.pos().z,Set.of(),target.yRot(),target.xRot()))return false;
        player.setData(ModAttachments.MANA,player.getData(ModAttachments.MANA).withCurrent(0));
        player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
        source.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,origin.x,origin.y+1,origin.z,12,.3,.5,.3,.02);
        target.newLevel().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,net.minecraft.sounds.SoundSource.PLAYERS,.7F,1.3F);
        target.newLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,player.getX(),player.getY()+1,player.getZ(),12,.3,.5,.3,.02);
        return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(player.isShiftKeyDown()){
            if(player instanceof ServerPlayer server)server.displayClientMessage(Component.translatable(bind(server,stack)?"message.omnira.compass.bound":"message.omnira.compass.world_spawn"),true);
        }else if(player.isAlive()&&!player.isSpectator())player.startUsingItem(hand);
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public int getUseDuration(ItemStack stack,LivingEntity user){return DURATION;}
    @Override public void onUseTick(Level level,LivingEntity user,ItemStack stack,int remaining){
        if(level instanceof net.minecraft.server.level.ServerLevel server&&remaining%5==0){
            double phase=(DURATION-remaining)*.3;
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,user.getX()+Math.cos(phase)*.4,user.getY()+1,user.getZ()+Math.sin(phase)*.4,1,0,0,0,0);
        }
    }
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){
        var player=context.getPlayer();if(player==null)return InteractionResult.PASS;
        return use(context.getLevel(),player,context.getHand()).getResult();
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user){
        if(user instanceof ServerPlayer player&&!returnHome(player,stack))player.displayClientMessage(Component.translatable("message.omnira.recall.blocked"),true);
        return stack;
    }
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> lines,TooltipFlag flag){
        lines.add(Component.translatable("tooltip.omnira.compass.bind").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.omnira.compass.recall").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
