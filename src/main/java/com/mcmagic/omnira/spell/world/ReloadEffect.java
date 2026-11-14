package com.mcmagic.omnira.spell.world;

import com.mcmagic.omnira.spell.SpellEffect;
import com.mcmagic.omnira.spell.UtilitySpellEffects;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Random replacements use registry content, with explicit extension tags for creature families. */
public final class ReloadEffect {
    private record Pool(List<BlockState> vanillaBlocks,List<BlockState> allBlocks,Map<WorldEntityKinds.Kind,List<EntityType<?>>> vanillaMobs,Map<WorldEntityKinds.Kind,List<EntityType<?>>> allMobs){}
    private static final Map<ServerLevel,Pool> POOLS=new WeakHashMap<>();
    public static boolean blocked(net.minecraft.world.level.block.Block block){return com.mcmagic.omnira.config.OmniraConfig.RELOAD_BLOCK_BLACKLIST.get().contains(BuiltInRegistries.BLOCK.getKey(block).toString());}
    public static boolean blocked(EntityType<?> type){return com.mcmagic.omnira.config.OmniraConfig.RELOAD_ENTITY_BLACKLIST.get().contains(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());}
    public static boolean completeBlock(BlockState state,ServerLevel level,BlockPos pos){return !state.isAir()&&!state.hasBlockEntity()
            &&state.getFluidState().isEmpty()&&state.isCollisionShapeFullBlock(level,pos);}
    private static Pool pool(ServerLevel level){return POOLS.computeIfAbsent(level,l->{
        var vanillaBlocks=new ArrayList<BlockState>();var allBlocks=new ArrayList<BlockState>();
        for(var block:BuiltInRegistries.BLOCK){var state=block.defaultBlockState();if(!completeBlock(state,l,BlockPos.ZERO))continue;
            allBlocks.add(state);if(BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("minecraft"))vanillaBlocks.add(state);}
        var vanillaMobs=new EnumMap<WorldEntityKinds.Kind,List<EntityType<?>>>(WorldEntityKinds.Kind.class);
        var allMobs=new EnumMap<WorldEntityKinds.Kind,List<EntityType<?>>>(WorldEntityKinds.Kind.class);
        for(var kind:WorldEntityKinds.Kind.values()){vanillaMobs.put(kind,new ArrayList<>());allMobs.put(kind,new ArrayList<>());}
        for(var type:BuiltInRegistries.ENTITY_TYPE){
            if(type==EntityType.PLAYER)continue;
            try{var entity=type.create(l);if(entity instanceof LivingEntity living&&WorldEntityKinds.eligible(living)){
                var kind=WorldEntityKinds.kind(living);allMobs.get(kind).add(type);
                if(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals("minecraft"))vanillaMobs.get(kind).add(type);
            }if(entity!=null)entity.discard();}catch(RuntimeException exception){com.mcmagic.omnira.Omnira.LOGGER.debug("Reload excludes entity factory {}",BuiltInRegistries.ENTITY_TYPE.getKey(type),exception);}
        }
        return new Pool(vanillaBlocks,allBlocks,vanillaMobs,allMobs);
    });}
    public static boolean block(SpellEffect effect,ServerPlayer player,BlockPos pos){
        var level=player.serverLevel();if(!UtilitySpellEffects.allowed(player,pos))return false;var before=level.getBlockState(pos);
        if(blocked(before.getBlock())||!completeBlock(before,level,pos))return false;
        var choices=(effect.infusion()>0?pool(level).allBlocks:pool(level).vanillaBlocks).stream()
                .filter(state->state.getBlock()!=before.getBlock()&&!blocked(state.getBlock())&&state.canSurvive(level,pos)).toList();
        if(choices.isEmpty())return false;
        if(net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(level,player.gameMode.getGameModeForPlayer(),player,pos,before).isCanceled())return false;
        var snapshot=net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
        if(!level.setBlock(pos,choices.get(level.random.nextInt(choices.size())),3))return false;
        if(net.neoforged.neoforge.event.EventHooks.onBlockPlace(player,snapshot,Direction.UP)){snapshot.restore();return false;}
        com.mcmagic.omnira.progression.BranchProgress.record(player,"rewrite_common_sense");
        return true;
    }
    public static List<EntityType<?>> livingCandidates(SpellEffect effect,LivingEntity target){
        if(!(target.level() instanceof ServerLevel level)||blocked(target.getType())||!WorldEntityKinds.eligible(target))return List.of();
        var kind=WorldEntityKinds.kind(target);
        if(effect.enhancement()==0&&(kind==WorldEntityKinds.Kind.HOSTILE||kind==WorldEntityKinds.Kind.BOSS))return List.of();
        return (effect.infusion()>0?pool(level).allMobs:pool(level).vanillaMobs).get(kind).stream().filter(type->type!=target.getType()&&!blocked(type)).toList();
    }
    public static boolean living(SpellEffect effect,LivingEntity target){
        return replaceLiving(effect,target)!=null;
    }
    public static Mob replaceLiving(SpellEffect effect,LivingEntity target){
        var choices=livingCandidates(effect,target);
        if(choices.isEmpty())return null;
        var level=(ServerLevel)target.level();
        var shuffled=new ArrayList<>(choices);Collections.shuffle(shuffled,new Random(level.random.nextLong()));
        for(var type:shuffled){
            var entity=type.create(level);if(!(entity instanceof Mob replacement))continue;
            replacement.moveTo(target.getX(),target.getY(),target.getZ(),target.getYRot(),target.getXRot());
            if(!replacementFits(level,target,replacement)){replacement.discard();continue;}
            replacement.finalizeSpawn(level,level.getCurrentDifficultyAt(target.blockPosition()),MobSpawnType.CONVERSION,null);
            if(!level.addFreshEntity(replacement)){replacement.discard();continue;}
            ReloadDragonEncounter.transfer(level,target,replacement);
            target.discard();return replacement;
        }
        return null;
    }
    public static void clear(){POOLS.clear();}
    /** Vanilla collision rules, excluding only the body and multipart children being replaced. */
    public static boolean replacementFits(ServerLevel level,LivingEntity target,Mob replacement){
        var box=replacement.getBoundingBox();
        // Dragons fly through terrain using their native multipart movement.
        if (!(replacement instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon))
            for(var shape:level.getBlockCollisions(replacement,box))if(!shape.isEmpty())return false;
        if(!level.getEntities(target,box.inflate(1.0E-7),entity->!entity.isSpectator()
                &&!(entity instanceof net.neoforged.neoforge.entity.PartEntity<?> part && part.getParent()==target)
                &&replacement.canCollideWith(entity)).isEmpty())return false;
        var border=level.getWorldBorder();
        return !border.isInsideCloseToBorder(replacement,box)||!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(
                border.getCollisionShape(),net.minecraft.world.phys.shapes.Shapes.create(box),net.minecraft.world.phys.shapes.BooleanOp.AND);
    }
    private ReloadEffect(){}
}
