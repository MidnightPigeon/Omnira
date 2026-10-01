package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.time.EventideRuins;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("omnira_time_finale")
@PrefixGameTestTemplate(false)
public final class TimeContinentFinaleGameTests {
    private static ServerPlayer player(GameTestHelper h){
        var level=h.getLevel();var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"time-finale");
        var p=new ServerPlayer(level.getServer(),level,profile,ClientInformation.createDefault());
        p.connection=new net.neoforged.neoforge.common.util.FakePlayer(level,profile).connection;
        var base=h.absolutePos(new BlockPos(5,1,5));p.setPos(base.getX()+.5,225,base.getZ()+.5);p.setGameMode(GameType.SURVIVAL);
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(EventideRuins.BIOME);
        level.getChunkAt(p.blockPosition()).fillBiomesFromNoise((x,y,z,s)->biome,level.getChunkSource().randomState().sampler());
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);p.setHealth(40);return p;
    }
    @GameTest(template="spell_arena") public static void trueDamageAndCycle(GameTestHelper h){
        var p=player(h);p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,600,4));
        p.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(8);
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new ItemStack(Items.NETHERITE_CHESTPLATE));p.setAbsorptionAmount(8);
        EventideRuins.pulsePlayer(h.getLevel(),p,10);
        h.assertTrue(p.getHealth()==20&&p.getAbsorptionAmount()==8,"Damage was reduced or absorbed");
        EventideRuins.pulsePlayer(h.getLevel(),p,10);h.assertTrue(p.getHealth()==20,"Duplicate node damage");
        p.setHealth(25);EventideRuins.pulsePlayer(h.getLevel(),p,11);
        h.assertTrue(p.getHealth()==5,"Damage used current rather than maximum health");
        EventideRuins.pulsePlayer(h.getLevel(),p,12);h.assertTrue(!p.isAlive(),"Low-health player survived");h.succeed();
    }
    @GameTest(template="spell_arena") public static void protectionAndBoundaries(GameTestHelper h){
        var p=player(h);p.getInventory().setItem(0,new ItemStack(ModItems.SPACETIME_ANCHORING_SIGIL.get()));
        EventideRuins.pulsePlayer(h.getLevel(),p,20);h.assertTrue(p.getHealth()==20,"Carried sigil grants protection");
        CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("charm",0,new ItemStack(ModItems.SPACETIME_ANCHORING_SIGIL.get()));
        EventideRuins.pulsePlayer(h.getLevel(),p,21);h.assertTrue(p.getHealth()==20,"Equipped sigil failed");
        CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("charm",0,ItemStack.EMPTY);
        p.setGameMode(GameType.CREATIVE);EventideRuins.pulsePlayer(h.getLevel(),p,22);h.assertTrue(p.getHealth()==20,"Creative damaged");
        p.setGameMode(GameType.SPECTATOR);EventideRuins.pulsePlayer(h.getLevel(),p,23);h.assertTrue(p.getHealth()==20,"Spectator damaged");
        p.setGameMode(GameType.SURVIVAL);
        var plain=h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
        h.getLevel().getChunkAt(p.blockPosition()).fillBiomesFromNoise((x,y,z,s)->plain,h.getLevel().getChunkSource().randomState().sampler());
        EventideRuins.pulsePlayer(h.getLevel(),p,24);h.assertTrue(p.getHealth()==20,"Player outside ruins damaged");h.succeed();
    }
    @GameTest(template="spell_arena") public static void generationParity(GameTestHelper h)throws Exception{
        for(String name:new String[]{"doomsday_rite","frozen_terra_palace","looking_glass_garden","rusty_lake_mill"}){
            try(var in=getClassResource("/data/omnira/worldgen/structure_set/"+name+".json")){
                var placement=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("placement");
                h.assertTrue(placement.get("spacing").getAsInt()==16&&placement.get("separation").getAsInt()==4&&placement.get("frequency").getAsDouble()==.25,"Structure frequency mismatch: "+name);
            }
        }
        var structure=h.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE).get(net.minecraft.resources.ResourceLocation.parse("omnira:doomsday_rite"));
        h.assertTrue(structure!=null&&structure.biomes().stream().allMatch(b->b.is(EventideRuins.BIOME)),"Rite biome restriction missing");h.succeed();
    }
    private static java.io.InputStream getClassResource(String path){return java.util.Objects.requireNonNull(TimeContinentFinaleGameTests.class.getResourceAsStream(path));}
}
