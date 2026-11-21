package com.alessiomartini.dispensa.data

import java.text.Normalizer
import java.time.LocalDate

/**
 * Per-catalog-item extras kept apart from [FoodCatalog]: names in other languages and the
 * Dutch growing season. Both are keyed by the catalog's English name (lowercase).
 */
object FoodInfo {

    /** English name -> (Italian, Dutch). */
    private val translations: Map<String, Pair<String, String>> = mapOf(
        "apple" to ("Mela" to "Appel"), "banana" to ("Banana" to "Banaan"),
        "orange" to ("Arancia" to "Sinaasappel"), "lemon" to ("Limone" to "Citroen"),
        "grapes" to ("Uva" to "Druiven"), "strawberry" to ("Fragola" to "Aardbei"),
        "watermelon" to ("Anguria" to "Watermeloen"), "pineapple" to ("Ananas" to "Ananas"),
        "avocado" to ("Avocado" to "Avocado"), "tomato" to ("Pomodoro" to "Tomaat"),
        "potato" to ("Patata" to "Aardappel"), "carrot" to ("Carota" to "Wortel"),
        "onion" to ("Cipolla" to "Ui"), "garlic" to ("Aglio" to "Knoflook"),
        "broccoli" to ("Broccoli" to "Broccoli"), "lettuce" to ("Lattuga" to "Sla"),
        "cucumber" to ("Cetriolo" to "Komkommer"), "bell pepper" to ("Peperone" to "Paprika"),
        "mushroom" to ("Funghi" to "Champignons"), "corn" to ("Mais" to "Mais"),
        "eggplant" to ("Melanzana" to "Aubergine"), "zucchini" to ("Zucchina" to "Courgette"),
        "celery" to ("Sedano" to "Bleekselderij"), "leeks" to ("Porri" to "Prei"),
        "cauliflower" to ("Cavolfiore" to "Bloemkool"),
        "endive" to ("Indivia" to "Andijvie"), "fennel" to ("Finocchio" to "Venkel"),
        "berries" to ("Frutti di bosco" to "Bessen"), "ginger" to ("Zenzero" to "Gember"),
        "vegetable pan" to ("Misto verdure" to "Groentepannetje"), "soy drink" to ("Bevanda di soia" to "Sojadrink"),
        "wraps" to ("Piadine" to "Wraps"), "bakery snack" to ("Prodotto da forno" to "Gebak"),
        "mayonnaise" to ("Maionese" to "Mayonaise"), "baking mix" to ("Preparato per dolci" to "Bakmix"),
        "multivitamin" to ("Multivitaminico" to "Multivitamine"), "chips" to ("Patatine" to "Chips"),
        "chili pepper" to ("Peperoncino" to "Chilipeper"), "sun-dried tomatoes" to ("Pomodori secchi" to "Zongedroogde tomaten"),
        "cream cheese" to ("Formaggio spalmabile" to "Roomkaas"), "pecorino" to ("Pecorino" to "Pecorino"),
        "cornflakes" to ("Cornflakes" to "Cornflakes"), "rice cakes" to ("Gallette di riso" to "Rijstwafels"),
        "toast bread" to ("Pane in cassetta" to "Tostibrood"),
        "ketchup" to ("Ketchup" to "Ketchup"), "soy sauce" to ("Salsa di soia" to "Sojasaus"),
        "tomato paste" to ("Concentrato di pomodoro" to "Tomatenpuree"), "capers" to ("Capperi" to "Kappertjes"),
        "flaxseed" to ("Semi di lino" to "Lijnzaad"), "hot sauce" to ("Salsa piccante" to "Hete saus"),
        "yeast" to ("Lievito" to "Gist"), "popcorn" to ("Popcorn" to "Popcorn"), "nesquik" to ("Nesquik" to "Nesquik"),
        "peas" to ("Piselli" to "Erwten"), "pancake mix" to ("Preparato per pancake" to "Pannenkoekenmix"),
        "pine nuts" to ("Pinoli" to "Pijnboompitten"),
        "basil" to ("Basilico" to "Basilicum"), "thyme" to ("Timo" to "Tijm"), "coriander" to ("Coriandolo" to "Koriander"),
        "turmeric" to ("Curcuma" to "Kurkuma"), "star anise" to ("Anice stellato" to "Steranijs"),
        "cayenne" to ("Pepe di cayenna" to "Cayennepeper"), "garlic powder" to ("Aglio in polvere" to "Knoflookpoeder"),
        "spice mix" to ("Mix di spezie" to "Kruidenmix"), "peppercorn mix" to ("Pepe in grani" to "Peperkorrels"),
        "instant coffee" to ("Caffè solubile" to "Oploskoffie"), "freezer bags" to ("Sacchetti per congelatore" to "Vrieszakken"),
        "rosemary" to ("Rosmarino" to "Rozemarijn"),
        "milk" to ("Latte" to "Melk"), "eggs" to ("Uova" to "Eieren"), "cheese" to ("Formaggio" to "Kaas"),
        "butter" to ("Burro" to "Boter"), "yogurt" to ("Yogurt" to "Yoghurt"), "cream" to ("Panna" to "Room"),
        "feta" to ("Feta" to "Feta"), "mozzarella" to ("Mozzarella" to "Mozzarella"), "ricotta" to ("Ricotta" to "Ricotta"),
        "chicken" to ("Pollo" to "Kip"), "beef" to ("Manzo" to "Rundvlees"), "pork" to ("Maiale" to "Varkensvlees"),
        "bacon" to ("Pancetta" to "Spek"), "fish" to ("Pesce" to "Vis"), "salmon" to ("Salmone" to "Zalm"),
        "shrimp" to ("Gamberi" to "Garnalen"), "sausage" to ("Salsiccia" to "Worst"), "ham" to ("Prosciutto" to "Ham"),
        "tofu" to ("Tofu" to "Tofu"), "schnitzel" to ("Cotoletta" to "Schnitzel"), "tuna" to ("Tonno" to "Tonijn"),
        "minced meat" to ("Carne macinata" to "Gehakt"),
        "bread" to ("Pane" to "Brood"), "pasta" to ("Pasta" to "Pasta"), "rice" to ("Riso" to "Rijst"),
        "cereal" to ("Cereali" to "Ontbijtgranen"), "oats" to ("Avena" to "Havermout"), "flour" to ("Farina" to "Bloem"),
        "bagel" to ("Bagel" to "Bagel"), "croissant" to ("Cornetto" to "Croissant"),
        "stroopwafel" to ("Stroopwafel" to "Stroopwafel"), "biscuits" to ("Biscotti" to "Koekjes"),
        "crackers" to ("Cracker" to "Crackers"), "couscous" to ("Couscous" to "Couscous"),
        "baguette" to ("Baguette" to "Stokbrood"), "muesli" to ("Muesli" to "Muesli"), "muffin" to ("Muffin" to "Muffin"),
        "puff pastry" to ("Pasta sfoglia" to "Bladerdeeg"),
        "sugar" to ("Zucchero" to "Suiker"), "olive oil" to ("Olio d'oliva" to "Olijfolie"),
        "vinegar" to ("Aceto" to "Azijn"), "honey" to ("Miele" to "Honing"),
        "canned tomatoes" to ("Pelati" to "Tomaten in blik"), "beans" to ("Fagioli" to "Bonen"),
        "jam" to ("Marmellata" to "Jam"), "peanut butter" to ("Burro di arachidi" to "Pindakaas"),
        "chocolate" to ("Cioccolato" to "Chocolade"), "pesto" to ("Pesto" to "Pesto"), "hummus" to ("Hummus" to "Hummus"),
        "omega 3" to ("Omega 3" to "Omega 3"), "broth dice" to ("Dado" to "Bouillonblokjes"),
        "chickpeas" to ("Ceci" to "Kikkererwten"), "lentils" to ("Lenticchie" to "Linzen"),
        "mixed nuts" to ("Frutta secca" to "Gemengde noten"), "nutella" to ("Nutella" to "Nutella"),
        "corn kernels" to ("Mais in scatola" to "Maiskorrels"), "olives" to ("Olive" to "Olijven"),
        "tahini" to ("Tahina" to "Tahini"), "coconut milk" to ("Latte di cocco" to "Kokosmelk"),
        "mashed potatoes" to ("Purè di patate" to "Aardappelpuree"),
        "parsley" to ("Prezzemolo" to "Peterselie"), "mint" to ("Menta" to "Munt"), "salt" to ("Sale" to "Zout"),
        "pepper" to ("Pepe" to "Peper"), "curry" to ("Curry" to "Curry"), "paprika powder" to ("Paprika in polvere" to "Paprikapoeder"),
        "oregano" to ("Origano" to "Oregano"), "cinnamon" to ("Cannella" to "Kaneel"), "cumin" to ("Cumino" to "Komijn"),
        "chili flakes" to ("Peperoncino" to "Chilivlokken"), "nutmeg" to ("Noce moscata" to "Nootmuskaat"),
        "ice cream" to ("Gelato" to "IJs"), "frozen pizza" to ("Pizza surgelata" to "Diepvriespizza"),
        "frozen vegetables" to ("Verdure surgelate" to "Diepvriesgroenten"), "fries" to ("Patatine fritte" to "Friet"),
        "burrito" to ("Burrito" to "Burrito"),
        "water" to ("Acqua" to "Water"), "juice" to ("Succo" to "Fruitsap"), "coffee" to ("Caffè" to "Koffie"),
        "tea" to ("Tè" to "Thee"), "wine" to ("Vino" to "Wijn"), "beer" to ("Birra" to "Bier"),
        "soda" to ("Bibita gassata" to "Frisdrank"),
        "toilet paper" to ("Carta igienica" to "Toiletpapier"), "paper towels" to ("Carta da cucina" to "Keukenrol"),
        "dish soap" to ("Detersivo piatti" to "Afwasmiddel"),
        "dishwasher tablets" to ("Pastiglie lavastoviglie" to "Vaatwastabletten"),
        "toilet freshener" to ("Profuma WC" to "WC-blok"), "laundry detergent" to ("Detersivo lavatrice" to "Wasmiddel"),
        "toothpaste" to ("Dentifricio" to "Tandpasta"), "shampoo" to ("Shampoo" to "Shampoo"),
        "trash bags" to ("Sacchi spazzatura" to "Vuilniszakken"), "sponge" to ("Spugna" to "Spons"),
        "deodorant" to ("Deodorante" to "Deodorant")
    )

    /**
     * Months (1-12) in which the product is in season in the Netherlands. Source: Milieu Centraal
     * "groente- en fruitkalender" (PDF linked from
     * https://www.milieucentraal.nl/eten-en-drinken/milieubewust-eten/groente-en-fruit/, made by
     * Voedingscentrum from Velt/Bionext data, 2023), consulted 2026-11-21. Rule: a month counts when the
     * product is listed in that month's column of the calendar (Dutch-grown, including storage and
     * the early/late months the calendar lists); imports are not counted.
     * Fruit/veg not in the calendar (banana, citrus, avocado, pineapple, ginger, garlic, chili) are
     * imported all year, so they get no season label.
     * Not verifiable: "potato" (not in the calendar, kept as all year), "berries" (mixed group, no label).
     * Calendar names: aardbei, appel, wortel, ui, broccoli, kropsla, tomaat, komkommer, paprika,
     * paddenstoelen, mais, aubergine, courgette, bleekselderij, prei, bloemkool, andijvie, venkel.
     */
    private val dutchSeason: Map<String, Set<Int>> = mapOf(
        "apple" to (1..5).toSet() + (8..12),
        "strawberry" to (4..8).toSet(),
        "tomato" to (6..10).toSet(),
        "potato" to (1..12).toSet(),
        "carrot" to (1..12).toSet(),
        "onion" to (1..12).toSet(),
        "broccoli" to (6..11).toSet(),
        "lettuce" to (5..10).toSet(),
        "cucumber" to (7..10).toSet(),
        "bell pepper" to (7..10).toSet(),
        "mushroom" to (1..12).toSet(),
        "corn" to (8..9).toSet(),
        "eggplant" to (7..10).toSet(),
        "zucchini" to (7..11).toSet(),
        "celery" to (7..11).toSet(),
        "leeks" to (1..12).toSet(),
        "cauliflower" to (3..11).toSet(),
        "endive" to (4..12).toSet(),
        "fennel" to (6..11).toSet()
    )

    /** Italian/Dutch names of [englishName], for [FoodCatalogItem.keywords]. */
    fun translationsOf(englishName: String): List<String> =
        translations[englishName.lowercase()]?.toList().orEmpty()

    /** Lowercase, accent-free, whitespace-collapsed form used to compare names ("  Caffè  " == "caffe"). */
    fun normalizeKey(name: String): String =
        Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("\\s+"), " ")

    /** Normalised English/Italian/Dutch name or alias -> catalog item; first catalog entry wins. */
    private val exactIndex: Map<String, FoodCatalogItem> by lazy {
        val index = HashMap<String, FoodCatalogItem>()
        for (item in FoodCatalog.items) {
            for (key in item.keywords + translationsOf(item.name)) index.putIfAbsent(normalizeKey(key), item)
        }
        index
    }

    /** Catalog item whose English/Italian/Dutch name or alias is exactly [name] (case, accents and spacing ignored). */
    private fun exact(name: String): FoodCatalogItem? = exactIndex[normalizeKey(name)]

    /**
     * Name to store for a hand-typed or synced item: whitespace collapsed, and either the catalog's
     * own spelling (when the text is exactly a catalog name/alias in any language, so "latte" and
     * "LATTE " become "Milk" and get icon, translations and season) or at least a leading capital.
     */
    fun tidyName(name: String): String {
        val collapsed = name.trim().replace(Regex("\\s+"), " ")
        exact(collapsed)?.let { return it.name }
        return collapsed.replaceFirstChar { it.uppercase() }
    }

    /** "Latte / Melk" for a catalog item shown in [langs] ("en", "it", "nl"); anything else stays as typed. */
    fun displayName(name: String, langs: List<String>): String {
        val shownAsTyped = name.trim().replaceFirstChar { it.uppercase() }
        val item = exact(name) ?: return shownAsTyped
        val t = translations[item.name.lowercase()] ?: return shownAsTyped
        val shown = langs.mapNotNull {
            when (it) {
                "en" -> item.name
                "it" -> t.first
                "nl" -> t.second
                else -> null
            }
        }.distinct()
        return if (shown.isEmpty()) shownAsTyped else shown.joinToString(" / ")
    }

    /** true = in Dutch season this month, false = out of season, null = no season info. */
    fun inSeason(name: String, month: Int = LocalDate.now().monthValue): Boolean? {
        val item = exact(name) ?: return null
        if (item.category != "Fruits and vegetables") return null
        return dutchSeason[item.name.lowercase()]?.contains(month)
    }
}
