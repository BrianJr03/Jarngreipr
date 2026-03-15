package jr.brian.home.service

import jr.brian.home.IShellService
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

/**
 * Shell service that runs with Shizuku's elevated privileges.
 * This service can execute commands that require ADB/shell permissions.
 */
class ShellService : IShellService.Stub() {
    /**
     * Force stop an app using the am command.
     * Returns 0 on success, non-zero on failure.
     */
    override fun forceStop(packageName: String): Int {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("am", "force-stop", packageName))
            process.waitFor()
        } catch (e: Exception) {
            -1
        }
    }
    
    /**
     * Remove an app from the system's recent apps screen.
     * Returns 0 on success, non-zero on failure.
     */
    override fun removeFromRecents(packageName: String): Int {
        return try {
            // Get task IDs for this package from dumpsys
            val taskIds = getTaskIdsForPackage(packageName)
            
            if (taskIds.isEmpty()) {
                return 0 // No tasks to remove, consider it success
            }
            
            var result = 0
            for (taskId in taskIds) {
                val process = Runtime.getRuntime().exec(arrayOf("am", "task", "remove", taskId.toString()))
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    result = exitCode
                }
            }
            result
        } catch (e: Exception) {
            -1
        }
    }
    
    /**
     * Force stop an app AND remove it from the system's recent apps screen.
     * Returns 0 on success, non-zero on failure.
     */
    override fun forceStopAndRemoveFromRecents(packageName: String): Int {
        return try {
            // First remove from recents (need to do this before force-stop while we can still find the tasks)
            val taskIds = getTaskIdsForPackage(packageName)
            
            // Force stop the app
            val forceStopResult = forceStop(packageName)
            
            // Then remove tasks from recents
            for (taskId in taskIds) {
                try {
                    val process = Runtime.getRuntime().exec(arrayOf("am", "task", "remove", taskId.toString()))
                    process.waitFor()
                } catch (_: Exception) {
                    // Continue even if individual task removal fails
                }
            }
            
            forceStopResult
        } catch (e: Exception) {
            -1
        }
    }
    
    /**
     * Get all task IDs associated with a package name.
     */
    private fun getTaskIdsForPackage(packageName: String): List<Int> {
        val taskIds = mutableListOf<Int>()
        
        try {
            // Use dumpsys activity recents to get recent tasks
            val process = Runtime.getRuntime().exec(arrayOf("dumpsys", "activity", "recents"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            var currentTaskId: Int? = null
            reader.useLines { lines ->
                for (line in lines) {
                    // Look for task ID pattern: "* Task{...} #123"  or "taskId=123"
                    val taskIdMatch = Regex("""#(\d+)""").find(line) ?: Regex("""taskId=(\d+)""").find(line)
                    if (taskIdMatch != null) {
                        currentTaskId = taskIdMatch.groupValues[1].toIntOrNull()
                    }
                    
                    // Check if this task belongs to our package
                    if (line.contains(packageName)) {
                        currentTaskId?.let { taskId ->
                            taskIds.add(taskId)
                            currentTaskId = null
                        }
                    }
                }
            }
            
            process.waitFor()
        } catch (_: Exception) {
            // Return empty list on error
        }
        
        return taskIds.distinct()
    }
    
    /**
     * Clean up and destroy the service.
     */
    override fun destroy() {
        exitProcess(0)
    }
}
