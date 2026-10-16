package com.diggydwarff.deepdiving.guideline;

import com.diggydwarff.deepdiving.DeepDiving;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public final class GuideNodeBlockEntity extends BlockEntity {
    private List<GuideLine> clientLines = List.of();

    public GuideNodeBlockEntity(BlockPos pos, BlockState state) {
        super(DeepDiving.GUIDE_NODE_ENTITY.get(), pos, state);
    }

    public List<GuideLine> lines() {
        return level instanceof ServerLevel server
                ? GuideNetwork.get(server).at(worldPosition)
                : clientLines;
    }

    public void sync() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (level instanceof ServerLevel) {
            sync();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        ListTag data = new ListTag();

        for (GuideLine line : lines()) {
            data.add(line.save());
        }

        tag.put("lines", data);
        return tag;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        var loaded = new ArrayList<GuideLine>();

        ListTag data = tag.getList("lines", 10);

        for (int i = 0; i < Math.min(GuideNetwork.MAX_CONNECTIONS, data.size()); i++) {
            GuideLine line = GuideLine.load(data.getCompound(i));

            if (line != null) {
                loaded.add(line);
            }
        }

        clientLines = List.copyOf(loaded);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public AABB lineBounds() {
        AABB box = new AABB(worldPosition);

        for (GuideLine line : lines()) {
            box = box.minmax(line.bounds());
        }

        return box;
    }
}
