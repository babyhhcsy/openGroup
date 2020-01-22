package com.thero.worktile.module;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;

public class FileModuleComponent implements ModuleComponent {
    Module module;

    public FileModuleComponent(Module module) {
        this.module = module;
    }

}
