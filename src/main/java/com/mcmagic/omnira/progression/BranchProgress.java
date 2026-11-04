package com.mcmagic.omnira.progression;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Successful actions record objectives immediately; AdvancementDependencies gates completion. */
@EventBusSubscriber(modid="omnira")
public final class BranchProgress {
    private static final String OPERATOR="OmniraProgressOperator";
    private static final Set<String> EGGS=Set.of("beyond_the_mist","personal_spaceflight","too_late_scholar","needle_sick_donkey","lure_fishing_for_birds");
    public static void record(Player player,String id){record(player,id,"performed");}
    public static void record(Player player,String id,String criterion){
        if(!(player instanceof ServerPlayer server)||player.isSpectator())return;
        award(server,id,criterion);
    }
    static boolean award(ServerPlayer server,String id,String criterion){
        var advancement=server.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("omnira",id.contains("/")?id:"branches/"+id));
        if(advancement==null||!advancement.value().criteria().containsKey(criterion))return false;
        server.getAdvancements().award(advancement,criterion);return true;
    }
    public static void record(net.minecraft.server.MinecraftServer server,UUID player,String id){
        if(player==null)return;var online=server.getPlayerList().getPlayer(player);
        if(online!=null)record(online,id);else BranchPending.get(server).add(player,id,"performed");
    }
    public static void operate(Player player,BlockEntity machine){
        machine=host(machine);
        if(machine==null||!net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(machine.getBlockState().getBlock()).getNamespace().equals("omnira"))return;
        if(player instanceof ServerPlayer&&machine!=null&&!player.isSpectator()&&(!machine.getPersistentData().hasUUID(OPERATOR)||!machine.getPersistentData().getUUID(OPERATOR).equals(player.getUUID()))){machine.getPersistentData().putUUID(OPERATOR,player.getUUID());machine.setChanged();}
    }
    private static BlockEntity host(BlockEntity machine){
        if(machine==null||machine.getLevel()==null)return machine;
        var actual=machine.getLevel().getBlockEntity(machine.getBlockPos());return actual==null?machine:actual;
    }
    public static ServerPlayer operator(BlockEntity machine){
        machine=host(machine);
        if(machine==null||!(machine.getLevel() instanceof net.minecraft.server.level.ServerLevel level))return null;
        var tag=machine.getPersistentData();return tag.hasUUID(OPERATOR)?level.getServer().getPlayerList().getPlayer(tag.getUUID(OPERATOR)):null;
    }
    public static void machine(BlockEntity machine,String id){
        machine=host(machine);
        if(machine!=null&&machine.getLevel() instanceof net.minecraft.server.level.ServerLevel level&&machine.getPersistentData().hasUUID(OPERATOR))
            record(level.getServer(),machine.getPersistentData().getUUID(OPERATOR),id);
    }
    public static void automated(BlockEntity machine,int amount){if(amount>0)machine(machine,"unseen_hands");}
    public static void processed(BlockEntity machine){
        if(machine!=null)com.mcmagic.omnira.pulse.PulseNetworks.processed(machine.getLevel(),machine.getBlockPos());
    }
    public static void port(BlockEntity machine,int amount){if(amount>0){machine(machine,"adjacent_worlds");automated(machine,amount);}}
    public static void spell(ServerPlayer player,ItemStack grid,int slot,int next){
        if(grid.is(com.mcmagic.omnira.registry.ModItems.OMNI_CRYSTAL_GRID.get()))record(player,"twelvefold");
        var items=grid.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).stream().toList();
        int required=0;var distinct=new ArrayList<ItemStack>();
        for(int i=0;i<items.size();i++)if(!items.get(i).isEmpty()){
            required|=1<<i;var item=items.get(i);
            if(distinct.stream().noneMatch(other->ItemStack.isSameItemSameComponents(item,other)))distinct.add(item);
        }
        var tag=player.getPersistentData();var snapshot=grid.copy();snapshot.remove(com.mcmagic.omnira.registry.ModDataComponents.GRID_CURSOR);
        var signature=snapshot.save(player.registryAccess());
        if(!signature.equals(tag.get("OmniraCycleSignature"))){tag.put("OmniraCycleSignature",signature);tag.putInt("OmniraCycleSlots",0);}
        int used=tag.getInt("OmniraCycleSlots")|1<<slot;
        if(next<=slot){if(distinct.size()>=2&&(used&required)==required)record(player,"spell_cycle");used=0;}
        tag.putInt("OmniraCycleSlots",used);
    }
    private static void eggs(ServerPlayer player){
        var manager=player.server.getAdvancements();
        for(var id:EGGS){var advancement=manager.get(ResourceLocation.fromNamespaceAndPath("omnira",id));
            if(advancement!=null&&player.getAdvancements().getOrStartProgress(advancement).isDone()){
                var root=manager.get(ResourceLocation.fromNamespaceAndPath("omnira","easter_eggs/root"));
                if(root!=null)player.getAdvancements().award(root,"discovered");return;
            }
        }
    }
    @SubscribeEvent public static void earned(net.neoforged.neoforge.event.entity.player.AdvancementEvent.AdvancementEarnEvent event){
        if(event.getEntity() instanceof ServerPlayer player&&event.getAdvancement().id().getNamespace().equals("omnira")&&EGGS.contains(event.getAdvancement().id().getPath()))eggs(player);
    }
    public static void reconcile(ServerPlayer player){eggs(player);BranchPending.get(player.server).deliver(player);AdvancementDependencies.sync(player);}
    @SubscribeEvent public static void login(net.neoforged.neoforge.event.OnDatapackSyncEvent event){event.getRelevantPlayers().forEach(BranchProgress::reconcile);}
    @SubscribeEvent public static void interact(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer))return;
        var level=event.getLevel();var pos=event.getPos();var state=level.getBlockState(pos);
        BlockEntity machine=level.getBlockEntity(pos);
        if(state.getBlock() instanceof com.mcmagic.omnira.alchemy.ReactionPoolBlock)machine=com.mcmagic.omnira.alchemy.ReactionPoolBlock.controller(level,pos);
        if(state.getBlock() instanceof com.mcmagic.omnira.alchemy.catalysis.CatalysisBlock)machine=com.mcmagic.omnira.alchemy.catalysis.CatalysisBlock.controller(level,pos);
        operate(event.getEntity(),machine);
    }
    @SubscribeEvent public static void placed(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent event){
        if(event.getEntity() instanceof ServerPlayer player)operate(player,player.level().getBlockEntity(event.getPos()));
    }
    @SubscribeEvent public static void explore(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player)||player.tickCount%20!=0)return;
        var level=player.serverLevel();var biome=level.getBiome(player.blockPosition()).unwrapKey().map(k->k.location().toString()).orElse("");
        if(biome.equals("omnira:dawn_crystal_fields"))record(player,"dream_duality","light");
        if(biome.equals("omnira:shadow_woodland"))record(player,"dream_duality","dark");
        if(level.dimension().equals(com.mcmagic.omnira.world.dimension.ModDimensions.SPACETIME_CORRIDOR)){
            if(player.getY()>=com.mcmagic.omnira.spacetime.CorridorLayout.TIME_BASE-2)record(player,"spacetime_shores","time");
            else if(player.getY()<=com.mcmagic.omnira.spacetime.CorridorLayout.SPACE_CEILING)record(player,"spacetime_shores","space");
        }
    }
    private BranchProgress(){}
}
