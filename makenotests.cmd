@echo off
cls
if not exist setenv.cmd (
    echo ---------------------------------------------
    echo ERROR: Could not find the setenv.cmd file
    echo        The file setenv.cmd.sample is provided
    echo        as a suitable starting point. Read the
    echo        comments in the sample file for help
    echo        on suitable settings .
    echo ---------------------------------------------
) else (
    @call setenv.cmd
    if errorlevel 0 (
        echo Started compile at %TIME%
        mvn clean install -Dmaven.test.skip=true dependency:copy-dependencies
        echo Completed at %TIME%
    )
)
pause