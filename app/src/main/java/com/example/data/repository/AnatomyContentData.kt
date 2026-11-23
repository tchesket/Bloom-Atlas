package com.example.data.repository

import com.example.data.model.AnatomyPart
import com.example.data.model.AnatomyViewType
import com.example.data.model.CyclePhase
import com.example.data.model.MythCard

object AnatomyContentData {

    val anatomyParts: List<AnatomyPart> = listOf(
        // 1. EXTERNAL VULVA VIEW
        AnatomyPart(
            id = "ext_mons",
            number = 1,
            name = "Mons Pubis",
            clinicalName = "Mons veneris",
            viewType = AnatomyViewType.EXTERNAL_VULVA,
            normX = 0.50f,
            normY = 0.14f,
            sensitivityTier = com.example.data.model.SensitivityTier.MODERATE,
            summary = "The soft, rounded cushion of fatty tissue sitting over the pubic bone.",
            functionText = "Protects the pubic bone during everyday movement and intimacy. After puberty it develops hair follicles and scent glands that support a healthy skin barrier.",
            pleasureNotes = "Firm, broad pressure across the mons can stimulate the deep internal roots of the clitoris underneath—many people find gentle palm pressure deeply soothing or arousing.",
            mythBuster = "Myth: Pubic hair is unhygienic. Fact: Pubic hair actually reduces skin-on-skin friction, wicks away sweat, and helps buffer bacteria. Grooming is purely personal preference.",
            partnerTip = "Starting with warm, whole-hand contact over the mons builds relaxation and pelvic blood flow before moving to more sensitive areas.",
            relatedArticleId = "vulva_vs_vagina"
        ),
        AnatomyPart(
            id = "ext_hood",
            number = 2,
            name = "Clitoral Hood",
            clinicalName = "Prepuce of the clitoris",
            viewType = AnatomyViewType.EXTERNAL_VULVA,
            normX = 0.50f,
            normY = 0.27f,
            sensitivityTier = com.example.data.model.SensitivityTier.HIGH,
            summary = "A gentle fold of skin formed where the upper inner labia meet, draping over the clitoral glans.",
            functionText = "Shields the intensely nerve-rich clitoral glans from everyday friction against underwear and movement, while keeping the tissue soft and protected.",
            pleasureNotes = "Many people prefer stimulation *through* the hood rather than directly on the exposed glans, which can feel overly sharp or overwhelming without lubrication.",
            mythBuster = "Myth: The clitoral glans should always peek out. Fact: Hood coverage varies widely—some cover the glans completely, others sit open—and both are completely healthy.",
            partnerTip = "Use the hood as a built-in cushion: gentle circular gliding over the hood with plenty of lube is often much more enjoyable than hard direct rubbing.",
            relatedArticleId = "clitoral_network"
        ),
        AnatomyPart(
            id = "ext_glans",
            number = 3,
            name = "Clitoral Glans",
            clinicalName = "Glans clitoridis",
            viewType = AnatomyViewType.EXTERNAL_VULVA,
            normX = 0.50f,
            normY = 0.35f,
            sensitivityTier = com.example.data.model.SensitivityTier.ULTRA,
            summary = "The visible pearl-like tip of the clitoris—just the tip of a much larger internal organ.",
            functionText = "Packed with an estimated 8,000 to 10,000+ sensory nerve endings dedicated entirely to pleasure and sexual arousal signaling.",
            pleasureNotes = "Sensitivity shifts dramatically across the arousal cycle. Early on, indirect strokes feel best; as blood flow swells the tissue, pressure preferences often change—check in!",
            mythBuster = "Myth: The clitoris is tiny. Fact: The glans is just the surface peak; the full internal wishbone structure extends 9–11 cm deep inside the pelvis.",
            partnerTip = "Never assume faster or firmer is better. Consistency in rhythm, light-to-medium pressure, and asking 'right there or a little softer?' works wonders.",
            relatedArticleId = "clitoral_network"
        ),
        AnatomyPart(
            id = "ext_labia_majora",
            number = 4,
            name = "Outer Labia",
            clinicalName = "Labia majora",
            viewType = AnatomyViewType.EXTERNAL_VULVA,
            normX = 0.23f,
            normY = 0.48f,
            summary = "The outer cushiony folds that frame and cradle the rest of the vulva.",
            functionText = "Protect the delicate inner structures, maintain warmth and moisture balance, and contain sweat and oil glands that nourish the skin barrier.",
            pleasureNotes = "The outer labia contain nerve endings and engorge subtly during arousal, parting slightly as pelvic blood flow increases.",
            mythBuster = "Myth: Outer labia always hide the inner labia. Fact: In roughly half of all adults, the inner labia naturally extend beyond the outer labia.",
            partnerTip = "Gentle warming strokes along the inner thighs and outer labia help signal safety and awaken the entire pelvic nerve network.",
            relatedArticleId = "vulva_vs_vagina"
        ),
        AnatomyPart(
            id = "ext_labia_minora",
            number = 5,
            name = "Inner Labia",
            clinicalName = "Labia minora",
            viewType = AnatomyViewType.EXTERNAL_VULVA,
            normX = 0.68f,
            normY = 0.52f,
            sensitivityTier = com.example.data.model.SensitivityTier.HIGH,
            summary = "Delicate, hairless inner folds with extraordinary natural diversity in length, ruffle, symmetry, and color.",
            functionText = "Channel urine smoothly, cushion the vaginal entrance, and swell with blood during arousal to become darker, plush, and even more sensitive.",
            pleasureNotes = "Rich in nerve fibers and erectile tissue—gentle gliding or light tugging (when lubricated and welcomed) can feel deeply pleasurable.",
            mythBuster = "Myth: Longer or darker inner labia come from having more sex. Fact: Shape, length, symmetry, and color are determined by genetics and hormones—never sexual activity.",
            partnerTip = "Celebrate natural variation! Asymmetry and rich coloration are universal traits of real human bodies.",
            relatedArticleId = "vulva_vs_vagina"
        ),
        AnatomyPart(
            id = "ext_urethra",
            number = 6,
            name = "Urethral Opening",
            clinicalName = "External urethral meatus",
            viewType = AnatomyViewType.EXTERNAL_VULVA,
            normX = 0.50f,
            normY = 0.50f,
            summary = "The small opening situated between the clitoral glans and the vaginal entrance where urine exits.",
            functionText = "Connected to the bladder via the short urethra (~4 cm). Completely separate from the vaginal canal!",
            pleasureNotes = "Surrounding the urethra is the urethral sponge (Skene's glands), which swells during arousal and contributes to deep internal pleasure sensations.",
            mythBuster = "Myth: People with vaginas urinate from the vagina or can't pee with a tampon in. Fact: Urine exits from the separate urethral opening above the vagina.",
            partnerTip = "Good hygiene (clean hands, avoiding transferring bacteria from anal area forward) and peeing after sex help prevent UTIs.",
            relatedArticleId = "everyday_care"
        ),
        AnatomyPart(
            id = "ext_introitus",
            number = 7,
            name = "Vaginal Opening",
            clinicalName = "Vaginal introitus & vestibule",
            viewType = AnatomyViewType.EXTERNAL_VULVA,
            normX = 0.50f,
            normY = 0.68f,
            sensitivityTier = com.example.data.model.SensitivityTier.HIGH,
            summary = "The elastic gateway leading from the external vulva into the internal muscular vaginal canal.",
            functionText = "Allows menstrual flow to exit, stretches comfortably during arousal or childbirth, and is flanked by Bartholin's glands that release natural lubrication.",
            pleasureNotes = "The outer third of the vaginal opening is the most nerve-dense part of the canal—unhurried arousal lets the muscles relax and expand naturally.",
            mythBuster = "Myth: The hymen is a sealed barrier that 'breaks' once. Fact: The vaginal corona (hymen) is a stretchy fringe of tissue with an opening already present; it stretches and wears naturally over life.",
            partnerTip = "Never rush penetration. Giving 15–25 minutes of arousal allows the vaginal walls to lengthen ('tenting') and produce natural slip.",
            relatedArticleId = "arousal_pleasure"
        ),
        AnatomyPart(
            id = "ext_perineum",
            number = 8,
            name = "Perineum",
            clinicalName = "Perineal body",
            viewType = AnatomyViewType.EXTERNAL_VULVA,
            normX = 0.50f,
            normY = 0.85f,
            summary = "The smooth, resilient bridge of skin and deep muscle between the vaginal opening and the anus.",
            functionText = "Acts as a central structural anchor for the pelvic floor muscles, supporting the bladder, uterus, and bowel.",
            pleasureNotes = "Contains sensitive pudendal nerve branches; gentle supportive pressure can feel grounding for some people.",
            mythBuster = "Myth: Perineal tension only happens after birth. Fact: Stress, athletics, or clenching can make pelvic floor muscles overly tight at any age—relaxation matters as much as strength.",
            partnerTip = "Warmth, breathwork, and gentle pace help the entire pelvic floor soften during intimacy.",
            relatedArticleId = "pelvic_floor_guide"
        ),

        // 2. INTERNAL REPRODUCTIVE SYSTEM (FRONT VIEW)
        AnatomyPart(
            id = "int_uterus",
            number = 1,
            name = "Uterus",
            clinicalName = "Uterus (Fundus & Endometrium)",
            viewType = AnatomyViewType.INTERNAL_FRONT,
            normX = 0.50f,
            normY = 0.28f,
            summary = "A remarkably strong, pear-sized muscular organ tilted forward over the bladder.",
            functionText = "Each cycle, its inner lining (endometrium) thickens in response to estrogen and progesterone, then sheds as menstrual flow if pregnancy doesn't occur. During pregnancy, it expands exponentially.",
            pleasureNotes = "During orgasm, gentle rhythmic contractions of the uterus and pelvic floor create deep waves of physical release.",
            mythBuster = "Myth: Menstrual blood is 'dirty' or toxic waste. Fact: Period flow is simply nutrient-rich endometrial tissue, cervical mucus, and normal blood.",
            partnerTip = "Cramping happens because prostaglandins trigger uterine muscle contractions—heat packs, magnesium, rest, and patience genuinely help.",
            relatedArticleId = "menstrual_cycle_deep"
        ),
        AnatomyPart(
            id = "int_fallopian",
            number = 2,
            name = "Fallopian Tubes",
            clinicalName = "Uterine tubes & fimbriae",
            viewType = AnatomyViewType.INTERNAL_FRONT,
            normX = 0.25f,
            normY = 0.22f,
            summary = "Delicate pathways lined with microscopic hair-like cilia ending in finger-like sweeps (fimbriae).",
            functionText = "When an ovary releases an egg, the sweeping fimbriae gently guide it into the tube, where tiny cilia sweep it toward the uterus over several days.",
            pleasureNotes = "Not directly sensory during intimacy, though mild one-sided twinges around ovulation ('mittelschmerz') are a normal signal many people notice.",
            mythBuster = "Myth: Tubes are permanently fused onto the ovaries. Fact: The feathery fimbriae actually hover freely near the ovaries and gracefully sweep over them at ovulation.",
            partnerTip = "Understanding the fertile window (roughly 5–6 days per cycle because sperm can survive up to 5 days in fertile mucus) empowers shared contraception or conception planning.",
            relatedArticleId = "menstrual_cycle_deep"
        ),
        AnatomyPart(
            id = "int_ovaries",
            number = 3,
            name = "Ovaries",
            clinicalName = "Ovaria",
            viewType = AnatomyViewType.INTERNAL_FRONT,
            normX = 0.80f,
            normY = 0.34f,
            summary = "Two almond-sized endocrine powerhouses producing hormones and maturing eggs.",
            functionText = "Orchestrate estrogen, progesterone, and small amounts of testosterone—hormones that influence not only cycles, but bone health, brain mood, skin elasticity, and libido.",
            pleasureNotes = "The mid-cycle rise in estrogen and testosterone around ovulation often boosts energy, natural lubrication, and spontaneous desire.",
            mythBuster = "Myth: Ovaries strictly alternate left-right every month. Fact: Ovulation side is largely random from cycle to cycle!",
            partnerTip = "Hormonal rhythms affect energy, sleep, and desire naturally—learning a partner's cycle fosters deeper empathy.",
            relatedArticleId = "menstrual_cycle_deep"
        ),
        AnatomyPart(
            id = "int_cervix",
            number = 4,
            name = "Cervix",
            clinicalName = "Cervix uteri",
            viewType = AnatomyViewType.INTERNAL_FRONT,
            normX = 0.50f,
            normY = 0.50f,
            summary = "The firm, doughnut-shaped neck of the uterus extending into the upper end of the vagina.",
            functionText = "Acts as a smart gatekeeper: its microscopic opening stays tightly sealed most of the month, produces protective mucus that changes across the cycle, and opens slightly for menstrual flow or birth.",
            pleasureNotes = "Feels like the tip of a nose (or softer, like lips, near ovulation). Deep bumping can feel startling or painful if unaroused, while some people enjoy gentle deep sensation once fully aroused.",
            mythBuster = "Myth: A tampon or menstrual cup can get lost inside the uterus. Fact: The cervical opening is pinhole-sized—nothing can pass through it into the body!",
            partnerTip = "During arousal, the uterus lifts slightly ('tenting'), pulling the cervix higher so deep movement feels far more comfortable.",
            relatedArticleId = "microbiome_discharge"
        ),
        AnatomyPart(
            id = "int_vagina",
            number = 5,
            name = "Vaginal Canal",
            clinicalName = "Vagina (with rugae folds)",
            viewType = AnatomyViewType.INTERNAL_FRONT,
            normX = 0.50f,
            normY = 0.72f,
            summary = "A stretchy, accordion-folded muscular canal connecting the cervix to the external vulva.",
            functionText = "Normally resting with its walls softly touching (not an empty hollow tube), its pleated folds (rugae) expand easily during arousal or birth and rebound afterward. Beneficial Lactobacillus bacteria keep it self-cleaning at an acidic pH (3.8–4.5).",
            pleasureNotes = "Most touch-sensitive nerves are in the outer third, while deeper walls respond to pressure, fullness, and the internal clitoral legs hugging the canal.",
            mythBuster = "Myth: Frequent sex makes the vagina 'loose.' Fact: The vagina is resilient smooth muscle surrounded by pelvic floor muscles—it expands with arousal and returns to baseline.",
            partnerTip = "If dryness or tightness shows up, it usually means more warm-up time, relaxation, or lube is needed—never push through discomfort.",
            relatedArticleId = "microbiome_discharge"
        ),
        AnatomyPart(
            id = "int_pelvic_floor",
            number = 6,
            name = "Pelvic Floor Hammock",
            clinicalName = "Levator ani & pelvic diaphragm",
            viewType = AnatomyViewType.INTERNAL_FRONT,
            normX = 0.26f,
            normY = 0.80f,
            summary = "A layered sling of supportive muscles spanning the base of the pelvis from pubic bone to tailbone.",
            functionText = "Supports internal organs, controls bladder and bowel continence, stabilizes the core, and drives orgasmic contractions.",
            pleasureNotes = "A healthy pelvic floor can both contract *and* fully release. Learning to breathe down into the pelvis enhances comfort and orgasm intensity.",
            mythBuster = "Myth: Everyone needs endless Kegels. Fact: Many people actually have an overly tight (hypertonic) pelvic floor where lengthening and deep breathing help more than squeezing.",
            partnerTip = "Slow breathing together reduces pelvic guarding and makes intimacy significantly more comfortable.",
            relatedArticleId = "pelvic_floor_guide"
        ),

        // 3. CLITORAL NETWORK VIEW ("THE ICEBERG")
        AnatomyPart(
            id = "clit_glans",
            number = 1,
            name = "Glans & Shaft",
            clinicalName = "Glans & Corpus clitoridis",
            viewType = AnatomyViewType.CLITORAL_NETWORK,
            normX = 0.50f,
            normY = 0.21f,
            sensitivityTier = com.example.data.model.SensitivityTier.ULTRA,
            summary = "The surface tip (glans) connects directly to a dense erectile shaft curving inward beneath the pubic bone.",
            functionText = "Acts as a super-highway for pleasure signals via the dorsal nerve of the clitoris.",
            pleasureNotes = "Both external strokes and broad pressure over the pubic bone vibrate and stimulate the shaft.",
            mythBuster = "Myth: Only the surface 'button' matters. Fact: Modern 3D MRI research (pioneered by urologist Dr. Helen O'Connell) revealed the clitoris is a lush internal 3D wishbone organ!",
            partnerTip = "Think of external touch as lighting up an entire internal network—gentle, steady rhythm builds deep arousal.",
            relatedArticleId = "clitoral_network"
        ),
        AnatomyPart(
            id = "clit_crura",
            number = 2,
            name = "Clitoral Legs (Crura)",
            clinicalName = "Crura of the clitoris",
            viewType = AnatomyViewType.CLITORAL_NETWORK,
            normX = 0.23f,
            normY = 0.45f,
            sensitivityTier = com.example.data.model.SensitivityTier.HIGH,
            summary = "Two wing-like erectile legs extending up to 9 cm backward along the pubic arch.",
            functionText = "Fill with oxygenated blood during arousal, becoming firm and cushioning the surrounding pelvic walls.",
            pleasureNotes = "Grinding, straddling, thigh pressure, and curved internal angles stimulate the crura directly through the tissue.",
            mythBuster = "Myth: 'Vaginal' and 'clitoral' orgasms are totally separate organs. Fact: Internal orgasms largely stimulate the internal clitoral legs and bulbs from inside the vaginal wall!",
            partnerTip = "Positions that allow external contact or rhythmic pelvic pressure simultaneously activate the legs and glans.",
            relatedArticleId = "clitoral_network"
        ),
        AnatomyPart(
            id = "clit_bulbs",
            number = 3,
            name = "Vestibular Bulbs",
            clinicalName = "Bulbs of the vestibule",
            viewType = AnatomyViewType.CLITORAL_NETWORK,
            normX = 0.72f,
            normY = 0.58f,
            sensitivityTier = com.example.data.model.SensitivityTier.HIGH,
            summary = "Two spongy, teardrop-shaped erectile cushions embracing both sides of the urethra and vaginal entrance.",
            functionText = "Swell dramatically during arousal, gently narrowing the outer vaginal entrance to enhance sensation and produce a warm, full feeling.",
            pleasureNotes = "Often felt as a delicious deep ache of arousal; gentle pressure along the sides of the vulva or front vaginal wall stimulates these bulbs.",
            mythBuster = "Myth: Arousal only means wetness. Fact: Swelling of the vestibular bulbs is a major physical sign of arousal—and natural wetness varies wildly from person to person.",
            partnerTip = "Arousal takes time—bulbs typically reach peak engorgement after 15–20+ minutes of relaxed, pleasurable stimulation.",
            relatedArticleId = "arousal_pleasure"
        ),
        AnatomyPart(
            id = "clit_urethral_sponge",
            number = 4,
            name = "Urethral Sponge (G-Zone)",
            clinicalName = "Clitourethrovaginal complex",
            viewType = AnatomyViewType.CLITORAL_NETWORK,
            normX = 0.50f,
            normY = 0.65f,
            sensitivityTier = com.example.data.model.SensitivityTier.HIGH,
            summary = "The spongy, nerve-rich intersection where the front vaginal wall meets the urethra, Skene's glands, and internal clitoral roots.",
            functionText = "Swells into a ridged, textured cushion a few centimeters inside the front vaginal wall (toward the belly button) once aroused.",
            pleasureNotes = "Responds best to curved, 'come-hither' pressure *after* full arousal—it isn't a magic switch, but an interactive anatomical zone!",
            mythBuster = "Myth: Everyone has an identical G-spot that feels amazing right away. Fact: Sensation varies hugely; some feel intense pleasure, some feel pressure to pee at first, and some prefer external touch.",
            partnerTip = "Wait until arousal is well underway before exploring the front inner wall, and check in with gentle curiosity.",
            relatedArticleId = "clitoral_network"
        ),

        // 4. SIDE PELVIC CROSS-SECTION
        AnatomyPart(
            id = "side_bladder",
            number = 1,
            name = "Bladder & Urethra",
            clinicalName = "Vesica urinaria",
            viewType = AnatomyViewType.PELVIC_SIDE,
            normX = 0.30f,
            normY = 0.42f,
            summary = "Sits right behind the pubic bone, directly in front of the vagina and beneath the forward-tilted uterus.",
            functionText = "Stores urine and empties via the short urethra opening above the vagina.",
            pleasureNotes = "Because the bladder rests against the front vaginal wall, emptying your bladder before intimacy makes deep pressure much more comfortable.",
            mythBuster = "Myth: Feeling like you need to pee during front-wall stimulation always means a bladder problem. Fact: Pressure on the urethral sponge naturally mimics that sensation before shifting into pleasure.",
            partnerTip = "Encouraging a relaxed bathroom break before and after sex is normal, caring, and great for urinary health.",
            relatedArticleId = "everyday_care"
        ),
        AnatomyPart(
            id = "side_uterus",
            number = 2,
            name = "Anteverted Uterus",
            clinicalName = "Uterus (sagittal plane)",
            viewType = AnatomyViewType.PELVIC_SIDE,
            normX = 0.52f,
            normY = 0.28f,
            summary = "In most people, the uterus leans gently forward over the bladder; in ~20% it tilts backward (retroverted)—both are completely normal!",
            functionText = "Suspended by flexible ligaments that allow it to lift upward during arousal and expand during pregnancy.",
            pleasureNotes = "Uterine tilt can influence which intimate angles feel deeply comfortable versus too direct against the cervix.",
            mythBuster = "Myth: A tilted (retroverted) uterus is abnormal or causes infertility. Fact: It is simply a common normal anatomical variation, like being left-handed.",
            partnerTip = "Try placing a small pillow under the hips to change pelvic tilt—a tiny angle shift often transforms comfort.",
            relatedArticleId = "vulva_vs_vagina"
        ),
        AnatomyPart(
            id = "side_vagina_angle",
            number = 3,
            name = "Vaginal Angle & Fornices",
            clinicalName = "Vaginal axis & posterior fornix",
            viewType = AnatomyViewType.PELVIC_SIDE,
            normX = 0.48f,
            normY = 0.62f,
            summary = "The vaginal canal doesn't go straight up—it angles backward toward the lower spine (~45° when standing).",
            functionText = "At the top, curved pockets called fornices surround the cervix, creating extra room as the canal elongates during arousal.",
            pleasureNotes = "Understanding this natural backward angle makes inserting tampons, menstrual cups, or fingers far easier and smoother.",
            mythBuster = "Myth: The vaginal canal runs straight vertically toward the belly button. Fact: Aiming slightly toward the small of the back follows the body's true natural curve.",
            partnerTip = "Following the natural backward slope of the canal makes entry gentle and effortless.",
            relatedArticleId = "everyday_care"
        ),
        AnatomyPart(
            id = "side_rectum",
            number = 4,
            name = "Rectum & Rectovaginal Wall",
            clinicalName = "Rectum & posterior septum",
            viewType = AnatomyViewType.PELVIC_SIDE,
            normX = 0.74f,
            normY = 0.58f,
            summary = "Sits behind the back wall of the vagina, separated by a thin, flexible layer of fibrous tissue.",
            functionText = "Sharing nerve pathways and pelvic floor muscles with the vagina means bowel comfort, hydration, and fiber directly affect pelvic comfort.",
            pleasureNotes = "Some people enjoy indirect pressure along the back vaginal wall, while hygiene boundaries (never moving from anal to vaginal contact without washing or changing barriers) protect the vaginal microbiome.",
            mythBuster = "Myth: Pelvic organs operate in isolation. Fact: Bladder, reproductive organs, and bowel share the same pelvic floor muscles and nerve supply.",
            partnerTip = "Always wipe and touch front-to-back and switch barriers/wash hands between zones to prevent bacterial infections.",
            relatedArticleId = "pelvic_floor_guide"
        )
    )

    val cyclePhases: List<CyclePhase> = listOf(
        CyclePhase(
            id = "menstrual",
            name = "Menstrual Phase",
            daysRange = "Days 1–5 (typical)",
            hormoneSummary = "Estrogen and progesterone are at their lowest baseline, signaling the uterus to release its cushioned lining.",
            cervicalFluid = "Menstrual flow (bright red, ruby, or dark brown as blood oxidizes) plus tiny soft tissue clots—all normal unless soaking a pad/tampon every hour.",
            cervixFeel = "Sits lower in the canal, feels firmer, and opens very slightly to allow menstrual flow to pass smoothly.",
            energyAndLibido = "Energy often turns inward—rest and warmth feel restorative. Interestingly, pelvic blood flow and lower baseline tension make some people feel heightened arousal during their period!",
            careTip = "Warmth (heating pads), hydration, gentle stretching, and breathable period care (cups, discs, period underwear, or unscented pads) support comfort.",
            colorHex = 0xFFB54A42
        ),
        CyclePhase(
            id = "follicular",
            name = "Follicular Phase",
            daysRange = "Days 6–13 (typical)",
            hormoneSummary = "Follicle-stimulating hormone (FSH) wakes up ovarian follicles while rising estrogen rebuilds the uterine lining and lifts brain serotonin.",
            cervicalFluid = "Transitions from dry/minimal right after bleeding to creamy, lotion-like, milky white or soft ivory discharge.",
            cervixFeel = "Gradually rises higher and begins softening as estrogen climbs.",
            energyAndLibido = "Many people notice clearer focus, upbeat stamina, rising curiosity, and growing physical responsiveness.",
            careTip = "Great time to notice baseline discharge patterns—creamy white or pale yellow dry spots on underwear are healthy signs of a thriving acidic microbiome.",
            colorHex = 0xFF4D8069
        ),
        CyclePhase(
            id = "ovulation",
            name = "Ovulation Window",
            daysRange = "Around Days 14–16",
            hormoneSummary = "Estrogen peaks, triggering a surge of luteinizing hormone (LH) that releases a mature egg, accompanied by a brief gentle peak in testosterone.",
            cervicalFluid = "Clear, stretchy, slippery 'egg-white' cervical mucus—specifically engineered by the body to nourish and guide sperm smoothly.",
            cervixFeel = "High, soft (like lips), and slightly open ('SHOW': Soft, High, Open, Wet).",
            energyAndLibido = "Often correlates with peak spontaneous desire, abundant natural lubrication, and vibrant social energy. Note: Desire is complex and never strictly dictated by hormones!",
            careTip = "Some notice a brief mild twinge on one side of the lower belly (mittelschmerz). Remember: tracking symptoms alone is informative, not standalone contraception without validated fertility awareness training.",
            colorHex = 0xFFC88B3A
        ),
        CyclePhase(
            id = "luteal",
            name = "Luteal Phase",
            daysRange = "Days 17–28 (typical)",
            hormoneSummary = "After ovulation, the follicle becomes the corpus luteum and releases calming progesterone. If no pregnancy starts, both hormones dip before the next bleed.",
            cervicalFluid = "Thickens into sticky, tacky, or opaque white discharge as progesterone closes the fertile window.",
            cervixFeel = "Lowers back down and feels firmer again, like the tip of a nose.",
            energyAndLibido = "Early luteal feels grounded and steady; late luteal can bring breast tenderness, bloating, or emotional sensitivity (PMS/PMDD if severe—always worth compassionate medical support).",
            careTip = "Prioritize sleep, complex carbs, gentle movement, and extra lubrication if vaginal tissue feels slightly drier in the days right before bleeding.",
            colorHex = 0xFF7A4465
        )
    )

    val mythCards: List<MythCard> = listOf(
        MythCard(
            id = "myth_1",
            mythStatement = "You need special scented washes or douches to keep the vagina clean and fresh.",
            isTrue = false,
            verdictTitle = "Myth — The Vagina Is Self-Cleaning!",
            explanation = "Beneficial Lactobacillus bacteria keep the vagina naturally acidic (pH 3.8–4.5), clearing away old cells and microbes via normal discharge. Douches and scented soaps strip those good bacteria and actually *cause* odor and infections like BV or yeast.",
            partnerAngle = "Healthy vulvas have a mild, natural, musky or slightly tangy scent—not floral perfume. Reassuring normalcy removes unnecessary shame."
        ),
        MythCard(
            id = "myth_2",
            mythStatement = "Inner labia that extend past the outer labia are just as common as tucked inner labia.",
            isTrue = true,
            verdictTitle = "Fact — Over 50% Have Visible Inner Labia",
            explanation = "Anatomical research confirms that prominent inner labia and left-right asymmetry are completely normal and actually occur in at least half of all people with vulvas.",
            partnerAngle = "Media rarely shows natural diversity. Celebrating a partner's real anatomy builds deep confidence and trust."
        ),
        MythCard(
            id = "myth_3",
            mythStatement = "Having more sexual partners stretches the vagina permanently.",
            isTrue = false,
            verdictTitle = "Myth — Vaginal Tissue Is Resilient Elastic Muscle",
            explanation = "The vagina expands with arousal to welcome comfort and returns to its resting shape afterward. Even after vaginal childbirth, pelvic floor muscles recover remarkably—sexual frequency never causes permanent 'looseness.'",
            partnerAngle = "When the vagina feels tighter, it often means someone is tense or not yet fully aroused; relaxation and fullness are signs of comfort and arousal!"
        ),
        MythCard(
            id = "myth_4",
            mythStatement = "Most people with vulvas need direct or indirect clitoral stimulation to orgasm.",
            isTrue = true,
            verdictTitle = "Fact — Around 75–85% Require Clitoral Touch",
            explanation = "Studies consistently show the vast majority of people with vulvas do not orgasm from internal penetration alone without external or simultaneous clitoral stimulation. That's completely normal anatomy, not a 'problem'!",
            partnerAngle = "Integrating fingers, oral stimulation, grinding, or a vibrator during intimacy isn't 'extra'—for most bodies, it's the main event."
        ),
        MythCard(
            id = "myth_5",
            mythStatement = "Light bleached or pale spots in dark underwear mean you have an infection.",
            isTrue = false,
            verdictTitle = "Myth — That's Proof of a Healthy Acidic pH!",
            explanation = "Normal healthy vaginal discharge has an acidic pH (around 3.8–4.5) that gently lightens dark cotton fabric over time. Unless paired with itching, strong fishy odor, or pain, it's a sign your ecosystem is thriving.",
            partnerAngle = "Educators and partners knowing this tiny everyday fact helps eliminate years of quiet worry."
        ),
        MythCard(
            id = "myth_6",
            mythStatement = "The clitoris extends 9–11 cm inside the body around the urethra and vagina.",
            isTrue = true,
            verdictTitle = "Fact — We Only See About 10% on the Surface",
            explanation = "The visible glans connects to an internal shaft, two crura (legs), and two vestibular bulbs that swell with blood during arousal—making it a substantial, deeply integrated pelvic organ.",
            partnerAngle = "Knowing the clitoris wraps around the vagina explains why broad pressure, angles, and unhurried arousal feel so rich."
        ),
        MythCard(
            id = "myth_7",
            mythStatement = "The hymen completely seals the vagina until first penetrative sex, and always bleeds.",
            isTrue = false,
            verdictTitle = "Myth — The Vaginal Corona Is Stretchy & Open",
            explanation = "If the hymen were completely sealed, menstrual blood couldn't exit! It is a thin, stretchy rim of tissue that naturally stretches with tampons, sports, or arousal. First-time sex does not have to hurt or bleed when aroused and lubricated.",
            partnerAngle = "Pain during sex is never something to 'push through'—slow down, add lubricant, and prioritize comfort."
        ),
        MythCard(
            id = "myth_8",
            mythStatement = "You cannot pee while wearing a tampon.",
            isTrue = false,
            verdictTitle = "Myth — Separate Openings!",
            explanation = "Urine exits through the urethral opening located *above* the vaginal opening where a tampon sits. Just tuck the string gently aside if you prefer keeping it dry.",
            partnerAngle = "Even many adults weren't taught there are three separate openings (urethra, vagina, anus)—interactive diagrams clear this up instantly."
        ),
        MythCard(
            id = "myth_9",
            mythStatement = "Lubricant is only for older adults or people who aren't turned on.",
            isTrue = false,
            verdictTitle = "Myth — Lube Enhances Comfort at Every Age",
            explanation = "Natural wetness fluctuates with hormones, medications (like antihistamines or birth control), stress, hydration, and cycle phase—even when someone is deeply aroused! Water- or silicone-based lube makes touch smoother for everyone.",
            partnerAngle = "Treating lube as a fun, standard pleasure enhancer removes pressure and makes intimacy dramatically better."
        ),
        MythCard(
            id = "myth_10",
            mythStatement = "Urinating shortly after partnered sex helps reduce the risk of UTIs.",
            isTrue = true,
            verdictTitle = "Fact — Flushing the Short Urethra Helps",
            explanation = "Because the female urethra is relatively short (~4 cm), friction during sex can nudge skin bacteria near its entrance. Peeing gently afterward helps flush bacteria out before they reach the bladder.",
            partnerAngle = "Clean hands, trimmed nails, and keeping a cozy post-sex water glass nearby are thoughtful, caring habits."
        ),
        MythCard(
            id = "myth_11",
            mythStatement = "Responsive desire (feeling turned on *after* starting affectionate touch) is just as normal as spontaneous desire.",
            isTrue = true,
            verdictTitle = "Fact — Context & Connection Drive Desire",
            explanation = "Researcher Dr. Emily Nagoski popularized the dual-control model: many people rarely feel out-of-the-blue spontaneous horniness, yet respond warmly once stress lowers and enjoyable touch begins.",
            partnerAngle = "Low spontaneous desire doesn't mean lack of attraction—focus on lowering stress ('taking off the brakes') and unpressured affection."
        ),
        MythCard(
            id = "myth_12",
            mythStatement = "Everyone with a vulva or vagina identifies as a woman.",
            isTrue = false,
            verdictTitle = "Myth — Anatomy Doesn't Define Gender Identity",
            explanation = "Cisgender women, transgender men, non-binary people, and some intersex people have vulvas and vaginas. Inclusive anatomical language ensures everyone receives respectful, accurate health education.",
            partnerAngle = "Always mirror the words a partner feels most comfortable using for their own body."
        )
    )
}
