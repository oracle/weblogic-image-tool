package com.oracle.weblogicx.imagebuilder.builder.cli;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Arg4jDriver {

    public static void main(String[] args) {
        ArgumentParser argParser = ArgumentParsers.newFor("builder").build()
                .description("Build WebLogic docker image");

//        argParser.addArgument("-it", "--installType")
//                .choices("wls", "fmw").setDefault("wls").required(false)
//                .help("Installer type. wls or fmw (Infrastructure)");

        argParser.addMutuallyExclusiveGroup("Choose one Installer type").required(true)
                .addArgument("-fmw, -wls").dest("type")
                .setDefault("-wls").help("Either -fmw or -wls");

        Namespace ns = null;
        try {
            ns = argParser.parseArgs(args);
            System.out.println(ns.getAttrs());
            if (ns.getAttrs().size() < 3) {
                System.out.println("Empty");
                //argParser.printHelp();
            } else {
                System.out.println(ns.getAttrs());
            }
        } catch (ArgumentParserException e) {
            argParser.handleError(e);
            System.exit(1);
        }
    }
}
