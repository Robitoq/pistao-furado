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

## Carro novo (vetor, sem foto)

O carro agora é **desenhado no código** (`CarArt.kt`), não é mais uma imagem pintada com filtro, então a cor não bugua mais.

- **Formas**: tabela `CAR_SHAPES` (13 carrocerias). Cada uma é uma lista de pontos, é só editar.
- **Destruição**: objeto `Wear` (quantidade de ferrugem, desbotado, sujeira, amassados) e `RustColors`.
- **Lataria** → ferrugem, tinta descascada, massa/primer, amassados, porta de outra cor, farol quebrado, para-choque pendurado, vidro trincado.
- **Motor** → fumaça cinza/preta e faíscas. **Pneus** → careca, calota enferrujada, furado. **Interior** → banco rasgado. **Suspensão** → traseira arriada.
- **Tuning** → rebaixada (`Wear.LOWER`), rodas, neon, película, adesivo, escape, turbo, som, couro, aerofólio, para-choque.

## Cidade (ajustada)

- **Bugs de camada corrigidos**: janelas acesas, estrelas e letreiros não aparecem mais por cima de pessoas, carros e balões de fala. Balões não saem da tela nem se empilham.
- **Menos cara de IA**: prédios do fundo irregulares (antena, caixa d'água, janelas acesas variadas), calçada com juntas e rachaduras diferentes, bueiros, remendos de asfalto, poças, faixa gasta, fios com passarinho, lixeiras, sacos de lixo, gatos e vasos de planta, postes em espaçamento irregular.
- **Falas mais naturais** (`IDLE_LINES` e `PED_LINES` em `City.kt`).

## Encontro de carros 🏁

- Fica na cidade (entre o Desmanche Central e o Pátio Premium). Abre às **18h**, uma participação por dia.
- Você escolhe um carro da garagem e concorre com 5 donos de carro (`Meet.kt`).
- Cada carro ganha **Beleza** (lataria, interior e tuning) e **Estado** (motor, pneus, suspensão), de 0 a 10, e uma nota final.
- Pódio com 1º, 2º e 3º lugar. Prêmios em `MEET_PRIZES` (R$ 4.000 / 2.000 / 800).

## Casa 🏠

- Fica no começo da cidade. De noite (a partir das 19h) é só chamar o **🚕 Táxi → Casa**: ele leva, o personagem dorme e acorda no dia seguinte, às 8h, na porta de casa (`Home.kt`).
- Depois das 23h20 o personagem oferece sozinho o táxi pra casa.
