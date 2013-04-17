---
layout: default
content_type: md

title: Download
---

# Download Isabelle/Eclipse

Isabelle/Eclipse is available both as standalone **Isabelle/Eclipse IDE** and as **plug-ins for Eclipse** to be installed via _Update Manager_.

The latest stable version is **1.2.0** for Isabelle 2013. Download it from the following links:

-   [**Download standalone Isabelle/Eclipse IDE**][download-standalone-120]
-   [**Isabelle/Eclipse update site**][download-updates-release] (use [_Update Manager_][update-manager] in Eclipse):

    [`http://andriusvelykis.github.io/isabelle-eclipse/updates/isabelle2013/releases/`][download-updates-release]

Make sure the requirements below are satisfied and [get started using Isabelle/Eclipse][getting-started]!


[download-standalone-120]: http://sourceforge.net/projects/isabelleeclipse/files/isabelle2013/isabelle-eclipse-ide/1.2.0/
[download-updates-release]: updates/isabelle2013/releases/
[update-manager]: http://www.vogella.com/articles/Eclipse/article.html#updatemanager
[getting-started]: getting-started/index.html

## Requirements

-   **Isabelle 2013**

    Isabelle/Eclipse works with Isabelle 2013 **only** at the moment. The Isabelle/Scala layer currently provides no easy backwards compatibility.
    
-   **Eclipse 3.7 (Indigo) or 4.2 (Juno)**

    Isabelle/Eclipse should work with any Eclipse distribution (e.g. Eclipse Classic or just the minimal Platform Runtime). The required Scala library is currently distributed together with the plug-ins. _Note that Eclipse 3.7 has issues using Java 7 on Mac OS X._

-   **Java 7**

    Isabelle/Scala requires [Java 7 runtime][java] - the plug-ins will be disabled if older Java version is used. On Mac OS X we recommend installing [Java 7 JDK][jdk].

[java]: http://www.java.com/getjava
[jdk]: http://www.oracle.com/technetwork/java/javase/downloads


## Nightly builds

Nightly builds are available to test the cutting-edge features of Isabelle/Eclipse. Use the following links to download:

-   [**Download standalone Isabelle/Eclipse IDE (nightly)**][download-standalone-nightly] (use the latest build)
-   [**Isabelle/Eclipse nightly update site**][download-updates-nightly] (use [_Update Manager_][update-manager] in Eclipse):

    [`http://andriusvelykis.github.io/isabelle-eclipse/updates/isabelle2013/nightly/`][download-updates-nightly]

[download-standalone-nightly]: http://sourceforge.net/projects/isabelleeclipse/files/isabelle2013/isabelle-eclipse-ide/nightly/
[download-updates-nightly]: updates/isabelle2013/nightly/


## Previous versions

An older version of Isabelle/Eclipse plug-ins is available for **Isabelle 2012**. Get them from the update site:

[`http://andriusvelykis.github.io/isabelle-eclipse/updates/isabelle2012/releases/`][download-updates-2012]

[download-updates-2012]: updates/isabelle2012/releases/
