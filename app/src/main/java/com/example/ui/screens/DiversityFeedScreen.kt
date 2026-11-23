package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOff
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.DiversityTrait
import com.example.data.model.VulvaStudy
import com.example.data.model.WikimediaPhotoCategory
import com.example.data.model.WikimediaPhotoItem
import com.example.data.repository.DiversityStudyGenerator
import com.example.data.repository.WikimediaMediaRepository
import com.example.ui.components.DiversityStudyCanvas
import com.example.ui.viewmodel.AtlasUiState
import com.example.ui.viewmodel.DiversityViewMode

@Composable
fun DiversityFeedScreen(
    uiState: AtlasUiState,
    onSelectViewMode: (DiversityViewMode) -> Unit,
    onConfirmAdultGate: () -> Unit,
    onSelectPhotoCategory: (WikimediaPhotoCategory) -> Unit,
    onLoadMoreRealPhotos: () -> Unit,
    onRetryRealPhotos: () -> Unit,
    onTogglePrivacyBlur: () -> Unit,
    onToggleUnblurPhoto: (Long) -> Unit,
    onOpenPhotoLightbox: (WikimediaPhotoItem) -> Unit,
    onSelectTrait: (DiversityTrait) -> Unit,
    onLoadMore: () -> Unit,
    onToggleStudyBookmark: (Int) -> Unit,
    onOpenStudyDetail: (VulvaStudy) -> Unit,
    onOpenSpectrumLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Persistent Top Mode Switcher: Real Photo Feed vs. Illustrated & Lab
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.diversity_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                DiversityViewMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = uiState.diversityViewMode == mode,
                        onClick = { onSelectViewMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = DiversityViewMode.entries.size
                        ),
                        modifier = Modifier.testTag("diversity_mode_${mode.name.lowercase()}"),
                        icon = {
                            Icon(
                                imageVector = if (mode == DiversityViewMode.PHOTO_ARCHIVES) {
                                    Icons.Filled.Collections
                                } else {
                                    Icons.Filled.Palette
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text(
                            text = mode.label,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        when (uiState.diversityViewMode) {
            DiversityViewMode.PHOTO_ARCHIVES -> {
                RealPhotoFeedSection(
                    uiState = uiState,
                    onConfirmGate = onConfirmAdultGate,
                    onSelectCategory = onSelectPhotoCategory,
                    onLoadMore = onLoadMoreRealPhotos,
                    onRetry = onRetryRealPhotos,
                    onTogglePrivacyBlur = onTogglePrivacyBlur,
                    onToggleUnblurPhoto = onToggleUnblurPhoto,
                    onOpenPhotoLightbox = onOpenPhotoLightbox,
                    onSwitchToIllustrated = { onSelectViewMode(DiversityViewMode.ILLUSTRATED_FEED) },
                    modifier = Modifier.weight(1f)
                )
            }
            DiversityViewMode.ILLUSTRATED_FEED -> {
                IllustratedDiversityFeedSection(
                    uiState = uiState,
                    onSelectTrait = onSelectTrait,
                    onLoadMore = onLoadMore,
                    onToggleStudyBookmark = onToggleStudyBookmark,
                    onOpenStudyDetail = onOpenStudyDetail,
                    onOpenSpectrumLab = onOpenSpectrumLab,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RealPhotoFeedSection(
    uiState: AtlasUiState,
    onConfirmGate: () -> Unit,
    onSelectCategory: (WikimediaPhotoCategory) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onTogglePrivacyBlur: () -> Unit,
    onToggleUnblurPhoto: (Long) -> Unit,
    onOpenPhotoLightbox: (WikimediaPhotoItem) -> Unit,
    onSwitchToIllustrated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { WikimediaMediaRepository.getImageLoader(context) }
    val listState = rememberLazyListState()

    val shouldLoadMore by remember(
        uiState.wikimediaPhotos.size,
        uiState.isLoadingPhotos,
        uiState.hasMorePhotos,
        uiState.hasConfirmedAdultEducationalGate,
        uiState.photoFeedError
    ) {
        derivedStateOf {
            if (!uiState.hasConfirmedAdultEducationalGate ||
                uiState.isLoadingPhotos ||
                !uiState.hasMorePhotos ||
                uiState.photoFeedError != null
            ) {
                false
            } else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val total = listState.layoutInfo.totalItemsCount
                total > 0 && lastVisible >= total - 4
            }
        }
    }

    LaunchedEffect(shouldLoadMore, uiState.selectedPhotoCategory) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    if (!uiState.hasConfirmedAdultEducationalGate) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("real_photo_consent_gate"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "18+ Real Educational Photo Feed",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Browse a continuous, paywall-free gallery of real, Creative Commons-licensed photographs of diverse human vulvas streamed directly from Wikimedia Commons right inside Bloom Atlas — no external websites or logins required.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Educational Notice: This gallery displays unretouched anatomical nudity strictly for adult (18+) body literacy, normalizing natural diversity, and sexual health education. An optional Privacy Blur mode is available inside the feed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Button(
                            onClick = onConfirmGate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_adult_photo_feed"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("I am 18+ · Open Real Photo Feed")
                        }

                        OutlinedButton(
                            onClick = onSwitchToIllustrated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("switch_to_illustrated_from_gate"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Browse Illustrated Feed & Spectrum Lab Instead")
                        }
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("real_photo_infinite_feed"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Category Chips + Privacy Blur Control
        item(key = "real_photo_controls") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item(key = "privacy_blur_chip") {
                        FilterChip(
                            selected = uiState.privacyBlurEnabled,
                            onClick = onTogglePrivacyBlur,
                            modifier = Modifier.testTag("toggle_privacy_blur_chip"),
                            leadingIcon = {
                                Icon(
                                    imageVector = if (uiState.privacyBlurEnabled) {
                                        Icons.Filled.BlurOn
                                    } else {
                                        Icons.Filled.BlurOff
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (uiState.privacyBlurEnabled) "Privacy Blur On" else "Privacy Blur",
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onTertiary
                            )
                        )
                    }

                    items(WikimediaPhotoCategory.entries) { category ->
                        val selected = uiState.selectedPhotoCategory == category
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCategory(category) },
                            modifier = Modifier.testTag("photo_category_${category.name.lowercase()}"),
                            label = {
                                Text(
                                    text = category.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Text(
                    text = uiState.selectedPhotoCategory.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Initial Loading State
        if (uiState.wikimediaPhotos.isEmpty() && uiState.isLoadingPhotos) {
            item(key = "real_photo_initial_loading") {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 42.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Loading real Wikimedia Commons photographs…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Real Photo Cards
        items(
            items = uiState.wikimediaPhotos,
            key = { "${uiState.selectedPhotoCategory.name}_${it.pageId}" }
        ) { photo ->
            val isBlurred = uiState.privacyBlurEnabled && photo.pageId !in uiState.unblurredPhotoIds
            WikimediaPhotoCard(
                photo = photo,
                isBlurred = isBlurred,
                imageLoader = imageLoader,
                onTapPhoto = {
                    if (isBlurred) {
                        onToggleUnblurPhoto(photo.pageId)
                    } else {
                        onOpenPhotoLightbox(photo)
                    }
                },
                onReblur = { onToggleUnblurPhoto(photo.pageId) },
                privacyModeActive = uiState.privacyBlurEnabled
            )
        }

        // Error State with Retry
        if (uiState.photoFeedError != null) {
            item(key = "real_photo_error") {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = uiState.photoFeedError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.testTag("retry_real_photos_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Loading Photos")
                        }
                    }
                }
            }
        }

        // Pagination Footer Spinner
        if (uiState.isLoadingPhotos && uiState.wikimediaPhotos.isNotEmpty()) {
            item(key = "real_photo_loading_more") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Loading more real photographs…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (!uiState.hasMorePhotos && uiState.wikimediaPhotos.isNotEmpty()) {
            item(key = "real_photo_end_of_category") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "End of ${uiState.selectedPhotoCategory.label} · Switch categories above to keep exploring",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item(key = "real_photo_bottom_spacer") { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun WikimediaPhotoCard(
    photo: WikimediaPhotoItem,
    isBlurred: Boolean,
    privacyModeActive: Boolean,
    imageLoader: coil.ImageLoader,
    onTapPhoto: () -> Unit,
    onReblur: () -> Unit
) {
    val context = LocalContext.current

    Card(
        onClick = onTapPhoto,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("real_photo_card_${photo.pageId}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(photo.aspectRatio)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = remember(photo.thumbUrl) {
                        ImageRequest.Builder(context)
                            .data(photo.thumbUrl)
                            .crossfade(true)
                            .build()
                    },
                    imageLoader = imageLoader,
                    contentDescription = photo.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isBlurred) Modifier.blur(26.dp) else Modifier),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    error = {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BrokenImage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Preview unavailable",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                // License Pill Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.62f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = photo.license,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Zoom hint pill or Privacy Blur overlay
                if (isBlurred) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Tap to Reveal Photo",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.56f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ZoomIn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Tap to Zoom",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = photo.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (privacyModeActive && !isBlurred) {
                        IconButton(
                            onClick = onReblur,
                            modifier = Modifier.testTag("reblur_photo_${photo.pageId}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BlurOn,
                                contentDescription = "Re-blur photo",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = photo.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Wikimedia Commons · ${photo.author}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IllustratedDiversityFeedSection(
    uiState: AtlasUiState,
    onSelectTrait: (DiversityTrait) -> Unit,
    onLoadMore: () -> Unit,
    onToggleStudyBookmark: (Int) -> Unit,
    onOpenStudyDetail: (VulvaStudy) -> Unit,
    onOpenSpectrumLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val shouldLoadMore by remember(uiState.diversityFeed.size) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 4
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("diversity_infinite_feed"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.diversity_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onOpenSpectrumLab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_spectrum_lab_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.diversity_open_spectrum_lab))
                }
            }
        }

        // Trait Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(DiversityTrait.entries) { trait ->
                    val selected = uiState.selectedTrait == trait
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectTrait(trait) },
                        modifier = Modifier.testTag("trait_filter_${trait.name.lowercase()}"),
                        label = {
                            Text(
                                text = trait.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                }
            }
        }

        items(
            items = uiState.diversityFeed,
            key = { "${uiState.selectedTrait.name}_${it.studyNumber}" }
        ) { study ->
            val isBookmarked = study.studyNumber in uiState.bookmarkedStudyNumbers
            DiversityFeedCard(
                study = study,
                isBookmarked = isBookmarked,
                onBookmarkToggle = { onToggleStudyBookmark(study.studyNumber) },
                onClick = { onOpenStudyDetail(study) }
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.diversity_loading_more),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiversityFeedCard(
    study: VulvaStudy,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onClick: () -> Unit
) {
    val palette = DiversityStudyGenerator.naturalPalettes[
        study.paletteIndex.coerceIn(0, DiversityStudyGenerator.naturalPalettes.lastIndex)
    ]

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diversity_study_card_${study.studyNumber}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.12f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                DiversityStudyCanvas(
                    innerLeftProjection = study.innerLeftProjection,
                    innerRightProjection = study.innerRightProjection,
                    outerFullness = study.outerFullness,
                    hoodCoverage = study.hoodCoverage,
                    ruffleFactor = study.ruffleFactor,
                    verticalLength = study.verticalLength,
                    paletteIndex = study.paletteIndex,
                    seed = study.seed
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = palette.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.testTag("bookmark_study_${study.studyNumber}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = stringResource(
                                if (isBookmarked) R.string.cd_bookmark_remove else R.string.cd_bookmark_add
                            ),
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = study.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    study.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = study.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = study.anatomicalNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
