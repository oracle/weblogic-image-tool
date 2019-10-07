# Cleanup

The Image Tool creates a temporary directory, prefixed by ```wlsimgbuilder_temp```,
every time it runs.  Under normal circumstances, this directory will be deleted. However,
if the process is aborted or the tool is unable to remove the directory, you can delete it manually.

By default, the temporary directory is created under the user's home directory. If you do not want it created under the home directory, set the environment variable:

 ```bash
export WLSIMG_BLDDIR="/path/to/dir"
```


If you see dangling images after the build, use the following command to remove them:

```bash
docker rmi $(docker images --filter "dangling=true" -q --no-trunc)
```

### Copyright
Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
