package com.mcmagic.omnira.paradox;

import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.space.anchor.StabilityField;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.*;

public final class ParadoxDustItem extends Item {
    public enum Paradox { BLINK, RESTORE, DREAM_CREATURE, VOID_ANVIL, MICROCORE }
    public static final TagKey<EntityType<?>> DREAMS=TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("omnira","dream_creatures"));
    public ParadoxDustItem(Properties properties){super(properties);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer server){
            trigger(server,Paradox.values()[player.getRandom().nextInt(Paradox.values().length)]);
            stack.consume(1,player);player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context){
        return context.getPlayer()==null?InteractionResult.PASS:use(context.getLevel(),context.getPlayer(),context.getHand()).getResult();
    }
    public static boolean trigger(ServerPlayer player,Paradox paradox){
        var level=player.serverLevel();
        return switch(paradox){
            case BLINK -> {
                var target=player.position().add(player.getLookAngle().normalize().scale(20));
                yield !StabilityField.blocks(player,level,target)&&player.teleportTo(level,target.x,target.y,target.z,
                        Set.of(),player.getYRot(),player.getXRot());
            }
            case RESTORE -> {player.setHealth(player.getMaxHealth());player.getFoodData().setFoodLevel(20);player.getFoodData().setSaturation(20);yield true;}
            case DREAM_CREATURE -> {
                var types=new ArrayList<EntityType<?>>();
                BuiltInRegistries.ENTITY_TYPE.getTag(DREAMS).ifPresent(tag->tag.forEach(holder->types.add(holder.value())));
                if(types.isEmpty())yield false;
                var entity=types.get(player.getRandom().nextInt(types.size())).create(level);
                if(!(entity instanceof Mob mob))yield false;
                mob.moveTo(player.getX()+2,player.getY(),player.getZ(),player.getYRot(),0);
                mob.finalizeSpawn(level,level.getCurrentDifficultyAt(mob.blockPosition()),MobSpawnType.MOB_SUMMONED,null);
                yield level.addFreshEntity(mob);
            }
            case VOID_ANVIL -> {
                var anvil=ParadoxContent.ANVIL.get().create(level);
                anvil.setPos(player.getX(),player.getY()+player.getBbHeight()+3,player.getZ());
                anvil.setStartPos(anvil.blockPosition());yield level.addFreshEntity(anvil);
            }
            case MICROCORE -> {
                var cores=List.of(ModItems.EARTH_MICROCORE,ModItems.WATER_MICROCORE,ModItems.FIRE_MICROCORE,ModItems.AIR_MICROCORE);
                var reward=new ItemStack(cores.get(player.getRandom().nextInt(cores.size())).get());
                if(!player.getInventory().add(reward))player.drop(reward,false);yield true;
            }
        };
    }
}
