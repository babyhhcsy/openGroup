package com.thero.worktile;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class THBundle extends AbstractBundle {
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    public static final String BUNDLE = "messages.IdeBundle";
    private static final THBundle INSTANCE = new THBundle();

    private THBundle() {
        super(BUNDLE);
    }
}
