package com.example.madomireader.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.madomireader.di.AppContainer
import com.example.madomireader.ui.chapters.ChapterListScreen
import com.example.madomireader.ui.home.HomeScreen
import com.example.madomireader.ui.reader.ReaderScreen
import com.example.madomireader.ui.settings.SettingsScreen

/**
 * Tất cả route của ứng dụng. Các tham số được mã hoá bằng [Uri.encode] để
 * an toàn với mã truyện/chương có ký tự đặc biệt.
 */
object Routes {

    const val HOME = "home"
    const val SETTINGS = "settings"

    const val CHAPTERS = "chapters/{sourceId}/{mangaId}"
    const val READER = "reader/{sourceId}/{mangaId}/{chapterId}"

    const val ARG_SOURCE_ID = "sourceId"
    const val ARG_MANGA_ID = "mangaId"
    const val ARG_CHAPTER_ID = "chapterId"

    fun chapters(sourceId: String, mangaId: String): String =
        "chapters/${Uri.encode(sourceId)}/${Uri.encode(mangaId)}"

    fun reader(sourceId: String, mangaId: String, chapterId: String): String =
        "reader/${Uri.encode(sourceId)}/${Uri.encode(mangaId)}/${Uri.encode(chapterId)}"
}

@Composable
fun AppNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(route = Routes.HOME) {
            HomeScreen(
                container = container,
                onOpenManga = { sourceId, mangaId ->
                    navController.navigate(Routes.chapters(sourceId, mangaId))
                },
                onOpenReader = { sourceId, mangaId, chapterId ->
                    navController.navigate(Routes.reader(sourceId, mangaId, chapterId))
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            route = Routes.CHAPTERS,
            arguments = listOf(
                navArgument(Routes.ARG_SOURCE_ID) { type = NavType.StringType },
                navArgument(Routes.ARG_MANGA_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val sourceId = entry.stringArg(Routes.ARG_SOURCE_ID)
            val mangaId = entry.stringArg(Routes.ARG_MANGA_ID)

            ChapterListScreen(
                container = container,
                sourceId = sourceId,
                mangaId = mangaId,
                onBack = { navController.popBackStack() },
                onOpenChapter = { chapterId ->
                    navController.navigate(Routes.reader(sourceId, mangaId, chapterId))
                },
            )
        }

        composable(
            route = Routes.READER,
            arguments = listOf(
                navArgument(Routes.ARG_SOURCE_ID) { type = NavType.StringType },
                navArgument(Routes.ARG_MANGA_ID) { type = NavType.StringType },
                navArgument(Routes.ARG_CHAPTER_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            ReaderScreen(
                container = container,
                sourceId = entry.stringArg(Routes.ARG_SOURCE_ID),
                mangaId = entry.stringArg(Routes.ARG_MANGA_ID),
                chapterId = entry.stringArg(Routes.ARG_CHAPTER_ID),
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(route = Routes.SETTINGS) {
            SettingsScreen(
                container = container,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/** Đọc một tham số đường dẫn dạng chuỗi, đã giải mã. */
private fun androidx.navigation.NavBackStackEntry.stringArg(key: String): String =
    Uri.decode(arguments?.getString(key).orEmpty())
