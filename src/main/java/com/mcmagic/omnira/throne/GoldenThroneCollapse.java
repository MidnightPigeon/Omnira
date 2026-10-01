package com.mcmagic.omnira.throne;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.ModItems;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid=Omnira.MOD_ID)
public final class GoldenThroneCollapse {
    private static final Set<Pending> PENDING=new HashSet<>();
    private static final ResourceKey<LootTable> LOOT=ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"chests/golden_throne"));
    private record Pending(ServerLevel level,BlockPos pos){}
    private GoldenThroneCollapse(){}

    public static void queue(ServerLevel level,BlockPos pos){PENDING.add(new Pending(level,pos.immutable()));}

    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        for(Pending pending:Set.copyOf(PENDING)){
            PENDING.remove(pending);
            ServerLevel level=pending.level();BlockPos pos=pending.pos();
            if(level.getServer()!=event.getServer() || !level.hasChunkAt(pos))continue;
            if(level.getBlockState(pos.above()).is(ModBlocks.GOLDEN_THRONE.get()))
                level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
            if(level.getBlockState(pos).is(ModBlocks.GOLDEN_THRONE.get()))
                level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            if(!level.isEmptyBlock(pos))continue;
            level.setBlockAndUpdate(pos,ModBlocks.CRYSTAL_BALL.get().defaultBlockState());
            if(level.getBlockEntity(pos) instanceof CrystalBallBlockEntity ball){
                ball.setLootTable(LOOT);
                ball.setLootTableSeed(level.getRandom().nextLong());
                ball.setChanged();
            }
            Block.popResource(level,pos,new ItemStack(ModItems.GOLDEN_TOILET.get()));
        }
    }
}
