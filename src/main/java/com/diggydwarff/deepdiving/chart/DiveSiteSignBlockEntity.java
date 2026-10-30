package com.diggydwarff.deepdiving.chart;

import com.diggydwarff.deepdiving.DeepDiving;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Normal editable sign data; only its front top line is used as a chart label. */
public final class DiveSiteSignBlockEntity extends SignBlockEntity {
    public DiveSiteSignBlockEntity(BlockPos pos, BlockState state) {
        super(DeepDiving.SITE_SIGN_ENTITY.get(), pos, state);
    }

    public Component siteName() {
        // Use filtered plain text: map labels must not restore filtered text or sign click actions.
        String name = getFrontText().getMessage(0, true).getString().strip();
        int end = name.offsetByCodePoints(0, Math.min(64, name.codePointCount(0, name.length())));
        return Component.literal(name.substring(0, end));
    }
}
