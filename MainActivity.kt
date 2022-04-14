package m.derakhshan.myapplication

import android.graphics.*
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.applyCanvas
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.*
import m.derakhshan.myapplication.ui.theme.MyApplicationTheme
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            val state = viewModel.state.value
            val view = LocalView.current
            var capturingViewBounds by remember { mutableStateOf<Rect?>(null) }
            var background by remember { mutableStateOf<Bitmap?>(null) }
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.day_night))
            val progress by animateLottieCompositionAsState(
                composition,
                isPlaying = true,
                speed = if (state.isDarkTheme) 1.7f else -2.2f,
                clipSpec = LottieClipSpec.Frame(max = 50),
            )



            LaunchedEffect(state.startThemeTransition) {
                val tasks = arrayListOf<Deferred<Unit>>()
                tasks.add(async {
                    animate(
                        initialValue = 0f,
                        targetValue = if (!state.isDarkTheme) 2500f else 0f,
                        animationSpec = tween(1500),
                        block = { i, _ -> viewModel.event(MainEvents.UpdateLightToDarkRadius(i)) })

                })
                tasks.add(async {
                    animate(
                        initialValue = 2500f,
                        targetValue = if (state.isDarkTheme) 0f else 2500f,
                        animationSpec = tween(1000),
                        block = { i, _ -> viewModel.event(MainEvents.UpdateDarkToLightRadius(i)) })
                })
                tasks.awaitAll()

            }


            MyApplicationTheme(darkTheme = state.isDarkTheme) {

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { capturingViewBounds = it.boundsInRoot() },
                    color = MaterialTheme.colors.background
                ) {

                    Box(contentAlignment = Alignment.Center) {

                        Column {
                            Text(text = "Mohammad Derakhshan!", style = MaterialTheme.typography.h4)
                            Text(
                                text = stringResource(id = R.string.lorem),
                                style = MaterialTheme.typography.body1
                            )
                        }

                        background?.let {
                            ChangeTheme(
                                darkToLightRadius = state.darkToLightRadius,
                                lightToDarkRadius = state.lightToDarkRadius,
                                it
                            )
                        }


                        LottieAnimation(
                            composition, progress,
                            Modifier
                                .width(100.dp)
                                .height(100.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .clickable {
                                    capturingViewBounds?.let {
                                        background = takeScreenShot(it, view)
                                        viewModel.event(MainEvents.ToggleThemeTransitionState)
                                        viewModel.event(MainEvents.ToggleDarkTheme)
                                    }
                                },
                        )

                    }
                }
            }
        }
    }

    private fun takeScreenShot(bounds: Rect, view: View): Bitmap {
        return Bitmap.createBitmap(
            bounds.width.roundToInt(), bounds.height.roundToInt(),
            Bitmap.Config.ARGB_8888
        ).applyCanvas { view.draw(this) }
    }
}

@Composable
fun ChangeTheme(darkToLightRadius: Float, lightToDarkRadius: Float, background: Bitmap) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.99f),
        onDraw = {

            drawIntoCanvas {
                val clipPath = Path()
                clipPath.addOval(
                    Rect(
                        center = Offset(
                            x = size.width - 50.dp.toPx(),
                            y = 50.dp.toPx()
                        ),
                        radius = darkToLightRadius
                    )
                )

                it.clipPath(clipPath)

                val transparentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.TRANSPARENT
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }

                it.nativeCanvas.drawBitmap(
                    background,
                    0f,
                    0f,
                    Paint()
                )

                it.nativeCanvas.drawCircle(
                    size.width - 50.dp.toPx(),
                    50.dp.toPx(),
                    lightToDarkRadius,
                    transparentPaint
                )
            }
        })
}


class MainViewModel : ViewModel() {
    private val _state = mutableStateOf(MainState())
    val state: State<MainState> = _state

    fun event(events: MainEvents) {
        when (events) {

            is MainEvents.ToggleThemeTransitionState -> {
                _state.value = _state.value.copy(
                    startThemeTransition = !_state.value.startThemeTransition
                )
            }

            is MainEvents.ToggleDarkTheme -> {
                viewModelScope.launch {
                    delay(20)
                    _state.value = _state.value.copy(
                        isDarkTheme = !_state.value.isDarkTheme
                    )
                }
            }
            is MainEvents.UpdateDarkToLightRadius -> {
                _state.value = _state.value.copy(
                    darkToLightRadius = events.radius
                )
            }
            is MainEvents.UpdateLightToDarkRadius -> {
                _state.value = _state.value.copy(
                    lightToDarkRadius = events.radius
                )
            }

        }
    }
}

data class MainState(
    val isDarkTheme: Boolean = false,
    val startThemeTransition: Boolean = false,
    val lightToDarkRadius: Float = 0f,
    val darkToLightRadius: Float = 0f,
)

sealed class MainEvents {
    object ToggleThemeTransitionState : MainEvents()
    object ToggleDarkTheme : MainEvents()
    data class UpdateLightToDarkRadius(val radius: Float) : MainEvents()
    data class UpdateDarkToLightRadius(val radius: Float) : MainEvents()
}
