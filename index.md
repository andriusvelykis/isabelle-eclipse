---
layout: default
content_type: md
permalink: /
---

<div class="alert alert-block">
  <h4>Support for Isabelle2013-1</h4>
  <p>Isabelle/Eclipse does not support Isabelle2013-1 at the moment. The upgrade to support the new version <a href="https://github.com/andriusvelykis/isabelle-eclipse/issues/82">is planned</a>. Unfortunately, due to writing-up of my PhD thesis, the upgrade is likely to be delayed until early next year.</p>
</div>

# Isabelle/Eclipse

[Eclipse][eclipse] plug-ins that provide Prover IDE for [Isabelle proof assistant][isabelle], based on Isabelle/Scala framework.

Isabelle/Eclipse started as a port of Isabelle/jEdit Prover IDE to integrate with Eclipse IDE as plug-ins. The integration uses common Eclipse components to provide theory editing, correct symbols, completion assistance, prover output and other features. By building on Eclipse it inherits various IDE goodies out of the box.

[![Isabelle/Eclipse][isa-scr]][isa-scr]

[isa-scr]: images/isabelle-eclipse.png
[eclipse]: http://www.eclipse.org/
[isabelle]: http://isabelle.in.tum.de/

## Getting started

To use Isabelle/Eclipse, download it either as standalone **Isabelle/Eclipse IDE** or use the update site to install **the plug-ins into your Eclipse**. The latest Isabelle/Eclipse requires **Java 7** to run and works with **Isabelle 2013** only. Refer to the [Downloads][download] page for download links and further details.

After downloading/installing Isabelle/Eclipse, open Isabelle perspective, create projects for your Isabelle theory files, launch the Isabelle prover and start proving! Read more about the first steps in the [Getting started][getting-started] page.

[download]: download.html
[getting-started]: getting-started/index.html


## Features

Isabelle/Eclipse provides a familiar Eclipse-based prover IDE with numerous features. Read about some of the [features here][features].

-   [Isabelle launch configurations]( features/launch-config.html )
-   [Theory editor]( features/theory-editor.html )
-   [Prover output]( features/prover-output.html )
-   [Go to definition]( features/go-to-definition.html )
-   [Content assist]( features/content-assist.html )
-   [Tooltips]( features/tooltips.html )
-   [Syntax colouring]( features/syntax-coloring.html )
-   [Adjust Isabelle font]( features/adjust-font.html )
-   [Outline]( features/outline.html )
-   [Symbols view]( features/symbols-view.html )
-   [Theories view]( features/theories-view.html )
-   [Controlling Isabelle execution]( features/isabelle-execution.html )
-   ... [and more!][features]


[features]: features/index.html

    
## Contributing

Please report bugs, feature requests, questions and other issues using the GitHub tracker:

[`http://github.com/andriusvelykis/isabelle-eclipse/issues`](http://github.com/andriusvelykis/isabelle-eclipse/issues)

You can also contribute to the project by forking the repository and sending [pull requests][pull-req] with your changes. We welcome various contributions!

Isabelle/Eclipse is built on Eclipse platform using Scala and Java programming languages. It is built using Maven and Eclipse Tycho. Refer to the [Developer documentation][developer] for hints on building Isabelle/Eclipse yourself.

Feel free to contact the author for assistance. 

[pull-req]: https://help.github.com/articles/using-pull-requests/
[developer]: dev/index.html

## Authors

Developed by [Andrius Velykis][av] (Newcastle University, UK) as part of the [AI4FM research project][ai4fm].

[av]: http://andrius.velykis.lt
[ai4fm]: http://www.ai4fm.org
