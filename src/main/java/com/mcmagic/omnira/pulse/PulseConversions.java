package com.mcmagic.omnira.pulse;

import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Rate conversion only: no FE battery and no additional source of pulse potential. */
public final class PulseConversions {
    public static final int EXPORT=100,IMPORT=2_000_000,KINETIC=6;
    private static final ThreadLocal<Boolean> EXPORTING=ThreadLocal.withInitial(()->false);
    private final PulseOrbEntity orb;
    private final int[] claims=new int[7],taken=new int[6],received=new int[20];
    private final long[] until=new long[7],receivedAt=new long[20];
    private final IEnergyStorage[] faces=new IEnergyStorage[6];
    private long tick=Long.MIN_VALUE;
    public PulseConversions(PulseOrbEntity orb){this.orb=orb;java.util.Arrays.fill(receivedAt,Long.MIN_VALUE);}
    private long now(){return orb.getLevel()==null?0:orb.getLevel().getGameTime();}
    private void clock(){if(tick!=now()){tick=now();java.util.Arrays.fill(taken,0);}}
    private static <T>T exporting(Supplier<T> action){boolean before=EXPORTING.get();EXPORTING.set(true);try{return action.get();}finally{EXPORTING.set(before);}}
    public boolean input(Direction face){
        return face==orb.inlet();
    }
    public boolean externalInput(Direction face){
        if(!input(face))return false;
        if(!(orb.getLevel() instanceof net.minecraft.server.level.ServerLevel level))return false;
        var pos=orb.getBlockPos().relative(face);var side=face.getOpposite();
        if(!level.hasChunkAt(pos))return false;
        var port=com.mcmagic.omnira.space.SpacetimePortState.at(level,pos);
        if(port!=null){var other=port.other();if(other==null||port.face()!=face)return false;pos=other.attached();side=other.face();}
        return PulseDevices.at(level,pos,side)==null;
    }
    public int claim(int output){return claims[output]>0&&now()<=until[output]?claims[output]:0;}
    public double imported(Direction face){
        if(!externalInput(face))return 0;
        return Math.min(orb.capacity(),receivedRate());
    }
    private int receivedRate(){int amount=0;for(int i=0;i<20;i++)if(now()>=receivedAt[i]&&now()-receivedAt[i]<20)amount+=received[i];return amount;}
    private void claim(int output,int amount){
        boolean changed=claims[output]!=amount;claims[output]=amount;until[output]=now()+1;
        if(changed)PulseNetworks.changed(orb.getLevel(),orb.getBlockPos());
    }
    public int reserve(int output,int requested,boolean simulate){
        if(orb.getLevel()==null||orb.getLevel().isClientSide||orb.isRemoved())return 0;
        int amount=PulseNetworks.conversion(orb,output,Math.clamp(requested,0,orb.capacity()));
        if(!simulate)claim(output,amount);return amount;
    }
    public int headroom(int output){return reserve(output,orb.capacity(),true);}
    private int quantum(int rate){long phase=Math.floorMod(now(),20);return (int)(((phase+1)*rate)/20-(phase*rate)/20);}
    public IEnergyStorage face(Direction face){
        if(face==null)return null;int i=face.ordinal();
        if(faces[i]==null)faces[i]=new IEnergyStorage(){
            public int receiveEnergy(int amount,boolean simulate){
                if(amount<=0||EXPORTING.get()||!externalInput(face)||orb.getLevel().isClientSide||orb.isRemoved())return 0;
                int accepted=Math.min(amount,Math.max(0,orb.capacity()-receivedRate()));
                if(!simulate&&accepted>0){
                    int bucket=(int)Math.floorMod(now(),20);if(receivedAt[bucket]!=now())received[bucket]=0;
                    received[bucket]+=accepted;receivedAt[bucket]=now();
                    PulseNetworks.changed(orb.getLevel(),orb.getBlockPos());
                    com.mcmagic.omnira.progression.BranchProgress.automated(orb.host,accepted);
                }return accepted;
            }
            public int extractEnergy(int amount,boolean simulate){
                if(amount<=0||input(face)||orb.getLevel()==null||orb.getLevel().isClientSide||orb.isRemoved()
                        ||PulseOutput.nativeConnection(orb.getLevel(),orb.getBlockPos(),face))return 0;
                clock();int requested=(int)Math.min(orb.capacity(),20L*(Math.max(0,amount)+(long)taken[i]));
                int rate=reserve(i,requested,true);int supplied=Math.min(amount,Math.max(0,quantum(rate)-taken[i]));
                if(!simulate&&supplied>0){claim(i,rate);taken[i]+=supplied;}return supplied;
            }
            public int getEnergyStored(){return canExtract()?extractEnergy(Integer.MAX_VALUE,true):0;}
            public int getMaxEnergyStored(){return orb.capacity();}
            public boolean canExtract(){return !input(face)&&orb.getLevel()!=null&&!orb.isRemoved()&&!PulseOutput.nativeConnection(orb.getLevel(),orb.getBlockPos(),face);}
            public boolean canReceive(){return externalInput(face)&&!EXPORTING.get()&&!orb.isRemoved();}
        };
        return faces[i];
    }
    public void tick(){
        boolean expired=false;
        for(int i=0;i<claims.length;i++)if(claims[i]>0&&now()>until[i]){claims[i]=0;expired=true;}
        if(expired)PulseNetworks.changed(orb.getLevel(),orb.getBlockPos());
        if(!com.mcmagic.omnira.energy.EnergyIntegration.available())return;
        for(var direction:Direction.values()){
            if(input(direction)||PulseOutput.nativeConnection(orb.getLevel(),orb.getBlockPos(),direction))continue;
            var next=orb.getBlockPos().relative(direction);if(!orb.getLevel().hasChunkAt(next))continue;
            var receiver=orb.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,next,direction.getOpposite());
            if(receiver==null||!receiver.canReceive())continue;var output=face(direction);
            exporting(()->{
                int offer=output.extractEnergy(Integer.MAX_VALUE,true);if(offer<=0)return 0;
                int accepted=Math.clamp(receiver.receiveEnergy(offer,true),0,offer);int paid=output.extractEnergy(accepted,false);
                int sent=Math.clamp(receiver.receiveEnergy(paid,false),0,paid);taken[direction.ordinal()]-=paid-sent;
                com.mcmagic.omnira.progression.BranchProgress.automated(orb.host,sent);return sent;
            });
        }
    }
}
