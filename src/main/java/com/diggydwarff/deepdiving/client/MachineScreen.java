package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.gas.GasMix;
import com.diggydwarff.deepdiving.machine.GasMachine;
import com.diggydwarff.deepdiving.machine.MachineBlock;
import com.diggydwarff.deepdiving.menu.MachineLayout;
import com.diggydwarff.deepdiving.menu.MachineMenu;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

public final class MachineScreen extends AbstractSimiContainerScreen<MachineMenu> {
    private MachineLayout layout = MachineLayout.SPACIOUS;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        setWindowSize(MachineLayout.WIDTH, layout.height);
    }

    @Override
    protected void init() {
        layout = height >= MachineLayout.SPACIOUS.height
                ? MachineLayout.SPACIOUS
                : MachineLayout.COMPACT;
        setWindowSize(MachineLayout.WIDTH, layout.height);
        menu.arrangeSlots(layout);
        super.init();

        if (menu.kind == MachineBlock.Kind.BLENDER) {
            String[] names = {"Air", "Nitrox", "Trimix", "Heliox"};

            for (int i = 0; i < 4; i++) {
                button(
                    16 + i * 62,
                    layout.presetsY,
                    54,
                    names[i],
                    "Select " + names[i] + " mixture",
                    i
                );
            }

            button(76, layout.mixtureY, 18, "-", "Reduce oxygen by 1%", 5);
            button(100, layout.mixtureY, 18, "+", "Increase oxygen by 1%", 6);
            button(214, layout.mixtureY, 18, "-", "Reduce helium by 1%", 7);
            button(238, layout.mixtureY, 18, "+", "Increase helium by 1%", 8);
        }

        if (fills()) {
            button(
                110,
                layout.slotsY,
                78,
                "Purge",
                "Vent the input cylinder before changing its mixture",
                4
            );
        }
    }

    private boolean fills() {
        return menu.kind == MachineBlock.Kind.BLENDER || menu.kind == MachineBlock.Kind.COMPRESSOR;
    }

    private void button(
        int x,
        int y,
        int width,
        String label,
        String tooltip,
        int id
    ) {
        addRenderableWidget(
            new CreateButton(
                leftPos + x,
                topPos + y,
                width,
                label,
                tooltip,
                () -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id)
            )
        );
    }

    @Override
    protected void renderBg(
        GuiGraphics g,
        float partial,
        int mx,
        int my
    ) {
        CreatePanel.draw(g, leftPos, topPos, MachineLayout.WIDTH, layout.panelHeight);
        renderPlayerInventory(g, leftPos + 48, topPos + layout.inventoryY);

        if (fills()) {
            CreatePanel.slot(g, leftPos + 23, topPos + layout.slotsY - 1);
            CreatePanel.slot(g, leftPos + 67, topPos + layout.slotsY - 1);
        }

        if (menu.kind == MachineBlock.Kind.BLENDER || menu.kind == MachineBlock.Kind.STORAGE) {
            CreatePanel.slot(g, leftPos + 231, topPos + layout.slotsY - 1);
        }

        g.fill(leftPos + 14, topPos + 22, leftPos + 258, topPos + 38, 0xFF171A1D);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        String heading = font.plainSubstrByWidth(title.getString(), 246);

        g.drawString(
            font,
            heading,
            (MachineLayout.WIDTH - font.width(heading)) / 2,
            5,
            0x281C11,
            false
        );
        text(
            g,
            GasMachine.statusText(menu.data.get(1)),
            20,
            26,
            menu.data.get(1) >= 3 && menu.data.get(1) <= 6 ? 0xFFD16A : 0xFFFFFF
        );

        GasMix mix = menu.targetMixture();

        if (menu.kind == MachineBlock.Kind.BLENDER) {
            text(g, "O2 " + mix.oxygen() + "%", 16, layout.mixtureY + 5, 0xFFFFFF);
            text(g, "He " + mix.helium() + "%", 154, layout.mixtureY + 5, 0xFFFFFF);

            var profile = DivingConfig.pressureProfile(minecraft.level.dimension().location());

            text(
                g,
                String.format(
                    Locale.ROOT,
                    "N2 %d%%   Safe depth %.0f-%.0f blocks",
                    mix.nitrogen(),
                    Math.ceil(mix.minimumDepth(profile)),
                    Math.floor(mix.maximumDepth(profile))
                ),
                16,
                layout.depthY,
                0xFFE2A1
            );
        } else {
            int row = layout == MachineLayout.SPACIOUS ? 51 : 43;

            if (menu.kind == MachineBlock.Kind.STORAGE) {
                var fluid = menu.storedFluid();

                text(
                    g,
                    fluid.isEmpty() ? "Empty gas vessel" : fluid.getHoverName().getString(),
                    20,
                    row,
                    0xFFFFFF
                );
                text(g, (menu.data.get(2) & 0xffff) + " / 64000 mB", 20, row + 21, 0xFFFFFF);
                text(
                    g,
                    String.format(
                        Locale.ROOT,
                        "Pressure: %.1f / 200 atm",
                        1 + 199.0 * (menu.data.get(2) & 0xffff) / 64000
                    ),
                    20,
                    row + 42,
                    0xFFFFFF
                );
            } else {
                text(g, "Rotation: " + menu.data.get(0) + " RPM", 20, row, 0xFFFFFF);
                text(g, "Air: " + menu.data.get(2) + " mB", 20, row + 19, 0xFFFFFF);

                if (menu.kind == MachineBlock.Kind.SEPARATOR) {
                    text(g, "O2: " + menu.data.get(3) + " mB", 20, row + 38, 0xFFFFFF);
                    text(g, "N2: " + menu.data.get(4) + " mB", 20, row + 57, 0xFFFFFF);
                } else {
                    text(
                        g,
                        "Cylinder: " + menu.data.get(8) + " / " + menu.data.get(11),
                        20,
                        row + 38,
                        0xFFFFFF
                    );
                }
            }
        }

        if (fills()) {
            text(g, "In", 26, layout.labelsY, 0xFFFFFF);
            text(g, "Out", 67, layout.labelsY, 0xFFFFFF);
        }

        if (menu.kind == MachineBlock.Kind.BLENDER || menu.kind == MachineBlock.Kind.STORAGE) {
            text(g, "Helium", 222, layout.labelsY, 0xFFFFFF);
        }
    }

    @Override
    protected void renderForeground(
        GuiGraphics g,
        int mx,
        int my,
        float partial
    ) {
        super.renderForeground(g, mx, my, partial);

        if (mx < leftPos + 14
            || mx >= leftPos + 258
            || my < topPos + 22
            || my >= topPos + layout.labelsY) {
            return;
        }

        if (my < topPos + 38
            || my >= topPos + layout.depthY
            || menu.kind == MachineBlock.Kind.SEPARATOR) {
            var lines = new java.util.ArrayList<Component>();

            lines.add(
                Component.literal(
                    switch (menu.kind) {
                        case COMPRESSOR -> "Rear shaft; air output on other faces";
                        case SEPARATOR -> "Air: top | O2: blue port | N2: green port";
                        case BLENDER -> "O2: blue port | N2: green port | He: top";
                        case STORAGE -> "Gas input/output on every face";
                    }
                )
            );

            if (menu.kind == MachineBlock.Kind.BLENDER) {
                lines.add(
                    Component.literal(
                        "O2 "
                                + menu.data.get(3)
                                + " | N2 "
                                + menu.data.get(4)
                                + " | He "
                                + menu.data.get(5)
                                + " mB"
                    )
                );
                lines.add(Component.literal(menu.targetMixture().summary()));
            }

            g.renderComponentTooltip(font, lines, mx, my);
        }
    }

    private void text(
        GuiGraphics g,
        String text,
        int x,
        int y,
        int color
    ) {
        g.drawString(font, font.plainSubstrByWidth(text, 258 - x), x, y, color, true);
    }
}
