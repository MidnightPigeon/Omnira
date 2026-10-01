package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.compat.CrystalEncasing;
import com.mcmagic.omnira.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.encasing.EncasableBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_crystal_encasing")
@PrefixGameTestTemplate(false)
public final class CrystalEncasingGameTests {
    @GameTest(template="spell_arena")
    public static void crystalMassIsZero(GameTestHelper h) {
        for(var block:java.util.List.of(ModBlocks.CRYSTAL_CASING.get(),ModBlocks.INFUSED_CRYSTAL_CASING.get(),ModBlocks.MANA_ENGINE.get(),
                ModBlocks.CRYSTAL_PROCESSING_TABLE.get(),ModBlocks.ANALYSIS_ARTISAN_TABLE.get(),
                ModBlocks.ARCANE_ASSEMBLY_TABLE.get(),ModBlocks.CRYSTAL_PEDESTAL.get(),ModBlocks.CRYSTAL_BALL.get(),
                CrystalEncasing.SHAFT.get(),CrystalEncasing.COG.get(),CrystalEncasing.LARGE_COG.get(),
                CrystalEncasing.INFUSED_SHAFT.get(),CrystalEncasing.INFUSED_COG.get(),CrystalEncasing.INFUSED_LARGE_COG.get())) {
            h.assertTrue(dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getMass(
                    h.getLevel(),h.absolutePos(new BlockPos(1,2,1)),block.defaultBlockState())==0,"Expected zero mass: "+block);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void encaseToggleAndUnwrap(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var originals=java.util.List.of(AllBlocks.SHAFT.get(),AllBlocks.COGWHEEL.get(),AllBlocks.LARGE_COGWHEEL.get());
        for(int family=0;family<2;family++) {
        var casing=new ItemStack(family==0?ModBlocks.CRYSTAL_CASING.get():ModBlocks.INFUSED_CRYSTAL_CASING.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,casing);
        var wrapped=family==0?java.util.List.of(CrystalEncasing.SHAFT.get(),CrystalEncasing.COG.get(),CrystalEncasing.LARGE_COG.get())
                :java.util.List.of(CrystalEncasing.INFUSED_SHAFT.get(),CrystalEncasing.INFUSED_COG.get(),CrystalEncasing.INFUSED_LARGE_COG.get());
        for(int i=0;i<3;i++) for(var axis:Direction.Axis.values()) {
            var pos=h.absolutePos(new BlockPos(2+i*3,3+axis.ordinal()*3,2));
            var original=originals.get(i).defaultBlockState().setValue(BlockStateProperties.AXIS,axis);
            h.getLevel().setBlockAndUpdate(pos,original);
            var face=Direction.fromAxisAndDirection(axis,Direction.AxisDirection.POSITIVE);
            var hit=new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false);
            ((EncasableBlock)original.getBlock()).tryEncase(original,h.getLevel(),pos,casing,player,InteractionHand.MAIN_HAND,hit);
            var state=h.getLevel().getBlockState(pos);
            h.assertTrue(state.is(wrapped.get(i)) && casing.getCount()==1,"Encasing must preserve item and select crystal variant");
            h.assertTrue(!state.canOcclude(),"Casing must remain transparent");
            var be=h.getLevel().getBlockEntity(pos);
            h.assertTrue(be!=null && be.getType().isValid(state),"Create entity must support the variant");
            var block=(IRotate)state.getBlock();
            boolean before=block.hasShaftTowards(h.getLevel(),pos,state,face);
            var context=new UseOnContext(h.getLevel(),player,InteractionHand.MAIN_HAND,new ItemStack(Items.STICK),hit);
            ((IWrenchable)state.getBlock()).onWrenched(state,context);
            var changed=h.getLevel().getBlockState(pos);
            h.assertTrue(block.hasShaftTowards(h.getLevel(),pos,changed,face)!=before,"Wrench must toggle axial connection");
            ((IWrenchable)changed.getBlock()).onSneakWrenched(changed,context);
            h.assertTrue(h.getLevel().getBlockState(pos).equals(original),"Unwrap must preserve original axis and component");
        }
        }
        h.succeed();
    }
}
