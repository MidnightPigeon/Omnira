package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.anchor.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_stability")
@PrefixGameTestTemplate(false)
public final class StabilityAnchorGameTests {
    private static StabilityAnchorEntity place(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(6,4,6));h.getLevel().setBlockAndUpdate(pos,AnchorContent.ACTIVE.get().defaultBlockState());
        return (StabilityAnchorEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="spell_arena",batch="anchor_sync")
    public static void fieldTeleportAndToolScope(GameTestHelper h){
        var anchor=place(h);var l=h.getLevel();var p=anchor.getBlockPos();var center=Vec3.atCenterOf(p);
        h.assertTrue(StabilityField.contains(l,center.add(64,0,0))&&!StabilityField.contains(l,center.add(64.01,0,0)),"Sphere boundary incorrect");
        var cow=h.spawn(EntityType.COW,new BlockPos(8,4,6));cow.setNoAi(true);
        var original=cow.position();cow.teleportTo(original.x+1,original.y,original.z);
        h.assertTrue(cow.position().equals(original),"Local entity teleport escaped anchor");
        h.assertTrue(!cow.randomTeleport(original.x+2,original.y,original.z,true),"Random teleport bypassed anchor");
        h.assertTrue(cow.changeDimension(new DimensionTransition(l,original.add(100,0,0),Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING))==null,"Dimension transition bypassed anchor");
        h.assertTrue(com.mcmagic.omnira.space.geodes.DeflectionMotion.coverage(l,cow)==0,"Crystal coverage not suppressed");
        com.mcmagic.omnira.space.geodes.DeflectionMotion.turn(cow,10);
        h.assertTrue(cow.getPersistentData().getFloat("DeflectionPending")==0,"Block deflection was queued inside field");
        StabilityField.toolTravel(cow,true,()->{cow.teleportTo(original.x+100,original.y,original.z);return true;});
        h.assertTrue(cow.getX()==original.x+100,"Enhanced tool cannot leave");
        cow.teleportTo(original.x,original.y,original.z);h.assertTrue(cow.getX()==original.x+100,"Inbound teleport bypassed field");
        StabilityField.toolTravel(cow,true,()->{cow.teleportTo(original.x,original.y,original.z);return true;});
        h.assertTrue(cow.position().equals(original),"Enhanced tool cannot enter");
        try{StabilityField.toolTravel(cow,true,()->{throw new IllegalStateException("test");});}catch(IllegalStateException expected){}
        cow.teleportTo(original.x+2,original.y,original.z);h.assertTrue(cow.position().equals(original),"Tool exception leaked bypass");
        var drops=net.minecraft.world.level.block.Block.getDrops(anchor.getBlockState(),l,p,anchor,cow,ItemStack.EMPTY);
        h.assertTrue(drops.size()==1&&drops.getFirst().is(AnchorContent.DORMANT.asItem()),"Active anchor must drop dormant anchor");
        l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
        h.assertTrue(!StabilityField.contains(l,center),"Broken anchor left a field");
        cow.teleportTo(original.x+1,original.y,original.z);h.assertTrue(cow.getX()==original.x+1,"Teleport did not resume after removal");
        cow.discard();h.succeed();
    }
    @GameTest(template="spell_arena",batch="anchor_sync")
    public static void playerTeleportsAndDormantActivation(GameTestHelper h){
        var anchor=place(h);var level=h.getLevel();var pos=anchor.getBlockPos();
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"stability-player");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        player.connection=new net.neoforged.neoforge.common.util.FakePlayer(level,profile).connection;
        var origin=Vec3.atCenterOf(pos).add(2,0,0);player.setPos(origin);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.ENHANCED_RECALL_CRYSTAL.get()));
        h.assertTrue(!player.teleportTo(level,origin.x+100,origin.y,origin.z,java.util.Set.of(),0,0),"Possessing an enhanced tool granted generic teleport immunity");
        player.teleportTo(level,origin.x+100,origin.y,origin.z,0,0);
        h.assertTrue(player.position().equals(origin),"Void player overload bypassed the field");
        h.assertTrue(player.changeDimension(new DimensionTransition(level,origin.add(100,0,0),Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING))==null,"Player dimension transition bypassed field");
        h.assertTrue(StabilityField.toolTravel(player,true,()->!StabilityField.blocks(player,level,origin.add(100,0,0))),"Enhanced departure not allowed");
        player.setPos(origin.add(100,0,0));
        h.assertTrue(StabilityField.blocks(player,level,origin),"Player arrival not blocked");
        h.assertTrue(StabilityField.toolTravel(player,true,()->!StabilityField.blocks(player,level,origin)),"Enhanced arrival not allowed");
        level.setBlockAndUpdate(pos,AnchorContent.DORMANT.get().defaultBlockState());
        h.assertTrue(!StabilityField.contains(level,origin),"Dormant anchor still active");
        var core=new ItemStack(ModItems.SPACE_MICROCORE.get(),2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,core);
        level.getBlockState(pos).useItemOn(core,level,player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        h.assertTrue(level.getBlockState(pos).is(AnchorContent.ACTIVE.get())&&core.getCount()==1,"Microcore activation failed or consumed wrong amount");
        h.assertTrue(StabilityField.contains(level,origin),"Reactivated anchor not indexed");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());h.succeed();
    }
    @GameTest(template="spell_arena",batch="anchor_sync")
    public static void coveredGateKeepsItsPair(GameTestHelper h){
        var anchor=place(h);var level=h.getLevel();
        for(int x=1;x<15;x++)for(int z=1;z<15;z++)h.setBlock(new BlockPos(x,2,z),Blocks.STONE);
        var gates=new com.mcmagic.omnira.space.FoldGateBlockEntity[2];
        for(int i=0;i<2;i++){
            var pos=h.absolutePos(i==0?new BlockPos(3,3,3):new BlockPos(10,3,10));
            var state=ModBlocks.STABLE_FOLD_GATE.get().defaultBlockState();level.setBlock(pos,state,2);
            level.setBlock(pos.above(),state.setValue(com.mcmagic.omnira.space.FoldGateBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),3);
            gates[i]=(com.mcmagic.omnira.space.FoldGateBlockEntity)level.getBlockEntity(pos);
        }
        com.mcmagic.omnira.space.FoldGateBlockEntity.pair(gates[0],gates[1]);
        var pig=h.spawn(EntityType.PIG,new BlockPos(3,3,4));pig.setNoAi(true);
        h.assertTrue(com.mcmagic.omnira.space.FoldGateTransit.transfer(gates[0],pig,true),"Covered portal could not depart and arrive");
        h.assertTrue(gates[0].peer!=null&&gates[1].peer!=null,"Anchor broke the portal pairing");
        h.assertTrue(com.mcmagic.omnira.space.FoldGateTransit.transfer(gates[1],pig,true),"Covered portal return failed");
        h.assertTrue(StabilityField.blocks(pig,level,pig.position().add(1,0,0)),"Portal exemption leaked into unrelated travel");
        level.setBlockAndUpdate(anchor.getBlockPos(),Blocks.AIR.defaultBlockState());
        h.assertTrue(com.mcmagic.omnira.space.FoldGateTransit.transfer(gates[0],pig,true),"Portal did not recover after removal");
        gates[0].unlink();pig.discard();h.succeed();
    }
    @GameTest(template="spell_arena",batch="anchor_native_portal",timeoutTicks=30)
    public static void nativePortalApiExemptsOnlyItsTransition(GameTestHelper h){
        var anchor=place(h);var level=h.getLevel();
        var pig=h.spawn(EntityType.PIG,new BlockPos(3,4,3));pig.setNoAi(true);pig.setNoGravity(true);
        var target=h.absoluteVec(new Vec3(11,4,11));
        pig.setAsInsidePortal((l,e,p)->new DimensionTransition(l,target,Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING),pig.blockPosition());
        h.runAfterDelay(3,()->{
            h.assertTrue(pig.position().distanceToSqr(target)<.01,"Vanilla Portal API was blocked by anchor");
            var original=pig.position();pig.teleportTo(original.x+1,original.y,original.z);
            h.assertTrue(pig.position().equals(original),"Native portal left a persistent exemption");
            pig.discard();level.setBlockAndUpdate(anchor.getBlockPos(),Blocks.AIR.defaultBlockState());h.succeed();
        });
    }
    @GameTest(template="spell_arena",batch="anchor_sync")
    public static void geodeRoughPatchesContainLuminousCrystals(GameTestHelper h){
        int rough=0,excited=0;
        for(int x=-64;x<64;x++)for(int z=-64;z<64;z++){
            int y=com.mcmagic.omnira.space.geodes.DislocatedTerrain.floor(x,z);
            var state=com.mcmagic.omnira.space.geodes.DislocatedTerrain.block(x,y,z);
            if(state.is(DreamContent.SPATIAL_CRYSTAL.get()))rough++;
            if(state.is(DreamContent.EXCITED_SPATIAL_CRYSTAL.get()))excited++;
        }
        double rate=excited/(double)(rough+excited);
        h.assertTrue(rough>0&&rate>.20&&rate<.30,"Expected about one quarter of rough patches to glow: "+rate);
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=90,batch="anchor_tools")
    public static void enhancedToolEntryPoints(GameTestHelper h){
        var anchor=place(h);var level=h.getLevel();
        var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"anchor-tools")){
            @Override public boolean teleportTo(net.minecraft.server.level.ServerLevel target,double x,double y,double z,
                    java.util.Set<net.minecraft.world.entity.RelativeMovement> relative,float yaw,float pitch){
                h.assertTrue(!StabilityField.blocks(this,target,new Vec3(x,y,z)),"Actual enhanced tool did not scope its exemption");
                setServerLevel(target);setPos(x,y,z);return true;
            }
        };
        player.setPos(h.absoluteVec(new Vec3(2,3,2)));
        player.getAttribute(ModAttributes.MAX_MANA).setBaseValue(500);
        player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(500,500));
        var hand=net.minecraft.world.InteractionHand.MAIN_HAND;
        player.setItemInHand(hand,new ItemStack(ModItems.DREAM_RABBIT_REMAINS.get()));
        h.assertTrue(!com.mcmagic.omnira.dream.DreamRabbitTravel.start(player,hand,(com.mcmagic.omnira.dream.DreamRabbitItem)ModItems.DREAM_RABBIT_REMAINS.get()),"Ordinary rabbit escaped source restriction");
        h.assertTrue(player.getMainHandItem().getCount()==1&&player.getData(ModAttachments.MANA).current()==500,"Blocked rabbit consumed resources");
        player.setItemInHand(hand,new ItemStack(ModItems.ALICE_DREAM_RABBIT_SIGIL.get()));
        h.assertTrue(com.mcmagic.omnira.dream.DreamRabbitTravel.start(player,hand,(com.mcmagic.omnira.dream.DreamRabbitItem)ModItems.ALICE_DREAM_RABBIT_SIGIL.get()),"Enhanced rabbit refused to leave field");
        player.setPos(player.position().add(1,0,0));com.mcmagic.omnira.dream.DreamRabbitTravel.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        var pos=h.absolutePos(new BlockPos(10,3,10));
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)level.setBlockAndUpdate(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState());
        var state=ModBlocks.WAYMARK.get().defaultBlockState();level.setBlock(pos,state,2);
        level.setBlock(pos.above(),state.setValue(com.mcmagic.omnira.block.WaymarkBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),3);
        var mark=(com.mcmagic.omnira.block.entity.WaymarkBlockEntity)level.getBlockEntity(pos);mark.discover(player);
        player.setItemInHand(hand,new ItemStack(ModItems.RECALL_CRYSTAL.get()));
        h.assertTrue(!com.mcmagic.omnira.travel.RecallTravel.start(player,hand,mark.id()),"Ordinary recall escaped source restriction");
        player.setItemInHand(hand,new ItemStack(ModItems.ENHANCED_RECALL_CRYSTAL.get()));
        h.assertTrue(com.mcmagic.omnira.travel.RecallTravel.start(player,hand,mark.id()),"Enhanced recall refused to start");
        h.runAfterDelay(61,()->{
            com.mcmagic.omnira.travel.RecallTravel.advance(player);
            h.assertTrue(player.position().distanceToSqr(pos.getCenter())<30,"Enhanced recall failed with both endpoints covered");
            h.assertTrue(player.getData(ModAttachments.MANA).current()==500,"Enhanced local recall changed its original cost");
            h.assertTrue(StabilityField.blocks(player,level,player.position().add(1,0,0)),"Tool exemption leaked after completion");
            level.setBlockAndUpdate(anchor.getBlockPos(),Blocks.AIR.defaultBlockState());level.destroyBlock(pos.above(),false);h.succeed();
        });
    }
    @GameTest(template="spell_arena",batch="anchor_sync")
    public static void floorAndStructure(GameTestHelper h){
        for(int x=-384;x<=384;x+=32)for(int z=-384;z<=384;z+=32)
            h.assertTrue(com.mcmagic.omnira.spacetime.CorridorLayout.block(x,23,z).is(ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()),"Space bottom is not sealed");
        var cells=com.mcmagic.omnira.space.geodes.CalibrationPiece.CELLS;
        h.assertTrue(cells.stream().filter(c->c.name().equals("omnira:spatial_stability_anchor")).count()==1,"Captured anchor missing");
        h.assertTrue(cells.stream().noneMatch(c->c.name().equals("omnira:pure_solidified_spacetime")),"Placeholder survived export");
        h.assertTrue(cells.stream().anyMatch(c->c.name().equals("omnira:light_crystal_torch")),"Player changes lost");
        h.assertTrue(cells.stream().filter(c->c.name().equals("omnira:crystal_ball")&&c.nbt().contains("LootTable")).count()==2,"Treasure tables lost");
        h.assertTrue(cells.stream().filter(c->c.name().equals("omnira:liquid_crystal_ball")&&c.nbt().contains("FluidTreasure:1b")).count()==1,"Fluid treasure not restored");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=1300,batch="anchor_conversion")
    public static void conversionRequiresOneMinuteAndDamagesOnlyHostiles(GameTestHelper h){
        var anchor=place(h);var l=h.getLevel();var p=anchor.getBlockPos();
        var blocks=java.util.List.of(DreamContent.SPATIAL_CRYSTAL.get(),DreamContent.TEMPORAL_SILT.get(),TimeNatureContent.SILT.get());
        for(int i=0;i<blocks.size();i++){var pos=p.offset(i+2,0,0);l.setBlockAndUpdate(pos,blocks.get(i).defaultBlockState());h.assertTrue(anchor.begin(pos),"Target not selected");}
        var saved=anchor.saveWithoutMetadata(l.registryAccess());
        var restored=new StabilityAnchorEntity(p,anchor.getBlockState());restored.loadWithComponents(saved,l.registryAccess());
        h.assertTrue(restored.saveWithoutMetadata(l.registryAccess()).getList("Conversions",10).size()==3,"Conversion records lost on reload");
        var hostile=h.spawn(EntityType.ENDERMITE,new BlockPos(6,4,9));hostile.setNoAi(true);hostile.setNoGravity(true);
        var passive=h.spawn(EntityType.COW,new BlockPos(9,4,9));passive.setNoAi(true);float health=passive.getHealth();
        h.runAfterDelay(24,()->{h.assertTrue(hostile.getHealth()<=6,"Hostile not damaged");h.assertTrue(passive.getHealth()==health,"Passive creature damaged");hostile.discard();passive.discard();});
        h.runAfterDelay(1198,()->{
            for(int i=0;i<blocks.size();i++)h.assertTrue(l.getBlockState(p.offset(i+2,0,0)).is(blocks.get(i)),"Conversion finished early or natural decay interfered");
        });
        h.runAfterDelay(1203,()->{
            for(int i=0;i<blocks.size();i++)h.assertTrue(l.getBlockState(p.offset(i+2,0,0)).equals(StabilityAnchorEntity.result(blocks.get(i).defaultBlockState())),"Target did not finish conversion");
            l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());h.succeed();
        });
    }
}
