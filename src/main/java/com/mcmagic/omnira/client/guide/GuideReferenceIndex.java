package com.mcmagic.omnira.client.guide;

import com.mcmagic.omnira.registry.ModItems;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Quick references use authored item entries, never decorative icons or inferred chapters. */
public final class GuideReferenceIndex {
    private final Map<ResourceLocation,String> items=new HashMap<>();
    public GuideReferenceIndex(GuideContents contents){contents.chapters().forEach(this::index);}
    private void index(GuideContents.Chapter chapter){
        for(var row:chapter.directory()){
            if(row.chapter()!=null)index(row.chapter());
            else {
                var entry=row.entry();var id=ResourceLocation.tryParse(entry.item());
                if(id!=null&&!id.equals(ResourceLocation.withDefaultNamespace("air")))items.putIfAbsent(id,entry.id());
                for(String related:entry.relatedItems()){
                    var key=ResourceLocation.tryParse(related);
                    if(key!=null&&!key.equals(ResourceLocation.withDefaultNamespace("air")))items.putIfAbsent(key,entry.id());
                }
            }
        }
    }
    public Optional<String> item(ItemStack stack){return stack.isEmpty()?Optional.empty():Optional.ofNullable(items.get(BuiltInRegistries.ITEM.getKey(stack.getItem())));}
    public Optional<String> target(Level level,Player player,HitResult hit){
        if(hit instanceof BlockHitResult block&&hit.getType()==HitResult.Type.BLOCK){
            var pos=block.getBlockPos();if(!level.hasChunkAt(pos))return Optional.empty();
            var state=level.getBlockState(pos);
            var direct=item(new ItemStack(state.getBlock()));
            return direct.isPresent()?direct:item(state.getCloneItemStack(hit,level,pos,player));
        }
        if(hit instanceof EntityHitResult entity)return entity(entity.getEntity());
        return Optional.empty();
    }
    public Optional<String> entity(Entity entity){
        if(entity instanceof ItemEntity dropped)return item(dropped.getItem());
        var egg=SpawnEggItem.byId(entity.getType());
        var introduction=egg==null?Optional.<String>empty():item(new ItemStack(egg));
        if(introduction.isPresent())return introduction;
        var direct=Optional.ofNullable(items.get(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())));
        if(direct.isPresent())return direct;
        if(entity instanceof com.mcmagic.omnira.archaeology.MovingDrill)return item(new ItemStack(com.mcmagic.omnira.archaeology.ArchaeologyContent.DRILL_ITEM.get()));
        var picked=entity.getPickResult();
        if(picked!=null){var found=item(picked);if(found.isPresent())return found;}
        return Optional.empty();
    }
    public static boolean carriesBook(Player player){
        for(int i=0;i<36;i++)if(player.getInventory().getItem(i).is(ModItems.GUIDE_BOOK.get()))return true;
        return false;
    }
}
