package com.mcmagic.omnira.space.abyss;

import java.util.*;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Tick-based transition, shared by physics and presentation; queries never advance it twice. */
@EventBusSubscriber(modid="omnira")
public final class GravityTransition {
    public static final int DURATION=24,STABILITY=4;
    private static final Map<Entity,State> STATES=Collections.synchronizedMap(new WeakHashMap<>());
    public static final class State {
        private long tick,started,candidateSince;
        private int target,candidate;
        private double fromGravity,fromRoll,toRoll;
        public State(int direction,long now){tick=started=candidateSince=now;target=candidate=direction;fromGravity=direction;fromRoll=toRoll=direction<0?180:0;started=now-DURATION;}
        public void update(int raw,long now){
            if(tick==now)return;tick=now;
            if(raw!=candidate){candidate=raw;candidateSince=now;}
            if(candidate!=target&&now-candidateSince>=STABILITY){
                fromGravity=gravity(now);fromRoll=roll(now);target=candidate;
                // Weightless space preserves the current attitude until a new gravity chooses an up direction.
                toRoll=target==0?fromRoll:target<0?180:0;started=now;
            }
        }
        private double blend(double now){double t=Math.clamp((now-started)/DURATION,0,1);return t*t*(3-2*t);}
        public double gravity(double now){return fromGravity+(target-fromGravity)*blend(now);}
        public double roll(double now){return fromRoll+(toRoll-fromRoll)*blend(now);}
        public boolean moving(long now){return now-started<DURATION;}
    }
    private static State state(Entity e){
        long now=e.level().getGameTime();var state=STATES.computeIfAbsent(e,key->new State(AbyssGravity.direction(e),now));
        state.update(AbyssGravity.direction(e),now);return state;
    }
    public static double gravity(Entity e){return e.isSpectator()?1:state(e).gravity(e.level().getGameTime());}
    public static float roll(Entity e,float partial){return e.isSpectator()?0:(float)state(e).roll(e.level().getGameTime()+partial);}
    public static boolean inverted(Entity e){return gravity(e)<-.001;}
    public static double eyeOffset(Entity e,float partial){
        double blend=(1-Math.cos(Math.toRadians(roll(e,partial))))/2;
        return (e.getBbHeight()-2*e.getEyeHeight())*blend;
    }
    @SubscribeEvent public static void tick(EntityTickEvent.Pre event){
        var e=event.getEntity();if(!e.isAlive()||e.isSpectator())return;
        var state=state(e);
        if(state.moving(e.level().getGameTime())){
            e.fallDistance=0;
            if(!e.isPassenger()&&!e.isNoGravity()&&!(e instanceof net.minecraft.world.entity.player.Player player&&player.getAbilities().flying)){
                var v=e.getDeltaMovement();e.setDeltaMovement(v.x,Math.clamp(v.y*.88,-.12,.12),v.z);
            }
        }
    }
    private GravityTransition(){}
}
