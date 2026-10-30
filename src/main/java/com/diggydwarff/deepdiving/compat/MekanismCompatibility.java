package com.diggydwarff.deepdiving.compat;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.gas.Gases;

import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/** Optional Mekanism 10.7 chemical ports. One chemical unit equals one mB of virtual gas. */
public final class MekanismCompatibility {
    private static Class<?> handlerType;
    private static Constructor<?> stackConstructor;
    private static Method amount, registryName, copyWithAmount, tags;
    private static Object empty;
    private static Registry<Object> registry;
    private static final Map<Integer, Optional<Object>> CHEMICALS = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static void register(RegisterCapabilitiesEvent event) {
        if (!ModList.get().isLoaded("mekanism")) return;
        try {
            handlerType = Class.forName("mekanism.api.chemical.IChemicalHandler");
            Class<?> stackType = Class.forName("mekanism.api.chemical.ChemicalStack");
            stackConstructor =
                    stackType.getConstructor(
                            Class.forName("mekanism.api.chemical.Chemical"), long.class);
            amount = stackType.getMethod("getAmount");
            registryName = stackType.getMethod("getTypeRegistryName");
            copyWithAmount = stackType.getMethod("copyWithAmount", long.class);
            tags = stackType.getMethod("getTags");
            empty = stackType.getField("EMPTY").get(null);
            registry =
                    (Registry<Object>)
                            Class.forName("mekanism.api.MekanismAPI")
                                    .getField("CHEMICAL_REGISTRY")
                                    .get(null);
            Object multi =
                    Class.forName("mekanism.common.capabilities.Capabilities")
                            .getField("CHEMICAL")
                            .get(null);
            var capability =
                    (BlockCapability<Object, Direction>)
                            multi.getClass().getMethod("block").invoke(multi);
            event.registerBlockEntity(
                    capability,
                    DeepDiving.MACHINE.get(),
                    (machine, side) -> {
                        IFluidHandler port = machine.fluidPort(side);
                        return port == null
                                ? null
                                : Proxy.newProxyInstance(
                                        handlerType.getClassLoader(),
                                        new Class<?>[] {handlerType},
                                        (proxy, method, args) -> invoke(port, proxy, method, args));
                    });
        } catch (ReflectiveOperationException | LinkageError failure) {
            com.mojang.logging.LogUtils.getLogger()
                    .warn(
                            "Deep Diving: Mekanism chemical API unavailable; chemical ports"
                                    + " disabled.",
                            failure);
        }
    }

    private static Object invoke(IFluidHandler port, Object proxy, Method method, Object[] args)
            throws Throwable {
        if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
        return switch (method.getName()) {
            case "toString" -> "Deep Diving breathing gas chemical port";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "getChemicalTanks" -> port.getTanks();
            case "getChemicalInTank" -> chemical(port.getFluidInTank((int) args[0]));
            case "getChemicalTankCapacity" -> (long) port.getTankCapacity((int) args[0]);
            case "isValid" -> port.isFluidValid((int) args[0], fluid(args[1]));
            case "setChemicalInTank" ->
                    throw new UnsupportedOperationException(
                            "Use chemical insertion/extraction on this port");
            case "insertChemical" -> {
                FluidStack offered = fluid(args[1]);
                long original = (long) amount.invoke(args[1]);
                int accepted = offered.isEmpty() ? 0 : port.fill(offered, action(args[2]));
                yield original == accepted
                        ? empty
                        : copyWithAmount.invoke(args[1], original - accepted);
            }
            case "extractChemical" -> {
                FluidStack stored = port.getFluidInTank((int) args[0]);
                Object converted = chemical(stored);
                if (converted == empty) yield empty;
                int requested = (int) Math.clamp((long) args[1], 0, Integer.MAX_VALUE);
                yield chemical(port.drain(stored.copyWithAmount(requested), action(args[2])));
            }
            default -> throw new UnsupportedOperationException(method.toString());
        };
    }

    private static IFluidHandler.FluidAction action(Object action) {
        return ((Enum<?>) action).name().equals("EXECUTE")
                ? IFluidHandler.FluidAction.EXECUTE
                : IFluidHandler.FluidAction.SIMULATE;
    }

    private static int kind(Object stack) throws ReflectiveOperationException {
        ResourceLocation id = (ResourceLocation) registryName.invoke(stack);
        String[] names = {"", "oxygen", "nitrogen", "helium"};
        for (int i = 1; i < names.length; i++) {
            if (id.equals(ResourceLocation.fromNamespaceAndPath("mekanism", names[i]))) return i;
            try (Stream<?> stream = (Stream<?>) tags.invoke(stack)) {
                String name = names[i];
                if (stream.anyMatch(
                        tag ->
                                tag instanceof TagKey<?> key
                                        && key.location()
                                                .equals(
                                                        ResourceLocation.fromNamespaceAndPath(
                                                                "c", name)))) return i;
            }
        }
        return -1;
    }

    private static FluidStack fluid(Object stack) throws ReflectiveOperationException {
        int kind = kind(stack);
        long quantity = (long) amount.invoke(stack);
        return kind < 0 || quantity <= 0
                ? FluidStack.EMPTY
                : new FluidStack(
                        Gases.all()[kind].source.get(),
                        (int) Math.min(Integer.MAX_VALUE, quantity));
    }

    private static Object chemical(FluidStack fluid) throws ReflectiveOperationException {
        int kind = GasCompatibility.kind(fluid);
        if (kind <= 0) return empty;
        Optional<Object> cached = CHEMICALS.get(kind);
        if (cached == null) {
            Object found = null;
            String name = new String[] {"", "oxygen", "nitrogen", "helium"}[kind];
            ResourceLocation preferred = ResourceLocation.fromNamespaceAndPath("mekanism", name);
            if (registry.containsKey(preferred)) found = registry.get(preferred);
            else
                for (Object candidate : registry) {
                    if (kind(stackConstructor.newInstance(candidate, 1L)) == kind) {
                        found = candidate;
                        break;
                    }
                }
            cached = Optional.ofNullable(found);
            CHEMICALS.put(kind, cached);
        }
        Object chemical = cached.orElse(null);
        return chemical == null
                ? empty
                : stackConstructor.newInstance(chemical, (long) fluid.getAmount());
    }

    /**
     * Chemical tags are reloadable; cached matches and misses must follow the current data pack.
     */
    @SubscribeEvent
    public static void tagsReloaded(TagsUpdatedEvent event) {
        if (event.shouldUpdateStaticData()) {
            CHEMICALS.clear();
        }
    }

    private MekanismCompatibility() {}
}
