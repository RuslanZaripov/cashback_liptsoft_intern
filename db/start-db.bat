@echo off
set BASEDIR=%~dp0
docker-compose --file "%BASEDIR%/docker-compose.yaml" up
