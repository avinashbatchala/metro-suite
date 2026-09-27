package com.metro.launcher.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.metro.launcher.R
import com.metro.launcher.data.DisplayTile
import com.metro.launcher.data.PinnedTileEntry
import com.metro.launcher.data.PinnedTileSize
import com.metro.launcher.data.TileBackgroundMode
import com.metro.launcher.data.TileCustomIcon
import com.metro.launcher.data.TileWidgetOption
import com.metro.launcher.data.supportsCustomWidget
import com.metro.system.MetroAccentPalette
import com.metro.system.MetroPreferences
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroListPickerOption
import com.metro.ui.MetroPivot
import com.metro.ui.MetroSlider
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Preview square matching Settings / lockscreen choose-photo thumbs. */
private val TileIconThumbSize = 108.dp

private val TileIconPlaceholderGray = Color(0xFF6E6E6E)

/**
 * Draft values for the tile customize page — applied only when Save is tapped.
 */
data class TileCustomizeDraft(
    val backgroundMode: TileBackgroundMode,
    val customBackgroundHex: String?,
    val launchTargetPackage: String?,
    val useCustomIcon: Boolean,
    val iconScale: Float,
    val customTitle: String,
    val hideTitle: Boolean,
    val useCustomWidget: Boolean,
    val widgetProvider: String?,
)

@Composable
fun TileCustomizeScreen(
    tile: DisplayTile,
    draft: TileCustomizeDraft,
    onDraftChange: (TileCustomizeDraft) -> Unit,
    onOpenColorPicker: () -> Unit,
    onOpenLaunchTargetPicker: () -> Unit,
    onRequestIconPick: () -> Unit,
    customIconReloadEpoch: Int,
    launchTargetLabel: String,
    modifier: Modifier = Modifier,
) {
    val widgetController = LocalTileAppWidgetController.current
    val showWidgetSection = tile.entry.supportsCustomWidget()
    var widgetOptions by remember(tile.entry.packageName) {
        mutableStateOf<List<TileWidgetOption>>(emptyList())
    }
    var widgetsLoading by remember(tile.entry.packageName) { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val tabCustomise = stringResource(R.string.tile_customize_tab_customise)
    val tabWidgets = stringResource(R.string.tile_customize_tab_widgets)
    val tabTiles = stringResource(R.string.tile_customize_tab_tiles)
    val pivotTitles = remember(tabCustomise, tabWidgets, tabTiles) {
        listOf(tabCustomise, tabWidgets, tabTiles)
    }

    // Defer AppWidgetManager + preview decode until the toggle is on — scanning providers
    // on open blocked the customize pivot / app-bar enter.
    LaunchedEffect(draft.useCustomWidget, tile.entry.packageName, widgetController) {
        if (!draft.useCustomWidget || widgetController == null) {
            widgetOptions = emptyList()
            widgetsLoading = false
            return@LaunchedEffect
        }
        widgetsLoading = true
        val options = withContext(Dispatchers.IO) {
            widgetController.listProvidersForPackage(tile.entry.packageName)
        }
        widgetOptions = options
        widgetsLoading = false
        // Auto-select the first provider when enabling with nothing chosen yet.
        if (draft.widgetProvider.isNullOrBlank() && options.isNotEmpty()) {
            onDraftChange(
                draft.copy(widgetProvider = options.first().provider.flattenToString()),
            )
        }
    }

    val backgroundOptions = listOf(
        MetroListPickerOption(
            TileBackgroundMode.Default,
            stringResource(R.string.tile_customize_bg_default),
        ),
        MetroListPickerOption(
            TileBackgroundMode.Accent,
            stringResource(R.string.tile_customize_bg_accent),
        ),
        MetroListPickerOption(
            TileBackgroundMode.Custom,
            stringResource(R.string.tile_customize_bg_custom),
        ),
    )
    val customHex = draft.customBackgroundHex
        ?.let { MetroAccentPalette.normalizeHex(it) }
    val customColor = customHex?.let { MetroPreferences.parseAccentHex(it) }
        ?: MetroTheme.colors.accent
    val customName = customHex?.let { MetroAccentPalette.displayName(it) }
        ?: stringResource(R.string.tile_customize_bg_custom)

    val startBackground = LocalStartBackgroundViewport.current
    val draftRevealsWindow = when (draft.backgroundMode) {
        TileBackgroundMode.Custom -> false
        TileBackgroundMode.Accent -> startBackground != null
        TileBackgroundMode.Default -> tile.revealsStartBackground && startBackground != null
    }
    val draftTileFill = when (draft.backgroundMode) {
        TileBackgroundMode.Accent -> MetroTheme.colors.accent
        TileBackgroundMode.Custom -> customColor
        TileBackgroundMode.Default -> tile.backgroundColor
    }
    val tileAspect = when (tile.entry.size) {
        PinnedTileSize.FourByTwo -> 2f
        else -> 1f
    }

    MetroPivot(
        titles = pivotTitles,
        pagerState = pagerState,
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
        header = {
            // App-title overline above pivot tabs (Settings / hub pattern).
            MetroAppTitle(title = tile.title)
        },
        onTitleClick = { index ->
            scope.launch { pagerState.animateScrollToPage(index) }
        },
    ) { page ->
        when (page) {
            0 -> CustomisePage(
                tile = tile,
                draft = draft,
                onDraftChange = onDraftChange,
                onOpenColorPicker = onOpenColorPicker,
                onOpenLaunchTargetPicker = onOpenLaunchTargetPicker,
                onRequestIconPick = onRequestIconPick,
                customIconReloadEpoch = customIconReloadEpoch,
                launchTargetLabel = launchTargetLabel,
                backgroundOptions = backgroundOptions,
                customColor = customColor,
                customName = customName,
            )
            1 -> WidgetsCustomizePage(
                draft = draft,
                onDraftChange = onDraftChange,
                showWidgetSection = showWidgetSection,
                widgetsLoading = widgetsLoading,
                widgetOptions = widgetOptions,
                tileAspect = tileAspect,
                draftRevealsWindow = draftRevealsWindow,
                draftTileFill = draftTileFill,
            )
            else -> TilesCustomizePage()
        }
    }
}

@Composable
private fun CustomisePage(
    tile: DisplayTile,
    draft: TileCustomizeDraft,
    onDraftChange: (TileCustomizeDraft) -> Unit,
    onOpenColorPicker: () -> Unit,
    onOpenLaunchTargetPicker: () -> Unit,
    onRequestIconPick: () -> Unit,
    customIconReloadEpoch: Int,
    launchTargetLabel: String,
    backgroundOptions: List<MetroListPickerOption<TileBackgroundMode>>,
    customColor: Color,
    customName: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 88.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        MetroText(
            text = stringResource(R.string.tile_customize_app_name_label),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                bottom = 4.dp,
            ),
        )
        MetroTextBox(
            value = draft.customTitle,
            onValueChange = { text ->
                onDraftChange(draft.copy(customTitle = text))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        Spacer(modifier = Modifier.height(28.dp))
        MetroListPicker(
            selected = draft.backgroundMode,
            options = backgroundOptions,
            onSelectedChange = { mode ->
                onDraftChange(draft.copy(backgroundMode = mode))
            },
            label = stringResource(R.string.tile_customize_bg_label),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        if (draft.backgroundMode == TileBackgroundMode.Custom) {
            Spacer(modifier = Modifier.height(20.dp))
            MetroText(
                text = stringResource(R.string.tile_customize_color_picker),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    end = MetroDimens.ScreenHorizontalMargin,
                    bottom = 4.dp,
                ),
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .border(2.dp, MetroTheme.colors.primaryText)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenColorPicker,
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(customColor),
                )
                Spacer(modifier = Modifier.width(12.dp))
                MetroText(
                    text = customName,
                    style = MetroTextStyle.Body,
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        MetroListPicker(
            selected = draft.launchTargetPackage,
            options = emptyList<MetroListPickerOption<String?>>(),
            onSelectedChange = {},
            label = stringResource(R.string.tile_customize_launch_target_label),
            placeholder = launchTargetLabel,
            onOpen = onOpenLaunchTargetPicker,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        Spacer(modifier = Modifier.height(28.dp))
        TileIconImagePickerRow(
            packageName = tile.entry.packageName,
            tileId = tile.entry.tileId,
            tileSize = tile.entry.size,
            enabled = draft.useCustomIcon,
            reloadEpoch = customIconReloadEpoch,
            onChoosePhoto = onRequestIconPick,
            onRemove = {
                onDraftChange(draft.copy(useCustomIcon = false))
            },
        )

        Spacer(modifier = Modifier.height(28.dp))
        MetroText(
            text = stringResource(R.string.tile_customize_icon_scale_label),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                bottom = 4.dp,
            ),
        )
        MetroSlider(
            value = draft.iconScale,
            onValueChange = { scale ->
                onDraftChange(
                    draft.copy(iconScale = PinnedTileEntry.clampIconScale(scale)),
                )
            },
            valueRange = PinnedTileEntry.MIN_ICON_SCALE..PinnedTileEntry.MAX_ICON_SCALE,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        // 1×1 faces never draw an app-name footer — only medium / wide.
        if (tile.entry.size != PinnedTileSize.OneByOne) {
            Spacer(modifier = Modifier.height(28.dp))
            MetroToggleSwitch(
                checked = draft.hideTitle,
                onCheckedChange = { hide ->
                    onDraftChange(draft.copy(hideTitle = hide))
                },
                label = stringResource(R.string.tile_customize_hide_title_label),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
            )
        }
    }
}

/**
 * WP8.1 Start-background-style choose photo row for the tile icon.
 */
@Composable
private fun TileIconImagePickerRow(
    packageName: String,
    tileId: String,
    tileSize: PinnedTileSize,
    enabled: Boolean,
    reloadEpoch: Int,
    onChoosePhoto: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var thumbBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val thumbAspect = when (tileSize) {
        PinnedTileSize.FourByTwo -> 2f
        else -> 1f
    }

    LaunchedEffect(enabled, reloadEpoch, packageName, tileId) {
        thumbBitmap = if (enabled) {
            withContext(Dispatchers.IO) {
                TileCustomIcon.decodeForPreview(context, packageName, tileId)
            }
        } else {
            null
        }
    }

    Row(
        modifier = modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .height(TileIconThumbSize)
                .aspectRatio(thumbAspect)
                .background(TileIconPlaceholderGray),
        ) {
            thumbBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = stringResource(R.string.tile_customize_icon_label),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f, fill = false)) {
            MetroText(
                text = stringResource(R.string.tile_customize_icon_label),
                style = MetroTextStyle.Body,
            )
            Spacer(modifier = Modifier.size(10.dp))
            MetroBorderButton(
                text = stringResource(R.string.tile_customize_choose_photo),
                onClick = onChoosePhoto,
            )
            if (enabled) {
                Spacer(modifier = Modifier.size(12.dp))
                val removeLabel = stringResource(R.string.tile_customize_remove_photo)
                MetroText(
                    text = remember(removeLabel) {
                        buildAnnotatedString {
                            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                append(removeLabel)
                            }
                        }
                    },
                    style = MetroTextStyle.Body,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                TileCustomIcon.clearDraft(context, packageName, tileId)
                                onRemove()
                            },
                        )
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun WidgetsCustomizePage(
    draft: TileCustomizeDraft,
    onDraftChange: (TileCustomizeDraft) -> Unit,
    showWidgetSection: Boolean,
    widgetsLoading: Boolean,
    widgetOptions: List<TileWidgetOption>,
    tileAspect: Float,
    draftRevealsWindow: Boolean,
    draftTileFill: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 88.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        if (!showWidgetSection) {
            MetroText(
                text = stringResource(R.string.tile_customize_widget_empty),
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
            )
            return
        }

        MetroToggleSwitch(
            checked = draft.useCustomWidget,
            onCheckedChange = { enabled ->
                onDraftChange(
                    draft.copy(
                        useCustomWidget = enabled,
                        widgetProvider = if (enabled) draft.widgetProvider else null,
                    ),
                )
            },
            label = stringResource(R.string.tile_customize_widget_label),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        if (draft.useCustomWidget) {
            Spacer(modifier = Modifier.height(12.dp))
            when {
                widgetsLoading -> Unit
                widgetOptions.isEmpty() -> {
                    MetroText(
                        text = stringResource(R.string.tile_customize_widget_empty),
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                        modifier = Modifier.padding(
                            horizontal = MetroDimens.ScreenHorizontalMargin,
                        ),
                    )
                }
                else -> {
                    MetroText(
                        text = stringResource(R.string.tile_customize_widget_pick),
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.accent,
                        modifier = Modifier.padding(
                            start = MetroDimens.ScreenHorizontalMargin,
                            end = MetroDimens.ScreenHorizontalMargin,
                            bottom = 8.dp,
                        ),
                    )
                    widgetOptions.forEach { option ->
                        WidgetProviderPreviewTile(
                            option = option,
                            selected = draft.widgetProvider ==
                                option.provider.flattenToString(),
                            aspectRatio = tileAspect,
                            useWindowFill = draftRevealsWindow,
                            tileFill = draftTileFill,
                            onClick = {
                                onDraftChange(
                                    draft.copy(
                                        widgetProvider = option.provider.flattenToString(),
                                    ),
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = MetroDimens.ScreenHorizontalMargin,
                                    vertical = 8.dp,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TilesCustomizePage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 24.dp,
            ),
    ) {
        MetroText(
            text = stringResource(R.string.tile_customize_tiles_empty),
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
        )
    }
}

/**
 * Selectable widget option framed as a Start tile — transparent wallpaper window or solid
 * Metro fill, matching how the launcher hosts the widget after Save.
 */
@Composable
private fun WidgetProviderPreviewTile(
    option: TileWidgetOption,
    selected: Boolean,
    aspectRatio: Float,
    useWindowFill: Boolean,
    tileFill: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Selected border is thicker so the pick reads clearly on wallpaper previews.
    val borderWidth = if (selected) 4.dp else 2.dp
    val borderColor = if (selected) MetroTheme.colors.accent else MetroTheme.colors.primaryText
    val preview = remember(option.previewBitmap) {
        option.previewBitmap?.asImageBitmap()
    }
    val startBackground = LocalStartBackgroundViewport.current
    val chrome = TileChrome.Standard
    val titleColor = if (useWindowFill) Color.White else MetroTheme.colors.primaryText

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .then(
                if (useWindowFill) {
                    Modifier.drawStartBackgroundWindow(startBackground)
                } else {
                    Modifier.background(tileFill)
                },
            )
            .border(borderWidth, borderColor),
    ) {
        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = option.label,
                // Fit keeps provider art inside the tile without inventing a Material card plate.
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.Medium,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Bottom-left app title — same placement as Start tile faces.
        TileText(
            text = option.label,
            style = chrome.titleStyle,
            color = titleColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    horizontal = chrome.titlePaddingH,
                    vertical = chrome.titlePaddingV,
                ),
        )
    }
}
