package com.example.data.model

/** Definition of a single achievement that can be unlocked. */
data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val icon: String, // emoji icon
    val maxProgress: Int = 1 // 1 = single-shot, N = progressive (e.g. "Hatch 10 pets")
)

/** All achievement definitions in the game. */
object AchievementRegistry {
    val all: List<AchievementDef> = listOf(
        AchievementDef("first_hatch", "New Life", "Hatch your first pet", "🥚"),
        AchievementDef("level_5", "Growing Up", "Reach level 5 with any pet", "⭐"),
        AchievementDef("level_10", "Adolescent", "Reach level 10 (Teen stage)", "🌟"),
        AchievementDef("level_20", "Full Grown", "Reach level 20 (Adult stage)", "👑"),
        AchievementDef("first_feed", "Nurturer", "Feed your pet for the first time", "🍽️"),
        AchievementDef("first_train", "Code Monkey", "Run your first training session", "💻"),
        AchievementDef("first_wild_catch", "Wild Tamer", "Capture your first wild pet", "🌿"),
        AchievementDef("shiny_catch", "Shiny Hunter", "Capture a shiny wild pet", "✨"),
        AchievementDef("train_10", "Dedicated Dev", "Train your pet 10 times", "⚡", maxProgress = 10),
        AchievementDef("feed_25", "Master Chef", "Feed your pet 25 times", "🧑‍🍳", maxProgress = 25),
        AchievementDef("hunt_5", "Explorer", "Hunt for wild pets 5 times", "🧭", maxProgress = 5),
        AchievementDef("pet_10", "Affectionate", "Pet your buddy 10 times", "💕", maxProgress = 10),
        AchievementDef("rename_pet", "Identity Crisis", "Rename your pet", "🏷️"),
        AchievementDef("zx_50", "Point Collector", "Earn 50 ZX Points", "🪙"),
        AchievementDef("zx_200", "Points Millionaire", "Earn 200 ZX Points", "💰", maxProgress = 200),
        AchievementDef("all_modes", "Mode Master", "Try all 4 gameplay modes", "🎭", maxProgress = 4),
        AchievementDef("simon_5", "Memory Pro", "Score 5 or more in Simon Says", "🧠"),
        AchievementDef("fortune_teller", "Fortune Seeker", "Get your fortune told", "🔮"),
        AchievementDef("sleepyhead", "Power Napper", "Put your pet to sleep", "💤"),
        AchievementDef("code_review", "Code Critic", "Review code with your pet", "📝"),
        AchievementDef("bug_squisher", "Bug Squisher", "Squish 50 bugs in Whack-a-Bug", "🪳", maxProgress = 50),
        AchievementDef("whack_master", "Whack Master", "Score 20+ in one Whack-a-Bug game", "🏆"),
        AchievementDef("lucky_dev", "Lucky Dev", "Win your first Lucky Spin", "🍀"),
    )
}
