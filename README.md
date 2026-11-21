# Ferro Velho 🚗

Jogo offline para Android (Kotlin + Jetpack Compose, 100% nativo, sem internet).

Você compra carros brasileiros (e alguns importados) em ferros-velhos, conserta peça por peça,
tuna, pinta e revende pro comprador que paga mais.

## Como o jogo funciona

- **3 ferros-velhos**: Ferro Velho do Zé (barato), Desmanche Central (médio) e Pátio Premium (esportivos/SUVs/importados).
- **Oficina**: motor, lataria, pneus, interior e suspensão. Cada peça pesa diferente no valor do carro.
- **Tuning**: som, rodas, rebaixada, película, adesivo, escape, couro, neon, turbo. O desenho do carro muda.
- **Pintura**: 14 cores.
- **Venda**: 3 ofertas por dia, de compradores diferentes (tunador, colecionador, fazendeiro, revendedor...).
- **Salva sozinho** no celular.

## Como gerar o APK (sem computador)

1. Suba esta pasta inteira num repositório no GitHub.
2. Aba **Actions** → **Build APK** → **Run workflow** (também roda sozinho a cada push).
3. Quando terminar, abra a execução e baixe o artefato **FerroVelho-APK** (vem num .zip).
4. Extraia, mande o `app-debug.apk` pro celular e instale (permita "fontes desconhecidas").

## Como compilar no computador

Abra a pasta no Android Studio e rode, ou use `gradle assembleDebug` (Gradle 8.9+, JDK 17).
O APK sai em `app/build/outputs/apk/debug/app-debug.apk`.

## Adicionar carros

Edite `app/src/main/java/com/ferrovelho/game/Cars.kt` e inclua uma linha:

```kotlin
m("Marca", "Modelo", valorEmReais, Corpo, Categoria, anoInicial, anoFinal),
```

Os valores e anos do catálogo são aproximados, feitos só para o jogo.

## Packs de veículos (brasileiros + luxo + tunagem)

Os três posters (`tools/gen/packs_src/`) viraram sprites individuais e estão integrados ao jogo:

- **72 veículos novos** (`assets/packs/pb<N>.png` carroceria, `pi<N>.png` interior, `pw<N>.png` rodas originais): 40 brasileiros, 5 motos
  (com piloto), 2 ônibus, 1 caminhão, 1 van e 24 carros de luxo. Aparecem no catálogo (ver `Cars.kt`, `PACK_SHAPE` e os modelos novos no
  fim de `CATALOG`) e no trânsito da cidade.
- **Cor**: a lataria é recolorida por pixel (qualquer das 33 tintas, 19 delas vindas do pack de tunagem), com o desgaste/ferrugem do jogo.
- **Tuning visível em TODOS os carros**: rodas esportivas (6 desenhos do pack, giram), aerofólio (6 desenhos, 3 tamanhos),
  escapamento/ponteira, adesivos (6 desenhos, só em cima da lataria), faróis, insulfilm (vidro escuro), suspensão rebaixada (carro desce +
  mola colorida do coilover aparece), neon (5 cores), para-choque, couro, som, turbo.
- Recortes individuais de cada peça (30 rodas, 12 aerofólios, 12 para-choques, 7 coilovers, 8 escapes, 12 adesivos, 12 faróis, bancos,
  volantes, som, extras, emblemas, efeitos...) estão em `tools/gen/packs_out/tun/`; os veículos em alta resolução, em `packs_out/cut/`.
- Para regenerar tudo: `sh tools/gen/build_packs.sh` (precisa de python3 + numpy, scipy, opencv-python, pillow).
  Teste de consistência: `python3 tools/gen/packs_selftest.py`. Pré-visualização: `tools/gen/packs_preview.py`.
