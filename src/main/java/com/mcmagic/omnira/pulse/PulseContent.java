package com.mcmagic.omnira.pulse;

import com.mcmagic.omnira.registry.*;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;

public final class PulseContent {
    private static PulseOrbBlock block(boolean resonant){return net.neoforged.fml.ModList.get().isLoaded("create")?com.mcmagic.omnira.compat.create.PulseOrbKinetics.block(resonant):new PulseOrbBlock(resonant);}
    public static final DeferredBlock<PulseOrbBlock> ORB=ModBlocks.BLOCKS.register("pulse_accumulator_orb",()->block(false));
    public static final DeferredBlock<PulseOrbBlock> RESONANT=ModBlocks.BLOCKS.register("resonant_accumulator_orb",()->block(true));
    public static final DeferredBlock<PulseWireBlock> WIRE=ModBlocks.BLOCKS.register("arcane_pulse_wire",()->new PulseWireBlock(BlockBehaviour.Properties.of().strength(.1F).noOcclusion().noCollission().lightLevel(s->s.getValue(PulseWireBlock.POWERED)?8:0)));
    public static final DeferredItem<BlockItem> ORB_ITEM=ModItems.ITEMS.registerSimpleBlockItem(ORB);
    public static final DeferredItem<BlockItem> RESONANT_ITEM=ModItems.ITEMS.registerSimpleBlockItem(RESONANT);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<BlockEntity>> ENTITY=ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register("pulse_accumulator_orb",()->BlockEntityType.Builder.of((net.minecraft.core.BlockPos p,net.minecraft.world.level.block.state.BlockState s)->net.neoforged.fml.ModList.get().isLoaded("create")?com.mcmagic.omnira.compat.create.PulseOrbKinetics.entity(p,s):new PulseOrbEntity(p,s),ORB.get(),RESONANT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<PulseWireEntity>> WIRE_ENTITY=ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register("arcane_pulse_wire",()->BlockEntityType.Builder.of(PulseWireEntity::new,WIRE.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<PulseOrbMenu>> MENU=ModMenuTypes.MENU_TYPES.register("pulse_accumulator_orb",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(PulseOrbMenu::new));
    public static void init(){}
    private PulseContent(){}
}
