package com.digoso.jbwindowmanager.store

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import java.nio.file.Path

/** Provides the [ProfileStore] backed by the global file `<IDE-Config>/digoso.JbWindowManager/profiles.json`. */
@Service(Service.Level.APP)
class ProfileService {
    val store = ProfileStore(Path.of(PathManager.getConfigPath(), "digoso.JbWindowManager", "profiles.json"))
}
