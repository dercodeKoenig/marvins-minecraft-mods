package advRocketry.BlockEntities;

import ARLib.ARLibRegistry;
import ARLib.blockentities.EntityEnergyInputBlock;
import ARLib.blockentities.EntityFluidOutputBlock;
import ARLib.blockentities.EntityItemOutputBlock;
import ARLib.multiblockCore.EntityMultiblockMachineMaster;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.RecipePartWithProbability;
import advRocketry.Config;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.GlobalTime;
import advRocketry.Registry.BlockEntities;
import advRocketry.Render.Particles.RocketParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.*;

public class EntityLaserDrill extends EntityMultiblockMachineMaster {


    public static Object[][][] structure =
            new Object[][][]{
                    {
                            {null, null, null, null, null, null, null, null, null, null, null},
                            {null, null, null, null, null, null, null, null, null, null, null},
                            {null, null, null, null, null, null, null, null, null, null, null},
                            {null, 'S', null, null, null, null, null, null, null, null, null},
                            {'S', 'S', 'S', null, null, null, null, null, null, null, null},
                            {null, 'S', null, null, null, null, null, null, null, null, null},
                            {null, null, null, null, null, null, null, null, null, null, null},
                            {null, null, null, null, null, null, null, null, null, null, null},
                            {null, null, null, null, null, null, null, null, null, null, null}
                    },
                    {
                            {null, null, null, null, null, null, 'S', 'L', 'L', 'L', null},
                            {null, null, null, null, 'S', 'G', 'G', 'L', 'L', 'L', 'P'},
                            {null, null, null, null, 'S', 'S', 'S', 'L', 'L', 'L', 'P'},
                            {'s', 'S', 's', null, 'G', null, 'S', 'L', 'L', 'L', null},
                            {'S', 'S', 'S', 'G', 'S', null, null, null, null, null, null},
                            {'s', 'S', 's', null, 'G', null, 'S', 'L', 'L', 'L', null},
                            {null, null, null, null, 'S', 'S', 'S', 'L', 'L', 'L', 'P'},
                            {null, null, null, null, 'S', 'G', 'G', 'L', 'L', 'L', 'P'},
                            {null, null, null, null, null, null, 'S', 'L', 'L', 'L', null}
                    },
                    {
                            {null, null, null, null, null, null, 'S', 'L', 'L', 'L', null},
                            {null, null, null, null, 'S', 'G', 'G', 'L', 'L', 'L', 'P'},
                            {'O', 'c', 'O', null, 'S', 'S', 'S', 'L', 'L', 'L', 'P'},
                            {'s', 's', 's', null, 'G', null, 'S', 'L', 'L', 'L', null},
                            {'s', 's', 's', 'G', 'S', null, null, null, null, null, null},
                            {'s', 's', 's', null, 'G', null, 'S', 'L', 'L', 'L', null},
                            {null, null, null, null, 'S', 'S', 'S', 'L', 'L', 'L', 'P'},
                            {null, null, null, null, 'S', 'G', 'G', 'L', 'L', 'L', 'P'},
                            {null, null, null, null, null, null, 'S', 'L', 'L', 'L', null}
                    },
            };

    public static HashMap<Character, List<Block>> charMapping = new HashMap<>();

    static {
        charMapping.put('c', List.of(advRocketry.Registry.Blocks.LASERDRILL.get()));
        charMapping.put('O', List.of(ARLibRegistry.BLOCK_ITEM_OUTPUT_BLOCK.get(), ARLibRegistry.BLOCK_FLUID_OUTPUT_BLOCK.get()));
        charMapping.put('P', List.of(ARLibRegistry.BLOCK_ENERGY_INPUT_BLOCK.get()));
        charMapping.put('L', List.of(advRocketry.Registry.Blocks.VACUUM_LASER.get()));
        charMapping.put('S', List.of(ARLibRegistry.BLOCK_ADVANCED_STRUCTURE.get()));
        charMapping.put('s', List.of(ARLibRegistry.BLOCK_STRUCTURE.get()));
        charMapping.put('G', List.of(Blocks.GLASS));

    }

    int particleTimeout = 0;

    public EntityLaserDrill(BlockPos pos, BlockState state) {
        super(BlockEntities.ENTITY_LASERDRILL.get(), pos, state);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityLaserDrill) t).tick();
    }

    public void tick() {
        if (!(DimensionManager.INSTANCE_SERVER.get(level.dimension().location()) instanceof PlanetDimension planet)) {
            return;
        }
        if (!level.isClientSide) {
            if (level.hasNeighborSignal(getBlockPos())) {
                List<EntityEnergyInputBlock> energyTiles = super.getEnergyInputTiles();
                int totalEnergy = super.getTotalEnergyStored(energyTiles);
                int energyCost = Config.INSTANCE.laserDrill_Energy_Per_Tick;
                if (totalEnergy > energyCost) {
                    if (GlobalTime.getGlobalTime() % 20 == 0) {
                        CompoundTag info = new CompoundTag();
                        info.put("send_particles", new CompoundTag());
                        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, info));
                    }
                    List<EntityItemOutputBlock> outTiles = super.getItemOutTiles();
                    List<EntityFluidOutputBlock> fluidOutTiles = super.getFluidOutTiles();
                    super.consumeEnergy(energyCost, energyTiles);
                    HashMap<String, Double> ores = planet.getLaserDrillOres();
                    for (String key : ores.keySet()) {
                        double p = ores.get(key);
                        RecipePartWithProbability part = new RecipePartWithProbability(key, 1, (float) p);
                        part.computeRandomAmount(); // needs it to fill the actual output
                        super.produceOutput(List.of(part), fluidOutTiles, outTiles);
                    }
                }
            }
        }

        if (level.isClientSide && particleTimeout > 0) {
            particleTimeout--;
            if (GlobalTime.getGlobalTime() % 2 == 0) {
                Vec3 pos = getBlockPos()
                        .relative(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite(), 2)
                        .getCenter().add(0, -0.5, 0);

                Vec3 vel = new Vec3(
                        (Math.random() - 0.5) / 20,
                        0.2 + Math.random() / 10,
                        (Math.random() - 0.5) / 20
                );

                pos = pos.add(
                        (Math.random() - 0.5) * 3,
                        0,
                        (Math.random() - 0.5) * 3
                );

                new RocketParticle(
                        (ClientLevel) level,
                        pos.x + (Math.random() - 0.5) * 0.05,
                        pos.y,
                        pos.z + (Math.random() - 0.5) * 0.05,
                        vel.x,
                        vel.y,
                        vel.z,
                        new Vector3f(0.6f, 0.6f, 0.6f),
                        0.35f,
                        1.2f,
                        200,
                        false, // particles spawn inside blocks
                        false
                );
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }


    @Override
    public void readServer(CompoundTag tag, ServerPlayer player) {
        super.readServer(tag, player);
    }

    @Override
    public void readClient(CompoundTag tag) {
        super.readClient(tag);
        if(tag.contains("send_particles")){
            particleTimeout = 30;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

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
        return true;
    }
}