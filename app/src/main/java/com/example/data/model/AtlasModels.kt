package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class AtlasTab(val label: String) {
    ANATOMY("Anatomy"),
    DIVERSITY("Diversity"),
    LEARN("Learn"),
    RESOURCES("Resources"),
    SETTINGS("Settings")
}

enum class AnatomyViewType(
    val title: String,
    val shortLabel: String,
    val subtitle: String
) {
    EXTERNAL_VULVA(
        title = "External Vulva",
        shortLabel = "Vulva",
        subtitle = "Outer landmarks — every vulva has its own unique shape, symmetry, and proportion."
    ),
    INTERNAL_FRONT(
        title = "Internal Reproductive System",
        shortLabel = "Internal",
        subtitle = "Front cross-section showing the vagina, cervix, uterus, fallopian tubes, and ovaries."
    ),
    CLITORAL_NETWORK(
        title = "The Full Clitoral Network",
        shortLabel = "Clitoris",
        subtitle = "More than 90% of the clitoris lives inside the pelvis, wrapping around the urethra and vaginal canal."
    ),
    PELVIC_SIDE(
        title = "Pelvic Cross-Section",
        shortLabel = "Side View",
        subtitle = "How the bladder, urethra, clitoris, vagina, uterus, rectum, and pelvic floor sit together."
    )
}

enum class SensitivityTier(val label: String, val description: String) {
    ULTRA("Ultra-Sensitive", "Highest concentration of sensory nerve endings — gentle fluttering pulse"),
    HIGH("High Sensitivity", "Rich vascular erectile & mucosal nerve network — warm double ripple"),
    MODERATE("Supportive & Structural", "Broad touch & pressure receptors — subtle grounding tap")
}

data class AnatomyPart(
    val id: String,
    val number: Int,
    val name: String,
    val clinicalName: String,
    val viewType: AnatomyViewType,
    val normX: Float,
    val normY: Float,
    val sensitivityTier: SensitivityTier = SensitivityTier.MODERATE,
    val summary: String,
    val functionText: String,
    val pleasureNotes: String,
    val mythBuster: String,
    val partnerTip: String,
    val relatedArticleId: String
)

enum class WikimediaPhotoCategory(
    val label: String,
    val commonsCategory: String,
    val photosOnly: Boolean,
    val description: String
) {
    ALL_VULVAS(
        label = "All Vulvas",
        commonsCategory = "Category:Human vulvas",
        photosOnly = true,
        description = "Real, consented educational photographs celebrating natural shape, symmetry, and color diversity."
    ),
    CLOSE_UPS(
        label = "Close-Up Detail",
        commonsCategory = "Category:Human vulva close-ups",
        photosOnly = true,
        description = "High-clarity educational close-up photography showing authentic anatomical contours and textures."
    ),
    LABIA_DIVERSITY(
        label = "Labia Diversity",
        commonsCategory = "Category:Labia minora",
        photosOnly = true,
        description = "Natural variations in inner labia length, asymmetry, pigment gradients, and ruffled vs. smooth edges."
    ),
    CLITORAL_VIEWS(
        label = "Clitoris & Hood",
        commonsCategory = "Category:Clitoris",
        photosOnly = true,
        description = "Real educational photographs illustrating natural variation in clitoral hood drape and glans visibility."
    ),
    MEDICAL_PLATES(
        label = "Medical Plates",
        commonsCategory = "Category:Anatomy of human female reproductive system",
        photosOnly = false,
        description = "Open-access clinical and textbook anatomical illustrations from Wikimedia Commons."
    )
}

data class WikimediaPhotoItem(
    val pageId: Long,
    val title: String,
    val displayTitle: String,
    val caption: String,
    val thumbUrl: String,
    val fullUrl: String,
    val pageUrl: String,
    val width: Int,
    val height: Int,
    val author: String,
    val license: String
) {
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) (width.toFloat() / height.toFloat()).coerceIn(0.68f, 1.55f) else 1.1f
}

data class MedicalPlateAsset(
    val viewType: AnatomyViewType,
    val imageUrl: String,
    val attribution: String,
    val license: String,
    val sourcePageUrl: String
)

enum class DiversityTrait(val label: String) {
    ALL("All Natural"),
    ASYMMETRY("Asymmetry"),
    INNER_PROMINENT("Prominent Inner"),
    INNER_TUCKED("Tucked Inner"),
    HOOD_VARIATION("Clitoral Hoods"),
    TONE_SPECTRUM("Tone Spectrum"),
    LIFE_STAGES("Life Stages")
}

data class NaturalTonePalette(
    val name: String,
    val outerBase: Color,
    val innerBase: Color,
    val accentLine: Color,
    val cardTint: Color,
    val shadeNote: String
)

data class VulvaStudy(
    val studyNumber: Int,
    val seed: Int,
    val title: String,
    val primaryTrait: DiversityTrait,
    val tags: List<String>,
    val innerLeftProjection: Float,   // 0.12f (tucked) .. 0.95f (prominent)
    val innerRightProjection: Float,  // 0.12f .. 0.95f (independent for natural asymmetry)
    val outerFullness: Float,         // 0.25f (slender) .. 0.92f (plush)
    val hoodCoverage: Float,          // 0.20f (open) .. 0.90f (draped)
    val ruffleFactor: Float,          // 0.0f (smooth) .. 1.0f (scalloped/wavy)
    val verticalLength: Float,        // 0.72f .. 1.0f
    val paletteIndex: Int,
    val caption: String,
    val anatomicalNote: String,
    val educatorPrompt: String
)

enum class AudienceMode(val label: String, val tagline: String) {
    EVERYONE("Everyone", "Balanced, inclusive everyday guidance"),
    INDIVIDUALS("My Body", "Self-awareness, comfort & personal wellness"),
    PARTNERS("Partners", "Communication, empathy & practical literacy"),
    EDUCATORS("Educators", "Accurate language & myth-busting framing")
}

enum class ArticleCategory(val label: String) {
    ALL("All Topics"),
    ANATOMY("Anatomy 101"),
    CYCLES_MICROBIOME("Cycles & Ecosystem"),
    PLEASURE_INTIMACY("Pleasure & Intimacy"),
    HEALTH_CARE("Health & Care"),
    LIFE_STAGES("Life & Inclusion"),
    PARTNER_EDUCATOR("Partners & Educators")
}

data class ArticleSection(
    val heading: String,
    val body: String
)

data class GuideArticle(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: ArticleCategory,
    val readMinutes: Int,
    val summary: String,
    val sections: List<ArticleSection>,
    val partnerTakeaway: String,
    val educatorNote: String,
    val relatedResourceIds: List<String> = emptyList()
)

data class CyclePhase(
    val id: String,
    val name: String,
    val daysRange: String,
    val hormoneSummary: String,
    val cervicalFluid: String,
    val cervixFeel: String,
    val energyAndLibido: String,
    val careTip: String,
    val colorHex: Long
)

data class MythCard(
    val id: String,
    val mythStatement: String,
    val isTrue: Boolean,
    val verdictTitle: String,
    val explanation: String,
    val partnerAngle: String
)

data class GlossaryTerm(
    val term: String,
    val pronunciation: String,
    val category: String,
    val definition: String
)

enum class ResourceCategory(val label: String) {
    PHOTO_ARCHIVES("Curated Diversity Photo Archives"),
    SEX_EDUCATION("Inclusive Sexual Health Education"),
    CLINICAL_GUIDES("Clinical & Evidence-Based Medical"),
    PELVIC_AND_INCLUSION("Pelvic Floor & Gender-Inclusive Care"),
    BOOKS_MEDIA("Essential Reading & Research")
}

data class ExternalResource(
    val id: String,
    val title: String,
    val organization: String,
    val category: ResourceCategory,
    val description: String,
    val url: String,
    val badge: String
)
