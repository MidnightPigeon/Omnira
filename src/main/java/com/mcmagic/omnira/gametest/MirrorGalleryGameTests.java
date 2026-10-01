package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_mirror_gallery")
@PrefixGameTestTemplate(false)
public final class MirrorGalleryGameTests {
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("omnira",path);}
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"gallery-test");
        var player=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        player.connection=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),profile).connection;
        return player;
    }
    private static CrystalBallBlockEntity ball(GameTestHelper h,int x) {
        var pos=h.absolutePos(new BlockPos(x,2,2));
        h.getLevel().setBlockAndUpdate(pos,ModBlocks.CRYSTAL_BALL.get().defaultBlockState());
        return (CrystalBallBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="spell_arena") public static void openingLootAwardsButOrdinaryBallDoesNot(GameTestHelper h) {
        var player=player(h);var advancement=h.getLevel().getServer().getAdvancements().get(id("beyond_the_mist"));
        h.assertTrue(advancement!=null,"Advancement missing");
        var ordinary=ball(h,2);ordinary.createMenu(1,player.getInventory(),player);
        h.assertTrue(!player.getAdvancements().getOrStartProgress(advancement).isDone(),"Ordinary storage awarded exploration advancement");
        var loot=ball(h,3);loot.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,id("chests/mirror_gallery_pool")),17);
        loot.unpackLootTable(null);
        h.assertTrue(!player.getAdvancements().getOrStartProgress(advancement).isDone(),"Automation awarded advancement");
        var saved=loot.saveWithFullMetadata(h.getLevel().registryAccess());
        loot.loadWithComponents(saved,h.getLevel().registryAccess());
        loot.createMenu(2,player.getInventory(),player);
        h.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(),"Previously generated loot lost its provenance after reload");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void spectatorCannotOpenPendingLoot(GameTestHelper h) {
        var player=player(h);player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        var loot=ball(h,2);loot.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,id("chests/mirror_gallery_gallery")),31);
        h.assertTrue(loot.createMenu(1,player.getInventory(),player)==null && loot.hasPendingLoot(),"Spectator opened pending loot");
        var advancement=h.getLevel().getServer().getAdvancements().get(id("beyond_the_mist"));
        h.assertTrue(!player.getAdvancements().getOrStartProgress(advancement).isDone(),"Spectator awarded advancement");
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        h.assertTrue(loot.createMenu(2,player.getInventory(),player)!=null,"Survival player could not open loot");
        h.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(),"First loot opening did not award");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=200) public static void lootTablesHaveCorrectRewards(GameTestHelper h) {
        var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).create(LootContextParamSets.CHEST);
        for(String kind:new String[]{"pool","gallery"}) {
            var table=h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,id("chests/mirror_gallery_"+kind)));
            int[] counts=new int[5];
            for(int seed=1;seed<=2000;seed++) {
                boolean ordinary=false;
                for(var stack:table.getRandomItems(params,net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed))) {
                    if(stack.is(kind.equals("pool")?ModItems.DREAM_CRYSTAL_SHARD.get():ModItems.PARADOX_DUST.get()))ordinary=true;
                    if(stack.is(ModItems.DREAM_SPELL_CORE.get()))counts[0]++;
                    if(stack.is(DreamContent.DREAM_CRYSTAL_BEDROCK.get().asItem()))counts[1]++;
                    if(stack.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id("arcane_crystal_grid"))))counts[2]++;
                    if(stack.is(ModItems.POCKET_MAGIC_BOTTLE.get())){counts[3]++;h.assertTrue(stack.getCount()==1,"Bottle count must be one");}
                    if(stack.is(ModItems.LIGHT_MICROCORE.get()) || stack.is(ModItems.DARK_MICROCORE.get()))counts[4]++;
                }
                h.assertTrue(ordinary,"Missing guaranteed ordinary loot");
            }
            if(kind.equals("gallery")) {
                h.assertTrue(counts[0]>50 && counts[0]<150,"Core not approximately 5 percent");
                h.assertTrue(counts[1]>140 && counts[1]<270 && counts[2]>140 && counts[2]<270,"Matrix/grid not approximately 10 percent");
                h.assertTrue(counts[3]>300 && counts[3]<500 && counts[4]==0,"Bottle or microcore loot wrong");
            } else h.assertTrue(counts[0]>10 && counts[0]<80 && counts[4]>130 && counts[4]<280,"Central pool rarity wrong");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void templateAndCrystalRecipeExist(GameTestHelper h) {
        var template=h.getLevel().getStructureManager().get(id("authored/mirror_gallery")).orElseThrow();
        h.assertTrue(template.getSize().equals(new net.minecraft.core.Vec3i(35,7,35)),"Wrong structure dimensions");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(id("dream_crystal_block")).isPresent(),"Crystal crafting missing");
        for(String material:new String[]{"engraved_mirror_rock","engraved_mirror_rock_stairs","engraved_mirror_rock_slab","spiritual_crystal_stairs"}) {
            h.assertTrue(h.getLevel().getRecipeManager().byKey(id(material)).isPresent(),"Missing crafting recipe: "+material);
            h.assertTrue(h.getLevel().getRecipeManager().byKey(id(material+"_stonecutting")).isPresent(),"Missing stonecutting recipe: "+material);
        }
        h.assertTrue(DreamContent.SPIRITUAL_CRYSTAL_STAIRS.get().defaultBlockState().getLightEmission()==10,"Crystal stairs lost luminosity");
        h.assertTrue(DreamContent.DREAM_CRYSTAL_BLOCK.get().defaultBlockState().getLightEmission()==10,"Crystal light incorrect");
        var tag=template.save(new net.minecraft.nbt.CompoundTag());int balls=0;
        for(var value:tag.getList("blocks",10))if(((net.minecraft.nbt.CompoundTag)value).getCompound("nbt").contains("LootTable"))balls++;
        h.assertTrue(balls==3,"Template requires exactly three pending loot balls");h.succeed();
    }
    @GameTest(template="spell_arena") public static void roofCornersSurviveVanillaNeighbourUpdates(GameTestHelper h) {
        var template=h.getLevel().getStructureManager().get(id("authored/mirror_gallery")).orElseThrow();
        var tag=template.save(new net.minecraft.nbt.CompoundTag());var palette=tag.getList("palette",10);
        var roof=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(var value:tag.getList("blocks",10)) {
            var block=(net.minecraft.nbt.CompoundTag)value;var pos=block.getList("pos",3);
            if(pos.getInt(1)!=5)continue;
            roof.put(new BlockPos(pos.getInt(0),0,pos.getInt(2)),net.minecraft.nbt.NbtUtils.readBlockState(h.getLevel().holderLookup(Registries.BLOCK),palette.getCompound(block.getInt("state"))));
        }
        int count=0,corners=0;var center=h.absolutePos(new BlockPos(4,3,4));
        for(var entry:roof.entrySet()) {
            var expected=entry.getValue();
            if(!(expected.getBlock() instanceof net.minecraft.world.level.block.StairBlock))continue;
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
                h.getLevel().setBlock(center.offset(dx,0,dz),roof.getOrDefault(entry.getKey().offset(dx,0,dz),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()),2);
            var actual=net.minecraft.world.level.block.Block.updateFromNeighbourShapes(expected,h.getLevel(),center);
            h.assertTrue(actual==expected,"Roof corner becomes disconnected after neighbour update at "+entry.getKey());
            if(expected.getValue(net.minecraft.world.level.block.StairBlock.SHAPE)!=net.minecraft.world.level.block.state.properties.StairsShape.STRAIGHT)corners++;
            count++;
        }
        h.assertTrue(count==186&&corners==112,"Continuous roof contour was not exported");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=160) public static void raisedPoolRetainsWater(GameTestHelper h) {
        var template=h.getLevel().getStructureManager().get(id("authored/mirror_gallery")).orElseThrow();
        var tag=template.save(new net.minecraft.nbt.CompoundTag());var palette=tag.getList("palette",10);
        for(var value:tag.getList("blocks",10)) {
            var block=(net.minecraft.nbt.CompoundTag)value;var pos=block.getList("pos",3);
            int x=pos.getInt(0),y=pos.getInt(1),z=pos.getInt(2);
            if(x<14||x>20||z<14||z>20||y>4)continue;
            var state=net.minecraft.nbt.NbtUtils.readBlockState(h.getLevel().holderLookup(Registries.BLOCK),palette.getCompound(block.getInt("state")));
            var world=h.absolutePos(new BlockPos(x-13,y+1,z-13));
            h.getLevel().setBlock(world,state,2);
        }
        for(int x=1;x<=7;x++)for(int z=1;z<=7;z++) {
            var pos=h.absolutePos(new BlockPos(x,3,z));
            if(h.getLevel().getFluidState(pos).is(net.minecraft.world.level.material.Fluids.WATER))
                h.getLevel().scheduleTick(pos,net.minecraft.world.level.material.Fluids.WATER,1);
        }
        h.runAfterDelay(100,()->{
            int sources=0;
            for(int x=0;x<=8;x++)for(int z=0;z<=8;z++)for(int y=1;y<=4;y++) {
                var pos=h.absolutePos(new BlockPos(x,y,z));var fluid=h.getLevel().getFluidState(pos);
                if(fluid.isEmpty())continue;
                h.assertTrue(y==3&&x>=3&&x<=5&&z>=3&&z<=5&&!(x==4&&z==4),"Raised pool leaked at "+pos);
                h.assertTrue(fluid.isSource(),"Pool lost a source");sources++;
            }
            h.assertTrue(sources==8,"Raised pool should retain eight water sources");h.succeed();
        });
    }
}
