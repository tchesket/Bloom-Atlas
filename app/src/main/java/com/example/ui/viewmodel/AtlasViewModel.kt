package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AnatomyPart
import com.example.data.model.AnatomyViewType
import com.example.data.model.ArticleCategory
import com.example.data.model.AtlasTab
import com.example.data.model.AudienceMode
import com.example.data.model.DiversityTrait
import com.example.data.model.GuideArticle
import com.example.data.model.MedicalPlateAsset
import com.example.data.model.VulvaStudy
import com.example.data.model.WikimediaPhotoCategory
import com.example.data.model.WikimediaPhotoItem
import com.example.data.repository.AnatomyContentData
import com.example.data.repository.ArticlesAndResourcesData
import com.example.data.repository.DiversityStudyGenerator
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.WikimediaMediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AtlasOverlay {
    data object None : AtlasOverlay
    data class ArticleDetail(val article: GuideArticle) : AtlasOverlay
    data object Glossary : AtlasOverlay
    data object MythQuiz : AtlasOverlay
    data object CycleExplorer : AtlasOverlay
    data object SpectrumLab : AtlasOverlay
    data class StudyDetail(val study: VulvaStudy) : AtlasOverlay
    data class PhotoLightbox(val photo: WikimediaPhotoItem) : AtlasOverlay
}

data class SpectrumLabState(
    val leftProjection: Float = 0.55f,
    val rightProjection: Float = 0.42f,
    val outerFullness: Float = 0.60f,
    val hoodCoverage: Float = 0.52f,
    val ruffleFactor: Float = 0.48f,
    val paletteIndex: Int = 1
)

enum class DiversityViewMode(val label: String) {
    PHOTO_ARCHIVES("Real Photo Feed"),
    ILLUSTRATED_FEED("Illustrated & Lab")
}

enum class VulvaDisplayMode(val label: String) {
    STUDIO_3D("3D Studio"),
    PAINTED_2D("2D Painted"),
    PHOTO_REFERENCE("Photo Ref")
}

data class AtlasUiState(
    val activeTab: AtlasTab = AtlasTab.ANATOMY,
    val overlay: AtlasOverlay = AtlasOverlay.None,
    val audienceMode: AudienceMode = AudienceMode.EVERYONE,

    // Interactive Anatomy, Deformable Vulva & Haptics state
    val selectedView: AnatomyViewType = AnatomyViewType.EXTERNAL_VULVA,
    val selectedPartId: String = "ext_hood",
    val showDiagramLabels: Boolean = false,
    val vulvaDisplayMode: VulvaDisplayMode = VulvaDisplayMode.STUDIO_3D,
    val holdVulvaOpen: Boolean = false,
    val vulvaResetTrigger: Int = 0,
    val vulvaToneIndex: Int = 1,
    val useMedicalPlates: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val touchExploreMode: Boolean = false,
    val medicalPlates: Map<AnatomyViewType, MedicalPlateAsset> = emptyMap(),

    // Diversity Tab — Native Wikimedia Commons Real Photo Feed + Illustrated Feed
    val diversityViewMode: DiversityViewMode = DiversityViewMode.PHOTO_ARCHIVES,
    val hasConfirmedAdultEducationalGate: Boolean = false,
    val selectedPhotoCategory: WikimediaPhotoCategory = WikimediaPhotoCategory.ALL_VULVAS,
    val wikimediaPhotos: List<WikimediaPhotoItem> = emptyList(),
    val photoContinueToken: String? = null,
    val hasMorePhotos: Boolean = true,
    val isLoadingPhotos: Boolean = false,
    val photoFeedError: String? = null,
    val privacyBlurEnabled: Boolean = false,
    val unblurredPhotoIds: Set<Long> = emptySet(),

    // Illustrated Diversity Feed & Spectrum Lab
    val selectedTrait: DiversityTrait = DiversityTrait.ALL,
    val diversityFeed: List<VulvaStudy> = emptyList(),
    val nextStudyNumber: Int = 1,
    val spectrumLab: SpectrumLabState = SpectrumLabState(),

    // Learn state
    val searchQuery: String = "",
    val selectedArticleCategory: ArticleCategory = ArticleCategory.ALL,
    val selectedCyclePhaseId: String = "follicular",

    // Persisted preferences
    val bookmarkedArticleIds: Set<String> = emptySet(),
    val bookmarkedStudyNumbers: Set<Int> = emptySet(),
    val masteredMythIds: Set<String> = emptySet()
) {
    val currentViewParts: List<AnatomyPart>
        get() = AnatomyContentData.anatomyParts.filter { it.viewType == selectedView }

    val selectedPart: AnatomyPart?
        get() = currentViewParts.firstOrNull { it.id == selectedPartId } ?: currentViewParts.firstOrNull()

    val currentMedicalPlate: MedicalPlateAsset?
        get() = medicalPlates[selectedView]

    val filteredArticles: List<GuideArticle>
        get() = ArticlesAndResourcesData.articles.filter { article ->
            val matchesCategory = selectedArticleCategory == ArticleCategory.ALL ||
                article.category == selectedArticleCategory
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                article.title.lowercase().contains(q) ||
                article.subtitle.lowercase().contains(q) ||
                article.summary.lowercase().contains(q) ||
                article.sections.any {
                    it.heading.lowercase().contains(q) || it.body.lowercase().contains(q)
                }
            matchesCategory && matchesQuery
        }
}

class AtlasViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application.applicationContext)

    /**
     * Scroll- and tab-persistent session state for Blossom's 3D viewport (v19.0).
     * Prevents `LazyColumn` item disposal or tab navigation from resetting Blossom's animations,
     * self-spread pose, tissue/leg/head positions, camera framing, heart rate, or Beta Lab settings.
     */
    val blossomViewportState = com.example.ui.components.BlossomViewportSessionState()

    private val _uiState = MutableStateFlow(AtlasUiState())
    val uiState: StateFlow<AtlasUiState> = _uiState.asStateFlow()

    private var photoLoadJob: Job? = null

    init {
        loadInitialDiversityFeed(DiversityTrait.ALL)
        loadMedicalPlates()
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                _uiState.update { state ->
                    state.copy(
                        bookmarkedArticleIds = prefs.bookmarkedArticleIds,
                        bookmarkedStudyNumbers = prefs.bookmarkedStudyNumbers,
                        masteredMythIds = prefs.masteredMythIds,
                        audienceMode = prefs.audienceMode
                    )
                }
            }
        }
    }

    private fun loadMedicalPlates() {
        viewModelScope.launch {
            val plates = WikimediaMediaRepository.fetchMedicalPlates()
            if (plates.isNotEmpty()) {
                _uiState.update { it.copy(medicalPlates = plates) }
            }
        }
    }

    fun selectTab(tab: AtlasTab) {
        _uiState.update { it.copy(activeTab = tab, overlay = AtlasOverlay.None) }
    }

    fun selectAnatomyView(viewType: AnatomyViewType) {
        val defaultPart = AnatomyContentData.anatomyParts.firstOrNull { it.viewType == viewType }
        _uiState.update {
            it.copy(
                selectedView = viewType,
                selectedPartId = defaultPart?.id ?: it.selectedPartId
            )
        }
    }

    fun selectAnatomyPart(partId: String) {
        _uiState.update { it.copy(selectedPartId = partId) }
    }

    fun toggleDiagramLabels() {
        _uiState.update { it.copy(showDiagramLabels = !it.showDiagramLabels) }
    }

    fun selectVulvaDisplayMode(mode: VulvaDisplayMode) {
        _uiState.update { it.copy(vulvaDisplayMode = mode) }
    }

    fun toggleHoldVulvaOpen() {
        _uiState.update { it.copy(holdVulvaOpen = !it.holdVulvaOpen) }
    }

    fun resetVulvaTissue() {
        _uiState.update {
            it.copy(
                holdVulvaOpen = false,
                vulvaResetTrigger = it.vulvaResetTrigger + 1
            )
        }
    }

    fun selectVulvaTone(index: Int) {
        _uiState.update { it.copy(vulvaToneIndex = index) }
    }

    fun toggleUseMedicalPlates() {
        _uiState.update { it.copy(useMedicalPlates = !it.useMedicalPlates) }
    }

    fun toggleHaptics() {
        _uiState.update { it.copy(hapticsEnabled = !it.hapticsEnabled) }
    }

    fun toggleTouchExplore() {
        _uiState.update { it.copy(touchExploreMode = !it.touchExploreMode) }
    }

    fun selectDiversityViewMode(mode: DiversityViewMode) {
        _uiState.update { it.copy(diversityViewMode = mode) }
        if (mode == DiversityViewMode.PHOTO_ARCHIVES &&
            _uiState.value.hasConfirmedAdultEducationalGate &&
            _uiState.value.wikimediaPhotos.isEmpty()
        ) {
            loadMoreRealPhotos()
        }
    }

    fun confirmAdultEducationalGate() {
        _uiState.update { it.copy(hasConfirmedAdultEducationalGate = true) }
        if (_uiState.value.wikimediaPhotos.isEmpty()) {
            loadMoreRealPhotos()
        }
    }

    fun selectPhotoCategory(category: WikimediaPhotoCategory) {
        if (_uiState.value.selectedPhotoCategory == category && _uiState.value.wikimediaPhotos.isNotEmpty()) {
            return
        }
        photoLoadJob?.cancel()
        _uiState.update {
            it.copy(
                selectedPhotoCategory = category,
                wikimediaPhotos = emptyList(),
                photoContinueToken = null,
                hasMorePhotos = true,
                isLoadingPhotos = false,
                photoFeedError = null,
                unblurredPhotoIds = emptySet()
            )
        }
        loadMoreRealPhotos()
    }

    fun retryRealPhotos() {
        _uiState.update { it.copy(photoFeedError = null) }
        loadMoreRealPhotos()
    }

    fun loadMoreRealPhotos() {
        val current = _uiState.value
        if (current.isLoadingPhotos || !current.hasMorePhotos) return

        photoLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPhotos = true, photoFeedError = null) }
            val result = WikimediaMediaRepository.fetchDiversityPhotos(
                category = current.selectedPhotoCategory,
                continueToken = current.photoContinueToken
            )
            result.fold(
                onSuccess = { page ->
                    _uiState.update { state ->
                        val existingIds = state.wikimediaPhotos.map { it.pageId }.toHashSet()
                        val uniqueNew = page.items.filter { it.pageId !in existingIds }
                        state.copy(
                            wikimediaPhotos = state.wikimediaPhotos + uniqueNew,
                            photoContinueToken = page.nextContinueToken,
                            hasMorePhotos = page.nextContinueToken != null,
                            isLoadingPhotos = false,
                            photoFeedError = null
                        )
                    }
                },
                onFailure = {
                    _uiState.update { state ->
                        state.copy(
                            isLoadingPhotos = false,
                            photoFeedError = "Unable to reach Wikimedia Commons right now. Check your connection and tap Retry."
                        )
                    }
                }
            )
        }
    }

    fun togglePrivacyBlur() {
        _uiState.update {
            it.copy(
                privacyBlurEnabled = !it.privacyBlurEnabled,
                unblurredPhotoIds = emptySet()
            )
        }
    }

    fun toggleUnblurPhoto(pageId: Long) {
        _uiState.update { state ->
            val updated = if (pageId in state.unblurredPhotoIds) {
                state.unblurredPhotoIds - pageId
            } else {
                state.unblurredPhotoIds + pageId
            }
            state.copy(unblurredPhotoIds = updated)
        }
    }

    fun openPhotoLightbox(photo: WikimediaPhotoItem) {
        _uiState.update { it.copy(overlay = AtlasOverlay.PhotoLightbox(photo)) }
    }

    fun selectDiversityTrait(trait: DiversityTrait) {
        if (_uiState.value.selectedTrait == trait && _uiState.value.diversityFeed.isNotEmpty()) return
        loadInitialDiversityFeed(trait)
    }

    private fun loadInitialDiversityFeed(trait: DiversityTrait) {
        val batch = DiversityStudyGenerator.generateBatch(startNumber = 1, count = 14, filter = trait)
        val nextNum = (batch.lastOrNull()?.studyNumber ?: 0) + 1
        _uiState.update {
            it.copy(
                selectedTrait = trait,
                diversityFeed = batch,
                nextStudyNumber = nextNum
            )
        }
    }

    fun loadMoreDiversityStudies() {
        val current = _uiState.value
        val nextBatch = DiversityStudyGenerator.generateBatch(
            startNumber = current.nextStudyNumber,
            count = 12,
            filter = current.selectedTrait
        )
        val newNext = (nextBatch.lastOrNull()?.studyNumber ?: current.nextStudyNumber) + 1
        _uiState.update {
            it.copy(
                diversityFeed = it.diversityFeed + nextBatch,
                nextStudyNumber = newNext
            )
        }
    }

    fun updateSpectrumLab(transform: (SpectrumLabState) -> SpectrumLabState) {
        _uiState.update { it.copy(spectrumLab = transform(it.spectrumLab)) }
    }

    fun randomizeSpectrumLab() {
        val randomStudy = DiversityStudyGenerator.generateSingle(
            studyNumber = (100..9999).random()
        )
        _uiState.update {
            it.copy(
                spectrumLab = SpectrumLabState(
                    leftProjection = randomStudy.innerLeftProjection,
                    rightProjection = randomStudy.innerRightProjection,
                    outerFullness = randomStudy.outerFullness,
                    hoodCoverage = randomStudy.hoodCoverage,
                    ruffleFactor = randomStudy.ruffleFactor,
                    paletteIndex = randomStudy.paletteIndex
                )
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectArticleCategory(category: ArticleCategory) {
        _uiState.update { it.copy(selectedArticleCategory = category) }
    }

    fun selectCyclePhase(phaseId: String) {
        _uiState.update { it.copy(selectedCyclePhaseId = phaseId) }
    }

    fun openArticleById(articleId: String) {
        val article = ArticlesAndResourcesData.articles.firstOrNull { it.id == articleId } ?: return
        _uiState.update { it.copy(overlay = AtlasOverlay.ArticleDetail(article)) }
    }

    fun openArticle(article: GuideArticle) {
        _uiState.update { it.copy(overlay = AtlasOverlay.ArticleDetail(article)) }
    }

    fun openStudyDetail(study: VulvaStudy) {
        _uiState.update { it.copy(overlay = AtlasOverlay.StudyDetail(study)) }
    }

    fun openStudyByNumber(studyNumber: Int) {
        val study = DiversityStudyGenerator.generateSingle(studyNumber)
        _uiState.update { it.copy(overlay = AtlasOverlay.StudyDetail(study)) }
    }

    fun openGlossary() {
        _uiState.update { it.copy(overlay = AtlasOverlay.Glossary) }
    }

    fun openMythQuiz() {
        _uiState.update { it.copy(overlay = AtlasOverlay.MythQuiz) }
    }

    fun openCycleExplorer() {
        _uiState.update { it.copy(overlay = AtlasOverlay.CycleExplorer) }
    }

    fun openSpectrumLab() {
        _uiState.update { it.copy(overlay = AtlasOverlay.SpectrumLab) }
    }

    fun closeOverlay() {
        _uiState.update { it.copy(overlay = AtlasOverlay.None) }
    }

    fun setAudienceMode(mode: AudienceMode) {
        viewModelScope.launch {
            preferencesRepository.setAudienceMode(mode)
        }
    }

    fun toggleArticleBookmark(articleId: String) {
        viewModelScope.launch {
            preferencesRepository.toggleArticleBookmark(articleId)
        }
    }

    fun toggleStudyBookmark(studyNumber: Int) {
        viewModelScope.launch {
            preferencesRepository.toggleStudyBookmark(studyNumber)
        }
    }

    fun markMythMastered(mythId: String) {
        viewModelScope.launch {
            preferencesRepository.markMythMastered(mythId)
        }
    }
}
