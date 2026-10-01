package com.mcmagic.omnira.gametest;
import com.mcmagic.omnira.forging.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import java.util.*;

public final class AdvancedForgeCompatibilityChecks {
    public static void run(GameTestHelper h) throws Exception{
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        var center=h.absolutePos(new BlockPos(5,2,5));var p=AdvancedForgeGameTests.player(h,center);AdvancedForgeGameTests.build(h,center,Direction.SOUTH);
        p.getAbilities().instabuild=true;var crystal=new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2);
        h.assertTrue(ForgeLayout.form(h.getLevel(),center,p,crystal) && crystal.getCount()==1,"Creative formation must also consume crystal");
        var state=h.getLevel().getBlockState(center);
        h.assertTrue(!com.simibubi.create.api.contraption.BlockMovementChecks.isMovementAllowed(state,h.getLevel(),center),"Unglued forge moved");
        var partial=new com.simibubi.create.content.contraptions.glue.SuperGlueEntity(h.getLevel(),new net.minecraft.world.phys.AABB(center).expandTowards(1,0,0));h.getLevel().addFreshEntity(partial);
        h.assertTrue(!com.mcmagic.omnira.compat.ForgeMovement.fullyGlued(h.getLevel(),center,state),"Partial glue accepted");partial.discard();
        var glue=new com.simibubi.create.content.contraptions.glue.SuperGlueEntity(h.getLevel(),new net.minecraft.world.phys.AABB(center).inflate(1,0,1).expandTowards(0,1,0));h.getLevel().addFreshEntity(glue);
        var parts=ForgeLayout.formedParts(h.getLevel(),center,state);
        h.assertTrue(com.simibubi.create.api.contraption.BlockMovementChecks.isMovementAllowed(state,h.getLevel(),center),"Fully glued forge rejected");
        h.assertTrue(com.mcmagic.omnira.compat.ForgeMovement.completeSelection(h.getLevel(),parts),"Whole selection rejected");
        var incomplete=new HashSet<>(parts);incomplete.remove(center.above());
        h.assertTrue(!com.mcmagic.omnira.compat.ForgeMovement.completeSelection(h.getLevel(),incomplete),"Partial selection accepted");
        if(net.neoforged.fml.ModList.get().isLoaded("sable")){
            SableChecks.run(h,center,state,parts,glue);return;
        }
        glue.discard();h.succeed();
    }
    private static final class SableChecks {
        private static void run(GameTestHelper h,BlockPos center,net.minecraft.world.level.block.state.BlockState state,Set<BlockPos> parts,net.minecraft.world.entity.Entity glue) throws Exception{
            var type=Class.forName("dev.simulated_team.simulated.util.assembly.SimAssemblyContraption");
            var selection=type.getConstructor(BlockPos.class,boolean.class).newInstance(center,false);
            h.assertTrue((boolean)type.getMethod("searchMovedStructure",net.minecraft.world.level.Level.class,BlockPos.class).invoke(selection,h.getLevel(),center),"Physics lever search rejected complete forge");
            glue.discard();
            var honeyType=Class.forName("dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity");
            var honey=(net.minecraft.world.entity.Entity)honeyType.getConstructor(net.minecraft.world.level.Level.class,net.minecraft.world.phys.AABB.class)
                    .newInstance(h.getLevel(),new net.minecraft.world.phys.AABB(center).expandTowards(1,0,0));h.getLevel().addFreshEntity(honey);
            h.assertTrue(!com.mcmagic.omnira.compat.ForgeMovement.fullyGlued(h.getLevel(),center,state),"Partial honey glue accepted");
            honeyType.getMethod("setBounds",net.minecraft.world.phys.AABB.class).invoke(honey,new net.minecraft.world.phys.AABB(center).inflate(1,0,1).expandTowards(0,1,0));
            h.assertTrue(com.mcmagic.omnira.compat.ForgeMovement.fullyGlued(h.getLevel(),center,state),"Whole honey glue rejected");
            selection=type.getConstructor(BlockPos.class,boolean.class).newInstance(center,false);
            h.assertTrue((boolean)type.getMethod("searchMovedStructure",net.minecraft.world.level.Level.class,BlockPos.class).invoke(selection,h.getLevel(),center),"Physics lever search rejected honey glue");
            var rejected=dev.ryanhcode.sable.api.SubLevelAssemblyHelper.gatherConnectedBlocks(center,h.getLevel(),100,(a,b,c,d,e)->false);
            h.assertTrue(rejected.assemblyState()!=dev.ryanhcode.sable.api.SubLevelAssemblyHelper.GatherResult.State.SUCCESS,"Sable accepted fragment");
            var gathered=dev.ryanhcode.sable.api.SubLevelAssemblyHelper.gatherConnectedBlocks(center,h.getLevel(),100,(a,b,c,d,e)->parts.contains(c));
            h.assertTrue(gathered.assemblyState()==dev.ryanhcode.sable.api.SubLevelAssemblyHelper.GatherResult.State.SUCCESS,"Sable rejected whole forge");
            var forge=(AdvancedForgeBlockEntity)h.getLevel().getBlockEntity(center);forge.setItem(0,new ItemStack(Items.DIAMOND));forge.setItem(9,new ItemStack(ModItems.DREAM_SPELL_CORE.get()));
            var ship=dev.ryanhcode.sable.api.SubLevelAssemblyHelper.assembleBlocks(h.getLevel(),center,gathered.blocks(),gathered.boundingBox());
            h.assertTrue(ship!=null && h.getLevel().getBlockState(center).isAir(),"Physical assembly failed");
            var moved=(AdvancedForgeBlockEntity)h.getLevel().getBlockEntity(ship.getPlot().getCenterBlock());
            h.assertTrue(moved!=null && moved.getItem(0).is(Items.DIAMOND) && moved.getItem(9).is(ModItems.DREAM_SPELL_CORE.get()),"Physical assembly lost inventory");
            h.assertTrue(ForgeLayout.formedParts(h.getLevel(),moved.getBlockPos(),moved.getBlockState()).size()==18,"Physical assembly lost parts");
            honey.discard();
            h.runAfterDelay(4,()->{
                var pushed=new HashSet<UUID>();var vortexCenter=ship.boundingBox().toMojang().getCenter().add(-2,0,0);
                var physics=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(h.getLevel()).physicsSystem().getPipeline();
                double before=physics.getLinearVelocity(ship,new org.joml.Vector3d()).x;
                com.mcmagic.omnira.compat.SableVortex.apply(h.getLevel(),vortexCenter,true,4,pushed);
                h.assertTrue(pushed.contains(ship.getUniqueId()),"Vortex did not target physics structure");
                double after=physics.getLinearVelocity(ship,new org.joml.Vector3d()).x;
                h.assertTrue(after>before,"Vortex impulse did not reach physics body: "+before+" -> "+after);
                com.mcmagic.omnira.compat.SableVortex.apply(h.getLevel(),vortexCenter,false,4,pushed);
                h.assertTrue(physics.getLinearVelocity(ship,new org.joml.Vector3d()).x<after,"Contraction did not pull physics body");
                h.assertTrue(moved.getItem(0).is(Items.DIAMOND),"Vortex dismantled physical work structure");h.succeed();
            });return;
        }
    }
}
