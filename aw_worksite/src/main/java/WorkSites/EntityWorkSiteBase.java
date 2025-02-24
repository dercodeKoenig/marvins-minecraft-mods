package WorkSites;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.ModularScreen;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleDefaultButton;
import ARLib.gui.modules.guiModuleImage;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import AgeOfSteam.Core.AbstractMechanicalBlock;
import AgeOfSteam.Core.IMechanicalBlockProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.util.*;

public abstract class EntityWorkSiteBase extends BlockEntity implements IMechanicalBlockProvider, INetworkTagReceiver {

    //config
    public static boolean allowMechanicalEnergy = Config.INSTANCE.allow_mechanical_energy;

    public int maxSize = 16; // can be modified with bounds upgrades
    public int minSize = 1;

    public double k = Config.INSTANCE.k;

    public int maxWorkersAllowed = 2; // for npc module
    // config end

    public static Set<BlockPos> knownWorkSites = new HashSet<>();
    public HashMap<Entity, Integer> workersWorkingHereWithTimeout = new HashMap<>();

    public GuiHandlerBlockEntity guiHandlerMain;
    public GuiHandlerBlockEntity guiHandlerBounds;

    public int w = 5;
    public int h = 5;
    public int controllerOffsetW = 0;
    public int controllerOffsetH = 0;

    public Set<Vector2i> blackList = new HashSet<>();
    public Set<BlockPos> blackListAsBlockPos = new HashSet<>();
    public Set<BlockPos> allowedBlocks = new HashSet<>();
    public List<BlockPos> allowedBlocksList = new ArrayList<>();

    public int renderInfoTimer = 0;

    public BlockPos pmin;
    public BlockPos pmax;

    public IEnergyStorage battery = new EnergyStorage(Config.INSTANCE.max_energy_storage) {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int r = super.receiveEnergy(toReceive, simulate);
            if (!simulate) setChanged();
            return r;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int r = super.extractEnergy(toExtract, simulate);
            if (!simulate) setChanged();
            return r;
        }
    };

    public double currentResistance;
    public AbstractMechanicalBlock myMechanicalBlock = new AbstractMechanicalBlock(0, this) {
        @Override
        public double getMaxStress() {
            return 999999;
        }

        @Override
        public double getInertia(Direction direction) {
            return 1;
        }

        @Override
        public double getTorqueResistance(Direction direction) {
            return currentResistance;
        }

        @Override
        public double getTorqueProduced(Direction direction) {
            return 0;
        }

        @Override
        public double getRotationMultiplierToInside(@Nullable Direction direction) {
            return 1;
        }
    };

    public EntityWorkSiteBase(BlockEntityType type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        guiHandlerMain = new GuiHandlerBlockEntity(this);

        pmax = new BlockPos(0, 0, 0);
        pmin = new BlockPos(0, 0, 0);

        guiModuleDefaultButton openBoundsGuiButton = new guiModuleDefaultButton(0, "bounds", guiHandlerMain, 10, 10, 40, 15) {
            @Override
            public void onButtonClicked() {
                guiHandlerBounds.openGui(200, 200, true);
            }
        };
        guiHandlerMain.getModules().add(openBoundsGuiButton);

        guiHandlerBounds = new GuiHandlerBlockEntity(this) {
            @Override
            public void onGuiClose() {
                EntityWorkSiteBase.this.openMainGui();
            }
        };
    }

    @Override
    public void onLoad() {
        if (level.isClientSide) {
            CompoundTag t = new CompoundTag();
            t.put("ping", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, t));
        } else {
            updateBoundsBp();
            myMechanicalBlock.mechanicalOnload();
           EntityWorkSiteBase.knownWorkSites.add(getBlockPos());
        }
    }
    @Override
    public void setRemoved(){
        EntityWorkSiteBase.knownWorkSites.remove(getBlockPos());
super.setRemoved();
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();
        if (!level.isClientSide) {
            guiHandlerMain.serverTick();

            // tick mechanical energy
            if(allowMechanicalEnergy) {
                double maxWorkingResistance = Math.abs(this.k * this.myMechanicalBlock.internalVelocity);
                int energyMaxProduced = (int) (Math.abs(this.myMechanicalBlock.internalVelocity) * maxWorkingResistance);
                int freeEnergyCapacity = battery.getMaxEnergyStored() - battery.getEnergyStored();
                int energyProduced = Math.min(freeEnergyCapacity, energyMaxProduced);
                battery.receiveEnergy(energyProduced, false);
                this.currentResistance = 0;
                if (energyProduced > 0) {
                    double actualResistanceProduced = maxWorkingResistance * (double) energyProduced / (double) energyMaxProduced;
                    this.currentResistance += actualResistanceProduced;
                }
            }

            // timeout workers for npc module
                for (Entity e : workersWorkingHereWithTimeout.keySet()){
                    int ticks = workersWorkingHereWithTimeout.get(e);
                    workersWorkingHereWithTimeout.put(e, ticks+1);
                    if(ticks > 100){
                        workersWorkingHereWithTimeout.remove(e);
                        break;
                    }
                }
        }
        if (level.isClientSide) {
            if (renderInfoTimer > 0) {
                renderInfoTimer--;
            }
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityWorkSiteBase) t).tick();
    }

    public void updateBlackList(){
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos p1 = getBlockPos().relative(facing,controllerOffsetH-1);
        p1 = p1.relative(facing.getClockWise(), controllerOffsetW);

        // remove blacklist entries outside bounds
        boolean sth = true;
        while (sth) {
            sth = false;
            for (Vector2i i : blackList) {
                if (i.x >= w || i.y >= h) {
                    blackList.remove(i);
                    sth = true;
                    break;
                }
            }
        }

        // compute blacklist blockpos to render
        blackListAsBlockPos.clear();
        for (Vector2i i : blackList) {
            BlockPos blocked = p1.relative(facing.getCounterClockWise(), i.x).relative(facing.getOpposite(), i.y);
            blocked = new BlockPos(blocked.getX(), getBlockPos().getY(), blocked.getZ());
            blackListAsBlockPos.add(blocked);
        }
    }

    public void updateAllowedBlocksList() {
        // compute allowed blockpos
        allowedBlocks.clear();
        for (int z = pmin.getZ(); z <= pmax.getZ(); z++) {
            for (int x = pmin.getX(); x <= pmax.getX(); x++) {
                for (int y = pmin.getY(); y <= pmax.getY(); y++) {
                    BlockPos target = new BlockPos(x, y, z);
                    if (!blackListAsBlockPos.contains(new BlockPos(x,getBlockPos().getY(),z))) {
                        allowedBlocks.add(target);
                    }
                }
            }
        }
        allowedBlocksList = allowedBlocks.stream().toList();
    }

    public void updateBoundsBp() {
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos p1 = getBlockPos().relative(facing,controllerOffsetH-1);
        p1 = p1.relative(facing.getClockWise(), controllerOffsetW);
        BlockPos p2 = p1.relative(facing.getCounterClockWise(), w - 1).relative(facing.getOpposite(), h - 1);

        pmin = new BlockPos(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()));
        pmax = new BlockPos(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()));

        updateBlackList();
        updateAllowedBlocksList();
    }

    public void updateGuiModules() {
        guiHandlerBounds.getModules().clear();

        ResourceLocation red = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/gui/red.png");
        ResourceLocation blue = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/gui/blue.png");
        ResourceLocation black = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/gui/black.png");

        int baseOffsetX = 30;
        int baseOffsetY = 30;

        int pxPerBlock = 140 / maxSize;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int _x = x;
                int _y = h - y - 1; // because highest y is directly next to the controller
                if (blackList.contains(new Vector2i(_x, _y))) {
                    guiModuleButton b = new guiModuleButton(-1, "", guiHandlerBounds, x * pxPerBlock + baseOffsetX, y * pxPerBlock + baseOffsetY, pxPerBlock, pxPerBlock, black, 1, 1) {
                        @Override
                        public void onButtonClicked() {
                            CompoundTag i = new CompoundTag();
                            CompoundTag j = new CompoundTag();
                            j.putInt("x", _x);
                            j.putInt("y", _y);
                            i.put("blacklist_remove", j);
                            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityWorkSiteBase.this, i));
                        }
                    };
                    guiHandlerBounds.getModules().add(b);
                } else {
                    guiModuleButton b = new guiModuleButton(-1, "", guiHandlerBounds, x * pxPerBlock + baseOffsetX, y * pxPerBlock + baseOffsetY, pxPerBlock, pxPerBlock, red, 1, 1) {
                        @Override
                        public void onButtonClicked() {
                            CompoundTag i = new CompoundTag();
                            CompoundTag j = new CompoundTag();
                            j.putInt("x", _x);
                            j.putInt("y", _y);
                            i.put("blacklist_add", j);
                            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityWorkSiteBase.this, i));
                        }
                    };
                    guiHandlerBounds.getModules().add(b);
                }
            }
        }

        int controllerX = baseOffsetX + controllerOffsetW * pxPerBlock;
        int controllerY = baseOffsetY + h * pxPerBlock-controllerOffsetH*pxPerBlock;
        guiModuleImage controllerPos = new guiModuleImage(guiHandlerBounds, controllerX, controllerY, pxPerBlock, pxPerBlock, blue, 1, 1);
        guiHandlerBounds.getModules().add(controllerPos);

        guiModuleDefaultButton hinc = new guiModuleDefaultButton(101, "h+", guiHandlerBounds, 5, 10, 15, 15);
        guiModuleDefaultButton hdec = new guiModuleDefaultButton(102, "h-", guiHandlerBounds, 30, 10, 15, 15);
        guiHandlerBounds.getModules().add(hinc);
        guiHandlerBounds.getModules().add(hdec);

        guiModuleDefaultButton winc = new guiModuleDefaultButton(103, "w+", guiHandlerBounds, 55, 10, 15, 15);
        guiModuleDefaultButton wdec = new guiModuleDefaultButton(104, "w-", guiHandlerBounds, 80, 10, 15, 15);
        guiHandlerBounds.getModules().add(winc);
        guiHandlerBounds.getModules().add(wdec);

        guiModuleDefaultButton xinc = new guiModuleDefaultButton(105, "x+", guiHandlerBounds, 105, 10, 15, 15);
        guiModuleDefaultButton xdec = new guiModuleDefaultButton(106, "x-", guiHandlerBounds, 130, 10, 15, 15);
        guiHandlerBounds.getModules().add(xinc);
        guiHandlerBounds.getModules().add(xdec);

        guiModuleDefaultButton yinc = new guiModuleDefaultButton(107, "y+", guiHandlerBounds, 155, 10, 15, 15);
        guiModuleDefaultButton ydec = new guiModuleDefaultButton(108, "y-", guiHandlerBounds, 180, 10, 15, 15);
        guiHandlerBounds.getModules().add(yinc);
        guiHandlerBounds.getModules().add(ydec);


        if (guiHandlerBounds.screen instanceof ModularScreen ms) {
            ms.calculateGuiOffsetAndNotifyModules();
        }
    }

    abstract public void openMainGui();

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction direction) {
        if(allowMechanicalEnergy)
            return myMechanicalBlock;
        else return null;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }


    public CompoundTag getUpdateTag() {
        CompoundTag t = new CompoundTag();
        t.putInt("controllerOffsetW", controllerOffsetW);
        t.putInt("controllerOffsetH", controllerOffsetH);
        t.putInt("maxSize", maxSize);
        t.putInt("w", w);
        t.putInt("h", h);
        ListTag blackListTag = new ListTag();
        for (Vector2i i : blackList) {
            CompoundTag o = new CompoundTag();
            o.putInt("x", i.x);
            o.putInt("y", i.y);
            blackListTag.add(o);
        }
        t.put("blacklist", blackListTag);
        return t;
    }

    public void readUpdateTag(CompoundTag compoundTag) {
        boolean needsBoundsUpdate = false;
        if (compoundTag.contains("controllerOffsetW")) {
            controllerOffsetW = compoundTag.getInt("controllerOffsetW");
            needsBoundsUpdate = true;
        }
        if (compoundTag.contains("controllerOffsetH")) {
            controllerOffsetH = compoundTag.getInt("controllerOffsetH");
            needsBoundsUpdate = true;
        }
        if (compoundTag.contains("maxSize")) {
            maxSize = compoundTag.getInt("maxSize");
            needsBoundsUpdate = true;
        }
        if (compoundTag.contains("w")) {
            w = compoundTag.getInt("w");
            needsBoundsUpdate = true;
        }
        if (compoundTag.contains("h")) {
            h = compoundTag.getInt("h");
            needsBoundsUpdate = true;
        }
        if (compoundTag.contains("blacklist")) {
            needsBoundsUpdate = true;
            ListTag blackListTag = compoundTag.getList("blacklist", Tag.TAG_COMPOUND);
            blackList.clear();
            for (int i = 0; i < blackListTag.size(); i++) {
                CompoundTag t = blackListTag.getCompound(i);
                int x = t.getInt("x");
                int y = t.getInt("y");
                blackList.add(new Vector2i(x, y));
            }
        }
        if(needsBoundsUpdate) {
            updateGuiModules();
            updateBoundsBp();
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
        tag.put("data", getUpdateTag());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
        readUpdateTag(tag.getCompound("data"));
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer p) {
        guiHandlerMain.readServer(compoundTag);
        myMechanicalBlock.mechanicalReadServer(compoundTag, p);

        if (compoundTag.contains("ping")) {
                PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(this, getUpdateTag()));
        }
        if (compoundTag.contains("blacklist_add")) {
            CompoundTag i = compoundTag.getCompound("blacklist_add");
            Vector2i target = new Vector2i(i.getInt("x"), i.getInt("y"));
            blackList.add(target);
            updateBoundsBp();
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, getUpdateTag()));
        }
        if (compoundTag.contains("blacklist_remove")) {
            CompoundTag i = compoundTag.getCompound("blacklist_remove");
            Vector2i target = new Vector2i(i.getInt("x"), i.getInt("y"));
            blackList.remove(target);
            updateBoundsBp();
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, getUpdateTag()));
        }

        if (compoundTag.contains("guiButtonClick")) {
            int button = compoundTag.getInt("guiButtonClick");

            if (button == 101) {
                //inc h
                if (h < maxSize) h++;
            }
            if (button == 102) {
                //dec h
                if (h > minSize) h--;
            }
            if (button == 103) {
                //inc w
                if (w < maxSize) w++;
            }
            if (button == 104) {
                //dec w
                if (w > minSize) w--;
            }

            if (button == 105) {
                //inc controllerOffsetW
                if (controllerOffsetW < maxSize) controllerOffsetW++;
            }
            if (button == 106) {
                //dec controllerOffsetW
                if (controllerOffsetW > 0) controllerOffsetW--;
            }
            controllerOffsetW = Math.min(w - 1, controllerOffsetW);

            if (button == 107) {
                //inc controllerOffsetH
                if (controllerOffsetH < maxSize) controllerOffsetH++;
            }
            if (button == 108) {
                //dec controllerOffsetH
                if (controllerOffsetH > 0) controllerOffsetH--;
            }
            controllerOffsetH = Math.min(h, controllerOffsetH);

            updateBoundsBp();
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, getUpdateTag()));
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        myMechanicalBlock.mechanicalReadClient(compoundTag);
        guiHandlerMain.readClient(compoundTag);
        readUpdateTag(compoundTag);
    }
}
