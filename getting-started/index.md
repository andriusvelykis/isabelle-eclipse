---
layout: docs
content_type: md

title: Getting started
---

# Getting started

Isabelle/Eclipse provides user interface (prover IDE) to work with [Isabelle][isabelle] proof assistant. It integrates with Eclipse IDE, thus users familiar with Eclipse will feel at home using Isabelle/Eclipse.

To get started with Isabelle/Eclipse, open Isabelle perspective, create projects for your Isabelle theory files, launch the Isabelle prover and start proving! Read on for more details.

[isabelle]: http://isabelle.in.tum.de


## Isabelle perspective

To use Isabelle/Eclipse, switch to Isabelle perspective: **Window > Open Perspective > Other... > Isabelle** or use the perspective selector in the top-right corner. This will open and arrange all Isabelle/Eclipse views.


## Open Isabelle theory files

Isabelle/Eclipse supports editing files in the workspace and external ones.

To follow Eclipse IDE conventions, create a project in the workspace using **File > New > Project > General > Project**. You can place the project wherever you want (e.g. select an existing directory containing your theory files by deselecting **Use default location**). The project files will then be available in the _Project Explorer_ view.

Open your theory files by double-clicking them in the _Project Explorer_ view. Create new theory files by selecting **File > New > File** and entering a file name with `*.thy` extension.

To open external files, use **File > Open File...** menu option. Note, however, that some features (e.g. problem markers) are not as well supported for external files.


## Configure and launch Isabelle prover

To start proving, you need to select, configure and launch the Isabelle theorem prover. Isabelle launch configurations can be managed by selecting **Run > Isabelle > Isabelle Configurations...** or the corresponding toolbar buttons ![Isabelle launch](../images/launch-isabelle-run-icon.png).

[Create a launch configuration][launch-config] for your operating system (e.g. _Isabelle Mac App_ for Mac OS X) and provide the location of Isabelle installation. Then choose the main session (e.g. **HOL**) below and **Run** the Isabelle prover. Read more about Isabelle launch configurations and further options in [_Launch configurations_ page][launch-config].

After the first launch, select **Run > Isabelle** or click the toolbar button ![Isabelle launch](../images/launch-isabelle-run-icon.png) to launch the last Isabelle configuration. You will need to launch the prover again when you restart Isabelle/Eclipse.

[launch-config]: ../features/launch-config.html


## Start proving

To start proving, open your theory files. The visible file contents are automatically sent to the prover. The progress and results are displayed in [the editor](../features/theory-editor.html). Use the [_Prover Output_ view](../features/prover-output.html) to inspect the outstanding goals and other results for commands selected in the editor.

Read more about various Isabelle/Eclipse features in the Features section.
