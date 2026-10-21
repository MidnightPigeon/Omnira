package com.mcmagic.omnira.myriad;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** One controller per frame; recipes are matched on material changes, not every tick. */
public final class MiniatureWorldBlockEntity extends BlockEntity {
    public enum Stage {
        EMPTY, OVERWORLD, RUINS, PORTAL, NETHER_EMPTY, NETHER, FORTRESS, BASTION, RIFT,
        END_EMPTY, END, CHORUS, CITY, VOID, COMPLETE,
        WORKSHOP, DREAM_PORTAL, DREAM_EMPTY, DREAM_LAYERS, SPACETIME_EMPTY, SPACETIME_LAYERS, PROBABILITY_EXPLOSION, PROBABILITY_COMPLETE,
        CODEX_WORLD, CODEX_MATTER, CODEX_FORM, CODEX_TIME, CODEX_LIFE, CODEX_COMPLETE;
        public String key(){return "miniature.omnira.stage."+name().toLowerCase(Locale.ROOT);}
        public int theme(){return switch(this){
            case WORKSHOP,DREAM_PORTAL->0xFFA8E6F2;
            case DREAM_EMPTY,DREAM_LAYERS->0xFFD4B8F5;
            case SPACETIME_EMPTY,SPACETIME_LAYERS,PROBABILITY_EXPLOSION->0xFF8BDCD0;
            case PROBABILITY_COMPLETE->0xFFD1A2F4;
            case CODEX_WORLD,CODEX_LIFE->0xFFA8DB83;
            case CODEX_MATTER,CODEX_FORM->0xFFA3D6EE;
            case CODEX_TIME->0xFFB4DDB0;
            case CODEX_COMPLETE->0xFFF1DFAB;
            default->ordinal()>=RIFT.ordinal()?0xFFC6A4F8:ordinal()>=NETHER_EMPTY.ordinal()?0xFFFF9781:0xFFA8E6F2;
        };}
        public Stage manualNext(){return switch(this){case PORTAL->NETHER_EMPTY;case DREAM_PORTAL->DREAM_EMPTY;default->null;};}
        public boolean complete(){return this==COMPLETE||this==PROBABILITY_COMPLETE||this==CODEX_COMPLETE;}
        public Stage completion(){return this==RIFT?END_EMPTY:this==VOID?COMPLETE:this==PROBABILITY_EXPLOSION?PROBABILITY_COMPLETE:null;}
        public ItemStack product(){return this==CODEX_COMPLETE?new ItemStack(com.mcmagic.omnira.codex.MyriadCodexContent.ITEM.get()):this==PROBABILITY_COMPLETE||this==PROBABILITY_EXPLOSION?new ItemStack(MiniatureWorldContent.PROBABILITY_CLOUD.get()):this==COMPLETE||this==VOID?new ItemStack(com.mcmagic.omnira.registry.ModItems.OMNI_MICROCORE.get()):ItemStack.EMPTY;}
    }
    private final List<ItemStack> materials=new ArrayList<>(9);
    private Stage stage=Stage.EMPTY;
    private Stage previous=Stage.EMPTY;
    private long changedAt;
    private ItemStack descending=ItemStack.EMPTY;
    private final List<ItemStack> offerings=new ArrayList<>();
    private int remaining, duration;
    private boolean needsMatch;
    private boolean lightChecked;
    private Direction hintFace=Direction.NORTH;
    private int hint=-1, hintTicks;
    private net.minecraft.resources.ResourceLocation series=WorldDevelopmentRecipe.MYRIAD;
    private final int[] initialAlternatives=new int[WorldDevelopmentSeries.PANEL_COUNT];
    public final net.neoforged.neoforge.items.IItemHandler inventory=new net.neoforged.neoforge.items.IItemHandler(){
        public int getSlots(){return 9;}
        public ItemStack getStackInSlot(int slot){return stage.complete()&&slot==0?stage.product():material(slot);}
        public int getSlotLimit(int slot){return 64;}
        public boolean isItemValid(int slot,ItemStack stack){return slot>=0&&slot<9&&acceptsMaterial(stack)&&!developing()&&!stage.complete()&&stage.manualNext()==null;}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){
            if(!isItemValid(slot,stack)||slot>materials.size())return stack;
            if(slot<materials.size()&&!ItemStack.isSameItemSameComponents(materials.get(slot),stack))return stack;
            int index=-1;for(int i=0;i<materials.size();i++)if(ItemStack.isSameItemSameComponents(materials.get(i),stack)){index=i;break;}
            if(index<0&&materials.size()==9)return stack;
            int accepted=Math.min(stack.getCount(),stack.getMaxStackSize()-(index<0?0:materials.get(index).getCount()));
            if(accepted<=0)return stack;
            if(!simulate)insert(stack.copyWithCount(accepted));
            return stack.copyWithCount(stack.getCount()-accepted);
        }
        public ItemStack extractItem(int slot,int amount,boolean simulate){
            if(amount<=0||slot<0||slot>=9||developing())return ItemStack.EMPTY;
            if(stage.complete()&&slot==0)return simulate?getStackInSlot(0):takeProduct();
            if(slot>=materials.size())return ItemStack.EMPTY;
            var stored=materials.get(slot);var result=stored.copyWithCount(Math.min(amount,stored.getCount()));
            if(!simulate){stored.shrink(result.getCount());if(stored.isEmpty())materials.remove(slot);changed();}
            return result;
        }
    };
    public MiniatureWorldBlockEntity(BlockPos pos,BlockState state){super(MiniatureWorldContent.ENTITY.get(),pos,state);}
    public Stage stage(){return stage;}
    public Stage previousStage(){return previous;}
    public long changedAt(){return changedAt;}
    public int duration(){return duration;}
    public boolean developing(){return remaining>0;}
    public int remaining(){return remaining;}
    public List<ItemStack> offerings(){return offerings.stream().map(ItemStack::copy).toList();}
    public Direction hintFace(){return hintFace;}
    public int hint(){return hint;}
    public net.minecraft.resources.ResourceLocation series(){return series;}
    public int initialAlternative(int panel){return initialAlternatives[panel];}
    public List<WorldDevelopmentSeries> initialSeries(){
        if(level==null)return List.of();
        return WorldDevelopmentSeries.collect(level.getRecipeManager().getAllRecipesFor(MiniatureWorldContent.TYPE.get())).stream()
                .filter(s->!s.at(Stage.EMPTY).isEmpty()).limit(WorldDevelopmentSeries.PANEL_COUNT).toList();
    }
    public List<WorldDevelopmentRecipe> recipes(){
        if(level==null)return List.of();
        var allowed=stage==Stage.EMPTY?initialSeries().stream().map(WorldDevelopmentSeries::id).toList():List.of(series);
        return level.getRecipeManager().getAllRecipesFor(MiniatureWorldContent.TYPE.get()).stream()
                .filter(h->h.value().from()==stage&&allowed.contains(h.value().series())).sorted(Comparator.comparing(h->h.id().toString()))
                .map(net.minecraft.world.item.crafting.RecipeHolder::value).toList();
    }
    public ItemStack descending(){return descending.copy();}
    public List<ItemStack> materials(){return materials.stream().map(ItemStack::copy).toList();}
    public int size(){return materials.size();}
    public ItemStack material(int index){return index>=0&&index<materials.size()?materials.get(index).copy():ItemStack.EMPTY;}
    private static boolean acceptsMaterial(ItemStack stack){
        return !stack.isEmpty()&&!com.mcmagic.omnira.item.bottle.PocketBottleItem.restricted(stack)
                &&!(stack.getItem() instanceof MiniatureWorldBlock.PlacementItem);
    }
    public int insert(ItemStack offered){
        if(!acceptsMaterial(offered)||developing()||stage.complete()||stage.manualNext()!=null)return 0;
        int index=-1;
        for(int i=0;i<materials.size();i++)if(ItemStack.isSameItemSameComponents(materials.get(i),offered)){index=i;break;}
        if(index<0&&materials.size()==9)return 0;
        int present=index<0?0:materials.get(index).getCount();
        int accepted=Math.min(offered.getCount(),offered.getMaxStackSize()-present);
        if(accepted<=0)return 0;
        if(index>=0)materials.remove(index);
        materials.add(offered.copyWithCount(present+accepted));hint=-1;needsMatch=true;changed();return accepted;
    }
    public ItemStack take(){
        if(materials.isEmpty()||developing())return ItemStack.EMPTY;
        var result=materials.remove(materials.size()-1);changed();return result;
    }
    public ItemStack takeProduct(){
        if(!stage.complete()||developing())return ItemStack.EMPTY;
        var product=stage.product();
        stage=previous=Stage.EMPTY;series=WorldDevelopmentRecipe.MYRIAD;Arrays.fill(initialAlternatives,0);descending=ItemStack.EMPTY;materials.clear();offerings.clear();remaining=duration=0;hint=-1;changed();
        return product;
    }
    public boolean enterDimension(){
        if(developing()||stage.manualNext()==null)return false;
        materials.clear();offerings.clear();begin(stage.manualNext(),60);return true;
    }
    public void query(Direction face){
        query(face,-1);
    }
    public void query(Direction face,int panel){
        if(face.getAxis().isVertical()||!materials.isEmpty()||developing())return;
        if(stage==Stage.EMPTY){
            if(hint<0){hint=0;hintFace=face;Arrays.fill(initialAlternatives,0);}
            else if(hintFace==face&&panel>=0&&panel<initialSeries().size()){
                int count=initialSeries().get(panel).at(Stage.EMPTY).size();initialAlternatives[panel]=(initialAlternatives[panel]+1)%count;
            }else hint=-1;
            changed();return;
        }
        int count=recipes().size();if(count==0)return;
        hint=hint>=0&&hintFace==face?hint+1:0;hintFace=face;
        if(hint>=count)hint=-1;hintTicks=300;changed();
    }
    public static void tick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,MiniatureWorldBlockEntity be){
        if(level.isClientSide)return;
        // Old saves may be waiting at the former manual End crossing.
        if(be.stage==Stage.RIFT&&be.remaining==0){be.previous=be.stage;be.stage=Stage.END_EMPTY;be.needsMatch=true;be.changed();}
        be.refreshLight();
        if(be.remaining>0){
            be.remaining--;be.setChanged();
            if(be.remaining==0){
                be.offerings.clear();be.descending=ItemStack.EMPTY;
                if(be.stage.completion()!=null){be.previous=be.stage;be.stage=be.stage.completion();}
                be.needsMatch=true;be.changed();
            }
        }else if(be.needsMatch){
            be.needsMatch=false;
            for(var recipe:be.recipes()){
                var use=recipe.allocation(be.materials);if(use==null)continue;
                be.series=recipe.series();
                be.offerings.clear();
                for(int i=0;i<use.length;i++)if(use[i]>0){be.offerings.add(be.materials.get(i).copyWithCount(use[i]));be.materials.get(i).shrink(use[i]);}
                be.materials.removeIf(ItemStack::isEmpty);
                // Rift creation leaves the Nether; ordinary scene changes retain unused stacks.
                if(recipe.to()==Stage.RIFT||recipe.to()==Stage.VOID||recipe.to()==Stage.SPACETIME_EMPTY||recipe.to()==Stage.PROBABILITY_EXPLOSION){be.offerings.addAll(be.materials);be.materials.clear();}
                be.descending=be.offerings.isEmpty()?ItemStack.EMPTY:be.offerings.getFirst().copy();
                be.begin(recipe.to(),recipe.duration());break;
            }
        }
        if(be.stage!=Stage.EMPTY&&be.hint>=0&&--be.hintTicks<=0){if(++be.hint>=be.recipes().size())be.hint=-1;be.hintTicks=300;be.changed();}
    }
    private void begin(Stage next,int ticks){remaining=duration=ticks;hint=-1;changeStage(next);}
    public void preview(Stage next){remaining=duration=0;offerings.clear();descending=ItemStack.EMPTY;hint=-1;needsMatch=true;changeStage(next);}
    private void changeStage(Stage next){previous=stage;stage=next;changedAt=level==null?0:level.getGameTime();changed();}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public ItemStack packed(){
        var result=new ItemStack(MiniatureWorldContent.ITEM.get());
        var tag=saveWithFullMetadata(level.registryAccess());tag.remove("x");tag.remove("y");tag.remove("z");
        result.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(tag));return result;
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);var list=new ListTag();
        for(var item:materials)list.add(item.save(registries));tag.put("Materials",list);
        tag.putString("Stage",stage.name());tag.putString("PreviousStage",previous.name());tag.putLong("ChangedAt",changedAt);
        tag.putInt("Remaining",remaining);tag.putInt("Duration",duration);
        var consumed=new ListTag();for(var item:offerings)consumed.add(item.save(registries));tag.put("Offerings",consumed);
        tag.putInt("Hint",hint);tag.putInt("HintTicks",hintTicks);tag.putInt("HintFace",hintFace.get3DDataValue());
        tag.putString("Series",series.toString());tag.putIntArray("InitialAlternatives",initialAlternatives);
        if(!descending.isEmpty())tag.put("Descending",descending.save(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);materials.clear();
        for(var entry:tag.getList("Materials",Tag.TAG_COMPOUND)){
            if(materials.size()==9)break;
            var stack=ItemStack.parseOptional(registries,(CompoundTag)entry);
            if(!stack.isEmpty())materials.add(stack.copyWithCount(Math.min(stack.getCount(),stack.getMaxStackSize())));
        }
        stage=readStage(tag.getString("Stage"));previous=readStage(tag.getString("PreviousStage"));changedAt=tag.getLong("ChangedAt");
        series=tag.getString("Series").isEmpty()?WorldDevelopmentRecipe.MYRIAD:net.minecraft.resources.ResourceLocation.tryParse(tag.getString("Series"));if(series==null)series=WorldDevelopmentRecipe.MYRIAD;
        Arrays.fill(initialAlternatives,0);var alternatives=tag.getIntArray("InitialAlternatives");for(int i=0;i<Math.min(alternatives.length,initialAlternatives.length);i++)initialAlternatives[i]=Math.max(0,alternatives[i]);
        descending=tag.contains("Descending")?ItemStack.parseOptional(registries,tag.getCompound("Descending")):ItemStack.EMPTY;
        duration=Math.clamp(tag.getInt("Duration"),0,1200);remaining=Math.clamp(tag.getInt("Remaining"),0,duration);
        offerings.clear();for(var entry:tag.getList("Offerings",Tag.TAG_COMPOUND))if(offerings.size()<18){var s=ItemStack.parseOptional(registries,(CompoundTag)entry);if(!s.isEmpty())offerings.add(s);}
        hint=tag.contains("Hint")?tag.getInt("Hint"):-1;hintTicks=Math.clamp(tag.getInt("HintTicks"),0,300);hintFace=Direction.from3DDataValue(tag.getInt("HintFace"));needsMatch=true;
    }
    public void resumeClock(){if(level!=null)changedAt=level.getGameTime()-(duration-remaining);}
    @Override public void onLoad(){super.onLoad();resumeClock();lightChecked=false;}
    private void refreshLight(){
        if(lightChecked||level==null)return;
        int side=MiniatureWorldBlock.size(getBlockState());
        for(int x=worldPosition.getX()>>4;x<=(worldPosition.getX()+side-1)>>4;x++)
            for(int z=worldPosition.getZ()>>4;z<=(worldPosition.getZ()+side-1)>>4;z++)
                if(!level.hasChunkAt(new BlockPos(x*16,worldPosition.getY(),z*16)))return;
        // Old saved light arrays predate this block's emission; refresh once after loading.
        for(int x=0;x<side;x++)for(int y=0;y<side;y++)for(int z=0;z<side;z++){
            if(x>0&&y>0&&z>0&&x<side-1&&y<side-1&&z<side-1)continue;
            var at=worldPosition.offset(x,y,z);
            if(level.getBlockState(at).is(MiniatureWorldContent.BLOCK.get()))level.getChunkSource().getLightEngine().checkBlock(at);
        }
        lightChecked=true;
    }
    private static Stage readStage(String value){try{return Stage.valueOf(value);}catch(IllegalArgumentException ex){return Stage.EMPTY;}}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
