package com.mcmagic.omnira.music;

import com.mcmagic.omnira.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** A persisted draw bag; an interrupted recording remains spent for this cycle. */
public final class ViolinTracks {
    private static final List<Supplier<SoundEvent>> SOUNDS=List.of(ModSounds.VIOLIN_ALLEMANDA,
            ModSounds.VIOLIN_ADAGIO,ModSounds.VIOLIN_CAPRICE20,ModSounds.VIOLIN_CAPRICE24,ModSounds.VIOLIN_SONATA6);
    private static final int[] TICKS={3630,5421,3954,6206,9269};
    public static int size(){return TICKS.length;}
    public static SoundEvent sound(int track){return SOUNDS.get(track).get();}
    public static int ticks(int track){return TICKS[track];}
    public static int draw(CompoundTag bag,RandomSource random){
        var remaining=new ArrayList<Integer>();
        if(bag.getInt("PoolSize")==size())for(int i:bag.getIntArray("Remaining"))
            if(i>=0&&i<size()&&!remaining.contains(i))remaining.add(i);
        if(remaining.isEmpty())for(int i=0;i<size();i++)remaining.add(i);
        int index=random.nextInt(remaining.size());
        if(remaining.size()>1&&bag.contains("Last")&&remaining.get(index)==bag.getInt("Last"))
            index=(index+1+random.nextInt(remaining.size()-1))%remaining.size();
        int track=remaining.remove(index);
        bag.putInt("PoolSize",size());bag.putInt("Last",track);
        bag.putIntArray("Remaining",remaining.stream().mapToInt(Integer::intValue).toArray());
        return track;
    }
    private ViolinTracks(){}
}
