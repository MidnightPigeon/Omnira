package com.mcmagic.omnira.client;

/** The default transition group; special sequences bypass these presets. */
public final class MiniatureWorldTransition {
    public enum Preset { WAVE, GROW, STARDUST }
    public static Preset preset(String scene){
        return switch(scene){
            case "overworld","nether","fortress","bastion","end","chorus","city","workshop"->Preset.GROW;
            case "nether_empty","end_empty","dream_empty","dream_layers"->Preset.STARDUST;
            default->Preset.WAVE;
        };
    }
    public static float hash(float x,float y,float z){
        int n=Math.round(x*8)*73856093^Math.round(y*8)*19349663^Math.round(z*8)*83492791;
        n^=n>>>16;n*=0x45d9f3b;n^=n>>>16;return (n&0xFFFF)/65535F;
    }
    public static float rank(Preset preset,boolean incoming,float x,float y,float z,boolean terrain){
        float height=Math.clamp((y-.5F)/3.6F,0,1);
        return switch(preset){
            case WAVE->.08F+.72F*Math.clamp((float)Math.hypot(x-2.5,z-2.5)/2.35F,0,1)+(terrain?0:.08F);
            case GROW->incoming?.32F+.53F*height+(terrain?0:.07F):.06F+.24F*(1-height);
            case STARDUST->incoming?.56F+.34F*hash(x,y,z):.06F+.36F*hash(x,y,z);
        };
    }
    public static float amount(Preset preset,float progress,boolean incoming,float x,float y,float z,boolean terrain){
        return atRank(rank(preset,incoming,x,y,z,terrain),progress,incoming);
    }
    public static float atRank(float t,float progress,boolean incoming){
        return incoming?MiniatureWorldMotion.smooth((progress-t)/.05F):1-MiniatureWorldMotion.smooth((progress-t+.05F)/.05F);
    }
    /** Residents and portals appear at their own local origin, never above full size. */
    public static float actorAmount(float progress,boolean incoming){
        return incoming?MiniatureWorldMotion.smooth((progress-.8F)/.2F):1-MiniatureWorldMotion.smooth(progress/.25F);
    }
    public static float gateRadius(float progress){return .65F+1.70F*MiniatureWorldMotion.smooth(progress/.45F);}
    public static float riftOpening(float progress){return MiniatureWorldMotion.smooth(progress/.2F)*(1-MiniatureWorldMotion.smooth((progress-.78F)/.22F));}
    public static float riftCapture(float progress,float x){return MiniatureWorldMotion.smooth((progress-.12F-.34F*Math.abs(x-2.5F)/1.8F)/.28F);}
    public record Tear(float x,float bottom,float top,float halfWidth,float depth){}
    public static int riftColor(double time,int row,int column){
        if((row+column)%7==0&&Math.sin(row*1.7+column*.8+time*.09)>.98)return 0xFFD4F2EF;
        return column==0||column==7?0xFF8E7BAF:column==1||column==6?0xFF394868:0xFF101929;
    }
    public static Tear tear(float progress,int row){
        float opening=riftOpening(progress),t=(row+.5F)/32F;
        float profile=(float)Math.pow(Math.sin(t*Math.PI),.65);
        return new Tear(2.5F+(hash(row,0,1)-.5F)*.18F*opening,2.5F+(row/32F*2-1)*1.65F*opening,
                2.5F+((row+1)/32F*2-1)*1.65F*opening,(.22F+.20F*hash(row,1,0))*profile*opening,.035F*opening);
    }
    public static float gateHeight(float progress){return .09375F+3.50625F*(1-MiniatureWorldMotion.smooth((progress-.12F)/.85F));}
    public static float gateAmount(float progress,float x,float y,float z){
        if(Math.hypot(x-2.5,z-2.5)>gateRadius(progress))return 1;
        return Math.clamp((gateHeight(progress)-(y-.0625F))/.125F,0,1);
    }
    public record Grain(float x,float y,float z,float size){}
    public static Grain grain(float progress,int index,float x,float y,float z,float X,float Y,float Z){
        float t=MiniatureWorldMotion.smooth((progress-.12F)/.78F);
        float arc=(float)Math.sin(t*Math.PI),angle=index*2.399963F;
        return new Grain(x+(X-x)*t+(float)Math.cos(angle)*arc*.14F,Math.min(4.6F,y+(Y-y)*t+arc*.65F),
                z+(Z-z)*t+(float)Math.sin(angle)*arc*.14F,.025F*arc);
    }
    private MiniatureWorldTransition(){}
}
