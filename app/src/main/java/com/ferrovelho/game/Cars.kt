package com.ferrovelho.game

/**
 * Carroceria (silhueta). Os valores P_* usam o sprite próprio de um veículo dos packs (brasileiros + luxo):
 * pk = índice do sprite (ver PackData.kt). Para os demais, pk = -1 e vale a carroceria genérica (vehicleFor).
 */
enum class Body(val pk: Int = -1) {
    BOXY, COMPACT, WAGON, MPV, HATCH, SEDAN, SUV, PICKUP, COUPE, CLASSIC, VAN, BEETLE, SUPER,
    UNO, GOL, NOTCH, OPALA, MUSCLE, P911, JEEP, MINI,
    // ---- sprites dos packs (gerado: tools/gen/build_packs.sh -> packs_out/body_enum.txt)
    P_FUSCA(27), P_KOMBI(28), P_UNO(29), P_GOL_Q(30), P_GOL_G4(31), P_CORSA(32), P_PALIO(33), P_STRADA(34), P_SIENA(35), P_CIVIC(36), P_COROLLA(37), P_ETIOS(38), P_OPALA(39), P_CHEVETTE(40), P_MAVERICK(41), P_DELREY(42), P_VOYAGE(43), P_SANTANA(44), P_MONZA(45), P_OMEGA(46), P_KADETT(47), P_LOGUS(48), P_TEMPRA(49), P_P206(50), P_HB20(51), P_SANDERO(52), P_DUSTER(53), P_ECOSPORT(54), P_TRACKER(55), P_CRETA(56), P_COMPASS(57), P_RENEGADE(58), P_F1000(59), P_C10(60), P_D20(61), P_RANGER(62), P_S10(63), P_AMAROK(64), P_HILUX(65), P_MB1113(66), P_ONIBUS(67), P_ESCOLAR(68), P_MOTO_CG(69), P_MOTO_TITAN(70), P_MOTO_FAN(71), P_MOTO_BIZ(72), P_MOTO_ENTREGA(73), P_VAN(74), P_L_PORSCHE(75), P_L_FERRARI(76), P_L_LAMBO(77), P_L_BUGATTI(78), P_L_MCLAREN(79), P_L_ROLLS(80), P_L_BMW(81), P_L_MERCEDES(82), P_L_AUDI(83), P_L_LEXUS(84), P_L_MASERATI(85), P_L_BENTLEY(86), P_L_JAGUAR(87), P_L_TESLA(88), P_L_RANGEROVER(89), P_L_CADILLAC(90), P_L_CORVETTE(91), P_L_VIPER(92), P_L_MCLAREN_F1(93), P_L_KOENIGSEGG(94), P_L_PAGANI(95), P_L_ASTON(96), P_L_SENNA(97), P_L_REVUELTO(98);

    /** tipo de veículo do pack ("hatch", "sedan", "pickup", "suv", "super", "bus"...) ou "" se não for do pack */
    val packKind: String get() = if (pk >= P_BASE) PACK_KIND[pk - P_BASE] else ""
}

enum class Cat(val label: String) {
    POPULAR("Popular"),
    SEDAN("Sedã"),
    SUV("SUV"),
    PICAPE("Picape"),
    ESPORTIVO("Esportivo"),
    CLASSICO("Clássico"),
}

/** base = valor aproximado de um exemplar em bom estado (R$). from/to = anos de fabricação. */
data class CarModel(
    val brand: String,
    val name: String,
    val base: Int,
    val body: Body,
    val cat: Cat,
    val from: Int,
    val to: Int,
) {
    val fullName: String get() = "$brand $name"
}

private val H = Body.HATCH
private val S = Body.SEDAN
private val U = Body.SUV
private val P = Body.PICKUP
private val C = Body.COUPE
private val K = Body.CLASSIC

private val PO = Cat.POPULAR
private val SE = Cat.SEDAN
private val SU = Cat.SUV
private val PI = Cat.PICAPE
private val ES = Cat.ESPORTIVO
private val CL = Cat.CLASSICO

/** Modelos que têm sprite PRÓPRIO nos packs (brasileiros + luxo). Os demais usam a silhueta genérica de SHAPE. */
private val PACK_SHAPE: Map<String, Body> = mapOf(
    "Uno" to Body.P_UNO,
    "Gol" to Body.P_GOL_G4,
    "Corsa" to Body.P_CORSA,
    "Palio" to Body.P_PALIO,
    "Strada" to Body.P_STRADA,
    "Siena" to Body.P_SIENA,
    "Civic" to Body.P_CIVIC,
    "Corolla" to Body.P_COROLLA,
    "Etios" to Body.P_ETIOS,
    "Opala" to Body.P_OPALA,
    "Chevette" to Body.P_CHEVETTE,
    "Maverick GT" to Body.P_MAVERICK,
    "Del Rey" to Body.P_DELREY,
    "Voyage" to Body.P_VOYAGE,
    "Santana" to Body.P_SANTANA,
    "Monza" to Body.P_MONZA,
    "Omega" to Body.P_OMEGA,
    "Kadett GSi" to Body.P_KADETT,
    "Tempra" to Body.P_TEMPRA,
    "206" to Body.P_P206,
    "HB20" to Body.P_HB20,
    "Sandero" to Body.P_SANDERO,
    "Duster" to Body.P_DUSTER,
    "EcoSport" to Body.P_ECOSPORT,
    "Tracker" to Body.P_TRACKER,
    "Creta" to Body.P_CRETA,
    "Compass" to Body.P_COMPASS,
    "Renegade" to Body.P_RENEGADE,
    "F-1000" to Body.P_F1000,
    "Ranger" to Body.P_RANGER,
    "S10" to Body.P_S10,
    "Amarok" to Body.P_AMAROK,
    "Hilux" to Body.P_HILUX,
    "Fusca" to Body.P_FUSCA,
    "Kombi" to Body.P_KOMBI,
    "911 Carrera" to Body.P_L_PORSCHE,
    "488 GTB" to Body.P_L_FERRARI,
    "Huracán" to Body.P_L_LAMBO,
    "M3" to Body.P_L_BMW,
    "AMG GT" to Body.P_L_MERCEDES,
    "R8" to Body.P_L_AUDI,
    "Corvette" to Body.P_L_CORVETTE,
)

/** Silhueta própria por modelo (o que não estiver aqui usa a carroceria genérica da linha). */
private val SHAPE: Map<String, Body> = buildMap {

    listOf("Uno", "147").forEach { put(it, Body.UNO) }
    listOf("Gol", "Gol GTI").forEach { put(it, Body.GOL) }
    listOf("Jetta", "Santana", "Passat", "Voyage").forEach { put(it, Body.NOTCH) }
    listOf("Opala", "Corcel").forEach { put(it, Body.OPALA) }
    listOf("Camaro SS", "Mustang GT", "Maverick GT", "Challenger", "Charger").forEach { put(it, Body.MUSCLE) }
    listOf("911 Carrera", "718 Cayman").forEach { put(it, Body.P911) }
    listOf("Wrangler", "Jeep CJ", "T4", "Defender", "Marruá", "Xavante", "Bandeirante").forEach { put(it, Body.JEEP) }
    listOf("Cooper S", "John Cooper Works", "Abarth 500").forEach { put(it, Body.MINI) }
    listOf("Brasília", "Kadett GSi", "Escort XR3", "Soul", "Kwid", "BR-800", "Fiorino")
        .forEach { put(it, Body.BOXY) }
    listOf("Palio", "Mobi", "Argo", "Punto", "Celta", "Corsa", "Onix", "Agile", "Ka", "Fiesta", "Up!", "Fox",
        "Polo", "HB20", "March", "206", "207", "208", "Etios", "Sandero", "Yaris", "Twingo", "Clio", "Dolphin")
        .forEach { put(it, Body.COMPACT) }
    listOf("Parati", "Belina").forEach { put(it, Body.WAGON) }
    listOf("Idea", "Doblò", "Meriva", "Spin", "Fit").forEach { put(it, Body.MPV) }
}

private fun m(brand: String, name: String, base: Int, body: Body, cat: Cat, from: Int, to: Int) =
    CarModel(brand, name, base, PACK_SHAPE[name] ?: (if (body.pk >= 0) body else SHAPE[name] ?: body), cat, from, to)

// Valores e anos aproximados, só pra jogabilidade. Pra adicionar um carro, é só incluir uma linha aqui.
val CATALOG: List<CarModel> = listOf(
    // ---------- Fiat ----------
    m("Fiat", "Uno", 28000, H, PO, 1984, 2021),
    m("Fiat", "Palio", 24000, H, PO, 1996, 2017),
    m("Fiat", "Siena", 22000, S, SE, 1996, 2016),
    m("Fiat", "Mobi", 45000, H, PO, 2016, 2026),
    m("Fiat", "Argo", 62000, H, PO, 2017, 2026),
    m("Fiat", "Cronos", 70000, S, SE, 2018, 2026),
    m("Fiat", "Strada", 75000, P, PI, 1998, 2026),
    m("Fiat", "Toro", 120000, P, PI, 2016, 2026),
    m("Fiat", "Punto", 38000, H, PO, 2007, 2017),
    m("Fiat", "Bravo", 40000, H, PO, 2010, 2016),
    m("Fiat", "Stilo", 25000, H, PO, 2002, 2011),
    m("Fiat", "Linea", 40000, S, SE, 2008, 2016),
    m("Fiat", "Idea", 25000, H, PO, 2005, 2016),
    m("Fiat", "Doblò", 30000, H, PO, 2001, 2022),
    m("Fiat", "Fiorino", 40000, H, PO, 1988, 2022),
    m("Fiat", "147", 15000, H, CL, 1976, 1986),
    m("Fiat", "Tempra", 15000, S, SE, 1991, 1999),
    m("Fiat", "Marea", 18000, S, SE, 1999, 2007),
    m("Fiat", "Pulse", 95000, U, SU, 2021, 2026),
    m("Fiat", "Fastback", 120000, U, SU, 2022, 2026),
    m("Fiat", "Abarth 500", 100000, C, ES, 2012, 2018),

    // ---------- Volkswagen ----------
    m("Volkswagen", "Gol", 30000, H, PO, 1980, 2023),
    m("Volkswagen", "Fusca", 35000, Body.BEETLE, CL, 1959, 1996),
    m("Volkswagen", "Kombi", 60000, Body.VAN, CL, 1957, 2013),
    m("Volkswagen", "Brasília", 25000, H, CL, 1973, 1982),
    m("Volkswagen", "Parati", 22000, H, PO, 1982, 2012),
    m("Volkswagen", "Saveiro", 40000, P, PI, 1982, 2023),
    m("Volkswagen", "Voyage", 35000, S, SE, 1981, 2023),
    m("Volkswagen", "Fox", 32000, H, PO, 2003, 2021),
    m("Volkswagen", "Up!", 35000, H, PO, 2014, 2021),
    m("Volkswagen", "Polo", 70000, H, PO, 2002, 2026),
    m("Volkswagen", "Virtus", 85000, S, SE, 2018, 2026),
    m("Volkswagen", "T-Cross", 110000, U, SU, 2019, 2026),
    m("Volkswagen", "Nivus", 105000, U, SU, 2020, 2026),
    m("Volkswagen", "Taos", 150000, U, SU, 2021, 2026),
    m("Volkswagen", "Amarok", 200000, P, PI, 2010, 2026),
    m("Volkswagen", "Jetta", 70000, S, SE, 2006, 2018),
    m("Volkswagen", "Santana", 15000, S, SE, 1984, 2006),
    m("Volkswagen", "Passat", 12000, S, CL, 1974, 1988),
    m("Volkswagen", "SP2", 150000, C, CL, 1972, 1976),
    m("Volkswagen", "Gol GTI", 40000, H, ES, 1989, 1996),
    m("Volkswagen", "Golf GTI", 130000, H, ES, 2013, 2022),

    // ---------- Chevrolet ----------
    m("Chevrolet", "Celta", 20000, H, PO, 2000, 2015),
    m("Chevrolet", "Corsa", 20000, H, PO, 1994, 2012),
    m("Chevrolet", "Chevette", 18000, H, CL, 1973, 1993),
    m("Chevrolet", "Opala", 45000, S, CL, 1968, 1992),
    m("Chevrolet", "Monza", 15000, S, CL, 1982, 1996),
    m("Chevrolet", "Kadett GSi", 45000, H, ES, 1989, 1998),
    m("Chevrolet", "Onix", 60000, H, PO, 2012, 2026),
    m("Chevrolet", "Prisma", 42000, S, SE, 2006, 2019),
    m("Chevrolet", "Classic", 22000, S, SE, 2003, 2016),
    m("Chevrolet", "Astra", 22000, H, PO, 1998, 2011),
    m("Chevrolet", "Vectra", 20000, S, SE, 1996, 2011),
    m("Chevrolet", "Omega", 25000, S, SE, 1992, 1998),
    m("Chevrolet", "Cruze", 85000, S, SE, 2011, 2023),
    m("Chevrolet", "Cobalt", 45000, S, SE, 2011, 2019),
    m("Chevrolet", "Agile", 28000, H, PO, 2009, 2014),
    m("Chevrolet", "Meriva", 25000, H, PO, 2002, 2012),
    m("Chevrolet", "Spin", 55000, H, PO, 2012, 2026),
    m("Chevrolet", "Blazer", 40000, U, SU, 1995, 2011),
    m("Chevrolet", "Tracker", 100000, U, SU, 2013, 2026),
    m("Chevrolet", "Trailblazer", 180000, U, SU, 2012, 2026),
    m("Chevrolet", "Equinox", 160000, U, SU, 2018, 2026),
    m("Chevrolet", "S10", 150000, P, PI, 1995, 2026),
    m("Chevrolet", "Montana", 70000, P, PI, 2003, 2026),
    m("Chevrolet", "Camaro SS", 350000, C, ES, 2011, 2023),
    m("Chevrolet", "Corvette", 600000, C, ES, 2010, 2022),

    // ---------- Ford ----------
    m("Ford", "Ka", 30000, H, PO, 1997, 2021),
    m("Ford", "Fiesta", 30000, H, PO, 1996, 2019),
    m("Ford", "Focus", 40000, H, PO, 2000, 2019),
    m("Ford", "Escort XR3", 40000, H, ES, 1983, 1996),
    m("Ford", "Corcel", 25000, S, CL, 1968, 1986),
    m("Ford", "Del Rey", 12000, S, CL, 1981, 1991),
    m("Ford", "Belina", 12000, H, CL, 1970, 1991),
    m("Ford", "Maverick GT", 130000, C, ES, 1973, 1979),
    m("Ford", "Mustang GT", 380000, C, ES, 2015, 2026),
    m("Ford", "EcoSport", 55000, U, SU, 2003, 2022),
    m("Ford", "Fusion", 65000, S, SE, 2006, 2020),
    m("Ford", "Territory", 150000, U, SU, 2020, 2026),
    m("Ford", "Ranger", 140000, P, PI, 1995, 2026),
    m("Ford", "F-1000", 50000, P, PI, 1983, 1998),

    // ---------- Renault ----------
    m("Renault", "Clio", 20000, H, PO, 1999, 2016),
    m("Renault", "Twingo", 22000, H, PO, 1995, 2012),
    m("Renault", "Sandero", 40000, H, PO, 2008, 2026),
    m("Renault", "Sandero RS", 70000, H, ES, 2010, 2025),
    m("Renault", "Logan", 38000, S, SE, 2008, 2026),
    m("Renault", "Kwid", 40000, H, PO, 2018, 2026),
    m("Renault", "Mégane", 25000, S, SE, 1998, 2012),
    m("Renault", "Fluence", 50000, S, SE, 2011, 2019),
    m("Renault", "Duster", 65000, U, SU, 2011, 2026),
    m("Renault", "Captur", 75000, U, SU, 2017, 2026),
    m("Renault", "Oroch", 80000, P, PI, 2015, 2026),

    // ---------- Toyota ----------
    m("Toyota", "Corolla", 100000, S, SE, 1998, 2026),
    m("Toyota", "Etios", 45000, H, PO, 2012, 2021),
    m("Toyota", "Yaris", 70000, H, PO, 2018, 2026),
    m("Toyota", "Corolla Cross", 150000, U, SU, 2021, 2026),
    m("Toyota", "SW4", 250000, U, SU, 2005, 2026),
    m("Toyota", "Hilux", 200000, P, PI, 1992, 2026),
    m("Toyota", "Bandeirante", 60000, U, CL, 1962, 2001),
    m("Toyota", "GR Supra", 400000, C, ES, 2020, 2026),

    // ---------- Honda ----------
    m("Honda", "Civic", 90000, S, SE, 1992, 2026),
    m("Honda", "Civic Si", 120000, S, ES, 2007, 2016),
    m("Honda", "Civic Type R", 350000, H, ES, 2018, 2022),
    m("Honda", "Fit", 55000, H, PO, 2003, 2021),
    m("Honda", "City", 70000, S, SE, 2009, 2026),
    m("Honda", "City Hatch", 85000, H, PO, 2021, 2026),
    m("Honda", "Accord", 70000, S, SE, 2008, 2017),
    m("Honda", "HR-V", 100000, U, SU, 2015, 2026),
    m("Honda", "WR-V", 90000, U, SU, 2017, 2026),
    m("Honda", "CR-V", 130000, U, SU, 1997, 2026),

    // ---------- Hyundai / Kia ----------
    m("Hyundai", "HB20", 50000, H, PO, 2012, 2026),
    m("Hyundai", "HB20S", 50000, S, SE, 2013, 2026),
    m("Hyundai", "i30", 50000, H, PO, 2009, 2018),
    m("Hyundai", "Azera", 60000, S, SE, 2008, 2016),
    m("Hyundai", "Veloster", 90000, C, ES, 2011, 2019),
    m("Hyundai", "ix35", 70000, U, SU, 2010, 2017),
    m("Hyundai", "Creta", 85000, U, SU, 2017, 2026),
    m("Hyundai", "Tucson", 100000, U, SU, 2005, 2026),
    m("Kia", "Soul", 60000, H, PO, 2010, 2018),
    m("Kia", "Cerato", 50000, S, SE, 2009, 2019),
    m("Kia", "Stinger", 300000, S, ES, 2018, 2023),
    m("Kia", "Sportage", 90000, U, SU, 2005, 2026),

    // ---------- Nissan / Mitsubishi / Subaru / Mazda ----------
    m("Nissan", "March", 35000, H, PO, 2011, 2020),
    m("Nissan", "Versa", 55000, S, SE, 2011, 2026),
    m("Nissan", "Sentra", 50000, S, SE, 2007, 2026),
    m("Nissan", "Kicks", 90000, U, SU, 2016, 2026),
    m("Nissan", "Frontier", 180000, P, PI, 2002, 2026),
    m("Nissan", "370Z", 250000, C, ES, 2010, 2020),
    m("Nissan", "GT-R", 800000, C, ES, 2009, 2023),
    m("Mitsubishi", "L200 Triton", 180000, P, PI, 2007, 2026),
    m("Mitsubishi", "Pajero TR4", 70000, U, SU, 2003, 2015),
    m("Mitsubishi", "Pajero Full", 120000, U, SU, 1992, 2020),
    m("Mitsubishi", "ASX", 80000, U, SU, 2011, 2019),
    m("Mitsubishi", "Eclipse Cross", 170000, U, SU, 2018, 2023),
    m("Mitsubishi", "Lancer Evolution X", 300000, S, ES, 2008, 2015),
    m("Subaru", "Impreza WRX STI", 200000, S, ES, 2008, 2021),
    m("Subaru", "Forester", 120000, U, SU, 2008, 2026),
    m("Mazda", "MX-5", 200000, C, ES, 2006, 2026),
    m("Mazda", "RX-8", 100000, C, ES, 2004, 2012),
    m("Mazda", "CX-5", 130000, U, SU, 2013, 2026),

    // ---------- Jeep / Troller / Agrale / Willys ----------
    m("Jeep", "Renegade", 90000, U, SU, 2015, 2026),
    m("Jeep", "Compass", 140000, U, SU, 2016, 2026),
    m("Jeep", "Commander", 200000, U, SU, 2021, 2026),
    m("Jeep", "Wrangler", 350000, U, SU, 2012, 2026),
    m("Jeep", "Grand Cherokee", 250000, U, SU, 2012, 2026),
    m("Willys", "Jeep CJ", 45000, U, CL, 1954, 1983),
    m("Troller", "T4", 120000, U, SU, 2001, 2021),
    m("Agrale", "Marruá", 250000, U, SU, 2004, 2026),

    // ---------- Peugeot / Citroën ----------
    m("Peugeot", "206", 20000, H, PO, 1999, 2010),
    m("Peugeot", "207", 28000, H, PO, 2008, 2014),
    m("Peugeot", "208", 70000, H, PO, 2013, 2026),
    m("Peugeot", "307", 30000, H, PO, 2002, 2012),
    m("Peugeot", "308", 50000, H, PO, 2012, 2016),
    m("Peugeot", "2008", 80000, U, SU, 2015, 2026),
    m("Peugeot", "3008", 150000, U, SU, 2011, 2026),
    m("Citroën", "C3", 45000, H, PO, 2003, 2026),
    m("Citroën", "Xsara Picasso", 20000, H, PO, 2001, 2012),
    m("Citroën", "C4 Lounge", 55000, S, SE, 2014, 2019),
    m("Citroën", "Aircross", 55000, U, SU, 2010, 2026),
    m("Citroën", "C4 Cactus", 70000, U, SU, 2018, 2022),
    m("Citroën", "Basalt", 90000, U, SU, 2024, 2026),

    // ---------- Nacionais raros / chineses ----------
    m("Gurgel", "BR-800", 25000, H, CL, 1988, 1991),
    m("Gurgel", "Xavante", 40000, U, CL, 1973, 1983),
    m("Puma", "GTE", 60000, C, CL, 1970, 1985),
    m("Chery", "Tiggo", 80000, U, SU, 2012, 2026),
    m("JAC", "J3", 25000, S, SE, 2011, 2016),
    m("BYD", "Dolphin", 150000, H, PO, 2023, 2026),
    m("BYD", "Seal", 250000, S, SE, 2024, 2026),
    m("GWM", "Haval H6", 200000, U, SU, 2022, 2026),

    // ---------- Importados / esportivos ----------
    m("Mini", "Cooper S", 150000, H, ES, 2007, 2026),
    m("Mini", "John Cooper Works", 250000, H, ES, 2013, 2026),
    m("Audi", "A3", 100000, S, SE, 2000, 2026),
    m("Audi", "Q3", 130000, U, SU, 2013, 2026),
    m("Audi", "TT", 150000, C, ES, 2007, 2020),
    m("Audi", "RS3", 400000, S, ES, 2017, 2026),
    m("Audi", "R8", 1000000, C, ES, 2008, 2022),
    m("BMW", "320i", 120000, S, SE, 2005, 2026),
    m("BMW", "X1", 150000, U, SU, 2010, 2026),
    m("BMW", "Z4", 250000, C, ES, 2009, 2026),
    m("BMW", "M2", 600000, C, ES, 2016, 2026),
    m("BMW", "M3", 450000, S, ES, 2008, 2026),
    m("BMW", "M4", 600000, C, ES, 2015, 2026),
    m("Mercedes-Benz", "Classe C", 120000, S, SE, 2000, 2026),
    m("Mercedes-Benz", "CLA 200", 160000, S, SE, 2014, 2026),
    m("Mercedes-Benz", "A 45 AMG", 380000, H, ES, 2014, 2026),
    m("Mercedes-Benz", "C 63 AMG", 500000, S, ES, 2010, 2022),
    m("Mercedes-Benz", "AMG GT", 900000, C, ES, 2015, 2026),
    m("Porsche", "718 Cayman", 450000, C, ES, 2006, 2026),
    m("Porsche", "911 Carrera", 800000, C, ES, 1995, 2026),
    m("Porsche", "Macan", 450000, U, SU, 2015, 2026),
    m("Porsche", "Cayenne", 500000, U, SU, 2004, 2026),
    m("Porsche", "Panamera", 700000, S, ES, 2010, 2026),
    m("Dodge", "Challenger", 350000, C, ES, 2009, 2026),
    m("Dodge", "Charger", 300000, S, ES, 2011, 2026),
    m("Ram", "2500", 400000, P, PI, 2012, 2026),
    m("Land Rover", "Defender", 400000, U, SU, 1990, 2026),
    m("Land Rover", "Range Rover Evoque", 250000, U, SU, 2012, 2026),
    m("Volvo", "XC60", 200000, U, SU, 2009, 2026),
    m("Volvo", "XC90", 300000, U, SU, 2003, 2026),
    m("Ferrari", "488 GTB", 2000000, Body.SUPER, ES, 2015, 2019),
    m("Lamborghini", "Huracán", 2500000, Body.SUPER, ES, 2014, 2026),

    // ---------- Pack de veículos brasileiros (modelos novos, sempre no FIM da lista pra não bagunçar jogos salvos) ----------
    m("Volkswagen", "Gol Quadrado", 22000, Body.P_GOL_Q, CL, 1980, 1994),
    m("Volkswagen", "Logus", 9000, Body.P_LOGUS, PO, 1993, 1996),
    m("Chevrolet", "C10", 70000, Body.P_C10, CL, 1964, 1985),
    m("Chevrolet", "D-20", 45000, Body.P_D20, PI, 1985, 1996),
    m("Renault", "Master", 180000, Body.P_VAN, PO, 2004, 2026),

    // ---------- Pack de carros de luxo ----------
    m("Bugatti", "Chiron", 8000000, Body.P_L_BUGATTI, ES, 2017, 2024),
    m("McLaren", "720S", 1800000, Body.P_L_MCLAREN, ES, 2018, 2025),
    m("Rolls-Royce", "Ghost", 2200000, Body.P_L_ROLLS, SE, 2010, 2025),
    m("Lexus", "RC F", 450000, Body.P_L_LEXUS, ES, 2015, 2025),
    m("Maserati", "Ghibli", 600000, Body.P_L_MASERATI, SE, 2014, 2025),
    m("Bentley", "Flying Spur", 1800000, Body.P_L_BENTLEY, SE, 2006, 2025),
    m("Jaguar", "XF", 350000, Body.P_L_JAGUAR, SE, 2009, 2025),
    m("Tesla", "Model S", 500000, Body.P_L_TESLA, SE, 2016, 2024),
    m("Land Rover", "Range Rover Vogue", 900000, Body.P_L_RANGEROVER, SU, 2012, 2026),
    m("Cadillac", "CTS-V", 450000, Body.P_L_CADILLAC, ES, 2009, 2019),
    m("Dodge", "Viper", 700000, Body.P_L_VIPER, ES, 1992, 2017),
    m("McLaren", "F1", 15000000, Body.P_L_MCLAREN_F1, CL, 1993, 1998),
    m("Koenigsegg", "Agera RS", 12000000, Body.P_L_KOENIGSEGG, ES, 2015, 2018),
    m("Pagani", "Huayra", 14000000, Body.P_L_PAGANI, ES, 2012, 2020),
    m("Aston Martin", "Vantage", 1200000, Body.P_L_ASTON, ES, 2018, 2025),
    m("McLaren", "Senna", 6000000, Body.P_L_SENNA, ES, 2018, 2019),
    m("Lamborghini", "Revuelto", 4500000, Body.P_L_REVUELTO, ES, 2024, 2026),
)

/**
 * Quais peças de tuning cabem em cada carro. Retorna o motivo de NÃO caber (null = cabe).
 * Pra mudar uma regra, é só mexer nas listas abaixo.
 */
fun tuneBlock(m: CarModel, t: Tuning): String? {
    val b = m.body
    val ev = m.name == "Dolphin" || m.name == "Seal" || m.name == "Model S"
    // sprites dos packs: TODA peça tem versão visual pra eles (rodas, aerofólio, neon, suspensão...). Só o carro elétrico
    // não aceita turbo/escape.
    if (b.packKind.isNotEmpty()) return when (t.id) {
        "turbo" -> if (ev) "Carro elétrico, sem motor a combustão" else null
        "escape" -> if (ev) "Carro elétrico, sem escapamento" else null
        else -> null
    }
    return when (t.id) {
        "aerofolio" -> if (b in listOf(Body.PICKUP, Body.VAN, Body.JEEP, Body.SUV, Body.MPV, Body.BEETLE, Body.SUPER, Body.P911))
            "Não cabe nesse carro" else null
        "parachoque" -> if (b in listOf(Body.VAN, Body.JEEP, Body.BEETLE, Body.CLASSIC, Body.OPALA, Body.SUPER))
            "Não existe kit pra ele" else null
        "turbo" -> if (ev) "Carro elétrico, sem motor a combustão"
            else if (b in listOf(Body.VAN, Body.SUPER)) "Não serve nesse motor" else null
        "escape" -> if (ev) "Carro elétrico, sem escapamento" else null
        "rebaixada" -> if (b in listOf(Body.PICKUP, Body.JEEP, Body.SUV, Body.SUPER, Body.P911))
            "Não rebaixa esse carro" else null
        "pelicula" -> if (b == Body.JEEP) "Sem vidros fechados" else null
        else -> null
    }
}
