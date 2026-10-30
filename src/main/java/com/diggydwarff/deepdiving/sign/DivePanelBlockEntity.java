package com.diggydwarff.deepdiving.sign;

import com.diggydwarff.deepdiving.DeepDiving;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/** Caption and ink state only. Panels have no ticker or per-sign world scanning. */
public final class DivePanelBlockEntity extends BlockEntity {
    private String caption = "";
    private int color;
    private int arrow;
    private boolean glowing;
    private UUID editor;
    private UUID editToken;
    private long editStarted;

    public DivePanelBlockEntity(BlockPos pos, BlockState state) {
        super(DeepDiving.DIVE_PANEL_ENTITY.get(), pos, state);
    }

    public DiveSignDesign design() {
        return ((DivePanelBlock) getBlockState().getBlock()).design;
    }

    public String customCaption() {
        return caption;
    }

    public Component caption() {
        return Component.literal(caption);
    }

    public int color() {
        return color;
    }

    public int arrow() {
        return arrow;
    }

    public boolean glowing() {
        return glowing;
    }

    public void setSettings(String caption, int color, int arrow) {
        this.caption = DiveSignText.clean(caption);
        this.color = Math.clamp(color, 0, 15);
        this.arrow = Math.clamp(arrow, 0, 4);
        sync();
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
        sync();
    }

    public DiveSignPackets.View beginEditing(ServerPlayer player) {
        editor = player.getUUID();
        editToken = UUID.randomUUID();
        editStarted = player.level().getGameTime();
        return new DiveSignPackets.View(
                worldPosition, editToken, design(), caption, color, arrow, glowing);
    }

    public boolean accepts(ServerPlayer player, UUID token) {
        long elapsed = player.level().getGameTime() - editStarted;
        return player.getUUID().equals(editor)
                && token.equals(editToken)
                && elapsed >= 0
                && elapsed <= 6000;
    }

    public void finishEditing() {
        editor = null;
        editToken = null;
    }

    public void loadItem(ItemStack stack) {
        var root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (root.contains("deepdiving_sign")) readSettings(root.getCompound("deepdiving_sign"));
        sync();
    }

    public ItemStack itemWithSettings() {
        ItemStack item = new ItemStack(getBlockState().getBlock());
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(itemData()));
        return item;
    }

    private CompoundTag itemData() {
        CompoundTag root = new CompoundTag();
        root.put("deepdiving_sign", settings());
        return root;
    }

    private CompoundTag settings() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Caption", caption);
        tag.putInt("Color", color);
        tag.putInt("Arrow", arrow);
        tag.putBoolean("Glowing", glowing);
        return tag;
    }

    private void readSettings(CompoundTag tag) {
        caption = DiveSignText.clean(tag.getString("Caption"));
        color = Math.clamp(tag.getInt("Color"), 0, 15);
        arrow = Math.clamp(tag.getInt("Arrow"), 0, 4);
        glowing = tag.getBoolean("Glowing");
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("DiveSign", settings());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readSettings(tag.getCompound("DiveSign"));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CUSTOM_DATA, CustomData.of(itemData()));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
