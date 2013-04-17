---
layout: docs
content_type: md

title: Getting started
---

# Getting started

Isabelle/Eclipse provides user interface (prover IDE) to work with [Isabelle][isabelle] proof assistant. It integrates with Eclipse IDE, thus users familiar with Eclipse will feel at home using Isabelle/Eclipse.

To get started with Isabelle/Eclipse, open Isabelle perspective, create projects for your Isabelle theory files, launch the Isabelle prover and start proving! Read on for more details.

[isabelle]: http://isabelle.in.tum.de


## Starting Isabelle/Eclipse

[Download][download] Isabelle/Eclipse IDE and unpack the archive. Launch the application by double-clicking the executable file:

-   _isabelle-eclipse.exe_ on Windows
-   _isabelle-eclipse_ on Linux
-   _isabelle-eclipse.app_ on Mac OS X

Isabelle/Eclipse is based on Eclipse IDE, so refer to [information on running Eclipse][running-eclipse] for details on allocating available memory, indicating Java executable, etc. For configuration, use the corresponding _isabelle-eclipse.ini_ file instead of _eclipse.ini_.

### Troubleshooting startup

When downloading, make sure to select a correct version for your system (32-bit or 64-bit), otherwise the application will fail.


#### Java 7

**Make sure [Java 7][java] is used to start Isabelle/Eclipse**. If the application does not look correct, or Isabelle/Eclipse views and perspective are missing, it is most likely that **Java 7** is not available. If the correct Java runtime is not detected automatically, [indicate it explicitly][running-eclipse]. On Mac OS X we recommend installing [Java 7 JDK][jdk].


#### "Damaged" application on Mac OS X

If you encounter the _"Isabelle/Eclipse is damaged and can't be opened"_ message when starting Isabelle/Eclipse on Mac OS X, [adjust your Gatekeeper settings temporarily][gatekeeper-mac] to allow the application to run. You only need to do this once.


[download]: ../download.html
[running-eclipse]: http://help.eclipse.org/juno/index.jsp?topic=%2Forg.eclipse.platform.doc.user%2Ftasks%2Frunning_eclipse.htm
[java]: http://www.java.com/getjava
[jdk]: http://www.oracle.com/technetwork/java/javase/downloads
[gatekeeper-mac]: http://apple.stackexchange.com/questions/58087/eclipse-4-2-on-mountain-lion-gatekeeper-rejects-as-unidentified-developer


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
