package Vehicles;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static Vehicles.Registry.*;


@Mod(Main.MODID)
public class Main {

    public static final String MODID = "aw_vehicles";

    public Main() {
        // Retrieve the mod event bus
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register event listeners
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerEntityRenderers);

        // Register your mod-specific registries
        Registry.register(modEventBus);
    }

    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ENTITY_BALLISTA.get(), BallistaRenderer::new);
        event.registerEntityRenderer(ENTITY_BALLISTA_BOLT.get(), BallistaBoltRenderer::new);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent e) {
        ResourceLocation tabKey = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(e.getTab());
        if (tabKey.equals(CreativeModeTabs.FUNCTIONAL_BLOCKS.location())) {
            e.accept(ITEM_BALLISTA_BOLT.get());
            e.accept(ITEM_BALLISTA_SPAWN.get());
            e.accept(ITEM_BALLISTA_REPAIR.get());
            e.accept(ITEM_WOODEN_HAMMER.get());
        }
    }
}