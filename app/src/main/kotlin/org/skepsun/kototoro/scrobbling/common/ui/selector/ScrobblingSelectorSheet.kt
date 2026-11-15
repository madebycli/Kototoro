package org.skepsun.kototoro.scrobbling.common.ui.selector

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import org.skepsun.kototoro.core.exceptions.resolve.ExceptionResolver
import org.skepsun.kototoro.core.ui.BaseComposeActivity
import org.skepsun.kototoro.core.ui.theme.KototoroTheme
import org.skepsun.kototoro.details.ui.compose.DetailsTranslucentBottomSheet
import org.skepsun.kototoro.core.util.ext.findActivity
import org.skepsun.kototoro.core.util.ext.getDisplayMessage
import org.skepsun.kototoro.parsers.model.Content
import org.skepsun.kototoro.scrobbling.common.ui.selector.compose.ScrobblingSelectorDialog

@Composable
fun ScrobblingSelectorSheetRoute(
    manga: Content,
    scrobblerServiceId: Int = -1,
    addToFavourites: Boolean = true,
    onDismissRequest: () -> Unit,
    viewModel: ScrobblingSelectorViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    // Reuse the host's resolver: it registers activity-result launchers, which Android only allows before the
    // activity starts, so creating one here while the sheet opens on a resumed activity crashes.
    val exceptionResolver = (context.findActivity() as? BaseComposeActivity)?.exceptionResolver
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(manga, scrobblerServiceId, addToFavourites) {
        viewModel.initialize(manga, addToFavourites)
        val selectedIndex = viewModel.availableScrobblers.indexOfFirst {
            it.scrobblerService.id == scrobblerServiceId
        }
        if (selectedIndex >= 0) {
            viewModel.setScrobblerIndex(selectedIndex)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.onError.collect { event ->
            event?.consume { error ->
                Toast.makeText(
                    context,
                    error.getDisplayMessage(context.resources),
                    Toast.LENGTH_LONG,
                ).show()
                if (viewModel.isEmpty) {
                    onDismissRequest()
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.onClose.collect { event ->
            event?.consume { onDismissRequest() }
        }
    }

    fun retry(error: Throwable) {
        if (ExceptionResolver.canResolve(error) && exceptionResolver != null) {
            coroutineScope.launch {
                if (exceptionResolver.resolve(error)) {
                    viewModel.retry()
                }
            }
        } else {
            viewModel.retry()
        }
    }

    KototoroTheme {
        // The same single glass sheet as the details page's tracking sheets it opens from.
        DetailsTranslucentBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = Modifier.fillMaxHeight(0.9f),
        ) {
            ScrobblingSelectorDialog(
                viewModel = viewModel,
                onDismissRequest = onDismissRequest,
                onRetry = ::retry,
                showServiceTabs = scrobblerServiceId < 0,
            )
        }
    }
}
