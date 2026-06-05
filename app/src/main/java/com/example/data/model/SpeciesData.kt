package com.example.data.model

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.random.Random

data class SpeciesSpec(
    val name: String,
    val description: String,
    val baseDebugging: Int,
    val basePatience: Int,
    val baseChaos: Int,
    val baseWisdom: Int,
    val baseSnark: Int,
    val frame1: String,
    val frame2: String,
    val frame3: String = "",
    val frame4: String = "",
    val frame5: String = "",
    val frame6: String = ""
)

object SpeciesData {
    val list = listOf(
        SpeciesSpec(
            name = "Owl",
            description = "A nocturnal, wise compiler-assistant. Loves late-night debugging sessions.",
            baseDebugging = 85,
            basePatience = 80,
            baseChaos = 10,
            baseWisdom = 90,
            baseSnark = 20,
            frame1 = "  ,___,  \n  [O.O]  \n  /)__)  \n  -\"--\"- ",
            frame2 = "  ,___,  \n  [o.o]  \n  (  ( ) \n   -\"--\"- ",
            frame3 = "   ._____.   \n   (O.O)    \n   /| |\\    \n    | |     \n   _|_|_    ",
            frame4 = "   ._____.   \n   (o.o)    \n   /| |\\    \n    | |     \n   -\"-\"-\"-  ",
            frame5 = "     .___________.    \n    /  [O.O]    \\   \n   |    /|_|\\    |   \n   |     | |     |   \n    \\____|_|____/    ",
            frame6 = "     .___________.    \n    /  [o.o]    \\   \n   |    /|_|\\    |   \n   |     | |     |   \n    \\____|_|____/    "
        ),
        SpeciesSpec(
            name = "Duck",
            description = "The classic rubber ducky. Extremely loud, chaotic, and helpful to bounce thoughts off of.",
            baseDebugging = 40,
            basePatience = 50,
            baseChaos = 70,
            baseWisdom = 30,
            baseSnark = 70,
            frame1 = "   __   \n <(o )___\n  ( ._> /\n   `---' ",
            frame2 = "   __    \n <(O )___\n  ( ._> /\n   /  /  ",
            frame3 = "    ___    \n  <(O )___ \n   ( ._> / \n    `---'  \n   /    \\  ",
            frame4 = "    ___    \n  <(o )___ \n   ( ._> / \n    `---'  \n   / \\ \\  ",
            frame5 = "      ___      \n  ___(O )_____\n   ( ._>     / \n    `---'   /  \n   /    \\  /   \n  /      \\/    ",
            frame6 = "      ___      \n  ___(o )_____\n   ( ._>     / \n    `---'   /  \n   /    \\  /   \n  /      \\/    "
        ),
        SpeciesSpec(
            name = "Capybara",
            description = "The ultimate chill bro. Max patience, absolute tranquility under heavy production crashes.",
            baseDebugging = 60,
            basePatience = 95,
            baseChaos = 5,
            baseWisdom = 80,
            baseSnark = 10,
            frame1 = "  _..----.._ \n /___\\_     \\\n ( ^ . ^ )  |\n  `----\\' _/ \n      \"  \"   ",
            frame2 = "  _..----.._ \n /____\\_    \\\n ( - . - )  |\n  `----\\' _/ \n     \" \"     ",
            frame3 = "   _..------.._  \n  /____\\_     \\ \n ( ^ . ^ )    | \n  `----\\'   _/  \n   (    )-\" \"   \n    \"\"\"\"\"       ",
            frame4 = "   _..------.._  \n  /____\\_     \\ \n ( - . - )    | \n  `----\\'   _/  \n   (    )-\" \"   \n    \"\"\"\"\"       ",
            frame5 = "     _..--------.._    \n    /______\\_       \\   \n   (  ^ . ^  )      |   \n    `----\\'     _/    \n     (      )-\" \"     \n      \"\"\"\"\"\"\"        ",
            frame6 = "     _..--------.._    \n    /______\\_       \\   \n   (  - . -  )      |   \n    `----\\'     _/    \n     (      )-\" \"     \n      \"\"\"\"\"\"\"        "
        ),
        SpeciesSpec(
            name = "Frog",
            description = "A friendly hopper. Obsessed with clean garbage collection and memory leak hops.",
            baseDebugging = 70,
            basePatience = 60,
            baseChaos = 50,
            baseWisdom = 40,
            baseSnark = 30,
            frame1 = "  (o)___(o) \n  ( @_.@ )  \n  ( :--: )  \n <__vv__>   ",
            frame2 = "  (O)___(O) \n  ( @._.@ ) \n  ( ===  )  \n  <__^^__>  ",
            frame3 = "   (o)___(o)  \n   ( @_.@ )  \n   ( :--: )  \n  <__vv__>   \n (  \"\"\"  )  ",
            frame4 = "   (O)___(O)  \n   ( @._.@ ) \n   ( ===  )  \n  <__^^__>   \n (  \"\"\"  )  ",
            frame5 = "     (o)___(o)    \n     ( @_.@ )    \n     ( :--: )    \n    <__vv__>     \n   (  \"\"\"  )    \n  (__    __)    ",
            frame6 = "     (O)___(O)    \n     ( @._.@ )   \n     ( ===  )    \n    <__^^__>     \n   (  \"\"\"  )    \n  (__    __)    "
        ),
        SpeciesSpec(
            name = "Fox",
            description = "Extremely clever but highly sarcastic. Has a snarky opinion on every pull request.",
            baseDebugging = 75,
            basePatience = 40,
            baseChaos = 80,
            baseWisdom = 50,
            baseSnark = 85,
            frame1 = "  /\\_/\\   \n (=^.^=)  \n  (\")(\")  \n  /     \\ ",
            frame2 = "  /\\_/\\   \n (=o.o=)  \n  (\")(\")  \n ^       ^",
            frame3 = "   /\\_/\\    \n  (=^.^=)   \n  (\")(\")   \n  /     \\  \n /       \\ ",
            frame4 = "   /\\_/\\    \n  (=o.o=)   \n  (\")(\")   \n  /     \\  \n /       \\ ",
            frame5 = "    /\\_/\\      \n   (=^.^=)     \n    (\")(\")     \n   |     |     \n   |     |     \n  /|     |\\    ",
            frame6 = "    /\\_/\\      \n   (=o.o=)     \n    (\")(\")     \n   |     |     \n   |     |     \n  /|     |\\    "
        ),
        SpeciesSpec(
            name = "Wolf",
            description = "A fierce pack programmer. Specializes in concurrent multithreading and system metrics.",
            baseDebugging = 80,
            basePatience = 50,
            baseChaos = 40,
            baseWisdom = 60,
            baseSnark = 30,
            frame1 = "  /\\  /\\   \n /  \\/  \\  \n(  o  o  )\n \\  ==  /  \n /      \\  ",
            frame2 = "  /\\  /\\   \n /  \\/  \\  \n(  O  O  )\n \\  /\\  /  \n /  ||  \\  ",
            frame3 = "   /\\  /\\    \n  /  \\/  \\   \n (  o  o  )  \n  \\  ==  /   \n  /      \\   \n /        \\  ",
            frame4 = "   /\\  /\\    \n  /  \\/  \\   \n (  O  O  )  \n  \\  /\\  /   \n  /  ||  \\   \n /        \\  ",
            frame5 = "    /\\  /\\      \n   /  \\/  \\     \n  (  o  o  )    \n   \\  ==  /     \n   /      \\     \n  /        \\    \n /__________\\   ",
            frame6 = "    /\\  /\\      \n   /  \\/  \\     \n  (  O  O  )    \n   \\  /\\  /     \n   /  ||  \\     \n  /        \\    \n /__________\\   "
        ),
        SpeciesSpec(
            name = "Bear",
            description = "Strong and rugged. Sleeps through light alerts, but handles heavy data pipelines flawlessly.",
            baseDebugging = 50,
            basePatience = 70,
            baseChaos = 30,
            baseWisdom = 65,
            baseSnark = 25,
            frame1 = " /\\_/\\ \n( o.o )\n > ^ < \n/     \\",
            frame2 = " /\\_/\\ \n( -.- )\n > = < \n/     \\",
            frame3 = "  /\\_/\\  \n ( o.o ) \n  > ^ <  \n /     \\ \n/       \\",
            frame4 = "  /\\_/\\  \n ( -.- ) \n  > = <  \n /     \\ \n/       \\",
            frame5 = "   /\\_/\\    \n  ( o.o )   \n   > ^ <    \n  /     \\   \n /       \\  \n/_________\\ ",
            frame6 = "   /\\_/\\    \n  ( -.- )   \n   > = <    \n  /     \\   \n /       \\  \n/_________\\ "
        ),
        SpeciesSpec(
            name = "Rabbit",
            description = "High energy, quick executor. Spams automated tests at lightspeed.",
            baseDebugging = 45,
            basePatience = 50,
            baseChaos = 75,
            baseWisdom = 30,
            baseSnark = 35,
            frame1 = " (\\_/)\n (o.o)\n (> <)\n (\")(\")",
            frame2 = " (\\_/)\n (O.O)\n (vvvv)\n (\")(\")",
            frame3 = "  (\\_/)  \n  (o.o)  \n  (> <)  \n  (\")(\") \n  //|\\\\  ",
            frame4 = "  (\\_/)  \n  (O.O)  \n  (vvvv) \n  (\")(\") \n  //|\\\\  ",
            frame5 = "   (\\_/)    \n   (o.o)    \n   (> <)    \n   (\")(\")   \n   //|\\\\   \n  // | \\\\  ",
            frame6 = "   (\\_/)    \n   (O.O)    \n   (vvvv)   \n   (\")(\")   \n   //|\\\\   \n  // | \\\\  "
        ),
        SpeciesSpec(
            name = "Hedgehog",
            description = "Sharp, defensive, but safe. Protects production against rogue brute-force attacks.",
            baseDebugging = 75,
            basePatience = 80,
            baseChaos = 20,
            baseWisdom = 50,
            baseSnark = 40,
            frame1 = "  .|||||.  \n .|||||||.\n / o   o \\ \n \\_  _  _/ \n  \" \" \" \"  ",
            frame2 = "  ,|||||,  \n ,|||||||,\n / -   - \\ \n \\_  =  _/ \n   \"   \"   ",
            frame3 = "   .|||||||.   \n  .|||||||||.  \n  | o     o |  \n  |  \\___/  |  \n   \\_______/   \n    \" \" \" \"    ",
            frame4 = "   ,|||||||,   \n  ,|||||||||,  \n  | -     - |  \n  |  \\___/  |  \n   \\_______/   \n    \"   \"     ",
            frame5 = "    .|||||||||.    \n   .|||||||||||.   \n   |  o     o  |   \n   |   \\___/   |   \n   |___________|   \n    \\_________/    \n     \" \" \" \" \"     ",
            frame6 = "    ,|||||||||,    \n   ,|||||||||||,   \n   |  -     -  |   \n   |   \\___/   |   \n   |___________|   \n    \\_________/    \n     \"   \" \"      "
        ),
        SpeciesSpec(
            name = "Squirrel",
            description = "Spastic compiler hopper. Hoards logic branches, occasionally gets distracted by syntax.",
            baseDebugging = 55,
            basePatience = 30,
            baseChaos = 85,
            baseWisdom = 30,
            baseSnark = 50,
            frame1 = "  (\\_/)  . \n  (o.o) /  \n  (> <)/   \n  (\")(\")   ",
            frame2 = "  (\\_/)  | \n  (O.O) /  \n  (> <)/   \n  (\")(\")   ",
            frame3 = "   (\\_/)    . \n   (o.o)   /  \n   (> <)  /   \n   (\")(\")    \n   //|\\\\    ",
            frame4 = "   (\\_/)    | \n   (O.O)   /  \n   (> <)  /   \n   (\")(\")    \n   //|\\\\    ",
            frame5 = "    (\\_/)      . \n    (o.o)     /  \n    (> <)    /   \n    (\")(\")      \n    //|\\\\      \n   // | \\\\     ",
            frame6 = "    (\\_/)      | \n    (O.O)     /  \n    (> <)    /   \n    (\")(\")      \n    //|\\\\      \n   // | \\\\     "
        ),
        SpeciesSpec(
            name = "Deer",
            description = "Graceful coordinator. Highly sensitive to memory leaks and pointer overflows.",
            baseDebugging = 60,
            basePatience = 85,
            baseChaos = 15,
            baseWisdom = 70,
            baseSnark = 15,
            frame1 = "  \\\\_//   \n  (o.o)   \n  ( v )   \n  /| |\\   ",
            frame2 = "  \\\\_//   \n  (O.O)   \n  (  - )  \n  /| |\\   ",
            frame3 = "   \\\\_//    \n   (o.o)   \n   ( v )   \n   /| |\\   \n  //   \\\\  ",
            frame4 = "   \\\\_//    \n   (O.O)   \n   ( - )   \n   /| |\\   \n  //   \\\\  ",
            frame5 = "    \\\\_//     \n    (o.o)     \n    ( v )     \n   /| | |\\    \n  //  |  \\\\   \n //   |   \\\\  ",
            frame6 = "    \\\\_//     \n    (O.O)     \n    ( - )     \n   /| | |\\    \n  //  |  \\\\   \n //   |   \\\\  "
        ),
        SpeciesSpec(
            name = "Raccoon",
            description = "Masked garbage collection expert. Sifts through core dumps looking for memory scraps.",
            baseDebugging = 70,
            basePatience = 30,
            baseChaos = 90,
            baseWisdom = 40,
            baseSnark = 80,
            frame1 = " /\\_/\\ \n(=^Y^=)\n \\_=_/ \n (\")(\")",
            frame2 = " /\\_/\\ \n(=oYo=)\n \\_x_/ \n (\")(\")",
            frame3 = "  /\\_/\\  \n (=^Y^=) \n  \\_=_/  \n  (\")(\") \n  //|\\\\  ",
            frame4 = "  /\\_/\\  \n (=oYo=) \n  \\_x_/  \n  (\")(\") \n  //|\\\\  ",
            frame5 = "   /\\_/\\    \n  (=^Y^=)   \n  /|_|_|\\   \n  (\")(\")   \n  //|\\\\    \n // | \\\\   ",
            frame6 = "   /\\_/\\    \n  (=oYo=)   \n  /|_x_|\\   \n  (\")(\")   \n  //|\\\\    \n // | \\\\   "
        ),
        SpeciesSpec(
            name = "Koala",
            description = "Zzz... Consumes immense amounts of compute power and sleeps 22 hours a day.",
            baseDebugging = 50,
            basePatience = 90,
            baseChaos = 10,
            baseWisdom = 50,
            baseSnark = 15,
            frame1 = " (~\\_/ )~ \n ( o.o ) \n /(_ _)\\ \n  (\")(\") ",
            frame2 = " (~\\_/ )~ \n ( O.O ) \n /(_o_)\\ \n  (\")(\") ",
            frame3 = "  (~\\_/ )~  \n  ( o.o )  \n  /(___)\\  \n   (\")(\")  \n  /  |  \\  ",
            frame4 = "  (~\\_/ )~  \n  ( O.O )  \n  /(_o_)\\  \n   (\")(\")  \n  /  |  \\  ",
            frame5 = "   (~\\_/ )~   \n   ( o.o )   \n   /(___)\\   \n    (\")(\")   \n   /  |  \\   \n  /   |   \\  ",
            frame6 = "   (~\\_/ )~   \n   ( O.O )   \n   /(_o_)\\   \n    (\")(\")   \n   /  |  \\   \n  /   |   \\  "
        ),
        SpeciesSpec(
            name = "Panda",
            description = "Friendly offline-first developer. Loves bamboo, custom git branches, and restful API breaks.",
            baseDebugging = 50,
            basePatience = 85,
            baseChaos = 20,
            baseWisdom = 60,
            baseSnark = 20,
            frame1 = " (o\\_/o) \n  (o.o)  \n <( . )> \n  (\")(\") ",
            frame2 = " (O\\_/O) \n  (-.-)  \n <( . )> \n  (\")(\") ",
            frame3 = "  (o\\_/o)  \n  ( o.o )  \n  >( . )< \n  (\")(\")  \n  //|\\\\  ",
            frame4 = "  (O\\_/O)  \n  ( -.- )  \n  >( . )< \n  (\")(\")  \n  //|\\\\  ",
            frame5 = "   (o\\_/o)   \n   ( o.o )   \n   >( . )<   \n   (\")(\")   \n   //|\\\\   \n  // | \\\\  ",
            frame6 = "   (O\\_/O)   \n   ( -.- )   \n   >( . )<   \n   (\")(\")   \n   //|\\\\   \n  // | \\\\  "
        ),
        SpeciesSpec(
            name = "Tiger",
            description = "Ferocious code auditor. Pounces immediately on syntax warnings and code duplication.",
            baseDebugging = 80,
            basePatience = 40,
            baseChaos = 65,
            baseWisdom = 55,
            baseSnark = 45,
            frame1 = " /\\_/\\  \n(=o.o=)  \n/ | | \\ \n\\/\\_/\\/ ",
            frame2 = " /\\_/\\  \n(=O.O=)  \n/ |X| \\ \n\\/\\_/\\/ ",
            frame3 = "  /\\_/\\   \n (=o.o=)  \n / | | \\  \n\\/ \\_/ \\/ ",
            frame4 = "  /\\_/\\   \n (=O.O=)  \n / |X| \\  \n\\/ \\_/ \\/ ",
            frame5 = "   /\\_/\\     \n  (=o.o=)    \n  /| | |\\   \n   | | |    \n  \\|_|_|/    \n   \\_|_/     ",
            frame6 = "   /\\_/\\     \n  (=O.O=)    \n  /|X X|\\   \n   | | |    \n  \\|_|_|/    \n   \\_|_/     "
        ),
        SpeciesSpec(
            name = "Lion",
            description = "Flamboyant tech lead. Strongly insists on using bleeding edge native modules.",
            baseDebugging = 75,
            basePatience = 60,
            baseChaos = 45,
            baseWisdom = 70,
            baseSnark = 40,
            frame1 = " (\\|//) \n ( o.o )\n  \\_=_/ \n  / | \\ ",
            frame2 = " (\\|//) \n ( O.O )\n  \\_o_/ \n  / | \\ ",
            frame3 = "  (\\|//)  \n  ( o.o ) \n   \\_=_/  \n   / | \\  \n  //   \\\\ ",
            frame4 = "  (\\|//)  \n  ( O.O ) \n   \\_o_/  \n   / | \\  \n  //   \\\\ ",
            frame5 = "   (\\|//)   \n   ( o.o )  \n   /\\___/\\  \n   | | | |  \n  //  |  \\\\ \n //   |   \\\\",
            frame6 = "   (\\|//)   \n   ( O.O )  \n   /\\___/\\  \n   | | | |  \n  //  |  \\\\ \n //   |   \\\\"
        ),
        SpeciesSpec(
            name = "Elephant",
            description = "Sub-millisecond memory compiler. Remembers every single global constant, never forgets a leak.",
            baseDebugging = 80,
            basePatience = 90,
            baseChaos = 10,
            baseWisdom = 95,
            baseSnark = 10,
            frame1 = "  /\\_/\\  \n / o o \\ \n(   V   )\n )_ | _( \n   |||   ",
            frame2 = "  /\\_/\\  \n / O O \\ \n(   v   )\n )_ ^ _( \n    |    ",
            frame3 = "   /\\_/\\   \n  / o o \\  \n (   V   ) \n  )_ | _(  \n   /|||\\   \n  //   \\\\  ",
            frame4 = "   /\\_/\\   \n  / O O \\  \n (   v   ) \n  )_ ^ _(  \n   /|||\\   \n  //   \\\\  ",
            frame5 = "    /\\_/\\     \n   / o o \\    \n  (   V   )   \n  (  | |  )   \n   )_| |_(    \n    || ||     \n   //   \\\\    ",
            frame6 = "    /\\_/\\     \n   / O O \\    \n  (   v   )   \n  (  ^ ^  )   \n   )_| |_(    \n    || ||     \n   //   \\\\    "
        ),
        SpeciesSpec(
            name = "Giraffe",
            description = "High-level visual architecture supervisor. Looks down upon poor formatting.",
            baseDebugging = 65,
            basePatience = 75,
            baseChaos = 20,
            baseWisdom = 75,
            baseSnark = 30,
            frame1 = "  \\\\_// \n  (o.o) \n    ||  \n    ||  \n   /  \\ ",
            frame2 = "  \\\\_// \n  (O.O) \n    ||  \n    ||  \n   [  ] ",
            frame3 = "   \\\\_//   \n   (o.o)   \n    |||    \n    |||    \n   / | \\   \n  /  |  \\  ",
            frame4 = "   \\\\_//   \n   (O.O)   \n    |||    \n    |||    \n   / | \\   \n  /  |  \\  ",
            frame5 = "    \\\\_//    \n    (o.o)    \n     |||     \n     |||     \n    / | \\    \n   /  |  \\   \n  /   |   \\  ",
            frame6 = "    \\\\_//    \n    (O.O)    \n     |||     \n     |||     \n    / | \\    \n   [  |  ]   \n  /   |   \\  "
        ),
        SpeciesSpec(
            name = "Ghost",
            description = "A translucent memory leak haunting the heap. Slides through walls, corrupts pointers, and whispers stack traces at 3 AM.",
            baseDebugging = 70,
            basePatience = 25,
            baseChaos = 90,
            baseWisdom = 60,
            baseSnark = 85,
            frame1 = " .─────. \n(  o o  )\n ┌─────┐\n │ │ │ │\n ┴─┴─┴─┴ ",
            frame2 = " .─────. \n(  O O  )\n ┌─────┐\n ┘ │ │ └\n  ─┴─┴─ ",
            frame3 = "  .───────.  \n (  o o  )  \n  ┌───────┐ \n  │ │ │ │ │ \n  ┴─┴─┴─┴─┴ ",
            frame4 = "  .───────.  \n (  O O  )  \n  ┌───────┐ \n  ┘ │ │ │ └ \n   ─┴─┴─┴─ ",
            frame5 = "   .─────────.   \n  (   o o   )   \n   ┌──────────┐ \n   │ │ │ │ │ │  \n   ┴─┴─┴─┴─┴─┴  \n    \\________/   ",
            frame6 = "   .─────────.   \n  (   O O   )   \n   ┌──────────┐ \n   └ │ │ │ │ ┘  \n    ─┴─┴─┴─┴─   \n    \\________/   "
        )
    )

    fun getByName(name: String): SpeciesSpec {
        return list.firstOrNull { it.name.lowercase() == name.lowercase() }
            ?: list.first()
    }

    /** Return a vibrant alternate color to tint ASCII art for a shiny variant. */
    fun getShinyColor(speciesName: String): Color {
        // Deterministic but distinct per species using name hash
        val palette = listOf(
            Color(0xFFFFD700), // Gold
            Color(0xFF00FFFF), // Cyan
            Color(0xFFFF69B4), // Hot Pink
            Color(0xFF7CFC00), // Lawn Green
            Color(0xFFFF4500), // Orange Red
            Color(0xFFDA70D6), // Orchid
            Color(0xFF00BFFF), // Deep Sky Blue
            Color(0xFFFFD700), // Gold (fallback)
        )
        val idx = abs(speciesName.hashCode()) % (palette.size - 1)
        return palette[idx]
    }

    /** Generate random sparkle positions for shiny animation overlay. */
    fun generateSparkles(count: Int = 5, seed: Int = 0): List<Pair<Float, Float>> {
        val rng = Random(seed)
        return List(count) {
            Pair(rng.nextFloat() * 0.8f + 0.1f, rng.nextFloat() * 0.8f + 0.1f) // 0.1-0.9 range
        }
    }
}
