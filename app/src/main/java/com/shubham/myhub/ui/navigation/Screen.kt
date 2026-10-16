package com.shubham.myhub.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Todo : Screen("todo", "Todo", Icons.Default.CheckCircle)
    object Notes : Screen("notes", "Notes", Icons.Default.Description)
    object Timetable : Screen("timetable", "Timetable", Icons.Default.CalendarMonth)
}
