package com.mcmagic.omnira.compat.jade;

import com.mcmagic.omnira.pulse.*;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.engine.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Jade discovers this optional plugin; the common mod never loads Jade classes. */
@WailaPlugin
public final class PulseJadePlugin implements IWailaPlugin {
    private static final Provider PROVIDER=new Provider();
    @Override public void register(IWailaCommonRegistration r){r.registerBlockDataProvider(PROVIDER,net.minecraft.world.level.block.entity.BlockEntity.class);}
    @Override public void registerClient(IWailaClientRegistration r){r.registerBlockComponent(PROVIDER,net.minecraft.world.level.block.Block.class);}
    private static final class Provider implements IServerDataProvider<BlockAccessor>,IBlockComponentProvider {
        @Override public ResourceLocation getUid(){return ResourceLocation.parse("omnira:pulse_potential");}
        @Override public void appendServerData(CompoundTag data,BlockAccessor accessor){
            var be=accessor.getBlockEntity();var level=accessor.getLevel();var pos=accessor.getPosition();
            var codex=com.mcmagic.omnira.codex.MyriadCodexEntity.from(be);
            if(codex!=null){data.putInt("CodexDiameter",codex.diameter());data.putInt("CodexPhase",codex.phase);}
            if(be instanceof ManaEngineAccess source){var state=source.engineState();data.putString("PulseKind","source");data.putDouble("PulseAmount",state.running()?state.ratedCapacity():0);data.putInt("PulseLimit",state.ratedCapacity());data.putDouble("PulseOccupied",PulseNetworks.sourceOccupied(level,pos,accessor.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)));return;}
            var engine=SpacetimeEngineBlock.controller(level,pos);
            if(engine!=null){int side=SpacetimeEngineBlock.side(accessor.getBlockState().getValue(SpacetimeEngineBlock.PART));int output=0,limit=0;double used=0;
                for(int i=0;i<4;i++)if(side<0||side==i){limit+=engine.capacity(i);if(engine.generatedSpeed(i)!=0)output+=engine.capacity(i);used+=PulseNetworks.sourceOccupied(level,engine.getBlockPos().below().relative(SpacetimeEngineBlock.OUTPUTS[i]),SpacetimeEngineBlock.OUTPUTS[i]);}
                data.putString("PulseKind","source");data.putDouble("PulseAmount",output);data.putInt("PulseLimit",limit);data.putDouble("PulseOccupied",used);return;}
            var orb=PulseOrbEntity.from(be);
            if(orb!=null){data.putString("PulseKind","orb");data.putDouble("PulseAmount",PulseNetworks.available(orb));data.putDouble("PulseOccupied",PulseNetworks.occupied(orb));data.putDouble("PulseLoss",PulseNetworks.loss(orb));data.putInt("PulseLimit",orb.capacity());return;}
            if(be instanceof PulseWireEntity wire){data.putString("PulseKind","wire");data.putDouble("PulseAmount",PulseNetworks.input(wire));data.putDouble("PulseLoss",PulseNetworks.loss(wire));return;}
            var power=PulseNetworks.power(level,pos);
            int base=PulseNetworks.base(level,pos);
            if(base>0){data.putString("PulseKind","consumer");data.putDouble("PulseAmount",codex==null?power.allocated():codex.potential);data.putDouble("PulseOccupied",power.occupied());data.putInt("PulseBase",base);data.putBoolean("PulseStopped",power.connected()&&!power.running());}
        }
        @Override public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config){
            var data=accessor.getServerData();String kind=data.getString("PulseKind");if(kind.isEmpty())return;
            String amount=String.format(java.util.Locale.ROOT,"%.1f",data.getDouble("PulseAmount"));
            if(kind.equals("orb")||kind.equals("source"))tooltip.add(new PulseBar(kind.equals("source")?Math.max(0,data.getDouble("PulseAmount")-data.getDouble("PulseOccupied")):data.getDouble("PulseAmount"),data.getDouble("PulseOccupied"),data.getInt("PulseLimit")));
            else tooltip.add(Component.translatable("jade.omnira.pulse."+kind,amount));
            if(kind.equals("wire")&&data.getDouble("PulseLoss")>0)tooltip.add(Component.translatable("jade.omnira.pulse.loss",String.format(java.util.Locale.ROOT,"%.1f",data.getDouble("PulseLoss"))));
            if(kind.equals("orb")&&data.getDouble("PulseLoss")>0)tooltip.add(Component.translatable("jade.omnira.pulse.attenuation",String.format(java.util.Locale.ROOT,"%.1f",data.getDouble("PulseLoss"))));
            if(kind.equals("consumer"))tooltip.add(Component.translatable("jade.omnira.pulse.base",data.getInt("PulseBase")));
            if(data.getBoolean("PulseStopped"))tooltip.add(Component.translatable("gui.omnira.pulse.insufficient").withStyle(net.minecraft.ChatFormatting.RED));
            if(data.contains("CodexDiameter")){tooltip.add(Component.translatable("codex.omnira.diameter",data.getInt("CodexDiameter")));if(data.getInt("CodexPhase")>0)tooltip.add(Component.translatable("codex.omnira.phase."+data.getInt("CodexPhase")));}
        }
    }
}
