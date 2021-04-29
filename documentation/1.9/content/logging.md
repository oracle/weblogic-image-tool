# Logging

The Image Tool uses standard Java logging. To alter the default logging settings, you can modify the `logging.properties` 
file under the `bin` directory where you installed the Image Tool.

### To enable debug logging
In `logging.properties`, comment the existing `handlers` property and uncomment the second `handlers` line below the first 
line that you just commented.  This should make the `logging.properties` file look something like this:
```properties
#handlers=java.util.logging.ConsoleHandler
handlers=java.util.logging.FileHandler, java.util.logging.ConsoleHandler
```
Then, change level to `FINE` or `FINER`.
```properties
#com.oracle.weblogic.imagetool.level=INFO
com.oracle.weblogic.imagetool.level=FINER
```

Logging severity levels from highest to lowest:

| Level | Description |
| --- | --- |
| `SEVERE` | Only error messages are written to the log file. |
| `WARNING` | Warning messages and higher are written to the log file. |
| `INFO` | (Default) Informational messages and higher are written to the log file. |
| `FINE` | First level debug messages and higher are written to the log file. |
| `FINER` | Detailed debug messages and higher are written to the log file. |
| `FINEST` | In addition to FINER level debug messages, all HTTP responses are written to the log file. |

### Copyright
Copyright (c) 2019, 2021, Oracle and/or its affiliates.
