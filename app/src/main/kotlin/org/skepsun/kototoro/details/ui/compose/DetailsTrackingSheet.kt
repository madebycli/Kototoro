package org.skepsun.kototoro.details.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.skepsun.kototoro.R
import org.skepsun.kototoro.core.ui.adaptive.TvTextButton
import org.skepsun.kototoro.core.ui.adaptive.tvFocusable
import org.skepsun.kototoro.core.ui.compose.rememberSafePainter
import org.skepsun.kototoro.details.ui.model.LinkedTrackingItemUiModel
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblerService
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblingInfo
import org.skepsun.kototoro.scrobbling.common.ui.scrobblerIconTint
import java.util.Locale

/**
 * The details page's tracking entry point: one row per tracker the user is signed in to, showing the linked entry's
 * status, progress and scores, or offering to link one. Linking and editing only touch the tracker entry; the
 * title's metadata and reading source stay as they are.
 */
@Composable
internal fun DetailsTrackingSheet(
    services: List<ScrobblerService>,
    entries: List<ScrobblingInfo>,
    linkedTrackingItems: List<LinkedTrackingItemUiModel>,
    onDismissRequest: () -> Unit,
    onOpenEntry: (ScrobblerService) -> Unit,
    onLink: (ScrobblerService) -> Unit,
    onManageAccounts: () -> Unit,
) {
    val statuses = stringArrayResource(R.array.scrobbling_statuses)
    DetailsTranslucentBottomSheet(onDismissRequest = onDismissRequest) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "header") {
                Column(
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tracking),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = stringResource(
                            if (services.isEmpty()) R.string.tracking_no_accounts else R.string.tracking_sheet_summary,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(services, key = { it.id }) { service ->
                val entry = entries.firstOrNull { it.scrobbler == service }
                TrackingServiceRow(
                    service = service,
                    entry = entry,
                    communityScore = entry?.let { linked ->
                        linkedTrackingItems.firstOrNull {
                            it.service == service && it.remoteId == linked.targetId
                        }?.communityScore
                    },
                    statuses = statuses,
                    onClick = { if (entry != null) onOpenEntry(service) else onLink(service) },
                )
            }
            item(key = "accounts") {
                TvTextButton(
                    onClick = onManageAccounts,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(stringResource(R.string.tracking_accounts))
                }
            }
        }
        Box(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun TrackingServiceRow(
    service: ScrobblerService,
    entry: ScrobblingInfo?,
    communityScore: Float?,
    statuses: Array<String>,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(shape = shape, addFocusTarget = false),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = rememberSafePainter(service.iconResId),
                contentDescription = null,
                tint = scrobblerIconTint(service.iconResId),
                modifier = Modifier.size(36.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(service.titleResId),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry == null) {
                    Text(
                        text = stringResource(R.string.tracking_not_tracked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(
                            entry.status?.let { statuses.getOrNull(it.ordinal) },
                            stringResource(R.string.tracking_progress_value, entry.chapter),
                            entry.rating.takeIf { it > 0f }?.let {
                                stringResource(R.string.tracking_score_value, formatTrackingScore(it * 10f))
                            },
                            communityScore?.let {
                                stringResource(R.string.tracking_community_score_value, formatTrackingScore(it))
                            },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (entry == null) {
                FilledTonalButton(onClick = onClick) {
                    Text(stringResource(R.string.add))
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A 0–10 score with one decimal when it has one: `8`, `8.5`. */
internal fun formatTrackingScore(score: Float, locale: Locale = Locale.getDefault()): String {
    val rounded = Math.round(score * 10f) / 10f
    return if (rounded % 1f == 0f) {
        rounded.toInt().toString()
    } else {
        String.format(locale, "%.1f", rounded)
    }
}
