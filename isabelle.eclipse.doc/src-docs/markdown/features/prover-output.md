# Prover output

Isabelle proof results can be inspected in the _Prover Output_ view. It displays results of the currently selected command in the editor.

![Isabelle prover output](../images/prover-output-view.png)

If the proof has been successful, the output view displays outstanding goals or similar information. In case of failed or erroneous commands, the error message can also be inspected there.

The prover output view features syntax highlighting, [rich tooltips](tooltips.html) and [hyperlinks to definitions](go-to-definition.html) just like the main editor.

Normally the prover output follows the command selection in the editor. To disable that temporarily (e.g. to keep the prover output while browsing a theory), toggle the **Link with Editor** button ![Link with Editor](../images/synced.gif).
