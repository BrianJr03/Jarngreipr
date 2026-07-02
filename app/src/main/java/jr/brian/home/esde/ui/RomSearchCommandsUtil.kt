package jr.brian.home.esde.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jr.brian.home.R

@Composable
internal fun romSearchCommands(): List<Pair<String, String>> = listOf(
    stringResource(R.string.rom_search_command_android) to stringResource(R.string.rom_search_command_android_desc),
    stringResource(R.string.rom_search_command_hidden) to stringResource(R.string.rom_search_command_hidden_desc),
    stringResource(R.string.rom_search_command_platform) to stringResource(R.string.rom_search_command_platform_desc),
    stringResource(R.string.rom_search_command_partial) to stringResource(R.string.rom_search_command_partial_desc),
    stringResource(R.string.rom_search_command_name) to stringResource(R.string.rom_search_command_name_desc),
)