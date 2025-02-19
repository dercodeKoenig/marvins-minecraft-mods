package ARLib.utils;

import ARLib.multiblockCore.BlockMultiblockPart;
import net.minecraft.core.BlockPos;

import java.util.Objects;

public class BlockIdentifier {
    public String levelId;
    public BlockPos pos;

    public BlockIdentifier(String level, BlockPos pos) {
        this.levelId = level;
        this.pos = pos;
    }


    // Override equals() to compare logical equality
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // Check if the same instance
        if (obj == null || getClass() != obj.getClass()) return false; // Ensure correct class

        BlockIdentifier that = (BlockIdentifier) obj;

        return Objects.equals(levelId, that.levelId) && Objects.equals(pos, that.pos);
    }

    // Override hashCode() to compute hash based on fields
    @Override
    public int hashCode() {
        return Objects.hash(levelId, pos);
    }
}
