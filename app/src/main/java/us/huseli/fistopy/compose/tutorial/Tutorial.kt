package us.huseli.fistopy.compose.tutorial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.fistopy.R
import us.huseli.fistopy.dataclasses.callbacks.LocalAppCallbacks
import us.huseli.fistopy.stringResource
import us.huseli.fistopy.viewmodels.TutorialViewModel

@Composable
fun Tutorial(viewModel: TutorialViewModel = hiltViewModel()) {
    val appCallbacks = LocalAppCallbacks.current
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val isFirstPage by viewModel.isFirstPage.collectAsStateWithLifecycle()
    val isLastPage by viewModel.isLastPage.collectAsStateWithLifecycle()
    val onNextClick: () -> Unit = {
        if (isLastPage) appCallbacks.onGotoLibraryClick()
        viewModel.gotoNextPage()
    }

    Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (currentPage) {
                1 -> TutorialPart1()
                2 -> TutorialPart2(onNextClick)
                3 -> TutorialPart3()
                4 -> TutorialPart4(onNextClick)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (!isFirstPage) OutlinedButton(
                onClick = { viewModel.gotoPreviousPage() },
                content = { Text(stringResource(R.string.back)) },
                shape = MaterialTheme.shapes.small,
            ) else Spacer(modifier = Modifier.width(1.dp))
            Button(
                onClick = onNextClick,
                content = { Text(stringResource(if (isLastPage) R.string.finish else R.string.next)) },
                shape = MaterialTheme.shapes.small,
            )
        }
    }
}
