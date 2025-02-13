package AOSBasicFluid;

import AOSBasicFluid.Pump.RenderPump;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.io.IOException;

import static AgeOfSteam.Registry.AOS_CREATIVETAB;


@Mod(Main.MODID)
public class Main {

    public static final String MODID = "aos_basic_fluid";

    public Main(IEventBus modEventBus, ModContainer modContaine) throws IOException {
        //modEventBus.register(this);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::loadComplete);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::RegisterCapabilities);
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::loadShaders);
        modEventBus.addListener(this::registerNetworkStuff);
        modEventBus.addListener(this::registerScreens);
        Registry.register(modEventBus);
    }

    public void registerScreens(RegisterMenuScreensEvent event) {
    }

    public void onClientSetup(FMLClientSetupEvent event) {
    }

    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent login) {
        if (login.getEntity() instanceof ServerPlayer p) {
        }
    }

    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registry.ENTITY_PUMP.get(), RenderPump::new);
    }

    public void registerNetworkStuff(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTab().equals(AOS_CREATIVETAB.get())) {
            e.accept(Registry.PUMP.get());
        }
    }

    private void loadShaders(RegisterShadersEvent e) {
    }

    private void RegisterCapabilities(RegisterCapabilitiesEvent e) {
    }

    private void loadComplete(FMLLoadCompleteEvent e) {
    }
}