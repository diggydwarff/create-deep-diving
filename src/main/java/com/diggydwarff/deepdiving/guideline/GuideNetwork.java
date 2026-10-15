package com.diggydwarff.deepdiving.guideline;

import com.diggydwarff.deepdiving.DeepDiving;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** One authoritative graph per dimension: cutting an unloaded endpoint cannot duplicate a line. */
public final class GuideNetwork extends SavedData {
    public static final int MAX_CONNECTIONS = 8;

    private final Map<UUID, GuideLine> lines = new LinkedHashMap<>();

    private final Map<BlockPos, Set<UUID>> nodes = new HashMap<>();

    public static GuideNetwork get(ServerLevel level) {
        return level.getDataStorage()
            .computeIfAbsent(
                new SavedData.Factory<>(GuideNetwork::new, GuideNetwork::load, null),
                "deepdiving_guidelines"
            );
    }

    public List<GuideLine> at(BlockPos pos) {
        return nodes.getOrDefault(pos, Set.of()).stream()
            .map(lines::get)
            .filter(Objects::nonNull)
            .toList();
    }

    public GuideLine connect(
        ServerLevel level,
        BlockPos a,
        BlockPos b,
        DyeColor color,
        boolean glow
    ) {
        // Validate both endpoints before changing either index or consuming a spool.
        if (a.equals(b)
            || a.distSqr(b) > GuideLine.MAX_LENGTH * GuideLine.MAX_LENGTH
            || !level.hasChunkAt(a)
            || !level.hasChunkAt(b)
            || !level.getBlockState(a).is(DeepDiving.GUIDE_NODE.get())
            || !level.getBlockState(b).is(DeepDiving.GUIDE_NODE.get())
            || at(a).size() >= MAX_CONNECTIONS
            || at(b).size() >= MAX_CONNECTIONS
            || at(a).stream().anyMatch(line -> line.other(a).equals(b))) {
            return null;
        }

        var line = new GuideLine(
            UUID.randomUUID(),
            a.immutable(),
            b.immutable(),
            level.getBlockState(a).getValue(GuideNodeBlock.FACING),
            level.getBlockState(b).getValue(GuideNodeBlock.FACING),
            color,
            glow
        );

        add(line);
        setDirty();
        notifyNodes(level, line);
        return line;
    }

    private void add(GuideLine line) {
        // Store the edge once; both node indexes reference that same authoritative edge.
        lines.put(line.id(), line);
        nodes.computeIfAbsent(line.a(), ignored -> new LinkedHashSet<>()).add(line.id());
        nodes.computeIfAbsent(line.b(), ignored -> new LinkedHashSet<>()).add(line.id());
    }

    public GuideLine remove(ServerLevel level, UUID id) {
        GuideLine line = lines.remove(id);

        if (line == null) {
            return null;
        }

        for (BlockPos pos : List.of(line.a(), line.b())) {
            Set<UUID> ids = nodes.get(pos);

            if (ids != null) {
                ids.remove(id);

                if (ids.isEmpty()) {
                    nodes.remove(pos);
                }
            }
        }

        setDirty();
        notifyNodes(level, line);
        return line;
    }

    public List<GuideLine> removeNode(ServerLevel level, BlockPos pos) {
        List<GuideLine> removed = at(pos);

        for (GuideLine line : removed) {
            remove(level, line.id());
        }

        return removed;
    }

    public boolean glow(ServerLevel level, UUID id) {
        GuideLine old = lines.get(id);

        if (old == null || old.glowing()) {
            return false;
        }

        GuideLine line = old.withGlow();

        lines.put(id, line);
        setDirty();
        notifyNodes(level, line);
        return true;
    }

    private static void notifyNodes(ServerLevel level, GuideLine line) {
        for (BlockPos pos : List.of(line.a(), line.b())) {
            if (level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof GuideNodeBlockEntity node) {
                node.sync();
            }
        }
    }

    /** Find the closest visible segment under the crosshair, respecting the block ray limit. */
    public GuideLine trace(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();

        Vec3 rayEnd = eye.add(player.getLookAngle().scale(5));

        var hit = player.level()
            .clip(
                new ClipContext(
                    eye,
                    rayEnd,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player
                )
            );

        if (hit.getType() != HitResult.Type.MISS) {
            rayEnd = hit.getLocation().add(player.getLookAngle().scale(.08));
        }

        double best = Double.MAX_VALUE;

        GuideLine selected = null;

        for (GuideLine line : lines.values()) {
            if (line.bounds().clip(eye, rayEnd).isEmpty() && !line.bounds().contains(eye)) {
                continue;
            }

            for (int i = 0; i < 32; i++) {
                double[] distance = GuideLine.rayDistance(
                    line.point(i / 32.0),
                    line.point((i + 1) / 32.0),
                    eye,
                    rayEnd
                );

                if (distance[0] < .14 * .14 && distance[1] < best) {
                    best = distance[1];
                    selected = line;
                }
            }
        }

        return selected;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag data = new ListTag();

        for (GuideLine line : lines.values()) {
            data.add(line.save());
        }

        tag.put("lines", data);
        return tag;
    }

    public static GuideNetwork load(CompoundTag tag, HolderLookup.Provider provider) {
        var graph = new GuideNetwork();

        ListTag data = tag.getList("lines", 10);

        for (int i = 0; i < data.size(); i++) {
            GuideLine line = GuideLine.load(data.getCompound(i));

            if (line != null
                && graph.at(line.a()).size() < MAX_CONNECTIONS
                && graph.at(line.b()).size() < MAX_CONNECTIONS
                && graph.at(line.a()).stream()
                    .noneMatch(existing -> existing.other(line.a()).equals(line.b()))) {
                graph.add(line);
            }
        }

        return graph;
    }
}
