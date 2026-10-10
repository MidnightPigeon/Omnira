package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.storage.*;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.fluids.FluidStack;

@GameTestHolder("omnira_storage_network")
@PrefixGameTestTemplate(false)
public final class CrystalStorageNetworkGameTests {
    @GameTest(template="spell_arena") public static void frequencyChangesPublishColorsImmediately(GameTestHelper h){
        for(boolean liquid:new boolean[]{false,true}){
            var a=orb(h,liquid?9:3,liquid);var b=orb(h,liquid?12:6,liquid);
            var marker=new ItemStack(Items.PAPER);marker.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(java.util.UUID.randomUUID().toString()));
            a.setFrequency(0,marker);b.setFrequency(0,marker);
            int color=liquid?2:1;
            h.assertTrue(a.getUpdateTag(h.getLevel().registryAccess()).getInt("NetworkStatus")==color&&b.getUpdateTag(h.getLevel().registryAccess()).getInt("NetworkStatus")==color,"Connected color waits for periodic tick");
            b.setFrequency(0,ItemStack.EMPTY);
            h.assertTrue(a.getUpdateTag(h.getLevel().registryAccess()).getInt("NetworkStatus")==0&&b.getUpdateTag(h.getLevel().registryAccess()).getInt("NetworkStatus")==0,"Disconnected color waits for periodic tick");
        }
        h.succeed();
    }
    private static CrystalBallBlockEntity orb(GameTestHelper h,int x,boolean liquid){
        var pos=new BlockPos(x,3,4);h.setBlock(pos,(liquid?CrystalStorageContent.FLUID_ORB:CrystalStorageContent.ITEM_ORB).get());
        return (CrystalBallBlockEntity)h.getBlockEntity(pos);
    }
    private static void frequency(GameTestHelper h,CrystalBallBlockEntity... balls){
        var marker=new ItemStack(Items.PAPER);
        marker.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(java.util.UUID.randomUUID().toString()));
        for(var ball:balls){ball.frequency[0]=marker.copy();CrystalStorageNetworks.get(h.getLevel()).register(ball);}
    }
    private static int count(CrystalBallBlockEntity ball){int sum=0;for(int i=0;i<ball.getContainerSize();i++)sum+=ball.getItem(i).getCount();return sum;}
    @GameTest(template="spell_arena") public static void sharedContentsUpgradesAndDetach(GameTestHelper h){
        var a=orb(h,3,false);var b=orb(h,6,false);
        a.setItem(0,new ItemStack(Items.DIAMOND,40));
        a.upgrades.setStackInSlot(0,new ItemStack(ModItems.BASIC_STACKING_UPGRADE.get()));
        frequency(h,a,b);
        h.assertTrue(a.networkStatus==1&&b.networkStatus==1,"Shared bases not green");
        h.assertTrue(count(b)==40&&b.multiplier()==2,"Contents or capacity upgrade not shared");
        b.removeItem(0,3);h.assertTrue(count(a)==37,"Removal did not affect shared storage");
        var tag=a.exportLocal();
        h.assertTrue(tag.getCompound("Upgrades").getList("Items",10).isEmpty(),"Endpoint saved duplicate upgrades");
        h.assertTrue(tag.getList("Items",10).isEmpty(),"Endpoint saved duplicate inventory");
        a.detachNetwork();h.assertTrue(count(a)==0&&count(b)==37,"Detach duplicated or lost contents");
        b.detachNetwork();h.assertTrue(count(b)==37&&b.multiplier()==2,"Last endpoint failed to recover payload");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void failedEntrantRequiresExplicitRetuning(GameTestHelper h){
        var a=orb(h,3,false);var b=orb(h,6,false);var existing=orb(h,9,false);
        for(int i=0;i<12;i++)a.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        frequency(h,a,existing);var marker=a.frequency[0].copy();
        b.setItem(0,new ItemStack(Items.DIAMOND));b.setFrequency(0,marker);
        h.assertTrue(a.networkStatus==1&&existing.networkStatus==1&&b.networkStatus==3,"Failed entrant affected the established network");
        h.assertTrue(b.frequency[0].isEmpty()&&b.frequency[1].isEmpty(),"Rejected frequency markers were retained");
        h.assertTrue(count(a)==768&&count(b)==1,"Failed merge changed either payload");
        a.removeItem(0,64);CrystalStorageNetworks.get(h.getLevel()).reconcile();
        h.assertTrue(b.networkStatus==3&&count(a)==704&&count(b)==1,"Free space retried a rejected connection");
        var saved=CrystalStorageNetworks.get(h.getLevel()).save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());
        h.getLevel().getServer().overworld().getDataStorage().set("omnira_crystal_storage",CrystalStorageNetworks.load(saved,h.getLevel().registryAccess()));
        CrystalStorageNetworks.get(h.getLevel()).register(b);CrystalStorageNetworks.get(h.getLevel()).reconcile();
        h.assertTrue(b.networkStatus==3&&count(b)==1,"Reload cleared rejection or retried connection");
        b.setFrequency(1,ItemStack.EMPTY);
        h.assertTrue(b.networkStatus==0&&count(b)==1,"Clearing an empty frequency did not reset failure");
        b.setFrequency(0,marker);
        h.assertTrue(a.networkStatus==1&&b.networkStatus==1&&count(a)==705&&count(b)==705,"Manual retuning failed to join after space was freed");
        a.detachNetwork();b.detachNetwork();existing.detachNetwork();h.succeed();
    }
    @GameTest(template="spell_arena") public static void liquidsMergeAndPersist(GameTestHelper h){
        var a=(LiquidCrystalBallBlockEntity)orb(h,3,true);var b=(LiquidCrystalBallBlockEntity)orb(h,6,true);
        a.tank.setFluid(new FluidStack(net.minecraft.world.level.material.Fluids.LAVA,3000));frequency(h,a,b);
        h.assertTrue(a.networkStatus==2&&b.tank.getFluidAmount()==3000,"Liquid sharing not established");
        b.tank.drain(500,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(a.tank.getFluidAmount()==2500,"Liquid drain not shared");
        var ledger=CrystalStorageNetworks.get(h.getLevel()).save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());
        h.assertTrue(!ledger.getList("Stores",10).isEmpty(),"Shared payload not persisted");
        h.getLevel().getServer().overworld().getDataStorage().set("omnira_crystal_storage",CrystalStorageNetworks.load(ledger,h.getLevel().registryAccess()));
        h.assertTrue(a.tank.getFluidAmount()==2500&&b.tank.getFluidAmount()==2500,"Reload lost authoritative fluid state");
        a.detachNetwork();b.detachNetwork();h.assertTrue(b.tank.getFluidAmount()==2500,"Final fluid payload lost");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void directedItemAndFluidTransfer(GameTestHelper h){
        var sourcePos=new BlockPos(3,3,4);var targetPos=new BlockPos(7,3,4);
        h.setBlock(sourcePos,Blocks.CHEST);h.setBlock(targetPos,Blocks.CHEST);
        h.setBlock(sourcePos.above(),ModBlocks.RESONANCE_CORE.get());h.setBlock(targetPos.above(),ModBlocks.RESONANCE_CORE.get());
        var a=(ResonanceCoreBlockEntity)h.getBlockEntity(sourcePos.above());var b=(ResonanceCoreBlockEntity)h.getBlockEntity(targetPos.above());
        ((net.minecraft.world.Container)h.getBlockEntity(sourcePos)).setItem(0,new ItemStack(Items.DIAMOND,4));
        NodeTransport.pair(a,b);NodeTransport.tick(a);
        h.assertTrue(a.output&&!b.output,"Transport directions reversed");
        h.assertTrue(a.getBlockState().getValue(com.mcmagic.omnira.block.ResonanceCoreBlock.MODE)==1&&b.getBlockState().getValue(com.mcmagic.omnira.block.ResonanceCoreBlock.MODE)==1,"Transport overwrote original node colors");
        h.assertTrue(((net.minecraft.world.Container)h.getBlockEntity(sourcePos)).getItem(0).getCount()==3,"Transport exceeded one item");
        h.assertTrue(((net.minecraft.world.Container)h.getBlockEntity(targetPos)).getItem(0).getCount()==1,"Transport failed");
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var rod=new ItemStack(CrystalStorageContent.ROD.get());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,rod);
        for(var endpoint:new ResonanceCoreBlockEntity[]{a,b}){
            NodeTransport.pair(a,b);
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(endpoint.getBlockPos()),net.minecraft.core.Direction.UP,endpoint.getBlockPos(),false);
            var result=rod.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
            h.assertTrue(result.consumesAction()&&a.peer==null&&b.peer==null,"Rod failed to unlink either endpoint");
            h.assertTrue(a.getBlockState().getValue(com.mcmagic.omnira.block.ResonanceCoreBlock.MODE)==1&&b.getBlockState().getValue(com.mcmagic.omnira.block.ResonanceCoreBlock.MODE)==1,"Unlinking overwrote node colors");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void recipePreservesOrbData(GameTestHelper h){
        var input=new net.minecraft.world.SimpleContainer(7);
        for(int i=0;i<6;i++)input.setItem(i,new ItemStack(ModItems.RESONANCE_CRYSTAL.get()));
        var original=new ItemStack(ModItems.CRYSTAL_BALL.get());var data=new net.minecraft.nbt.CompoundTag();data.putString("Test","kept");
        original.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(data));input.setItem(6,original);
        var recipes=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.ASSEMBLY_TYPE.get());
        var recipe=recipes.stream().filter(r->r.value().getResultItem(h.getLevel().registryAccess()).is(CrystalStorageContent.ITEM.get())).findFirst().orElseThrow().value();
        h.assertTrue(recipe.matches(new com.mcmagic.omnira.recipe.AssemblyRecipe.Input(input),h.getLevel()),"Resonant assembly did not match");
        var output=recipe.assemble(new com.mcmagic.omnira.recipe.AssemblyRecipe.Input(input),h.getLevel().registryAccess());
        h.assertTrue(output.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA).copyTag().getString("Test").equals("kept"),"Assembly erased contents");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=1200) public static void crossDimensionBridgeAndSplit(GameTestHelper h){
        var a=orb(h,3,false);a.setItem(0,new ItemStack(Items.DIAMOND,30));
        var other=h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        h.assertTrue(other!=null,"Nether missing from integration environment");
        var pos=h.absolutePos(new BlockPos(6,3,4)).atY(80);other.getChunkAt(pos);
        var previous=other.getBlockState(pos);other.setBlockAndUpdate(pos,CrystalStorageContent.ITEM_ORB.get().defaultBlockState());
        var b=(CrystalBallBlockEntity)other.getBlockEntity(pos);b.setItem(0,new ItemStack(Items.DIAMOND,10));
        try{
            frequency(h,a,b);
            h.assertTrue(a.networkStatus==0&&b.networkStatus==0&&count(a)==30&&count(b)==10,"Dimensions joined without stabilization");
            a.upgrades.setStackInSlot(0,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()));
            CrystalStorageNetworks.get(h.getLevel()).reconcile();
            h.assertTrue(a.networkStatus==1&&b.networkStatus==1&&count(a)==40&&count(b)==40,"Bridge failed to merge dimensions");
            b.upgrades.extractItem(0,1,false);CrystalStorageNetworks.get(h.getLevel()).reconcile();
            h.assertTrue(count(a)+count(b)==40,"Removing bridge duplicated or lost storage");
        }finally{a.detachNetwork();b.detachNetwork();other.setBlockAndUpdate(pos,previous);}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void berryAndCropHarvest(GameTestHelper h){
        var pos=new BlockPos(4,3,4);h.setBlock(pos.below(),Blocks.FARMLAND);
        h.setBlock(pos,Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE,7));
        var rod=new ItemStack(CrystalStorageContent.ROD.get());var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(pos)),net.minecraft.core.Direction.UP,h.absolutePos(pos),false);
        TuningRodItem.harvest(h.getLevel(),player,rod,hit);
        h.assertTrue(h.getBlockState(pos).getValue(net.minecraft.world.level.block.CropBlock.AGE)==0,"Wheat not replanted at initial age");
        h.setBlock(pos.below(),Blocks.DIRT);h.setBlock(pos,Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE,3));
        TuningRodItem.harvest(h.getLevel(),player,rod,hit);
        h.assertTrue(h.getBlockState(pos).is(Blocks.SWEET_BERRY_BUSH)&&h.getBlockState(pos).getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE)==1,"Berry bush was removed");
        h.setBlock(pos.above(),Blocks.STONE);h.setBlock(pos,Blocks.CAVE_VINES.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.BERRIES,true));
        TuningRodItem.harvest(h.getLevel(),player,rod,hit);
        h.assertTrue(h.getBlockState(pos).is(Blocks.CAVE_VINES)&&!h.getBlockState(pos).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.BERRIES),"Cave vine was removed");
        h.setBlock(pos,Blocks.MELON);TuningRodItem.harvest(h.getLevel(),player,rod,hit);
        h.assertTrue(h.getBlockState(pos).isAir(),"Melon not harvested as block break");
        h.succeed();
    }
}
