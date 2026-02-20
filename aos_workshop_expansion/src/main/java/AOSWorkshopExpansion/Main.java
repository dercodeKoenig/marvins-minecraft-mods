package AOSWorkshopExpansion;


import AOSWorkshopExpansion.Conveyor.ConveyorConfig;
import AOSWorkshopExpansion.Conveyor.RenderConveyorBelt;
import ARLib.holoProjector.itemHoloProjector;
import AOSWorkshopExpansion.MillStone.EntityMillStone;
import AOSWorkshopExpansion.MillStone.MillStoneConfig;
import AOSWorkshopExpansion.MillStone.RenderMillStone;
import AOSWorkshopExpansion.Sieve.RenderSieve;
import AOSWorkshopExpansion.Sieve.SieveConfig;
import AOSWorkshopExpansion.SpinningWheel.RenderSpinningWheel;
import AOSWorkshopExpansion.SpinningWheel.SpinningWheelConfig;
import AOSWorkshopExpansion.WoodMill.EntityWoodMill;
import AOSWorkshopExpansion.WoodMill.RenderWoodMill;
import AOSWorkshopExpansion.WoodMill.WoodMillConfig;
import ARLib.network.SimpleNetworkPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static AOSWorkshopExpansion.Registry.*;
import static AgeOfSteam.Registry.AOS_CREATIVETAB;


@Mod(Main.MODID)
public class Main {

    public static final String MODID = "aos_workshop_expansion";
    public static final Path configDir = Path.of(FMLPaths.CONFIGDIR.get().toString(),Main.MODID);

    public Main(IEventBus modEventBus, ModContainer modContaine) throws IOException {
        //modEventBus.register(this);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::loadComplete);
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::registerCapabilities);
        Registry.register(modEventBus);

    }

    public void onServerStarting(ServerStartingEvent event) {
        if(!Files.exists(configDir))
            DataFiles.copyDataFiles("config/aos_workshop_expansion", configDir);
        SimpleNetworkPacket.registerReceiver(SieveConfig.packetConfigSyncID, SieveConfig.INSTANCE);
        SimpleNetworkPacket.registerReceiver(WoodMillConfig.packetConfigSyncID, WoodMillConfig.INSTANCE);
        SimpleNetworkPacket.registerReceiver(MillStoneConfig.packetConfigSyncID, MillStoneConfig.INSTANCE);
        SimpleNetworkPacket.registerReceiver(SpinningWheelConfig.packetConfigSyncID, SpinningWheelConfig.INSTANCE);
        SimpleNetworkPacket.registerReceiver(ConveyorConfig.packetConfigSyncID, ConveyorConfig.INSTANCE);
    }

    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent login) {
        if (login.getEntity() instanceof ServerPlayer p) {
            SieveConfig.SyncConfig(p);
            SpinningWheelConfig.SyncConfig(p);
            WoodMillConfig.SyncConfig(p);
            MillStoneConfig.SyncConfig(p);
            ConveyorConfig.SyncConfig(p);
        }
    }

    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ENTITY_SIEVE.get(), RenderSieve::new);
        event.registerBlockEntityRenderer(ENTITY_WOODMILL.get(), RenderWoodMill::new);
        event.registerBlockEntityRenderer(ENTITY_SPINNING_WHEEL.get(), RenderSpinningWheel::new);
        event.registerBlockEntityRenderer(ENTITY_MILLSTONE.get(), RenderMillStone::new);
        event.registerBlockEntityRenderer(ENTITY_CONVEYOR_BELT.get(), RenderConveyorBelt::new);
    }


    private void registerCapabilities(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY_CONVEYOR_BELT.get(), (x, y) -> (x));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTab().equals(AOS_CREATIVETAB.get())) {
            e.accept(SIEVE.get());
            e.accept(STRING_MESH.get());
            e.accept(SIEVE_HOPPER_UPGRADE.get());

            e.accept(SPINNING_WHEEL.get());

            e.accept(WOODMILL.get());

            e.accept(MILLSTONE.get());
            e.accept(FLOUR.get());

            e.accept(CONVEYOR_BELT.get());
            e.accept(CONVEYOR_ENGINE.get());

            e.accept(PISTON.get());
            e.accept(PISTON_HEAD.get());
            e.accept(PISTON_EXTENSION.get());
        }
    }


    private void loadComplete(FMLLoadCompleteEvent e) {
        itemHoloProjector.registerMultiblock("MillStone", EntityMillStone.structure, EntityMillStone.charMapping);
        itemHoloProjector.registerMultiblock("WoodMill", EntityWoodMill.structure, EntityWoodMill.charMapping);
    }
}