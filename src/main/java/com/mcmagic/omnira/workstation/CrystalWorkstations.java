package com.mcmagic.omnira.workstation;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.fml.ModList;

public final class CrystalWorkstations {
    public enum Kind { FURNACE, GRINDSTONE, ANVIL }
    public static final String[] IDS={"crystal_furnace","crystal_grindstone","crystal_anvil","infused_crystal_furnace","infused_crystal_grindstone","infused_crystal_anvil"};
    public static final java.util.List<DeferredBlock<? extends Block>> BLOCKS=new java.util.ArrayList<>();
    static {
        for(int i=0;i<IDS.length;i++){
            final int index=i;
            var block=ModBlocks.BLOCKS.register(IDS[i],()->{
                var p=BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.AMETHYST).noOcclusion();
                return index>=3&&index!=4&&ModList.get().isLoaded("create")
                        ?com.mcmagic.omnira.compat.create.CrystalWorkstationFactory.block(p,index):new CrystalWorkstationBlock(p,index);
            });
            BLOCKS.add(block);ModItems.ITEMS.registerSimpleBlockItem(block);
        }
    }
    public static final java.util.function.Supplier<BlockEntityType<BlockEntity>> ENTITY=ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register("crystal_workstation",
            ()->BlockEntityType.Builder.of(CrystalWorkstations::entity,BLOCKS.stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
    public static final java.util.function.Supplier<net.minecraft.world.inventory.MenuType<CrystalWorkstationMenu>> MENU=ModMenuTypes.MENU_TYPES.register("crystal_workstation",
            ()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(CrystalWorkstationMenu::new));
    public static int index(BlockState state){return ((CrystalWorkstationBlock)state.getBlock()).index;}
    public static BlockEntity entity(BlockPos pos,BlockState state){
        return index(state)>=3&&index(state)!=4&&ModList.get().isLoaded("create")
                ?com.mcmagic.omnira.compat.create.CrystalWorkstationFactory.entity(pos,state):new CrystalWorkstationEntity(pos,state);
    }
    public static CrystalWorkstationEntity work(BlockEntity entity){return entity instanceof WorkstationAccess a?a.workstation():null;}
    public static void init(){}
    private CrystalWorkstations(){}
}
