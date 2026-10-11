@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
"C:\Users\onderproje\tools\apache-maven-3.9.9\bin\mvn.cmd" %*
