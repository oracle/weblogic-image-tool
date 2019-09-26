# Argument Appendix

This appendix provides additional information for command-line parameters requiring addtional detail or clarificaiton.

## --additionalBuildCommands
This is an advanced option that allows you to provide additional commands to the Docker build step.  
The input for this parameter is a simple text file that contains one or more of the valid sections: 
`before-jdk-install, after-jdk-install, before-fmw-install, after-fmw-install, before-wdt-command, after-wdt-command, final-build-commands`.  
Each section can contain one or more valid Dockerfile commands and looks like:
```dockerfile
[after-fmw-install]
RUN rm /some/dir/unnecessary-file

[final-build-commands]
LABEL owner="middleware team"
```
