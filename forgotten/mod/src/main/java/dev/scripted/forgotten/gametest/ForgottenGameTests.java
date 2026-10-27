package dev.scripted.forgotten.gametest;

import dev.scripted.forgotten.Forgotten;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.registry.Registries;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;

public final class ForgottenGameTests {
    @GameTest
    public void lensIsRegistered(TestContext context) {
        if (Registries.ITEM.getId(Forgotten.LENS) == null || !Registries.ITEM.getId(Forgotten.LENS).equals(Forgotten.id("lens"))) {
            context.throwGameTestException(Text.literal("lens is not registered as forgotten:lens"));
        }
        context.complete();
    }
}
