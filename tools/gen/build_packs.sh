#!/bin/sh
# Reconstrói TODOS os sprites dos packs (brasileiros, luxo e tunagem) a partir dos posters em packs_src/.
# Gera: app/src/main/assets/packs/*.png, app/src/main/java/com/ferrovelho/game/PackData.kt e os recortes individuais em packs_out/.
# Requer: python3 + numpy, scipy, opencv-python, pillow.   Uso (da raiz do projeto):  sh tools/gen/build_packs.sh
set -e
cd "$(dirname "$0")"
OUT=packs_out
ASSET=../../app/src/main/assets/packs
KT=../../app/src/main/java/com/ferrovelho/game/PackData.kt
rm -rf $OUT/cut $OUT/tun "$ASSET"; mkdir -p $OUT "$ASSET"
python3 packs_cars.py $OUT/cut > /dev/null          # recorta os 72 veículos (alta resolução, virados pra direita)
python3 packs_build.py $OUT/cut "$ASSET" x          # reduz, tira rodas, codifica pintura/vidro, medidas
python3 packs_tuning.py $OUT/tun > /dev/null        # recorta cada peça de tunagem
python3 packs_assets.py $OUT/tun "$ASSET"           # rodas esportivas/aerofólios/escapes/adesivos/faróis nos tamanhos do jogo
python3 packs_kotlin.py $OUT/cut/metas.json $OUT/tun/index.json "$ASSET/tune.json" "$KT" > $OUT/body_enum.txt
rm -f "$ASSET/tune.json"
echo "ok: $(ls "$ASSET" | wc -l) arquivos em assets/packs"
