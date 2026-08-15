@echo off
rem GBF one-click regression gate (calls run_all_tests.ps1)
rem Usage: run_all_tests.cmd [-Lint] [-Quick] [-SkipStatic] [-NoCompile] [-Card "card name"]
rem   -Lint   lint + static verify only (fast, seconds) - use for card iteration
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run_all_tests.ps1" %*
exit /b %ERRORLEVEL%
