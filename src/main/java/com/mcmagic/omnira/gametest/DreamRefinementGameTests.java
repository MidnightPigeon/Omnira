package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.DreamCrystalMatrixBlock;
import com.mcmagic.omnira.item.bottle.*;
import com.mcmagic.omnira.registry.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("omnira_dream_refinement")
@PrefixGameTestTemplate(false)
public final class DreamRefinementGameTests {
    @GameTest(template="spell_arena")
    public static void crystalHasFourGrowthStages(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,4,5));
        level.setBlockAndUpdate(pos,DreamContent.DREAM_CRYSTAL_BEDROCK.get().defaultBlockState());
        Block[] stages={DreamContent.SMALL_DREAM_BUD.get(),DreamContent.MEDIUM_DREAM_BUD.get(),DreamContent.LARGE_DREAM_BUD.get(),DreamContent.DREAM_CRYSTAL.get()};
        for(var face:Direction.values()) {
            level.setBlockAndUpdate(pos.relative(face),Blocks.WATER.defaultBlockState());
            for(var stage:stages) {
                h.assertTrue(DreamCrystalMatrixBlock.grow(level,pos,face),"Growth did not advance");
                var state=level.getBlockState(pos.relative(face));
                h.assertTrue(state.is(stage) && state.getValue(AmethystClusterBlock.FACING)==face,"Incorrect stage or facing");
                h.assertTrue(state.getValue(AmethystClusterBlock.WATERLOGGED),"Growth lost waterlogging");
            }
            h.assertTrue(!DreamCrystalMatrixBlock.grow(level,pos,face),"Mature cluster grew again");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void bottleRestoresEntityOnce(GameTestHelper h) {
        var level=h.getLevel();
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"bottle-test"));
        var cow=EntityType.COW.create(level);cow.setPos(h.absoluteVec(new Vec3(4,3,4)));
        cow.setCustomName(Component.literal("Bottled Cow"));cow.setHealth(6);
        level.addFreshEntity(cow);var uuid=cow.getUUID();
        var stack=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
        h.assertTrue(PocketBottleItem.capture(player,cow,stack),"Capture failed");
        h.assertTrue(cow.isRemoved() && PocketBottleItem.filled(stack),"Capture did not transfer entity");
        var id=stack.get(ModDataComponents.BOTTLE_CAPTURE);
        h.assertTrue(!PocketBottleItem.release(level,stack,new Vec3(0,-1000,0)),"Invalid release succeeded");
        h.assertTrue(BottleStorage.get(level).get(id)!=null,"Failed release lost saved entity");
        h.assertTrue(PocketBottleItem.release(level,stack,h.absoluteVec(new Vec3(6,3,6))),"Release failed");
        var restored=level.getEntity(uuid);
        h.assertTrue(restored instanceof LivingEntity living && living.getHealth()==6,"Health or UUID lost");
        h.assertTrue(restored.getName().getString().equals("Bottled Cow"),"Custom name lost");
        h.assertTrue(!PocketBottleItem.release(level,stack,h.absoluteVec(new Vec3(8,3,8))),"Duplicate release succeeded");
        h.assertTrue(!PocketBottleItem.capture(player,player,new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get())),"Player capture accepted");
        h.succeed();
    }
}
