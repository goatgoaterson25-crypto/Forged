package com.forge.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun loadBitmap(context: Context, uriString: String): Bitmap? = try {
    context.contentResolver.openInputStream(Uri.parse(uriString))?.use {
        BitmapFactory.decodeStream(it)
    }
} catch (e: Exception) {
    null
}

private fun IntSize.toSize() = Size(width.toFloat(), height.toFloat())

@Composable
fun CornerBrackets(color: Color, length: Float = 14f, thickness: Float = 3f) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawLine(color, Offset(0f, 0f), Offset(length, 0f), thickness)
        drawLine(color, Offset(0f, 0f), Offset(0f, length), thickness)
        drawLine(color, Offset(w, h), Offset(w - length, h), thickness)
        drawLine(color, Offset(w, h), Offset(w, h - length), thickness)
    }
}

@Composable
fun GalleryScreen() {
    val context = LocalContext.current
    val uris = remember { mutableStateListOf<String>().apply { addAll(getGalleryOrder(context)) } }
    var openIndex by remember { mutableStateOf<Int?>(null) }

    if (openIndex != null) {
        FullScreenViewer(uris = uris, startIndex = openIndex!!, onClose = { openIndex = null })
        return
    }

    Surface(color = Background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "gallery / ${uris.size} stored",
                    fontSize = 12.sp,
                    color = InkDim,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "hold + drag to reorder",
                    fontSize = 12.sp,
                    color = Accent,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(12.dp))

            if (uris.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("nothing saved yet", color = InkDim, fontFamily = FontFamily.Monospace)
                }
            } else {
                ReorderableGrid(
                    uris = uris,
                    onReorder = { newList ->
                        uris.clear()
                        uris.addAll(newList)
                        saveGalleryOrder(context, newList)
                    },
                    onTap = { idx -> openIndex = idx }
                )
            }
        }
    }
}

@Composable
private fun ReorderableGrid(
    uris: List<String>,
    onReorder: (List<String>) -> Unit,
    onTap: (Int) -> Unit
) {
    val context = LocalContext.current
    val columns = 3
    val bounds = remember { mutableStateMapOf<Int, Rect>() }
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var didDrag by remember { mutableStateOf(false) }

    val rows = (uris.size + columns - 1) / columns

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index >= uris.size) {
                        Spacer(Modifier.weight(1f))
                        continue
                    }
                    val uriString = uris[index]
                    var bitmap by remember(uriString) { mutableStateOf<Bitmap?>(null) }
                    LaunchedEffect(uriString) {
                        bitmap = withContext(Dispatchers.IO) { loadBitmap(context, uriString) }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .zIndex(if (index == draggingIndex) 1f else 0f)
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInRoot()
                                bounds[index] = Rect(pos, coords.size.toSize())
                            }
                            .clip(RoundedCornerShape(6.dp))
                            .background(Panel)
                            .pointerInput(uris.size, index) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingIndex = index
                                        didDrag = true
                                        dragPosition = bounds[index]?.center ?: Offset.Zero
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragPosition += amount
                                        val targetIndex = bounds.entries.firstOrNull { (_, r) ->
                                            r.contains(dragPosition)
                                        }?.key
                                        if (targetIndex != null && targetIndex != draggingIndex) {
                                            val newList = uris.toMutableList()
                                            val moved = newList.removeAt(draggingIndex)
                                            newList.add(targetIndex, moved)
                                            draggingIndex = targetIndex
                                            onReorder(newList)
                                        }
                                    },
                                    onDragEnd = { draggingIndex = -1 },
                                    onDragCancel = { draggingIndex = -1 }
                                )
                            }
                            .pointerInput(index) {
                                androidx.compose.foundation.gestures.detectTapGestures(
                                    onTap = { onTap(index) }
                                )
                            }
                    ) {
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        }
                        CornerBrackets(color = Accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenViewer(uris: List<String>, startIndex: Int, onClose: () -> Unit) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { uris.size })

    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                var bitmap by remember(page) { mutableStateOf<Bitmap?>(null) }
                LaunchedEffect(page) {
                    bitmap = withContext(Dispatchers.IO) { loadBitmap(context, uris[page]) }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    CornerBrackets(color = Accent, length = 22f, thickness = 3f)
                }
            }

            Text(
                "${pagerState.currentPage + 1} / ${uris.size}",
                color = Ink,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )

            TextButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Text("close", color = Ink, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
