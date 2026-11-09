package com.alessiomartini.dispensa.data

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
        "canned tomatoes" to ("Pelati" to "Tomaten uit blik"), "beans" to ("Fagioli" to "Bonen"),
        "jam" to ("Marmellata" to "Jam"), "peanut butter" to ("Burro di arachidi" to "Pindakaas"),
        "chocolate" to ("Cioccolato" to "Chocolade"), "pesto" to ("Pesto" to "Pesto"), "hummus" to ("Hummus" to "Hummus"),
        "omega 3" to ("Omega 3" to "Omega 3"), "broth dice" to ("Dado" to "Bouillonblokjes"),
        "chickpeas" to ("Ceci" to "Kikkererwten"), "lentils" to ("Lenticchie" to "Linzen"),
        "mixed nuts" to ("Frutta secca" to "Gemengde noten"), "nutella" to ("Nutella" to "Nutella"),
        "corn kernels" to ("Mais in scatola" to "Maiskorrels"), "olives" to ("Olive" to "Olijven"),
        "tahini" to ("Tahina" to "Tahini"), "coconut milk" to ("Latte di cocco" to "Kokosmelk"),
        "mashed potatoes" to ("Purè di patate" to "Aardappelpuree"),
        "parsley" to ("Prezzemolo" to "Peterselie"), "mint" to ("Menta" to "Munt"), "salt" to ("Sale" to "Zout"),
        "pepper" to ("Pepe" to "Peper"), "curry" to ("Curry" to "Kerrie"), "paprika powder" to ("Paprika in polvere" to "Paprikapoeder"),
        "oregano" to ("Origano" to "Oregano"), "cinnamon" to ("Cannella" to "Kaneel"), "cumin" to ("Cumino" to "Komijn"),
        "chili flakes" to ("Peperoncino" to "Chilivlokken"), "nutmeg" to ("Noce moscata" to "Nootmuskaat"),
        "ice cream" to ("Gelato" to "IJs"), "frozen pizza" to ("Pizza surgelata" to "Diepvriespizza"),
        "frozen vegetables" to ("Verdure surgelate" to "Diepvriesgroenten"), "fries" to ("Patatine fritte" to "Friet"),
        "burrito" to ("Burrito" to "Burrito"),
        "water" to ("Acqua" to "Water"), "juice" to ("Succo" to "Sap"), "coffee" to ("Caffè" to "Koffie"),
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
     * Months (1-12) in which the product is on the Dutch market from Dutch growing (open ground,
     * greenhouse or storage). Basis: Milieu Centraal groente- en fruitkalender
     * (groentefruit.milieucentraal.nl, Dutch-origin columns), rounded to whole months.
     * Fruit/veg not listed here (banana, citrus, avocado, pineapple, grapes, watermelon, garlic)
     * are imported all year, so they get no season label.
     */
    private val dutchSeason: Map<String, Set<Int>> = mapOf(
        "apple" to (setOf(1, 2, 3, 4) + (8..12)),
        "strawberry" to (5..9).toSet(),
        "tomato" to (5..10).toSet(),
        "potato" to (1..12).toSet(),
        "carrot" to (1..12).toSet(),
        "onion" to (1..12).toSet(),
        "broccoli" to (6..11).toSet(),
        "lettuce" to (5..10).toSet(),
        "cucumber" to (5..10).toSet(),
        "bell pepper" to (6..10).toSet(),
        "mushroom" to (1..12).toSet(),
        "corn" to (8..10).toSet(),
        "eggplant" to (7..10).toSet(),
        "zucchini" to (6..10).toSet(),
        "celery" to (7..10).toSet(),
        "leeks" to (setOf(1, 2, 3, 4) + (7..12)),
        "cauliflower" to (6..11).toSet()
    )

    /** Italian/Dutch names of [englishName], for [FoodCatalogItem.keywords]. */
    fun translationsOf(englishName: String): List<String> =
        translations[englishName.lowercase()]?.toList().orEmpty()

    /** Catalog item whose English/Italian/Dutch name or alias is exactly [name] (not a substring match). */
    private fun exact(name: String): FoodCatalogItem? {
        val n = name.trim().lowercase()
        return FoodCatalog.items.firstOrNull { item ->
            n in item.keywords || translationsOf(item.name).any { it.lowercase() == n }
        }
    }

    /** "Latte / Melk" for a catalog item shown in [langs] ("en", "it", "nl"); anything else stays as typed. */
    fun displayName(name: String, langs: List<String>): String {
        val item = exact(name) ?: return name
        val t = translations[item.name.lowercase()] ?: return name
        val shown = langs.mapNotNull {
            when (it) {
                "en" -> item.name
                "it" -> t.first
                "nl" -> t.second
                else -> null
            }
        }.distinct()
        return if (shown.isEmpty()) name else shown.joinToString(" / ")
    }

    /** true = in Dutch season this month, false = out of season, null = no season info. */
    fun inSeason(name: String, month: Int = LocalDate.now().monthValue): Boolean? {
        val item = exact(name) ?: return null
        if (item.category != "Fruits and vegetables") return null
        return dutchSeason[item.name.lowercase()]?.contains(month)
    }
}
