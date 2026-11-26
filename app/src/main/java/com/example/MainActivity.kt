package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AtlasTab
import com.example.ui.screens.AnatomyScreen
import com.example.ui.screens.ArticleDetailScreen
import com.example.ui.screens.CycleExplorerScreen
import com.example.ui.screens.DiversityFeedScreen
import com.example.ui.screens.GlossaryScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.MythQuizScreen
import com.example.ui.screens.PhotoLightboxScreen
import com.example.ui.screens.ResourcesScreen
import com.example.ui.screens.SpectrumLabScreen
import com.example.ui.screens.StudyDetailScreen
import com.example.ui.theme.BloomAtlasTheme
import com.example.ui.viewmodel.AtlasOverlay
import com.example.ui.viewmodel.AtlasViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BloomAtlasTheme {
                BloomAtlasApp()
            }
        }
    }
}

@Composable
fun BloomAtlasApp(
    viewModel: AtlasViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Full-screen interactive sub-screens / detail overlays with BackHandler
    when (val overlay = uiState.overlay) {
        is AtlasOverlay.ArticleDetail -> {
            ArticleDetailScreen(
                article = overlay.article,
                isBookmarked = overlay.article.id in uiState.bookmarkedArticleIds,
                onToggleBookmark = { viewModel.toggleArticleBookmark(overlay.article.id) },
                onBack = viewModel::closeOverlay
            )
            return
        }
        is AtlasOverlay.StudyDetail -> {
            StudyDetailScreen(
                study = overlay.study,
                isBookmarked = overlay.study.studyNumber in uiState.bookmarkedStudyNumbers,
                onToggleBookmark = { viewModel.toggleStudyBookmark(overlay.study.studyNumber) },
                onOpenSpectrumLab = viewModel::openSpectrumLab,
                onBack = viewModel::closeOverlay
            )
            return
        }
        is AtlasOverlay.SpectrumLab -> {
            SpectrumLabScreen(
                labState = uiState.spectrumLab,
                onUpdateLab = viewModel::updateSpectrumLab,
                onRandomize = viewModel::randomizeSpectrumLab,
                onBack = viewModel::closeOverlay
            )
            return
        }
        is AtlasOverlay.CycleExplorer -> {
            CycleExplorerScreen(
                selectedPhaseId = uiState.selectedCyclePhaseId,
                onSelectPhase = viewModel::selectCyclePhase,
                onBack = viewModel::closeOverlay
            )
            return
        }
        is AtlasOverlay.MythQuiz -> {
            MythQuizScreen(
                masteredMythIds = uiState.masteredMythIds,
                onMarkMastered = viewModel::markMythMastered,
                onBack = viewModel::closeOverlay
            )
            return
        }
        is AtlasOverlay.Glossary -> {
            GlossaryScreen(onBack = viewModel::closeOverlay)
            return
        }
        is AtlasOverlay.PhotoLightbox -> {
            PhotoLightboxScreen(
                photo = overlay.photo,
                onBack = viewModel::closeOverlay
            )
            return
        }
        AtlasOverlay.None -> Unit
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        if (useNavigationRail) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        AtlasTab.entries.forEach { tab ->
                            val selected = uiState.activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        TabContentHost(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier.widthIn(max = 800.dp)
                        )
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        AtlasTab.entries.forEach { tab ->
                            val selected = uiState.activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                TabContentHost(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun TabContentHost(
    uiState: com.example.ui.viewmodel.AtlasUiState,
    viewModel: AtlasViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (uiState.activeTab) {
            AtlasTab.ANATOMY -> AnatomyScreen(
                uiState = uiState,
                blossomViewportState = viewModel.blossomViewportState,
                onSelectView = viewModel::selectAnatomyView,
                onSelectPart = viewModel::selectAnatomyPart,
                onToggleLabels = viewModel::toggleDiagramLabels,
                onSelectVulvaDisplayMode = viewModel::selectVulvaDisplayMode,
                onToggleHoldVulvaOpen = viewModel::toggleHoldVulvaOpen,
                onResetVulvaTissue = viewModel::resetVulvaTissue,
                onSelectVulvaTone = viewModel::selectVulvaTone,
                onToggleMedicalPlates = viewModel::toggleUseMedicalPlates,
                onToggleHaptics = viewModel::toggleHaptics,
                onToggleTouchExplore = viewModel::toggleTouchExplore,
                onReadArticle = viewModel::openArticleById,
                onOpenDiversity = { viewModel.selectTab(AtlasTab.DIVERSITY) }
            )
            AtlasTab.DIVERSITY -> DiversityFeedScreen(
                uiState = uiState,
                onSelectViewMode = viewModel::selectDiversityViewMode,
                onConfirmAdultGate = viewModel::confirmAdultEducationalGate,
                onSelectPhotoCategory = viewModel::selectPhotoCategory,
                onLoadMoreRealPhotos = viewModel::loadMoreRealPhotos,
                onRetryRealPhotos = viewModel::retryRealPhotos,
                onTogglePrivacyBlur = viewModel::togglePrivacyBlur,
                onToggleUnblurPhoto = viewModel::toggleUnblurPhoto,
                onOpenPhotoLightbox = viewModel::openPhotoLightbox,
                onSelectTrait = viewModel::selectDiversityTrait,
                onLoadMore = viewModel::loadMoreDiversityStudies,
                onToggleStudyBookmark = viewModel::toggleStudyBookmark,
                onOpenStudyDetail = viewModel::openStudyDetail,
                onOpenSpectrumLab = viewModel::openSpectrumLab
            )
            AtlasTab.LEARN -> LearnScreen(
                uiState = uiState,
                onAudienceModeChange = viewModel::setAudienceMode,
                onSearchChange = viewModel::updateSearchQuery,
                onSelectCategory = viewModel::selectArticleCategory,
                onOpenArticle = viewModel::openArticle,
                onToggleArticleBookmark = viewModel::toggleArticleBookmark,
                onOpenCycleExplorer = viewModel::openCycleExplorer,
                onOpenMythQuiz = viewModel::openMythQuiz,
                onOpenGlossary = viewModel::openGlossary
            )
            AtlasTab.RESOURCES -> ResourcesScreen(
                uiState = uiState,
                onOpenArticle = viewModel::openArticle,
                onOpenStudyByNumber = viewModel::openStudyByNumber
            )
        }
    }
}

private fun tabIcon(tab: AtlasTab, selected: Boolean): ImageVector = when (tab) {
    AtlasTab.ANATOMY -> if (selected) Icons.Filled.Spa else Icons.Outlined.Spa
    AtlasTab.DIVERSITY -> if (selected) Icons.Filled.GridView else Icons.Outlined.GridView
    AtlasTab.LEARN -> if (selected) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook
    AtlasTab.RESOURCES -> if (selected) Icons.Filled.Explore else Icons.Outlined.Explore
}
