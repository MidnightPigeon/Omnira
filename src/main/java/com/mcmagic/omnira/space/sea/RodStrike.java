package com.mcmagic.omnira.space.sea;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Read-only weapon context for a vanilla player attack made with the offhand rod. */
public final class RodStrike {
    public interface Wielder { void omnira$offhandStrike(Entity target); }
    private final Map<Holder<Attribute>,AttributeInstance> attributes=new HashMap<>();

    public RodStrike(Player player,ItemStack main,ItemStack rod){
        remove(player,main,EquipmentSlot.MAINHAND);
        remove(player,rod,EquipmentSlot.OFFHAND);
        rod.forEachModifier(EquipmentSlot.MAINHAND,(key,modifier)->{
            var stat=copy(player,key);
            if(stat!=null){stat.removeModifier(modifier.id());stat.addTransientModifier(modifier);}
        });
    }
    private void remove(Player player,ItemStack stack,EquipmentSlot slot){
        stack.forEachModifier(slot,(key,modifier)->{
            var stat=copy(player,key);
            if(stat!=null)stat.removeModifier(modifier.id());
        });
    }
    private AttributeInstance copy(Player player,Holder<Attribute> key){
        return attributes.computeIfAbsent(key,k->{
            var current=player.getAttribute(k);
            if(current==null)return null;
            var stat=new AttributeInstance(k,ignored->{});
            stat.replaceFrom(current);return stat;
        });
    }
    public AttributeInstance attribute(Holder<Attribute> key){return attributes.get(key);}
}
