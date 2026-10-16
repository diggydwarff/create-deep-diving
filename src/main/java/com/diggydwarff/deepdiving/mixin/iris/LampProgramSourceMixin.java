package com.diggydwarff.deepdiving.mixin.iris;

import com.diggydwarff.deepdiving.client.lighting.LampIrisHooks;
import com.diggydwarff.deepdiving.client.lighting.LampMaterialShader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Edits the expanded source in memory. Installed shader ZIPs stay untouched. */
@Mixin(targets = "net.irisshaders.iris.shaderpack.programs.ProgramSource", remap = false)
abstract class LampProgramSourceMixin {
    @Unique
    private volatile Optional<String> deepdiving$materialSource;

    @Shadow
    public abstract String getName();

    @Inject(method = "getFragmentSource", at = @At("RETURN"), cancellable = true)
    private void deepdiving$materialSource(CallbackInfoReturnable<Optional<String>> callback) {
        if (deepdiving$materialSource == null) {
            Optional<String> original = callback.getReturnValue();

            deepdiving$materialSource = LampIrisHooks.canPatch()
                ? original.map(source -> LampMaterialShader.patch(getName(), source)) : original;
        }

        callback.setReturnValue(deepdiving$materialSource);
    }
}
