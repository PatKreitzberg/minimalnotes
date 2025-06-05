// File: app/src/main/java/com/wyldsoft/notes/ui/views/HomeView.kt
package com.wyldsoft.notes.ui.views

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wyldsoft.notes.database.entities.Folder
import com.wyldsoft.notes.database.entities.Notebook
import com.wyldsoft.notes.ui.viewmodels.HomeViewModel
import com.wyldsoft.notes.ui.components.CreateFolderDialog
import com.wyldsoft.notes.ui.components.CreateNotebookDialog

/**
 * HomeView displays the current folder's contents with breadcrumb navigation
 */
@Composable
fun HomeView(
    viewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val folders by viewModel.folders.collectAsState()
    val notebooks by viewModel.notebooks.collectAsState()
    val breadcrumb by viewModel.breadcrumb.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateNotebookDialog by remember { mutableStateOf(false) }

    // Handle errors
    LaunchedEffect(error) {
        error?.let {
            // Handle error display - for now just clear it
            viewModel.clearError()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Title bar with breadcrumb navigation
        BreadcrumbBar(
            breadcrumb = breadcrumb,
            onBreadcrumbClick = { folder ->
                if (folder == null) {
                    viewModel.navigateToParentFolder()
                } else {
                    viewModel.navigateToFolder(folder)
                }
            },
            onBackClick = {
                viewModel.navigateToParentFolder()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Log.d("HomeView", "isLoading")
            Text(
                text = "poop1:",
                fontSize = 14.sp,
                color = Color.Black
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Log.d("HomeView", "NOT isLoading")
            Text(
                text = "poop2:",
                fontSize = 14.sp,
                color = Color.Black
            )
            // Action buttons
            ActionButtonsRow(
                onCreateFolder = { showCreateFolderDialog = true },
                onCreateNotebook = { showCreateNotebookDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Folders section
            if (folders.isNotEmpty()) {
                FoldersSection(
                    folders = folders,
                    onFolderClick = { folder ->
                        viewModel.navigateToFolder(folder)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Notebooks section
            NotebooksSection(
                notebooks = notebooks,
                onNotebookClick = { notebook ->
                    viewModel.navigateToNotebook(notebook)
                }
            )
        }
    }

    // Dialogs
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                viewModel.createFolder(name)
                showCreateFolderDialog = false
            }
        )
    }

    if (showCreateNotebookDialog) {
        CreateNotebookDialog(
            onDismiss = { showCreateNotebookDialog = false },
            onConfirm = { name ->
                viewModel.createNotebook(name)
                showCreateNotebookDialog = false
            }
        )
    }
}

@Composable
private fun BreadcrumbBar(
    breadcrumb: List<Folder>,
    onBreadcrumbClick: (Folder?) -> Unit,
    onBackClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button (only show if not at root)
            if (breadcrumb.isNotEmpty()) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Current location text
            Text(
                text = "Current Folder:",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Breadcrumb navigation
            if (breadcrumb.isEmpty()) {
                Text(
                    text = "Home",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            } else {
                LazyRow {
                    // Home link
                    item {
                        Text(
                            text = "Home",
                            fontSize = 16.sp,
                            color = Color.Blue,
                            modifier = Modifier.clickable {
                                onBreadcrumbClick(null)
                            }
                        )
                        Text(
                            text = " > ",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // Breadcrumb items
                    items(breadcrumb) { folder ->
                        val isLast = folder == breadcrumb.last()

                        Text(
                            text = folder.name,
                            fontSize = 16.sp,
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                            color = if (isLast) Color.Black else Color.Blue,
                            modifier = if (!isLast) {
                                Modifier.clickable {
                                    onBreadcrumbClick(folder)
                                }
                            } else Modifier
                        )

                        if (!isLast) {
                            Text(
                                text = " > ",
                                fontSize = 16.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    onCreateFolder: () -> Unit,
    onCreateNotebook: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onCreateFolder,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            Icon(
                Icons.Default.CreateNewFolder,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Folder")
        }

        Button(
            onClick = onCreateNotebook,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Notebook")
        }
    }
}

@Composable
private fun FoldersSection(
    folders: List<Folder>,
    onFolderClick: (Folder) -> Unit
) {
    Column {
        Text(
            text = "Folders",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(folders) { folder ->
                FolderItem(
                    folder = folder,
                    onClick = { onFolderClick(folder) }
                )
            }
        }
    }
}

@Composable
private fun FolderItem(
    folder: Folder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = "Folder",
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = folder.name,
                fontSize = 16.sp,
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun NotebooksSection(
    notebooks: List<Notebook>,
    onNotebookClick: (Notebook) -> Unit
) {
    Column {
        Text(
            text = "Notebooks",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (notebooks.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Text(
                    text = "No notebooks in this folder.\nCreate your first notebook to get started!",
                    modifier = Modifier.padding(32.dp),
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notebooks) { notebook ->
                    NotebookItem(
                        notebook = notebook,
                        onClick = { onNotebookClick(notebook) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotebookItem(
    notebook: Notebook,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = "Notebook",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = notebook.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}