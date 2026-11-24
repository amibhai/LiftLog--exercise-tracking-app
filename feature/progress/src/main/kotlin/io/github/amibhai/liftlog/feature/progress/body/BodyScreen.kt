package io.github.amibhai.liftlog.feature.progress.body

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.amibhai.liftlog.core.domain.model.ProgressPhotoId
import io.github.amibhai.liftlog.core.domain.progress.BodyMetricType
import io.github.amibhai.liftlog.core.domain.progress.ProgressPhoto
import io.github.amibhai.liftlog.core.ui.format.dateText
import io.github.amibhai.liftlog.core.ui.format.metricValueText
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.progress.R
import java.io.File
import io.github.amibhai.liftlog.core.ui.R as UiR

/** Photos shown next to each other. */
private const val PHOTOS_PER_ROW = 3
private val PhotoCorner = RoundedCornerShape(8.dp)
private const val PHOTO_FOLDER = "camera"

@Composable
internal fun BodyRoute(
    onBack: () -> Unit,
    onOpenMetric: (BodyMetricType) -> Unit,
    viewModel: BodyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbars = remember { SnackbarHostState() }
    BodyScreen(
        state = state,
        snackbars = snackbars,
        onBack = onBack,
        onOpenMetric = onOpenMetric,
        onAddPhoto = viewModel::addPhoto,
        onDeletePhoto = viewModel::deletePhoto,
    )
    UndoSnackbar(
        deleted = state.deletedPhoto,
        host = snackbars,
        onUndo = viewModel::undoDeletePhoto,
        onDone = viewModel::onMessageShown,
    )
    FailureMessage(state.hasFailedToAdd, snackbars, viewModel::onMessageShown)
}

@Composable
private fun FailureMessage(hasFailed: Boolean, host: SnackbarHostState, onDone: () -> Unit) {
    val done by rememberUpdatedState(onDone)
    val text = stringResource(R.string.progress_photo_failed)
    LaunchedEffect(hasFailed) {
        if (hasFailed) {
            host.showSnackbar(text)
            done()
        }
    }
}

@Composable
internal fun BodyScreen(
    state: BodyUiState,
    snackbars: SnackbarHostState,
    onBack: () -> Unit,
    onOpenMetric: (BodyMetricType) -> Unit,
    onAddPhoto: (uri: String) -> Unit,
    onDeletePhoto: (ProgressPhotoId) -> Unit,
    modifier: Modifier = Modifier,
) {
    var openPhoto by remember { mutableStateOf<ProgressPhoto?>(null) }
    val context = LocalContext.current
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onAddPhoto(it.toString()) }
    }
    val camera = rememberCamera(onAddPhoto)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.progress_body)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(LiftLogIcons.ArrowBack), stringResource(UiR.string.ui_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbars) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = LiftLogDimens.spacingM),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
        ) {
            item(key = "metrics") { SectionTitle(stringResource(R.string.progress_body_metrics)) }
            items(state.metrics, key = { it.type.name }) { row ->
                ListItem(
                    onClick = { onOpenMetric(row.type) },
                    supportingContent = row.latest?.let { { Text(dateText(it.measuredAt)) } },
                    trailingContent = {
                        Text(row.latest?.let { metricValueText(row.type, it.value, state.unit) }.orEmpty())
                    },
                ) { Text(stringResource(row.type.labelRes())) }
            }
            item(key = "divider") { HorizontalDivider() }
            item(key = "photos") {
                PhotosHeader(
                    onPick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onTake = { camera.take(context) },
                )
            }
            if (state.photos.isEmpty()) {
                item(key = "no-photos") {
                    Text(
                        text = stringResource(R.string.progress_photo_empty),
                        modifier = Modifier.padding(horizontal = LiftLogDimens.screenGutter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.photos.chunked(PHOTOS_PER_ROW), key = { row -> row.first().id.value }) { row ->
                PhotoRow(row, onOpen = { openPhoto = it })
            }
        }
    }
    openPhoto?.let { photo ->
        PhotoViewer(
            photo = photo,
            onDelete = {
                openPhoto = null
                onDeletePhoto(photo.id)
            },
            onDismiss = { openPhoto = null },
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .padding(horizontal = LiftLogDimens.screenGutter)
            .semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun PhotosHeader(onPick: () -> Unit, onTake: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = LiftLogDimens.screenGutter)) {
        SectionTitle(stringResource(R.string.progress_photos), Modifier.padding(horizontal = 0.dp))
        Text(
            text = stringResource(R.string.progress_photos_private),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS)) {
            TextButton(onClick = onTake) {
                Icon(painterResource(LiftLogIcons.Image), contentDescription = null)
                Text(stringResource(R.string.progress_photo_take))
            }
            TextButton(onClick = onPick) {
                Icon(painterResource(LiftLogIcons.Add), contentDescription = null)
                Text(stringResource(R.string.progress_photo_pick))
            }
        }
    }
}

@Composable
private fun PhotoRow(photos: List<ProgressPhoto>, onOpen: (ProgressPhoto) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LiftLogDimens.screenGutter),
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
    ) {
        for (photo in photos) {
            val description = stringResource(R.string.progress_photo_description, dateText(photo.takenAt))
            AsyncImage(
                model = File(photo.path),
                contentDescription = description,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(PhotoCorner)
                    .clickable(role = Role.Button) { onOpen(photo) },
                contentScale = ContentScale.Crop,
            )
        }
        // Fill the row so a lone photo does not stretch.
        repeat(PHOTOS_PER_ROW - photos.size) { Box(Modifier.weight(1f)) }
    }
}

@Composable
private fun PhotoViewer(
    photo: ProgressPhoto,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val date = dateText(photo.takenAt)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column {
                Row {
                    IconButton(onClick = onDismiss) {
                        Icon(painterResource(LiftLogIcons.Close), stringResource(UiR.string.ui_close))
                    }
                    Text(
                        text = date,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = LiftLogDimens.spacingM),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = onDelete) {
                        Icon(painterResource(LiftLogIcons.Delete), stringResource(UiR.string.ui_delete))
                    }
                }
                AsyncImage(
                    model = File(photo.path),
                    contentDescription = stringResource(R.string.progress_photo_description, date),
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = date },
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

/** Takes a picture with the camera app into a file of ours; nothing happens without one. */
private class Camera(
    private val launcher: ManagedActivityResultLauncher<Uri, Boolean>,
    private val remember: (Uri) -> Unit,
) {
    fun take(context: Context) {
        val file = File(File(context.cacheDir, PHOTO_FOLDER).apply { mkdirs() }, "photo.jpg")
        val uri = FileProvider.getUriForFile(context, context.packageName + ".photos", file)
        remember(uri)
        try {
            launcher.launch(uri)
        } catch (_: ActivityNotFoundException) {
            // A phone without a camera app: the gallery still works.
        }
    }
}

@Composable
private fun rememberCamera(onAddPhoto: (String) -> Unit): Camera {
    var taking by rememberSaveable { mutableStateOf<String?>(null) }
    val add by rememberUpdatedState(onAddPhoto)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { isSaved ->
        taking?.takeIf { isSaved }?.let { add(it) }
        taking = null
    }
    return remember(launcher) { Camera(launcher) { taking = it.toString() } }
}
