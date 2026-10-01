package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.encased.*;
import net.minecraft.core.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Loaded only with Create. Native kinetic entities retain networking, rendering and wrench behavior. */
public final class CrystalEncasing {
    public static final DeferredBlock<EncasedShaftBlock> SHAFT=ModBlocks.BLOCKS.register("crystal_encased_shaft",
            ()->new CrystalShaft(properties(),ModBlocks.CRYSTAL_CASING::get));
    public static final DeferredBlock<EncasedCogwheelBlock> COG=ModBlocks.BLOCKS.register("crystal_encased_cogwheel",
            ()->new EncasedCogwheelBlock(properties(),false,ModBlocks.CRYSTAL_CASING::get));
    public static final DeferredBlock<EncasedCogwheelBlock> LARGE_COG=ModBlocks.BLOCKS.register("crystal_encased_large_cogwheel",
            ()->new EncasedCogwheelBlock(properties(),true,ModBlocks.CRYSTAL_CASING::get));
    public static final DeferredBlock<EncasedShaftBlock> INFUSED_SHAFT=ModBlocks.BLOCKS.register("infused_crystal_encased_shaft",
            ()->new CrystalShaft(properties(),ModBlocks.INFUSED_CRYSTAL_CASING::get));
    public static final DeferredBlock<EncasedCogwheelBlock> INFUSED_COG=ModBlocks.BLOCKS.register("infused_crystal_encased_cogwheel",
            ()->new EncasedCogwheelBlock(properties(),false,ModBlocks.INFUSED_CRYSTAL_CASING::get));
    public static final DeferredBlock<EncasedCogwheelBlock> INFUSED_LARGE_COG=ModBlocks.BLOCKS.register("infused_crystal_encased_large_cogwheel",
            ()->new EncasedCogwheelBlock(properties(),true,ModBlocks.INFUSED_CRYSTAL_CASING::get));

    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().strength(1.5F).requiresCorrectToolForDrops().sound(SoundType.GLASS).lightLevel(s->10)
                .noOcclusion().isRedstoneConductor((s,l,p)->false).isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false);
    }
    public static void register(IEventBus bus) {
        bus.addListener((BlockEntityTypeAddBlocksEvent event)->{
            event.modify(AllBlockEntityTypes.ENCASED_SHAFT.get(),SHAFT.get(),INFUSED_SHAFT.get());
            event.modify(AllBlockEntityTypes.ENCASED_COGWHEEL.get(),COG.get(),INFUSED_COG.get());
            event.modify(AllBlockEntityTypes.ENCASED_LARGE_COGWHEEL.get(),LARGE_COG.get(),INFUSED_LARGE_COG.get());
        });
        bus.addListener((FMLCommonSetupEvent event)->event.enqueueWork(()->{
            EncasingRegistry.addVariant(AllBlocks.SHAFT.get(),SHAFT.get());
            EncasingRegistry.addVariant(AllBlocks.COGWHEEL.get(),COG.get());
            EncasingRegistry.addVariant(AllBlocks.LARGE_COGWHEEL.get(),LARGE_COG.get());
            EncasingRegistry.addVariant(AllBlocks.SHAFT.get(),INFUSED_SHAFT.get());
            EncasingRegistry.addVariant(AllBlocks.COGWHEEL.get(),INFUSED_COG.get());
            EncasingRegistry.addVariant(AllBlocks.LARGE_COGWHEEL.get(),INFUSED_LARGE_COG.get());
        }));
    }

    private static final class CrystalShaft extends EncasedShaftBlock {
        CrystalShaft(BlockBehaviour.Properties properties,java.util.function.Supplier<Block> casing) {
            super(properties,casing);
            registerDefaultState(defaultBlockState().setValue(EncasedCogwheelBlock.TOP_SHAFT,true)
                    .setValue(EncasedCogwheelBlock.BOTTOM_SHAFT,true));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(EncasedCogwheelBlock.TOP_SHAFT,EncasedCogwheelBlock.BOTTOM_SHAFT);
        }
        @Override public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction face) {
            return face.getAxis()==state.getValue(AXIS) && state.getValue(face.getAxisDirection()==Direction.AxisDirection.POSITIVE
                    ?EncasedCogwheelBlock.TOP_SHAFT:EncasedCogwheelBlock.BOTTOM_SHAFT);
        }
        @Override public InteractionResult onWrenched(BlockState state,UseOnContext context) {
            if(context.getClickedFace().getAxis()!=state.getValue(AXIS)) return super.onWrenched(state,context);
            if(!context.getLevel().isClientSide) {
                var property=context.getClickedFace().getAxisDirection()==Direction.AxisDirection.POSITIVE
                        ?EncasedCogwheelBlock.TOP_SHAFT:EncasedCogwheelBlock.BOTTOM_SHAFT;
                KineticBlockEntity.switchToBlockState(context.getLevel(),context.getClickedPos(),state.cycle(property));
            }
            return InteractionResult.SUCCESS;
        }
        @Override protected boolean areStatesKineticallyEquivalent(BlockState a,BlockState b) {
            return super.areStatesKineticallyEquivalent(a,b)
                    && a.getValue(EncasedCogwheelBlock.TOP_SHAFT)==b.getValue(EncasedCogwheelBlock.TOP_SHAFT)
                    && a.getValue(EncasedCogwheelBlock.BOTTOM_SHAFT)==b.getValue(EncasedCogwheelBlock.BOTTOM_SHAFT);
        }
    }
}
