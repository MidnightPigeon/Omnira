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
    private static final Map<Player,TemporalAmber> DYING=new IdentityHashMap<>();
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

    public static TemporalAmber capture(Player player){
        var amber=ModEntityTypes.TEMPORAL_AMBER.get().create(player.level());
        amber.owner=player.getUUID();amber.setPos(player.position());
        var inventory=player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++){
            amber.store("inventory",i,false,inventory.getItem(i));inventory.setItem(i,ItemStack.EMPTY);
        }
        // Cursor and the personal crafting grid are also carried possessions, not the open chest.
        amber.store("extra",-1,false,player.containerMenu.getCarried());player.containerMenu.setCarried(ItemStack.EMPTY);
        for(int i=1;i<=4;i++){
            var slot=player.inventoryMenu.getSlot(i);amber.store("extra",-1,false,slot.getItem());slot.set(ItemStack.EMPTY);
        }
        CuriosApi.getCuriosInventory(player).ifPresent(inv->new ArrayList<>(inv.getCurios().entrySet()).forEach(pair->{
            for(boolean cosmetic:new boolean[]{false,true}){
                var stacks=cosmetic?pair.getValue().getCosmeticStacks():pair.getValue().getStacks();
                for(int i=0;i<stacks.getSlots();i++){
                    amber.store(pair.getKey(),i,cosmetic,stacks.getStackInSlot(i));stacks.setStackInSlot(i,ItemStack.EMPTY);
                }
            }
        }));
        return amber;
    }
    public static void sealDeath(Player player){
        FORCED.add(player);
        try{player.setHealth(0);player.die(player.level().damageSources().genericKill());}
        finally{FORCED.remove(player);}
    }
    public static void beginDeath(Player player){
        if(!DYING.containsKey(player) && (FORCED.contains(player)||AnchoringSigilItem.equipped(player)))DYING.put(player,capture(player));
    }
    public static void finishDeath(Player player){
        var amber=DYING.remove(player);if(amber==null)return;
        if(player.isAlive()){amber.interact(player,InteractionHand.MAIN_HAND);return;}
        if(player.level().addFreshEntity(amber)){
            amber.registered=true;AmberDirectory.get((net.minecraft.server.level.ServerLevel)player.level()).register(amber);
        }
    }
    @net.neoforged.bus.api.SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST,receiveCanceled=true)
    public static void captureDeathDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event){
        var amber=DYING.get(event.getEntity());if(amber==null)return;
        for(var drop:event.getDrops())amber.storeExtra(drop.getItem());
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
