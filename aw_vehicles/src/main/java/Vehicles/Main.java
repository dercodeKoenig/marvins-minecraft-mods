package Vehicles;

import Vehicles.Ballista.Ballista;
import Vehicles.Ballista.BallistaBoltRenderer;
import Vehicles.Ballista.BallistaRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
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
        modEventBus.addListener(this::entityAttributeCreation);

        Registry.register(modEventBus);

    }

    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ENTITY_BALLISTA.get(), BallistaRenderer::new);
        event.registerEntityRenderer(ENTITY_BALLISTA_BOLT.get(), BallistaBoltRenderer::new);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTab().equals(Registry.CREATIVETAB.get())) {
            e.accept(ITEM_BALLISTA_BOLT.get());
            e.accept(ITEM_BALLISTA_SPAWN.get());
            e.accept(ITEM_BALLISTA_REPAIR.get());
            if (!ModList.get().isLoaded("age_of_steam")) {
                e.accept(ITEM_WOODEN_HAMMER.get());
            }
        }
    }



    public void entityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ENTITY_BALLISTA.get(), LivingEntity.createLivingAttributes().build());
    }

}