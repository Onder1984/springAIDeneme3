@echo off
if not defined JAVA_HOME (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
    ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot"
    )
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2021.3.2\plugins\maven\lib\maven3\bin\mvn.cmd" (
    "C:\Program Files\JetBrains\IntelliJ IDEA 2021.3.2\plugins\maven\lib\maven3\bin\mvn.cmd" %*
) else if exist "C:\Users\onderproje\tools\apache-maven-3.9.9\bin\mvn.cmd" (
    "C:\Users\onderproje\tools\apache-maven-3.9.9\bin\mvn.cmd" %*
) else (
    mvn.cmd %*
)

