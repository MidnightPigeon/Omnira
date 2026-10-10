package com.mcmagic.omnira.space.sea;

import com.mcmagic.omnira.spacetime.AnchoringSigilItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;

@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class SpatialImmunity {
    public static final TagKey<EntityType<?>> SPACE=TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.parse("omnira:space_creatures"));
    public static final TagKey<EntityType<?>> STARFLOW=TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.parse("omnira:starflow_creatures"));
    public static boolean protectedPlayer(Entity e){return e instanceof Player p&&(p.isCreative()||p.isSpectator()||AnchoringSigilItem.equipped(p));}
    public static boolean fissure(Entity e){return e.isSpectator()||protectedPlayer(e)||e.getType().is(SPACE);}
    public static boolean starflow(Entity e){return fissure(e)||e.getType().is(STARFLOW)||e instanceof Player&&e.getRootVehicle() instanceof VoidManta manta&&manta.isTame();}
    private static final TagKey<net.minecraft.world.damagesource.DamageType> MAGIC=TagKey.create(Registries.DAMAGE_TYPE,ResourceLocation.parse("c:is_magic"));
    public static boolean evades(Entity entity,net.minecraft.world.damagesource.DamageSource source){
        if(!entity.getType().is(SPACE) || source instanceof com.mcmagic.omnira.spell.SpellDamageSource
                || source.getDirectEntity() instanceof com.mcmagic.omnira.spell.entity.SpellEntity || source.is(MAGIC))return false;
        return source.getEntity()!=null && source.is(com.mcmagic.omnira.entity.DreamMirror.PHYSICAL);
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void evade(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event){
        if(evades(event.getEntity(),event.getSource()))event.setCanceled(true);
    }
    private SpatialImmunity(){}
}
