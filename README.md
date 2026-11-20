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

## Veículos em pixel art (sprites em camadas)

Todos os veículos agora são **pixel art autoral** (nenhum asset de terceiros), desenhados em camadas por `Vehicles.kt`:
`sombra → interior + motorista → poço da roda → RODAS → carroceria`. A carroceria tem os arcos de roda recortados e
os vidros translúcidos, então a roda "entra" no paralama e o motorista aparece pela janela.

- **Modelos**: Fusca, Kombi (bicolor), Uno Mille, Gol quadrado, Corsa, Opala (teto de vinil) + 15 carrocerias do catálogo.
  Só no trânsito: **táxi do Rio** (amarelo com faixa azul e luminoso), **moto** e **moto-entrega**, **caminhão baú**,
  **caminhão de carga (sacos)** e **ônibus** urbano.
- **Cor**: a pintura usa 5 tons-marcador; em tempo de execução eles viram o degradê da cor escolhida (brilho/claro/base/sombra/escuro). Sem filtro lavado.
- **Desgaste** (assado no sprite, em cache): ferrugem, tinta desbotada, massa/primer, amassados, riscos, sujeira, porta de outra cor, farol quebrado, vidro trincado.
- **Rodas**: atlas com 6 quadros de giro + quadro borrado em alta velocidade; variantes aço, esportiva, enferrujada, pneu gasto e furado. Posições vêm de `VehicleData.kt` (gerado junto da arte), então **não desalinham**.
- **Motor** → fumaça/faíscas. **Interior** → banco rasgado/couro. **Suspensão** → traseira arriada. **Tuning** → rebaixada, rodas, neon, película, adesivo, escape, turbo, som, couro, aerofólio, para-choque.
- **Trânsito**: cada carro mantém distância do da frente (ninguém se atravessa), freia se o jogador parar na faixa, e o jogador ultrapassa pelo meio. Ordem de desenho por profundidade (y do chão).

### Como editar a arte
```
cd tools/gen
python3 build_assets.py     # requer Python 3 + pillow + numpy
```
Isso regrava todos os PNG em `app/src/main/assets/city/` e o `VehicleData.kt`. As silhuetas ficam em `heroes.py` / `models.py`,
as rodas em `wheels.py`, prédios em `scenery.py`, adereços em `props.py`. `scene_test.py` monta uma cena de teste.
Os PNG de carro usam cores-marcador magenta (veja `px.py`): não use esses tons para outras coisas.

## Cidade (ajustada)

- **Cenário brasileiro**: sobrados com portão de garagem, casas de laje com tijolo aparente e caixa d'água, prédios com varanda e ar-condicionado, padaria, farmácia, mercado, boteco com bandeirinhas, igrejinha, kitnet; postes de concreto com cruzeta, transformador e fios emaranhados; coqueiro, mangueira e ipê; placas PARE/estacionamento/lombada; ponto de ônibus, orelhão, carrinho de milho; calçada de concreto e de **pedra portuguesa**, faixa amarela central, faixa de pedestres.

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
