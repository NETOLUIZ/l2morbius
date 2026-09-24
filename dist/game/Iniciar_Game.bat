@echo off
title L2J Mobius - GameServer
cd /d "%~dp0"
"C:\Users\Luiz\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.4.v20260826-0822\jre\bin\java.exe" -server -Xmx4g -jar ../libs/GameServer.jar
pause
