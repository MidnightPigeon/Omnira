package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spell.ElementType;
import com.mcmagic.omnira.spell.SpellCasting;
import com.mcmagic.omnira.spell.SpellPattern;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("omnira_spell_shapes")
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private static Player caster(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(8.5, 2, 8.5)));
        player.setYRot(0);
        player.setXRot(0);
        helper.getLevel().addFreshEntity(player);
        for (int x = 4; x <= 12; x++) for (int z = 4; z <= 12; z++) helper.setBlock(x, 1, z, Blocks.STONE);
        return player;
    }

    private static java.util.List<SpellEntity> spells(GameTestHelper helper, Player player) {
        return helper.getLevel().getEntitiesOfClass(SpellEntity.class, new AABB(player.position(), player.position()).inflate(30), e -> e.ownedBy(player));
    }

    @GameTest(template = "spell_arena")
    public static void allSixteen(GameTestHelper helper) {
        Player player = caster(helper);
        helper.setBlock(8, 3, 13, Blocks.STONE);
        ElementType[] elements = {ElementType.EARTH, ElementType.WATER, ElementType.FIRE, ElementType.AIR};
        for (ElementType target : elements) for (ElementType shape : elements) {
            helper.assertTrue(SpellCasting.cast(player, new SpellPattern(target, shape)), "Cast rejected: " + target + "/" + shape);
            var entities = spells(helper, player);
            int expected = shape == ElementType.WATER ? 4 : target == ElementType.EARTH ? 8
                    : target == ElementType.FIRE && shape == ElementType.EARTH ? 6 : 1;
            helper.assertTrue(entities.size() == expected, "Wrong count for " + target + "/" + shape + ": " + entities.size());
            for (SpellEntity entity : entities) entity.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "spell_arena")
    public static void invalidTargetsAndProjection(GameTestHelper helper) {
        Player player = caster(helper);
        player.setXRot(-90);
        helper.assertTrue(!SpellCasting.cast(player, new SpellPattern(ElementType.FIRE, ElementType.AIR)), "Target must not cast into air");
        helper.assertTrue(SpellCasting.groundPoints(helper.getLevel(), player.position()).size() == 8, "Flat ground must resolve eight columns");
        helper.setBlock(8, 2, 6, Blocks.STONE);
        helper.setBlock(8, 3, 6, Blocks.STONE);
        var raised = SpellCasting.groundPoints(helper.getLevel(), player.position());
        helper.assertTrue(raised.stream().anyMatch(p -> Math.abs(p.surface().y - player.getY() - 2) < .01), "Occupied column must search upward");
        helper.assertTrue(SpellCasting.groundPoints(helper.getLevel(), player.position().add(0, 12, 0)).size() == 8, "Twelve-block projection is inclusive");
        helper.assertTrue(SpellCasting.groundPoints(helper.getLevel(), player.position().add(0, 13, 0)).size() == 1, "Out-of-range columns must fail independently");
        helper.succeed();
    }

    @GameTest(template = "spell_arena")
    public static void unshapedGroundTargetAndAim(GameTestHelper helper) {
        Player player=caster(helper);
        player.setXRot(8);
        var harm=new com.mcmagic.omnira.spell.SpellPayload(30,0,
                java.util.List.of(new com.mcmagic.omnira.spell.SpellEffect(false,1,0,false)));
        var near=helper.spawn(EntityType.COW,8.5F,2,10.5F);
        near.setNoAi(true);
        float health=near.getHealth();
        helper.assertTrue(SpellCasting.cast(player,new SpellPattern(ElementType.EARTH,null),harm,1),"Direct ground rejected");
        helper.assertTrue(near.getHealth()<health,"Direct ground missed the creature above a ground point");
        near.setHealth(health);
        near.invulnerableTime=0;
        helper.assertTrue(SpellCasting.cast(player,new SpellPattern(ElementType.FIRE,null),harm,1),"Direct target rejected");
        helper.assertTrue(near.getHealth()<health,"Direct target missed the aimed creature");
        near.setHealth(health);
        near.invulnerableTime=0;
        helper.assertTrue(SpellCasting.cast(player,new SpellPattern(ElementType.AIR,null),harm,1),"Direct aim rejected");
        helper.assertTrue(near.getHealth()<health,"Direct aim missed its only valid creature");
        helper.succeed();
    }

    @GameTest(template = "spell_arena")
    public static void harmlessBurst(GameTestHelper helper) {
        Player player = caster(helper);
        var target = helper.spawn(EntityType.ZOMBIE, 9.5F, 2, 8.5F);
        target.setNoAi(true);
        float health = target.getHealth();
        player.setDeltaMovement(Vec3.ZERO);
        SpellCasting.burst(helper.getLevel(), player, player.getBoundingBox().getCenter(), true);
        helper.assertTrue(target.getHealth() == health, "Burst dealt damage");
        helper.assertTrue(target.getDeltaMovement().lengthSqr() > 0, "Burst did not push nearby entity");
        helper.assertTrue(player.getDeltaMovement().lengthSqr() > 0, "Self burst incorrectly exempted caster");
        helper.assertBlockPresent(Blocks.STONE, 8, 1, 8);
        SpellCasting.burst(helper.getLevel(), player, player.getBoundingBox().getCenter(), false);
        helper.assertTrue(player.getDeltaMovement().lengthSqr() > 0, "Other bursts incorrectly exempted caster");
        helper.succeed();
    }

    @GameTest(template = "spell_arena", timeoutTicks = 430)
    public static void barrierCollisionTriggerAndExpiry(GameTestHelper helper) {
        Player player = caster(helper);
        SpellEntity barrier = SpellEntity.spawn(helper.getLevel(), player, SpellEntity.Kind.BARRIER, player.position().add(2,0,0));
        var target = helper.spawn(EntityType.ZOMBIE, 12, 2, 8.5F);
        target.setNoAi(true);
        target.move(MoverType.SELF, new Vec3(-3,0,0));
        helper.assertTrue(target.getX() > barrier.getX(), "Barrier did not physically block movement");
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(barrier.charges() == 0, "Barrier contact did not trigger");
            helper.assertTrue(!barrier.isRemoved() && barrier.canBeCollidedWith(), "Contact consumed physical barrier");
        });
        helper.runAtTickTime(405, () -> {
            helper.assertTrue(barrier.isRemoved(), "Barrier outlived twenty seconds");
            helper.succeed();
        });
    }

    @GameTest(template = "spell_arena", timeoutTicks = 70)
    public static void flyingGroundFallsWithoutTerrainLoss(GameTestHelper helper) {
        Player player = caster(helper);
        SpellCasting.cast(player, new SpellPattern(ElementType.EARTH, ElementType.AIR));
        var flying = spells(helper, player);
        helper.runAtTickTime(8, () -> helper.assertTrue(flying.stream().allMatch(e -> e.getY() > player.getY() + 1), "Ground blocks did not rise"));
        helper.runAtTickTime(60, () -> {
            helper.assertTrue(flying.stream().allMatch(SpellEntity::isRemoved), "Ground blocks did not land and consume");
            helper.assertBlockPresent(Blocks.STONE, 8, 1, 6);
            helper.succeed();
        });
    }

    @GameTest(template = "spell_arena", timeoutTicks = 60)
    public static void homingAndStraightProjectile(GameTestHelper helper) {
        Player player = caster(helper);
        var target = helper.spawn(EntityType.HUSK, 8.5F, 2, 13.5F);
        target.setNoAi(true);
        target.setNoGravity(true);
        helper.assertTrue(SpellCasting.cast(player, new SpellPattern(ElementType.FIRE, ElementType.AIR)), "Homing cast failed");
        SpellEntity homing = spells(helper, player).getFirst();
        target.setPos(target.position().add(2,0,0));
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(homing.getX() > player.getX(), "Projectile did not track moving target");
            helper.assertTrue(Math.abs(homing.getDeltaMovement().length() - SpellCasting.PROJECTILE_SPEED) < .00001, "Wrong projectile speed");
        });
        helper.runAtTickTime(50, () -> {
            helper.assertTrue(homing.isRemoved(), "Homing projectile did not collide with target");
            helper.assertTrue(target.getHealth() == target.getMaxHealth(), "Projectile dealt damage");
            helper.succeed();
        });
    }

    @GameTest(template = "spell_arena", timeoutTicks = 30)
    public static void wardConsumesFourDamageTriggers(GameTestHelper helper) {
        Player player = caster(helper);
        var target = helper.spawn(EntityType.ZOMBIE, 8.5F, 2, 11.5F);
        target.setNoAi(true);
        SpellEntity ward = SpellEntity.spawn(helper.getLevel(), player, SpellEntity.Kind.WARD, target.getBoundingBox().getCenter());
        ward.follow(target);
        for (int i = 1; i <= 4; i++) {
            final int remaining = 4 - i;
            helper.runAtTickTime(i * 3, () -> {
                target.invulnerableTime = 0;
                target.hurt(helper.getLevel().damageSources().generic(), 1);
                helper.assertTrue(ward.charges() == remaining, "Ward must consume exactly one charge per damage event");
                if (remaining == 0) { helper.assertTrue(ward.isRemoved(), "Empty ward survived"); helper.succeed(); }
            });
        }
    }

    @GameTest(template = "spell_arena", timeoutTicks = 30)
    public static void selfFlightAndOrbitContact(GameTestHelper helper) {
        Player player = caster(helper);
        helper.assertTrue(SpellCasting.cast(player, new SpellPattern(ElementType.WATER, ElementType.AIR)), "Self launch failed");
        SpellEntity flight = (SpellEntity) player.getVehicle();
        helper.assertTrue(flight != null && !flight.shouldRiderSit(), "Self launch must carry the upright caster");
        var victim = helper.spawn(EntityType.ZOMBIE, 10, 2, 11);
        victim.setNoAi(true);
        SpellEntity orb = SpellEntity.spawn(helper.getLevel(), player, SpellEntity.Kind.ORB, victim.getBoundingBox().getCenter());
        orb.orbit(victim.getBoundingBox().getCenter().add(-1.2, 0, 0), 0, java.util.List.of());
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(flight.getZ() > helper.absoluteVec(new Vec3(8.5,2,8.5)).z + .3, "Self launch did not move");
            helper.assertTrue(Math.abs(player.getY() - flight.getY()) < .01, "Caster does not align with flight collision box");
            helper.assertTrue(orb.isRemoved(), "Contact did not consume orbit ball");
            player.stopRiding();
        });
        helper.runAtTickTime(8, () -> {
            helper.assertTrue(flight.isRemoved(), "Dismount left active flight entity");
            helper.succeed();
        });
    }

    @GameTest(template = "spell_arena", timeoutTicks = 60)
    public static void barrierBreaksAfterEightHits(GameTestHelper helper) {
        Player player = caster(helper);
        SpellEntity barrier = SpellEntity.spawn(helper.getLevel(),player,SpellEntity.Kind.BARRIER,player.position().add(0,0,2));
        helper.assertTrue(!barrier.hurt(helper.getLevel().damageSources().generic(),10),"Environmental damage counted");
        for(int i=0;i<8;i++) {
            final int hits=i+1;
            helper.runAtTickTime(1+i*6,() -> {
                helper.assertTrue(barrier.hurt(helper.getLevel().damageSources().playerAttack(player),1),"Attack rejected");
                helper.assertTrue(barrier.isRemoved()==(hits==8),"Incorrect barrier lifetime");
                if(hits<8) helper.assertTrue(!barrier.hurt(helper.getLevel().damageSources().playerAttack(player),100),"Duplicate hit counted");
            });
        }
        helper.runAtTickTime(45,helper::succeed);
    }

    @GameTest(template = "spell_arena", timeoutTicks = 430, skyAccess = true)
    public static void straightProjectileLifetime(GameTestHelper helper) {
        Player player = caster(helper);
        player.setXRot(-90);
        SpellCasting.cast(player, new SpellPattern(ElementType.AIR, ElementType.AIR));
        SpellEntity projectile = spells(helper, player).getFirst();
        Vec3 start = projectile.position();
        helper.runAtTickTime(40, () -> {
            helper.assertTrue(Math.abs(projectile.getX() - start.x) < .01 && Math.abs(projectile.getZ() - start.z) < .01, "Straight projectile drifted sideways");
            helper.assertTrue(Math.abs(projectile.getY() - start.y - 40 * SpellCasting.PROJECTILE_SPEED) < .31, "Straight projectile speed must be six blocks per second");
        });
        helper.runAtTickTime(395, () -> helper.assertTrue(!projectile.isRemoved(), "Projectile expired before twenty seconds"));
        helper.runAtTickTime(405, () -> {
            helper.assertTrue(projectile.isRemoved(), "Projectile did not expire");
            helper.succeed();
        });
    }

    @GameTest(template = "spell_arena")
    public static void crystalConsumption(GameTestHelper helper) {
        Player player = caster(helper);
        player.setXRot(-90);
        var item = com.mcmagic.omnira.registry.ModItems.LOW_TIER_MAGIC_CRYSTAL.get();
        var stack = new net.minecraft.world.item.ItemStack(item);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
        item.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(stack.getCount() == 1, "Unwritten crystal was consumed");
        stack.set(com.mcmagic.omnira.registry.ModDataComponents.SPELL_PATTERN,
                new SpellPattern(ElementType.FIRE, ElementType.AIR));
        item.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(stack.getCount() == 1, "Invalid target consumed crystal");
        stack.set(com.mcmagic.omnira.registry.ModDataComponents.SPELL_PATTERN,
                new SpellPattern(ElementType.AIR, ElementType.AIR));
        item.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(stack.isEmpty(), "Valid cast did not consume crystal");
        helper.succeed();
    }

    @GameTest(template = "spell_arena")
    public static void groundShapesAndIndependentColumns(GameTestHelper helper) {
        Player player = caster(helper);
        helper.setBlock(8, 1, 6, Blocks.STONE_SLAB);
        helper.setBlock(9, 2, 7, Blocks.SHORT_GRASS);
        var points = SpellCasting.groundPoints(helper.getLevel(), player.position());
        helper.assertTrue(points.size() == 8, "Plants/slabs removed valid columns");
        var slab = points.stream().filter(p -> p.block().equals(helper.absolutePos(new BlockPos(8, 1, 6)))).findFirst().orElseThrow();
        helper.assertTrue(Math.abs(slab.surface().y - (player.getY() - .5)) < .001, "Slab surface must use collision height");
        helper.assertTrue(points.stream().noneMatch(p -> p.block().equals(helper.absolutePos(new BlockPos(9, 2, 7)))), "Plant selected as solid ground");
        SpellCasting.cast(player, new SpellPattern(ElementType.EARTH, ElementType.AIR));
        for (SpellEntity spell : spells(helper, player)) {
            Vec3 start = spell.position();
            spell.tick();
            helper.assertTrue(!spell.isRemoved() && spell.getY() > start.y, "Ground projectile failed first upward tick");
            helper.assertTrue(spell.getX() == start.x && spell.getZ() == start.z, "Ground projectile drifted sideways");
            spell.discard();
        }
        helper.assertBlockPresent(Blocks.STONE_SLAB, 8, 1, 6);
        helper.succeed();
    }

    @GameTest(template = "spell_arena")
    public static void flightOnlyHitsActualSweptPath(GameTestHelper helper) {
        Player player = caster(helper);
        var level = helper.getLevel();
        var diagonalVictim = helper.spawn(EntityType.COW, 4, 6, 7);
        diagonalVictim.setNoAi(true);
        float health = diagonalVictim.getHealth();
        var diagonal = SpellEntity.spawn(level, player, SpellEntity.Kind.FLYING_BLOCK,
                helper.absoluteVec(new Vec3(4, 6, 4)));
        diagonal.configure(1, 6);
        diagonal.setDeltaMovement(3, 0, 3);
        diagonal.tick();
        helper.assertTrue(!diagonal.isRemoved() && diagonalVictim.getHealth() == health,
                "Broad-phase corner falsely counted as impact");
        diagonal.discard();
        var behindWall = helper.spawn(EntityType.COW, 8, 6, 9);
        behindWall.setNoAi(true);
        for (int x = 7; x <= 9; x++) for (int y = 5; y <= 8; y++) helper.setBlock(x, y, 7, Blocks.STONE);
        var blocked = SpellEntity.spawn(level, player, SpellEntity.Kind.FLYING_BLOCK,
                helper.absoluteVec(new Vec3(8, 6, 5)));
        blocked.configure(1, 6);
        blocked.setDeltaMovement(0, 0, 5);
        health = behindWall.getHealth();
        blocked.tick();
        helper.assertTrue(blocked.isRemoved() && behindWall.getHealth() == health, "Flight damaged entity behind wall");
        var onPath = helper.spawn(EntityType.COW, 12, 6, 6);
        onPath.setNoAi(true);
        var impact = SpellEntity.spawn(level, player, SpellEntity.Kind.FLYING_BLOCK,
                helper.absoluteVec(new Vec3(12, 6, 4)));
        impact.configure(1, 6);
        impact.setDeltaMovement(0, 0, 3);
        health = onPath.getHealth();
        impact.tick();
        helper.assertTrue(impact.isRemoved() && onPath.getHealth() == health - 6, "Real swept collision missed");
        helper.succeed();
    }
}
