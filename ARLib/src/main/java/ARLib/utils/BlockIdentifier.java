package ARLib.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Objects;

public class BlockIdentifier {
    public Level level;
    public BlockPos pos;


    public BlockIdentifier(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }


    // Override equals() to compare logical equality
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // Check if the same instance
        if (obj == null || getClass() != obj.getClass()) return false; // Ensure correct class

        BlockIdentifier that = (BlockIdentifier) obj;

        return Objects.equals(level, that.level) && Objects.equals(pos, that.pos);
    }

    // Override hashCode() to compute hash based on fields
    @Override
    public int hashCode() {
        return Objects.hash(level, pos);
    }
}
