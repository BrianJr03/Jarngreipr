package jr.brian.home.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.data.NotificationItem
import jr.brian.home.data.NowPlayingManager
import jr.brian.home.ui.CompactDrawerOptionsContent
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.util.getSimpleBatteryInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DISMISS_THRESHOLD = -120f

@Composable
fun NotificationShade(
    volume: Float,
    duration: Long,
    visible: Boolean,
    currentPosition: Long,
    nowPlaying: NowPlayingManager.NowPlayingInfo?,
    modifier: Modifier = Modifier,
    notifications: List<NotificationItem> = emptyList(),
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onDismissNotification: (String) -> Unit = {},
    onNotificationClick: (NotificationItem) -> Unit = {},
    onClearAllNotifications: () -> Unit = {},
    onSeeAllNotifications: () -> Unit = {},
    onPowerClick: () -> Unit = {},
    onTabsClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onQuickDeleteClick: () -> Unit = {},
    onDockSettingsClick: () -> Unit = {},
    onESDESetupClick: () -> Unit = {},
    onNavigateToSystemApps: () -> Unit = {},
    onNavigateToRomSearch: () -> Unit = {},
    onCreateFolderClick: (() -> Unit)? = null,
    showDrawerOptionsPage: Boolean = true,
    initialTabPage: Int = 0,
    onTabPageChange: (Int) -> Unit = {}
) {
    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 0.6f else 0f,
        animationSpec = tween(300),
        label = "scrim_alpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(250)
            ) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ShadeCard(
                nowPlaying = nowPlaying,
                currentPosition = currentPosition,
                duration = duration,
                volume = volume,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onVolumeChange = onVolumeChange,
                onSeek = onSeek,
                onDismiss = onDismiss,
                onSettingsClick = onSettingsClick,
                notifications = notifications,
                onDismissNotification = onDismissNotification,
                onNotificationClick = onNotificationClick,
                onClearAllNotifications = onClearAllNotifications,
                onSeeAllNotifications = onSeeAllNotifications,
                onPowerClick = onPowerClick,
                onTabsClick = onTabsClick,
                onMenuClick = onMenuClick,
                onQuickDeleteClick = onQuickDeleteClick,
                onDockSettingsClick = onDockSettingsClick,
                onESDESetupClick = onESDESetupClick,
                onNavigateToSystemApps = onNavigateToSystemApps,
                onNavigateToRomSearch = onNavigateToRomSearch,
                onCreateFolderClick = onCreateFolderClick,
                showDrawerOptionsPage = showDrawerOptionsPage,
                initialTabPage = initialTabPage,
                onTabPageChange = onTabPageChange
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShadeCard(
    nowPlaying: NowPlayingManager.NowPlayingInfo?,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    notifications: List<NotificationItem>,
    onDismissNotification: (String) -> Unit,
    onNotificationClick: (NotificationItem) -> Unit,
    onClearAllNotifications: () -> Unit,
    onSeeAllNotifications: () -> Unit,
    onPowerClick: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit,
    onQuickDeleteClick: () -> Unit,
    onDockSettingsClick: () -> Unit,
    onESDESetupClick: () -> Unit,
    onNavigateToSystemApps: () -> Unit,
    onNavigateToRomSearch: () -> Unit,
    onCreateFolderClick: (() -> Unit)?,
    showDrawerOptionsPage: Boolean,
    initialTabPage: Int,
    onTabPageChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val onDismissState = rememberUpdatedState(onDismiss)
    val dragOffsetYState = remember { mutableFloatStateOf(0f) }
    var dragOffsetY by dragOffsetYState
    var batteryPercentage by remember { mutableIntStateOf(0) }
    var isCharging by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf("") }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f) {
                    dragOffsetYState.floatValue = (dragOffsetYState.floatValue + available.y).coerceAtMost(60f)
                    return available
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f) {
                    dragOffsetYState.floatValue += available.y
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dragOffsetYState.floatValue < DISMISS_THRESHOLD) onDismissState.value()
                dragOffsetYState.floatValue = 0f
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                dragOffsetYState.floatValue = 0f
                return Velocity.Zero
            }
        }
    }

    val pageCount = if (showDrawerOptionsPage) 3 else 2
    val pagerState = rememberPagerState(initialPage = initialTabPage, pageCount = { pageCount })

    LaunchedEffect(pagerState.currentPage) {
        onTabPageChange(pagerState.currentPage)
    }
    var page0HeightPx by remember { mutableIntStateOf(0) }
    var page1HeightPx by remember { mutableIntStateOf(0) }
    var page2HeightPx by remember { mutableIntStateOf(0) }
    val pagerHeightDp = with(density) { maxOf(page0HeightPx, page1HeightPx, page2HeightPx).toDp() }

    LaunchedEffect(Unit) {
        val (pct, charging) = context.getSimpleBatteryInfo()
        batteryPercentage = pct
        isCharging = charging
        currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp)
            .graphicsLayer {
                translationY = dragOffsetY.coerceAtLeast(0f)
            }
            .nestedScroll(nestedScrollConnection)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111111))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShadeHeader(
                batteryPercentage = batteryPercentage,
                isCharging = isCharging,
                currentTime = currentTime,
                onSettingsClick = onSettingsClick,
                onDismiss = onDismiss
            )

            ShadePagerIndicator(currentPage = pagerState.currentPage, pageCount = pageCount)

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (pagerHeightDp > 0.dp) Modifier.height(pagerHeightDp) else Modifier)
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            when (page) {
                                0 -> if (size.height > 0) page0HeightPx = size.height
                                1 -> if (size.height > 0) page1HeightPx = size.height
                                2 -> if (size.height > 0) page2HeightPx = size.height
                            }
                        }
                ) {
                    when (page) {
                        0 -> ActionsAndNotificationsPage(
                            notifications = notifications,
                            onDismissNotification = onDismissNotification,
                            onNotificationClick = onNotificationClick,
                            onClearAllNotifications = onClearAllNotifications,
                            onSeeAllNotifications = onSeeAllNotifications,
                        )

                        1 -> NowPlayingPage(
                            nowPlaying = nowPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            volume = volume,
                            onPlayPause = onPlayPause,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onVolumeChange = onVolumeChange,
                            onSeek = onSeek
                        )

                        2 -> CompactDrawerOptionsContent(
                            onDismiss = onDismiss,
                            onPowerClick = onPowerClick,
                            onTabsClick = onTabsClick,
                            onMenuClick = onMenuClick,
                            onQuickDeleteClick = onQuickDeleteClick,
                            onCreateFolderClick = onCreateFolderClick,
                            onDockSettingsClick = onDockSettingsClick,
                            onESDESetupClick = onESDESetupClick,
                            onNavigateToSystemApps = onNavigateToSystemApps,
                            onNavigateToRomSearch = onNavigateToRomSearch
                        )
                    }
                }
            }
        }

        ShadePill(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ShadeHeader(
    batteryPercentage: Int,
    isCharging: Boolean,
    currentTime: String,
    onSettingsClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        HeaderIconButton(
            icon = Icons.Default.Settings,
            contentDescription = stringResource(R.string.common_settings),
            onClick = onSettingsClick
        )
        Spacer(Modifier.weight(1f))

        if (isCharging) {
            Icon(
                imageVector = Icons.Default.BatteryChargingFull,
                contentDescription = null,
                tint = ThemePrimaryColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(2.dp))
        }
        Text(
            text = "$batteryPercentage%",
            color = ThemePrimaryColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = currentTime,
            color = ThemePrimaryColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.common_close),
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun ShadePagerIndicator(currentPage: Int, pageCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(
                        width = if (currentPage == index) 16.dp else 5.dp,
                        height = 5.dp
                    )
                    .clip(CircleShape)
                    .background(
                        if (currentPage == index) ThemePrimaryColor
                        else Color.White.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

@Composable
private fun ShadePill(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(bottom = 8.dp)
            .size(width = 36.dp, height = 4.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
    )
}

@Composable
internal fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String = "",
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = ThemePrimaryColor,
            modifier = Modifier.size(14.dp)
        )
    }
}
