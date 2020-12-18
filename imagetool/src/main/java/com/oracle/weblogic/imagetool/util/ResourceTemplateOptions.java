// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

/**
 * Provide backing bean for Mustache templates provided in --resourceTemplates.
 */
public class ResourceTemplateOptions {
    private String imageName;
    private String domainHome;
    private DomainHomeSourceType domainHomeSourceType = DomainHomeSourceType.PV;


    /**
     * Return the image name value passed in the image --tag argument.
     *
     * @return image tag name
     */
    public String imageName() {
        return imageName;
    }

    /**
     * Set the image name value.
     *
     * @param value image name from --tag argument.
     * @return builder/this
     */
    public ResourceTemplateOptions imageName(String value) {
        imageName = value;
        return this;
    }

    /**
     * Return the domain home passed in the --wdtDomainHome argument or the default if not provided.
     *
     * @return domain home value
     */
    public String domainHome() {
        return domainHome;
    }

    /**
     * Set the domain home.
     *
     * @param value the domain home from the --wdtDomainHome argument
     * @return builder/this
     */
    public ResourceTemplateOptions domainHome(String value) {
        domainHome = value;
        return this;
    }

    /**
     * Return the domain home type based on WDT inputs provided.
     * If --wdtModelOnly, then type is FromModel.
     * If not model only but --wdtModel is provided, then Image.
     * Else, PersistentVolume (domain in PV).
     *
     * @return domain home source type
     */
    public String domainHomeSourceType() {
        return domainHomeSourceType.toString();
    }

    /**
     * Set the domain home source type (FromModel, Image, or PersistentVolume).
     *
     * @param value FromModel, Image, or PersistentVolume
     * @return builder/this
     */
    public ResourceTemplateOptions domainHomeSourceType(DomainHomeSourceType value) {
        domainHomeSourceType = value;
        return this;
    }
}
