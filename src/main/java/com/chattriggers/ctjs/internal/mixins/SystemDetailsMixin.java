package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.engine.module.Module;
import com.chattriggers.ctjs.internal.engine.module.ModuleManager;
import com.chattriggers.ctjs.internal.engine.module.ModuleMetadata;
import net.minecraft.util.SystemDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

@Mixin(SystemDetails.class)
public abstract class SystemDetailsMixin {
    @Shadow
    public abstract void addSection(String string, Supplier<String> supplier);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addModules(CallbackInfo ci) {
        addSection("ChatTriggers Modules", () -> {
            List<Module> modules = ModuleManager.INSTANCE.getCachedModules();
            modules.sort(Comparator.comparing(Module::getName));

            StringBuilder sb = new StringBuilder();

            for (Module module : modules) {
                sb
                    .append("\n")
                    .append("\t\t")
                    .append(module.getName())
                    .append(": ");

                ModuleMetadata metadata = module.getMetadata();
                if (metadata.getVersion() != null) {
                        sb.append("v")
                        .append(module.getMetadata().getVersion());
                } else {
                    sb.append("No module version specified");
                }
            }

            return sb.toString();
        });
    }
}
