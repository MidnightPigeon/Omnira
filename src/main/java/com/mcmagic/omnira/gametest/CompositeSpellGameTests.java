package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.spell.entity.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.menu.CrystalProcessingTableMenu;
import com.mcmagic.omnira.aggregation.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_composite_spells")
@PrefixGameTestTemplate(false)
public final class CompositeSpellGameTests {
    @GameTest(template="spell_arena")
    public static void slotsAndCrystalSerialization(GameTestHelper h){
        for(var core:List.of(ModItems.TIME_MICROCORE.get(),ModItems.SPACE_MICROCORE.get())){
            var input=new SimpleContainer(12);input.setItem(0,new ItemStack(core));
            var pattern=SpellPattern.fromMicrocores(input.getItem(0),ItemStack.EMPTY).orElseThrow();
            h.assertTrue(pattern.composite(),"Missing composite form");
            h.assertTrue(!SpellPattern.compatibleSlots(input,1,new ItemStack(ModItems.AIR_MICROCORE.get())),"Second slot accepted material");
            h.assertTrue(AggregationRingBlockEntity.accepts(0,input.getItem(0)),"Ring rejected target core");
            h.assertTrue(!AggregationRingBlockEntity.accepts(1,input.getItem(0)),"Ring accepted composite in slot two");
            var low=CrystalProcessingTableMenu.previewCrystal(input);
            var high=CrystalProcessingTableMenu.previewCrystal(input,5,8,2,5,AggregationContent.CRYSTAL.get());
            h.assertTrue(pattern.equals(low.get(ModDataComponents.SPELL_PATTERN))&&pattern.equals(high.get(ModDataComponents.SPELL_PATTERN)),"Writing tables disagree");
            var encoded=SpellPattern.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,pattern).getOrThrow();
            h.assertTrue(pattern.equals(SpellPattern.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,encoded).getOrThrow()),"Composite did not round-trip");
            input.setItem(1,new ItemStack(ModItems.AIR_MICROCORE.get()));
            h.assertTrue(SpellPattern.fromMicrocores(input.getItem(0),input.getItem(1)).isEmpty(),"Occupied ghost slot produced spell");
            h.assertTrue(!SpellPattern.compatibleSlots(input,0,input.getItem(0)),"Composite overwrote an existing shape");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void sweptContactGeometry(GameTestHelper h){
        h.assertTrue(!CompositeSpellMotion.shellTouches(new AABB(3,3,3,4,4,4),Vec3.ZERO,4,4.5),"Cube corner counted as sphere");
        h.assertTrue(CompositeSpellMotion.shellTouches(new AABB(4,0,0,5,1,1),Vec3.ZERO,4,4.5),"Partial sphere contact missed");
        h.assertTrue(!CompositeSpellMotion.shellTouches(new AABB(-.2,-.2,-.2,.2,.2,.2),Vec3.ZERO,3,4.5),"Inner volume triggered early");
        var from=Vec3.ZERO;var to=new Vec3(0,0,4);var right=new Vec3(1,0,0);var up=new Vec3(0,1,0);
        h.assertTrue(CompositeSpellMotion.bladeTouches(new AABB(.8,-.1,1,1.2,.1,2),from,to,right,up,1,.125),"Blade edge contact missed");
        h.assertTrue(!CompositeSpellMotion.bladeTouches(new AABB(0,1,1,1,2,2),from,to,right,up,1,.125),"Broad query box caused false hit");
        h.succeed();
    }
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h){
        var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"composite-test"));
        p.setPos(h.absoluteVec(new Vec3(5,3,5)));p.setYRot(0);return p;
    }
    private static SpellPayload payload(String keyword){return new SpellPayload(10,0,List.of(),keyword.isEmpty()?List.of():List.of(keyword));}
    @GameTest(template="spell_arena")
    public static void reverseFlowAndEnhancement(GameTestHelper h){
        var p=player(h);var center=p.getBoundingBox().getCenter();
        var target=EntityType.PIG.create(h.getLevel());target.setNoAi(true);target.setPos(center.add(3,0,0));h.getLevel().addFreshEntity(target);
        CompositeSpellMotion.cast(p,new SpellPattern(ElementType.TIME,null),payload("enhancement"),9,new SpellCasting.Modifiers(1,1));
        var spell=h.getLevel().getEntitiesOfClass(SpellEntity.class,p.getBoundingBox().inflate(6),e->e.ownedBy(p)).getFirst();
        h.assertTrue(spell.burstRadius()==4.5,"Spell power changed radius");
        h.assertTrue(p.getEffect(ModEffects.TEMPORAL_DISLOCATION).getDuration()==200&&p.getEffect(ModEffects.TEMPORAL_DISLOCATION).getAmplifier()==1,"Wrong dislocation effect");
        for(int i=0;i<10;i++)spell.tick();
        h.assertTrue(target.getEffect(net.minecraft.world.effect.MobEffects.WITHER).getAmplifier()==2,"Enhanced wither missing");
        h.assertTrue(target.getDeltaMovement().x<0,"Target not gathered inward");
        h.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.WITHER),"Caster hit by own contraction");
        for(int i=10;i<21;i++)spell.tick();h.assertTrue(spell.isRemoved(),"Single contraction did not finish");
        h.assertTrue(p.getEffect(ModEffects.TEMPORAL_DISLOCATION).getDuration()==200,"Contraction refreshed dislocation");
        target.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void workstationFiltersAndRingCompletion(GameTestHelper h){
        var p=player(h);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);h.getLevel().addFreshEntity(p);
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(5,3,5));
        try{
            AggregationStructure.assemble(h.getLevel(),pos,net.minecraft.core.Direction.NORTH);
            var machine=(AggregationRingBlockEntity)h.getLevel().getBlockEntity(pos);
            machine.setItem(0,new ItemStack(ModItems.SPACE_MICROCORE.get(),2));
            h.assertTrue(!machine.canPlaceItem(1,new ItemStack(ModItems.EARTH_MICROCORE.get())),"Automation filled reserved slot");
            var menu=new com.mcmagic.omnira.aggregation.AggregationRingMenu(1,p.getInventory(),machine);
            h.assertTrue(!menu.getSlot(1).mayPlace(new ItemStack(ModItems.EARTH_MICROCORE.get())),"Ring menu filled ghost");
            var lowInput=new SimpleContainer(9);lowInput.setItem(0,new ItemStack(ModItems.TIME_MICROCORE.get()));
            var low=new CrystalProcessingTableMenu(2,p.getInventory(),lowInput);
            h.assertTrue(!low.getSlot(1).mayPlace(new ItemStack(ModItems.EARTH_MICROCORE.get())),"Low menu filled ghost");
            machine.setItem(8,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));
            machine.setItem(9,new ItemStack(ModItems.SPELL_INK.get()));machine.setItem(10,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
            h.assertTrue(machine.start(p),"Composite ring recipe cannot start");
            for(int i=0;i<160;i++)machine.tick();
            var output=machine.getItem(11);
            h.assertTrue(new SpellPattern(ElementType.SPACE,null).equals(output.get(ModDataComponents.SPELL_PATTERN)),"Ring failed to write composite");
            h.assertTrue(machine.getItem(1).isEmpty()&&machine.getItem(0).getCount()==1,"Ghost consumed a material");
        }finally{p.discard();}
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void fullRangeWithoutGravity(GameTestHelper h){
        var p=player(h);p.setXRot(-90);
        var start=p.getEyePosition();
        CompositeSpellMotion.cast(p,new SpellPattern(ElementType.SPACE,null),payload(""),1,new SpellCasting.Modifiers(1,1));
        var shots=h.getLevel().getEntitiesOfClass(SpellEntity.class,p.getBoundingBox().inflate(4),e->e.ownedBy(p));
        for(int i=0;i<140;i++)for(var shot:shots)if(!shot.isRemoved())shot.tick();
        for(var shot:shots){
            h.assertTrue(shot.isRemoved(),"Blade did not finish");
            h.assertTrue(Math.abs(shot.getY()-start.y-128.8)<.0001,"Wrong full travel distance");
            h.assertTrue(Math.abs(shot.getX()-start.x)<.0001,"Gravity or horizontal drift applied");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void cuttingPrecedesPayloadAndBypassesExistingHurtFrames(GameTestHelper h){
        var p=player(h);
        var pig=EntityType.PIG.create(h.getLevel());pig.setNoAi(true);pig.setPos(p.getEyePosition().add(0,-.3,2));pig.setHealth(9);
        h.getLevel().addFreshEntity(pig);
        var healing=new SpellPayload(10,0,List.of(new SpellEffect(true,0,0)));
        CompositeSpellMotion.cast(p,new SpellPattern(ElementType.SPACE,null),healing,1,new SpellCasting.Modifiers(1,1));
        var shots=h.getLevel().getEntitiesOfClass(SpellEntity.class,p.getBoundingBox().inflate(4),e->e.ownedBy(p));
        var first=shots.stream().filter(e->e.compositeDelay()==0).findFirst().orElseThrow();
        for(int i=0;i<5;i++)first.tick();
        h.assertTrue(pig.getHealth()==10,"Healing happened before the true-health cut");shots.forEach(SpellEntity::discard);
        pig.setHealth(9);pig.invulnerableTime=20;
        CompositeSpellMotion.cast(p,new SpellPattern(ElementType.SPACE,null),new SpellPayload(10,0,List.of(new SpellEffect(false,1,0))),1,new SpellCasting.Modifiers(1,1));
        shots=h.getLevel().getEntitiesOfClass(SpellEntity.class,p.getBoundingBox().inflate(4),e->e.ownedBy(p));
        first=shots.stream().filter(e->e.compositeDelay()==0).findFirst().orElseThrow();
        for(int i=0;i<5;i++)first.tick();
        h.assertTrue(pig.getHealth()==1,"Old invulnerability frames prevented payload after cutting");
        h.assertTrue(pig.invulnerableTime>=20,"Temporary clearing leaked after payload");
        pig.setHealth(1);h.assertTrue(CompositeSpellMotion.percentDamage(pig,false)==2&&CompositeSpellMotion.percentDamage(pig,true)==2,"Minimum cut is not two health");
        shots.forEach(SpellEntity::discard);pig.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void bladesPierceAndRoundDamage(GameTestHelper h){
        var p=player(h);var targets=new ArrayList<LivingEntity>();
        for(int distance:new int[]{2,4}){
            var pig=EntityType.PIG.create(h.getLevel());pig.setNoAi(true);pig.setPos(p.getEyePosition().add(0,-.3,distance));pig.setHealth(9);
            h.getLevel().addFreshEntity(pig);targets.add(pig);
        }
        CompositeSpellMotion.cast(p,new SpellPattern(ElementType.SPACE,null),payload(""),1,new SpellCasting.Modifiers(1,1));
        var shots=h.getLevel().getEntitiesOfClass(SpellEntity.class,p.getBoundingBox().inflate(4),e->e.ownedBy(p));
        h.assertTrue(shots.size()==1,"Cast spawned more than one blade");
        for(int i=0;i<16;i++)shots.forEach(SpellEntity::tick);
        for(var target:targets)h.assertTrue(target.getHealth()==7,"Piercing or once-per-wave rounding failed");
        var wither=EntityType.WITHER.create(h.getLevel());wither.setHealth(100);
        h.assertTrue(CompositeSpellMotion.percentDamage(wither,false)==5&&CompositeSpellMotion.percentDamage(wither,true)==10,"Wrong boss ratios");
        var pig=targets.getFirst();pig.setHealth(9);
        h.assertTrue(CompositeSpellMotion.percentDamage(pig,true)==5,"Infusion not rounded upward");
        shots.forEach(SpellEntity::discard);targets.forEach(Entity::discard);h.succeed();
    }
}
