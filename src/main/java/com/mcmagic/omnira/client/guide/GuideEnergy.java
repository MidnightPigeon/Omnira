package com.mcmagic.omnira.client.guide;

import com.mcmagic.omnira.reversal.ReversalFuels;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class GuideEnergy {
    public static boolean supports(String entry){return entry.equals("temporal_energy")||entry.equals("spatial_energy");}
    public static List<GuideRecipes.Diagram> pages(String entry){
        boolean spatial=entry.equals("spatial_energy");var slots=new ArrayList<GuideRecipes.Slot>();
        var fuels=ReversalFuels.all(spatial);
        for(int i=0;i<fuels.size();i++)slots.add(new GuideRecipes.Slot(26+i%3*34,64+i/3*28,List.of(fuels.get(i).stack()),false));
        return List.of(new GuideRecipes.Diagram(Component.translatable("jei.omnira."+entry),slots,List.of()));
    }
    public static Component tooltip(String entry,ItemStack stack){return Component.translatable("jei.omnira.energy_points",ReversalFuels.value(stack,entry.equals("spatial_energy")));}
    private GuideEnergy(){}
}
