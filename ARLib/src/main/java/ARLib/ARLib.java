package ARLib;

import ARLib.network.PacketBlockEntity;
import ARLib.network.PacketEntity;
import ARLib.network.PacketPlayerMainHand;
import ARLib.network.SimpleNetworkPacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(ARLib.MODID)
public class ARLib {
    public static final String MODID = "arlib";

    public ARLib(IEventBus modEventBus, ModContainer modContaine) {
        //NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::RegisterCapabilities);
        modEventBus.addListener(this::registerNetworkStuff);
        modEventBus.addListener(this::loadComplete);
        modEventBus.addListener(this::registerEntityRenderers);

        ARLibRegistry.register(modEventBus);
    }

    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ARLibRegistry.registerRenderers(event);
    }

    public void registerNetworkStuff(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        PacketBlockEntity.register(registrar);
        PacketPlayerMainHand.register(registrar);
        PacketEntity.register(registrar);
        SimpleNetworkPacket.register(registrar);
    }

    private void RegisterCapabilities(RegisterCapabilitiesEvent e) {
        ARLibRegistry.registerCapabilities(e);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent e) {
        ARLibRegistry.addCreative(e);
    }

    private void loadComplete(FMLLoadCompleteEvent e) {
    }
}
