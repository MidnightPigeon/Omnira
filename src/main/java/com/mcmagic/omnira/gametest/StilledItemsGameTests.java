package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.time.StilledTime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_stilled_items")
@PrefixGameTestTemplate(false)
public final class StilledItemsGameTests {
    @GameTest(template="spell_arena", timeoutTicks=80)
    public static void slowMovementPickupAndExpiry(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(4,8,4));
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(StilledTime.BIOME);
        // Cover neighbouring chunks too, so the test remains independent of template placement.
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
            level.getChunkAt(p.offset(x*16,0,z*16)).fillBiomesFromNoise((a,b,c,s)->biome,level.getChunkSource().randomState().sampler());
        verifySpellRecovery(h, p);
        var item=new ItemEntity(level,p.getX()+.5,p.getY(),p.getZ()+.5,new ItemStack(Items.GOLD_INGOT));
        item.setNoGravity(true);item.setDeltaMovement(.2,0,0);item.setDefaultPickUpDelay();level.addFreshEntity(item);
        var shot=EntityType.SNOWBALL.create(level);shot.moveTo(p.getCenter().add(0,2,0));
        shot.setNoGravity(true);shot.setDeltaMovement(.2,0,0);level.addFreshEntity(shot);
        double start=shot.getX();int ticks=shot.tickCount;
        var magic=com.mcmagic.omnira.spell.entity.SpellEntity.spawn(level,null,
                com.mcmagic.omnira.spell.entity.SpellEntity.Kind.PROJECTILE,p.getCenter().add(0,6,0));
        magic.setDeltaMovement(.2,0,0);double magicStart=magic.getX();
        h.assertTrue(StilledTime.stopped(new net.minecraft.world.entity.ExperienceOrb(level,p.getX(),p.getY(),p.getZ(),1)),"Other non-living entities must retain stasis");
        var expiring=new ItemEntity(level,p.getX()+.5,p.getY()+4,p.getZ()+.5,new ItemStack(Items.STICK));
        var tag=new net.minecraft.nbt.CompoundTag();expiring.saveWithoutId(tag);tag.putShort("Age",(short)5999);
        expiring.load(tag);expiring.setNoGravity(true);expiring.setDeltaMovement(Vec3.ZERO);level.addFreshEntity(expiring);
        h.runAfterDelay(12,()->{
            h.assertTrue(!item.hasPickUpDelay(),"Stasis froze the normal pickup cooldown");
            var player=h.makeMockPlayer(GameType.SURVIVAL);player.moveTo(item.position());item.playerTouch(player);
            h.assertTrue(item.isRemoved()&&player.getInventory().contains(new ItemStack(Items.GOLD_INGOT)),"Slow item cannot be collected");
        });
        h.runAfterDelay(40,()->{
            h.assertTrue(shot.tickCount-ticks==4,"Projectile must advance exactly 10% of world ticks");
            h.assertTrue(shot.getX()>start&&shot.getX()<start+1,"Projectile movement must continue at 10%");
            h.assertTrue(magic.tickCount==4&&magic.getX()>magicStart&&magic.getX()<magicStart+1,"Custom spell projectile remained frozen or moved at full speed");
            h.assertTrue(expiring.isRemoved(),"Slow item must still expire");
            // Leaving the biome must restore normal ticking without resetting its saved velocity.
            var plains=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
                level.getChunkAt(p.offset(x*16,0,z*16)).fillBiomesFromNoise((a,b,c,s)->plains,level.getChunkSource().randomState().sampler());
            int before=shot.tickCount;
            h.runAfterDelay(4,()->{h.assertTrue(shot.tickCount-before==4,"Projectile did not resume outside biome");h.succeed();});
        });
    }
    private static void verifySpellRecovery(GameTestHelper h, BlockPos pos) {
        var level = h.getLevel();
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "spell-clock");
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile,
                net.minecraft.server.level.ClientInformation.createDefault());
        player.connection = new net.neoforged.neoforge.common.util.FakePlayer(level, profile).connection;
        player.setPos(pos.getCenter());
        player.setNoGravity(true);
        var staff = com.mcmagic.omnira.registry.ModItems.MODULAR_STAFF.get();
        var crystal = com.mcmagic.omnira.registry.ModItems.LOW_TIER_MAGIC_CRYSTAL.get();
        var weapon = new ItemStack(Items.IRON_SWORD);
        var cooldowns = player.getCooldowns();
        cooldowns.addCooldown(staff, 80);
        cooldowns.addCooldown(crystal, 20);
        cooldowns.addCooldown(Items.STICK, 20);
        com.mcmagic.omnira.spell.IndependentSpellCooldown.start(player, weapon, 20);
        try {
            for (int i=1; i<=40; i++) {
                player.tickCount++;
                player.doTick();
                h.assertTrue(!com.mcmagic.omnira.spell.IndependentSpellCooldown.ready(player, weapon) == (i<40),
                        "Independent spell did not recover in 40 ticks");
            }
            h.assertTrue(cooldowns.getCooldownPercent(staff, 0)==.75f, "Chain cooldown did not run at half speed");
            h.assertTrue(!cooldowns.isOnCooldown(crystal) && !cooldowns.isOnCooldown(Items.STICK), "Item cooldown failed to expire");
        } finally {
            player.discard();
        }
    }

}
