package com.mcmagic.omnira.mire;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.registries.*;

/** Effect-clock timing also follows the Fleeting Woods' accelerated effect ticks. */
@EventBusSubscriber(modid="omnira")
public final class TimePassage extends MobEffect {
    public static final String RECORD="OmniraTimePassage";
    public static final DeferredRegister<Potion> POTIONS=DeferredRegister.create(Registries.POTION,"omnira");
    public static final DeferredHolder<Potion,Potion> NORMAL=POTIONS.register("time",()->potion(600,0));
    public static final DeferredHolder<Potion,Potion> LONG=POTIONS.register("long_time",()->potion(1800,0));
    public static final DeferredHolder<Potion,Potion> STRONG=POTIONS.register("strong_time",()->potion(600,1));
    public TimePassage(){super(MobEffectCategory.BENEFICIAL,0xB4D5A0);}
    private static Potion potion(int ticks,int amplifier){return new Potion("time",new MobEffectInstance(ModEffects.TIME_PASSAGE,ticks,amplifier));}
    @SubscribeEvent public static void brew(RegisterBrewingRecipesEvent event){
        var b=event.getBuilder();b.addMix(Potions.AWKWARD,MireContent.EEL_ITEM.get(),NORMAL);
        b.addMix(NORMAL,Items.REDSTONE,LONG);b.addMix(NORMAL,Items.GLOWSTONE_DUST,STRONG);
    }
    public static void remember(ServerPlayer p){
        var n=new CompoundTag();n.putString("Dimension",p.level().dimension().location().toString());
        n.putDouble("X",p.getX());n.putDouble("Y",p.getY());n.putDouble("Z",p.getZ());
        n.putFloat("Health",p.getHealth());n.putInt("Food",p.getFoodData().getFoodLevel());
        n.putFloat("Saturation",p.getFoodData().getSaturationLevel());p.getPersistentData().put(RECORD,n);
    }
    @SubscribeEvent public static void added(MobEffectEvent.Added event){
        if(!(event.getEntity() instanceof ServerPlayer p)||!event.getEffectInstance().is(ModEffects.TIME_PASSAGE))return;
        var old=event.getOldEffectInstance();var next=event.getEffectInstance();
        if(old==null)remember(p);
    }
    @SubscribeEvent public static void died(LivingDeathEvent event){event.getEntity().getPersistentData().remove(RECORD);}
    @Override public boolean shouldApplyEffectTickThisTick(int duration,int amplifier){return true;}
    @Override public boolean applyEffectTick(LivingEntity entity,int amplifier){
        if(!(entity instanceof ServerPlayer p)||!p.isAlive())return true;
        var effect=p.getEffect(ModEffects.TIME_PASSAGE);if(effect==null)return true;
        if(!p.getPersistentData().contains(RECORD))remember(p);
        var n=p.getPersistentData().getCompound(RECORD);int elapsed=n.getInt("Elapsed")+1;n.putInt("Elapsed",elapsed);
        if(effect.getDuration()==1||(amplifier==0&&elapsed%600==0))restore(p,amplifier);
        return true;
    }
    public static void restore(ServerPlayer p,int amplifier){
        if(!p.isAlive()||!p.getPersistentData().contains(RECORD))return;
        var n=p.getPersistentData().getCompound(RECORD);
        var dimension=ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(n.getString("Dimension")));
        var destination=p.server.getLevel(dimension);
        if(destination!=null){
            p.stopRiding();p.teleportTo(destination,n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z"),p.getYRot(),p.getXRot());
            p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        }
        p.setHealth(amplifier>0?p.getMaxHealth():Math.min(p.getMaxHealth(),n.getFloat("Health")));
        p.getFoodData().setFoodLevel(amplifier>0?20:n.getInt("Food"));
        p.getFoodData().setSaturation(amplifier>0?20:n.getFloat("Saturation"));
        if(amplifier>0){
            var mana=p.getData(ModAttachments.MANA).withMaximum(p.getAttributeValue(ModAttributes.MAX_MANA));
            p.setData(ModAttachments.MANA,mana.withCurrent(mana.maximum()));
            for(var slot:EquipmentSlot.values())if(slot!=EquipmentSlot.BODY){
                var stack=p.getItemBySlot(slot);if(stack.isDamageableItem())stack.setDamageValue(0);
            }
        }
        p.serverLevel().sendParticles(ModParticles.TIME_WARP_SPARK.get(),p.getX(),p.getY()+1,p.getZ(),24,.4,.7,.4,.025);
    }
}
