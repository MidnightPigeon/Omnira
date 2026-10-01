package com.mcmagic.omnira.client.hud;

import com.mcmagic.omnira.item.CrystalGridItem;
import com.mcmagic.omnira.item.StaffItem;
import com.mcmagic.omnira.menu.CrystalGridMenu;
import com.mcmagic.omnira.mana.ManaCosts;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Quiet spell-chain HUD: shape glyphs replace indistinguishable crystal item icons. */
public final class LatticeOverlay {
    private LatticeOverlay() {}
    public static void render(GuiGraphics g,DeltaTracker delta) {
        var mc=Minecraft.getInstance();
        var player=mc.player;
        if(player==null || mc.screen!=null || mc.options.hideGui || !player.isAlive() || player.isSpectator()) return;
        ItemStack staff=player.getMainHandItem().getItem() instanceof StaffItem?player.getMainHandItem():player.getOffhandItem();
        if(!(staff.getItem() instanceof StaffItem))return;
        var stats=com.mcmagic.omnira.item.staff.StaffAssembly.of(staff).stats();
        ItemStack grid=CrystalGridMenu.locate(player,-2);
        if(!(grid.getItem() instanceof CrystalGridItem item)) return;
        NonNullList<ItemStack> stacks=NonNullList.withSize(item.capacity,ItemStack.EMPTY);
        grid.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(stacks);
        int next=CrystalGridMenu.next(grid,item.capacity);
        int second=-1;
        if(next>=0 && stats.shots()>1) {
            for(int i=1;i<=item.capacity;i++) {
                int index=(next+i)%item.capacity;
                if(CrystalGridMenu.isCrystal(stacks.get(index))) {second=index;break;}
            }
        }
        int cx=46,cy=Math.max(44,g.guiHeight()-92),radius=32;
        // Hollow stepped circle, with no panel obscuring the world.
        for(int i=0;i<24;i++) {
            double a=i*Math.PI/12,b=(i+1)*Math.PI/12;
            line(g,cx+(int)Math.round(Math.sin(a)*22),cy-(int)Math.round(Math.cos(a)*22),
                    cx+(int)Math.round(Math.sin(b)*22),cy-(int)Math.round(Math.cos(b)*22),0x506F8595);
        }
        for(int i=0;i<item.capacity;i++) {
            double a=i*Math.PI*2/item.capacity,b=(i+1)*Math.PI*2/item.capacity;
            int x=cx+(int)Math.round(Math.sin(a)*radius),y=cy-(int)Math.round(Math.cos(a)*radius);
            int nx=cx+(int)Math.round(Math.sin(b)*radius),ny=cy-(int)Math.round(Math.cos(b)*radius);
            line(g,x,y,nx,ny,0x806F8595);
            int ax=(x+nx)/2,ay=(y+ny)/2;
            double direction=Math.atan2(ny-y,nx-x);
            for(double offset:new double[]{-.7,.7}) line(g,ax,ay,
                    ax-(int)Math.round(Math.cos(direction+offset)*3),ay-(int)Math.round(Math.sin(direction+offset)*3),0xB0B5BBC7);
        }
        for(int i=0;i<item.capacity;i++) {
            double a=i*Math.PI*2/item.capacity;
            int x=cx+(int)Math.round(Math.sin(a)*radius),y=cy-(int)Math.round(Math.cos(a)*radius);
            var pattern=stacks.get(i).get(ModDataComponents.SPELL_PATTERN);
            if(pattern==null) {g.fill(x-1,y-1,x+1,y+1,0x807A7C87);continue;}
            if(i==next || i==second) {
                int color=i==next?0xFFF4E4AA:0xFF9BABA8;
                line(g,x-6,y-6,x+5,y-6,color);line(g,x-6,y+5,x+5,y+5,color);
                line(g,x-6,y-6,x-6,y+5,color);line(g,x+5,y-6,x+5,y+5,color);
            }
            glyph(g,x-4,y-4,pattern,i==next?255:175);
            if(SpellPayload.of(stacks.get(i)).hasHealing()) g.fill(x-5,y+3,x-3,y+5,0xFF8EDBA4);
            if(SpellPayload.of(stacks.get(i)).hasHarm()) g.fill(x+3,y+3,x+5,y+5,0xFFEF8A8F);
        }
        if(next<0) return;
        var crystal=stacks.get(next);
        var pattern=crystal.get(ModDataComponents.SPELL_PATTERN);
        Component label=patternLabel(pattern);
        text(g,label,cx,cy+44,86);
        var payload=SpellPayload.of(crystal);
        var keywords=Component.empty();
        java.util.List<String> keys=payload.keywords().isEmpty()
                ?payload.effects().stream().map(effect->effect.utility()?effect.operation():effect.healing()?"healing":"harm").toList()
                :payload.keywords();
        for(String key:keys) {
            if(!keywords.getSiblings().isEmpty()) keywords.append(" · ");
            keywords.append(Component.translatable("spell_component.omnira."+key));
        }
        text(g,keywords.withStyle(style->style.withColor(0xDED4BD)),cx,cy+55,86);
        double reduction=CastAttributes.reduction(player,stats.reduction());
        double cost=ManaCosts.cost(payload.baseCost(),1,reduction);
        String number=number(cost);
        if(second>=0) number+=" + "+number(ManaCosts.cost(SpellPayload.of(stacks.get(second)).baseCost(),1,reduction));
        text(g,Component.translatable("hud.omnira.chain.cost",number)
                .withStyle(s->s.withColor(ManaCosts.canSpend(player,SpellPayload.of(crystal).baseCost())?0xDDDEE5:0xF08B8B)),cx,cy+66,86);
        if(second>=0) {
            var following=stacks.get(second).get(ModDataComponents.SPELL_PATTERN);
            text(g,patternLabel(following).copy().withStyle(s->s.withColor(0xAAB8BF)),cx,cy-46,86);
        }
    }
    private static String number(double value) {
        return value==(int)value?Integer.toString((int)value):String.format(java.util.Locale.ROOT,"%.1f",value);
    }
    private static void glyph(GuiGraphics g,int x,int y,SpellPattern pattern,int alpha) {
        if(pattern.shapeKeyword()==null) {
            g.fill(x-1,y-1,x+8,y+8,0x80202228);
            int color=(alpha<<24)|pattern.targetElement().tooltipColor();
            g.fill(x+3,y+1,x+4,y+6,color);g.fill(x+1,y+3,x+6,y+4,color);
            return;
        }
        String[] pixels=switch(pattern.shapeKeyword()) {
            case BARRIER->new String[]{"0111110","1100011","1000001","1000001","1000001","1100011","0111110"};
            case ORBIT->new String[]{"0001000","0100010","1000001","1001001","1000001","0100010","0001000"};
            case BURST->new String[]{"1001001","0101010","0011100","1111111","0011100","0101010","1001001"};
            case PROJECTILE->new String[]{"0000100","0001110","0011111","0001110","0010100","0100000","1000000"};
        };
        g.fill(x-1,y-1,x+8,y+8,0x80202228);
        int target=(alpha<<24)|pattern.targetElement().tooltipColor();
        g.fill(x-1,y+7,x+8,y+8,target);
        int color=(alpha<<24)|(pattern.composite()?pattern.targetElement():pattern.shapeElement()).tooltipColor();
        for(int row=0;row<7;row++) for(int col=0;col<7;col++)
            if(pixels[row].charAt(col)=='1') g.fill(x+col,y+row,x+col+1,y+row+1,color);
    }
    private static Component patternLabel(SpellPattern pattern) {
        if(pattern.composite())return Component.translatable("spell_composite.omnira."+pattern.compositeName())
                .withStyle(s->s.withColor(pattern.targetElement().tooltipColor()));
        var label=Component.translatable("spell_target_keyword.omnira."+pattern.targetKeyword().getSerializedName())
                .withStyle(s->s.withColor(pattern.targetElement().tooltipColor()));
        if(pattern.shapeElement()!=null)label.append(Component.literal(" · "))
                .append(Component.translatable("spell_shape_keyword.omnira."+pattern.shapeKeyword().getSerializedName())
                        .withStyle(s->s.withColor(pattern.shapeElement().tooltipColor())));
        return label;
    }
    private static void text(GuiGraphics g,Component text,int x,int y,int width) {
        var font=Minecraft.getInstance().font;
        float scale=Math.min(.85F,width/(float)Math.max(1,font.width(text)));
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);
        g.drawString(font,text,-font.width(text)/2,0,0xFFFFFFFF,true);g.pose().popPose();
    }
    private static void line(GuiGraphics g,int x,int y,int endX,int endY,int color) {
        int dx=Math.abs(endX-x),dy=-Math.abs(endY-y),sx=x<endX?1:-1,sy=y<endY?1:-1,err=dx+dy;
        while(true) {
            g.fill(x,y,x+1,y+1,color);
            if(x==endX && y==endY) break;
            int e=2*err;
            if(e>=dy) {err+=dy;x+=sx;} if(e<=dx) {err+=dx;y+=sy;}
        }
    }
}
