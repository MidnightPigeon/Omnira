package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.staff.StaffAssembly;
import com.mcmagic.omnira.item.staff.StaffPart;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;

@GameTestHolder("omnira_wide_area")
@PrefixGameTestTemplate(false)
public final class WideAreaRuneGameTests {
    private static StaffAssembly wide() {
        return StaffAssembly.basic().withUpgrades(List.of(new ItemStack(ModItems.WIDE_AREA_RUNE.get()))).orElseThrow();
    }

    @GameTest(template="spell_arena")
    public static void groundRingsAndOrbit(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new Vec3(8.5,2,8.5)));
        player.setItemInHand(InteractionHand.MAIN_HAND,wide().create());
        for(int x=2;x<=14;x++) for(int z=2;z<=14;z++) h.setBlock(x,1,z,Blocks.STONE);
        var points=SpellCasting.groundPoints(h.getLevel(),player.position(),3);
        h.assertTrue(points.size()==20 && points.stream().map(SpellCasting.GroundPoint::block).distinct().count()==20,"Outer ring duplicated or lost points");
        for(var shape:List.of(ElementType.EARTH,ElementType.WATER,ElementType.FIRE,ElementType.AIR)) {
            h.assertTrue(SpellCasting.cast(player,new SpellPattern(ElementType.EARTH,shape),SpellPayload.EMPTY,1,new SpellCasting.Modifiers(1,1.5)),"Ground cast failed");
            var spells=h.getLevel().getEntitiesOfClass(SpellEntity.class,player.getBoundingBox().inflate(12),e->e.ownedBy(player));
            h.assertTrue(spells.size()==(shape==ElementType.WATER?6:20),"Wrong ground coverage for "+shape+": "+spells.size());
            spells.forEach(SpellEntity::discard);
        }
        SpellCasting.cast(player,new SpellPattern(ElementType.WATER,ElementType.WATER),SpellPayload.EMPTY,1,new SpellCasting.Modifiers(1,1.5));
        var orbs=h.getLevel().getEntitiesOfClass(SpellEntity.class,player.getBoundingBox().inflate(3),e->e.ownedBy(player));
        h.assertTrue(orbs.size()==6,"Wide orbit must have six projectiles");
        for(var orb:orbs) {
            var delta=orb.position().subtract(player.getBoundingBox().getCenter());
            h.assertTrue(Math.abs(Math.hypot(delta.x,delta.z)-1.8)<1e-6,"Orbit radius incorrect");
            orb.discard();
        }
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void radiusAndIndependentRuneBonuses(GameTestHelper h) {
        var upgraded=wide();
        h.assertTrue(upgraded.usedSlots(StaffPart.SlotKind.RUNE)==1 && upgraded.stats().rangeMultiplier()==1.5,"Wrong range stats");
        h.assertTrue(upgraded.stats().power()==0 && upgraded.stats().cooldownTicks()==60,"Range rune changed damage or cooldown");
        var both=new StaffAssembly(upgraded.shaft(),upgraded.reinforcement(),new ItemStack(ModItems.MULTI_ARCANE_TIP.get()),
                List.of(new ItemStack(ModItems.WIDE_AREA_RUNE.get()),new ItemStack(ModItems.SWIFTNESS_RUNE.get())));
        h.assertTrue(both.valid() && both.stats().rangeMultiplier()==1.5 && both.stats().speedMultiplier()==1.5
                && both.stats().cooldownTicks()==60 && both.stats().cooldownReduction()==.2,"Runes cannot coexist");
        h.assertTrue(both.withUpgrades(List.of(new ItemStack(ModItems.WIDE_AREA_RUNE.get()))).isEmpty(),"Duplicate rune allowed");
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new Vec3(8,3,8)));
        player.setItemInHand(InteractionHand.MAIN_HAND,upgraded.create());
        var near=h.spawn(EntityType.COW,12,3,8);near.setNoAi(true);near.setDeltaMovement(Vec3.ZERO);
        var far=h.spawn(EntityType.COW,13,3,8);far.setNoAi(true);far.setDeltaMovement(Vec3.ZERO);
        near.setPos(player.position().add(4,0,0));
        far.setPos(player.position().add(5,0,0));
        SpellCasting.cast(player,new SpellPattern(ElementType.WATER,ElementType.FIRE),SpellPayload.EMPTY,1,new SpellCasting.Modifiers(1,1.5));
        h.assertTrue(near.getDeltaMovement().lengthSqr()>0 && far.getDeltaMovement().lengthSqr()==0,"Explosion radius not 4.5");
        h.assertTrue(near.getHealth()==near.getMaxHealth(),"Range rune introduced damage");
        h.succeed();
    }
}
