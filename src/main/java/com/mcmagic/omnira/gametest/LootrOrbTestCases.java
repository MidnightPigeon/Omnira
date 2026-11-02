package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.compat.LootrOrbs;
import com.mcmagic.omnira.menu.CrystalBallMenu;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;


final class LootrOrbTestCases {
    public static void preparedCrystalPersists(GameTestHelper h) {
        var key=ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse("omnira:chests/apprentice_hut"));
        var table=h.getLevel().getServer().reloadableRegistries().getLootTable(key);
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                        h.absoluteVec(new net.minecraft.world.phys.Vec3(4,2,4)))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        long seed=1;
        while(seed<1000 && table.getRandomItems(params,seed).stream().noneMatch(s->s.is(com.mcmagic.omnira.registry.ModItems.LOW_TIER_MAGIC_CRYSTAL))) seed++;
        h.assertTrue(seed<1000,"No prepared crystal seed found");
        var source=orb(h,false);source.setLootTable(key,seed);
        var a=player(h,"spell-a");var b=player(h,"spell-b");
        h.assertTrue(LootrOrbs.manage(source),"Treasure was not managed");
        var first=(CrystalBallMenu)LootrOrbs.menu(1,a.getInventory(),a,source);
        int slot=-1;
        for(int i=0;i<first.ball.getContainerSize();i++) if(first.ball.getItem(i).is(com.mcmagic.omnira.registry.ModItems.LOW_TIER_MAGIC_CRYSTAL)) {slot=i;break;}
        h.assertTrue(slot>=0,"Lootr personal generation bypassed crystal modifier");
        first.ball.removeItem(slot,1);first.removed(a);
        var reopened=(CrystalBallMenu)LootrOrbs.menu(2,a.getInventory(),a,source);
        h.assertTrue(reopened.ball.displayItems().stream().noneMatch(s->s.is(com.mcmagic.omnira.registry.ModItems.LOW_TIER_MAGIC_CRYSTAL)),"Reopening rerolled personal loot");
        reopened.removed(a);
        var other=(CrystalBallMenu)LootrOrbs.menu(3,b.getInventory(),b,source);
        h.assertTrue(other.ball.displayItems().stream().anyMatch(s->s.is(com.mcmagic.omnira.registry.ModItems.LOW_TIER_MAGIC_CRYSTAL)),"One player removed another's prepared crystal");
        other.removed(b);h.succeed();
    }

    private static ServerPlayer player(GameTestHelper h,String name){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),name);
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),profile).connection;
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);return p;
    }
    private static CrystalBallBlockEntity orb(GameTestHelper h,boolean liquid){
        var pos=h.absolutePos(new BlockPos(4,2,4));
        h.getLevel().setBlockAndUpdate(pos,(liquid?ModBlocks.LIQUID_CRYSTAL_BALL:ModBlocks.CRYSTAL_BALL).get().defaultBlockState());
        var orb=(CrystalBallBlockEntity)h.getLevel().getBlockEntity(pos);
        if(liquid)((LiquidCrystalBallBlockEntity)orb).markFluidTreasure();
        else orb.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse("lootr_test:chests/foreign_structure")),42);
        return orb;
    }
    public static void personalItemsPersistAndStayIsolated(GameTestHelper h){
        var source=orb(h,false);var a=player(h,"orb-a");var b=player(h,"orb-b");
        h.assertTrue(LootrOrbs.manage(source),"Lootr did not manage treasure");
        h.assertTrue(!LootrOrbs.opened(source,a)&&!LootrOrbs.opened(source,b),"Appearance lookup generated loot");
        var first=(CrystalBallMenu)LootrOrbs.menu(1,a.getInventory(),a,source);
        h.assertTrue(LootrOrbs.opened(source,a)&&!LootrOrbs.opened(source,b),"Personal appearance leaked across players");
        h.assertTrue(first!=null && first.ball.displayItems().stream().anyMatch(stack->stack.is(Items.DIAMOND)),"Personal loot not generated");
        a.getInventory().setItem(9,new ItemStack(Items.STONE,7));first.quickMoveStack(a,first.playerStart);
        h.assertTrue(first.ball.displayItems().stream().anyMatch(stack->stack.is(Items.STONE)&&stack.getCount()==7),"Personal shift insertion blocked");
        var upgrade=new ItemStack(com.mcmagic.omnira.registry.ModItems.BASIC_STACKING_UPGRADE.get());
        a.getInventory().setItem(9,upgrade);first.quickMoveStack(a,first.playerStart);
        h.assertTrue(a.getInventory().getItem(9).getCount()==1&&first.ball.upgrades.getStackInSlot(0).isEmpty(),"Upgrade was installed or deleted");
        first.ball.clearContent();first.ball.setChanged();first.removed(a);
        h.assertTrue(LootrOrbs.opened(source,a),"Closing or emptying reset opened appearance");
        var other=(CrystalBallMenu)LootrOrbs.menu(2,b.getInventory(),b,source);
        h.assertTrue(other.ball.displayItems().stream().anyMatch(stack->stack.is(Items.DIAMOND)),"Player A depleted player B");other.removed(b);
        var again=(CrystalBallMenu)LootrOrbs.menu(3,a.getInventory(),a,source);
        h.assertTrue(again.ball.isEmpty(),"Reopening regenerated player A loot");
        h.assertTrue(source.hasPendingLoot() && source.getItem(0).isEmpty(),"World inventory exposed private loot");again.removed(a);h.succeed();
    }
    public static void personalFluidsPersistAndStayIsolated(GameTestHelper h){
        var source=orb(h,true);var a=player(h,"fluid-a");var b=player(h,"fluid-b");
        h.assertTrue(LootrOrbs.manage(source),"Fluid treasure not managed");
        var first=(CrystalBallMenu)LootrOrbs.menu(1,a.getInventory(),a,source);
        var fluid=(LiquidCrystalBallBlockEntity)first.ball;
        h.assertTrue(fluid.tank.getFluidAmount()==24000,"Personal fluid not rolled");
        fluid.tank.drain(1000,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);first.removed(a);
        var other=(CrystalBallMenu)LootrOrbs.menu(2,b.getInventory(),b,source);
        h.assertTrue(((LiquidCrystalBallBlockEntity)other.ball).tank.getFluidAmount()==24000,"Player B fluid shared");other.removed(b);
        var again=(CrystalBallMenu)LootrOrbs.menu(3,a.getInventory(),a,source);
        h.assertTrue(((LiquidCrystalBallBlockEntity)again.ball).tank.getFluidAmount()==23000,"Player A fluid not persisted");again.removed(a);h.succeed();
    }
    public static void nativeConversionAndNoOrbDrops(GameTestHelper h){
        var source=orb(h,false);LootrOrbs.manage(source);
        h.assertTrue(!source.canPlaceItem(0,new ItemStack(Items.STONE)),"World container accepts hopper input");
        h.assertTrue(!source.dropsOnRemoval(),"Orb allows content drops");
        var pos=source.getBlockPos();h.getLevel().destroyBlock(pos,true);
        h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1)).isEmpty(),"Destroyed Lootr orb dropped items");
        var chest=pos.east(3);h.getLevel().setBlockAndUpdate(chest,Blocks.CHEST.defaultBlockState());
        var be=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(chest);
        be.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse("lootr_test:chests/foreign_structure")),42);
        noobanidus.mods.lootr.common.block.entity.BlockEntityTicker.addEntity(be,h.getLevel(),new net.minecraft.world.level.ChunkPos(chest));
        h.succeedWhen(()->h.assertTrue(h.getLevel().getBlockState(chest).is(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse("lootr:lootr_chest"))),"Foreign-table chest did not convert"));
    }
    public static void playerBreakDropsOnlyCleanShell(GameTestHelper h){
        var player=player(h,"orb-break");
        for(boolean liquid:new boolean[]{false,true}){
            var source=orb(h,liquid);LootrOrbs.manage(source);
            player.setShiftKeyDown(false);
            var event=new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(h.getLevel(),source.getBlockPos(),source.getBlockState(),player);
            com.mcmagic.omnira.compat.LootrOrbEvents.broken(event);
            h.assertTrue(event.isCanceled(),"Unsneaking breaks managed orb");
            player.setShiftKeyDown(true);
            h.assertTrue(LootrOrbs.mayBreak(player),"Sneak break rejected by default Lootr settings");
            var drops=net.minecraft.world.level.block.Block.getDrops(source.getBlockState(),h.getLevel(),source.getBlockPos(),source,player,ItemStack.EMPTY);
            h.assertTrue(drops.size()==1&&drops.getFirst().is(source.getBlockState().getBlock().asItem())
                    &&!drops.getFirst().has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA),"Break dropped loot, identity or no shell");
            h.assertTrue(!source.dropsOnRemoval(),"Breaking releases personal contents");
        }
        h.succeed();
    }

    public static void actualStructoryTemplateConvertsAndGeneratesLoot(GameTestHelper h){
        actualStructure(h,"structory:outcast_villager/boat/3/top",Blocks.BARREL);
    }
    public static void actualStructoryChestConvertsAndGeneratesLoot(GameTestHelper h){
        actualStructure(h,"structory:harvest/old_manor/decor1",Blocks.CHEST);
    }
    public static void cataclysmTreasureConverts(GameTestHelper h){actualStructure(h,"cataclysm:amethyst_nest",Blocks.BARREL);}
    public static void cataclysmChestConverts(GameTestHelper h){actualStructure(h,"cataclysm:ruined_citadel12",Blocks.CHEST);}
    private static void actualStructure(GameTestHelper h,String id,net.minecraft.world.level.block.Block container){
        var level=h.getLevel();
        var template=level.getStructureManager().get(ResourceLocation.parse(id));
        if(template.isEmpty()){if(id.startsWith("cataclysm:")&&net.neoforged.fml.ModList.get().isLoaded("cataclysm"))h.fail("Missing installed Cataclysm template");else h.succeed();return;}
        var origin=h.absolutePos(new BlockPos(5,id.contains("ruined_citadel")?90:2,5));
        template.get().placeInWorld(level,origin,origin,new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setIgnoreEntities(true),level.random,2);
        var settings=new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setIgnoreEntities(true);
        var boxes=template.get().filterBlocks(origin,settings,container);
        h.assertTrue(!boxes.isEmpty(),"No actual template containers");
        var pos=boxes.stream().filter(b->b.nbt()!=null&&b.nbt().contains("LootTable")).findFirst().orElseThrow().pos();
        var chest=(net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity)level.getBlockEntity(pos);
        h.assertTrue(chest!=null && chest.getLootTable()!=null,"Actual external chest/table missing");
        noobanidus.mods.lootr.common.block.entity.BlockEntityTicker.addEntity(chest,level,new net.minecraft.world.level.ChunkPos(pos));
        h.succeedWhen(()->{
            var provider=noobanidus.mods.lootr.common.api.LootrAPI.resolveBlockEntity(level.getBlockEntity(pos));
            h.assertTrue(provider!=null,"Actual external chest not converted");
            var owner=player(h,"structory-owner");
            var loot=noobanidus.mods.lootr.common.api.LootrAPI.getInventory(provider,owner);
            h.assertTrue(loot!=null && !loot.isEmpty(),"Converted external chest generated no personal loot");
        });
    }

    public static void ordinaryStorageAndSandStayNative(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(4,2,4));
        h.getLevel().setBlockAndUpdate(pos,ModBlocks.CRYSTAL_BALL.get().defaultBlockState());
        var plain=(CrystalBallBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(!LootrOrbs.manage(plain),"Player storage was enrolled");
        plain.setItem(0,new ItemStack(Items.STONE,7));h.assertTrue(plain.getItem(0).getCount()==7 && plain.dropsOnRemoval(),"Ordinary storage behavior changed");
        var sand=com.mcmagic.omnira.archaeology.ArchaeologyContent.SUSPICIOUS.get().defaultBlockState();
        h.assertTrue(noobanidus.mods.lootr.common.api.LootrAPI.replacementBlockState(sand)==null,"Omnira archaeology sand is convertible");h.succeed();
    }

    public static void blockedAutomationShatteringAndPersonalBucket(GameTestHelper h){
        var source=(LiquidCrystalBallBlockEntity)orb(h,true);var owner=player(h,"bucket-owner");LootrOrbs.manage(source);
        h.assertTrue(h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,source.getBlockPos(),net.minecraft.core.Direction.UP)==null,"Managed orb exposes fluid automation");
        var menu=(CrystalBallMenu)LootrOrbs.menu(1,owner.getInventory(),owner,source);var view=(LiquidCrystalBallBlockEntity)menu.ball;
        h.assertTrue(!view.upgrades.isItemValid(0,new ItemStack(com.mcmagic.omnira.registry.ModItems.BASIC_STACKING_UPGRADE.get())),"Personal orb accepts upgrades");
        var nodePos=source.getBlockPos().above();h.getLevel().setBlockAndUpdate(nodePos,ModBlocks.RESONANCE_CORE.get().defaultBlockState());
        var node=(ResonanceCoreBlockEntity)h.getLevel().getBlockEntity(nodePos);node.owner=owner.getUUID();
        h.assertTrue(node.items()==null&&node.fluids()==null&&!node.permits(owner),"Remote core accesses Lootr container");
        view.tank.setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,24000));
        view.setItem(0,new ItemStack(Items.BUCKET));
        for(int tick=1;tick<=12;tick++)h.runAtTickTime(tick,menu::broadcastChanges);
        h.runAtTickTime(14,()->{
            h.assertTrue(view.getItem(0).is(Items.WATER_BUCKET)&&view.tank.getFluidAmount()==23000,"Personal bucket UI broken");
            menu.removed(owner);for(int i=0;i<9;i++)source.landedOn(owner);
            h.assertTrue(source.isRemoved()&&h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(source.getBlockPos()).inflate(1)).isEmpty(),"Shattering failed or dropped contents");h.succeed();
        });
    }
    public static void sameLootrOwnerUsesOneLiveView(GameTestHelper h){
        var source=orb(h,false);LootrOrbs.manage(source);var owner=player(h,"shared-owner");
        var a=(CrystalBallMenu)LootrOrbs.menu(1,owner.getInventory(),owner,source);
        var b=(CrystalBallMenu)LootrOrbs.menu(2,owner.getInventory(),owner,source);
        h.assertTrue(a.ball==b.ball,"Same Lootr inventory received two mutable snapshots");
        a.ball.clearContent();a.ball.setChanged();h.assertTrue(b.ball.isEmpty(),"Shared view can duplicate loot");
        a.removed(owner);b.removed(owner);h.succeed();
    }
}
