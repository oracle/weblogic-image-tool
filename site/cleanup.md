# Cleanup

The Image Tool creates a temporary Docker context directory, prefixed by ```wlsimgbuilder_temp```,
every time the tool runs.  Under normal circumstances, this context directory will be deleted. However,
if the process is aborted or the tool is unable to remove the directory, it is safe for you to delete it manually.

By default, the Image Tool creates the Docker context directory under the user's home directory. 
If you prefer to use a different directory for the temporary context, set the environment variable `WLSIMG_BLDDIR`.

 ```bash
export WLSIMG_BLDDIR="/path/to/dir"
```


The Image Tool will try to prune intermediate images from the Docker multi-stage build after the build step. 
If you see dangling images after the build (images labeled as `<none>`, use the following command to remove them:

```bash
docker rmi $(docker images --filter "dangling=true" -q --no-trunc)
```

If you wish to keep the Docker context directory and/or the intermediate images, using the `--skipcleanup` flag will 
skip the delete steps.  Some older versions of Docker do not support the prune command used by the Image Tool. 
As a temporary workaround, `--skipcleanup` can be used to skip the prune command until you are able to upgrade to a 
newer version of Docker.
 
### Copyright
Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
