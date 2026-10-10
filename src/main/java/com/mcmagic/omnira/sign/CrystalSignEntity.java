package com.mcmagic.omnira.sign;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class CrystalSignEntity extends SignBlockEntity {
    private final List<String> remaining=new ArrayList<>();
    private String poolSignature="";
    private String lastStatement="";
    public CrystalSignEntity(BlockPos pos,BlockState state){super(ModBlockEntityTypes.CRYSTAL_SIGN.get(),pos,state);}
    public boolean wisdom(){return CrystalSigns.wisdom(getBlockState().getBlock());}
    @Override public int getMaxTextLineWidth(){return 120;}
    @Override protected SignText createDefaultSignText(){return new SignText().setColor(DyeColor.BLACK);}
    @Override public void setAllowedPlayerEditor(UUID player){if(!wisdom())super.setAllowedPlayerEditor(player);}
    @Override public void updateSignText(Player player,boolean front,List<FilteredText> lines){
        if(!wisdom()&&front)super.updateSignText(player,true,lines);
    }
    public void draw(){
        if(level==null||level.isClientSide||!wisdom())return;
        draw(WisdomStatements.pool());
    }
    public void draw(List<String> pool){
        if(level==null||level.isClientSide||!wisdom())return;
        var signature=WisdomStatements.signature(pool);
        boolean initialize=!signature.equals(poolSignature);
        if(initialize){remaining.clear();poolSignature=signature;}
        if(lastStatement.isEmpty()){
            var displayed=Arrays.stream(getFrontText().getMessages(false)).map(Component::getString).toArray(String[]::new);
            for(var phrase:pool)if(Arrays.equals(displayed,WisdomStatements.positioned(phrase))){lastStatement=phrase;break;}
        }
        remaining.removeIf(s->!pool.contains(s));
        if(remaining.isEmpty()){
            remaining.addAll(pool);
            // Imported structure signs already display one entry, which counts as drawn.
            if(initialize&&remaining.size()>1)remaining.remove(lastStatement);
        }
        int index=remaining.isEmpty()?-1:level.random.nextInt(remaining.size());
        // A fresh cycle must not immediately repeat the preceding cycle's last line.
        if(index>=0&&remaining.size()>1&&remaining.get(index).equals(lastStatement))
            index=(index+1+level.random.nextInt(remaining.size()-1))%remaining.size();
        String picked=index<0?"":remaining.remove(index);
        lastStatement=picked;
        var text=new SignText().setColor(DyeColor.BLACK).setHasGlowingText(true);
        var lines=WisdomStatements.positioned(picked);
        for(int i=0;i<4;i++)text=text.setMessage(i,Component.literal(lines[i]));
        setText(text,true);setText(text,false);setChanged();
    }
    public int remainingCount(){return remaining.size();}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);tag.putString("WisdomPool",poolSignature);tag.putString("WisdomLast",lastStatement);
        var bag=new ListTag();for(var phrase:remaining)bag.add(StringTag.valueOf(phrase));tag.put("WisdomRemaining",bag);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);poolSignature=tag.getString("WisdomPool");lastStatement=tag.getString("WisdomLast");remaining.clear();
        for(var phrase:tag.getList("WisdomRemaining",Tag.TAG_STRING)){
            String value=phrase.getAsString();if(!remaining.contains(value))remaining.add(value);
        }
    }
}
