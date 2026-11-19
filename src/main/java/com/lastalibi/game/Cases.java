package com.lastalibi.game;

/** The six murder cases, grouped into three worlds (Hollywood, Jaipur, Arabian Nights). */
public final class Cases {
    private Cases() {}

    /** One suspect: full name, short name, and whether the pronoun is "she". */
    public record Person(String name, String shortName, boolean female) {}

    /** Visual/word theme: I = Indian (Jaipur), H = Hollywood, A = Arabian. */
    public enum Variant { I, H, A }

    public record Case(String title, String location, String story, String[] rooms, Person[] cast,
                       int transform, Variant variant) {}

    public record Theme(String name, String tagline, int[] cases) {}

    public static final String[] LEVEL_NAMES = {"Easy", "Medium", "Hard"};
    /** Par solve time in seconds per difficulty (for the 3-star rating). */
    public static final int[] PAR = {240, 420, 720};

    public static final Theme[] THEMES = {
        new Theme("HOLLYWOOD", "Lights. Camera. Murder.", new int[]{1, 3}),
        new Theme("JAIPUR", "Secrets of the Pink City", new int[]{0, 4}),
        new Theme("ARABIAN NIGHTS", "Palaces of sand and silk", new int[]{2, 5}),
    };

    private static Person p(String n, String s, boolean f) { return new Person(n, s, f); }

    public static final Case[] ALL = {
        new Case("The Necklace of Rang Mahal", "Jaipur, Rajasthan",
            "Teej festival night at Haveli Rang Mahal. Sethani Kamla Devi, the wealthy matriarch, lies dead and her emerald Rajputana necklace is missing. The main gate was bolted from inside, so the killer is still here.",
            new String[]{"Rasoi", "Baithak", "Dalaan", "Zenana Mahal", "Sheesh Mahal", "Snaan Ghar"},
            new Person[]{p("Thakur Vikram Singh", "Vikram", false), p("Rani Padmini Devi", "Padmini", true),
                p("Kunwar Aditya Rathore", "Aditya", false), p("Munim Ratanlal", "Ratanlal", false),
                p("Dr. Kavita Sharma", "Kavita", true), p("Pandit Shivnarayan", "Shivnarayan", false),
                p("Bhanwari Devi", "Bhanwari", true), p("Jamal Khan", "Jamal", false),
                p("Sethani Kamla Devi", "Kamla", true)},
            0, Variant.I),
        new Case("Lights, Camera, Murder!", "Beverly Hills, Hollywood",
            "Oscar night after-party at a Beverly Hills mansion. Screen legend Veronica Starling is found dead and the gold statuette from her mantel is missing. The gates are guarded by security, so the killer is still among the guests.",
            new String[]{"Studio Kitchen", "Screening Room", "Grand Foyer", "Guest Suite", "Star Suite", "Spa Bath"},
            new Person[]{p("Max Sterling", "Max", false), p("Dolores Dane", "Dolores", true),
                p("Rex Hartwell", "Rex", false), p("Sheila Rourke", "Sheila", true),
                p("Dr. Nigel Pryce", "Nigel", false), p("Lola Vega", "Lola", true),
                p("Chuck Brannigan", "Chuck", false), p("Ingrid Holm", "Ingrid", true),
                p("Veronica Starling", "Veronica", true)},
            1, Variant.H),
        new Case("The Sheikh's Emerald", "Dubai, UAE",
            "Midnight in a clifftop palace above the Dubai skyline. The priceless Star of Arabia emerald has vanished and Sheikha Noura Al-Rashid lies dead. The palace doors were sealed for the night, so the culprit is still inside.",
            new String[]{"Majlis Kitchen", "Grand Majlis", "Diwan Corridor", "Guest Chamber", "Sheikh Suite", "Hammam"},
            new Person[]{p("Sheikh Khalid Al-Rashid", "Khalid", false), p("Layla Al-Mansoori", "Layla", true),
                p("Zubair Khan", "Zubair", false), p("Hitesh Patel", "Hitesh", false),
                p("Dr. Amira Hassan", "Amira", true), p("Omar Qureshi", "Omar", false),
                p("Meera Patel", "Meera", true), p("Faisal Al-Dosari", "Faisal", false),
                p("Sheikha Noura Al-Rashid", "Noura", true)},
            1, Variant.A),
        new Case("Backlot Blackout", "Studio Lot, Burbank",
            "The power dies on the last night of shooting a blockbuster. When the lights return, studio mogul Sterling Voss is dead and the reels of the final cut are missing. The studio gates were locked for wrap night.",
            new String[]{"Craft Services", "Soundstage", "Studio Lot", "Trailer Suite", "Producer Office", "Makeup Room"},
            new Person[]{p("Jack Calloway", "Jack", false), p("Savannah Blake", "Savannah", true),
                p("Marcus Thorne", "Marcus", false), p("Gloria Finch", "Gloria", true),
                p("Dev Malhotra", "Dev", false), p("Penny Ng", "Penny", true),
                p("Leo Castellano", "Leo", false), p("Harper Quinn", "Harper", true),
                p("Sterling Voss", "Sterling", false)},
            2, Variant.H),
        new Case("The Maharaja's Durbar", "Amer Fort, Jaipur",
            "Dussehra week at Amer Fort. The royal sceptre has been stolen from the Durbar and Raj Mata Hemlata Devi lies dead. The fort gates were bolted at sunset, so the culprit walks among the family.",
            new String[]{"Rasoi Mahal", "Diwan-e-Khas", "Zenana Dalaan", "Sukh Niwas", "Maharaja Mahal", "Hammam"},
            new Person[]{p("Thakur Gajendra Singh", "Gajendra", false), p("Rani Kanchan Kumari", "Kanchan", true),
                p("Yuvraj Harshvardhan", "Harsh", false), p("Diwan Balwant Rao", "Balwant", false),
                p("Vaidya Ashok Joshi", "Ashok", false), p("Mohini Bai", "Mohini", true),
                p("Colonel Pratap Rathore", "Pratap", false), p("Dr. Sunita Mehta", "Sunita", true),
                p("Raj Mata Hemlata Devi", "Hemlata", true)},
            3, Variant.I),
        new Case("The Oasis Gala", "Liwa Oasis, Arabian Desert",
            "A sandstorm traps the guests of the Oasis Gala deep in the desert. Emir Nasser Al-Qasimi is found dead and his jeweled dagger has disappeared. Nobody can leave until the storm passes.",
            new String[]{"Spice Kitchen", "Grand Majlis", "Mosaic Gallery", "Oasis Suite", "Emir Suite", "Hammam Spa"},
            new Person[]{p("Sheikh Tariq Al-Farsi", "Tariq", false), p("Yasmin Patel", "Yasmin", true),
                p("Rafiq Khan", "Rafiq", false), p("Noor Al-Hashimi", "Noor", true),
                p("Prof. Idris Mahdi", "Idris", false), p("Salma Qureshi", "Salma", true),
                p("Vikram Patel", "Vikram", false), p("Jamal Al-Said", "Jamal", false),
                p("Emir Nasser Al-Qasimi", "Nasser", false)},
            3, Variant.A),
    };

    /** Theme index (0..2) that owns case ci. */
    public static int themeOf(int ci) {
        for (int t = 0; t < THEMES.length; t++)
            for (int c : THEMES[t].cases()) if (c == ci) return t;
        return 0;
    }
}
