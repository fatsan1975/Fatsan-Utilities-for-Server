# Release Checklist (Faz 6)

## Build Gate
- `./gradlew test` geçmeli.
- `./gradlew build` geçmeli.

## Manual Smoke (Paper/Folia 1.21.x)
- Economy: `/balance`, `/balancetop`, `/pay`
- Teleport: `/tpa`, `/tpaccept`, `/tpdeny`, `/spawn`, `/rtp`
- Social: `/itemchat`, `/invchat`
- Admin: `/invsee`, `/fudebug`, `/fumodule`, `/futilitiesreload`, `/fuhelp`

## Runtime Safety
- `teleport.yml` ve `economy.yml` kritik limitler doğrulanmalı.
- Admin audit log satırları çalışmalı (`[AUDIT]`).
- Module toggle sonrası davranış doğrulanmalı.

## Versioning
- `version` güncellendi mi?
- README ve değişiklik notları güncellendi mi?
