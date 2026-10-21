package com.mcmagic.omnira.progression;

import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** A bounded receipt per storage slot; withdrawals consume receipts, including at the original end. */
public final class BranchStorage {
    private static final ThreadLocal<Integer> MANUAL=ThreadLocal.withInitial(()->0);
    public static boolean manual(){return MANUAL.get()>0;}
    public static void manually(Runnable action){int old=MANUAL.get();MANUAL.set(old+1);try{action.run();}finally{MANUAL.set(old);}}
    public static void items(CrystalBallBlockEntity ball,int slot,ItemStack before,ItemStack after){
        if(ball.virtualStorage||!(ball.getLevel() instanceof ServerLevel level)||!ball.resonant()||ball.storage()==ball)return;
        if(!before.isEmpty()&&(!ItemStack.isSameItemSameComponents(before,after)||after.getCount()<before.getCount()))
            transfer(ball,slot,before.copyWithCount(1).save(level.registryAccess()),-(ItemStack.isSameItemSameComponents(before,after)?before.getCount()-after.getCount():before.getCount()));
        if(!after.isEmpty()&&(!ItemStack.isSameItemSameComponents(before,after)||after.getCount()>before.getCount()))
            transfer(ball,slot,after.copyWithCount(1).save(level.registryAccess()),ItemStack.isSameItemSameComponents(before,after)?after.getCount()-before.getCount():after.getCount());
    }
    public static void fluid(CrystalBallBlockEntity ball,net.neoforged.neoforge.fluids.FluidStack sample,int amount){
        if(!sample.isEmpty()&&ball.getLevel() instanceof ServerLevel level)
            transfer(ball,12,sample.copyWithAmount(1).save(level.registryAccess()),amount);
    }
    private static void transfer(CrystalBallBlockEntity ball,int slot,net.minecraft.nbt.Tag sample,int delta){
        if(delta==0||ball.virtualStorage||!(ball.getLevel() instanceof ServerLevel level)||!ball.resonant()||ball.storage()==ball)return;
        var storage=ball.storage();String key="OmniraTransferReceipt"+slot;var receipt=storage.getPersistentData().getCompound(key);
        String dimension=level.dimension().location().toString();
        if(delta>0){
            if(!sample.equals(receipt.get("Sample")))receipt=new CompoundTag();
            receipt.put("Sample",sample);append(receipt,ball.storageIdentity,dimension,delta);
            storage.getPersistentData().put(key,receipt);
        }else if(sample.equals(receipt.get("Sample"))){
            var entries=receipt.getList("Sources",10);int remaining=-delta;
            for(int i=0;i<entries.size()&&remaining>0;){
                var source=entries.getCompound(i);int taken=Math.min(remaining,source.getInt("Amount"));remaining-=taken;
                if(taken>0&&source.hasUUID("Endpoint")&&!source.getUUID("Endpoint").equals(ball.storageIdentity)){
                    BranchProgress.machine(ball,"distant_echo");
                    if(!dimension.equals(source.getString("Dimension"))&&ball.stabilized())BranchProgress.machine(ball,"across_dimensions");
                }
                int rest=source.getInt("Amount")-taken;if(rest<=0)entries.remove(i);else{source.putInt("Amount",rest);i++;}
            }
        }
        storage.setChanged();
    }
    private static void append(CompoundTag receipt,java.util.UUID endpoint,String dimension,int amount){
        var entries=receipt.getList("Sources",10);CompoundTag source=null;
        for(var value:entries){var candidate=(CompoundTag)value;if(candidate.hasUUID("Endpoint")&&candidate.getUUID("Endpoint").equals(endpoint)&&candidate.getString("Dimension").equals(dimension)){source=candidate;break;}}
        if(source==null){source=new CompoundTag();source.putUUID("Endpoint",endpoint);source.putString("Dimension",dimension);entries.add(source);}
        source.putInt("Amount",(int)Math.min(Integer.MAX_VALUE,(long)source.getInt("Amount")+amount));receipt.put("Sources",entries);
    }
    /** Copy a slice of provenance to a trial merge, never mutate the original stores on failed admission. */
    public static void merge(CrystalBallBlockEntity from,int sourceSlot,CrystalBallBlockEntity to,int targetSlot,int offset,int amount){
        var receipt=from.getPersistentData().getCompound("OmniraTransferReceipt"+sourceSlot);if(!receipt.contains("Sample"))return;
        String key="OmniraTransferReceipt"+targetSlot;var result=to.getPersistentData().getCompound(key);
        if(!receipt.get("Sample").equals(result.get("Sample")))result=new CompoundTag();result.put("Sample",receipt.get("Sample").copy());
        for(var value:receipt.getList("Sources",10)){
            var source=(CompoundTag)value;int count=source.getInt("Amount");int skipped=Math.min(offset,count);offset-=skipped;count-=skipped;
            int copied=Math.min(count,amount);if(copied>0&&source.hasUUID("Endpoint")){append(result,source.getUUID("Endpoint"),source.getString("Dimension"),copied);amount-=copied;}
            if(amount<=0)break;
        }to.getPersistentData().put(key,result);
    }
    private BranchStorage(){}
}
