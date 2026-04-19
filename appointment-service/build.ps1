# build.ps1
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 2
Remove-Item -Recurse -Force ".\target" -ErrorAction SilentlyContinue
mvn clean package -DskipTests