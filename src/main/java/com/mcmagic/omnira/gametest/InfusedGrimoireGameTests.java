package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.InfusedGrimoireItem;
import com.mcmagic.omnira.spell.IndependentSpellCooldown;
import com.mcmagic.omnira.registry.ModAttachments;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("omnira_grimoire")
@PrefixGameTestTemplate(false)
public final class InfusedGrimoireGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var profile=new GameProfile(UUID.randomUUID(),"grimoire-test");
        var player=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());
        player.connection=new FakePlayer(h.getLevel(),profile).connection;
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(5.5,2,5.5)));
        player.setYRot(-90);player.setXRot(0);
        h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(8,3,5)),Blocks.STONE.defaultBlockState());
        return player;
    }
    @GameTest(template="spell_arena") public static void experienceShortfallCostsHealth(GameTestHelper h) {
        var player=player(h);var book=(InfusedGrimoireItem)ModItems.INFUSED_GRIMOIRE.get();
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(book));
        player.giveExperiencePoints(18);
        h.assertTrue(book.use(h.getLevel(),player,InteractionHand.MAIN_HAND).getResult()!=InteractionResult.PASS,"Grimoire did not take main-hand use");
        h.assertTrue(player.totalExperience==0 && player.getHealth()==17,"Twelve missing experience must cost three health; xp="+player.totalExperience+", health="+player.getHealth());
        h.assertTrue(!IndependentSpellCooldown.ready(player,player.getMainHandItem())
                        && book.use(h.getLevel(),player,InteractionHand.MAIN_HAND).getResult()==InteractionResult.PASS,
                "Cooldown did not release the offhand");
        h.assertTrue(IndependentSpellCooldown.ready(player,new ItemStack(book)),"A second grimoire inherited the first one's cooldown");
        var projectiles=h.getLevel().getEntitiesOfClass(SpellEntity.class,new AABB(player.blockPosition()).inflate(3));
        h.assertTrue(projectiles.size()==1 && projectiles.getFirst().knowledgeBlood(),"Blood-paid projectile missing");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void sanctifiedNeedsManaAndDoesNotSpendExperience(GameTestHelper h) {
        var player=player(h);var book=(InfusedGrimoireItem)ModItems.SANCTIFIED_GRIMOIRE.get();
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(book));
        player.giveExperiencePoints(30);
        h.assertTrue(book.use(h.getLevel(),player,InteractionHand.MAIN_HAND).getResult()==InteractionResult.FAIL,
                "Sanctified spell ignored insufficient mana");
        player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(150,150));
        h.assertTrue(book.use(h.getLevel(),player,InteractionHand.MAIN_HAND).getResult()!=InteractionResult.FAIL,
                "Sanctified spell rejected paid mana");
        h.assertTrue(player.getData(ModAttachments.MANA).current()==0 && player.totalExperience==30 && player.getHealth()==20,
                "Sanctified spell consumed the wrong resource");
        h.succeed();
    }
}
