package AgeOfSteam.Blocks.Mechanics.BlockMotor;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.*;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockEntityBattery;
import ARLib.utils.VertexBufferCleaner;
import AgeOfSteam.Core.AbstractMechanicalBlock;
import AgeOfSteam.Core.IMechanicalBlockProvider;
import AgeOfSteam.Main;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import static AgeOfSteam.Registry.ENTITY_MOTOR;
import static AgeOfSteam.Registry.SOUND_MOTOR;
import static AgeOfSteam.Static.*;


public class EntityMotor extends BlockEntity implements IMechanicalBlockProvider, INetworkTagReceiver, IEnergyStorage {

    public  VertexBuffer vertexBuffer;
    public  MeshData mesh;
    public int lastLight = -1;


    double n = 1;

    public double MOTOR_BASE_FRICTION = 5*Math.sqrt(n);
    public double K = 10*n;
    public double HEAT_CAPACITY_TIMES_MASS_CONSTANT_FOR_HEAT_CALCULATIONS = 100;
    public double AREA_FOR_HEAT_RADIATION = 1;
    public double WIRE_RESISTANCE_FOR_HEAT_GENERATION = 5*n;

    double TARGET_HEAT = 300;
    double MAX_HEAT = 500;
    double MAX_RPM = 200/Math.sqrt(n);

    public double myInertia = 10*n;
    int rfPerTick = 500;

    double maxHeatRad = (Math.pow(MAX_HEAT*0.95, 4) - Math.pow(TARGET_HEAT, 4)) * SB_CONSTANT * AREA_FOR_HEAT_RADIATION;
    double maxConstantTorqueAllowedBeforeOverheat = Math.sqrt(maxHeatRad / WIRE_RESISTANCE_FOR_HEAT_GENERATION) * K;


    /**
     * I use F = Fmax - k * V to get the current force
     * Fmax = k * Vmax
     * I use P = Fmax * Vmax / 4 because:
     * when using Pin = Pout:
     * Pin = F*V
     * Pin = (Fmax - kV) * V
     * This will have its maximum at V = Vmax/2 with F*V = P/4
     * So... I spent a lot of time figuring out why P = Fmax * Vmax has this horrible efficiency of 25%
     * At the end, I came to conclusion that I will just multiply Fmax by 4 to scale it to 100% max efficiency
     *
     * @param p power
     * @param k motor torque to speed constant
     * @return Fmax
     */
    public static double Fmax_from_p_and_k(double p, double k) {
        return Math.sqrt(4 * p * k);
    }

    double currentForceProduced;
    double currentResistance;
    int directionMultiplier = 1;

    double currentHeat = 300;

    int serverLastHeatlvlForVisualEffects;
    int serverLastRPMForVisualEffects;
    int clientHeatlvlForVisualEffects;
    int clientRPMForVisualEffects;


    GuiHandlerBlockEntity guiHandler;
    BlockEntityBattery energyStorage;

    guiModuleEnergy e1;
    guiModuleRotationalProgress rpm;
    guiModuleRotationalProgress torque;
    guiModuleRotationalProgress efficiency;
    guiModuleVerticalProgressBar heat;
    guiModuleText torqueText;
    guiModuleText RPMText;
    guiModuleText efficiencyText;

    guiModuleText currentPowerText;
    guiModuleButton increasePower;
    guiModuleButton decreasePower;

    guiModuleText invertRotationText;
    guiModuleButton invertRotation;

    public EntityMotor(BlockPos pos, BlockState blockState) {
        super(ENTITY_MOTOR.get(), pos, blockState);

        //System.out.println("max constant torque before overheat:" + maxConstantTorqueAllowedBeforeOverheat);

        energyStorage = new BlockEntityBattery(this, 10000);

        guiHandler = new GuiHandlerBlockEntity(this);
        //e1 = new guiModuleEnergy(0,this,guiHandler,10,10);
        //guiHandler.getModules().add(e1);

        rpm = new guiModuleRotationalProgress(1, guiHandler, 30, 10);
        rpm.bg = ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/simple_scale_round_red_end.png");
        guiHandler.getModules().add(rpm);

        torque = new guiModuleRotationalProgress(2, guiHandler, 90, 10);
        torque.bg = ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/simple_scale_round_red_line_at_61.png");
        guiHandler.getModules().add(torque);

        heat = new guiModuleVerticalProgressBar(3, guiHandler, 10, 10);
        heat.bar = ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_vertical_progress_bar_i.png");
        guiHandler.getModules().add(heat);

        torqueText = new guiModuleText(4, "TORQUE", guiHandler, 100, 50, 0xFF000000, false);
        guiHandler.getModules().add(torqueText);
        RPMText = new guiModuleText(5, "RPM", guiHandler, 35, 50, 0xFF000000, false);
        guiHandler.getModules().add(RPMText);

        currentPowerText = new guiModuleText(6, rfPerTick + " RF/tick", guiHandler, 45, 74, 0xFF000000, false);
        guiHandler.getModules().add(currentPowerText);

        increasePower = new guiModuleButton(7, "+50", guiHandler, 110, 70, 30, 14, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_button_black.png"), 64, 20);
        increasePower.color = 0xFFFFFFFF;
        guiHandler.getModules().add(increasePower);

        decreasePower = new guiModuleButton(8, "-50", guiHandler, 10, 70, 30, 14, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_button_black.png"), 64, 20);
        decreasePower.color = 0xFFFFFFFF;
        guiHandler.getModules().add(decreasePower);

        efficiency = new guiModuleRotationalProgress(9,guiHandler,150,10);
        guiHandler.getModules().add(efficiency);

        efficiencyText = new guiModuleText(10, "", guiHandler, 155, 50, 0xFF000000, false);
        guiHandler.getModules().add(efficiencyText);

        invertRotation = new guiModuleButton(11, directionMultiplier > 0 ? "+":"-", guiHandler, 155, 70, 30,15,ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_button_black.png"), 64, 20);
        invertRotation.color = 0xFFFFFFFF;
        guiHandler.getModules().add(invertRotation);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                VertexBufferCleaner.register(this, vertexBuffer);
            });
        }
    }

    public void openGui() {
        if (level.isClientSide)
            guiHandler.openGui(210, 90, true );
    }

    public AbstractMechanicalBlock myMechanicalBlock = new AbstractMechanicalBlock(0, this) {
        @Override
        public double getMaxStress() {
            return Double.MAX_VALUE;
        }

        @Override
        public double getInertia(Direction face) {
            return myInertia;
        }

        @Override
        public double getTorqueResistance(Direction face) {
            return currentResistance;
        }

        @Override
        public double getTorqueProduced(Direction face) {
            return currentForceProduced;
        }

        @Override
        public double getRotationMultiplierToInside(@org.jetbrains.annotations.Nullable Direction receivingFace) {
            return 1;
        }
    };

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        myMechanicalBlock.mechanicalOnload();
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();

        if (!level.isClientSide) {
            guiHandler.serverTick();

            int torque = 0;
            double efficiency = 0;
            if (level.hasNeighborSignal(getBlockPos())) {
                double facingMultiplier = getBlockState().getValue(BlockMotor.FACING).getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
                int maxConsumedEnergy = Math.min(getEnergyStored(), rfPerTick);
                double workingForce = Fmax_from_p_and_k(maxConsumedEnergy, K) - K * myMechanicalBlock.internalVelocity * facingMultiplier * directionMultiplier;
                workingForce = Math.max(0, workingForce);
                currentForceProduced = workingForce * facingMultiplier * directionMultiplier;
                currentResistance = MOTOR_BASE_FRICTION;
                energyStorage.setEnergy(getEnergyStored() - maxConsumedEnergy);

                currentHeat += Math.pow(Math.abs(workingForce) / K, 2) * WIRE_RESISTANCE_FOR_HEAT_GENERATION / TPS / HEAT_CAPACITY_TIMES_MASS_CONSTANT_FOR_HEAT_CALCULATIONS;

                torque = (int) Math.round(Math.abs(workingForce));
                efficiency = Math.abs(currentForceProduced * myMechanicalBlock.internalVelocity) / (rfPerTick+0.01);
            } else {
                currentForceProduced = 0;
                double maxWorkingResistance = Math.abs(-K * myMechanicalBlock.internalVelocity);
                int energyMaxProduced = (int) (Math.abs(myMechanicalBlock.internalVelocity) * maxWorkingResistance);
                int freeEnergyCapacity = getMaxEnergyStored() - getEnergyStored();
                int energyProduced = Math.min(freeEnergyCapacity, energyMaxProduced);
                energyStorage.setEnergy(getEnergyStored() + energyProduced);
                currentResistance = MOTOR_BASE_FRICTION;

                if (energyMaxProduced > 0) {
                    double actualTorqueProduced = maxWorkingResistance * energyProduced / energyMaxProduced;
                    currentResistance += actualTorqueProduced;
                    torque = (int) -Math.round(Math.abs(actualTorqueProduced));
                    currentHeat += Math.pow(actualTorqueProduced / K, 2) * WIRE_RESISTANCE_FOR_HEAT_GENERATION / TPS / HEAT_CAPACITY_TIMES_MASS_CONSTANT_FOR_HEAT_CALCULATIONS;
                }

                if (getEnergyStored() > 0) {
                    for (Direction i : Direction.allShuffled(level.random)) {
                        int toExtract = getEnergyStored();
                        BlockPos o = getBlockPos().relative(i);
                        IEnergyStorage e = level.getCapability(Capabilities.EnergyStorage.BLOCK, o, null);
                        if (e != null) {
                            int extracted = e.receiveEnergy(toExtract, false);
                            extractEnergy(extracted, false);
                        }
                    }
                }
            }


            double heat_radiate_energy = Math.clamp((Math.pow(currentHeat, 4) - Math.pow(TARGET_HEAT, 4)) * SB_CONSTANT * AREA_FOR_HEAT_RADIATION, -1000000, 1000000);
            double tempDiffCreated = heat_radiate_energy / TPS / HEAT_CAPACITY_TIMES_MASS_CONSTANT_FOR_HEAT_CALCULATIONS;

            currentHeat -= tempDiffCreated;

            double heatProgress = (currentHeat - 272) / (MAX_HEAT - 272);
            heat.setProgressAndSync(heatProgress);
            heat.setHoverInfoAndSync((Math.round(currentHeat) - 272) + "°C");

            double dps = Math.abs(rad_to_degree(myMechanicalBlock.internalVelocity));
            double rps = dps / 360;
            double rpm = rps * 60;

            this.rpm.setProgressAndSync(rpm / MAX_RPM);
            this.RPMText.setTextAndSync("RPM: " + Math.round(rpm));

            torqueText.setTextAndSync("T: " + torque);
            this.torque.setProgressAndSync(Math.abs(torque) / maxConstantTorqueAllowedBeforeOverheat * 0.61);

            this.efficiency.setProgressAndSync(efficiency);
            this.efficiencyText.setTextAndSync("eff: "+Math.round(efficiency*100)+"%");


            int heatlvl = (int) Math.round(heatProgress*10);
            if(serverLastHeatlvlForVisualEffects != heatlvl){
                serverLastHeatlvlForVisualEffects = heatlvl;
                CompoundTag heatlvlupdate = new CompoundTag();
                heatlvlupdate.putInt("heatLvl", serverLastHeatlvlForVisualEffects);
                PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, heatlvlupdate));
            }
            if(heatlvl > 10){
                level.destroyBlock(getBlockPos(), false);
                level.explode(null,getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(),2,true, Level.ExplosionInteraction.BLOCK);
                level.setBlock(getBlockPos(), Blocks.FIRE.defaultBlockState(),3);
            }
            int rpmlvl = (int)Math.round(rpm/MAX_RPM*10);
            //  currently does nothing
            if(serverLastRPMForVisualEffects != rpmlvl){
                serverLastRPMForVisualEffects = rpmlvl;
                CompoundTag heatlvlupdate = new CompoundTag();
                heatlvlupdate.putInt("rpmLvl", serverLastRPMForVisualEffects);
                PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, heatlvlupdate));
            }
             //*/
            if(rpmlvl > 10){
                level.destroyBlock(getBlockPos(), false);
                level.explode(null,getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(),2,true, Level.ExplosionInteraction.BLOCK);
                level.setBlock(getBlockPos(), Blocks.FIRE.defaultBlockState(),3);
            }
        }




        int particleNum = Math.max(clientHeatlvlForVisualEffects -8, 0);
        for (int i = 0; i < particleNum; i++) {
            double x = level.random.nextDouble() - 0.5;
            double y = level.random.nextDouble() - 0.5;
            double z = level.random.nextDouble() - 0.5;
            level.addParticle(new DustParticleOptions( new Vector3f(0.2f, 0.2f, 0.2f),1f), getBlockPos().getCenter().x + x, getBlockPos().getCenter().y + 0.5 + y, getBlockPos().getCenter().z + z, x, y, z);
        }
        if(serverLastRPMForVisualEffects > 5 && level.hasNeighborSignal(getBlockPos())) {
            if((level.getGameTime() & 5) == 0){
                double relativeSpeed = Math.abs(rad_to_degree(myMechanicalBlock.internalVelocity)) / 6 / MAX_RPM;
                level.playSound((Entity) null, getBlockPos(),  SOUND_MOTOR.get(),
                        SoundSource.BLOCKS, 0.5f * (float) (Math.max(0, relativeSpeed - 0.5)), (float)(relativeSpeed-0.5)*4f);  //
                }
        }
    }


    @Override
    public void readClient(CompoundTag tag) {
        //System.out.println("readClient:"+tag);
        myMechanicalBlock.mechanicalReadClient(tag);
        guiHandler.readClient(tag);
        if(tag.contains("heatLvl")){
            clientHeatlvlForVisualEffects = tag.getInt("heatLvl");
        }
        if(tag.contains("rpmLvl")){
            clientRPMForVisualEffects = tag.getInt("rpmLvl");
        }
    }

    @Override
    public void readServer(CompoundTag tag, ServerPlayer p) {
        //System.out.println("readServer:"+tag);
        myMechanicalBlock.mechanicalReadServer(tag, p);
        guiHandler.readServer(tag);

        if (tag.contains("guiButtonClick")) {
            int id = tag.getInt("guiButtonClick");
            if (id == 7) {
                rfPerTick += 50;
            }
            if (id == 8) {
                rfPerTick -= 50;
            }
            rfPerTick = Math.max(0, rfPerTick);
            this.currentPowerText.setTextAndSync(rfPerTick + " RF/tick");

            if(id == 11){
                directionMultiplier =directionMultiplier > 0 ? -1:1;
                invertRotation.setTextAndSync(directionMultiplier > 0 ? "+":"-");
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
        directionMultiplier = tag.getInt("direction");
        invertRotation.setTextAndSync(directionMultiplier > 0 ? "+":"-");
        rfPerTick = tag.getInt("rfpt");
        currentPowerText.setTextAndSync(rfPerTick + " RF/tick");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
        tag.putInt("direction", directionMultiplier);
        tag.putInt("rfpt", rfPerTick);

    }

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction side) {
        BlockState myState = getBlockState();
        if (myState.getBlock() instanceof BlockMotor) {
            if (side == myState.getValue(BlockMotor.FACING))
                return myMechanicalBlock;
        }
        return null;
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityMotor) t).tick();
    }

    @Override
    public int receiveEnergy(int i, boolean b) {
        return energyStorage.receiveEnergy(i, b);
    }

    @Override
    public int extractEnergy(int i, boolean b) {
        return energyStorage.extractEnergy(i, b);
    }

    @Override
    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return energyStorage.canExtract();
    }

    @Override
    public boolean canReceive() {
        return energyStorage.canReceive();
    }
}