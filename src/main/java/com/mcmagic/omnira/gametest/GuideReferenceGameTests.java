package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.client.guide.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.List;

@GameTestHolder("omnira_guide_reference") @PrefixGameTestTemplate(false)
public final class GuideReferenceGameTests {
    @GameTest(template="spell_arena") public static void sharedEntriesExposeAllAuthoredItems(GameTestHelper h)throws Exception{
        for(var language:List.of("zh_cn","en_us")){
            var book=GuideContents.fromJson(GuidanceGameTests.guide(language));var index=new GuideReferenceIndex(book);
            for(var id:List.of("tyrannosaurus_essence","mosasaurus_essence","pterosaur_essence")){
                var key=net.minecraft.resources.ResourceLocation.parse("omnira:"+id);
                var stack=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key));
                h.assertTrue(index.item(stack).orElse("").equals("ancient_essences_guide"),"Essence shortcut missing: "+id);
                h.assertTrue(book.itemDestination(key.toString()).orElseThrow().entry().id().equals("ancient_essences_guide"),"Recipe link differs from shortcut");
            }
            h.assertTrue(book.itemDestination("omnira:repaired_mechanical_component").isPresent(),"Repaired part has no page");
            h.assertTrue(book.destination("ancient_essences_guide").isPresent(),"ID navigation broken by related items");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void holdRequiresContinuousTargetAndRelease(GameTestHelper h){
        var hold=new GuideReferenceHold();
        for(int i=1;i<20;i++)h.assertTrue(!hold.tick("item-a",true)&&hold.percent("item-a")==i*5,"Hold triggered early");
        h.assertTrue(hold.tick("item-a",true),"One-second hold failed");
        for(int i=0;i<40;i++)h.assertTrue(!hold.tick("item-b",true),"Held key opened a second book");
        hold.tick(null,false);
        for(int i=0;i<12;i++)hold.tick("item-a",true);
        hold.tick(null,true);
        for(int i=0;i<19;i++)h.assertTrue(!hold.tick("item-a",true),"Hover interruption retained charge");
        h.assertTrue(!hold.tick("item-b",true)&&hold.percent("item-b")==5,"New target retained old charge");
        hold.tick(null,false);h.assertTrue(hold.percent("item-b")==0,"Release retained progress");h.succeed();
    }
    @GameTest(template="spell_arena") public static void bookMustBeInOrdinaryInventory(GameTestHelper h){
        var player=BranchAdvancementGameTests.player(h);
        try{
            player.getInventory().clearContent();player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.GUIDE_BOOK.get()));
            h.assertTrue(!GuideReferenceIndex.carriesBook(player),"Offhand counted as inventory");
            var chest=new ItemStack(Items.CHEST);chest.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(ModItems.GUIDE_BOOK.get()))));
            player.getInventory().setItem(0,chest);h.assertTrue(!GuideReferenceIndex.carriesBook(player),"Nested container scanned");
            for(int slot:new int[]{0,8,9,35}){
                player.getInventory().clearContent();player.getInventory().setItem(slot,new ItemStack(ModItems.GUIDE_BOOK.get()));
                h.assertTrue(GuideReferenceIndex.carriesBook(player),"Ordinary slot ignored: "+slot);
                h.assertTrue(player.getInventory().getItem(slot).get(DataComponents.CUSTOM_DATA)==null,"Reference scan modified the book");
            }
        }finally{player.discard();}h.succeed();
    }
    @GameTest(template="spell_arena") public static void authoredItemsUseSameDestinationInBothLanguages(GameTestHelper h)throws Exception{
        for(var language:List.of("zh_cn","en_us")){
            var book=GuideContents.fromJson(GuidanceGameTests.guide(language));var index=new GuideReferenceIndex(book);
            for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM){
                var stack=new ItemStack(item);var expected=book.itemDestination(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString()).map(d->d.entry().id());
                h.assertTrue(index.item(stack).equals(stack.isEmpty()?java.util.Optional.empty():expected),"Quick index differs from guide item links: "+item);
            }
            h.assertTrue(index.item(ItemStack.EMPTY).isEmpty(),"Empty item resolved");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void worldMachinesCreaturesVehiclesAndUnlistedTargets(GameTestHelper h)throws Exception{
        var player=BranchAdvancementGameTests.player(h);
        try{
            for(var language:List.of("zh_cn","en_us")){
                var book=GuideContents.fromJson(GuidanceGameTests.guide(language));var index=new GuideReferenceIndex(book);
                var pos=new BlockPos(3,3,3);h.setBlock(pos,ModBlocks.REACTION_POOL.get());var at=h.absolutePos(pos);
                var hit=new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false);
                h.assertTrue(index.target(h.getLevel(),player,hit).equals(index.item(new ItemStack(ModItems.REACTION_POOL.get()))),"Machine part did not resolve its entry");
                var mirror=ModEntityTypes.DREAM_MIRROR.get().create(h.getLevel());
                h.assertTrue(index.entity(mirror).orElseThrow().equals("dream_mirror_spawn_egg"),"Creature did not resolve its introduction");mirror.discard();
                var orb=ModEntityTypes.CRUISE_ORB.get().create(h.getLevel());
                h.assertTrue(index.entity(orb).orElseThrow().equals("cruise_orb"),"Vehicle did not resolve");orb.discard();
                var boss=ModEntityTypes.WEAVER_SPIRIT.get().create(h.getLevel());
                h.assertTrue(index.entity(boss).isEmpty(),"Unlisted boss acquired a guide entry");boss.discard();
                var dropped=new ItemEntity(h.getLevel(),0,0,0,new ItemStack(ModItems.GUIDE_BOOK.get()));
                h.assertTrue(index.entity(dropped).orElseThrow().equals("guide_book"),"Dropped item did not resolve");dropped.discard();
                h.assertTrue(index.target(h.getLevel(),player,BlockHitResult.miss(Vec3.ZERO,Direction.UP,BlockPos.ZERO)).isEmpty(),"Air resolved to an entry");
            }
        }finally{player.discard();}h.succeed();
    }
    @GameTest(template="spell_arena") public static void internalEntitiesCannotBeSummonedButStillConstruct(GameTestHelper h){
        var waste=com.mcmagic.omnira.alchemy.AlchemyMaterials.THROWN_WASTE.get();var seat=com.mcmagic.omnira.archaeology.ArchaeologyContent.SEAT.get();
        h.assertTrue(!waste.canSummon()&&!seat.canSummon()&&!seat.canSerialize(),"Internal entity exposed to summon or seat saving");
        var projectile=waste.create(h.getLevel());var rider=seat.create(h.getLevel());
        h.assertTrue(projectile!=null&&rider!=null&&!rider.isPickable(),"Legitimate internal entity functionality removed");
        projectile.discard();rider.discard();h.succeed();
    }
}
