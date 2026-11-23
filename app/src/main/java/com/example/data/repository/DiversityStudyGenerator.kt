package com.example.data.repository

import androidx.compose.ui.graphics.Color
import com.example.data.model.DiversityTrait
import com.example.data.model.NaturalTonePalette
import com.example.data.model.VulvaStudy
import kotlin.math.abs
import kotlin.random.Random

object DiversityStudyGenerator {

    val naturalPalettes: List<NaturalTonePalette> = listOf(
        NaturalTonePalette(
            name = "Warm Porcelain & Petal",
            outerBase = Color(0xFFF3D7C8),
            innerBase = Color(0xFFDFA097),
            accentLine = Color(0xFF7D453E),
            cardTint = Color(0xFFFBF3EE),
            shadeNote = "Soft contrast between outer skin and rosy inner mucosal tissue."
        ),
        NaturalTonePalette(
            name = "Golden Honey & Terracotta",
            outerBase = Color(0xFFDFB089),
            innerBase = Color(0xFFC07663),
            accentLine = Color(0xFF6B392C),
            cardTint = Color(0xFFFAF1EA),
            shadeNote = "Warm golden undertones with deeper terracotta pigment along inner folds."
        ),
        NaturalTonePalette(
            name = "Rich Sienna & Plum",
            outerBase = Color(0xFFB37253),
            innerBase = Color(0xFF8C4C4B),
            accentLine = Color(0xFF4B2524),
            cardTint = Color(0xFFF7ECE8),
            shadeNote = "Natural melanin variation often creates deeper rose-plum borders."
        ),
        NaturalTonePalette(
            name = "Deep Espresso & Berry",
            outerBase = Color(0xFF7A4A36),
            innerBase = Color(0xFF643435),
            accentLine = Color(0xFF341818),
            cardTint = Color(0xFFF4EAE6),
            shadeNote = "Rich mahogany tones with velvety plum-berry inner shading."
        ),
        NaturalTonePalette(
            name = "Olive Sand & Dusty Rose",
            outerBase = Color(0xFFD5B195),
            innerBase = Color(0xFFB57A74),
            accentLine = Color(0xFF5F3B37),
            cardTint = Color(0xFFF9F2EC),
            shadeNote = "Neutral olive outer contours paired with muted dusty-rose inner folds."
        ),
        NaturalTonePalette(
            name = "Warm Amber & Fig",
            outerBase = Color(0xFFC58C64),
            innerBase = Color(0xFF9A5B57),
            accentLine = Color(0xFF532B28),
            cardTint = Color(0xFFF9EFE9),
            shadeNote = "Natural darkening along labia edges is common after puberty and pregnancy."
        ),
        NaturalTonePalette(
            name = "Chestnut & Deep Rose",
            outerBase = Color(0xFF955E44),
            innerBase = Color(0xFF7D4446),
            accentLine = Color(0xFF422021),
            cardTint = Color(0xFFF6ECE8),
            shadeNote = "Smooth natural gradient where outer skin meets inner mucosal folds."
        ),
        NaturalTonePalette(
            name = "Soft Peach & Coral Clay",
            outerBase = Color(0xFFEDC6B1),
            innerBase = Color(0xFFD38C80),
            accentLine = Color(0xFF743F37),
            cardTint = Color(0xFFFDF4EE),
            shadeNote = "Blood flow during arousal temporarily deepens inner color by several shades."
        ),
        NaturalTonePalette(
            name = "Deep Umber & Velvet Wine",
            outerBase = Color(0xFF663B2A),
            innerBase = Color(0xFF582A30),
            accentLine = Color(0xFF2B1215),
            cardTint = Color(0xFFF2E8E5),
            shadeNote = "High melanin density creates striking tonal harmony and soft highlights."
        ),
        NaturalTonePalette(
            name = "Toasted Almond & Mauve",
            outerBase = Color(0xFFCBA082),
            innerBase = Color(0xFFA66B6B),
            accentLine = Color(0xFF5B3333),
            cardTint = Color(0xFFFAF1EB),
            shadeNote = "Mauve and brownish-pink edges are among the most common natural hues."
        )
    )

    private val captionsByTrait: Map<DiversityTrait, List<String>> = mapOf(
        DiversityTrait.ASYMMETRY to listOf(
            "Over half of all people have one inner labium noticeably longer, wider, or differently shaped than the other.",
            "Just like faces, hands, and breasts, vulvas rarely mirror perfectly from left to right.",
            "Asymmetry develops naturally during puberty as each side responds independently to hormones.",
            "One fold may tuck neatly inside while the other drapes beyond the outer labia—both are completely healthy."
        ),
        DiversityTrait.INNER_PROMINENT to listOf(
            "Clinical studies show that inner labia extending past outer labia occur in roughly 50% or more of adults.",
            "Prominent inner folds naturally cushion and protect the delicate vaginal and urethral openings.",
            "Inner labia lengths measured in medical studies span from under 1 cm to over 5 cm—all within normal range.",
            "Ruffled, scalloped, or petal-like edges add surface elasticity and sensory nerve endings."
        ),
        DiversityTrait.INNER_TUCKED to listOf(
            "Some vulvas have inner labia that nestle completely inside plush or closely meeting outer labia.",
            "Whether tucked or visible, the inner labia still swell with blood flow during arousal.",
            "Full outer labia naturally soften impact during cycling, sitting, and everyday movement.",
            "Smooth, compact inner contours are one point on a wide anatomical continuum—never a single standard."
        ),
        DiversityTrait.HOOD_VARIATION to listOf(
            "The clitoral hood (prepuce) can drape fully over the glans, frame it like a crescent, or sit higher up.",
            "A fuller hood protects the ultra-sensitive glans—home to thousands of nerve endings—from clothing friction.",
            "During arousal, engorgement gently shifts the hood, changing how direct or indirect touch feels best.",
            "Some hoods form layered folds that merge directly into the upper inner labia."
        ),
        DiversityTrait.TONE_SPECTRUM to listOf(
            "Vulval skin is frequently 1 to 4 shades deeper than the surrounding thighs—a completely normal effect of estrogen and melanin.",
            "Two-tone edges, freckling, and gentle gradients from outer skin to inner tissue are ubiquitous in nature.",
            "Skin color shifts across a lifetime due to puberty, pregnancy hormones, and natural blood flow changes.",
            "Media airbrushing often erases natural pigment transitions, creating false expectations that real anatomy debunks."
        ),
        DiversityTrait.LIFE_STAGES to listOf(
            "Puberty transforms soft childhood tissue into fuller contours with hair follicles, sweat glands, and deeper pigment.",
            "Pregnancy increases pelvic blood flow, often temporarily deepening color and softening connective tissue.",
            "During perimenopause and menopause, lower estrogen can make folds slightly thinner—gentle moisturizers help comfort.",
            "Bodies evolve across every decade; healthy vulvas reflect our lived stories, movement, and age."
        )
    )

    private val anatomicalNotes = listOf(
        "The labia minora contain rich vascular erectile tissue that can swell 2–3x during arousal.",
        "Sebaceous glands (Fordyce spots) appear as tiny harmless pale bumps—they keep skin supple.",
        "Pubic hair varies from coarse curls to fine wisps, naturally wicking moisture and reducing friction.",
        "Bartholin’s glands sit quietly near the lower vaginal opening, releasing lubricating fluid when aroused.",
        "Every vulva sits at a slightly different angle along the pelvis, affecting which positions feel most comfortable.",
        "No medical test can determine sexual history from vulval shape, length, or color—that is pure myth."
    )

    private val educatorPrompts = listOf(
        "Partner tip: Never compare a partner's body to curated media—real diversity is far richer and more varied.",
        "Educator note: Emphasize that 'average' does not mean 'ideal'—healthy anatomy covers an enormous range.",
        "Self-care reflection: Exploring with a handheld mirror in neutral light builds lasting body confidence.",
        "Partner tip: Ask how direct or indirect touch feels around the clitoral hood, since hood coverage changes sensitivity."
    )

    fun generateBatch(
        startNumber: Int,
        count: Int,
        filter: DiversityTrait
    ): List<VulvaStudy> {
        val result = mutableListOf<VulvaStudy>()
        var currentNum = startNumber
        var attempts = 0
        while (result.size < count && attempts < count * 8) {
            val study = generateSingle(currentNum, forcedFilter = filter)
            if (filter == DiversityTrait.ALL || study.primaryTrait == filter) {
                result.add(study)
            }
            currentNum++
            attempts++
        }
        return result
    }

    fun generateSingle(studyNumber: Int, forcedFilter: DiversityTrait = DiversityTrait.ALL): VulvaStudy {
        val seed = studyNumber * 7919 + 104729
        val rng = Random(seed)

        val selectableTraits = listOf(
            DiversityTrait.ASYMMETRY,
            DiversityTrait.INNER_PROMINENT,
            DiversityTrait.INNER_TUCKED,
            DiversityTrait.HOOD_VARIATION,
            DiversityTrait.TONE_SPECTRUM,
            DiversityTrait.LIFE_STAGES
        )
        val primaryTrait = if (forcedFilter != DiversityTrait.ALL) {
            forcedFilter
        } else {
            selectableTraits[abs(seed) % selectableTraits.size]
        }

        var leftProj = 0.25f + rng.nextFloat() * 0.65f
        var rightProj = 0.25f + rng.nextFloat() * 0.65f
        var outerFull = 0.35f + rng.nextFloat() * 0.55f
        var hoodCov = 0.25f + rng.nextFloat() * 0.65f
        var ruffle = rng.nextFloat()
        val vertLen = 0.78f + rng.nextFloat() * 0.20f
        val paletteIndex = abs(rng.nextInt()) % naturalPalettes.size

        when (primaryTrait) {
            DiversityTrait.ASYMMETRY -> {
                if (rng.nextBoolean()) {
                    leftProj = 0.68f + rng.nextFloat() * 0.26f
                    rightProj = 0.18f + rng.nextFloat() * 0.25f
                } else {
                    leftProj = 0.18f + rng.nextFloat() * 0.25f
                    rightProj = 0.68f + rng.nextFloat() * 0.26f
                }
            }
            DiversityTrait.INNER_PROMINENT -> {
                leftProj = 0.66f + rng.nextFloat() * 0.28f
                rightProj = 0.62f + rng.nextFloat() * 0.30f
                ruffle = 0.45f + rng.nextFloat() * 0.55f
            }
            DiversityTrait.INNER_TUCKED -> {
                leftProj = 0.14f + rng.nextFloat() * 0.22f
                rightProj = 0.14f + rng.nextFloat() * 0.22f
                outerFull = 0.62f + rng.nextFloat() * 0.28f
            }
            DiversityTrait.HOOD_VARIATION -> {
                hoodCov = if (rng.nextBoolean()) 0.72f + rng.nextFloat() * 0.22f else 0.18f + rng.nextFloat() * 0.18f
            }
            DiversityTrait.TONE_SPECTRUM -> {
                // Spread evenly across all 10 diverse palettes
            }
            DiversityTrait.LIFE_STAGES -> {
                outerFull = 0.32f + rng.nextFloat() * 0.55f
            }
            DiversityTrait.ALL -> Unit
        }

        val palette = naturalPalettes[paletteIndex]
        val tags = buildList {
            val diff = abs(leftProj - rightProj)
            if (diff > 0.22f) add("Asymmetrical") else add("Balanced Contour")
            val avgInner = (leftProj + rightProj) / 2f
            when {
                avgInner > 0.62f -> add("Prominent Inner")
                avgInner < 0.34f -> add("Tucked Inner")
                else -> add("Mid-Length Inner")
            }
            if (ruffle > 0.6f) add("Scalloped Edge") else if (ruffle < 0.28f) add("Smooth Edge")
            if (hoodCov > 0.68f) add("Full Hood") else if (hoodCov < 0.34f) add("Open Hood")
            add(palette.name.substringBefore(" &"))
        }.take(3)

        val traitCaptions = captionsByTrait[primaryTrait] ?: captionsByTrait[DiversityTrait.ASYMMETRY]!!
        val caption = traitCaptions[abs(seed / 3) % traitCaptions.size]
        val note = anatomicalNotes[abs(seed / 7) % anatomicalNotes.size]
        val prompt = educatorPrompts[abs(seed / 11) % educatorPrompts.size]

        val titleDescriptors = listOf(
            "Natural Asymmetry", "Petal Contour", "Soft Crescent", "Organic Harmony",
            "Sculpted Fold", "Botanical Silhouette", "Balanced Form", "Gentle Cascade",
            "Warm Horizon", "Living Variation", "Velvet Ridge", "Unique Signature"
        )
        val descriptor = titleDescriptors[abs(seed / 5) % titleDescriptors.size]

        return VulvaStudy(
            studyNumber = studyNumber,
            seed = seed,
            title = "Study #%03d · %s".format(studyNumber, descriptor),
            primaryTrait = primaryTrait,
            tags = tags,
            innerLeftProjection = leftProj.coerceIn(0.12f, 0.95f),
            innerRightProjection = rightProj.coerceIn(0.12f, 0.95f),
            outerFullness = outerFull.coerceIn(0.25f, 0.92f),
            hoodCoverage = hoodCov.coerceIn(0.18f, 0.92f),
            ruffleFactor = ruffle.coerceIn(0f, 1f),
            verticalLength = vertLen.coerceIn(0.72f, 1.0f),
            paletteIndex = paletteIndex,
            caption = caption,
            anatomicalNote = note,
            educatorPrompt = prompt
        )
    }
}
