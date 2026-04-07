import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
  `java-library`
  id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
  id("xyz.jpenilla.run-paper") version "3.0.2"
  id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.3.0"
}

group = "com.fatsan1975"
version = "0.1.0"
description = "Fatsan Utilities for Survival Servers"

java {
  toolchain.languageVersion = JavaLanguageVersion.of(21)
}

dependencies {
  paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
  compileOnly(files("libs/VaultUnlocked-2.19.0.jar"))

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
}

bukkitPluginYaml {
  main = "com.fatsan1975.utilities.FatsanUtilitiesPlugin"
  load = BukkitPluginYaml.PluginLoadOrder.POSTWORLD
  authors.add("Fatsan1975")
  apiVersion = "1.21"
  foliaSupported = true
  depend = listOf("Vault")

  commands {
    register("balance") {
      aliases = listOf("bal")
      description = "Bakiyenizi görüntüler"
      usage = "/balance [oyuncu]"
      permission = "fatsanutilities.balance"
    }
    register("balancetop") {
      aliases = listOf("topbalance", "baltop")
      description = "En zengin oyuncuları listeler"
      usage = "/balancetop [sayfa]"
      permission = "fatsanutilities.balancetop"
    }
    register("pay") {
      description = "Bir oyuncuya para gönderir"
      usage = "/pay <oyuncu> <miktar>"
      permission = "fatsanutilities.pay"
    }
    register("tpa") {
      description = "Oyuncuya ışınlanma isteği gönderir"
      usage = "/tpa <oyuncu>"
      permission = "fatsanutilities.tpa"
    }
    register("tpaccept") {
      description = "Işınlanma isteğini kabul eder"
      usage = "/tpaccept [oyuncu]"
      permission = "fatsanutilities.tpa.accept"
    }
    register("tpdeny") {
      description = "Işınlanma isteğini reddeder"
      usage = "/tpdeny [oyuncu]"
      permission = "fatsanutilities.tpa.deny"
    }
    register("rtp") {
      description = "Rastgele güvenli lokasyona ışınlar"
      usage = "/rtp [dünya]"
      permission = "fatsanutilities.rtp"
    }
    register("spawn") {
      description = "Spawn noktasına ışınlar"
      usage = "/spawn"
      permission = "fatsanutilities.spawn"
    }
    register("itemchat") {
      aliases = listOf("showitem")
      description = "Elindeki eşyayı sohbette gösterir"
      usage = "/itemchat"
      permission = "fatsanutilities.itemchat"
    }
    register("invchat") {
      aliases = listOf("showinv")
      description = "Envanter bilgini sohbette gösterir"
      usage = "/invchat"
      permission = "fatsanutilities.invchat"
    }
    register("invsee") {
      description = "Admin için oyuncu envanterini görüntüler"
      usage = "/invsee <oyuncu> [ender]"
      permission = "fatsanutilities.admin.invsee"
    }
    register("fuhelp") {
      description = "Kullanılabilir komutları listeler"
      usage = "/fuhelp"
      permission = "fatsanutilities.help"
    }
    register("fudebug") {
      description = "Admin debug bilgilerini gösterir"
      usage = "/fudebug"
      permission = "fatsanutilities.admin.debug"
    }
    register("fumodule") {
      description = "Modülleri aç/kapat/durum"
      usage = "/fumodule <economy|teleport|social|admin> <on|off|status>"
      permission = "fatsanutilities.admin.module"
    }
    register("futilitiesreload") {
      aliases = listOf("fureload")
      description = "Plugin ayarlarını yeniler"
      usage = "/futilitiesreload"
      permission = "fatsanutilities.admin.reload"
    }
  }

}
