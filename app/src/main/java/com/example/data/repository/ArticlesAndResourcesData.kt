package com.example.data.repository

import com.example.data.model.ArticleCategory
import com.example.data.model.ArticleSection
import com.example.data.model.ExternalResource
import com.example.data.model.GlossaryTerm
import com.example.data.model.GuideArticle
import com.example.data.model.ResourceCategory

object ArticlesAndResourcesData {

    val articles: List<GuideArticle> = listOf(
        GuideArticle(
            id = "vulva_vs_vagina",
            title = "Vulva vs. Vagina: Naming Every Part Clearly",
            subtitle = "Why clear words build confidence, better health care, and stronger communication.",
            category = ArticleCategory.ANATOMY,
            readMinutes = 4,
            summary = "In everyday speech, people often say 'vagina' to mean everything down there—but knowing the difference between the external vulva and the internal vagina unlocks clarity for health and pleasure.",
            sections = listOf(
                ArticleSection(
                    heading = "The Vulva Is Everything on the Outside",
                    body = "When you look with a mirror, you are looking at the vulva: the mons pubis, outer labia (labia majora), inner labia (labia minora), clitoral hood, clitoral glans, urethral opening, and the entrance to the vagina. Just like faces, no two vulvas look alike—symmetry, color gradients, hair patterns, and fold lengths vary tremendously."
                ),
                ArticleSection(
                    heading = "The Vagina Is the Internal Muscular Canal",
                    body = "The vagina sits entirely inside the body, stretching roughly 7–10 cm at rest and lengthening further during arousal. Its walls are lined with pleated rugae folds that expand comfortably and rebound. It connects the external vulva to the cervix at the base of the uterus."
                ),
                ArticleSection(
                    heading = "Why Words Matter",
                    body = "Using accurate names isn't about being stiff or clinical—it lets you tell a doctor exactly where an irritation sits (vulvar skin rash vs. internal vaginal discharge) and lets you tell a partner what feels amazing without awkward guesswork."
                )
            ),
            partnerTakeaway = "Using clear, comfortable words without giggling or euphemisms shows maturity and makes conversations about pleasure and health effortless.",
            educatorNote = "Teaching accurate anatomical names from an early age correlates strongly with stronger bodily autonomy, safety, and health literacy.",
            relatedResourceIds = listOf("res_labia_library", "res_vulva_gallery", "res_scarleteen")
        ),
        GuideArticle(
            id = "clitoral_network",
            title = "The Iceberg Below: Understanding the Full Clitoris",
            subtitle = "Meet the 10-centimeter erectile organ dedicated solely to pleasure.",
            category = ArticleCategory.ANATOMY,
            readMinutes = 5,
            summary = "For centuries, medical textbooks left out the internal anatomy of the clitoris. Today we know it is an extraordinary, deeply branched network wrapping around the urethra and vaginal canal.",
            sections = listOf(
                ArticleSection(
                    heading = "Beyond the Pearl",
                    body = "The external glans clitoridis is home to thousands of ultra-responsive nerve endings, protected by the soft clitoral hood. Beneath the skin, the clitoral shaft bends inward and splits into two curved legs (crura) anchored to the pelvic bone, plus two plush vestibular bulbs embracing both sides of the vaginal opening."
                ),
                ArticleSection(
                    heading = "Why Arousal Takes Time",
                    body = "Just like a penis, the clitoris is made of spongy erectile tissue that swells with blood during arousal. Because the internal network is expansive, full engorgement often takes 15 to 25 minutes of relaxed, enjoyable stimulation. Rushing straight to intense direct contact skips this crucial warm-up."
                ),
                ArticleSection(
                    heading = "Internal & External Pleasure Connected",
                    body = "What people call the 'G-spot' or anterior vaginal wall sensitivity isn't a separate mystery button—it is the rich clitourethrovaginal (CUV) complex where the front vaginal wall meets the swollen urethral sponge and internal clitoral roots. Combining external clitoral touch with internal warmth activates the whole organ at once."
                )
            ),
            partnerTakeaway = "Ask whether your partner prefers circular, side-to-side, up-and-down, or pulsing pressure, and whether touch feels better through the hood or with extra lube.",
            educatorNote = "Highlight Dr. Helen O'Connell's landmark 1998–2005 MRI research showing the true 3D scale of the clitoris—many textbooks only updated their diagrams recently!",
            relatedResourceIds = listOf("res_omgyes", "res_cliteracy", "res_book_cliterate")
        ),
        GuideArticle(
            id = "microbiome_discharge",
            title = "The Self-Cleaning Ecosystem: pH, Microbiome & Discharge",
            subtitle = "How healthy bacteria protect the vagina—and how to decode everyday discharge.",
            category = ArticleCategory.CYCLES_MICROBIOME,
            readMinutes = 5,
            summary = "Far from needing artificial fragrances or washes, the vagina maintains one of the body's smartest ecosystems powered by protective Lactobacillus bacteria.",
            sections = listOf(
                ArticleSection(
                    heading = "Lactobacilli & Acidic Balance",
                    body = "Estrogen encourages vaginal cells to produce glycogen, which feeds friendly Lactobacillus bacteria. Those bacteria produce lactic acid, keeping vaginal pH between 3.8 and 4.5—acidic enough to crowd out harmful microbes naturally. Even faint lightened patches in dark cotton underwear are simply a badge of healthy acidity!"
                ),
                ArticleSection(
                    heading = "Reading Your Daily Discharge",
                    body = "Clear and stretchy (like raw egg white) signals rising estrogen near ovulation. Creamy white or pale ivory is typical in the follicular or luteal phases. Thin, watery fluid often appears after exercise or arousal. Thick white clumpy discharge with itching may point to yeast, while thin gray-white fluid with a strong fishy odor after sex or periods often signals Bacterial Vaginosis (BV)—both are super common and easily treatable."
                ),
                ArticleSection(
                    heading = "Keep It Simple: No Douching",
                    body = "Never wash *inside* the vagina with soap, douches, wipes, or steaming. For the external vulva, warm water alone (or a very mild, fragrance-free pH-balanced cleanser on outer hair-bearing folds only) is everything your body needs."
                )
            ),
            partnerTakeaway = "Semen is slightly alkaline (pH ~7.5), which can temporarily nudge vaginal pH upward after barrier-free sex—using condoms or rinsing gently externally can help partners prone to BV.",
            educatorNote = "Normalizing everyday discharge relieves enormous teen and adult anxiety; clarify the difference between healthy scent and signs of infection.",
            relatedResourceIds = listOf("res_acog", "res_book_vagina_bible")
        ),
        GuideArticle(
            id = "menstrual_cycle_deep",
            title = "Beyond the Period: The Whole Menstrual Cycle",
            subtitle = "Hormones, cervical shifts, energy waves, and menstrual care options explained.",
            category = ArticleCategory.CYCLES_MICROBIOME,
            readMinutes = 5,
            summary = "A period is just one chapter of a continuous 21-to-35-day hormonal rhythm that touches sleep, mood, cervical mucus, skin, and sexual energy.",
            sections = listOf(
                ArticleSection(
                    heading = "Four Distinct Chapters",
                    body = "Cycle Day 1 begins on the first day of full menstrual flow. As bleeding winds down, estrogen rises through the follicular phase until an egg releases at ovulation. Afterward, progesterone dominates the luteal phase, warming resting body temperature slightly until hormones dip again."
                ),
                ArticleSection(
                    heading = "Modern Period Care Choices",
                    body = "Beyond disposable pads and tampons, reusable menstrual cups (sitting lower in the canal with a gentle seal), menstrual discs (tucked behind the pubic bone—even allowing mess-free intimacy!), and breathable period underwear offer eco-friendly, comfortable options for every flow."
                ),
                ArticleSection(
                    heading = "When Period Pain Isn't Just 'Normal'",
                    body = "Mild cramping that eases with heat or an anti-inflammatory is common, but pain that disrupts school, work, or daily life deserves medical care. Conditions like endometriosis, adenomyosis, PCOS, and fibroids are common, real, and treatable—advocate for thorough listening!"
                )
            ),
            partnerTakeaway = "Checking in with simple kindness ('Want a heat pack or quiet night in?') and never dismissing emotions as 'just hormonal' builds real partnership.",
            educatorNote = "Emphasize that cycle lengths between 21 and 35 days (and up to 45 days in the first years after menarche) can be healthy—28 days is only a textbook average.",
            relatedResourceIds = listOf("res_acog", "res_planned_parenthood")
        ),
        GuideArticle(
            id = "arousal_pleasure",
            title = "Pleasure, Arousal & Lubrication Science",
            subtitle = "Accelerators, brakes, lube chemistry, and letting go of performance myths.",
            category = ArticleCategory.PLEASURE_INTIMACY,
            readMinutes = 6,
            summary = "Great sexual pleasure isn't a checklist or a race to an orgasm—it's a responsive, sensory conversation between the nervous system, blood flow, and emotional safety.",
            sections = listOf(
                ArticleSection(
                    heading = "Accelerators & Brakes (The Dual-Control Model)",
                    body = "Every brain has a Sexual Excitation System ('the gas pedal') and a Sexual Inhibition System ('the brakes'). Stress, body self-consciousness, fatigue, or rushed pressure slam on the brakes even when you love your partner. Often the most powerful path to arousal isn't hitting the gas harder—it's gently lifting the brakes!"
                ),
                ArticleSection(
                    heading = "Choosing the Right Lubricant",
                    body = "Water-based lubes are safe with condoms and silicone toys (look for glycerin-free, fragrance-free formulas if you're prone to irritation). Silicone-based lubes last extra long and work in the shower, though they shouldn't be used with silicone toys. Oil-based lubes (like coconut oil) feel lush for massage or solo play but weaken latex condoms—never pair oil with latex."
                ),
                ArticleSection(
                    heading = "Solo Exploration Builds Body Literacy",
                    body = "Learning what rhythms, textures, breathing, and pressure feel good on your own removes pressure during partnered intimacy. Every body has its own map—and that map can evolve over time."
                )
            ),
            partnerTakeaway = "Treat orgasm as a wonderful possibility, never a scorecard or ego test. Curious feedback ('slower', 'lighter', 'keep doing exactly that') makes pleasure collaborative.",
            educatorNote = "Distinguishing arousal non-concordance (physical wetness doesn't automatically mean mental desire, and dryness doesn't mean lack of desire) is transformative for learners.",
            relatedResourceIds = listOf("res_omgyes", "res_book_come_as_you_are")
        ),
        GuideArticle(
            id = "everyday_care",
            title = "Everyday Vulva & Vaginal Care Without Shame",
            subtitle = "Practical habits for comfort, UTI prevention, STI testing, and confident clinic visits.",
            category = ArticleCategory.HEALTH_CARE,
            readMinutes = 5,
            summary = "Caring for vulvar and vaginal health is refreshingly low-maintenance once you strip away marketing hype and stigma.",
            sections = listOf(
                ArticleSection(
                    heading = "Simple Daily Comfort Habits",
                    body = "Wear breathable cotton underwear (or sleep commando to let skin breathe), change promptly out of damp swimsuits or workout tights, wash externally with warm water, and wipe front-to-back. If external vulvar skin feels dry from cycling, shaving, or winter air, plain unscented ointment or balm on outer skin can soothe friction."
                ),
                ArticleSection(
                    heading = "Stigma-Free STI Testing & Safer Sex",
                    body = "Regular STI testing is basic preventive health care—just like a dental checkup. Many common STIs (like chlamydia or HPV) cause zero symptoms at first. Condoms, internal condoms, dental dams, HPV vaccination, and open partner conversations keep intimacy joyful and safe."
                ),
                ArticleSection(
                    heading = "Advocating at Gynecological Visits",
                    body = "You are always in charge of your body during a pelvic exam. You can ask for a smaller plastic speculum, warm gel, a mirror to watch, a narration of each step before touch happens, or pause at any moment."
                )
            ),
            partnerTakeaway = "Getting tested together before ditching barriers shows care, respect, and responsibility—never wait for symptoms.",
            educatorNote = "Reassure learners that visiting a clinician for itching, unusual odor, or pelvic pain is routine and confidential—providers see these every single day.",
            relatedResourceIds = listOf("res_planned_parenthood", "res_who_srh", "res_bedsider")
        ),
        GuideArticle(
            id = "pelvic_floor_guide",
            title = "Pelvic Floor 101: Strength, Softness & Breath",
            subtitle = "Why relaxation is just as vital as strength for comfort, bladder health, and pleasure.",
            category = ArticleCategory.HEALTH_CARE,
            readMinutes = 4,
            summary = "Your pelvic floor is a dynamic muscular trampoline at the bottom of your core—not a static muscle you should constantly clench.",
            sections = listOf(
                ArticleSection(
                    heading = "What a Healthy Pelvic Floor Does",
                    body = "It lengthens gently when you inhale deeply into your belly and recoils softly when you exhale. It supports your bladder, uterus, and bowel, prevents leaking when you laugh or jump, and contracts rhythmically during orgasm."
                ),
                ArticleSection(
                    heading = "Too Tight vs. Too Weak",
                    body = "Leaking urine isn't always caused by weakness—an overly tense, fatigued (hypertonic) pelvic floor can cause both leakage and pain during penetration or tampon insertion! Before doing high-rep Kegels, practice 'reverse Kegels': deep diaphragmatic inhales imagining your sit-bones gently widening."
                ),
                ArticleSection(
                    heading = "Pelvic Floor Physical Therapy",
                    body = "If you experience pain with penetration (vaginismus or vulvodynia), pelvic heaviness, or persistent bladder leaks—whether postpartum or never having given birth—a specialized Pelvic Floor Physical Therapist can be life-changing."
                )
            ),
            partnerTakeaway = "Pain during sex is never 'in someone's head'—pelvic floor tension is a real physical muscle reflex that responds warmly to patience, breath, and zero pressure.",
            educatorNote = "Move beyond 'just do Kegels'—teach the full inhale-release and exhale-lift cycle connected to diaphragmatic breathing.",
            relatedResourceIds = listOf("res_pelvic_guru", "res_acog")
        ),
        GuideArticle(
            id = "life_stages_inclusion",
            title = "Every Life Stage & Every Body",
            subtitle = "Puberty, postpartum, menopause, and gender-inclusive reproductive literacy.",
            category = ArticleCategory.LIFE_STAGES,
            readMinutes = 6,
            summary = "Vulvas and vaginas are living, adapting tissues that evolve across decades—and belong to people across genders.",
            sections = listOf(
                ArticleSection(
                    heading = "Puberty, Pregnancy & Postpartum",
                    body = "Estrogen during puberty deepens labial pigment, thickens vaginal walls, and launches discharge and cycles. During pregnancy, increased blood flow creates lush swelling; postpartum recovery takes months (not just six weeks!), especially while breastfeeding keeps estrogen temporarily low and tissues drier."
                ),
                ArticleSection(
                    heading = "Perimenopause & Menopause",
                    body = "As ovarian estrogen declines in our 40s and 50s, vaginal and vulvar tissue can become thinner, less elastic, and slightly higher in pH—known as Genitourinary Syndrome of Menopause (GSM). Silicone or hyaluronic-acid moisturizers and low-dose local vaginal estrogen (prescribed by a clinician) restore comfort and prevent recurrent UTIs remarkably well."
                ),
                ArticleSection(
                    heading = "Gender-Inclusive Anatomy Care",
                    body = "Transgender men, non-binary individuals, and people on testosterone therapy also deserve compassionate vaginal/front-hole care. Testosterone can thin internal tissue similarly to menopause (making local topical estrogen—which acts locally without reversing systemic transition—very helpful for comfort). Intersex variations remind us human biology exists on a rich natural spectrum."
                )
            ),
            partnerTakeaway = "Bodies change after birth, during menopause, or through hormone therapy—adapting with extra lube, patience, and affirming language keeps intimacy vibrant for life.",
            educatorNote = "Using inclusive phrasing like 'people with vulvas/vaginas' alongside 'women' welcomes every learner without erasing anyone.",
            relatedResourceIds = listOf("res_ucsf_trans", "res_interact", "res_black_womens_health")
        ),
        GuideArticle(
            id = "partner_educator_handbook",
            title = "The Partner & Educator Communication Handbook",
            subtitle = "Practical ways to ask, listen, teach, and replace embarrassment with curiosity.",
            category = ArticleCategory.PARTNER_EDUCATOR,
            readMinutes = 5,
            summary = "Whether you're supporting a romantic partner or answering tough questions in a classroom or living room, tone makes all the difference.",
            sections = listOf(
                ArticleSection(
                    heading = "Enthusiastic, Ongoing Consent",
                    body = "Consent is warm, active, reversible, and specific—not just the absence of 'no.' Checking in ('Does this pressure feel good?', 'Want to try something slower?') isn't awkward; it's deeply attentive and sexy."
                ),
                ArticleSection(
                    heading = "Listening Without Defensiveness",
                    body = "When a partner guides your hand or asks for a different rhythm, celebrate it! Feedback isn't criticism—it's a treasure map to their specific nervous system."
                ),
                ArticleSection(
                    heading = "Tips for Educators & Parents",
                    body = "Answer the exact question asked in calm, neutral, age-appropriate language. Pair accurate diagrams with myth-busting facts so young people don't rely on unrealistic pornography as their only visual reference for what human bodies look like."
                )
            ),
            partnerTakeaway = "Talk about preferences outside the bedroom too—relaxed conversations over coffee remove vulnerability pressure.",
            educatorNote = "Check out AMAZE and Scarleteen in the Resources tab for free, evidence-based videos and lesson frameworks.",
            relatedResourceIds = listOf("res_amaze", "res_scarleteen", "res_book_our_bodies")
        )
    )

    val glossaryTerms: List<GlossaryTerm> = listOf(
        GlossaryTerm("Arousal Non-Concordance", "uh-ROW-zul non-kun-KOR-dunss", "Pleasure", "The completely normal gap between physical signs of arousal (like wetness) and mental feelings of desire—having one doesn't always mean the other."),
        GlossaryTerm("Bartholin's Glands", "BAR-toh-linz glandz", "Anatomy", "Two pea-sized glands tucked just inside the lower sides of the vaginal opening that release smooth lubricating fluid during arousal."),
        GlossaryTerm("Bacterial Vaginosis (BV)", "bak-TEER-ee-ul vaj-ih-NOH-sis", "Health", "A common, non-STI shift in vaginal bacteria where Lactobacilli dip and other bacteria overgrow, causing thin discharge and a distinct odor; easily treated."),
        GlossaryTerm("Cervical Mucus", "SUR-vih-kul MYOO-kus", "Cycles", "Fluid produced by the cervix that changes from dry/creamy to clear, stretchy 'egg-white' texture around ovulation."),
        GlossaryTerm("Cervix", "SUR-viks", "Anatomy", "The firm, doughnut-shaped lower neck of the uterus sitting at the top of the vaginal canal."),
        GlossaryTerm("Clitoral Crura", "KLIT-or-ul KROO-ruh", "Anatomy", "The two internal wishbone-shaped legs of the clitoris extending backward along the pubic arch."),
        GlossaryTerm("Clitoral Glans", "KLIT-or-ul glanz", "Anatomy", "The visible, ultra-sensitive external tip of the clitoris, home to thousands of sensory nerve endings."),
        GlossaryTerm("Clitoral Hood", "KLIT-or-ul hood", "Anatomy", "The protective fold of skin (prepuce) draping gently over the clitoral glans."),
        GlossaryTerm("CUV Complex", "see-yoo-vee KOM-pleks", "Pleasure", "Clitourethrovaginal complex—the sensitive intersection of the front vaginal wall, urethral sponge, and internal clitoral roots often called the G-zone."),
        GlossaryTerm("Dental Dam", "DEN-tul dam", "Health", "A thin, flexible latex or polyurethane sheet placed over the vulva or anus as an STI barrier during oral sex."),
        GlossaryTerm("Endometrium", "en-doh-MEE-tree-um", "Cycles", "The nutrient-rich inner lining of the uterus that builds each cycle and sheds during menstruation."),
        GlossaryTerm("Estrogen", "ES-truh-jen", "Cycles", "A primary hormone guiding cycle building, vaginal lubrication, tissue elasticity, bone density, and healthy pH."),
        GlossaryTerm("Fallopian Tubes", "fuh-LOH-pee-un toobz", "Anatomy", "Two slender pathways with sweeping ends (fimbriae) that carry a released egg from the ovary toward the uterus."),
        GlossaryTerm("Fordyce Spots", "FOR-dyss spots", "Diversity", "Tiny, harmless, painless pale or skin-colored oil glands commonly visible on the labia—not an STI!"),
        GlossaryTerm("GSM (Genitourinary Syndrome of Menopause)", "jee-ess-em", "Life Stages", "Vaginal dryness, thinning, or urinary sensitivity caused by lower estrogen in perimenopause/menopause—highly treatable with moisturizers or local estrogen."),
        GlossaryTerm("Hymen (Vaginal Corona)", "HY-men", "Anatomy", "A stretchy, naturally open fringe of thin tissue around the vaginal opening—not a sealed barrier and never a measure of sexual history."),
        GlossaryTerm("Labia Majora", "LAY-bee-uh muh-JOR-uh", "Anatomy", "The outer, cushiony hair-bearing folds of the vulva that frame and protect the inner structures."),
        GlossaryTerm("Labia Minora", "LAY-bee-uh mih-NOR-uh", "Anatomy", "The delicate, hairless inner folds of the vulva, naturally diverse in length, ruffle, symmetry, and pigment."),
        GlossaryTerm("Lactobacillus", "lak-toh-buh-SIL-us", "Health", "Friendly probiotic bacteria that keep the healthy vagina naturally acidic (pH 3.8–4.5) and self-cleaning."),
        GlossaryTerm("Menstrual Disc", "MEN-stroo-ul disk", "Cycles", "A flexible reusable or disposable period barrier that sits tucked high behind the pubic bone in the vaginal fornix."),
        GlossaryTerm("Mons Pubis", "monz PYOO-bis", "Anatomy", "The rounded mound of soft cushiony tissue sitting over the pubic bone at the top of the vulva."),
        GlossaryTerm("Ovulation", "ov-yoo-LAY-shun", "Cycles", "The release of a mature egg from an ovarian follicle roughly midway through a menstrual cycle."),
        GlossaryTerm("Pelvic Floor", "PEL-vik flor", "Anatomy", "The layered hammock of muscles spanning the base of the pelvis, supporting organs, continence, and orgasmic contractions."),
        GlossaryTerm("Perineum", "pair-ih-NEE-um", "Anatomy", "The bridge of skin and muscle between the vaginal opening and the anus."),
        GlossaryTerm("Progesterone", "proh-JES-tuh-rohn", "Cycles", "A calming hormone dominant in the second half (luteal phase) of the cycle after ovulation."),
        GlossaryTerm("Responsive Desire", "rih-SPON-siv dih-ZY-ur", "Pleasure", "Sexual desire that wakes up *in response* to affectionate touch, safety, and context rather than popping up spontaneously."),
        GlossaryTerm("Rugae", "ROO-gee", "Anatomy", "The soft accordion-like ridges inside the vaginal walls that allow comfortable expansion and elastic rebound."),
        GlossaryTerm("Skene's Glands", "skeen'z glandz", "Anatomy", "Paraurethral glands hugging the urethra that swell during arousal and can release clear fluid."),
        GlossaryTerm("Tenting", "TEN-ting", "Pleasure", "The natural lengthening and lifting of the upper vaginal canal and uterus during sexual arousal."),
        GlossaryTerm("Urethral Opening", "yoo-REE-thrul OH-puh-ning", "Anatomy", "The small opening above the vagina where urine exits from the bladder."),
        GlossaryTerm("Vagina", "vuh-JY-nuh", "Anatomy", "The internal elastic muscular canal connecting the cervix to the external vulva."),
        GlossaryTerm("Vestibular Bulbs", "ves-TIB-yoo-ler bulbz", "Anatomy", "Paired internal erectile cushions of the clitoral network flanking both sides of the vaginal entrance."),
        GlossaryTerm("Vulva", "VUL-vuh", "Anatomy", "All external genital structures together: mons pubis, outer and inner labia, clitoral hood/glans, urethral opening, and vaginal entrance.")
    )

    val externalResources: List<ExternalResource> = listOf(
        // Curated Visual & Diversity Archives
        ExternalResource(
            id = "res_labia_library",
            title = "The Labia Library",
            organization = "Women's Health Victoria (Australia)",
            category = ResourceCategory.PHOTO_ARCHIVES,
            description = "A pioneering, medically supported public-health educational photo gallery showing the wide natural diversity of real, unretouched human vulvas alongside body-confidence facts.",
            url = "https://www.labialibrary.org.au/",
            badge = "Real Photo Archive"
        ),
        ExternalResource(
            id = "res_gynodiversity",
            title = "Gynodiversity",
            organization = "Crowd-Sourced Educational Photo Project",
            category = ResourceCategory.PHOTO_ARCHIVES,
            description = "An educational photography project documenting real-world anatomical variation in vulvas contributed by consenting adults to counter unrealistic media norms.",
            url = "https://gynodiversity.com/",
            badge = "Real Photo Archive"
        ),
        ExternalResource(
            id = "res_great_wall",
            title = "The Great Wall of Vulva",
            organization = "Jamie McCartney — 400 Real Life-Casts",
            category = ResourceCategory.PHOTO_ARCHIVES,
            description = "Monumental educational plaster sculpture panels cast directly from 400 real people across ages, ethnicities, childbirth histories, and life stages.",
            url = "https://www.thegreatwallofvulva.com/",
            badge = "Real Life-Casts"
        ),
        ExternalResource(
            id = "res_vulva_gallery",
            title = "The Vulva Gallery",
            organization = "Hilde Atalanta — Educational Art & Stories",
            category = ResourceCategory.PHOTO_ARCHIVES,
            description = "Celebrated watercolor portrait series and personal stories celebrating anatomical diversity across ages, ethnicities, life stages, and gender identities.",
            url = "https://www.thevulvagallery.com/",
            badge = "Portraits & Stories"
        ),
        ExternalResource(
            id = "res_cliteracy",
            title = "Cliteracy & 3D Clitoral Literacy",
            organization = "Educational Anatomical Literacy Project",
            category = ResourceCategory.PHOTO_ARCHIVES,
            description = "Explore 3D anatomical research and visual literacy dedicated to closing the knowledge gap around the full internal clitoral organ.",
            url = "https://www.plannedparenthood.org/learn/health-and-wellness/sexual-and-reproductive-anatomy/what-are-parts-female-sexual-anatomy",
            badge = "Visual Guide"
        ),

        // Inclusive Sexual Health Education
        ExternalResource(
            id = "res_scarleteen",
            title = "Scarleteen: Sex Ed for the Real World",
            organization = "Scarleteen",
            category = ResourceCategory.SEX_EDUCATION,
            description = "Warm, inclusive, fiercely accurate guides on anatomy, consent, pleasure, cycles, identity, and communication for learners and partners of all backgrounds.",
            url = "https://www.scarleteen.com/",
            badge = "Inclusive Ed"
        ),
        ExternalResource(
            id = "res_planned_parenthood",
            title = "Sexual & Reproductive Anatomy Hub",
            organization = "Planned Parenthood",
            category = ResourceCategory.SEX_EDUCATION,
            description = "Clear, accessible articles covering reproductive anatomy, STI testing, birth control options, pleasure, and everyday wellness.",
            url = "https://www.plannedparenthood.org/learn",
            badge = "Trusted Hub"
        ),
        ExternalResource(
            id = "res_omgyes",
            title = "OMGYES Pleasure Research",
            organization = "OMGYES & Indiana University Kinsey Research",
            category = ResourceCategory.SEX_EDUCATION,
            description = "Large-scale scientific research into real techniques, touch patterns, and language surrounding female sexual pleasure for individuals and partners.",
            url = "https://www.omgyes.com/",
            badge = "Research-Backed"
        ),
        ExternalResource(
            id = "res_bedsider",
            title = "Bedsider Birth Control & Wellness",
            organization = "Power to Decide",
            category = ResourceCategory.SEX_EDUCATION,
            description = "Friendly, non-judgmental comparisons of contraception methods, side effects, and reproductive wellness.",
            url = "https://www.bedsider.org/",
            badge = "Contraception"
        ),
        ExternalResource(
            id = "res_amaze",
            title = "AMAZE Animated Sex Ed",
            organization = "Advocates for Youth & AMAZE",
            category = ResourceCategory.SEX_EDUCATION,
            description = "Short, friendly animated educational videos and educator toolkits for parents, teachers, and curious learners.",
            url = "https://amaze.org/",
            badge = "For Educators"
        ),

        // Clinical & Medical Guides
        ExternalResource(
            id = "res_acog",
            title = "ACOG Women's Health Library",
            organization = "American College of Obstetricians and Gynecologists",
            category = ResourceCategory.CLINICAL_GUIDES,
            description = "Evidence-based patient FAQs on vulvovaginal health, menstruation, pelvic pain, endometriosis, pregnancy, and menopause.",
            url = "https://www.acog.org/womens-health",
            badge = "Clinical OB/GYN"
        ),
        ExternalResource(
            id = "res_who_srh",
            title = "WHO Sexual & Reproductive Health",
            organization = "World Health Organization",
            category = ResourceCategory.CLINICAL_GUIDES,
            description = "Global health standards, comprehensive sexuality education briefs, and evidence-based reproductive health guidelines.",
            url = "https://www.who.int/health-topics/sexual-health",
            badge = "Global Health"
        ),
        ExternalResource(
            id = "res_isswsh",
            title = "ISSWSH Sexual Health Resources",
            organization = "International Society for the Study of Women's Sexual Health",
            category = ResourceCategory.CLINICAL_GUIDES,
            description = "Specialist clinical insights on arousal, pain-free intimacy, menopause care, and finding sexual medicine providers.",
            url = "https://www.isswsh.org/",
            badge = "Medical Society"
        ),

        // Pelvic Floor & Gender-Inclusive Care
        ExternalResource(
            id = "res_pelvic_guru",
            title = "Pelvic Guru & Therapist Directory",
            organization = "Pelvic Guru",
            category = ResourceCategory.PELVIC_AND_INCLUSION,
            description = "Educational articles and global directory to find a qualified Pelvic Floor Physical Therapist for pain, postpartum, or bladder support.",
            url = "https://pelvicguru.com/",
            badge = "Pelvic Health"
        ),
        ExternalResource(
            id = "res_ucsf_trans",
            title = "Gender-Affirming Pelvic & Sexual Health",
            organization = "UCSF Transgender Care",
            category = ResourceCategory.PELVIC_AND_INCLUSION,
            description = "Evidence-based guidelines on gynecological care, hormone effects on vaginal tissue, and respectful clinical practices for trans and non-binary people.",
            url = "https://transcare.ucsf.edu/guidelines",
            badge = "Inclusive Care"
        ),
        ExternalResource(
            id = "res_interact",
            title = "InterACT Intersex Education",
            organization = "InterACT Advocates for Intersex Youth",
            category = ResourceCategory.PELVIC_AND_INCLUSION,
            description = "Accessible brochures and medical literacy resources affirming natural intersex anatomical variations.",
            url = "https://interactadvocates.org/",
            badge = "Intersex Literacy"
        ),
        ExternalResource(
            id = "res_black_womens_health",
            title = "Black Women's Health Imperative",
            organization = "BWHI",
            category = ResourceCategory.PELVIC_AND_INCLUSION,
            description = "Culturally centered reproductive wellness, fibroid awareness, maternal advocacy, and sexual health resources.",
            url = "https://bwhi.org/",
            badge = "Health Equity"
        ),

        // Essential Books & Research
        ExternalResource(
            id = "res_book_vagina_bible",
            title = "The Vagina Bible — Dr. Jen Gunter",
            organization = "OB/GYN Evidence-Based Guide",
            category = ResourceCategory.BOOKS_MEDIA,
            description = "A witty, fiercely myth-busting, comprehensive guide to vulva and vaginal medicine written by a board-certified OB/GYN.",
            url = "https://drjengunter.com/",
            badge = "Essential Book"
        ),
        ExternalResource(
            id = "res_book_come_as_you_are",
            title = "Come As You Are — Dr. Emily Nagoski",
            organization = "Science of Sexual Wellbeing",
            category = ResourceCategory.BOOKS_MEDIA,
            description = "The landmark science-backed book explaining responsive desire, stress brakes, and why every body's sexual response is normal.",
            url = "https://www.emilynagoski.com/",
            badge = "Essential Book"
        ),
        ExternalResource(
            id = "res_book_cliterate",
            title = "Becoming Cliterate — Dr. Laurie Mintz",
            organization = "Pleasure Literacy Guide",
            category = ResourceCategory.BOOKS_MEDIA,
            description = "Practical, inclusive guide for individuals and partners on closing the pleasure gap with accurate anatomy and communication.",
            url = "https://www.drlauriemintz.com/",
            badge = "Partner Favorite"
        ),
        ExternalResource(
            id = "res_book_our_bodies",
            title = "Our Bodies, Ourselves Today",
            organization = "OBOS Global Health Collective",
            category = ResourceCategory.BOOKS_MEDIA,
            description = "The enduring feminist health education platform offering accessible guides across every life stage.",
            url = "https://www.ourbodiesourselves.org/",
            badge = "Classic Guide"
        )
    )
}
