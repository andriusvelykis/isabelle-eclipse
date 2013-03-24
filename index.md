---
layout: default
content_type: md
---

# Isabelle/Eclipse

[Eclipse][eclipse] plug-ins that provide Prover IDE for [Isabelle proof assistant][isabelle], based on Isabelle/Scala framework.

[eclipse]: http://www.eclipse.org/
[isabelle]: http://www.cl.cam.ac.uk/research/hvg/isabelle/

## Description

Isabelle/Eclipse started as a port of Isabelle/jEdit Prover IDE to integrate with Eclipse IDE as plug-ins. The integration uses common Eclipse components to provide theory editing, correct symbols, completion assistance, prover output and other features. By building on Eclipse it inherits various IDE goodies out of the box.

## Downloads

Download the latest version of Isabelle/Eclipse plug-ins from the update site:

[http://www.ai4fm.org/isabelle-eclipse/updates/latest/](http://www.ai4fm.org/isabelle-eclipse/updates/latest/)

Use the _Update Manager_ in Eclipse to download the plug-ins. Note that this update site serves a "nightly" build of Isabelle/Eclipse, built from the latest source code.

### Requirements

-   **Isabelle 2013**

    Isabelle/Eclipse works with Isabelle 2013 **only** at the moment. The Isabelle/Scala layer currently provides no easy backwards compatibility.
    
-   **Eclipse 3.7 (Indigo) or 4.2 (Juno)**

    Isabelle/Eclipse should work with any Eclipse distribution (e.g. Eclipse Classic or just the minimal Platform Runtime). The required Scala library is currently distributed together with the plug-ins.

-   **Java 7**

    Isabelle/Scala requires Java 7 runtime - the plug-ins will be disabled if older Java version is used.

## Previous versions

An older version of Isabelle/Eclipse plug-ins is available for Isabelle 2012. Get them from the update site:

[http://www.ai4fm.org/isabelle-eclipse/download/isabelle2012/releases/](http://www.ai4fm.org/isabelle-eclipse/download/isabelle2012/releases/)

    
## Contributing

Please report bugs, feature requests and other issues using the GitHub tracker:

[http://github.com/andriusvelykis/isabelle-eclipse/issues](http://github.com/andriusvelykis/isabelle-eclipse/issues)

You can contribute to the project by forking the repository and sending [pull requests][pull-req] with your changes. The plug-ins are built using Maven - will try adding installation/building instructions next. Feel free to contact the author for assistance. 

[pull-req]: https://help.github.com/articles/using-pull-requests/

## Authors

Developed by [Andrius Velykis][av] (Newcastle University, UK) as part of the [AI4FM research project][ai4fm].

[av]: http://andrius.velykis.lt
[ai4fm]: http://www.ai4fm.org
