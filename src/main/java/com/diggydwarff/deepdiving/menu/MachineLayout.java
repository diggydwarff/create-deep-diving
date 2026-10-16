package com.diggydwarff.deepdiving.menu;

/** The compact fallback fits Minecraft's minimum 240-pixel GUI height without shrinking text. */
public enum MachineLayout {
    SPACIOUS(264, 152, 156, 46, 72, 99, 112, 124),
    COMPACT(240, 128, 132, 38, 62, 80, 91, 100);

    public static final int WIDTH = 272;

    public final int height;

    public final int panelHeight;

    public final int inventoryY;

    public final int presetsY;

    public final int mixtureY;

    public final int depthY;

    public final int labelsY;

    public final int slotsY;

    MachineLayout(
        int height,
        int panelHeight,
        int inventoryY,
        int presetsY,
        int mixtureY,
        int depthY,
        int labelsY,
        int slotsY
    ) {
        this.height = height;
        this.panelHeight = panelHeight;
        this.inventoryY = inventoryY;
        this.presetsY = presetsY;
        this.mixtureY = mixtureY;
        this.depthY = depthY;
        this.labelsY = labelsY;
        this.slotsY = slotsY;
    }
}
