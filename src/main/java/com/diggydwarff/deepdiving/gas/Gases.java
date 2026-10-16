package com.diggydwarff.deepdiving.gas;

import com.simibubi.create.content.fluids.VirtualFluid;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Virtual gases move through standard Create pipes and tanks, without flowing into the world. */
public final class Gases {
    public static final DeferredRegister<FluidType> TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, "deepdiving");

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, "deepdiving");

    public static final Gas AIR = new Gas("compressed_air", 0xFFC0DADA);

    public static final Gas OXYGEN = new Gas("oxygen", 0xFF72C4DB);

    public static final Gas NITROGEN = new Gas("nitrogen", 0xFFB6BA8A);

    public static final Gas HELIUM = new Gas("helium", 0xFFCC9BE2);

    public static final class Gas {
        public final int tint;

        public final DeferredHolder<FluidType, FluidType> type;

        public final DeferredHolder<Fluid, VirtualFluid> source;

        public final DeferredHolder<Fluid, VirtualFluid> flowing;

        Gas(String name, int tint) {
            this.tint = tint;
            type = TYPES.register(
                name,
                () -> new FluidType(
                    FluidType.Properties.create()
                        .density(-100)
                        .viscosity(500)
                        .canSwim(false)
                        .canDrown(false)
                ) {}
            );
            source = FLUIDS.register(name, () -> VirtualFluid.createSource(properties()));
            flowing = FLUIDS.register("flowing_" + name, () -> VirtualFluid.createFlowing(properties()));
        }

        private BaseFlowingFluid.Properties properties() {
            return new BaseFlowingFluid.Properties(type, () -> source.get(), () -> flowing.get());
        }
    }

    public static Gas[] all() {
        return new Gas[] {AIR, OXYGEN, NITROGEN, HELIUM};
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
    }

    private Gases() {}
}
