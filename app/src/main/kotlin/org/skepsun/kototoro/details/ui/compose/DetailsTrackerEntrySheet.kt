package org.skepsun.kototoro.details.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import org.skepsun.kototoro.R
import org.skepsun.kototoro.core.ui.adaptive.TvTextButton
import org.skepsun.kototoro.core.ui.adaptive.tvChipRing
import org.skepsun.kototoro.core.ui.compose.rememberSafePainter
import org.skepsun.kototoro.core.util.ext.takeIfUsableImageUri
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblingInfo
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblingStatus
import org.skepsun.kototoro.scrobbling.common.ui.scrobblerIconTint
import kotlin.math.roundToInt

/** How long progress steps wait for the next tap before they are sent, so tapping + five times is one update. */
private const val PROGRESS_COMMIT_DELAY_MS = 700L

/**
 * One linked tracker entry: what the tracker counts (status, chapters read, score) and the controls to change it,
 * in the same single glass sheet as the details page's other sheets. Every change goes straight to the tracker; the
 * sheet shows the stored entry again once the service has accepted it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailsTrackerEntrySheet(
    entry: ScrobblingInfo,
    communityScore: Float?,
    totalChapters: Int?,
    countsEpisodes: Boolean,
    onDismissRequest: () -> Unit,
    onStatusChange: (ScrobblingStatus) -> Unit,
    onScoreChange: (Float) -> Unit,
    onProgressChange: (Int) -> Unit,
    onSync: () -> Unit,
    onOpenDetails: () -> Unit,
    onOpenInBrowser: (() -> Unit)?,
    onChangeLink: () -> Unit,
    onUnlink: () -> Unit,
) {
    val statuses = stringArrayResource(R.array.scrobbling_statuses)
    DetailsTranslucentBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TrackerEntryHeader(entry = entry, communityScore = communityScore, onOpenDetails = onOpenDetails)

            TrackerSection(title = stringResource(R.string.tracking_status)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    supportedStatusesForService(entry.scrobbler).forEach { status ->
                        val selected = entry.status == status
                        FilterChip(
                            selected = selected,
                            onClick = { if (!selected) onStatusChange(status) },
                            label = { Text(statuses.getOrElse(status.ordinal) { status.name }) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp)) }
                            } else {
                                null
                            },
                            modifier = Modifier.tvChipRing(),
                        )
                    }
                }
            }

            TrackerSection(title = stringResource(R.string.tracking_progress_edit)) {
                TrackerProgressControl(
                    progress = entry.chapter,
                    total = totalChapters?.takeIf { it > 0 },
                    countsEpisodes = countsEpisodes,
                    onProgressChange = onProgressChange,
                )
            }

            TrackerSection(title = stringResource(R.string.tracking_score)) {
                TrackerScoreControl(rating = entry.rating, onScoreChange = onScoreChange)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TrackerAction(Icons.Outlined.Sync, stringResource(R.string.tracking_sync_now), onSync)
                TrackerAction(Icons.Outlined.Info, stringResource(R.string.tracking_open_details), onOpenDetails)
                if (onOpenInBrowser != null) {
                    TrackerAction(Icons.AutoMirrored.Outlined.OpenInNew, stringResource(R.string.open_in_browser), onOpenInBrowser)
                }
                TrackerAction(Icons.Outlined.Edit, stringResource(R.string.tracking_change_link), onChangeLink)
                TrackerAction(Icons.Outlined.LinkOff, stringResource(R.string.tracking_unlink), onUnlink)
            }
        }
        Box(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun TrackerEntryHeader(
    entry: ScrobblingInfo,
    communityScore: Float?,
    onOpenDetails: () -> Unit,
) {
    val context = LocalContext.current
    val coverShape = RoundedCornerShape(12.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            onClick = onOpenDetails,
            shape = coverShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(width = 84.dp, height = 120.dp),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(entry.coverUrl.takeIfUsableImageUri())
                    .crossfade(true)
                    .build(),
                contentDescription = entry.title,
                contentScale = ContentScale.Crop,
                placeholder = rememberSafePainter(R.drawable.ic_placeholder),
                error = rememberSafePainter(R.drawable.ic_placeholder),
                modifier = Modifier.clip(coverShape),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = rememberSafePainter(entry.scrobbler.iconResId),
                    contentDescription = null,
                    tint = scrobblerIconTint(entry.scrobbler.iconResId),
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(entry.scrobbler.titleResId),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            communityScore?.let { score ->
                Text(
                    text = stringResource(R.string.tracking_community_score_value, formatTrackingScore(score)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TrackerSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        content()
    }
}

/**
 * Chapters read on the tracker, with -/+ steps that are sent together once the taps stop and a tap on the number to
 * type it.
 */
@Composable
private fun TrackerProgressControl(
    progress: Int,
    total: Int?,
    countsEpisodes: Boolean,
    onProgressChange: (Int) -> Unit,
) {
    var draft by remember(progress) { mutableIntStateOf(progress) }
    var isEditing by remember { mutableStateOf(false) }
    val currentOnProgressChange by rememberUpdatedState(onProgressChange)
    LaunchedEffect(draft, progress) {
        if (draft == progress) return@LaunchedEffect
        delay(PROGRESS_COMMIT_DELAY_MS)
        currentOnProgressChange(draft)
    }
    // Closing the sheet right after a tap must not drop the step that was still waiting.
    val latestDraft by rememberUpdatedState(draft)
    val latestProgress by rememberUpdatedState(progress)
    DisposableEffect(Unit) {
        onDispose {
            if (latestDraft != latestProgress) currentOnProgressChange(latestDraft)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = { draft = (draft - 1).coerceAtLeast(0) }, enabled = draft > 0) {
            Icon(Icons.Filled.Remove, contentDescription = null)
        }
        TvTextButton(
            onClick = { isEditing = true },
            modifier = Modifier.weight(1f),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = draft.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when {
                        total != null && countsEpisodes -> stringResource(R.string.tracking_progress_of_episodes, total)
                        total != null -> stringResource(R.string.tracking_progress_of, total)
                        countsEpisodes -> stringResource(R.string.tracking_progress_episodes)
                        else -> stringResource(R.string.tracking_progress_chapters)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FilledTonalIconButton(onClick = { draft += 1 }) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
    }
    if (isEditing) {
        TrackerProgressDialog(
            progress = draft,
            onDismissRequest = { isEditing = false },
            onConfirm = { value ->
                isEditing = false
                draft = value
            },
        )
    }
}

@Composable
private fun TrackerProgressDialog(
    progress: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(progress.toString()) }
    // Nine digits keep the value an Int; no tracker counts that many chapters or episodes.
    val parsed = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.tracking_progress_edit)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { value -> text = value.filter(Char::isDigit).take(9) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = { parsed?.let(onConfirm) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/** The user's score as 0–10 in steps of one, sent when the thumb is released. */
@Composable
private fun TrackerScoreControl(rating: Float, onScoreChange: (Float) -> Unit) {
    val stored = (rating.coerceIn(0f, 1f) * 10f).roundToInt().toFloat()
    var draft by remember(stored) { mutableFloatStateOf(stored) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Slider(
            value = draft,
            onValueChange = { draft = it.roundToInt().toFloat() },
            onValueChangeFinished = { if (draft != stored) onScoreChange(draft / 10f) },
            valueRange = 0f..10f,
            steps = 9,
            modifier = Modifier.weight(1f),
        )
        Text(
            // Until the thumb moves, show the stored score as the tracker has it, decimals included.
            text = when {
                draft != stored -> if (draft > 0f) draft.roundToInt().toString() else "–"
                rating > 0f -> formatTrackingScore(rating.coerceIn(0f, 1f) * 10f)
                else -> "–"
            },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.width(40.dp),
        )
    }
}

@Composable
private fun TrackerAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.height(40.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, maxLines = 1)
    }
}
