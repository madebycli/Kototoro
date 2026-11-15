package org.skepsun.kototoro.details.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.skepsun.kototoro.R
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblerService
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblingInfo

@Composable
internal fun TrackingDialog(
    entries: List<ScrobblingInfo>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (ScrobblerService) -> Unit,
) {
    val statuses = stringArrayResource(R.array.scrobbling_statuses)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tracking)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (entries.isEmpty()) Text(stringResource(R.string.tracking_no_links))
                entries.forEach { entry ->
                    Column(Modifier.fillMaxWidth().clickable { onOpen(entry.scrobbler) }.padding(vertical = 12.dp)) {
                        Text(stringResource(entry.scrobbler.titleResId), style = MaterialTheme.typography.titleMedium)
                        Text(entry.title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            listOfNotNull(
                                entry.status?.let { statuses.getOrNull(it.ordinal) },
                                stringResource(R.string.tracking_progress_value, entry.chapter),
                                stringResource(R.string.tracking_score_value, (entry.rating * 100).toInt()),
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onAdd) { Text(stringResource(R.string.tracking_add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
