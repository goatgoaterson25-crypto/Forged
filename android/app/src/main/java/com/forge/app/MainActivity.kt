package com.forge.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private val Background = Color(0xFF101314)
private val Panel = Color(0xFF171B1C)
private val Line = Color(0xFF2A3133)
private val Ink = Color(0xFFE9ECE9)
private val InkDim = Color(0xFF93A09C)
private val Accent = Color(0xFF4DE8B0)
private val Danger = Color(0xFFE8654D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Background,
                    surface = Panel,
                    primary = Accent,
                    onBackground = Ink,
                    onSurface = Ink
                )
            ) {
                ForgeScreen()
            }
        }
    }
}

private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(120, TimeUnit.SECONDS)
    .build()

private val JSON = "application/json; charset=utf-8".toMediaType()

suspend fun generateImage(
    prompt: String,
    negativePrompt: String,
    width: Int,
    height: Int,
    steps: Int
): Result<Bitmap> = withContext(Dispatchers.IO) {
    try {
        val body = JSONObject().apply {
            put("prompt", prompt)
            put("negative_prompt", negativePrompt)
            put("width", width)
            put("height", height)
            put("steps", steps)
        }.toString().toRequestBody(JSON)

        val request = Request.Builder()
            .url("${BuildConfig.API_BASE_URL}/api/generate")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val message = try { JSONObject(text).optString("error", text) } catch (e: Exception) { text }
                return@withContext Result.failure(IOException(message))
            }
            val json = JSONObject(text)
            val dataUrl = json.getString("image")
            val base64 = dataUrl.substringAfter("base64,")
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext Result.failure(IOException("Could not decode returned image"))
            Result.success(bitmap)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgeScreen() {
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var negativePrompt by remember { mutableStateOf("blurry, low quality, distorted") }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showTips by remember { mutableStateOf(false) }

    Surface(color = Background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                "Forge",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "your own image generator",
                fontSize = 13.sp,
                color = InkDim,
                fontFamily = FontFamily.Monospace
            )

            Spacer(Modifier.height(24.dp))

            Text("PROMPT", fontSize = 12.sp, color = InkDim, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                placeholder = { Text("Describe the image you want...", color = InkDim) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Panel,
                    unfocusedContainerColor = Panel,
                    focusedTextColor = Ink,
                    unfocusedTextColor = Ink,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Line
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Spacer(Modifier.height(16.dp))

            Text("NEGATIVE PROMPT", fontSize = 12.sp, color = InkDim, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = negativePrompt,
                onValueChange = { negativePrompt = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Panel,
                    unfocusedContainerColor = Panel,
                    focusedTextColor = Ink,
                    unfocusedTextColor = Ink,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Line
                )
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (prompt.isBlank()) {
                        errorMessage = "Enter a prompt first."
                        return@Button
                    }
                    errorMessage = null
                    loading = true
                    resultBitmap = null
                    scope.launch {
                        val result = generateImage(prompt, negativePrompt, 1024, 1024, 25)
                        loading = false
                        result.onSuccess {
                            resultBitmap = it
                            showTips = false
                        }
                        result.onFailure {
                            errorMessage = it.message ?: "Unknown error"
                            showTips = true
                        }
                    }
                },
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color(0xFF0A1613),
                    disabledContainerColor = Panel,
                    disabledContentColor = InkDim
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (loading) "Generating..." else "Generate", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    loading -> CircularProgressIndicator(color = Accent)
                    resultBitmap != null -> Image(
                        bitmap = resultBitmap!!.asImageBitmap(),
                        contentDescription = "Generated image",
                        modifier = Modifier.fillMaxWidth()
                    )
                    errorMessage != null -> Text(
                        errorMessage ?: "",
                        color = Danger,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(20.dp)
                    )
                    else -> Text(
                        "Your image will show up here",
                        color = InkDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }

            if (showTips && errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Panel)
                        .padding(14.dp)
                ) {
                    Text(
                        "if this was a content rejection",
                        fontSize = 12.sp,
                        color = InkDim,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Describe the scene literally — action, setting, lighting, art style — rather than words that imply harm.",
                        "Name the composition (e.g. camera angle, motion blur) instead of dramatic verbs.",
                        "If it keeps getting rejected here, the same prompt may render fine on your own model — worth trying there."
                    ).forEach { tip ->
                        Text(
                            "· $tip",
                            fontSize = 13.sp,
                            color = Ink,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
