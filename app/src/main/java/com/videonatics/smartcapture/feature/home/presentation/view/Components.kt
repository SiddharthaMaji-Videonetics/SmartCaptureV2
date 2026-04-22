package com.videonatics.smartcapture.feature.home.presentation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videonatics.smartcapture.feature.home.domain.model.Task
import com.videonatics.smartcapture.feature.home.domain.model.TaskStatus

@Composable
fun DashboardScreen() {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        /*item { FilterTabs() }*/

        item { WarningCard() }

        item { InfoCard() }

        item { StatsCard(title = "Active Tasks", value = "1") }

        item { PassRateCard(passRate = 0) }

        item { StatsCard(title = "Completed", value = "0") }

        item { StatsCard(title = "Overrides Pending", value = "2") }

        item { StagePassRate() }

        item { FailureReasons() }

        /*item { ActiveTaskCard() }*/
    }
}

@Composable
fun MyTaskScreen(
    tasks: List<Task> = listOf(
        Task(
            title = "Task 1",
            name = "test1",
            location = "test1",
            status = TaskStatus.IN_PROGRESS
        ),
        Task(
            title = "Task 2",
            name = "test2",
            location = "test2",
            status = TaskStatus.COMPLETED
        ),
        Task(
            title = "Task 3",
            name = "test3",
            location = "test3",
            status = TaskStatus.PENDING
        )
    ),
    onTaskOpen: () -> Unit ={}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

       // item { MyTaskTopBar() }

       // item { UserHeader() }

       // item { WelcomeSection(userName, userEmail) }

        items(tasks) { task ->
            TaskItem(task,onTaskOpen)
        }
    }
}

@Composable
fun ProfileScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Profile Screen")
    }
}