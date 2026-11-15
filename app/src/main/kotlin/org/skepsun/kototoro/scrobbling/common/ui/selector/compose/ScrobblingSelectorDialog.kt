package org.skepsun.kototoro.scrobbling.common.ui.selector.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.recyclerview.widget.RecyclerView.NO_ID
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.skepsun.kototoro.R
import org.skepsun.kototoro.core.ui.adaptive.tvFocusable
import org.skepsun.kototoro.core.ui.adaptive.tvIconButtonRing
import org.skepsun.kototoro.core.ui.adaptive.tvTabRing
import org.skepsun.kototoro.core.ui.compose.rememberSafePainter
import org.skepsun.kototoro.core.ui.theme.KototoroTheme
import org.skepsun.kototoro.core.util.ext.getDisplayMessage
import org.skepsun.kototoro.list.ui.model.ListModel
import org.skepsun.kototoro.list.ui.model.LoadingFooter
import org.skepsun.kototoro.list.ui.model.LoadingState
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblerContent
import org.skepsun.kototoro.scrobbling.common.ui.scrobblerIconTint
import org.skepsun.kototoro.scrobbling.common.ui.selector.ScrobblingSelectorViewModel
import org.skepsun.kototoro.scrobbling.common.ui.selector.model.ScrobblerHint

/**
 * Finds the entry to link on a tracker: a search field filled with the title, the results with their covers and a
 * "Link" button for the picked one. With [showServiceTabs] off the sheet searches only the tracker it was opened
 * for, which is how the details page's tracking sheet uses it; otherwise tabs switch between all trackers.
 */
@Composable
fun ScrobblingSelectorDialog(
    viewModel: ScrobblingSelectorViewModel,
    onDismissRequest: () -> Unit,
    onRetry: (Throwable) -> Unit,
    showServiceTabs: Boolean = true,
) {
    val selectedIndex by viewModel.selectedScrobblerIndex.collectAsStateWithLifecycle()
    val selectedItemId by viewModel.selectedItemId.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val service = viewModel.availableScrobblers.getOrNull(selectedIndex)?.scrobblerService
    val requiresLogin = service != null && !viewModel.isScrobblerAuthorized(selectedIndex)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!showServiceTabs && service != null) {
                Icon(
                    painter = rememberSafePainter(service.iconResId),
                    contentDescription = null,
                    tint = scrobblerIconTint(service.iconResId),
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = if (!showServiceTabs && service != null) {
                    stringResource(R.string.tracking_search_on, stringResource(service.titleResId))
                } else {
                    stringResource(R.string.tracking_short)
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismissRequest, modifier = Modifier.tvIconButtonRing()) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
            }
        }

        ScrobblerSearchField(viewModel = viewModel)

        if (showServiceTabs) {
            ScrobblerTabs(viewModel)
        }
        if (requiresLogin) {
            Text(
                text = stringResource(R.string.scrobbler_search_auth_hint, stringResource(service.titleResId)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        ScrobblerListContent(
            viewModel = viewModel,
            onRetry = onRetry,
            modifier = Modifier.weight(1f),
        )

        Button(
            onClick = viewModel::onDoneClick,
            enabled = !isLoading && (requiresLogin || selectedItemId != NO_ID),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 12.dp)
                .heightIn(min = 52.dp),
        ) {
            if (isLoading && selectedItemId != NO_ID) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
            } else {
                Text(stringResource(if (requiresLogin) R.string.sign_in else R.string.tracking_link_action))
            }
        }
    }
}

/**
 * Search text for the current tracker. It starts as the title; a link to the entry or `id:123` picks that entry
 * directly. The search button or the keyboard's search key runs it.
 */
@Composable
private fun ScrobblerSearchField(viewModel: ScrobblingSelectorViewModel) {
    val searchedQuery by viewModel.query.collectAsStateWithLifecycle()
    var query by remember(searchedQuery) { mutableStateOf(searchedQuery) }
    val focusManager = LocalFocusManager.current
    val submit = {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            viewModel.search(trimmed)
            focusManager.clearFocus()
        }
    }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
        placeholder = { Text(stringResource(R.string.tracking_search_hint)) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }, modifier = Modifier.tvIconButtonRing()) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                    }
                }
                IconButton(
                    onClick = submit,
                    enabled = query.isNotBlank(),
                    modifier = Modifier.tvIconButtonRing(),
                ) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { submit() }),
    )
}

@Composable
private fun ScrobblerTabs(viewModel: ScrobblingSelectorViewModel) {
    val selectedIndex by viewModel.selectedScrobblerIndex.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val scrobblers = viewModel.availableScrobblers
    if (scrobblers.isEmpty()) return
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = Modifier.fillMaxWidth(),
        edgePadding = 16.dp,
        divider = {},
        containerColor = Color.Transparent,
    ) {
        scrobblers.forEachIndexed { index, scrobbler ->
            val title = stringResource(scrobbler.scrobblerService.titleResId)
            Tab(
                modifier = Modifier.tvTabRing(),
                selected = selectedIndex == index,
                onClick = { if (!isLoading) viewModel.setScrobblerIndex(index) },
                text = {
                    Text(
                        text = if (viewModel.isScrobblerAuthorized(index)) {
                            title
                        } else {
                            stringResource(
                                R.string.scrobbler_search_requires_login_label,
                                title,
                                stringResource(R.string.filter_need_login),
                            )
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                icon = {
                    Icon(
                        painter = rememberSafePainter(scrobbler.scrobblerService.iconResId),
                        contentDescription = null,
                        tint = scrobblerIconTint(scrobbler.scrobblerService.iconResId),
                        modifier = Modifier.size(24.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun ScrobblerListContent(
    viewModel: ScrobblingSelectorViewModel,
    onRetry: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items by viewModel.content.collectAsStateWithLifecycle()
    val selectedItemId by viewModel.selectedItemId.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val selectedIndex by viewModel.selectedScrobblerIndex.collectAsStateWithLifecycle()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            listState.layoutInfo.totalItemsCount > 0 && lastVisible >= listState.layoutInfo.totalItemsCount - 2
        }
    }

    LaunchedEffect(selectedIndex) {
        listState.scrollToItem(0)
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadNextPage()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = items,
            key = ::listItemKey,
            contentType = { it::class },
        ) { item ->
            when (item) {
                is ScrobblerContent -> ScrobblerContentItem(
                    item = item,
                    isSelected = item.id == selectedItemId,
                    onClick = { viewModel.selectItem(item.id) },
                )
                is ScrobblerHint -> ScrobblerHintItem(
                    item = item,
                    onRetry = { error -> error?.let(onRetry) },
                )
                is LoadingState -> LoadingItem()
                is LoadingFooter -> LoadingFooterItem()
            }
        }
    }
}

@Composable
private fun ScrobblerContentItem(
    item: ScrobblerContent,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(18.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(shape = shape, addFocusTarget = false),
        shape = shape,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
        },
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.cover)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = rememberSafePainter(R.drawable.ic_placeholder),
                error = rememberSafePainter(R.drawable.ic_placeholder),
                modifier = Modifier
                    .size(width = 52.dp, height = 74.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.isBestMatch) {
                        Icon(
                            painter = rememberSafePainter(R.drawable.ic_star_small),
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp).size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                item.altName?.takeIf { it.isNotBlank() && it != item.name }?.let { altName ->
                    Text(
                        text = altName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                listOfNotNull(item.subtitle, item.progressText)
                    .filter { it.isNotBlank() }
                    .takeIf { it.isNotEmpty() }
                    ?.let { facts ->
                        Text(
                            text = facts.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ScrobblerHintItem(
    item: ScrobblerHint,
    onRetry: (Throwable?) -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.margin_normal)),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = rememberSafePainter(item.icon),
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = Color.Unspecified,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = dimensionResource(R.dimen.margin_small)),
        ) {
            Text(text = stringResource(item.textPrimary), style = MaterialTheme.typography.titleMedium)
            val secondary = item.error?.getDisplayMessage(context.resources)
                ?: item.textSecondary.takeIf { it != 0 }?.let { context.getString(it) }
            secondary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.margin_small)),
                )
            }
            if (item.actionStringRes != 0 && item.error != null) {
                FilledTonalButton(
                    onClick = { onRetry(item.error) },
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.margin_small)),
                ) {
                    Text(stringResource(item.actionStringRes))
                }
            }
        }
    }
}

@Composable
private fun LoadingItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LoadingFooterItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

private fun listItemKey(item: ListModel): String = when (item) {
    is ScrobblerContent -> "content-${item.id}"
    is ScrobblerHint -> "hint-${item.textPrimary}"
    is LoadingState -> "loading"
    is LoadingFooter -> "loading-footer"
    else -> item::class.qualifiedName.orEmpty()
}

@Preview(showBackground = true)
@Composable
private fun ScrobblerContentItemPreview() {
    KototoroTheme {
        ScrobblerContentItem(
            item = ScrobblerContent(
                id = 1,
                name = "Sample title",
                altName = "Alternative title",
                cover = null,
                url = "https://example.com",
                isBestMatch = true,
            ),
            isSelected = true,
            onClick = {},
        )
    }
}
