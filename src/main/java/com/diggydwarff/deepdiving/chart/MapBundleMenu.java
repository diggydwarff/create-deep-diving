package com.diggydwarff.deepdiving.chart;

import com.diggydwarff.deepdiving.DeepDiving;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** The open bundle is locked in place; all transfers use ordinary server container transactions. */
public final class MapBundleMenu extends AbstractContainerMenu {
    public static final int PREVIOUS = 16, NEXT = 17;
    public final SimpleContainer maps = new SimpleContainer(MapBundleItem.CAPACITY);
    public final int bundleSlot;
    private final Inventory inventory;
    private final ItemStack bundle;
    private final DataSlot page = DataSlot.standalone();

    public MapBundleMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readVarInt());
    }

    public MapBundleMenu(int id, Inventory inventory, int bundleSlot) {
        super(DeepDiving.MAP_BUNDLE_MENU.get(), id);
        this.inventory = inventory;
        this.bundleSlot = bundleSlot;
        this.bundle =
                bundleSlot >= 0 && bundleSlot < inventory.getContainerSize()
                        ? inventory.getItem(bundleSlot)
                        : ItemStack.EMPTY;
        var stored = MapBundleItem.contents(bundle);
        for (int slot = 0; slot < maps.getContainerSize(); slot++)
            maps.setItem(slot, stored.get(slot));
        page.set(MapBundleItem.page(bundle));
        if (!inventory.player.level().isClientSide) maps.addListener(ignored -> save());
        addDataSlot(page);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                addSlot(
                        new Slot(maps, row * 4 + col, 194 + col * 18, 42 + row * 18) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return MapBundleItem.accepts(stack.copyWithCount(1));
                            }

                            @Override
                            public int getMaxStackSize() {
                                return 1;
                            }
                        });
            }
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addPlayerSlot(col + row * 9 + 9, 66 + col * 18, 158 + row * 18);
        for (int col = 0; col < 9; col++) addPlayerSlot(col, 66 + col * 18, 216);
        normalizePage();
    }

    private void addPlayerSlot(int index, int x, int y) {
        addSlot(
                new Slot(inventory, index, x, y) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return index != bundleSlot;
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return index != bundleSlot;
                    }
                });
    }

    private void save() {
        if (!stillValid(inventory.player)) return;
        var items =
                net.minecraft.core.NonNullList.withSize(MapBundleItem.CAPACITY, ItemStack.EMPTY);
        for (int slot = 0; slot < items.size(); slot++) items.set(slot, maps.getItem(slot));
        bundle.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        normalizePage();
        inventory.setChanged();
    }

    private void normalizePage() {
        if (!maps.getItem(page.get()).isEmpty()) return;
        for (int slot = 0; slot < maps.getContainerSize(); slot++) {
            if (!maps.getItem(slot).isEmpty()) {
                select(slot);
                return;
            }
        }
        select(0);
    }

    private void select(int slot) {
        page.set(slot);
        if (!inventory.player.level().isClientSide && stillValid(inventory.player))
            MapBundleItem.setPage(bundle, slot);
    }

    public int selectedSlot() {
        return Math.clamp(page.get(), 0, MapBundleItem.CAPACITY - 1);
    }

    public ItemStack selectedMap() {
        return maps.getItem(selectedSlot());
    }

    public int count() {
        int count = 0;
        for (int i = 0; i < maps.getContainerSize(); i++) if (!maps.getItem(i).isEmpty()) count++;
        return count;
    }

    public int pageNumber() {
        int number = 0;
        for (int i = 0; i <= selectedSlot(); i++) if (!maps.getItem(i).isEmpty()) number++;
        return number;
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (!stillValid(player)) return false;
        if (button >= 0 && button < MapBundleItem.CAPACITY && !maps.getItem(button).isEmpty()) {
            select(button);
            return true;
        }
        if (button != PREVIOUS && button != NEXT) return false;
        int direction = button == PREVIOUS ? -1 : 1;
        for (int slot = selectedSlot() + direction;
                slot >= 0 && slot < MapBundleItem.CAPACITY;
                slot += direction) {
            if (!maps.getItem(slot).isEmpty()) {
                select(slot);
                return true;
            }
        }
        return false;
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player)) return;
        if (type == ClickType.SWAP && button == bundleSlot) return;
        if (slot >= MapBundleItem.CAPACITY
                && slot < slots.size()
                && slots.get(slot).getSlotIndex() == bundleSlot) return;
        super.clicked(slot, button, type, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return bundle.getItem() instanceof MapBundleItem && inventory.getItem(bundleSlot) == bundle;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack original = slot.getItem(), result = original.copy();
        if (index < MapBundleItem.CAPACITY) {
            if (!moveItemStackTo(original, MapBundleItem.CAPACITY, slots.size(), true))
                return ItemStack.EMPTY;
        } else {
            if (!MapBundleItem.accepts(original.copyWithCount(1))
                    || !moveItemStackTo(original, 0, MapBundleItem.CAPACITY, false))
                return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, original);
        return result;
    }

    @Override
    public void broadcastChanges() {
        normalizePage();
        super.broadcastChanges();
        if (inventory.player instanceof ServerPlayer player
                && selectedMap().getItem() instanceof DepthMapItem chart) {
            // A page can be selected before its next inventory tick. Establish tracking first.
            var data = net.minecraft.world.item.MapItem.getSavedData(selectedMap(), player.level());
            if (data != null) data.tickCarriedBy(player, selectedMap());
            var packet = chart.getUpdatePacket(selectedMap(), player.level(), player);
            if (packet != null) player.connection.send(packet);
        }
    }
}
