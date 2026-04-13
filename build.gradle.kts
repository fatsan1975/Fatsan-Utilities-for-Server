import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
  `java-library`
  id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
  id("xyz.jpenilla.run-paper") version "3.0.2"
  id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.3.0"
}

group = "com.fatsan1975"
version = "0.2.0"
description = "Fatsan Utilities for Survival/Folia Servers"

java {
  toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
  mavenCentral()
}

dependencies {
  paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
  compileOnly(files("libs/VaultUnlocked-2.19.0.jar"))
  implementation("org.xerial:sqlite-jdbc:3.46.1.3")

  testImplementation(platform("org.junit:junit-bom:5.11.4"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
  compileJava {
    options.release = 21
    options.encoding = Charsets.UTF_8.name()
  }
  javadoc {
    options.encoding = Charsets.UTF_8.name()
  }
  test {
    useJUnitPlatform()
  }
  jar {
    from({
      configurations.runtimeClasspath.get()
        .filter { it.name.startsWith("sqlite-jdbc") }
        .map { if (it.isDirectory) it else zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }
}

bukkitPluginYaml {
  main = "com.fatsan1975.utilities.FatsanUtilitiesPlugin"
  load = BukkitPluginYaml.PluginLoadOrder.STARTUP
  authors.add("Fatsan1975")
  apiVersion = "1.21"
  foliaSupported = true
  softDepend = listOf("Vault", "VaultUnlocked", "FoliaPerms", "LuckPerms")

  commands {
    register("balance") {
      aliases = listOf("bal", "money")
      description = "Bakiyenizi goruntuler / View balance"
      usage = "/balance [player]"
    }
    register("balancetop") {
      aliases = listOf("topbalance", "baltop")
      description = "En zengin oyunculari listeler / Wealth ranking"
      usage = "/balancetop [page]"
    }
    register("pay") {
      description = "Para gonder / Send money"
      usage = "/pay <player> <amount>"
    }
    register("eco") {
      aliases = listOf("economy")
      description = "Admin ekonomi yonetimi / Admin economy management"
      usage = "/eco <give|take|set|reset> <player> [amount]"
    }
    register("tpa") {
      description = "Oyuncuya isinlanma istegi / Send TPA request"
      usage = "/tpa <player>"
    }
    register("tpaccept") {
      description = "TPA kabul / Accept TPA"
      usage = "/tpaccept"
    }
    register("tpdeny") {
      description = "TPA reddet / Deny TPA"
      usage = "/tpdeny"
    }
    register("rtp") {
      description = "Rastgele guvenli isinlanma / Random safe teleport"
      usage = "/rtp [world]"
    }
    register("spawn") {
      description = "Spawn'a don / Teleport to spawn"
      usage = "/spawn"
    }
    register("setspawn") {
      description = "Spawn noktasini ayarla / Set spawn point"
      usage = "/setspawn"
    }
    register("home") {
      description = "Ev noktasina isinlan / Teleport to home"
      usage = "/home [name]"
    }
    register("sethome") {
      description = "Ev ayarla / Set a home"
      usage = "/sethome [name]"
    }
    register("delhome") {
      description = "Ev sil / Delete a home"
      usage = "/delhome [name]"
    }
    register("homes") {
      description = "Evleri listele / List homes"
      usage = "/homes"
    }
    register("itemchat") {
      aliases = listOf("showitem")
      description = "Esya goster / Show held item"
      usage = "/itemchat"
    }
    register("invchat") {
      aliases = listOf("showinv")
      description = "Envanter goster / Show inventory"
      usage = "/invchat"
    }
    register("invsee") {
      description = "Admin envanter goruntule / View inventory"
      usage = "/invsee <player> [ender]"
    }
    register("fuhelp") {
      description = "Yardim / Help"
      usage = "/fuhelp"
    }
    register("fudebug") {
      description = "Debug bilgileri / Debug info"
      usage = "/fudebug"
    }
    register("fumodule") {
      description = "Modul yonetimi / Module management"
      usage = "/fumodule <module> <on|off|status>"
    }
    register("futilitiesreload") {
      aliases = listOf("fureload")
      description = "Plugin yenile / Reload plugin"
      usage = "/futilitiesreload"
    }
  }
}
