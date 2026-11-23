@echo off
setlocal
set "BACKEND_JAR=target\its-shared-mobility-0.0.1-SNAPSHOT.jar"
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
title ITS-Shared-Mobility Control Panel
color 0B
cls
echo ==============================================================================
echo         ITS-SHARED-MOBILITY - HE THONG CHO THUE PHUONG TIEN CHIA SE
echo ==============================================================================
echo.
echo  Dang khoi dong cac dich vu...
echo.
echo  [1/2] Khoi dong Spring Boot Backend (JDK 17+) tai cong 8080...
start "ITS Backend Server" /D "%~dp0backend" cmd /k "chcp 65001 >nul && (if not exist %BACKEND_JAR% call mvnw.cmd package -DskipTests) && java -jar %BACKEND_JAR%"

echo  [2/2] Khoi dong Web Admin (Next.js + VietMap) tai cong 3000...
start "ITS Web Admin" cmd /k "cd /d "%~dp0frontend" && npm run dev"

echo.
echo ==============================================================================
echo  KHOI DONG THANH CONG!
echo.
echo  - Web Admin URL : http://localhost:3000
echo  - Fleet Map IoT : http://localhost:3000/admin/fleet-map
echo  - Backend API   : http://localhost:8080/api/v1
echo  - Swagger Docs  : http://localhost:8080/swagger-ui/index.html
echo  - Mobile App    : Mo thu muc mobile-android/ trong Android Studio
echo.
echo  Tai khoan demo: admin / password  |  manager / password  |  user / password
echo ==============================================================================
echo.
pause

