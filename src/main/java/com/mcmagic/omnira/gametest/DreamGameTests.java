package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.world.dimension.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

@GameTestHolder("omnira_dream")
@PrefixGameTestTemplate(false)
public final class DreamGameTests {
    @GameTest(template="spell_arena",timeoutTicks=120)
    public static void ritualHeightAndCollapse(GameTestHelper h) {
        checkRitual(h,false);
    }
    @GameTest(template="spell_arena",timeoutTicks=120)
    public static void ritualCoreSustainsPortal(GameTestHelper h) {
        checkRitual(h,true);
    }
    private static void checkRitual(GameTestHelper h,boolean breakCore) {
        checkRitual(h,breakCore,false);
    }
    @GameTest(template="spell_arena",timeoutTicks=120)
    public static void advancedCoreSustainsPortal(GameTestHelper h) {checkRitual(h,true,true);}
    private static void checkRitual(GameTestHelper h,boolean breakCore,boolean advanced) {
        var core=(advanced?ModBlocks.ADVANCED_RITUAL_ENERGY_CORE:ModBlocks.RITUAL_ENERGY_CORE).get();
        String[] layout={"0001000","0100010","0000000","1002001","0000000","0100010","0001000"};
        var expected=new java.util.HashSet<BlockPos>();
        for(int z=0;z<7;z++) for(int x=0;x<7;x++)
            if(layout[z].charAt(x)=='1') expected.add(new BlockPos(x-3,0,z-3));
        var actual=new java.util.HashSet<BlockPos>();
        for(int[] anchor:DreamRitual.ANCHORS) actual.add(new BlockPos(anchor[0],0,anchor[1]));
        h.assertTrue(DreamRitual.ANCHORS.length==8 && actual.equals(expected),"Ritual does not match the eight-pedestal layout");
        var level=h.getLevel();
        BlockPos center=h.absolutePos(new BlockPos(6,4,6));
        for(int x=-3;x<=3;x++) for(int z=-3;z<=3;z++)
            level.setBlockAndUpdate(center.offset(x,-2,z),Blocks.STONE.defaultBlockState());
        for(int[] offset:DreamRitual.ANCHORS)
            level.setBlockAndUpdate(center.offset(offset[0],-1,offset[1]),ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState());
        level.setBlockAndUpdate(center.below(),core.defaultBlockState());
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"dream-test"));
        var hand=net.minecraft.world.InteractionHand.MAIN_HAND;
        player.setItemInHand(hand,new ItemStack(Items.RED_BED,2));
        player.setPos(center.getX()+.5,center.getY()-1,center.getZ()+2.5);
        level.setDayTime(14000);
        DreamRitual.activate(level,center.below(),player,hand);
        h.assertTrue(level.getBlockState(center).isAir() && player.getItemInHand(hand).getCount()==2,"Missing crystals consumed the offering");
        for(int[] offset:DreamRitual.ANCHORS)
            ((CrystalPedestalBlockEntity)level.getBlockEntity(center.offset(offset[0],-1,offset[1])))
                    .setItem(0,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
        level.setBlockAndUpdate(center,Blocks.STONE.defaultBlockState());
        DreamRitual.activate(level,center.below(),player,hand);
        h.assertTrue(level.getBlockState(center).is(Blocks.STONE) && player.getItemInHand(hand).getCount()==2,"Obstructed ritual consumed bed or replaced a block");
        level.removeBlock(center,false);
        DreamRitual.activate(level,center.below(),player,hand);
        h.assertTrue(level.getBlockState(center).is(ModBlocks.DREAM_PORTAL.get()),"Portal not one block above core");
        h.assertTrue(level.getBlockState(center.below()).is(core),"Core was consumed");
        h.assertTrue(player.getItemInHand(hand).getCount()==1,"Ritual did not consume exactly one bed");
        DreamRitual.activate(level,center.below(),player,hand);
        h.assertTrue(player.getItemInHand(hand).getCount()==1,"Active portal consumed another bed");
        h.assertTrue(DreamRitual.anchorsIntact(level,center),"Outer pedestals consumed");
        if(breakCore) level.removeBlock(center.below(),false);
        else ((CrystalPedestalBlockEntity)level.getBlockEntity(center.offset(0,-1,-3))).clearContent();
        h.runAfterDelay(20,()->{
            h.assertTrue(level.getBlockEntity(center) instanceof DreamPortalBlockEntity,"Portal disappeared without shrinking");
            h.assertTrue(((DreamPortalBlockEntity)level.getBlockEntity(center)).scale(0)<1,"Collapse did not start");
        });
        h.runAfterDelay(85,()->{
            h.assertTrue(level.getBlockState(center).isAir(),"Broken ritual did not close");
            h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=1200)
    public static void dreamTerrainIsPermanent(GameTestHelper h) {
        var dream=h.getLevel().getServer().getLevel(ModDimensions.DREAM_REALM);
        h.assertTrue(dream!=null,"Dream dimension failed to load");
        var generator=dream.getChunkSource().getGenerator();
        var random=dream.getChunkSource().randomState();
        BlockPos[] sample=new BlockPos[3];
        int[] coverage=new int[3];
        for(int x=-1024;x<=1024;x+=128) for(int z=-1024;z<=1024;z+=128) {
            var column=generator.getBaseColumn(x,z,dream,random);
            boolean[] found=new boolean[3];
            for(int y=0;y<256;y++) if(!column.getBlock(y).isAir()) {
                int band=y<90?0:y<145?1:2;
                found[band]=true;
                if(sample[band]==null) sample[band]=new BlockPos(x,y,z);
            }
            for(int i=0;i<3;i++) if(found[i]) coverage[i]++;
        }
        for(int i=0;i<3;i++) h.assertTrue(sample[i]!=null,"Missing terrain band "+i);
        h.assertTrue(coverage[1]<coverage[0] && coverage[1]<coverage[2],"Mirror islands are not rarer than main islands");
        for(int i=0;i<3;i++) {
            var p=sample[i];dream.getChunkAt(p);
            var expected=(i==0?DreamContent.SHADOW_ROCK:i==1?DreamContent.MIRROR_ROCK:DreamContent.LIGHT_CONDENSATE).get();
            h.assertTrue(dream.getBlockState(p).is(expected),"Incorrect terrain material for band "+i+": "+dream.getBlockState(p));
            h.assertTrue(dream.getBlockEntity(p)==null,"Terrain has block entities");
        }
        com.mcmagic.omnira.Omnira.LOGGER.info("Dream sampled columns: shadow={}, mirror={}, light={}",coverage[0],coverage[1],coverage[2]);
        h.succeed();
    }
}
