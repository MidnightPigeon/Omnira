package com.mcmagic.omnira.mire;

import com.mcmagic.omnira.registry.*;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** One extraction per player, state and overworld day; six independent category rolls per opened cube. */
public final class MemoryCubeRewards {
    private static final String PEACEFUL_DAY="OmniraPeacefulMemoryDay",CORRUPTED_DAY="OmniraCorruptedMemoryDay";
    private MemoryCubeRewards(){}
    public static boolean extract(ServerLevel level,Player player,boolean peaceful){
        var root=player.getPersistentData();var data=root.getCompound(Player.PERSISTED_NBT_TAG);
        String key=peaceful?PEACEFUL_DAY:CORRUPTED_DAY;
        long day=level.getServer().overworld().getDayTime()/24000;
        if(data.contains(key)&&data.getLong(key)==day){
            player.displayClientMessage(Component.translatable("message.omnira.memory_cube.daily_limit"),true);return false;
        }
        data.putLong(key,day);root.put(Player.PERSISTED_NBT_TAG,data);
        var item=new ItemStack((peaceful?ModItems.PEACEFUL_MEMORY:ModItems.CORRUPTED_MEMORY).get());
        if(!player.getInventory().add(item))player.drop(item,false);
        player.displayClientMessage(Component.translatable("message.omnira.memory_cube.extracted"),true);
        return true;
    }
    public static List<ItemStack> roll(ServerLevel level,net.minecraft.world.phys.Vec3 origin,RandomSource random,boolean peaceful){
        var output=new ArrayList<ItemStack>(6);
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,origin)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        for(int i=0;i<6;i++){
            String category=peaceful?(random.nextBoolean()?"time":"light"):(random.nextBoolean()?"space":"shadow");
            var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    net.minecraft.resources.ResourceLocation.parse("omnira:memory/"+category));
            output.addAll(level.getServer().reloadableRegistries().getLootTable(key).getRandomItems(params,random.nextLong()));
        }
        return List.copyOf(output);
    }
    public static void copyDailyLimits(Player old,Player next){
        var source=old.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        var target=next.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        for(String key:List.of(PEACEFUL_DAY,CORRUPTED_DAY))if(source.contains(key))target.putLong(key,source.getLong(key));
        next.getPersistentData().put(Player.PERSISTED_NBT_TAG,target);
    }
}
