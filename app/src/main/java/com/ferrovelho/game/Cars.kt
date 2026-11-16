package com.ferrovelho.game

enum class Body { HATCH, SEDAN, SUV, PICKUP, COUPE, CLASSIC, VAN, BEETLE, SUPER }

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

private fun m(brand: String, name: String, base: Int, body: Body, cat: Cat, from: Int, to: Int) =
    CarModel(brand, name, base, body, cat, from, to)

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
)
