package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.R
import com.example.data.model.AnatomyPart
import com.example.data.model.AnatomyViewType
import com.example.data.model.AudienceMode
import com.example.data.model.SensitivityTier
import com.example.ui.components.AnatomyHapticsEngine
import com.example.ui.components.InteractiveAnatomyCanvas
import com.example.ui.components.InteractiveVulva3DCanvas
import com.example.ui.components.InteractiveVulvaCanvas
import com.example.ui.components.VulvaToneStyles
import com.example.ui.viewmodel.AtlasUiState
import com.example.ui.viewmodel.VulvaDisplayMode

@Composable
fun AnatomyScreen(
    uiState: AtlasUiState,
    onSelectView: (AnatomyViewType) -> Unit,
    onSelectPart: (String) -> Unit,
    onToggleLabels: () -> Unit,
    onSelectVulvaDisplayMode: (VulvaDisplayMode) -> Unit,
    onToggleHoldVulvaOpen: () -> Unit,
    onResetVulvaTissue: () -> Unit,
    onSelectVulvaTone: (Int) -> Unit,
    onToggleMedicalPlates: () -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleTouchExplore: () -> Unit,
    onReadArticle: (String) -> Unit,
    onOpenDiversity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticsEngine = remember(context) { AnatomyHapticsEngine(context) }
    val isVulvaView = uiState.selectedView == AnatomyViewType.EXTERNAL_VULVA
    val isInteractiveDeformableMode = isVulvaView &&
        (uiState.vulvaDisplayMode == VulvaDisplayMode.STUDIO_3D || uiState.vulvaDisplayMode == VulvaDisplayMode.PAINTED_2D)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = !uiState.touchExploreMode,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Editorial Hero Header Banner
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_atlas_1795315100018),
                        contentDescription = stringResource(R.string.cd_hero_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.14f),
                                        Color(0xFF2B1310).copy(alpha = 0.80f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = stringResource(R.string.app_tagline),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                        Text(
                            text = "Use your own fingers to gently spread, lift the hood, or glide across anatomy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.90f)
                        )
                    }
                }
            }
        }

        // View Switcher Pills + Sensory Haptics & Plate Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = uiState.selectedView.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = uiState.selectedView.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Anatomy View Tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AnatomyViewType.entries) { viewType ->
                        val selected = uiState.selectedView == viewType
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectView(viewType) },
                            modifier = Modifier.testTag("anatomy_view_${viewType.name.lowercase()}"),
                            label = {
                                Text(
                                    text = viewType.shortLabel,
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

                // 3-Mode Renderer Switcher for External Vulva: 3D Studio | 2D Painted | Photo Ref
                if (isVulvaView) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VulvaDisplayMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = uiState.vulvaDisplayMode == mode,
                                onClick = { onSelectVulvaDisplayMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = VulvaDisplayMode.entries.size
                                ),
                                modifier = Modifier.testTag("vulva_mode_${mode.name.lowercase()}"),
                                icon = {
                                    Icon(
                                        imageVector = when (mode) {
                                            VulvaDisplayMode.STUDIO_3D -> Icons.Filled.ViewInAr
                                            VulvaDisplayMode.PAINTED_2D -> Icons.Filled.Brush
                                            VulvaDisplayMode.PHOTO_REFERENCE -> Icons.Filled.Photo
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            ) {
                                Text(
                                    text = mode.label,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                // Interactive Control Strip tailored to External Vulva vs. Other Views
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isVulvaView) {
                        if (isInteractiveDeformableMode) {
                            item {
                                FilterChip(
                                    selected = uiState.holdVulvaOpen,
                                    onClick = onToggleHoldVulvaOpen,
                                    modifier = Modifier.testTag("toggle_hold_vulva_open"),
                                    label = { Text(if (uiState.holdVulvaOpen) "Holding Open" else "Hold Open") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (uiState.holdVulvaOpen) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                            contentDescription = "Hold labia and hood open when lifting fingers",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }

                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = onResetVulvaTissue,
                                    modifier = Modifier.testTag("reset_vulva_tissue"),
                                    label = { Text("Reset") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.RestartAlt,
                                            contentDescription = "Reset Blossom's pose and tissue to resting state",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        item {
                            FilterChip(
                                selected = uiState.touchExploreMode,
                                onClick = onToggleTouchExplore,
                                modifier = Modifier.testTag("toggle_touch_explore"),
                                label = { Text(if (uiState.touchExploreMode) "Touch Explore: Active" else "Touch Explore") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.TouchApp,
                                        contentDescription = "Toggle finger drag exploration",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.useMedicalPlates,
                                onClick = onToggleMedicalPlates,
                                modifier = Modifier.testTag("toggle_medical_plate"),
                                label = { Text(if (uiState.useMedicalPlates) "Medical Plate" else "Vector Art") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.MenuBook,
                                        contentDescription = "Switch illustration style",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    item {
                        FilterChip(
                            selected = uiState.hapticsEnabled,
                            onClick = onToggleHaptics,
                            modifier = Modifier.testTag("toggle_haptics"),
                            label = { Text(if (uiState.hapticsEnabled) "Haptics On" else "Haptics Off") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Vibration,
                                    contentDescription = "Toggle haptic vibrations",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = uiState.showDiagramLabels,
                            onClick = onToggleLabels,
                            modifier = Modifier.testTag("toggle_diagram_labels"),
                            label = { Text(stringResource(R.string.anatomy_show_labels)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (uiState.showDiagramLabels) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = stringResource(R.string.cd_toggle_labels),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // Hero Interactive Anatomy Surface
        item {
            if (isInteractiveDeformableMode) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (uiState.vulvaDisplayMode == VulvaDisplayMode.STUDIO_3D) {
                        InteractiveVulva3DCanvas(
                            parts = uiState.currentViewParts,
                            selectedPartId = uiState.selectedPart?.id ?: "",
                            showLabels = uiState.showDiagramLabels,
                            hapticsEnabled = uiState.hapticsEnabled,
                            holdOpen = uiState.holdVulvaOpen,
                            resetTrigger = uiState.vulvaResetTrigger,
                            toneIndex = uiState.vulvaToneIndex,
                            onSelectPart = onSelectPart
                        )
                    } else {
                        InteractiveVulvaCanvas(
                            parts = uiState.currentViewParts,
                            selectedPartId = uiState.selectedPart?.id ?: "",
                            showLabels = uiState.showDiagramLabels,
                            hapticsEnabled = uiState.hapticsEnabled,
                            holdOpen = uiState.holdVulvaOpen,
                            resetTrigger = uiState.vulvaResetTrigger,
                            toneIndex = uiState.vulvaToneIndex,
                            onSelectPart = onSelectPart
                        )
                        Text(
                            text = "Spread labia apart with two fingers, lift the clitoral hood upward, or glide across to feel sensitivity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // Natural Skin-Tone Selector Swatches (applies live to both 3D Studio & 2D Painted)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Natural tone",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            itemsIndexed(VulvaToneStyles) { index, tone ->
                                val selected = index == uiState.vulvaToneIndex
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .testTag("vulva_tone_swatch_$index")
                                        .semantics {
                                            contentDescription = "Select ${tone.name} skin tone"
                                        }
                                        .clickable { onSelectVulvaTone(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(tone.skinMid)
                                            .border(
                                                width = if (selected) 2.5.dp else 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else tone.creaseShadow.copy(alpha = 0.45f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(tone.mucosaMid)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                InteractiveAnatomyCanvas(
                    viewType = uiState.selectedView,
                    parts = uiState.currentViewParts,
                    selectedPartId = uiState.selectedPart?.id ?: "",
                    showLabels = uiState.showDiagramLabels,
                    useMedicalPlate = if (isVulvaView) true else uiState.useMedicalPlates,
                    medicalPlate = uiState.currentMedicalPlate,
                    hapticsEnabled = uiState.hapticsEnabled,
                    touchExploreMode = uiState.touchExploreMode,
                    onSelectPart = onSelectPart
                )
            }
        }

        // Quick Numbered Legend Chips (also triggers region-specific haptic pulse!)
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.currentViewParts) { part ->
                    val isSelected = part.id == uiState.selectedPart?.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onSelectPart(part.id)
                            if (uiState.hapticsEnabled) {
                                hapticsEngine.triggerTap(part.sensitivityTier)
                            }
                        },
                        modifier = Modifier.testTag("legend_chip_${part.id}"),
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.primaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = part.number.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        },
                        label = { Text(part.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Detailed Structure Inspector Card
        uiState.selectedPart?.let { part ->
            item {
                AnimatedContent(targetState = part, label = "part_inspector") { activePart ->
                    StructureInspectorCard(
                        part = activePart,
                        audienceMode = uiState.audienceMode,
                        onReadArticle = onReadArticle
                    )
                }
            }
        }

        // Empowering Bridge Card to Real Photo Diversity Feed
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Every Vulva Is Unique",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Diagrams show landmarks—scroll the paywall-free Real Photo Feed in Diversity to see authentic human variation directly in-app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.88f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onOpenDiversity,
                            modifier = Modifier.testTag("anatomy_jump_diversity"),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(
                                text = "Open Real Photo Feed",
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun StructureInspectorCard(
    part: AnatomyPart,
    audienceMode: AudienceMode,
    onReadArticle: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("structure_inspector_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = part.number.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = part.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = part.clinicalName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Nerve Sensitivity Haptic Badge
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = when (part.sensitivityTier) {
                    SensitivityTier.ULTRA -> MaterialTheme.colorScheme.primaryContainer
                    SensitivityTier.HIGH -> MaterialTheme.colorScheme.tertiaryContainer
                    SensitivityTier.MODERATE -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Vibration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Nerve Sensitivity: ${part.sensitivityTier.label}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = part.sensitivityTier.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = part.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            InspectorInfoBlock(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.anatomy_section_function),
                body = part.functionText,
                accentColor = MaterialTheme.colorScheme.primary
            )

            InspectorInfoBlock(
                icon = Icons.Filled.Favorite,
                title = stringResource(R.string.anatomy_section_pleasure),
                body = part.pleasureNotes,
                accentColor = MaterialTheme.colorScheme.tertiary
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.anatomy_section_myth),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = part.mythBuster,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (audienceMode == AudienceMode.PARTNERS || audienceMode == AudienceMode.EDUCATORS) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.anatomy_section_partner),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = part.partnerTip,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Button(
                onClick = { onReadArticle(part.relatedArticleId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("anatomy_read_deep_dive_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.anatomy_read_deep_dive))
            }
        }
    }
}

@Composable
private fun InspectorInfoBlock(
    icon: ImageVector,
    title: String,
    body: String,
    accentColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
