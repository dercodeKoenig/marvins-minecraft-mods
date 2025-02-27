package AgeOfSteam;

import AgeOfSteam.Blocks.Mechanics.Axle.RenderAxle;
import AgeOfSteam.Blocks.Mechanics.Axle.RenderWoodenAxle;
import AgeOfSteam.Blocks.Mechanics.CrankShaft.RenderBigWoodenCrankShaft;
import AgeOfSteam.Blocks.Mechanics.CrankShaft.RenderSmallWoodenCrankShaft;
import AgeOfSteam.Blocks.Mechanics.DistributorGearbox.RenderWoodenDistributorGearbox;
import AgeOfSteam.Blocks.Mechanics.FlyWheel.RenderWoodenFlyWheel;
import AgeOfSteam.Blocks.Mechanics.BlockMotor.RenderMotor;
import AgeOfSteam.Blocks.Mechanics.FlyWheel.RenderWoodenFlyWheelLarge;
import AgeOfSteam.Blocks.Mechanics.Gearbox.RenderWoodenGearbox;
import AgeOfSteam.Blocks.Mechanics.HandGenerator.RenderHandGenerator;

import AgeOfSteam.Blocks.Mechanics.TJunction.RenderWoodenTJunction;
import AgeOfSteam.Config.Config;
import AgeOfSteam.Config.PacketConfigSync;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.io.IOException;

import static AgeOfSteam.Registry.*;
import static AgeOfSteam.Static.POSITION_COLOR_TEXTURE_NORMAL_LIGHT;


@Mod(Main.MODID)
public class Main {

public static final String MODID ="age_of_steam";

    public Main(IEventBus modEventBus, ModContainer modContaine) {
        //modEventBus.register(this);

        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::loadComplete);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::RegisterCapabilities);
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::registerNetworkStuff);
        Registry.register(modEventBus);


    }

    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent login){
        if(login.getEntity() instanceof ServerPlayer p){
            Config.INSTANCE.SyncConfig(p);
        }
    }

    public void onClientSetup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(WOODEN_DISTRIBUTOR_GEARBOX.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(WOODEN_GEARBOX.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(WOODEN_TJUNCTION.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(WOODEN_AXLE_ENCASED.get(), RenderType.cutout());
    }


    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ENTITY_WOODEN_AXLE.get(), RenderWoodenAxle::new);
        event.registerBlockEntityRenderer(ENTITY_WOODEN_AXLE_ENCASED.get(), RenderWoodenAxle::new);
        event.registerBlockEntityRenderer(ENTITY_WOODEN_FLYWHEEL.get(), RenderWoodenFlyWheel::new);
        event.registerBlockEntityRenderer(ENTITY_WOODEN_FLYWHEEL_LARGE.get(), RenderWoodenFlyWheelLarge::new);
        event.registerBlockEntityRenderer(ENTITY_SMALL_WOODEN_CRANKSHAFT.get(), RenderSmallWoodenCrankShaft::new);
        event.registerBlockEntityRenderer(ENTITY_BIG_WOODEN_CRANKSHAFT.get(), RenderBigWoodenCrankShaft::new);
        event.registerBlockEntityRenderer(ENTITY_WOODEN_DISTRIBUTOR_GEARBOX.get(), RenderWoodenDistributorGearbox::new);
        event.registerBlockEntityRenderer(ENTITY_WOODEN_GEARBOX.get(), RenderWoodenGearbox::new);
        event.registerBlockEntityRenderer(ENTITY_MOTOR.get(), RenderMotor::new);
        event.registerBlockEntityRenderer(ENTITY_HAND_GENERATOR.get(), RenderHandGenerator::new);
        event.registerBlockEntityRenderer(ENTITY_WOODEN_TJUNCTION.get(), RenderWoodenTJunction::new);

    }

    public void registerNetworkStuff(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        PacketConfigSync.register(registrar);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTab().equals(AOS_CREATIVETAB.get())) {
            e.accept(WOODEN_AXLE.get());
            e.accept(WOODEN_AXLE_ENCASED.get());
            e.accept(WOODEN_FLYWHEEL.get());
            e.accept(WOODEN_FLYWHEEL_LARGE.get());
            e.accept(SMALL_WOODEN_CRANKSHAFT.get());
            e.accept(BIG_WOODEN_CRANKSHAFT.get());
            e.accept(WOODEN_DISTRIBUTOR_GEARBOX.get());
            e.accept(WOODEN_GEARBOX.get());
            e.accept(MOTOR.get());
            e.accept(CLUTCH.get());
            e.accept(HAND_GENERATOR.get());
            e.accept(WOODEN_TJUNCTION.get());

            e.accept(CASING.get());
            e.accept(CASING_SLAB.get());
            e.accept(ITEM_WOODEN_HAMMER.get());
            e.accept(ITEM_WOODEN_GEAR.get());
        }
    }

    private void RegisterCapabilities(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ENTITY_MOTOR.get(), (x, y) -> (x));
    }

    private void loadComplete(FMLLoadCompleteEvent e) {
    }
}