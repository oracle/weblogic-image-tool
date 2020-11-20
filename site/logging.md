# Logging

The Image Tool uses standard Java logging. To alter the default logging settings, you can modify the `logging.properties` 
file under the ```bin``` directory where you installed the Image Tool.

### To enable debug logging
In `logging.properties`, comment the existing `handlers` property and uncomment the `handlers` line below the first that 
contains the FileHandler.
```properties
#handlers=java.util.logging.ConsoleHandler
handlers=java.util.logging.FileHandler, java.util.logging.ConsoleHandler
```
Then, change the Level to `FINE` or `FINER`.
```properties
#com.oracle.weblogic.imagetool.level=INFO
com.oracle.weblogic.imagetool.level=FINER
```

Logging levels from highest to lowest:

| Level | Description |
| --- | --- |
| `SEVERE` | Only error messages will be written to the log file. |
| `WARNING` | Warning messages and above will be written to the log file. |
| `INFO` | (Default) Informational messages and above will be written to the log file. |
| `FINE` | First level of debug messages and above are written to the log file. |
| `FINER` | Detailed debug messages and above are written to the log file. |
| `FINEST` | All HTTP responses in addition to `FINER` level debug messages are written to the log file. |

### Copyright
Copyright (c) 2019, 2020, Oracle and/or its affiliates.
