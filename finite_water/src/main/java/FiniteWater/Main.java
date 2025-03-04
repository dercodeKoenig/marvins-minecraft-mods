package FiniteWater;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.CreateFluidSourceEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.util.Map;

@Mod("finite_water")
public class Main {

    public Main(IEventBus modEventBus, ModContainer modContaine) {
        NeoForge.EVENT_BUS.addListener(this::onSourceCreate);
    }

    private void onSourceCreate(CreateFluidSourceEvent e) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            if (e.getFluidState().getType().isSame(Fluids.WATER)) {
                Holder<Biome> h = e.getLevel().getBiome(e.getPos());
                ResourceLocation id = server.registryAccess().registryOrThrow(Registries.BIOME).getKey(h.value());
                String idString = id.toString();

                if (Config.INSTANCE.biomes.contains(idString)) {
                    if (Config.INSTANCE.isBlackList) {
                        e.setCanConvert(false);
                    }
                } else {
                    if (!Config.INSTANCE.isBlackList) {
                        e.setCanConvert(false);
                    }
                }
            }
        }
    }
}