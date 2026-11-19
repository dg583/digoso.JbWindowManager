package com.digoso.jbwindowmanager.store

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import java.nio.file.Path

/** Stellt den [ProfileStore] auf die globale Datei `<IDE-Config>/layoutmanager/profiles.json` bereit. */
@Service(Service.Level.APP)
class ProfileService {
    val store = ProfileStore(Path.of(PathManager.getConfigPath(), "layoutmanager", "profiles.json"))
}
