package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleEnergy;
import ARLib.gui.modules.guiModuleText;
import ARLib.utils.BlockEntityBattery;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import static advRocketry.Registry.BlockEntities.ENTITY_SOLAR_PANEL;

public class EntitySolarPanel extends BlockEntity implements ARLib.network.INetworkTagReceiver {

    public GuiHandlerBlockEntity guiHandler;
    public BlockEntityBattery battery;
    float partialRf = 0;
    guiModuleText infoText;

    public EntitySolarPanel(BlockPos pos, BlockState blockState) {
        super(ENTITY_SOLAR_PANEL.get(), pos, blockState);
        guiHandler = new GuiHandlerBlockEntity(this);
        battery = new BlockEntityBattery(this, 1000);
        guiHandler.modules.add(new guiModuleEnergy(0, battery, guiHandler, 10, 10));
        infoText = new guiModuleText(1, "", guiHandler, 30, 30, 0xff000000, false);
        guiHandler.modules.add(infoText);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySolarPanel) t).tick();
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
        battery.deserializeNBT(registries, tag.get("battery"));
    }

    public float getGenerationSpeed() {

        if (!level.canSeeSky(getBlockPos().above()))
            return 0;

        ResourceLocation levelId = level.dimension().location();
        Dimension dim = DimensionManager.INSTANCE_SERVER.get(levelId);
        if (dim == null)
            return 0;

        double accumulatedStarlightIntensity = 0;
        Vec3 up;
        if (dim instanceof PlanetDimension planetDimension) {
            up = planetDimension.getGlobalAxisDirections(0, planetDimension.getLatitudeFromZPosition(getBlockPos().getZ())).up.normalize();
        } else {
            up = dim.getGlobalAxisDirections(0).up.normalize();
        }

        Vec3 myPos = dim.getPosition(0);
        for (ResourceLocation starId : dim.getCurrentMainStars()) {
            if (DimensionManager.INSTANCE_SERVER.get(starId) instanceof PlanetDimension star) {
                Vec3 planetToStar = star.getPosition(0).subtract(myPos);
                double distance = planetToStar.length();
                double dot = Math.max(0, up.dot(planetToStar.normalize()));
                double intensity = star.getRadiationIntensity();
                double atmModifier = 1 - (dim.getAtmosphereDensity() / (1 + dim.getAtmosphereDensity()));
                double finalIntensity = dot * intensity * atmModifier / (distance * distance);
                accumulatedStarlightIntensity += finalIntensity;
            }
        }

        // with normal atmosphere density (1) and sun intensity (2) and normal distance (1AU)
        // this should make it produce around 2.5rf/tick during noon
        double multiplier = 2.5;
        return (float) Math.min(10,accumulatedStarlightIntensity * multiplier);
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();

            int remainingCapacity = battery.getMaxEnergyStored() - battery.getEnergyStored();
            if (remainingCapacity > 0) {
                float generationSpeed = getGenerationSpeed();
                partialRf += generationSpeed;
                int toProduce = (int) partialRf;
                partialRf -= toProduce;
                battery.receiveEnergy(toProduce, false);
                String text = battery.getEnergyStored() + " rf\n\n" +
                        (float) Math.round(generationSpeed * 1000) / 1000 + " rf / tick";
                infoText.setTextAndSync(text);
            }else{
                infoText.setTextAndSync(battery.getEnergyStored() + " rf");
            }

            // output to other energy handlers
            for (Direction i : Direction.values()) {
                if (i == Direction.UP)
                    continue;
                if(battery.getEnergyStored() == 0)
                    break;

                IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK, getBlockPos().relative(i), i.getOpposite());
                if (neighbor instanceof BlockEntityBattery otherBattery && otherBattery.parent instanceof EntitySolarPanel otherPanel) {
                    int otherPanelEnergy = otherPanel.battery.getEnergyStored();
                    int myEnergy = battery.getEnergyStored();
                    int diff = myEnergy - otherPanelEnergy;
                    if(diff > 5){
                        // move some into the other panel
                        int toMove = diff / 2;
                        int received = neighbor.receiveEnergy(toMove,false);
                        battery.extractEnergy(received, false);
                    }
                } else if (neighbor != null) {
                    int received = neighbor.receiveEnergy(battery.getEnergyStored(),false);
                    battery.extractEnergy(received, false);
                }
            }
        }
    }

    public void openGui() {
        if (level.isClientSide)
            guiHandler.openGui(176, 70, true);
    }
}
