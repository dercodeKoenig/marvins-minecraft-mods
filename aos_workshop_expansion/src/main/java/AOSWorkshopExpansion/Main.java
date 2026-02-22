package AOSWorkshopExpansion;


import AOSWorkshopExpansion.Conveyor.ConveyorConfig;
import AOSWorkshopExpansion.Conveyor.RenderConveyorBelt;
import AOSWorkshopExpansion.Drill.DrillConfig;
import AOSWorkshopExpansion.Drill.RenderDrill;
import AOSWorkshopExpansion.Piston.PistonConfig;
import AOSWorkshopExpansion.Piston.RenderPiston;
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
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.io.IOException;
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
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::loadComplete);
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::registerCapabilities);
        Registry.register(modEventBus);


        SimpleNetworkPacket.registerReceiver(SieveConfig.packetConfigSyncID, new SieveConfig());
        SimpleNetworkPacket.registerReceiver(WoodMillConfig.packetConfigSyncID, new WoodMillConfig());
        SimpleNetworkPacket.registerReceiver(MillStoneConfig.packetConfigSyncID, new MillStoneConfig());
        SimpleNetworkPacket.registerReceiver(SpinningWheelConfig.packetConfigSyncID, new SpinningWheelConfig());
        SimpleNetworkPacket.registerReceiver(ConveyorConfig.packetConfigSyncID, new ConveyorConfig());
        SimpleNetworkPacket.registerReceiver(PistonConfig.packetConfigSyncID, new PistonConfig());
        SimpleNetworkPacket.registerReceiver(DrillConfig.packetConfigSyncID, new DrillConfig());

    }

    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent login) {
        if (login.getEntity() instanceof ServerPlayer p) {
            SieveConfig.SyncConfig(p);
            SpinningWheelConfig.SyncConfig(p);
            WoodMillConfig.SyncConfig(p);
            MillStoneConfig.SyncConfig(p);
            ConveyorConfig.SyncConfig(p);
            PistonConfig.SyncConfig(p);
            DrillConfig.SyncConfig(p);
        }
    }

    public void onServerStarted(ServerStartedEvent event){
        SieveConfig.load();
        SpinningWheelConfig.load();
        WoodMillConfig.load();
        MillStoneConfig.load();
        ConveyorConfig.load();
        PistonConfig.load();
        DrillConfig.load();
    }

    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ENTITY_SIEVE.get(), RenderSieve::new);
        event.registerBlockEntityRenderer(ENTITY_WOODMILL.get(), RenderWoodMill::new);
        event.registerBlockEntityRenderer(ENTITY_SPINNING_WHEEL.get(), RenderSpinningWheel::new);
        event.registerBlockEntityRenderer(ENTITY_MILLSTONE.get(), RenderMillStone::new);
        event.registerBlockEntityRenderer(ENTITY_CONVEYOR_BELT.get(), RenderConveyorBelt::new);
        event.registerBlockEntityRenderer(ENTITY_PISTON.get(), RenderPiston::new);
        event.registerBlockEntityRenderer(ENTITY_DRILL.get(), RenderDrill::new);
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

            e.accept(DRILL.get());
        }
    }


    private void loadComplete(FMLLoadCompleteEvent e) {
        itemHoloProjector.registerMultiblock("MillStone", EntityMillStone.structure, EntityMillStone.charMapping);
        itemHoloProjector.registerMultiblock("WoodMill", EntityWoodMill.structure, EntityWoodMill.charMapping);
    }
}