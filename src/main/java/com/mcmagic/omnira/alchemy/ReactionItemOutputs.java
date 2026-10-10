package com.mcmagic.omnira.alchemy;

import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.world.item.ItemStack;

/** One slot per item kind, including every simultaneously delivered product. */
final class ReactionItemOutputs {
    static boolean fit(List<ItemStack> inventory,List<ItemStack> outputs,ToIntFunction<ItemStack> limit){
        var trial=new java.util.ArrayList<>(inventory.stream().map(ItemStack::copy).toList());
        for(var output:outputs){
            int match=-1,empty=-1;
            for(int i=0;i<trial.size();i++){
                var stored=trial.get(i);
                if(ItemStack.isSameItemSameComponents(stored,output)){match=i;break;}
                if(stored.isEmpty()&&empty<0)empty=i;
            }
            int slot=match>=0?match:empty;if(slot<0)return false;
            int count=trial.get(slot).getCount()+output.getCount();if(count>limit.applyAsInt(output))return false;
            trial.set(slot,output.copyWithCount(count));
        }
        return true;
    }
    private ReactionItemOutputs(){}
}
