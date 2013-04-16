# [Isabelle/Eclipse]( http://andriusvelykis.github.io/isabelle-eclipse )

[Eclipse][eclipse] plug-ins that provide Prover IDE for [Isabelle proof assistant][isabelle], based on Isabelle/Scala framework.

[eclipse]: http://www.eclipse.org/
[isabelle]: http://www.cl.cam.ac.uk/research/hvg/isabelle/

## Description

Isabelle/Eclipse started as a port of Isabelle/jEdit Prover IDE to integrate with Eclipse IDE as plug-ins. The integration uses common Eclipse components to provide theory editing, correct symbols, completion assistance, prover output and other features. By building on Eclipse it inherits various IDE goodies out of the box.

Visit [Isabelle/Eclipse website][isabelle-eclipse] for more details.

[isabelle-eclipse]: http://andriusvelykis.github.io/isabelle-eclipse

## Downloads

Isabelle/Eclipse is available both as standalone **Isabelle/Eclipse IDE** and as **plug-ins for Eclipse** to be installed via _Update Manager_.

The latest stable version is **1.2.0** for Isabelle 2013. Download it from the following links:

-   [**Download standalone Isabelle/Eclipse IDE**][download-standalone-120]
-   [**Isabelle/Eclipse update site**][download-updates-release] (use [_Update Manager_][update-manager] in Eclipse):

    [`http://andriusvelykis.github.io/isabelle-eclipse/updates/isabelle2013/releases/`][download-updates-release]

[download-standalone-120]: http://sourceforge.net/projects/isabelleeclipse/files/isabelle2013/isabelle-eclipse-ide/1.2.0/
[download-updates-release]: http://andriusvelykis.github.io/isabelle-eclipse/updates/isabelle2013/releases/
[update-manager]: http://www.vogella.com/articles/Eclipse/article.html#updatemanager

### Requirements

-   **Isabelle 2013**

    Isabelle/Eclipse works with Isabelle 2013 **only** at the moment. The Isabelle/Scala layer currently provides no easy backwards compatibility.
    
-   **Eclipse 3.7 (Indigo) or 4.2 (Juno)**

    Isabelle/Eclipse should work with any Eclipse distribution (e.g. Eclipse Classic or just the minimal Platform Runtime). The required Scala library is currently distributed together with the plug-ins.

-   **Java 7**

    Isabelle/Scala requires Java 7 runtime - the plug-ins will be disabled if older Java version is used.

### Nightly builds

Nightly builds are available to test the cutting-edge features of Isabelle/Eclipse. Refer to the [`develop`][develop-branch] branch for details.

[develop-branch]: http://github.com/andriusvelykis/isabelle-eclipse/tree/develop/

### Previous versions

An older version of Isabelle/Eclipse plug-ins is available for **Isabelle 2012**. Get them from the update site:

[`http://andriusvelykis.github.io/isabelle-eclipse/updates/isabelle2012/releases/`][download-updates-2012]

[download-updates-2012]: http://andriusvelykis.github.io/isabelle-eclipse/updates/isabelle2012/releases/

    
## Contributing

Please report bugs, feature requests, questions and other issues using the GitHub tracker:

[`http://github.com/andriusvelykis/isabelle-eclipse/issues`](http://github.com/andriusvelykis/isabelle-eclipse/issues)

You can also contribute to the project by forking the repository and sending [pull requests][pull-req] with your changes. We welcome various contributions!

**Note that `master` branch is used to track release code. The new development is added to the [`develop`][develop-branch] branch!**

Isabelle/Eclipse is built on Eclipse platform using Scala and Java programming languages. It is built using Maven and Eclipse Tycho. Refer to the [Developer documentation][developer] for hints on building Isabelle/Eclipse yourself.

Feel free to contact the author for assistance. 

[pull-req]: https://help.github.com/articles/using-pull-requests/
[developer]: http://andriusvelykis.github.io/isabelle-eclipse/dev/index.html


## Authors

Developed by [Andrius Velykis][av] (Newcastle University, UK) as part of the [AI4FM research project][ai4fm].

[av]: http://andrius.velykis.lt
[ai4fm]: http://www.ai4fm.org
