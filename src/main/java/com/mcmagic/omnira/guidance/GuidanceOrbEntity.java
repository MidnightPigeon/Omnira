package com.mcmagic.omnira.guidance;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Orb-owned progression; interaction views never consume the offered item. */
public final class GuidanceOrbEntity extends BlockEntity {
    public static final int IDLE=0,BUTTONS=1,HINT=2;
    private int view;
    public GuidanceOrbEntity(BlockPos p,BlockState s){super(GuidanceContent.ENTITY.get(),p,s);}
    public int stage(){return getBlockState().getValue(GuidanceOrbBlock.STAGE);}
    public int view(){return view;}
    public boolean buttonsVisible(){return view==BUTTONS&&stage()>0&&stage()<5;}
    public String topLabelKey(){return stage()==4?"guidance.omnira.deconstruct":"guidance.omnira.deeper";}
    public static boolean advances(int stage,ItemStack stack){
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return switch(stage){
            case 0->id.equals("omnira:spiritual_crystal");
            case 1->id.equals("omnira:dream_crystal_bedrock");
            case 2->id.equals("omnira:time_warp_point")||id.equals("omnira:space_warp_point");
            case 3->id.equals("omnira:genesis_particles");
            case 4->id.equals("omnira:myriad_codex");
            default->false;
        };
    }
    public String interact(ItemStack stack,Direction face){
        if(level==null||level.isClientSide)return "";
        if(stage()==5)return "";
        if(view==HINT&&advances(stage(),stack)){
            if(stage()==4){
                level.setBlock(worldPosition,GuidanceContent.UNDERSTANDING.get().defaultBlockState()
                        .setValue(GuidanceOrbBlock.FACING,getBlockState().getValue(GuidanceOrbBlock.FACING)),3);
                return "";
            }
            level.setBlock(worldPosition,getBlockState().setValue(GuidanceOrbBlock.STAGE,stage()+1),3);
            view=IDLE;sync();return "";
        }
        if(view==HINT){
            view=IDLE;sync();return "";
        }
        if(stage()==0){view=HINT;sync();return "";}
        if(view==IDLE){view=BUTTONS;sync();return "";}
        if(face==Direction.UP){
            view=HINT;sync();
            return stage()==4?"myriad_codex#recipe":stage()==1?"visit_dream":stage()==3?"genesis_ritual":"";
        }
        return target(face);
    }
    public String target(Direction face){
        if(!buttonsVisible()||face.getAxis()==Direction.Axis.Y)return "";
        Direction front=getBlockState().getValue(GuidanceOrbBlock.FACING);
        int side=face==front?0:face==front.getClockWise()?1:face==front.getOpposite()?2:3;
        return switch(stage()){
            case 1->new String[]{"initial_spells","initial_alchemy","initial_industry","simple_storage"}[side];
            case 2->side==3?"pocket_magic_bottle":side==1?"spacetime_entry":"";
            case 3->new String[]{"spacetime_terrain","advanced_industry","dream_alchemy","advanced_spells"}[side];
            case 4->new String[]{"world_forming_ritual","myriad_aggregation","world_generation_probability_cloud","omni_crystal_grid"}[side];
            default->"";
        };
    }
    public String messageKey(){
        if(view==IDLE)return "";
        return switch(stage()){
            case 0->"guidance.omnira.dormant";
            case 1->view==HINT?"guidance.omnira.dream_hint":"";
            case 2->view==HINT?"guidance.omnira.spacetime_hint":"guidance.omnira.transition";
            case 3->view==HINT?"guidance.omnira.genesis_hint":"";
            case 4->view==HINT?"guidance.omnira.codex_hint":"";
            default->"";
        };
    }
    public String[] projections(){
        if(view==IDLE)return new String[0];
        if(stage()==0)return new String[]{"simple_condensation_table","spiritual_crystal"};
        if(view==HINT&&stage()==1)return new String[]{"dream_crystal_bedrock"};
        if(view==HINT&&stage()==2)return new String[]{"time_warp_point","space_warp_point"};
        if(view==HINT&&stage()==3)return new String[]{"genesis_particles"};
        if(view==HINT&&stage()==4)return new String[]{"myriad_codex"};
        return new String[0];
    }
    public static ItemStack projection(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("omnira",id)));}
    private void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.saveAdditional(tag,lookup);tag.putInt("View",view);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.loadAdditional(tag,lookup);view=stage()==5?IDLE:Math.clamp(tag.getInt("View"),IDLE,HINT);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
