package com.ferrovelho.game

// GERADO por tools/gen/packs_kotlin.py a partir dos packs (brasileiros + luxo + tunagem) — não edite à mão.

const val P_BASE = 27            // primeiro índice de sprite dos packs (0..26 = sprites originais)
const val P_COUNT = 72
const val V_TOTAL = P_BASE + P_COUNT

const val PK_FUSCA = 27
const val PK_KOMBI = 28
const val PK_UNO = 29
const val PK_GOL_Q = 30
const val PK_GOL_G4 = 31
const val PK_CORSA = 32
const val PK_PALIO = 33
const val PK_STRADA = 34
const val PK_SIENA = 35
const val PK_CIVIC = 36
const val PK_COROLLA = 37
const val PK_ETIOS = 38
const val PK_OPALA = 39
const val PK_CHEVETTE = 40
const val PK_MAVERICK = 41
const val PK_DELREY = 42
const val PK_VOYAGE = 43
const val PK_SANTANA = 44
const val PK_MONZA = 45
const val PK_OMEGA = 46
const val PK_KADETT = 47
const val PK_LOGUS = 48
const val PK_TEMPRA = 49
const val PK_P206 = 50
const val PK_HB20 = 51
const val PK_SANDERO = 52
const val PK_DUSTER = 53
const val PK_ECOSPORT = 54
const val PK_TRACKER = 55
const val PK_CRETA = 56
const val PK_COMPASS = 57
const val PK_RENEGADE = 58
const val PK_F1000 = 59
const val PK_C10 = 60
const val PK_D20 = 61
const val PK_RANGER = 62
const val PK_S10 = 63
const val PK_AMAROK = 64
const val PK_HILUX = 65
const val PK_MB1113 = 66
const val PK_ONIBUS = 67
const val PK_ESCOLAR = 68
const val PK_MOTO_CG = 69
const val PK_MOTO_TITAN = 70
const val PK_MOTO_FAN = 71
const val PK_MOTO_BIZ = 72
const val PK_MOTO_ENTREGA = 73
const val PK_VAN = 74
const val PK_L_PORSCHE = 75
const val PK_L_FERRARI = 76
const val PK_L_LAMBO = 77
const val PK_L_BUGATTI = 78
const val PK_L_MCLAREN = 79
const val PK_L_ROLLS = 80
const val PK_L_BMW = 81
const val PK_L_MERCEDES = 82
const val PK_L_AUDI = 83
const val PK_L_LEXUS = 84
const val PK_L_MASERATI = 85
const val PK_L_BENTLEY = 86
const val PK_L_JAGUAR = 87
const val PK_L_TESLA = 88
const val PK_L_RANGEROVER = 89
const val PK_L_CADILLAC = 90
const val PK_L_CORVETTE = 91
const val PK_L_VIPER = 92
const val PK_L_MCLAREN_F1 = 93
const val PK_L_KOENIGSEGG = 94
const val PK_L_PAGANI = 95
const val PK_L_ASTON = 96
const val PK_L_SENNA = 97
const val PK_L_REVUELTO = 98

val PACK_NAMES: List<String> = listOf("Fusca", "Kombi", "Uno Mille", "Gol Quadrado", "Gol G4", "Corsa", "Palio", "Strada", "Siena", "Civic", "Corolla", "Etios", "Opala", "Chevette", "Maverick", "Del Rey", "Voyage", "Santana", "Monza", "Omega", "Kadett", "Logus", "Tempra", "Peugeot 206", "HB20", "Sandero", "Duster", "EcoSport", "Tracker", "Creta", "Compass", "Renegade", "F-1000", "C10", "D-20", "Ranger", "S10", "Amarok", "Hilux", "Mercedes-Benz 1113", "Ônibus Urbano", "Ônibus Escolar", "Honda CG", "Titan 150", "Fan 160", "Biz", "Moto Delivery", "Van", "Porsche 911 Turbo", "Ferrari", "Lamborghini", "Bugatti", "McLaren", "Rolls-Royce", "BMW M5", "Mercedes-Benz", "Audi R8", "Lexus", "Maserati", "Bentley", "Jaguar", "Tesla", "Range Rover", "Cadillac", "Corvette", "Dodge Viper", "McLaren F1", "Koenigsegg", "Pagani", "Aston Martin", "McLaren Senna", "Lamborghini Revuelto")
val PACK_KIND: List<String> = listOf("beetle", "van", "hatch", "hatch", "hatch", "hatch", "hatch", "pickup", "sedan", "sedan", "sedan", "sedan", "sedan", "hatch", "coupe", "sedan", "sedan", "sedan", "sedan", "sedan", "hatch", "hatch", "sedan", "hatch", "hatch", "hatch", "suv", "suv", "suv", "suv", "suv", "suv", "pickup", "pickup", "pickup", "pickup", "pickup", "pickup", "pickup", "truck", "bus", "bus", "moto", "moto", "moto", "moto", "moto", "van", "super", "super", "super", "super", "super", "sedan", "sedan", "coupe", "super", "coupe", "sedan", "sedan", "coupe", "sedan", "suv", "sedan", "super", "super", "super", "super", "super", "coupe", "super", "super")

val PMETA: List<VMeta> = listOf(
    VMeta("fusca", 70, 32, 35, 29, 11, intArrayOf(11, 49), intArrayOf(18, 18), 6.0f, 26,
        intArrayOf(29, 8), intArrayOf(19, 7, 44, 12), intArrayOf(19, 14, 44, 18), intArrayOf(6, 15), intArrayOf(3, 24),
        66, 3, intArrayOf(65, 16), intArrayOf(4, 19), intArrayOf(59, 15, 4, 3), intArrayOf(3, 18, 2, 3), 14, true, true),
    VMeta("kombi", 73, 38, 36, 35, 10, intArrayOf(13, 49), intArrayOf(25, 25), 5.5f, 33,
        intArrayOf(30, 9), intArrayOf(7, 8, 66, 14), intArrayOf(7, 16, 66, 21), intArrayOf(6, 5), intArrayOf(3, 31),
        69, 3, intArrayOf(68, 13), intArrayOf(4, 9), intArrayOf(62, 12, 4, 3), intArrayOf(3, 8, 2, 3), 5, true, true),
    VMeta("uno", 64, 32, 32, 29, 11, intArrayOf(8, 43), intArrayOf(18, 18), 6.0f, 24,
        intArrayOf(24, 7), intArrayOf(17, 6, 36, 10), intArrayOf(17, 12, 36, 16), intArrayOf(6, 8), intArrayOf(3, 22),
        60, 3, intArrayOf(59, 17), intArrayOf(4, 12), intArrayOf(56, 16, 2, 2), intArrayOf(3, 11, 2, 3), 13, true, true),
    VMeta("gol_q", 66, 31, 33, 28, 11, intArrayOf(9, 45), intArrayOf(17, 17), 6.0f, 23,
        intArrayOf(25, 6), intArrayOf(14, 5, 42, 12), intArrayOf(14, 14, 42, 17), intArrayOf(6, 11), intArrayOf(3, 21),
        62, 3, intArrayOf(61, 14), intArrayOf(4, 15), intArrayOf(55, 13, 4, 3), intArrayOf(5, 14, 2, 2), 12, true, true),
    VMeta("gol_g4", 65, 30, 32, 27, 11, intArrayOf(8, 44), intArrayOf(16, 16), 6.0f, 23,
        intArrayOf(27, 7), intArrayOf(20, 6, 38, 10), intArrayOf(20, 12, 38, 15), intArrayOf(6, 10), intArrayOf(3, 21),
        61, 3, intArrayOf(60, 16), intArrayOf(4, 14), intArrayOf(56, 15, 3, 2), intArrayOf(3, 13, 2, 3), 12, true, true),
    VMeta("corsa", 66, 31, 33, 28, 11, intArrayOf(9, 45), intArrayOf(17, 17), 6.0f, 25,
        intArrayOf(26, 6), intArrayOf(15, 5, 43, 12), intArrayOf(15, 14, 43, 17), intArrayOf(6, 10), intArrayOf(3, 23),
        62, 3, intArrayOf(61, 14), intArrayOf(4, 14), intArrayOf(55, 13, 4, 3), intArrayOf(5, 13, 2, 2), 12, true, true),
    VMeta("palio", 67, 31, 33, 28, 11, intArrayOf(9, 46), intArrayOf(17, 17), 6.0f, 23,
        intArrayOf(25, 6), intArrayOf(15, 5, 40, 12), intArrayOf(15, 14, 40, 17), intArrayOf(6, 11), intArrayOf(3, 21),
        63, 3, intArrayOf(62, 17), intArrayOf(4, 14), intArrayOf(58, 16, 3, 2), intArrayOf(6, 13, 2, 2), 13, true, true),
    VMeta("strada", 73, 32, 36, 29, 10, intArrayOf(12, 52), intArrayOf(19, 19), 5.5f, 24,
        intArrayOf(37, 8), intArrayOf(33, 7, 44, 10), intArrayOf(33, 12, 44, 16), intArrayOf(6, 10), intArrayOf(3, 22),
        69, 3, intArrayOf(68, 17), intArrayOf(4, 14), intArrayOf(64, 16, 3, 3), intArrayOf(3, 13, 2, 3), 13, true, true),
    VMeta("siena", 67, 31, 33, 28, 11, intArrayOf(9, 46), intArrayOf(17, 17), 6.0f, 25,
        intArrayOf(25, 6), intArrayOf(15, 5, 42, 12), intArrayOf(15, 14, 42, 17), intArrayOf(6, 10), intArrayOf(3, 23),
        63, 3, intArrayOf(62, 15), intArrayOf(4, 14), intArrayOf(56, 14, 4, 3), intArrayOf(3, 13, 2, 3), 12, true, true),
    VMeta("civic", 70, 30, 35, 27, 11, intArrayOf(10, 48), intArrayOf(16, 16), 6.0f, 22,
        intArrayOf(25, 6), intArrayOf(15, 5, 42, 15), intArrayOf(15, 16, 42, 19), intArrayOf(6, 10), intArrayOf(3, 20),
        66, 3, intArrayOf(65, 16), intArrayOf(4, 14), intArrayOf(60, 15, 3, 2), intArrayOf(3, 13, 2, 3), 13, true, true),
    VMeta("corolla", 70, 29, 35, 26, 11, intArrayOf(11, 48), intArrayOf(15, 15), 6.0f, 23,
        intArrayOf(27, 6), intArrayOf(17, 5, 44, 11), intArrayOf(17, 13, 44, 16), intArrayOf(6, 9), intArrayOf(3, 21),
        66, 3, intArrayOf(65, 14), intArrayOf(4, 13), intArrayOf(59, 13, 4, 3), intArrayOf(5, 12, 3, 2), 12, true, true),
    VMeta("etios", 64, 30, 32, 27, 11, intArrayOf(9, 44), intArrayOf(16, 16), 6.0f, 24,
        intArrayOf(26, 7), intArrayOf(20, 6, 37, 10), intArrayOf(20, 12, 37, 16), intArrayOf(6, 10), intArrayOf(3, 22),
        60, 3, intArrayOf(59, 15), intArrayOf(4, 14), intArrayOf(55, 14, 3, 2), intArrayOf(3, 13, 2, 3), 12, true, true),
    VMeta("opala", 74, 29, 37, 26, 10, intArrayOf(15, 53), intArrayOf(16, 16), 5.5f, 23,
        intArrayOf(30, 7), intArrayOf(19, 6, 48, 10), intArrayOf(19, 12, 48, 15), intArrayOf(6, 11), intArrayOf(3, 21),
        70, 3, intArrayOf(69, 13), intArrayOf(4, 15), intArrayOf(63, 12, 4, 3), intArrayOf(3, 14, 2, 3), 11, true, true),
    VMeta("chevette", 59, 28, 29, 25, 9, intArrayOf(10, 41), intArrayOf(16, 16), 5.0f, 21,
        intArrayOf(22, 6), intArrayOf(10, 5, 41, 13), intArrayOf(10, 15, 41, 17), intArrayOf(6, 11), intArrayOf(3, 19),
        55, 3, intArrayOf(54, 15), intArrayOf(4, 15), intArrayOf(52, 14, 3, 2), intArrayOf(3, 14, 2, 3), 11, true, true),
    VMeta("maverick", 70, 29, 35, 26, 11, intArrayOf(12, 48), intArrayOf(15, 15), 6.0f, 23,
        intArrayOf(30, 7), intArrayOf(21, 6, 45, 10), intArrayOf(21, 12, 45, 15), intArrayOf(6, 10), intArrayOf(3, 21),
        66, 3, intArrayOf(65, 13), intArrayOf(4, 14), intArrayOf(59, 12, 4, 3), intArrayOf(3, 13, 2, 3), 11, true, true),
    VMeta("delrey", 70, 30, 35, 27, 10, intArrayOf(13, 49), intArrayOf(17, 17), 5.5f, 22,
        intArrayOf(27, 4), intArrayOf(13, 3, 49, 12), intArrayOf(13, 14, 49, 17), intArrayOf(6, 11), intArrayOf(3, 20),
        66, 3, intArrayOf(65, 16), intArrayOf(4, 15), intArrayOf(63, 15, 2, 2), intArrayOf(3, 14, 2, 3), 12, true, true),
    VMeta("voyage", 72, 30, 36, 27, 11, intArrayOf(12, 49), intArrayOf(16, 16), 6.0f, 22,
        intArrayOf(30, 7), intArrayOf(23, 6, 41, 10), intArrayOf(23, 12, 41, 15), intArrayOf(6, 10), intArrayOf(3, 20),
        68, 3, intArrayOf(67, 16), intArrayOf(4, 14), intArrayOf(62, 15, 4, 2), intArrayOf(3, 13, 2, 3), 12, true, true),
    VMeta("santana", 73, 32, 36, 29, 10, intArrayOf(13, 52), intArrayOf(19, 19), 5.5f, 26,
        intArrayOf(29, 6), intArrayOf(17, 5, 48, 12), intArrayOf(17, 14, 48, 18), intArrayOf(6, 10), intArrayOf(3, 24),
        69, 3, intArrayOf(68, 15), intArrayOf(4, 14), intArrayOf(62, 14, 4, 3), intArrayOf(3, 13, 2, 3), 13, true, true),
    VMeta("monza", 68, 30, 34, 27, 11, intArrayOf(10, 46), intArrayOf(16, 16), 6.0f, 22,
        intArrayOf(26, 6), intArrayOf(15, 5, 43, 15), intArrayOf(15, 16, 43, 19), intArrayOf(6, 10), intArrayOf(3, 20),
        64, 3, intArrayOf(63, 17), intArrayOf(4, 14), intArrayOf(59, 16, 3, 2), intArrayOf(3, 13, 2, 3), 13, true, true),
    VMeta("omega", 67, 30, 33, 27, 11, intArrayOf(10, 47), intArrayOf(16, 16), 6.0f, 24,
        intArrayOf(27, 7), intArrayOf(16, 6, 45, 11), intArrayOf(16, 13, 45, 16), intArrayOf(6, 10), intArrayOf(3, 22),
        63, 3, intArrayOf(62, 14), intArrayOf(4, 14), intArrayOf(56, 13, 4, 3), intArrayOf(3, 13, 2, 3), 12, true, true),
    VMeta("kadett", 64, 30, 32, 27, 11, intArrayOf(9, 45), intArrayOf(16, 16), 6.0f, 22,
        intArrayOf(25, 7), intArrayOf(17, 6, 37, 10), intArrayOf(17, 12, 37, 15), intArrayOf(6, 10), intArrayOf(3, 20),
        60, 3, intArrayOf(59, 16), intArrayOf(4, 14), intArrayOf(56, 15, 3, 2), intArrayOf(3, 13, 2, 3), 12, true, true),
    VMeta("logus", 65, 30, 32, 27, 11, intArrayOf(9, 44), intArrayOf(16, 16), 6.0f, 22,
        intArrayOf(25, 6), intArrayOf(15, 5, 42, 12), intArrayOf(15, 14, 42, 17), intArrayOf(6, 10), intArrayOf(3, 20),
        61, 3, intArrayOf(60, 16), intArrayOf(4, 14), intArrayOf(56, 15, 3, 2), intArrayOf(3, 13, 2, 3), 12, true, true),
    VMeta("tempra", 79, 31, 39, 28, 11, intArrayOf(15, 57), intArrayOf(17, 17), 6.0f, 23,
        intArrayOf(30, 7), intArrayOf(17, 6, 50, 12), intArrayOf(17, 14, 50, 17), intArrayOf(6, 10), intArrayOf(3, 21),
        75, 3, intArrayOf(74, 17), intArrayOf(4, 14), intArrayOf(70, 16, 3, 2), intArrayOf(3, 13, 2, 3), 13, true, true),
    VMeta("p206", 63, 32, 31, 29, 10, intArrayOf(7, 43), intArrayOf(19, 19), 5.5f, 27,
        intArrayOf(24, 7), intArrayOf(13, 6, 41, 13), intArrayOf(13, 15, 41, 19), intArrayOf(6, 9), intArrayOf(3, 25),
        59, 3, intArrayOf(58, 15), intArrayOf(4, 13), intArrayOf(52, 14, 4, 3), intArrayOf(3, 12, 2, 3), 13, true, true),
    VMeta("hb20", 68, 31, 34, 28, 11, intArrayOf(10, 47), intArrayOf(17, 17), 6.0f, 24,
        intArrayOf(26, 7), intArrayOf(15, 6, 44, 12), intArrayOf(15, 14, 44, 17), intArrayOf(6, 10), intArrayOf(3, 22),
        64, 3, intArrayOf(63, 15), intArrayOf(4, 14), intArrayOf(57, 14, 4, 3), intArrayOf(6, 13, 2, 2), 13, true, true),
    VMeta("sandero", 66, 32, 33, 29, 11, intArrayOf(8, 45), intArrayOf(18, 18), 6.0f, 24,
        intArrayOf(26, 7), intArrayOf(18, 6, 38, 11), intArrayOf(18, 13, 38, 16), intArrayOf(6, 9), intArrayOf(3, 22),
        62, 3, intArrayOf(61, 17), intArrayOf(4, 13), intArrayOf(57, 16, 3, 2), intArrayOf(3, 12, 2, 3), 13, true, true),
    VMeta("duster", 69, 33, 34, 30, 10, intArrayOf(10, 48), intArrayOf(20, 20), 5.5f, 28,
        intArrayOf(22, 4), intArrayOf(4, 3, 49, 18), intArrayOf(4, 20, 49, 23), intArrayOf(6, 8), intArrayOf(3, 26),
        65, 3, intArrayOf(64, 15), intArrayOf(4, 15), intArrayOf(58, 14, 4, 3), intArrayOf(5, 14, 2, 2), 14, true, true),
    VMeta("ecosport", 65, 33, 32, 30, 10, intArrayOf(9, 45), intArrayOf(20, 20), 5.5f, 25,
        intArrayOf(25, 7), intArrayOf(13, 6, 43, 13), intArrayOf(13, 15, 43, 18), intArrayOf(6, 10), intArrayOf(3, 23),
        61, 3, intArrayOf(60, 17), intArrayOf(4, 15), intArrayOf(55, 16, 3, 2), intArrayOf(6, 14, 2, 3), 14, true, true),
    VMeta("tracker", 66, 32, 33, 29, 10, intArrayOf(9, 45), intArrayOf(19, 19), 5.5f, 24,
        intArrayOf(25, 6), intArrayOf(13, 5, 43, 13), intArrayOf(13, 15, 43, 18), intArrayOf(6, 8), intArrayOf(3, 22),
        62, 3, intArrayOf(61, 15), intArrayOf(4, 14), intArrayOf(55, 14, 4, 3), intArrayOf(5, 13, 2, 2), 13, true, true),
    VMeta("creta", 66, 32, 33, 29, 10, intArrayOf(9, 46), intArrayOf(19, 19), 5.5f, 24,
        intArrayOf(24, 8), intArrayOf(15, 7, 38, 11), intArrayOf(15, 13, 38, 16), intArrayOf(6, 9), intArrayOf(3, 22),
        62, 3, intArrayOf(61, 16), intArrayOf(4, 13), intArrayOf(56, 15, 4, 2), intArrayOf(3, 12, 2, 3), 13, true, true),
    VMeta("compass", 70, 32, 35, 29, 10, intArrayOf(10, 49), intArrayOf(19, 19), 5.5f, 24,
        intArrayOf(22, 4), intArrayOf(4, 3, 49, 14), intArrayOf(4, 16, 49, 19), intArrayOf(6, 6), intArrayOf(3, 22),
        66, 3, intArrayOf(65, 14), intArrayOf(4, 14), intArrayOf(59, 13, 4, 3), intArrayOf(5, 13, 2, 2), 12, true, true),
    VMeta("renegade", 64, 32, 32, 29, 10, intArrayOf(8, 44), intArrayOf(19, 19), 5.5f, 24,
        intArrayOf(18, 7), intArrayOf(12, 6, 27, 12), intArrayOf(12, 14, 27, 17), intArrayOf(6, 6), intArrayOf(3, 22),
        60, 3, intArrayOf(59, 17), intArrayOf(4, 10), intArrayOf(56, 16, 2, 2), intArrayOf(3, 9, 2, 3), 13, true, true),
    VMeta("f1000", 74, 32, 37, 29, 10, intArrayOf(14, 53), intArrayOf(19, 19), 5.5f, 23,
        intArrayOf(41, 7), intArrayOf(37, 6, 47, 13), intArrayOf(37, 15, 47, 18), intArrayOf(6, 11), intArrayOf(3, 21),
        70, 3, intArrayOf(69, 16), intArrayOf(4, 15), intArrayOf(67, 15, 3, 2), intArrayOf(3, 14, 2, 3), 12, true, true),
    VMeta("c10", 68, 29, 34, 26, 11, intArrayOf(12, 48), intArrayOf(15, 15), 6.0f, 23,
        intArrayOf(23, 4), intArrayOf(7, 3, 48, 15), intArrayOf(7, 17, 48, 19), intArrayOf(6, 10), intArrayOf(3, 21),
        64, 3, intArrayOf(63, 15), intArrayOf(4, 14), intArrayOf(61, 14, 3, 2), intArrayOf(3, 13, 2, 3), 11, true, true),
    VMeta("d20", 71, 32, 35, 29, 10, intArrayOf(13, 51), intArrayOf(19, 19), 5.5f, 26,
        intArrayOf(41, 6), intArrayOf(36, 5, 49, 11), intArrayOf(36, 13, 49, 17), intArrayOf(6, 10), intArrayOf(3, 24),
        67, 3, intArrayOf(66, 14), intArrayOf(4, 14), intArrayOf(60, 13, 4, 3), intArrayOf(3, 13, 2, 3), 12, true, true),
    VMeta("ranger", 68, 31, 34, 28, 10, intArrayOf(13, 49), intArrayOf(18, 18), 5.5f, 25,
        intArrayOf(39, 7), intArrayOf(35, 6, 45, 11), intArrayOf(35, 13, 45, 17), intArrayOf(6, 10), intArrayOf(3, 23),
        64, 3, intArrayOf(63, 14), intArrayOf(4, 14), intArrayOf(57, 13, 4, 3), intArrayOf(3, 13, 2, 3), 12, true, true),
    VMeta("s10", 67, 29, 33, 26, 11, intArrayOf(11, 47), intArrayOf(15, 15), 6.0f, 21,
        intArrayOf(30, 5), intArrayOf(21, 4, 45, 9), intArrayOf(21, 11, 45, 14), intArrayOf(6, 10), intArrayOf(3, 19),
        63, 3, intArrayOf(62, 15), intArrayOf(4, 14), intArrayOf(59, 14, 2, 2), intArrayOf(3, 13, 2, 3), 11, true, true),
    VMeta("amarok", 69, 32, 34, 29, 10, intArrayOf(12, 50), intArrayOf(19, 19), 5.5f, 24,
        intArrayOf(36, 7), intArrayOf(30, 6, 46, 13), intArrayOf(30, 15, 46, 18), intArrayOf(6, 11), intArrayOf(3, 22),
        65, 3, intArrayOf(64, 16), intArrayOf(4, 15), intArrayOf(61, 15, 2, 2), intArrayOf(3, 14, 2, 3), 12, true, true),
    VMeta("hilux", 73, 32, 36, 29, 10, intArrayOf(13, 53), intArrayOf(19, 19), 5.5f, 26,
        intArrayOf(40, 7), intArrayOf(34, 6, 49, 12), intArrayOf(34, 14, 49, 18), intArrayOf(6, 11), intArrayOf(3, 24),
        69, 3, intArrayOf(68, 14), intArrayOf(4, 15), intArrayOf(62, 13, 4, 3), intArrayOf(4, 14, 2, 3), 12, true, true),
    VMeta("mb1113", 64, 35, 32, 32, 9, intArrayOf(10, 44), intArrayOf(23, 23), 5.0f, 27,
        intArrayOf(28, 5), intArrayOf(20, 4, 42, 11), intArrayOf(20, 13, 42, 17), intArrayOf(6, 3), intArrayOf(3, 25),
        60, 3, intArrayOf(59, 19), intArrayOf(4, 7), intArrayOf(57, 18, 3, 2), intArrayOf(3, 6, 2, 3), 15, true, true),
    VMeta("onibus", 96, 40, 48, 37, 12, intArrayOf(20, 64), intArrayOf(25, 25), 6.5f, 33,
        intArrayOf(39, 4), intArrayOf(4, 3, 92, 22), intArrayOf(4, 24, 92, 27), intArrayOf(6, 5), intArrayOf(3, 31),
        92, 3, intArrayOf(91, 14), intArrayOf(4, 9), intArrayOf(89, 13, 3, 2), intArrayOf(3, 8, 2, 3), 5, true, true),
    VMeta("escolar", 95, 39, 47, 36, 11, intArrayOf(19, 64), intArrayOf(25, 25), 6.0f, 33,
        intArrayOf(40, 4), intArrayOf(7, 3, 91, 21), intArrayOf(7, 23, 91, 26), intArrayOf(6, 3), intArrayOf(3, 31),
        91, 3, intArrayOf(90, 5), intArrayOf(4, 7), intArrayOf(84, 4, 3, 3), intArrayOf(3, 6, 2, 3), 3, true, true),
    VMeta("moto_cg", 43, 29, 21, 26, 12, intArrayOf(3, 28), intArrayOf(14, 14), 6.5f, 21,
        intArrayOf(0, 1), intArrayOf(0, 0, 0, 0), intArrayOf(11, 11, 30, 19), intArrayOf(6, 3), intArrayOf(3, 19),
        38, 3, intArrayOf(37, 13), intArrayOf(4, 7), intArrayOf(35, 12, 3, 2), intArrayOf(3, 6, 2, 3), 9, false, true),
    VMeta("moto_titan", 43, 29, 21, 26, 12, intArrayOf(3, 28), intArrayOf(14, 14), 6.5f, 21,
        intArrayOf(0, 1), intArrayOf(0, 0, 0, 0), intArrayOf(11, 11, 30, 19), intArrayOf(6, 25), intArrayOf(3, 19),
        38, 3, intArrayOf(37, 13), intArrayOf(4, 29), intArrayOf(35, 12, 3, 2), intArrayOf(3, 28, 2, 3), 9, false, true),
    VMeta("moto_fan", 44, 29, 22, 26, 12, intArrayOf(4, 28), intArrayOf(14, 14), 6.5f, 21,
        intArrayOf(0, 1), intArrayOf(0, 0, 0, 0), intArrayOf(11, 5, 32, 19), intArrayOf(6, 9), intArrayOf(3, 19),
        40, 3, intArrayOf(39, 9), intArrayOf(4, 13), intArrayOf(37, 8, 3, 2), intArrayOf(3, 12, 2, 3), 3, false, true),
    VMeta("moto_biz", 41, 29, 20, 26, 11, intArrayOf(4, 27), intArrayOf(15, 15), 6.0f, 22,
        intArrayOf(0, 1), intArrayOf(0, 0, 0, 0), intArrayOf(11, 12, 29, 20), intArrayOf(6, 16), intArrayOf(3, 20),
        37, 3, intArrayOf(36, 14), intArrayOf(4, 20), intArrayOf(34, 13, 3, 2), intArrayOf(3, 19, 2, 3), 10, false, true),
    VMeta("moto_entrega", 45, 32, 22, 29, 11, intArrayOf(6, 6), intArrayOf(18, 18), 6.0f, 28,
        intArrayOf(0, 1), intArrayOf(0, 0, 0, 0), intArrayOf(11, 19, 33, 26), intArrayOf(6, 3), intArrayOf(3, 26),
        41, 3, intArrayOf(40, 21), intArrayOf(4, 7), intArrayOf(38, 20, 3, 2), intArrayOf(3, 6, 2, 3), 17, false, true),
    VMeta("van", 69, 40, 34, 37, 9, intArrayOf(12, 49), intArrayOf(28, 28), 5.0f, 32,
        intArrayOf(25, 4), intArrayOf(5, 3, 56, 21), intArrayOf(5, 23, 56, 26), intArrayOf(6, 5), intArrayOf(3, 30),
        65, 3, intArrayOf(64, 21), intArrayOf(4, 23), intArrayOf(58, 20, 4, 3), intArrayOf(5, 21, 2, 4), 18, true, true),
    VMeta("l_porsche", 74, 28, 37, 25, 10, intArrayOf(13, 51), intArrayOf(15, 15), 5.5f, 23,
        intArrayOf(33, 5), intArrayOf(23, 4, 49, 9), intArrayOf(23, 11, 49, 15), intArrayOf(6, 5), intArrayOf(3, 21),
        70, 3, intArrayOf(69, 13), intArrayOf(4, 9), intArrayOf(63, 12, 4, 3), intArrayOf(3, 8, 2, 3), 11, true, true),
    VMeta("l_ferrari", 75, 28, 37, 25, 12, intArrayOf(11, 51), intArrayOf(13, 13), 6.5f, 22,
        intArrayOf(26, 6), intArrayOf(21, 5, 35, 7), intArrayOf(21, 9, 35, 13), intArrayOf(6, 5), intArrayOf(3, 20),
        71, 3, intArrayOf(70, 15), intArrayOf(4, 9), intArrayOf(68, 14, 3, 2), intArrayOf(3, 8, 2, 3), 11, true, true),
    VMeta("l_lambo", 74, 27, 37, 24, 10, intArrayOf(10, 50), intArrayOf(14, 14), 5.5f, 21,
        intArrayOf(25, 4), intArrayOf(3, 3, 60, 14), intArrayOf(3, 15, 60, 18), intArrayOf(6, 5), intArrayOf(3, 19),
        70, 3, intArrayOf(69, 14), intArrayOf(4, 9), intArrayOf(63, 13, 4, 3), intArrayOf(3, 8, 2, 3), 10, true, true),
    VMeta("l_bugatti", 75, 30, 37, 27, 13, intArrayOf(11, 51), intArrayOf(14, 14), 7.0f, 24,
        intArrayOf(22, 7), intArrayOf(6, 6, 46, 15), intArrayOf(6, 17, 46, 19), intArrayOf(6, 8), intArrayOf(3, 22),
        71, 3, intArrayOf(70, 15), intArrayOf(4, 12), intArrayOf(68, 14, 3, 2), intArrayOf(3, 11, 2, 3), 10, true, true),
    VMeta("l_mclaren", 74, 29, 37, 26, 12, intArrayOf(50, 50), intArrayOf(14, 14), 6.5f, 25,
        intArrayOf(27, 4), intArrayOf(3, 3, 63, 15), intArrayOf(3, 17, 63, 20), intArrayOf(6, 7), intArrayOf(3, 23),
        70, 3, intArrayOf(69, 15), intArrayOf(4, 12), intArrayOf(67, 14, 3, 2), intArrayOf(5, 10, 3, 4), 10, true, true),
    VMeta("l_rolls", 74, 30, 37, 27, 11, intArrayOf(10, 51), intArrayOf(16, 16), 6.0f, 24,
        intArrayOf(28, 4), intArrayOf(20, 3, 41, 10), intArrayOf(20, 12, 41, 16), intArrayOf(6, 10), intArrayOf(3, 22),
        70, 3, intArrayOf(69, 13), intArrayOf(4, 14), intArrayOf(63, 12, 4, 3), intArrayOf(3, 13, 2, 3), 11, true, true),
    VMeta("l_bmw", 76, 31, 38, 28, 12, intArrayOf(12, 54), intArrayOf(16, 16), 6.5f, 25,
        intArrayOf(25, 4), intArrayOf(4, 3, 57, 16), intArrayOf(4, 18, 57, 20), intArrayOf(6, 10), intArrayOf(3, 23),
        72, 3, intArrayOf(71, 16), intArrayOf(4, 14), intArrayOf(69, 15, 3, 2), intArrayOf(3, 13, 2, 3), 12, true, true),
    VMeta("l_mercedes", 76, 29, 38, 26, 11, intArrayOf(12, 53), intArrayOf(15, 15), 6.0f, 23,
        intArrayOf(23, 4), intArrayOf(3, 3, 55, 15), intArrayOf(3, 17, 55, 19), intArrayOf(6, 9), intArrayOf(3, 21),
        72, 3, intArrayOf(71, 14), intArrayOf(4, 13), intArrayOf(65, 13, 4, 3), intArrayOf(3, 12, 2, 3), 12, true, true),
    VMeta("l_audi", 72, 28, 36, 25, 12, intArrayOf(49, 49), intArrayOf(13, 13), 6.5f, 24,
        intArrayOf(32, 5), intArrayOf(22, 4, 48, 8), intArrayOf(22, 10, 48, 14), intArrayOf(6, 8), intArrayOf(3, 22),
        68, 3, intArrayOf(67, 13), intArrayOf(4, 12), intArrayOf(61, 12, 4, 3), intArrayOf(3, 11, 2, 3), 11, true, true),
    VMeta("l_lexus", 73, 27, 36, 24, 11, intArrayOf(9, 49), intArrayOf(13, 13), 6.0f, 21,
        intArrayOf(32, 5), intArrayOf(22, 4, 49, 8), intArrayOf(22, 10, 49, 13), intArrayOf(6, 8), intArrayOf(3, 19),
        69, 3, intArrayOf(68, 13), intArrayOf(4, 12), intArrayOf(62, 12, 4, 3), intArrayOf(3, 11, 2, 3), 11, true, true),
    VMeta("l_maserati", 75, 29, 37, 26, 12, intArrayOf(10, 51), intArrayOf(14, 14), 6.5f, 23,
        intArrayOf(28, 9), intArrayOf(20, 8, 42, 11), intArrayOf(20, 13, 42, 16), intArrayOf(6, 9), intArrayOf(3, 21),
        71, 3, intArrayOf(70, 15), intArrayOf(4, 13), intArrayOf(64, 14, 2, 2), intArrayOf(3, 12, 2, 3), 11, true, true),
    VMeta("l_bentley", 75, 30, 37, 27, 12, intArrayOf(11, 53), intArrayOf(15, 15), 6.5f, 24,
        intArrayOf(33, 5), intArrayOf(23, 4, 50, 9), intArrayOf(23, 11, 50, 15), intArrayOf(6, 10), intArrayOf(3, 22),
        71, 3, intArrayOf(70, 16), intArrayOf(4, 14), intArrayOf(66, 15, 3, 2), intArrayOf(3, 13, 2, 3), 11, true, true),
    VMeta("l_jaguar", 75, 28, 37, 25, 14, intArrayOf(11, 50), intArrayOf(11, 11), 7.5f, 22,
        intArrayOf(33, 5), intArrayOf(23, 4, 50, 8), intArrayOf(23, 10, 50, 14), intArrayOf(6, 7), intArrayOf(3, 20),
        71, 3, intArrayOf(70, 15), intArrayOf(4, 11), intArrayOf(64, 14, 4, 2), intArrayOf(3, 10, 2, 3), 11, true, true),
    VMeta("l_tesla", 74, 30, 37, 27, 12, intArrayOf(10, 50), intArrayOf(15, 15), 6.5f, 24,
        intArrayOf(28, 7), intArrayOf(21, 6, 40, 9), intArrayOf(21, 11, 40, 15), intArrayOf(6, 9), intArrayOf(3, 22),
        70, 3, intArrayOf(69, 15), intArrayOf(4, 13), intArrayOf(67, 14, 3, 2), intArrayOf(3, 12, 2, 3), 11, true, true),
    VMeta("l_rangerover", 72, 33, 36, 30, 14, intArrayOf(10, 49), intArrayOf(16, 16), 7.5f, 27,
        intArrayOf(30, 9), intArrayOf(23, 8, 41, 11), intArrayOf(23, 13, 41, 17), intArrayOf(6, 5), intArrayOf(3, 25),
        68, 3, intArrayOf(67, 17), intArrayOf(4, 14), intArrayOf(65, 16, 3, 2), intArrayOf(5, 12, 2, 4), 12, true, true),
    VMeta("l_cadillac", 74, 29, 37, 26, 14, intArrayOf(10, 50), intArrayOf(12, 12), 7.5f, 23,
        intArrayOf(28, 4), intArrayOf(3, 3, 67, 15), intArrayOf(3, 17, 67, 19), intArrayOf(6, 9), intArrayOf(3, 21),
        70, 3, intArrayOf(69, 15), intArrayOf(4, 12), intArrayOf(63, 14, 2, 3), intArrayOf(5, 11, 2, 2), 11, true, true),
    VMeta("l_corvette", 74, 27, 37, 24, 13, intArrayOf(11, 50), intArrayOf(11, 11), 7.0f, 21,
        intArrayOf(33, 5), intArrayOf(23, 4, 49, 8), intArrayOf(23, 10, 49, 13), intArrayOf(6, 7), intArrayOf(3, 19),
        70, 3, intArrayOf(69, 14), intArrayOf(4, 11), intArrayOf(67, 13, 3, 2), intArrayOf(3, 10, 2, 3), 10, true, true),
    VMeta("l_viper", 72, 26, 36, 23, 14, intArrayOf(10, 48), intArrayOf(9, 9), 7.5f, 20,
        intArrayOf(13, 7), intArrayOf(5, 6, 26, 11), intArrayOf(5, 13, 26, 15), intArrayOf(6, 4), intArrayOf(3, 18),
        68, 3, intArrayOf(67, 15), intArrayOf(4, 8), intArrayOf(66, 14, 2, 2), intArrayOf(3, 7, 2, 3), 9, true, true),
    VMeta("l_mclaren_f1", 77, 26, 38, 23, 10, intArrayOf(11, 52), intArrayOf(13, 13), 5.5f, 21,
        intArrayOf(24, 4), intArrayOf(3, 3, 56, 13), intArrayOf(3, 15, 56, 17), intArrayOf(6, 5), intArrayOf(3, 19),
        73, 3, intArrayOf(72, 13), intArrayOf(4, 9), intArrayOf(66, 12, 4, 3), intArrayOf(3, 8, 2, 3), 11, true, true),
    VMeta("l_koenigsegg", 76, 28, 38, 25, 12, intArrayOf(10, 53), intArrayOf(13, 13), 6.5f, 22,
        intArrayOf(18, 7), intArrayOf(3, 6, 42, 14), intArrayOf(3, 16, 42, 18), intArrayOf(6, 5), intArrayOf(3, 20),
        72, 3, intArrayOf(71, 14), intArrayOf(4, 9), intArrayOf(69, 13, 3, 2), intArrayOf(3, 8, 2, 3), 10, true, true),
    VMeta("l_pagani", 74, 26, 37, 23, 10, intArrayOf(10, 51), intArrayOf(13, 13), 5.5f, 21,
        intArrayOf(33, 5), intArrayOf(23, 4, 49, 8), intArrayOf(23, 10, 49, 13), intArrayOf(6, 6), intArrayOf(3, 19),
        70, 3, intArrayOf(69, 16), intArrayOf(4, 10), intArrayOf(63, 15, 4, 3), intArrayOf(3, 9, 2, 3), 11, true, true),
    VMeta("l_aston", 75, 27, 37, 24, 10, intArrayOf(11, 52), intArrayOf(14, 14), 5.5f, 21,
        intArrayOf(26, 7), intArrayOf(19, 6, 38, 11), intArrayOf(19, 13, 38, 16), intArrayOf(6, 9), intArrayOf(3, 19),
        71, 3, intArrayOf(70, 15), intArrayOf(4, 13), intArrayOf(68, 14, 3, 2), intArrayOf(3, 12, 2, 3), 11, true, true),
    VMeta("l_senna", 75, 29, 37, 26, 14, intArrayOf(50, 50), intArrayOf(12, 12), 7.5f, 25,
        intArrayOf(28, 7), intArrayOf(20, 6, 41, 9), intArrayOf(20, 11, 41, 15), intArrayOf(6, 4), intArrayOf(3, 23),
        71, 3, intArrayOf(70, 16), intArrayOf(4, 8), intArrayOf(68, 15, 3, 2), intArrayOf(3, 7, 2, 3), 11, true, true),
    VMeta("l_revuelto", 74, 27, 37, 24, 12, intArrayOf(8, 48), intArrayOf(12, 12), 6.5f, 21,
        intArrayOf(24, 4), intArrayOf(3, 3, 57, 13), intArrayOf(3, 15, 57, 17), intArrayOf(6, 4), intArrayOf(3, 19),
        70, 3, intArrayOf(69, 14), intArrayOf(4, 8), intArrayOf(67, 13, 3, 2), intArrayOf(3, 7, 2, 3), 10, true, true),
)

/** Tintas do pack de tunagem (anexadas ao fim de PAINTS: os índices salvos continuam valendo). */
val PAINTS_PACK: List<Pair<String, Long>> = listOf(
    "Vermelho vivo" to 0xFFF1010C,
    "Azul elétrico" to 0xFF020AFC,
    "Azul céu" to 0xFF017FFD,
    "Amarelo vivo" to 0xFFFED400,
    "Verde neon" to 0xFF02CC6E,
    "Azul gelo" to 0xFF8EB5F6,
    "Prata azulada" to 0xFF9598AF,
    "Bronze" to 0xFF543F35,
    "Magenta" to 0xFF9A029B,
    "Laranja vivo" to 0xFFEE8101,
    "Verde bandeira" to 0xFF017427,
    "Azul royal" to 0xFF1C39B0,
    "Cobre" to 0xFF771A01,
    "Marrom" to 0xFF442E34,
    "Dourado" to 0xFFF9A600,
    "Chumbo" to 0xFF2D3149,
    "Azul cobalto" to 0xFF266FF1,
    "Preto piano" to 0xFF030813,
    "Laranja queimado" to 0xFFE06F01,
)

/** Cores das molas dos coilovers do pack (suspensão rebaixada). */
val COIL_COLORS: LongArray = longArrayOf(0xFFEF182E, 0xFF1A75F2, 0xFFF3B307, 0xFF6D6C79)

