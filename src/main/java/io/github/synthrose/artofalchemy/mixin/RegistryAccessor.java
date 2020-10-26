package io.github.synthrose.artofalchemy.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Supplier;

// Thanks, UpcraftLP!
@Mixin(Registry.class)
public interface RegistryAccessor {
	@Invoker("create")
	static <T, R extends MutableRegistry<T>> R create(RegistryKey<Registry<T>> key, R registry, Supplier<T> defaultEntry, Lifecycle lifecycle) {
		throw new AssertionError("mixin dummy");
	}
}
