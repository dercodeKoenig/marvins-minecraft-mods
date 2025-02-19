package NPCs.programs;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.*;

import java.util.*;

import static NPCs.Utils.*;

public class SlowPathFinder {

    public static class PathFindExit {
        int exitCode;
        Path path;

        public PathFindExit(int code, Path path) {
            this.exitCode = code;
            this.path = path;
        }
    }

    Node[] neighbors = new Node[32];
    WalkNodeEvaluator nodeEvaluator;
    BinaryHeap openSet = new BinaryHeap();
    Mob mob;

    public SlowPathFinder(Mob mob) {
        this.nodeEvaluator = new WalkNodeEvaluator();
        nodeEvaluator.setCanOpenDoors(true);
        nodeEvaluator.setCanFloat(true);
        nodeEvaluator.setCanPassDoors(true);
        this.mob = mob;

    }

    Node startNode = null;
    BlockPos lastTarget = null;
    int lastMaxRange = 0;
    int lastAccuracy = 0;
    int lastMaxNodes = 0;
    int i = 0;

    public PathFindExit findPath(BlockPos targetPos, int maxRange, int accuracy, int maxNodes, int steps) {

        if (!Objects.equals(lastTarget, targetPos) ||
                !Objects.equals(lastAccuracy, accuracy) ||
                !Objects.equals(lastMaxRange, maxRange) ||
                !Objects.equals(maxNodes, lastMaxNodes) ||
                startNode == null
        ) {
            //long t0 = System.nanoTime();

            // reset all
            lastAccuracy = accuracy;
            lastMaxNodes = maxNodes;
            lastTarget = targetPos;
            lastMaxRange = maxRange;
            i = 0;

            PathNavigationRegion pathnavigationregion = new PathNavigationRegion(
                    mob.level(),
                    targetPos.offset(-maxRange, -maxRange, -maxRange),
                    targetPos.offset(maxRange, maxRange, maxRange)
            );
            this.openSet.clear();
            this.nodeEvaluator.prepare(pathnavigationregion, mob);
            startNode = this.nodeEvaluator.getStart();
            //System.out.println("start node: "+startNode.asBlockPos());
            startNode.g = 0.0F;
            startNode.h = startNode.distanceTo(targetPos);
            startNode.f = startNode.h;
            this.openSet.insert(startNode);

            //long t1 = System.nanoTime();
            //System.out.println("reset pathfinder: "+targetPos+":"+(double)(t1-t0) / 1000 / 1000);
        }

        int n = 0;
        while (n < steps){
            if (openSet.isEmpty()) {
                startNode = null; // makes it reset on next run
                nodeEvaluator.done();
                return new PathFindExit(EXIT_FAIL, null);
            }

            if (i >= maxNodes) {
                startNode = null; // makes it reset on next run
                nodeEvaluator.done();
                return new PathFindExit(EXIT_FAIL, null);
            }

            Node node = this.openSet.pop();
            node.closed = true;

            //System.out.println(openSet.size()+":"+node.asBlockPos());

            if (node.distanceManhattan(targetPos) <= accuracy) {
                //success
                Path p = reconstructPath(node, targetPos);
                startNode = null; // makes it reset on next run
                nodeEvaluator.done();
                return new PathFindExit(EXIT_SUCCESS, p);
            }

            if (node.distanceTo(startNode) < maxRange) {
                int k = this.nodeEvaluator.getNeighbors(this.neighbors, node);

                for (int l = 0; l < k; ++l) {
                    Node node1 = this.neighbors[l];
                    float f = this.distance(node, node1);
                    node1.walkedDistance = node.walkedDistance + f;
                    float f1 = node.g + f + node1.costMalus;
                    if ((!node1.inOpenSet() || f1 < node1.g)) {
                        node1.cameFrom = node;
                        node1.g = f1;
                        node1.h = node1.distanceTo(targetPos) * 1.5F;
                        if (node1.inOpenSet()) {
                            this.openSet.changeCost(node1, node1.g + node1.h);
                        } else {
                            node1.f = node1.g + node1.h;
                            this.openSet.insert(node1);
                        }
                    }
                }

                i++;
                n++;
            }
        }

        return new PathFindExit(SUCCESS_STILL_RUNNING, null);
    }

    protected float distance(Node first, Node second) {
        return first.distanceTo(second);
    }


    private Path reconstructPath(Node point, BlockPos targetPos) {
        List<Node> list = Lists.newArrayList();
        Node node = point;
        list.add(0, point);

        while (node.cameFrom != null) {
            node = node.cameFrom;
            list.add(0, node);
        }

        return new Path(list, targetPos, true);
    }
}
