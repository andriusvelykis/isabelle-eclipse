---
layout: default
content_type: md

title: Download
---

# Download Isabelle/Eclipse (nightly)

Isabelle/Eclipse is available both as standalone **Isabelle/Eclipse IDE** and as **plug-ins for Eclipse** to be installed via _Update Manager_.

The current version has not been officially released yet, but nightly builds are available from the following links:

-   [**Download standalone Isabelle/Eclipse IDE**][download-standalone-nightly] (use latest build)
-   [**Isabelle/Eclipse update site**][download-updates-nightly] (use [_Update Manager_][update-manager] in Eclipse):

    [`http://andriusvelykis.github.com/isabelle-eclipse/updates/isabelle2013/nightly/`][download-updates-nightly]

[download-standalone-nightly]: http://sourceforge.net/projects/isabelleeclipse/files/isabelle2013/isabelle-eclipse-ide/nightly/
[download-updates-nightly]: updates/isabelle2013/nightly/
[update-manager]: http://www.vogella.com/articles/Eclipse/article.html#updatemanager

## Requirements

-   **Isabelle 2013**

    Isabelle/Eclipse works with Isabelle 2013 **only** at the moment. The Isabelle/Scala layer currently provides no easy backwards compatibility.
    
-   **Eclipse 3.7 (Indigo) or 4.2 (Juno)**

    Isabelle/Eclipse should work with any Eclipse distribution (e.g. Eclipse Classic or just the minimal Platform Runtime). The required Scala library is currently distributed together with the plug-ins.

-   **Java 7**

    Isabelle/Scala requires Java 7 runtime - the plug-ins will be disabled if older Java version is used.

# Previous versions

An older version of Isabelle/Eclipse plug-ins is available for **Isabelle 2012**. Get them from the update site:

[`http://andriusvelykis.github.com/isabelle-eclipse/updates/isabelle2012/releases/`][download-updates-2012]

[download-updates-2012]: updates/isabelle2012/releases/
