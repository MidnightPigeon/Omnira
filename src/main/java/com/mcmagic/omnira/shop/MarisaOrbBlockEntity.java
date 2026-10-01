package com.mcmagic.omnira.shop;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashSet;
import java.util.Set;

/** An access point, never the owner of a player's stock. Legacy per-orb stock is ignored. */
public final class MarisaOrbBlockEntity extends BlockEntity {
    private final Set<MarisaMerchant> sessions=new HashSet<>();
    public MarisaOrbBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntityTypes.MARISA_ORB.get(),pos,state);}
    public MarisaMerchant connect(Player player){
        var session=new MarisaMerchant(this,player);sessions.add(session);return session;
    }
    void forget(MarisaMerchant session){sessions.remove(session);}
    public void open(Player player){
        connect(player).openTradingScreen(player,Component.translatable("block.omnira.marisa_crystal_ball"),1);
    }
    public void tick(){
        if(level!=null&&level.getGameTime()%20==0)com.mcmagic.omnira.world.structure.AuthoredStructureConnections.initialize(this);
        for(var session:Set.copyOf(sessions))if(session.getTradingPlayer()==null)session.close();
    }
    public void close(){for(var session:Set.copyOf(sessions))session.close();}
    @Override public void setRemoved(){close();super.setRemoved();}
}
