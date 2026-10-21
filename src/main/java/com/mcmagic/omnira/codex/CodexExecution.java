package com.mcmagic.omnira.codex;

import com.mcmagic.omnira.codex.edit.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;

/** Validate a complete snapshot before consuming the formula or recalling its placement marker. */
public final class CodexExecution {
    public static boolean active(MyriadCodexEntity codex){return codex.job!=null&&CodexEdits.get((ServerLevel)codex.getLevel()).active(codex.job);}
    public static void update(MyriadCodexEntity codex){
        var level=(ServerLevel)codex.getLevel();var edits=CodexEdits.get(level);
        var progress=edits.progress(level,codex.job).orElse(null);
        if(!edits.active(codex.job)){
            if(progress!=null&&!progress.error().isEmpty()&&codex.requester!=null){var p=level.getServer().getPlayerList().getPlayer(codex.requester);if(p!=null)p.displayClientMessage(net.minecraft.network.chat.Component.translatable("codex.omnira.error.execution"),false);}
            edits.forget(codex.job);codex.job=null;codex.requester=null;codex.phase=0;codex.sync();return;
        }
        int next=progress==null?1:switch(progress.status()) {case SAMPLING->1;case CLEARING->2;case PLACING->3;default->4;};
        if(next!=codex.phase){codex.phase=next;codex.sync();}
    }
    public static String start(MyriadCodexEntity codex,ServerPlayer player){
        var level=(ServerLevel)codex.getLevel();
        var first=codex.materials.getStackInSlot(0).copyWithCount(1);
        var second=codex.materials.getStackInSlot(1).copyWithCount(1);
        if(!CodexMaterials.allowed(first)||!CodexMaterials.allowed(second))
            return CodexMaterials.hasContents(first)||CodexMaterials.hasContents(second)?"contents":"material";
        if(codex.mode!=1&&first.isEmpty())return "material";
        var op=codex.mode==0?Operation.MATTER:codex.mode==2?Operation.ECOLOGY:switch(codex.shape){case 1->Operation.INSCRIBED;case 2->Operation.PLANE;default->Operation.SHAPE;};
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome=null;
        if(codex.mode==2){
            var id=BuiltInRegistries.ITEM.getKey(first.getItem());
            if(!id.getNamespace().equals("omnira"))return "material";
            EcologySelection.Group group=null;
            for(var g:EcologySelection.Group.values())if(id.getPath().equals(g.name().toLowerCase(java.util.Locale.ROOT)+"_microcore")){group=g;break;}
            if(group==null)return "material";
            biome=EcologySelection.select(level.registryAccess().registryOrThrow(Registries.BIOME),group,level.random).orElse(null);
            if(biome==null)return "biome";
        }
        var result=CodexEdits.get(level).submit(codex.heartPos,codex.diameter(),op,Direction.Axis.values()[codex.axis],codex.hollow,
                codex.mode==0?first:ItemStack.EMPTY,codex.mode==0?second:codex.mode==1?first:ItemStack.EMPTY,biome,player);
        if(!result.accepted())return error(result.error());
        codex.job=result.jobId();
        codex.requester=player.getUUID();codex.phase=codex.mode==2?1:3;
        codex.recall();
        codex.materials.extractItem(0,1,false);
        if(codex.mode==0)codex.materials.extractItem(1,1,false);
        codex.sync();return "";
    }
    private static String error(String reason){
        if(reason==null)return "invalid";
        if(reason.contains("loaded")||reason.contains("chunks"))return "chunks";
        if(reason.contains("height")||reason.contains("border"))return "border";
        if(reason.contains("biome")||reason.contains("sample"))return "biome";
        if(reason.contains("missing"))return "material";
        if(reason.equals("filled_material"))return "contents";
        if(reason.contains("limit"))return "limit";
        return "invalid";
    }
    private CodexExecution(){}
}
