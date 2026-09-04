package advRocketry.BlockEntities;

import ARLib.ARLibRegistry;
import ARLib.blockentities.EntityItemInputBlock;
import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.multiblockCore.EntityMultiblockMachineMaster;
import ARLib.multiblockCore.EntityMultiblockMaster;
import advRocketry.Config;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.SpaceStationDimension;
import advRocketry.Items.ItemDilithiumCrystal;
import advRocketry.Registry.BlockEntities;
import advRocketry.Registry.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;

public class EntityWarpCore extends EntityMultiblockMachineMaster {

    public static Object[][][] structure =
            new Object[][][]{
                    {{'a', 'a', 'a'},
                            {'a', 'i', 'a'},
                            {'a', 'a', 'a'}},
                    {{null, null, null},
                            {null, 'c', null},
                            {null, null, null}},
                    {{'a', 'a', 'a'},
                            {'a', 'g', 'a'},
                            {'a', 'a', 'a'}}
            };
    public static HashMap<Character, List<Block>> charMapping = new HashMap<>();

    static {
        charMapping.put('c', List.of(advRocketry.Registry.Blocks.WARP_CORE.get()));
        charMapping.put('a', List.of(ARLibRegistry.BLOCK_ADVANCED_STRUCTURE.get()));
        charMapping.put('g', List.of(Blocks.GOLD_BLOCK));
        charMapping.put('i', List.of(ARLibRegistry.BLOCK_ITEM_INPUT_BLOCK.get()));
    }

    public EntityWarpCore(BlockPos pos, BlockState state) {
        super(BlockEntities.ENTITY_WARP_CORE.get(), pos, state);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityWarpCore) t).tick();
    }

    public void tick() {
        // only handles particles clientside
        if (this.level == null) return;
        if (!this.level.isClientSide()) return;
        if (!isComplete()) return;

        float intensity = 0f;
        boolean isInWarp = false;
        if (DimensionManager.INSTANCE_CLIENT.get(level.dimension().location()) instanceof SpaceStationDimension spaceStation) {
            double movementRelative = spaceStation.getMovement().length() / Config.INSTANCE.station_SpaceTravel_AU_Per_Second;
            isInWarp = spaceStation.isInSpaceTravel();
            double b = 0.005;
            intensity = (float) (b + (1 - b) * Math.pow(movementRelative, 0.2));
        }

        BlockPos pos = this.getBlockPos();
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;

        // SCATTER OFFSET: Keeps adjacent cores from syncing up perfectly
        long posOffset = (pos.getX() * 3129871L) ^ (pos.getY() * 116129L) ^ (pos.getZ() * 7329871L);
        long time = this.level.getGameTime() + Math.abs(posOffset);

        RandomSource random = this.level.random;

        // ==========================================
        // EFFECT 1: Boiling Core
        // ==========================================
        float targetFlames = intensity * 5.0F;
        int flameCount = (int) targetFlames + (random.nextFloat() < (targetFlames % 1.0F) ? 1 : 0);
        if (isInWarp) {
            for (int i = 0; i < flameCount; i++) {
                double xOffset = (random.nextDouble() - 0.5D) * 0.4D;
                double yOffset = (random.nextDouble() - 0.5D) * 0.4D;
                double zOffset = (random.nextDouble() - 0.5D) * 0.4D;

                double vx = (random.nextDouble() - 0.5D) * 0.05D;
                double vy = (random.nextDouble() - 0.5D) * 0.05D;
                double vz = (random.nextDouble() - 0.5D) * 0.05D;

                this.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        x + xOffset, y + yOffset, z + zOffset,
                        vx, vy, vz);
            }
        }

        // ==========================================
        // EFFECT 2: Gyroscopic Containment Grid
        // ==========================================
        float targetSparks = intensity * 8.0F;
        int sparkCount = (int) targetSparks + (random.nextFloat() < (targetSparks % 1.0F) ? 1 : 0);

        if (sparkCount > 0 && intensity > 0.1) {
            double radius = 1.1D + (random.nextDouble() * 0.1D);

            double baseAngle = (time % 40) * (Math.PI / 20);
            double heightBob = Math.sin((time % 60) * (Math.PI / 30)) * 0.4D;

            for (int i = 0; i < sparkCount; i++) {
                double angle = baseAngle + (i * (Math.PI * 2.0D / sparkCount));

                double orbitX = x + Math.cos(angle) * radius;
                double orbitZ = z + Math.sin(angle) * radius;

                this.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        orbitX, y + heightBob, orbitZ,
                        0.0D, 0.0D, 0.0D);

            }
        }

        // ==========================================
        // EFFECT 3: Intensity-Scaled Gravitational Pull
        // ==========================================
        float targetPortals = intensity * 8.0F;
        int portalCount = (int) targetPortals + (random.nextFloat() < (targetPortals % 1.0F) ? 1 : 0);
        if (isInWarp) {
            for (int i = 0; i < portalCount; i++) {
                double spread = 2.0D + (intensity * 4.0D);

                double startOffsetX = (random.nextDouble() - 0.5D) * spread;
                double startOffsetY = (random.nextDouble() - 0.5D) * spread;
                double startOffsetZ = (random.nextDouble() - 0.5D) * spread;

                this.level.addParticle(ParticleTypes.PORTAL,
                        x, y, z,
                        startOffsetX, startOffsetY, startOffsetZ);
            }
        }
    }

    public SpaceStationDimension getSpaceStation() {
        if (level == null || level.isClientSide) return null;
        var dim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
        if (dim instanceof SpaceStationDimension spaceStationDimension) {
            return spaceStationDimension;
        }
        return null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        SpaceStationDimension station = getSpaceStation();
        if (station != null) {
            station.warpCores.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        SpaceStationDimension station = getSpaceStation();
        if (station != null) {
            station.warpCores.remove(this);
        }
    }

    public boolean isComplete() {
        return getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED);
    }

    public int getFuel() {
        if (!isComplete()) return 0;
        int fuel = 0;
        for (EntityItemInputBlock input : super.getItemInTiles()) {
            for (int i = 0; i < input.inventory.getSlots(); i++) {
                ItemStack stack = input.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof ItemDilithiumCrystal) {
                    fuel += stack.getCount();
                }
            }
        }
        return fuel;
    }

    public int consumeFuel(int amount) {
        if (!isComplete() || amount <= 0) return 0;
        int consumed = 0;
        for (EntityItemInputBlock input : super.getItemInTiles()) {
            if (consumed >= amount) break;
            for (int i = 0; i < input.inventory.getSlots(); i++) {
                if (consumed >= amount) break;
                ItemStack stack = input.inventory.getStackInSlot(i);
                if (stack.isEmpty() || !(stack.getItem() instanceof ItemDilithiumCrystal)) continue;
                int take = Math.min(stack.getCount(), amount - consumed);
                input.inventory.extractItem(i, take, false);
                consumed += take;
            }
        }
        if (consumed > 0) {
            setChanged();
        }
        return consumed;
    }

    @Override
    public Object[][][] getStructure() {
        return structure;
    }

    @Override
    public HashMap<Character, List<Block>> getCharMapping() {
        return charMapping;
    }

    @Override
    public boolean shouldHideBlock(int y, int z, int x, BlockState stateInWorld) {
        return !stateInWorld.getBlock().equals(ARLibRegistry.BLOCK_ITEM_INPUT_BLOCK.get());
    }
}
