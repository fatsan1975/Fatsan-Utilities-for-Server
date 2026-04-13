# Fatsan Utilities for Server

[Türkçe](#türkçe) · [English](#english)

---

## Türkçe

**Folia öncelikli, Paper 1.21.x uyumlu, çok dilli temel sunucu utility plugini.**

Essentials’ın Folia’da çalışmaması nedeniyle hayata geçti. Hafif, hızlı, korunaklı, kolayca yapılandırılabilir; ekonomi, teleport, sosyal ve admin modüllerini tek paket halinde sunar.

### Öne Çıkan Özellikler (v0.2.0)
- ✅ **Tam Folia uyumlu scheduler** — `RegionScheduler`, `EntityScheduler`, `AsyncScheduler` adapter’ı.
- ✅ **Kendi ekonomi sağlayıcısı** — SQLite tabanlı, BigDecimal precision, atomik transferler. Hem **Vault** hem **VaultUnlocked** köprüsü.
- ✅ **Auto / Provider / Consumer** modu: harici provider (CMI, EssentialsX) varsa onu tüketir, yoksa kendi ekonomimizi register eder.
- ✅ **Kalıcı günlük transfer limiti** — restart’ta sıfırlanmaz, exploit kapatılır.
- ✅ **Türkçe + İngilizce** dil desteği, oyuncu bazlı dil seçimi (`fatsanutilities.lang.*` permission veya client locale).
- ✅ **Asenkron audit log** — `logs/audit.log` dosyasına yazar, ana thread’i bloklamaz.
- ✅ **Thread-safe state** — tüm cooldown / rate-limit / TPA / pay-limit map’leri `ConcurrentHashMap`.
- ✅ **Player quit cleanup** — bellek sızıntısı ve hayalet TPA istekleri yok.
- ✅ **STARTUP load order** — provider olarak diğer plugin’lerden önce yüklenir.
- ✅ **FoliaPerms / Vault Permission adapter** — offline permission lookup için reflective.

### Komutlar

| Komut | Permission | Açıklama |
|---|---|---|
| `/balance [oyuncu]` | `fatsanutilities.balance` | Bakiye gösterir |
| `/balancetop [sayfa]` | `fatsanutilities.balancetop` | Zenginlik sıralaması (SQL tabanlı, hızlı) |
| `/pay <oyuncu> <miktar>` | `fatsanutilities.pay` | Atomik para transferi |
| `/eco <give\|take\|set\|reset> <oyuncu> [miktar]` | `fatsanutilities.admin.eco` | Admin ekonomi yönetimi |
| `/tpa <oyuncu>` | `fatsanutilities.tpa` | Işınlanma isteği |
| `/tpaccept` / `/tpdeny` | `fatsanutilities.tpa.accept` / `.deny` | Kabul/Red |
| `/rtp [dünya]` | `fatsanutilities.rtp` + `fatsanutilities.rtp.world.<dünya>` | Asenkron güvenli RTP |
| `/spawn` | `fatsanutilities.spawn` | Spawn’a ışınlanma |
| `/itemchat`, `/invchat` | `fatsanutilities.itemchat` / `.invchat` | Sohbette eşya/envanter göster |
| `/invsee <oyuncu> [ender]` | `fatsanutilities.admin.invsee` | Admin envanter görüntüleyici |
| `/fuhelp` | `fatsanutilities.help` | Yetkine göre filtrelenmiş yardım |
| `/fudebug` | `fatsanutilities.admin.debug` | Runtime durumu |
| `/fumodule <modül> <on\|off\|status>` | `fatsanutilities.admin.module` | Modül aç/kapat |
| `/futilitiesreload` | `fatsanutilities.admin.reload` | Config yenile |

### Ekonomi Modu Seçimi
`economy.yml` içindeki `mode` ayarı:
- `auto` (önerilen) — Vault/VaultUnlocked’a kayıtlı bir provider varsa onu tüket; yoksa kendi SQLite ekonomimizi register et.
- `provider` — Her zaman kendi ekonomimizi çalıştır ve register et.
- `consumer` — Sadece harici provider’ı tüket; yoksa hazır olmaz.

Provider modunda veriler `plugins/FatsanUtilities/data/economy.sqlite` dosyasında saklanır (WAL mode, atomik transactions).

### Dil Yönetimi
`config.yml -> plugin.language: tr|en` sunucu varsayılanını belirler. Oyuncuya özel dil için iki yol var:
1. `fatsanutilities.lang.en` permission ata → İngilizce gösterilir.
2. `plugin.follow-client-language: true` aç → oyuncu client locale’ine (`tr` veya `en`) göre otomatik seçilir.

Eksik anahtar olduğunda `tr` fallback’ına düşer; her iki dosya da CI’da parity testiyle doğrulanır.

### Gereksinimler
- Java 21
- Paper veya Folia **1.21.x**
- (Opsiyonel) Vault, VaultUnlocked, FoliaPerms, LuckPerms

### Kurulum
1. JAR’ı `plugins/` klasörüne at, sunucuyu başlat.
2. `plugins/FatsanUtilities/` altındaki YAML’ları düzenle.
3. `/futilitiesreload` ile yeniden yükle.

### Geliştirme
```bash
./gradlew build
./gradlew test
./gradlew runServer
```

---

## English

**Folia-first, Paper 1.21.x compatible, multilingual core server utility plugin.**

Born because Essentials does not run on Folia. Lightweight, fast, hardened, easy to configure; bundles economy, teleport, social and admin modules in a single package.

### Highlights (v0.2.0)
- ✅ **Full Folia-aware scheduler** — adapter over `RegionScheduler`, `EntityScheduler`, `AsyncScheduler`.
- ✅ **Built-in economy provider** — SQLite backed, BigDecimal precision, atomic transfers. Bridges both **Vault** and **VaultUnlocked**.
- ✅ **Auto / Provider / Consumer** mode: consumes an existing provider (CMI, EssentialsX) if present; otherwise registers our own.
- ✅ **Persistent daily transfer limits** — survive restarts.
- ✅ **Turkish + English** localisation with per-player language selection.
- ✅ **Async audit logger** to `logs/audit.log`.
- ✅ **Thread-safe state** everywhere — `ConcurrentHashMap`s for cooldown / rate-limit / TPA / pay-limit.
- ✅ **Player-quit cleanup** to prevent leaks.
- ✅ **STARTUP load order** so we register before consumers initialize.
- ✅ **Reflective FoliaPerms / Vault Permission adapter** for offline permission lookups.

### Economy Modes (`economy.yml -> mode`)
- `auto` (recommended) — consume an existing provider; otherwise register our own.
- `provider` — always register our SQLite economy.
- `consumer` — only consume an external provider.

Provider data is stored at `plugins/FatsanUtilities/data/economy.sqlite` (WAL, atomic transactions).

### Localisation
Set the server-wide default with `config.yml -> plugin.language: tr|en`. Per-player override:
1. Grant `fatsanutilities.lang.en` (or `.tr`).
2. Or enable `plugin.follow-client-language: true` to follow the client locale.

Missing keys fall back to `tr`. A CI parity test ensures both files share the same key set.

### Requirements
- Java 21
- Paper or Folia **1.21.x**
- (Optional) Vault, VaultUnlocked, FoliaPerms, LuckPerms

### Build
```bash
./gradlew build
./gradlew test
./gradlew runServer
```

---

## Migration Notes (0.1.x → 0.2.x)

- `EconomyService` artık `BigDecimal` döner; `pay()` `TransferOutcome` kullanır.
- `EconomyTransferEvent.amount()` `BigDecimal` döndürür (eski `double` için `amountAsDouble()`).
- `messages_tr.yml` formatı korundu, `messages_en.yml` eklendi.
- `economy.yml` içine `mode`, `starting-balance`, `currency.*` alanları eklendi (varsayılanlar mevcut sunucularla uyumludur).
- `BalanceTopCacheService` artık `OfflinePlayer` yerine `TopEntry` döndürür.
- Folia’da çalışmak için `bukkit.getScheduler()` çağrıları kaldırıldı.

## Lisans
Bkz. [LICENSE](LICENSE).
