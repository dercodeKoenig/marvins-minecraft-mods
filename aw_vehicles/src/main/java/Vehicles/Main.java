package Vehicles;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.io.IOException;

import static Vehicles.Registry.*;


@Mod(Main.MODID)
public class Main {

    public static final String MODID = "aw_vehicles";

    public Main(IEventBus modEventBus, ModContainer modContaine) throws IOException {
        //modEventBus.register(this);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::registerNetworkStuff);
        modEventBus.addListener(this::entityAttributeCreation);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerCapabilities);

        Registry.register(modEventBus);

    }
    public void onClientSetup(FMLClientSetupEvent event) {
    }


    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent login){

    }


    private void registerCapabilities(RegisterCapabilitiesEvent e) {

    }

    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ENTITY_BALLISTA.get(), BallistaRenderer::new);
        event.registerEntityRenderer(ENTITY_BALLISTA_BOLT.get(), BallistaBoltRenderer::new);
    }

    public void registerNetworkStuff(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
    }

    public void entityAttributeCreation(EntityAttributeCreationEvent event) {
    }

    private void addCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTab().equals(Registry.CREATIVETAB.get())) {
            e.accept(ITEM_BALLISTA_BOLT.get());
            e.accept(ITEM_BALLISTA_SPAWN.get());
            e.accept(ITEM_BALLISTA_REPAIR.get());
        }
    }
}