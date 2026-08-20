//go:build windows

package main

import (
	"os"
	"os/exec"
	"path/filepath"
	"syscall"
)

func main() {
	dir, _ := filepath.Abs(filepath.Dir(os.Args[0]))
	appDir := filepath.Join(dir, "app")
	java := filepath.Join(dir, "jre", "bin", "java.exe")

	// Find JAR dynamically
	matches, _ := filepath.Glob(filepath.Join(appDir, "aspect-model-editor-runtime-*.jar"))
	if len(matches) == 0 {
		os.Exit(1)
	}
	jar := matches[0]

	var dllPath string
	dllMatches, _ := filepath.Glob(filepath.Join(appDir, "*zstd-jni-*.dll"))
	if len(dllMatches) > 0 {
		dllPath = dllMatches[0]
	}

	args := []string{
		"--enable-native-access=ALL-UNNAMED",
		"--sun-misc-unsafe-memory-access=allow",
		"-Dpolyglotimpl.DisableMultiReleaseCheck=true",
		"-Djava.library.path=" + appDir,
		"-Djna.nounpack=true",
        "-Djna.boot.library.path=" + appDir,
	}

	if dllPath != "" {
		args = append(args, "-DZstdNativePath="+dllPath)
	}

	args = append(args, "-jar", jar)
	args = append(args, os.Args[1:]...)

	cmd := exec.Command(java, args...)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.SysProcAttr = &syscall.SysProcAttr{HideWindow: true}
	cmd.Run()
}
