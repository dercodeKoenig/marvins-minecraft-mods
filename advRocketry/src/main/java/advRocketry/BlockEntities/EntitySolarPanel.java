package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleEnergy;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.gui.modules.guiModuleText;
import ARLib.utils.BlockEntityBattery;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

import static advRocketry.Registry.ENTITY_CARGO_HOLD;
import static advRocketry.Registry.ENTITY_SOLAR_PANEL;

public class EntitySolarPanel extends BlockEntity implements ARLib.network.INetworkTagReceiver {

    public GuiHandlerBlockEntity guiHandler;
    public BlockEntityBattery battery;
    float partialRf = 0;
    guiModuleText infoText;

    public EntitySolarPanel(BlockPos pos, BlockState blockState) {
        super(ENTITY_SOLAR_PANEL.get(), pos, blockState);
        guiHandler = new GuiHandlerBlockEntity(this);
        battery = new BlockEntityBattery(this,1000);
        battery.canReceive = false;
        guiHandler.modules.add(new guiModuleEnergy(0,battery,guiHandler,10,10));
        infoText = new guiModuleText(1,"",guiHandler, 30,30,0xff000000,false);
        guiHandler.modules.add(infoText);
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("battery", battery.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        battery.deserializeNBT(registries, tag.getCompound("battery"));
    }

    public float getGenerationSpeed(){
        ResourceLocation levelId = level.dimension().location();
        Dimension dim = DimensionManager.INSTANCE_SERVER.get(levelId);
        double accumulatedStarlightIntensity = 0;
        if(dim instanceof PlanetDimension planetDimension){
            Vec3 up = planetDimension.getGlobalAxisDirections(0,planetDimension.getLatitudeFromZPosition(getBlockPos().getZ())).up.normalize();
            Vec3 myPos = planetDimension.getPosition(0);
            for(ResourceLocation starId : planetDimension.getCurrentMainStars()){
                if(DimensionManager.INSTANCE_SERVER.get(starId) instanceof PlanetDimension star){
                    Vec3 planetToStar = star.getPosition(0).subtract(myPos);
                    double distance = planetToStar.length();
                    double dot = Math.max(0,up.dot(planetToStar.normalize()));
                    double intensity = star.getRadiationIntensity();
                    double atmModifier = 1 - (planetDimension.getAtmosphereDensity() / (1+planetDimension.getAtmosphereDensity()));
                    double finalIntensity = dot * intensity * atmModifier / (distance * distance);
                    accumulatedStarlightIntensity += finalIntensity;
                }
            }
        }
        return (float) accumulatedStarlightIntensity;
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();
            float generationSpeed = getGenerationSpeed();
            partialRf += generationSpeed;
            int toProduce = (int) partialRf;
            partialRf -= toProduce;
            battery.receiveEnergy(toProduce, false);
            if(!guiHandler.playersTrackingGui.isEmpty()){
                infoText.setTextAndSync(Math.round(generationSpeed * 100) / 100 +" rf / tick");
            }
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySolarPanel) t).tick();
    }

    public void openGui() {
        if (level.isClientSide)
            guiHandler.openGui(176, 70, true);
    }
}
