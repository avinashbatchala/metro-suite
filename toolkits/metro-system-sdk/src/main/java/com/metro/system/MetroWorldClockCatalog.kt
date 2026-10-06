package com.metro.system

/**
 * Shared, offline world-clock city catalog used by Clock (World Clock pivot) and the Widgets
 * World Clock widget. UI-independent, no network.
 *
 * Search matches city, region, IANA zone and aliases (case-insensitive).
 */
object MetroWorldClockCatalog {

    /** Sensible default selection for a freshly pinned World Clock widget (max 3). */
    val DEFAULT_IDS: List<String> = listOf("london", "new-york", "tokyo")

    private fun city(
        id: String,
        name: String,
        region: String,
        zoneId: String,
        vararg aliases: String,
    ) = MetroWorldClockCity(id, name, region, zoneId, aliases.toList())

    val cities: List<MetroWorldClockCity> = listOf(
        // Europe
        city("london", "London", "United Kingdom", "Europe/London", "uk", "england", "gmt", "bst"),
        city("paris", "Paris", "France", "Europe/Paris", "fr"),
        city("berlin", "Berlin", "Germany", "Europe/Berlin", "de"),
        city("madrid", "Madrid", "Spain", "Europe/Madrid", "es"),
        city("barcelona", "Barcelona", "Spain", "Europe/Madrid", "es"),
        city("rome", "Rome", "Italy", "Europe/Rome", "it"),
        city("milan", "Milan", "Italy", "Europe/Rome", "it"),
        city("amsterdam", "Amsterdam", "Netherlands", "Europe/Amsterdam", "nl"),
        city("brussels", "Brussels", "Belgium", "Europe/Brussels", "be"),
        city("dublin", "Dublin", "Ireland", "Europe/Dublin", "ie"),
        city("lisbon", "Lisbon", "Portugal", "Europe/Lisbon", "pt"),
        city("zurich", "Zurich", "Switzerland", "Europe/Zurich", "ch"),
        city("vienna", "Vienna", "Austria", "Europe/Vienna", "at"),
        city("prague", "Prague", "Czechia", "Europe/Prague", "cz"),
        city("warsaw", "Warsaw", "Poland", "Europe/Warsaw", "pl"),
        city("stockholm", "Stockholm", "Sweden", "Europe/Stockholm", "se"),
        city("oslo", "Oslo", "Norway", "Europe/Oslo", "no"),
        city("copenhagen", "Copenhagen", "Denmark", "Europe/Copenhagen", "dk"),
        city("helsinki", "Helsinki", "Finland", "Europe/Helsinki", "fi"),
        city("athens", "Athens", "Greece", "Europe/Athens", "gr"),
        city("bucharest", "Bucharest", "Romania", "Europe/Bucharest", "ro"),
        city("kyiv", "Kyiv", "Ukraine", "Europe/Kyiv", "kiev", "ua"),
        city("istanbul", "Istanbul", "Turkey", "Europe/Istanbul", "tr"),
        city("moscow", "Moscow", "Russia", "Europe/Moscow", "ru"),
        city("reykjavik", "Reykjavik", "Iceland", "Atlantic/Reykjavik", "is"),
        // Americas
        city("new-york", "New York", "United States", "America/New_York", "nyc", "us", "eastern"),
        city("washington", "Washington, D.C.", "United States", "America/New_York", "dc", "us"),
        city("miami", "Miami", "United States", "America/New_York", "us", "florida"),
        city("chicago", "Chicago", "United States", "America/Chicago", "us", "central"),
        city("denver", "Denver", "United States", "America/Denver", "us", "mountain"),
        city("phoenix", "Phoenix", "United States", "America/Phoenix", "us", "arizona"),
        city("los-angeles", "Los Angeles", "United States", "America/Los_Angeles", "la", "us", "pacific"),
        city("san-francisco", "San Francisco", "United States", "America/Los_Angeles", "sf", "us"),
        city("seattle", "Seattle", "United States", "America/Los_Angeles", "us"),
        city("anchorage", "Anchorage", "United States", "America/Anchorage", "alaska", "us"),
        city("honolulu", "Honolulu", "United States", "Pacific/Honolulu", "hawaii", "us"),
        city("toronto", "Toronto", "Canada", "America/Toronto", "ca"),
        city("vancouver", "Vancouver", "Canada", "America/Vancouver", "ca"),
        city("mexico-city", "Mexico City", "Mexico", "America/Mexico_City", "mx", "cdmx"),
        city("bogota", "Bogotá", "Colombia", "America/Bogota", "co"),
        city("lima", "Lima", "Peru", "America/Lima", "pe"),
        city("santiago", "Santiago", "Chile", "America/Santiago", "cl"),
        city("sao-paulo", "São Paulo", "Brazil", "America/Sao_Paulo", "br", "sao paulo"),
        city("buenos-aires", "Buenos Aires", "Argentina", "America/Argentina/Buenos_Aires", "ar"),
        // Asia
        city("tokyo", "Tokyo", "Japan", "Asia/Tokyo", "jp"),
        city("osaka", "Osaka", "Japan", "Asia/Tokyo", "jp"),
        city("seoul", "Seoul", "South Korea", "Asia/Seoul", "kr"),
        city("beijing", "Beijing", "China", "Asia/Shanghai", "cn"),
        city("shanghai", "Shanghai", "China", "Asia/Shanghai", "cn"),
        city("hong-kong", "Hong Kong", "China", "Asia/Hong_Kong", "hk"),
        city("taipei", "Taipei", "Taiwan", "Asia/Taipei", "tw"),
        city("singapore", "Singapore", "Singapore", "Asia/Singapore", "sg"),
        city("kuala-lumpur", "Kuala Lumpur", "Malaysia", "Asia/Kuala_Lumpur", "my", "kl"),
        city("jakarta", "Jakarta", "Indonesia", "Asia/Jakarta", "id"),
        city("manila", "Manila", "Philippines", "Asia/Manila", "ph"),
        city("bangkok", "Bangkok", "Thailand", "Asia/Bangkok", "th"),
        city("hanoi", "Hanoi", "Vietnam", "Asia/Ho_Chi_Minh", "vn"),
        city("mumbai", "Mumbai", "India", "Asia/Kolkata", "in", "bombay"),
        city("new-delhi", "New Delhi", "India", "Asia/Kolkata", "in", "delhi"),
        city("kolkata", "Kolkata", "India", "Asia/Kolkata", "in", "calcutta"),
        city("dhaka", "Dhaka", "Bangladesh", "Asia/Dhaka", "bd"),
        city("kathmandu", "Kathmandu", "Nepal", "Asia/Kathmandu", "np"),
        city("karachi", "Karachi", "Pakistan", "Asia/Karachi", "pk"),
        city("dubai", "Dubai", "United Arab Emirates", "Asia/Dubai", "ae", "uae"),
        city("abu-dhabi", "Abu Dhabi", "United Arab Emirates", "Asia/Dubai", "ae", "uae"),
        city("riyadh", "Riyadh", "Saudi Arabia", "Asia/Riyadh", "sa"),
        city("tel-aviv", "Tel Aviv", "Israel", "Asia/Jerusalem", "il"),
        city("tehran", "Tehran", "Iran", "Asia/Tehran", "ir"),
        // Africa
        city("cairo", "Cairo", "Egypt", "Africa/Cairo", "eg"),
        city("casablanca", "Casablanca", "Morocco", "Africa/Casablanca", "ma"),
        city("lagos", "Lagos", "Nigeria", "Africa/Lagos", "ng"),
        city("accra", "Accra", "Ghana", "Africa/Accra", "gh"),
        city("nairobi", "Nairobi", "Kenya", "Africa/Nairobi", "ke"),
        city("addis-ababa", "Addis Ababa", "Ethiopia", "Africa/Addis_Ababa", "et"),
        city("johannesburg", "Johannesburg", "South Africa", "Africa/Johannesburg", "za"),
        city("cape-town", "Cape Town", "South Africa", "Africa/Johannesburg", "za"),
        // Oceania
        city("sydney", "Sydney", "Australia", "Australia/Sydney", "au", "nsw"),
        city("melbourne", "Melbourne", "Australia", "Australia/Melbourne", "au", "vic"),
        city("brisbane", "Brisbane", "Australia", "Australia/Brisbane", "au", "qld"),
        city("perth", "Perth", "Australia", "Australia/Perth", "au", "wa"),
        city("adelaide", "Adelaide", "Australia", "Australia/Adelaide", "au", "sa"),
        city("auckland", "Auckland", "New Zealand", "Pacific/Auckland", "nz"),
        city("wellington", "Wellington", "New Zealand", "Pacific/Auckland", "nz"),
        city("suva", "Suva", "Fiji", "Pacific/Fiji", "fj"),
        // Neutral
        city("utc", "UTC", "Coordinated Universal Time", "UTC", "gmt", "zulu"),
    )

    private val byId: Map<String, MetroWorldClockCity> = cities.associateBy { it.id }

    fun byId(id: String): MetroWorldClockCity? = byId[id]

   /** Resolves [ids] to cities in order, skipping unknown ids. */
    fun resolve(ids: List<String>): List<MetroWorldClockCity> = ids.mapNotNull { byId[it] }

    fun search(query: String): List<MetroWorldClockCity> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return cities
        return cities.filter { city ->
            city.name.lowercase().contains(q) ||
                city.region.lowercase().contains(q) ||
                city.zoneId.lowercase().contains(q) ||
                city.aliases.any { it.lowercase().contains(q) }
        }
    }
}
