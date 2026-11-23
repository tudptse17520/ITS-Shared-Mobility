@echo off
title Project_ITS - Shared Mobility Control Panel
color 0B
cls
echo ==============================================================================
echo         PROJECT_ITS - HE THONG CHO THUE PHUONG TIEN CHIA SE (SHARED MOBILITY)
echo ==============================================================================
echo.
echo  Dang khoi dong cac dich vu...
echo.
echo  [1/2] Khoi dong Spring Boot Backend (JDK 17) tai cong 8080...
start "ITS Backend Server" cmd /k "chcp 65001 >nul && set JAVA_HOME=C:\Program Files\Java\jdk-17&& set PATH=C:\Program Files\Java\jdk-17\bin;%PATH%&& cd /d "%~dp0backend" && if not exist target\parking-building-management-system-0.0.1-SNAPSHOT.jar (call mvnw.cmd package -DskipTests) && java -jar target\parking-building-management-system-0.0.1-SNAPSHOT.jar"

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

