@echo off
chcp 936 >nul
rem ==========================================
rem  一键推送源码到 GitHub（本地小工具）
rem ==========================================
cd /d "%~dp0"
echo.
echo [1/4] 当前改动的文件：
git status --short
echo.
echo [2/4] 添加所有改动...
git add -A
echo.
echo [3/4] 提交...
set /p MSG=提交说明（可留空，直接回车用自动）: 
if "%MSG%"=="" set "MSG=自动提交"
git commit -m "%MSG%"
echo.
echo [4/4] 推送到 GitHub...
git push origin main
echo.
echo ==========================================
echo  完成！如果上面出现红色报错，请把窗口内容发给 AI 看。
echo ==========================================
pause
