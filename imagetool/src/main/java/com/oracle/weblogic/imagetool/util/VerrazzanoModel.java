// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

/**
 * Mustache collection of options used to resolve values for the Verrazzano Model
 * as provided with the --resolverFiles command line argument.
 */
public class VerrazzanoModel {

    private String imageName;
    private String domainHome;


    /**
     * Construct file with the options.
     *
     * @param imageName name from the image tag argument
     * @param domainHome domain home argument or default if no argument
     */
    public VerrazzanoModel(String imageName, String domainHome) {
        this.imageName = imageName;
        this.domainHome = domainHome;
    }

    /**
     * Return the image name value passed in the image tag argument.
     *
     * @return image tag name
     */
    public String imageName() {
        return imageName;
    }

    /**
     * Return the domain home passed in the domain home argument or the default if not passed as
     * an argument.
     *
     * @return domain home value
     */
    public String domainHome() {
        return domainHome;
    }
}