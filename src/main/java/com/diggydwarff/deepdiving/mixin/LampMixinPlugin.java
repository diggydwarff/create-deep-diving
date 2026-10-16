package com.diggydwarff.deepdiving.mixin;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Avoids loading any Iris compatibility class when Iris is absent. */
public final class LampMixinPlugin implements IMixinConfigPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {

        if (!mixinClassName.contains(".iris.")) {
            return true;
        }

        String classFile = targetClassName.replace('.', '/') + ".class";

        if (getClass().getClassLoader().getResource(classFile) != null) {
            return true;
        }

        // Some module class loaders do not expose another mod's class files
        // as resources during discovery. Use NeoForge's discovered mod list
        // as a fallback rather than silently skipping an installed Iris.
        var modList = FMLLoader.getLoadingModList();

        if (modList != null) {

            for (var mod : modList.getMods()) {

                if (mod.getModId().equals("iris")) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
