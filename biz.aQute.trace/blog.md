---
title: Measuring startup time
layout: post
description:  "The use of Java on small gateways makes startup performance crucial"
comments: true
---


# Trace

The Trace function measures the timing of methods. Out of the box it measures the `BundleActivator` `start` and `stop` methods  as well as the DS components life cycle methods: activate, deactivate, and modified. It is highly optimized to only weave the classes that are actual components. For this, it parses the DS XML. 

However, it is possible to also measure arbitrary methods.

The `biz.aQute.trace` support _weaves_ classes with methods that must be measured. That is, the to be measured methods are _instrumented_ using byte code weaving. A small prolog to the methods marks the invocation time of the method and a method in a finally block measures the exit time. 

The trace support consists of a JAR and a bundle:

* `biz.aQute.trace.runpath` – Must be placed on the `-runpath` of the executable. This JAR will perform the actual weaving and recording in a global class `ActivationTrace`.
* `biz.aQute.trace.gui` – Provides Gogo shell commands to see the event queue and clear it, as well as adding arbitrary methods. It uses the global class `ActivationTrace` to access the event queue and the interaction with the weaver to add additional methods. It also provides a page on the Apache Felix WebConsole to graphically show the timing diagram.


## How to Use

* Add the `biz.aQute.trace.runpath` to the `-runpath`. This JAR has an _embedded activator_ that is recognized by the bnd launcher. It will register a `WeavingHook` service. 
* Add the `biz.aQute.trace.gui` bundle to the `-runbundles` instruction, preferably via the `-runrequires`.

The `biz.aQute.runpath` supports the following System properties:

* `biz.aQute.trace.extra` – A list of extra methods to weave. The format is a comma separated list of the format specified in the `weave` command, see later on. All these methods will be measured as well. 
* `biz.aQute.trace.debug` – When set to anything, will provide a detailed account of its actions on the system console. The `trace:debug` command can toggle this flag.

## Gogo Commands

The `biz.aQute.trace.gui` command provides a number of Gogo commands elucidated in the following sections.

### traces ( text | html | json )?`

Show the current set of events on the console. Formats are text, html, or json. If nothing is specified it is formatted to read from the console most easiest. The default format looks like:

    1001    463 ms       4 ms  A      [13] biz.aQute.trace.tester.SomeComponent.activate(org.osgi.framework.BundleContext)
    1002    467 ms      58 µs  A      [13] biz.aQute.trace.tester.LazyComponent.activate()
    1003     28 sec     37 µs  D      [13] biz.aQute.trace.tester.LazyComponent.deactivate()
    1004     28 sec     72 µs  D      [13] biz.aQute.trace.tester.SomeComponent.deactivate()
    1005     28 sec      3 ms  A      [13] biz.aQute.trace.tester.SomeComponent.activate(org.osgi.framework.BundleContext)
    1006     28 sec     43 µs  A      [13] biz.aQute.trace.tester.LazyComponent.activate()
    
The columns are :

* An id, starting at 1000
* Starting time from the point where the trace bundle was started or last cleared
* Duration
* Action
* Bundle id
* Class & method

The actual desired format can be specified as option. This is one of:

* `text` – CSV format, can be used in spread sheets like Excel
* `json` – JSON format. Can be used qith `jsonql` and is very suitable for Javascript based applications like in the browser
* `html` – HTML output. Provides a graphic representation of the timing diagram.

### clear

Clears the event queue and resets the timers.

### weave spec

Weave a method to instrument it with measuring code. The spec is defined in one of the following sections.
 
### unweave ( class | spec )

Remove an earlier weave command.

### debug

Toggle the debug flag. This flag can be set by setting a value (any value) in `biz.aQute.trace.debug` System property. The debug command toggles it. Debug output is on the console to not have a dependency on any of the myriad of logging frameworks. 

### Weave Specification

The `weave` and `unweave` commands, as well as the `biz.aQute.trace.extra` property, take a weave _specification_. Such a specification has the following format.

    spec = class ':' method ':' action
    class = fqn
    method = NAME
    action = NAME

The class and method name should be clear, they uniquely specify a method in the runtime. If the class & method are present multiple times then they all will be instrumented. The action is a textual marker 

For example, to trace the method that finds an `activate` method in DS in Apache Felix SCR you can use the following spec:

    org.apache.felix.scr.impl.inject.methods.ActivateMethod:doFindMethod:DS
    
In the `biz.aQute.trace.extra` system property, the spec can be repeated by separating the specs by comma:

## Web Console

The Trace Monitor bundle (`biz.aQute.trace.gui`) registers an Apache Felix Inventory Plugin. Such plugins can provide any information in either HTML, JSON, or plain text. The Web Console supports these plugins. The Trace Monitor bundle provides an elegant graphic presentation of the timeline. The plugin is listed in the menu of the Web Console under `Status/Trace`.

The layout should be clear. At the top there is a timeline form the first measurement until the last measurement. Vertically the methods classes are list. The timings of the measured methods of those classes is show on the main field. Hovering over a class name or measurement will provide additional information. If the measurements took place on different threads than the measuring information is marked with different colors. The tool tip will tell which thread was used.

## Example:

The following bndrun file could be used for the trace tool.

    -runproperties: \
        biz.aQute.trace.debug = ok, \
        biz.aQute.trace.extra = \
            "org.apache.felix.scr.impl.inject.methods.ActivateMethod:doFindMethod:DSF, \
            org.apache.felix.scr.impl.inject.methods.ActivateMethod:activate:DSA"
        
    -runpath: \
        biz.aQute.trace.runpath
    
    -runbundles.trace: \
        biz.aQute.trace.gui

