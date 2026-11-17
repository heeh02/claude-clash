package com.github.kr328.clash.core

import android.os.Build
import android.system.Os
import com.github.kr328.clash.core.bridge.ClashException
import it.krzeminski.snakeyaml.engine.kmp.api.Dump
import it.krzeminski.snakeyaml.engine.kmp.api.DumpSettings
import it.krzeminski.snakeyaml.engine.kmp.api.Load
import it.krzeminski.snakeyaml.engine.kmp.api.LoadSettings
import it.krzeminski.snakeyaml.engine.kmp.common.FlowStyle
import java.io.File
import java.nio.file.Files

object GlobalProfile {
    private val nonForwardingTypes = setOf("direct", "dns", "reject", "reject-drop", "rematch")

    fun secure(source: File): File {
        val config = if (source.isDirectory) source.resolve("config.yaml") else source
        val parent = config.parentFile ?: throw ClashException("Profile has no parent directory")
        val settings = LoadSettings.builder()
            .setAllowDuplicateKeys(false)
            .setMaxAliasesForCollections(50)
            .setCodePointLimit(16 * 1024 * 1024)
            .build()
        val parsed = Load(settings).loadOne(config.readText(Charsets.UTF_8))
        val root = (parsed as? Map<*, *>)?.entries?.associateTo(linkedMapOf()) {
            it.key.toString() to it.value
        } ?: throw ClashException("Profile root must be a YAML map")

        val proxies = (root["proxies"] as? List<*>).orEmpty().mapNotNull { it as? Map<*, *> }
        val names = proxies.mapNotNull { proxy ->
            val name = proxy["name"] as? String
            val type = (proxy["type"] as? String)?.lowercase(java.util.Locale.ROOT)
            name?.takeIf { it.isNotBlank() && type !in nonForwardingTypes }
        }
        val providers = (root["proxy-providers"] as? Map<*, *>)?.keys?.mapNotNull { it as? String }.orEmpty()

        if (names.isNotEmpty() || providers.isNotEmpty()) {
            val groups = (root["proxy-groups"] as? List<*>).orEmpty().filter {
                (it as? Map<*, *>)?.get("name") != "GLOBAL"
            }.toMutableList()
            groups.add(linkedMapOf<String, Any>(
                "name" to "GLOBAL",
                "type" to "select",
                "proxies" to names,
                "use" to providers,
            ))
            root["proxy-groups"] = groups
        }

        val runtime = parent.resolve(".privacy")
        if (!runtime.isDirectory && !runtime.mkdirs()) {
            throw ClashException("Unable to create runtime profile directory")
        }
        linkProviders(parent.resolve("providers"), runtime.resolve("providers"))
        val destination = runtime.resolve("config.yaml")
        val dumpSettings = DumpSettings.builder()
            .setDefaultFlowStyle(FlowStyle.BLOCK)
            .setUseUnicodeEncoding(true)
            .setWidth(120)
            .build()
        destination.writeText(Dump(dumpSettings).dumpToString(root), Charsets.UTF_8)
        destination.setReadable(false, false)
        destination.setWritable(false, false)
        destination.setReadable(true, true)
        destination.setWritable(true, true)
        return runtime
    }

    private fun linkProviders(source: File, destination: File) {
        if (!source.isDirectory) return
        destination.delete()
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                Files.createSymbolicLink(destination.toPath(), source.toPath())
            } else {
                Os.symlink(source.absolutePath, destination.absolutePath)
            }
        } catch (e: Exception) {
            destination.deleteRecursively()
            source.copyRecursively(destination, overwrite = true)
        }
    }
}
