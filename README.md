# Fatsan Utilities for Server

Folia (öncelikli) ve Paper 1.21.x ile uyumlu, hafif bir survival utilities plugini.

## Özellikler (ilk sürüm)
- Ekonomi: `balance`, `balancetop`, `pay` (VaultUnlocked / Vault API)
- Teleport: `tpa`, `tpaccept`, `tpdeny`, `rtp`, `spawn`
- Sohbet: `itemchat`, `invchat`
- Admin: `invsee` (envanter + zırh + 2. el + enderchest görüntüleme)
- Türkçe mesaj sistemi ve modül bazlı config dosyaları

## Komutlar ve Permission Node'ları
Aşağıdaki tüm komutlar permission kontrollüdür. LuckPerms ile doğrudan verilebilir.

| Komut | Permission | Açıklama |
|---|---|---|
| `/balance [oyuncu]` | `fatsanutilities.balance` | Kendi veya hedef oyuncunun bakiyesini gösterir. |
| `/balancetop [sayfa]` | `fatsanutilities.balancetop` | En yüksek bakiyeye sahip oyuncuları listeler. |
| `/pay <oyuncu> <miktar>` | `fatsanutilities.pay` | Oyuncuya para gönderir. |
| `/tpa <oyuncu>` | `fatsanutilities.tpa` | Hedef oyuncuya ışınlanma isteği gönderir. |
| `/tpaccept [oyuncu]` | `fatsanutilities.tpa.accept` | Gelen TPA isteğini kabul eder. |
| `/tpdeny [oyuncu]` | `fatsanutilities.tpa.deny` | Gelen TPA isteğini reddeder. |
| `/rtp [dünya]` | `fatsanutilities.rtp` + `fatsanutilities.rtp.world.<dünya_adı>` | Oyuncuyu güvenli RTP noktasına ışınlar (dünya seçimi destekli). |
| `/spawn` | `fatsanutilities.spawn` | Oyuncuyu spawn noktasına ışınlar. |
| `/itemchat` | `fatsanutilities.itemchat` | Elde tutulan itemi sohbette hover ile paylaşır. |
| `/invchat` | `fatsanutilities.invchat` | Envanter özetini sohbette hover ile paylaşır. |
| `/invsee <oyuncu> [ender]` | `fatsanutilities.admin.invsee` | Admin için oyuncunun envanter/zırh/2. elini görüntüler; `ender` ile enderchest açar. |
| `/fuhelp` | `fatsanutilities.help` | Yetkine göre kullanılabilir komutları listeler. |
| `/fudebug` | `fatsanutilities.admin.debug` | Modül ve temel runtime debug bilgilerini gösterir. |
| `/fumodule <modül> <on|off|status>` | `fatsanutilities.admin.module` | Modülleri acil durum için aç/kapatır. |
| `/futilitiesreload` | `fatsanutilities.admin.reload` | Plugin configlerini yeniden yükler. |


> Dünya bazlı RTP yetkisi için örnek: `fatsanutilities.rtp.world.world`, `fatsanutilities.rtp.world.world_nether`

### LuckPerms örnekleri
```bash
/lp group default permission set fatsanutilities.balance true
/lp group vip permission set fatsanutilities.rtp true
/lp user OyuncuAdi permission set fatsanutilities.itemchat true
/lp group admin permission set fatsanutilities.admin.invsee true
/lp group vip permission set fatsanutilities.rtp.world.world true
/lp group vip permission set fatsanutilities.rtp.world.world_nether true
```

## Config Dosyaları
- `config.yml` → genel plugin ayarları
- `economy.yml` → ekonomi ayarları
- `cooldowns.yml` → komut cooldown ayarları
- `teleport.yml` → TPA/RTP ayarları
- `messages_tr.yml` → Türkçe mesajlar

## Gereksinimler
- Java 21
- Paper/Folia 1.21.x
- VaultUnlocked (veya Vault API sağlayıcı bir ekonomi plugin)

## Geliştirme
```bash
./gradlew build
./gradlew runServer
```

## RTP Konfigürasyonu
- `teleport.yml -> rtp.default-world`: `/rtp` için varsayılan dünya.
- `teleport.yml -> rtp.cooldown-millis`: RTP cooldown (ms).
- `teleport.yml -> rtp.default.*`: varsayılan min/max koordinat, max attempt, yasak biome/materyal listeleri.
- `teleport.yml -> rtp.worlds.<dünya>`: dünya bazlı override (enabled, min/max, max-attempts, denied listeleri).
- Güvenlik için RTP lava/su/magma/tehlikeli zeminler ve okyanus biome’ları gibi konumlardan kaçınır.

## Faz 1 Altyapı (Yeni)
- `config.yml -> modules.*` ile modül aç/kapat: `economy`, `teleport`, `social`, `admin`.
- `config.yml -> rate-limit.commands.*` ile komut bazlı anti-spam sınırı (ms).
- Reload sonrası config doğrulama (`PluginConfigValidator`) otomatik çalışır ve hatalı değerleri loglar.

## Faz 2 Teleport Geliştirmeleri
- `tpa.overwrite-request`: var olan isteğin üzerine yazma davranışı.
- `tpa.teleport-delay-ticks` + `tpa.cancel-on-move`: TPA kabul sonrası gecikmeli/anti-abuse teleport.
- `spawn.delay-ticks`, `spawn.cancel-on-move`: spawn ışınlanması için gecikme kontrolü.
- `rtp.delay-ticks`, `rtp.cancel-on-move`: RTP için gecikme + hareket iptali.
- RTP artık dünya sınırı dışında kalan lokasyonları reddeder ve sınır kaynaklı başarısızlığı ayrı mesajlar.

## Faz 3 Ekonomi Geliştirmeleri
- `pay.max-amount` ve `pay.daily-limit` ile exploit/spam azaltımı.
- `/balancetop` artık sayfalama destekler ve cache üzerinden çalışır (`cache-ttl-millis`).
- Başarılı para transferlerinde `EconomyTransferEvent` tetiklenir ve audit log basılır.

## Faz 4 Admin/Operasyon Geliştirmeleri
- Yeni acil durum modül komutu: `/fumodule` (module on/off/status).
- Admin debug komutu: `/fudebug`.
- Staff aksiyonları (`invsee`, `reload`, `module`) audit log ile kayıt altına alınır.

## Faz 5 UX ve Yardım Geliştirmeleri
- Permission-aware `/fuhelp` komutu eklendi (oyuncu sadece yetkili olduğu komutları görür).
- `/rtp`, `/invsee`, `/fumodule` komutlarına tab-complete desteği eklendi.
- Placeholder formatlama için `MessageFormatter` utility eklendi (sonraki adımda tüm mesaj akışına yayılacak).

## Faz 6 Kalite ve Release Geliştirmeleri
- JUnit 5 test altyapısı eklendi (`MessageFormatter`, `RateLimitService`, `PayLimitService` testleri).
- CI workflow eklendi (`.github/workflows/ci.yml`) ve build+test gate tanımlandı.
- `RELEASE.md` ile release öncesi smoke/checklist dokümante edildi.
