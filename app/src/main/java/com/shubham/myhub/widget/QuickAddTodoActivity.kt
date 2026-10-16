package com.shubham.myhub.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.shubham.myhub.MyHubApplication
import com.shubham.myhub.ui.theme.MyHubTheme
import kotlinx.coroutines.launch

/** Small sheet opened by the widget's "+" button to add a to-do without opening the full app. */
class QuickAddTodoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as MyHubApplication).todoRepository

        setContent {
            MyHubTheme {
                QuickAddSheet(
                    onAdd = { text ->
                        lifecycleScope.launch {
                            repository.addTodo(text)
                            finish()
                        }
                    },
                    onDismiss = ::finish
                )
            }
        }
    }
}

@Composable
private fun QuickAddSheet(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = Color(0xFF1C2330),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                // Swallow taps so tapping the sheet doesn't dismiss it.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(20.dp)
            ) {
                Text("New to-do", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE3E2E6))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("What needs doing?", color = Color(0xFF5F6368)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onAdd(text) }),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8AB4F8),
                            unfocusedBorderColor = Color(0xFF263042),
                            focusedTextColor = Color(0xFFE3E2E6),
                            unfocusedTextColor = Color(0xFFE3E2E6),
                            cursorColor = Color(0xFF8AB4F8)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                    )
                    Spacer(Modifier.width(12.dp))
                    FilledIconButton(
                        onClick = { onAdd(text) },
                        enabled = text.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF8AB4F8),
                            contentColor = Color(0xFF0F1117),
                            disabledContainerColor = Color(0xFF263042),
                            disabledContentColor = Color(0xFF5F6368)
                        ),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add to-do")
                    }
                }
            }
        }
    }
}
