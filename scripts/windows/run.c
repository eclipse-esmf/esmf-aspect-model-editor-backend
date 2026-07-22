#include <windows.h>
#include <stdio.h>
#include <string.h>

int main(int argc, char *argv[]) {
    char dir[MAX_PATH];
    GetModuleFileNameA(NULL, dir, MAX_PATH);
    
    // Get directory of this exe
    char *lastSlash = strrchr(dir, '\\');
    if (lastSlash) *(lastSlash + 1) = '\0';
    
    char cmd[8192];
    snprintf(cmd, sizeof(cmd),
        "\"%sjre\\bin\\java.exe\" "
        "--enable-native-access=ALL-UNNAMED "
        "--sun-misc-unsafe-memory-access=allow "
        "-Dpolyglotimpl.DisableMultiReleaseCheck=true "
        "-jar \"%saspect-model-editor-runtime-DEV-SNAPSHOT.jar\"",
        dir, dir);
    
    // Append extra arguments
    for (int i = 1; i < argc; i++) {
        strcat(cmd, " ");
        strcat(cmd, argv[i]);
    }
    
    STARTUPINFOA si = { .cb = sizeof(si) };
    PROCESS_INFORMATION pi;
    
    if (!CreateProcessA(NULL, cmd, NULL, NULL, TRUE, 0, NULL, NULL, &si, &pi)) {
        return 1;
    }
    
    WaitForSingleObject(pi.hProcess, INFINITE);
    
    DWORD exitCode;
    GetExitCodeProcess(pi.hProcess, &exitCode);
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    
    return exitCode;
}