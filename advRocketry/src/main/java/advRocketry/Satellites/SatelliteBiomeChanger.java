package advRocketry.Satellites;

import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.TerraformingSystem;
import advRocketry.GlobalTime;
import com.google.common.base.Objects;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

import java.util.LinkedList;
import java.util.List;

public class SatelliteBiomeChanger extends Satellite {

    long lastActionTime = 0;
    List<Work> workData = new LinkedList<>();

    public String getName() {
        return "Biome Changer";
    }

    public double energyPerAction() {
        return 1000;
    }

    public int minActionTicks() {
        return 5;
    }

    public boolean submitWork(ResourceLocation levelId, int blockX, int blockZ, ResourceLocation targetBiome) {
        if (!parentDimensionId.equals(levelId))
            return false;
        if (targetBiome == null)
            return false;
        workData.add(new Work(levelId, blockX, blockZ, targetBiome));
        return true;
    }

    @Override
    public Pair<Boolean, String> validateBuild() {
        iterateEquipment();

        if (getEnergyCapacity() < energyPerAction())
            return Pair.of(false, "need more batteries");

        if (getEnergyStored() < 20000 && energyProducers.isEmpty()) {
            return Pair.of(false, "not enough energy");
        }

        return Pair.of(true, "");
    }

    public void tick() {
        super.tick();
        if (!workData.isEmpty()) {
            if (getEnergyStored() > energyPerAction() && lastActionTime + minActionTicks() < GlobalTime.getGlobalTime()) {
                Work w = workData.removeFirst();
                ServerLevel target = DimensionManager.getServerLevel(w.levelId);
                ResourceLocation currentBiome = TerraformingSystem.getCurrentSurfaceBiome(target, w.blockX, w.blockZ);
                if (!Objects.equal(currentBiome, w.biomeId) && w.levelId.equals(parentDimensionId)) {
                    extractEnergy(energyPerAction());
                    lastActionTime = GlobalTime.getGlobalTime();
                    TerraformingSystem.changeBiome(target, w.blockX, w.blockZ, w.biomeId);
                    System.out.println("satellite change biome at " + w.blockX + ":" + w.blockZ);
                }
            }
        }
    }

    static class Work {
        ResourceLocation levelId;
        int blockX;
        int blockZ;
        ResourceLocation biomeId;

        public Work(ResourceLocation levelId, int blockX, int blockZ, ResourceLocation biomeId) {
            this.biomeId = biomeId;
            this.blockX = blockX;
            this.blockZ = blockZ;
            this.levelId = levelId;
        }
    }
}
