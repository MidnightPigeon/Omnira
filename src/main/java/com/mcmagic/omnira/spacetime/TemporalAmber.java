package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.registry.ModEntityTypes;
import net.minecraft.nbt.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.*;

/** Death storage owns its stacks; discard never spills them. */
@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class TemporalAmber extends Entity {
    private record PendingDeath(TemporalAmber amber, boolean sealDrops, Map<ItemStack,Entry> curioSlots) {}
    private static final Map<Player,PendingDeath> DYING=new IdentityHashMap<>();
    private static final Set<Player> FORCED=Collections.newSetFromMap(new IdentityHashMap<>());
    private record Entry(String group,int slot,boolean cosmetic,ItemStack stack) {}
    private final List<Entry> contents=new ArrayList<>();
    private UUID owner;
    private boolean registered;
    public UUID owner(){return owner;}

    public TemporalAmber(EntityType<? extends TemporalAmber> type,Level level){super(type,level);setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override public boolean isPickable(){return !isRemoved();}
    @Override public boolean isPushable(){return false;}
    @Override public void tick(){
        super.tick();setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if(level() instanceof net.minecraft.server.level.ServerLevel server && owner!=null){
            var directory=AmberDirectory.get(server);
            if(directory.retired(getUUID()) || (registered && !directory.owns(owner,getUUID()))){breakAndSpill();return;}
            if(!registered){registered=true;directory.register(this);}
        }
    }
    @Override public void remove(RemovalReason reason){
        if(!isRemoved() && reason.shouldDestroy() && level() instanceof net.minecraft.server.level.ServerLevel server)
            AmberDirectory.get(server).removed(this);
        super.remove(reason);
    }

    private static TemporalAmber marker(Player player){
        var amber=ModEntityTypes.TEMPORAL_AMBER.get().create(player.level());
        amber.owner=player.getUUID();amber.setPos(player.position());
        return amber;
    }
    public static TemporalAmber capture(Player player){
        var amber=marker(player);
        amber.captureInventory(player);
        return amber;
    }
    private void captureInventory(Player player){
        var inventory=player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++){
            store("inventory",i,false,inventory.getItem(i));inventory.setItem(i,ItemStack.EMPTY);
        }
        // Cursor and the personal crafting grid are also carried possessions, not the open chest.
        store("extra",-1,false,player.containerMenu.getCarried());player.containerMenu.setCarried(ItemStack.EMPTY);
        for(int i=1;i<=4;i++){
            var slot=player.inventoryMenu.getSlot(i);store("extra",-1,false,slot.getItem());slot.set(ItemStack.EMPTY);
        }
    }
    // Track original slots, leaving the stacks and their death rules to Curios.
    private static Map<ItemStack,Entry> curioSlots(Player player){
        Map<ItemStack,Entry> slots=new IdentityHashMap<>();
        CuriosApi.getCuriosInventory(player).ifPresent(inv->new ArrayList<>(inv.getCurios().entrySet()).forEach(pair->{
            for(boolean cosmetic:new boolean[]{false,true}){
                var stacks=cosmetic?pair.getValue().getCosmeticStacks():pair.getValue().getStacks();
                for(int i=0;i<stacks.getSlots();i++){
                    var stack=stacks.getStackInSlot(i);
                    if(!stack.isEmpty())slots.put(stack,new Entry(pair.getKey(),i,cosmetic,stack));
                }
            }
        }));
        return slots;
    }
    public static void stormDamage(Player player){
        FORCED.add(player);
        try{com.mcmagic.omnira.combat.OmniraDamage.trueDamage(player,40,player.damageSources().explosion(null,null));}
        finally{FORCED.remove(player);}
    }
    public static void beginDeath(Player player){
        if(DYING.containsKey(player) || !(FORCED.contains(player)||AnchoringSigilItem.equipped(player)))return;
        boolean keep=player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);
        var amber=marker(player);
        if(!player.level().addFreshEntity(amber))return;
        if(!keep)amber.captureInventory(player);
        DYING.put(player,new PendingDeath(amber,!keep,keep?Map.of():curioSlots(player)));
    }
    public static void finishDeath(Player player){
        var death=DYING.remove(player);if(death==null)return;
        var amber=death.amber();
        if(player.isAlive()){amber.interact(player,InteractionHand.MAIN_HAND);return;}
        amber.registered=true;AmberDirectory.get((net.minecraft.server.level.ServerLevel)player.level()).register(amber);
    }
    @net.neoforged.bus.api.SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public static void captureDeathDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event){
        var death=DYING.get(event.getEntity());if(death==null||!death.sealDrops())return;
        var amber=death.amber();
        for(var drop:event.getDrops()){
            var stack=drop.getItem();
            var origin=death.curioSlots().remove(stack);
            if(origin==null)amber.storeExtra(stack);
            else amber.store(origin.group(),origin.slot(),origin.cosmetic(),stack);
        }
        event.getDrops().clear();event.setCanceled(true);
    }
    public void storeExtra(ItemStack stack){store("extra",-1,false,stack);}
    private void store(String group,int slot,boolean cosmetic,ItemStack stack){
        if(!stack.isEmpty())contents.add(new Entry(group,slot,cosmetic,stack.copy()));
    }
    public int storedStacks(){return contents.size();}

    @Override public InteractionResult interact(Player player,InteractionHand hand){
        if(level().isClientSide)return InteractionResult.SUCCESS;
        if(!player.getUUID().equals(owner))return InteractionResult.PASS;
        if(isRemoved() || !player.isAlive())return InteractionResult.FAIL;
        if(registered && !AmberDirectory.get((net.minecraft.server.level.ServerLevel)level()).owns(owner,getUUID())){
            breakAndSpill();return InteractionResult.FAIL;
        }
        var iterator=contents.iterator();
        while(iterator.hasNext()){
            var entry=iterator.next();var stack=entry.stack();
            if(entry.group().equals("inventory") && entry.slot()>=0 && entry.slot()<player.getInventory().getContainerSize()
                    && player.getInventory().getItem(entry.slot()).isEmpty()){
                player.getInventory().setItem(entry.slot(),stack.copy());iterator.remove();continue;
            }
            if(!entry.group().equals("inventory") && !entry.group().equals("extra")){
                var handler=CuriosApi.getCuriosInventory(player).flatMap(inv->inv.getStacksHandler(entry.group()));
                if(handler.isPresent()){
                    var slots=entry.cosmetic()?handler.get().getCosmeticStacks():handler.get().getStacks();
                    if(entry.slot()>=0 && entry.slot()<slots.getSlots() && slots.getStackInSlot(entry.slot()).isEmpty()){
                        slots.setStackInSlot(entry.slot(),stack.copy());iterator.remove();continue;
                    }
                }
            }
            // Inventory.add mutates the remaining stack; preserve overflow in the amber.
            player.getInventory().add(stack);if(stack.isEmpty())iterator.remove();
        }
        player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();
        if(contents.isEmpty())discard();
        return InteractionResult.CONSUME;
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(level().isClientSide || isRemoved() || amount<=0)return false;
        breakAndSpill();return true;
    }
    public void breakAndSpill(){
        if(level().isClientSide || isRemoved())return;
        var drops=new ArrayList<>(contents);contents.clear();discard();
        for(var entry:drops)spawnAtLocation(entry.stack());
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){
        if(owner!=null)tag.putUUID("Owner",owner);
        tag.putBoolean("Registered",registered);
        var items=new ListTag();
        for(var entry:contents){
            var item=new CompoundTag();item.putString("Group",entry.group());item.putInt("Slot",entry.slot());
            item.putBoolean("Cosmetic",entry.cosmetic());item.put("Stack",entry.stack().save(registryAccess()));items.add(item);
        }
        tag.put("Contents",items);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag){
        owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;contents.clear();
        registered=tag.getBoolean("Registered");
        for(var element:tag.getList("Contents",Tag.TAG_COMPOUND)){
            var item=(CompoundTag)element;
            store(item.getString("Group"),item.getInt("Slot"),item.getBoolean("Cosmetic"),ItemStack.parseOptional(registryAccess(),item.getCompound("Stack")));
        }
    }
}
