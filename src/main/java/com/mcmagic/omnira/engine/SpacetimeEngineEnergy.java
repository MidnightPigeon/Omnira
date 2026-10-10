package com.mcmagic.omnira.engine;

import com.mcmagic.omnira.energy.EngineEnergyBuffer;
import com.mcmagic.omnira.energy.EnergyIntegration;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Each physical output shares one generation budget between active push and passive pull. */
public final class SpacetimeEngineEnergy {
    private final SpacetimeEngineController engine;
    private final EngineEnergyBuffer[] buffers=new EngineEnergyBuffer[4];
    private final IEnergyStorage[] ports=new IEnergyStorage[4];
    public SpacetimeEngineEnergy(SpacetimeEngineController engine){
        this.engine=engine;
        for(int i=0;i<4;i++){
            final int side=i;buffers[i]=new EngineEnergyBuffer();
            ports[i]=new IEnergyStorage(){
                public int receiveEnergy(int amount,boolean simulate){return 0;}
                public int extractEnergy(int amount,boolean simulate){
                    if(!running(side))return 0;
                    buffers[side].beginTick(engine.getLevel().getGameTime(),rate(side),true);
                    int taken=buffers[side].extract(amount,simulate);
                    if(!simulate&&taken>0)engine.setChanged();
                    return taken;
                }
                public int getEnergyStored(){return running(side)?Math.min(buffers[side].stored(),rate(side)):0;}
                public int getMaxEnergyStored(){return rate(side);}
                public boolean canExtract(){return running(side);}
                public boolean canReceive(){return false;}
            };
        }
    }
    public int rate(int side){
        if(side<0||side>=4)return 0;
        return SpacetimeEngineController.hasKinetics()?engine.capacity(side):(int)Math.round(512*engine.gridMultiplier());
    }
    private boolean running(int side){
        return engine.getLevel()!=null&&!engine.getLevel().isClientSide&&EnergyIntegration.available()
                &&engine.getLevel().getBlockEntity(engine.getBlockPos())==engine&&engine.generatedSpeed(side)!=0;
    }
    public IEnergyStorage port(int side){return side>=0&&side<4&&EnergyIntegration.available()?ports[side]:null;}
    public void tick(){
        var level=engine.getLevel();if(level==null||level.isClientSide||!EnergyIntegration.available())return;
        for(int side=0;side<4;side++){
            buffers[side].beginTick(level.getGameTime(),rate(side),running(side));
            var output=ports[side];int offer=output.extractEnergy(Integer.MAX_VALUE,true);
            if(offer<=0)continue;
            Direction face=SpacetimeEngineBlock.OUTPUTS[side];var neighbor=engine.getBlockPos().below().relative(face,2);
            if(!level.hasChunkAt(neighbor))continue;
            var receiver=level.getCapability(Capabilities.EnergyStorage.BLOCK,neighbor,face.getOpposite());
            if(receiver!=null&&receiver.canReceive()){
                int accepted=Math.clamp(receiver.receiveEnergy(offer,false),0,offer);
                output.extractEnergy(accepted,false);
            }
        }
    }
    public void save(CompoundTag tag){
        int[] stored=new int[4],remainders=new int[4];
        for(int i=0;i<4;i++){stored[i]=buffers[i].stored();remainders[i]=buffers[i].remainder();}
        tag.putIntArray("OutputEnergy",stored);tag.putIntArray("OutputEnergyFractions",remainders);
    }
    public void load(CompoundTag tag){
        var stored=tag.getIntArray("OutputEnergy");var remainders=tag.getIntArray("OutputEnergyFractions");
        for(int i=0;i<4;i++)buffers[i].restore(stored.length==4?Math.min(stored[i],rate(i)):0,remainders.length==4?remainders[i]:0);
    }
}
