package ARMachines;

import AOSWorkshopExpansion.MillStone.MillStoneConfig;
import AOSWorkshopExpansion.Sieve.SieveConfig;
import AOSWorkshopExpansion.SpinningWheel.SpinningWheelConfig;
import AOSWorkshopExpansion.WoodMill.WoodMillConfig;
import ARLib.holoProjector.itemHoloProjector;
import ARMachines.lathe.EntityLathe;
import ARMachines.lathe.LatheConfig;
import ARMachines.rollingMachine.EntityRollingMachine;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(ARMachines.MODID)
public class ARMachines {
    public static final String MODID = "armachines";

    public ARMachines(IEventBus modEventBus, ModContainer modContaine) {
        //NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::loadComplete);
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::registerNetworkStuff);
        MultiblockRegistry.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);


    }

    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent login) {
        if (login.getEntity() instanceof ServerPlayer p) {
            LatheConfig.INSTANCE.SyncConfig(p);
        }
    }
    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        MultiblockRegistry.registerRenderers(event);
    }
    public void registerNetworkStuff(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        LatheConfig.PacketConfigSync.register(registrar);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent e) {
        MultiblockRegistry.addCreative(e);
    }

    private void loadComplete(FMLLoadCompleteEvent e) {

        itemHoloProjector.registerMultiblock("Lathe", EntityLathe.structure, EntityLathe.charMapping);
        itemHoloProjector.registerMultiblock("Rolling Machine", EntityRollingMachine.structure, EntityRollingMachine.charMapping);
        //itemHoloProjector.registerMultiblock("Crystallizer", EntityCrystallizer.structure, EntityCrystallizer.charMapping);
        //itemHoloProjector.registerMultiblock("Electrolyzer", EntityElectrolyzer.structure, EntityElectrolyzer.charMapping);
    }
}
