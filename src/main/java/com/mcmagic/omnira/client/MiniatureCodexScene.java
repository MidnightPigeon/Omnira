package com.mcmagic.omnira.client;

import java.util.ArrayList;
import java.util.List;

/** Deterministic shared geometry for runtime and offline previews; evolution is in [0,5]. */
public final class MiniatureCodexScene {
    public record Box(String id, float x, float y, float z, float width, float height, float depth,
                      int color, boolean glass) {}
    public record Span(float bottom, float top) {}

    /** Only exposed vertical spans of adjacent crystal columns are emitted. */
    public static List<Span> sideSpans(Box box, List<Box> boxes, int side) {
        float bottom = box.y() - box.height() / 2, top = box.y() + box.height() / 2;
        if (box.glass() && box.id().startsWith("hill_")) {
            for (var other : boxes) {
                if (!other.id().startsWith("hill_") || other == box) continue;
                float contact = (box.width() + other.width()) / 2;
                if (Math.abs(other.x() - box.x() - side * contact) > .0001F) continue;
                float low = Math.max(bottom, other.y() - other.height() / 2);
                float high = Math.min(top, other.y() + other.height() / 2);
                if (high <= low) continue;
                var spans = new ArrayList<Span>(2);
                if (low > bottom + .0001F) spans.add(new Span(bottom, low));
                if (high < top - .0001F) spans.add(new Span(high, top));
                return List.copyOf(spans);
            }
        }
        return List.of(new Span(bottom, top));
    }

    public static List<Box> boxes(float evolution, double time) {
        float e = Math.clamp(evolution, 0, 5);
        float matter = phase(e, 0), form = phase(e, 1), age = phase(e, 2);
        float life = phase(e, 3), authorship = Math.clamp(e-4,0,1);
        float page = phase(authorship / .48F, 0), pedestal = phase((authorship-.48F)/.32F,0);
        float bookLift = phase((authorship-.60F)/.30F,0)*.85F;
        var parts = new ArrayList<Box>();
        // These four anchors persist through every rewrite, including the final ink drawing.
        for (int x = 0; x < 6; x++) for (int z = 0; z < 6; z++) {
            if ((x == 0 || x == 5) && (z == 0 || z == 5)) continue;
            add(parts, "ground_" + x + "_" + z, 1.25F + x * .5F, .57F, 1.25F + z * .5F,
                    .5F, .24F, .5F, 0xFF827666, false);
            add(parts, "ground_grass_"+x+"_"+z,1.25F+x*.5F,.705F,1.25F+z*.5F,
                    .5F,.03F,.5F,z<2?0xFF739A65:0xFF64885B,false);
        }
        for (int i = 0; i < 5; i++) {
            float dome = 1 - Math.abs(i - 2) / 3F;
            float top = 1.05F + dome * .8F;
            float bottom = mix(.69F, top - .27F, form * (i == 0 || i == 4 ? 0 : 1));
            add(parts, "hill_" + i, 1.1F + i * .34F, (top + bottom) / 2, 2.35F,
                    .34F, top - bottom, .68F, tint(0xFF78966C, 0xBDA3D6EE, matter), matter > 0);
            float skirtHeight=.12F+dome*.20F;
            add(parts,"foothill_"+i,1.1F+i*.34F,.72F+skirtHeight/2,2.78F+(i%2)*.04F,
                    .34F,skirtHeight,.18F,tint(0xFF879476,0xFFA0B8C0,matter),false);
        }
        for (int i = 0; i < 11; i++) {
            float x = 1.2F + i * .26F, z = 1.45F + (float) Math.sin(i * .6) * .16F;
            add(parts, "river_" + i, x, .725F, z, .28F, .065F, .26F,
                    tint(0xC769B8D3, 0xC744557E, matter), true);
            float star = matter * (.85F + .15F * (float) Math.sin(time * .045 + i));
            add(parts, "river_star_" + i, x, .78F, z, .055F * star, .025F * star,
                    .055F * star, 0xFFFFE6AB, false);
            if (i >= 3 && i <= 8) {
                float lift = life * (.35F + (i - 3) * .14F);
                add(parts, "flying_river_" + i, x, .76F + lift, z + life * .32F,
                        .28F * life, .055F * life, .22F * life, 0xA77DCEEB, true);
                if(i>3)add(parts,"cascade_drop_"+i,x-.13F,.76F+lift-life*.07F,z+life*.32F,
                        .035F*life,.14F*life,.22F*life,0xA77DCEEB,true);
            }
        }
        // Bent trunk segments keep their root on the left arch pier, reaching over its crown.
        for (int i = 0; i < 3; i++) {
            add(parts, "tree_trunk_" + i, 1.18F + form * i * .27F, 1.47F + i * .25F,
                    2.35F, mix(.15F, .35F, form), .27F, .16F, 0xFF785649, false);
        }
        float crownX = 1.18F + form * .7F;
        for(int side:new int[]{-1,1})for(int layer=0;layer<3;layer++){
            float size=.52F-layer*.10F,shift=age*(side<0?-.12F:.13F);
            add(parts,"tree_crown_"+side+"_"+layer,crownX+side*.20F+(layer==1?.04F:0),2.01F+layer*.16F+shift,2.35F+(layer==1?-.06F:0),
                    size,.16F,size+.10F,tint(layer==2?0xFF86B873:0xFF65965B,side<0?0xFF9ACD7D:0xFFB2A16C,age),false);
        }
        add(parts, "tree_old_branch", crownX + .28F, 1.93F, 2.35F, age * .35F, age * .11F,
                age * .14F, 0xFF6C6255, false);
        // Young timbers and weathered stone occupy opposite halves of this same cottage.
        for (int half = 0; half < 2; half++) {
            float x = 3.02F + half * .4F;
            float height = .55F + age * (half == 0 ? -.1F : .12F);
            add(parts, "house_wall_" + half, x, .69F + height / 2, 2.8F, .4F, height, .65F,
                    tint(0xFFD7CBA4, half == 0 ? 0xFFE8D7AD : 0xFF9BACA5, age), false);
            for(int tier=0;tier<4;tier++)add(parts,"house_roof_"+half+"_"+tier,x,.72F+height+tier*.075F,2.8F,
                    .45F,.075F,.85F-tier*.19F,tint(tier%2==0?0xFFA96F65:0xFF92594F,half==0?0xFFB67E68:0xFF697F7C,age),false);
            for(int side:new int[]{-1,1}){
                add(parts,"house_beam_"+half+"_"+side,x+side*.175F,.69F+height/2,2.457F,.045F,height,.045F,0xFF675346,false);
                add(parts,"house_sill_"+half+"_"+side,x,.70F+(side+1)*height/2,2.45F,.4F,.035F,.05F,0xFF675346,false);
                add(parts,"rear_beam_"+half+"_"+side,x+side*.175F,.69F+height/2,3.143F,.045F,height,.045F,0xFF675346,false);
                add(parts,"rear_sill_"+half+"_"+side,x,.70F+(side+1)*height/2,3.15F,.4F,.035F,.05F,0xFF675346,false);
            }
            add(parts,"rear_window_"+half,x,.96F,3.14F,.14F,.17F,.025F,0xFFE8CE8D,false);
            add(parts,"rear_mullion_"+half,x,.96F,3.16F,.025F,.17F,.018F,0xFF675346,false);
        }
        for(int side:new int[]{-1,1}){
            float x=3.22F+side*.41F;
            add(parts,"side_window_"+side,x,1.01F,2.8F,.025F,.18F,.17F,0xFFE8CE8D,false);
            add(parts,"side_crossbar_"+side,x+side*.017F,1.01F,2.8F,.018F,.025F,.17F,0xFF675346,false);
            add(parts,"side_mullion_"+side,x+side*.017F,1.01F,2.8F,.018F,.18F,.025F,0xFF675346,false);
        }
        add(parts, "house_door", 3.14F, .9F, 2.462F, .16F, .4F, .025F, 0xFF64594C, false);
        add(parts, "house_window", 3.42F, 1.06F, 2.46F, .12F, .14F, .03F, 0xFFFFD78C, false);
        add(parts,"window_mullion",3.42F,1.06F,2.438F,.018F,.14F,.018F,0xFF675346,false);
        add(parts,"window_crossbar",3.42F,1.06F,2.438F,.12F,.018F,.018F,0xFF675346,false);
        add(parts,"house_step",3.14F,.75F,2.36F,.23F,.06F,.16F,0xFFADAAA0,false);
        add(parts,"house_chimney",3.50F,1.58F+age*.12F,2.98F,.12F,.40F,.14F,0xFF9A9C91,false);
        add(parts,"chimney_cap",3.50F,1.79F+age*.12F,2.98F,.16F,.04F,.18F,0xFF777F79,false);
        for(int i=0;i<9;i++){
            float x=3.14F-i*.115F,z=2.22F-i*.08F;
            add(parts,"path_"+i,x,.735F,z,.13F,.025F,.12F,i%3==0?0xFFAEAA95:0xFFC0B69D,false);
        }
        for(int i=0;i<8;i++){
            float x=1.12F+(i%4)*.72F,z=3.64F-(i/4)*.43F;
            add(parts,"meadow_stem_"+i,x,.77F,z,.025F,.10F,.025F,0xFF547C52,false);
            add(parts,"meadow_bloom_"+i,x,.83F,z,.065F,.03F,.065F,i%2==0?0xFFE9BD7B:0xFFB4BBE5,false);
        }
        for (int i = 0; i < 5; i++) {
            float x = 1.55F + i * .39F, z = 3.45F;
            add(parts, "life_stem_" + i, x, .7F + life * .16F, z, .07F * life, .32F * life,
                    .07F * life, 0xFF81B485, false);
            add(parts, "life_crystal_" + i, x, .7F + life * .4F, z, .18F * life, .25F * life,
                    .18F * life, i % 2 == 0 ? 0xBBA3D6EE : 0xBBC7A0EF, true);
            add(parts, "tree_crystal_" + i, crownX - .32F + i * .16F, 2.5F + life * .2F,
                    2.35F, .09F * life, .25F * life, .09F * life, 0xBBA3D6EE, true);
        }
        // Keep the same terrain as a small relief resting on the open pages.
        if (page > 0) {
            for (int i = 0; i < parts.size(); i++) {
                var b = parts.get(i);
                float y=.85F+(b.y()-.45F)*.25F;
                parts.set(i,new Box(b.id(),mix(b.x(),2.5F+(b.x()-2.5F)*.65F,page),mix(b.y(),y,page),
                        mix(b.z(),2.5F+(b.z()-2.5F)*.65F,page),b.width()*mix(1,.65F,page),
                        b.height()*mix(1,.25F,page),b.depth()*mix(1,.65F,page),b.color(),b.glass()));
            }
            add(parts, "page_spine", 2.5F, .8F, 2.5F, .075F * page, .06F * page, 3 * page, 0xFF6B7892, false);
            for (int side : new int[]{-1, 1}) {
                add(parts, "page_paper_" + side, 2.5F + side * .78F, .82F, 2.5F,
                        1.43F * page, .06F * page, 3 * page, 0xFFF0EAD7, false);
                add(parts, "page_cover_" + side, 2.5F + side * .78F, .75F, 2.5F,
                        1.48F * page, .065F * page, 3.08F * page, 0xFF53677E, false);
            }
            // The entire written world rises together only after it has settled on the pages.
            for(int i=0;i<parts.size();i++){
                var b=parts.get(i);
                parts.set(i,new Box(b.id(),b.x(),b.y()+bookLift,b.z(),b.width(),b.height(),b.depth(),b.color(),b.glass()));
            }
            add(parts,"pedestal_foot",2.5F,.49F,2.5F,1.30F*pedestal,.08F*pedestal,1.30F*pedestal,0xBBA9DDE2,true);
            add(parts,"pedestal_step",2.5F,.56F,2.5F,.94F*pedestal,.06F*pedestal,.94F*pedestal,0xBBBDCBEB,true);
            float top=.7175F+bookLift;
            add(parts,"pedestal_column",2.5F,(.59F+top-.06F)/2,2.5F,.26F*pedestal,(top-.65F)*pedestal,.26F*pedestal,0xBBABD8DE,true);
            add(parts,"pedestal_cap",2.5F,top-.03F,2.5F,.88F*pedestal,.06F*pedestal,.88F*pedestal,0xBBCADDF2,true);
        }
        return List.copyOf(parts);
    }

    private static void add(List<Box> parts, String id, float x, float y, float z,
                            float w, float h, float d, int color, boolean glass) {
        if (w > .0001F && h > .0001F && d > .0001F) parts.add(new Box(id, x, y, z, w, h, d, color, glass));
    }
    private static float phase(float evolution, int start) {
        float p = Math.clamp(evolution - start, 0, 1);
        return p * p * (3 - 2 * p);
    }
    private static float mix(float a, float b, float p) { return a + (b - a) * p; }
    private static int tint(int a, int b, float p) {
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8)
            result |= Math.round(mix(a >>> shift & 255, b >>> shift & 255, p)) << shift;
        return result;
    }
    private MiniatureCodexScene() {}
}
