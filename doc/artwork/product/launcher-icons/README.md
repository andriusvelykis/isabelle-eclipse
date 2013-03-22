# Branding icons for Eclipse launcher

Notes on branding icons for Eclipse launcher for different OS builds: their formats, and hints
on producing them.

These instructions are for building a branded Eclipse product using [Eclipse Tycho][tycho]
with Eclipse Juno (4.2) as the base.

[tycho]: http://www.eclipse.org/tycho/


## Setup

Eclipse launcher can be branded using a corresponding _.product_ file. The _Launching_ tab allows
setting icons for each OS.

To set the launcher file name, indicate it in _Launcher Name_ field. For example, if you write
"CZT" there, there will be a _CZT.exe_ in Windows, _CZT.app_ in Mac OS X and _CZT_ executable
in Linux.


### Relative paths

The launcher icons need not be packaged into plug-ins, because they are only used to customise the
launcher. So we do not need to put these icons into the _branding plug-in_, and can store them
together with the _.product_ file.

If the launcher icons are in the same project as the _.product_ file, they must be indicated
using a relative path. For example, if the icons are in _/icons_ folder of the project, use
the following path for icon: _/icons/czt.icns_. 

If you select the icon using _Browse_ button in the _Product Configuration Editor_, it will 
prepend the project name as well, e.g. _/net.sourceforge.czt.eclipse.product/icons/czt.icns_.
When building with [Tycho][tycho], it will fail to resolve the relative path because of the
project part. So correct it to be relative to the _.product_ file, e.g. _/icons/czt.icns_.


## Linux

Linux launcher file does not actually have an icon. However, a icon file (_XPM_) can be provided
for users to customise their shortcuts.

Eclipse bundles for Linux provide an _XPM_ icon of size `256×256`. Note that the file is included
in the Linux product bundle _as is_, so the suggested name for it is _icon.xpm_.

To create an _XPM_ icon file, I used [GIMP][gimp]. Unfortunately GIMP cannot read or write _XPM_
files on Mac OS X (I tried version 2.8.2). To work around this, I used a virtual
[Ubuntu Linux][ubuntu] machine running on [VirtualBox][virtualbox]. I installed GIMP there and
could export (_Save As_) the _XPM_ file.

When creating the _XPM_ file I used a `256×256` _PNG_ file with transparency as my source. During
export, GIMP asks to indicate _Threshold Alpha_ value to handle the partial transparency in the
icon. I found that the default value of `127` gives me the best result, but try what suits you.

[gimp]: http://www.gimp.org/
[ubuntu]: http://www.ubuntu.com/
[virtualbox]: http://www.virtualbox.org/


## Mac OS X

Mac OS X launcher requires a single _ICNS_ file that will be part of the `<Launcher>.app`
folder. The file is standard Mac OS X icon file and supports multiple icons
(e.g. _PNG_ files with transparency) of the following sizes:

> `16×16`, `32×32`, `128×128`, `256×256`, `512×512`.

Note that the icons should come in pairs (standard and high resolution, e.g. for _Retina_
displays). The high resolution is double the pixels of a standard one.

The instructions to produce icons are available in [Apple documentation][mac-hires]. You need
to put icons named `icon_<sizeinpoints>x<sizeinpoints>[@<scale>].png`, e.g. _icon_16x16.png_,
_icon_16x16@2x.png_, etc, into an _.iconset_ folder. Then run `iconutil` tool to produce
the _ICNS_ file for Eclipse. Refer to [Apple documentation][mac-hires] for details.

Alternatively, the _ICNS_ file can be produced using the _Icon Composer_ app (available with
developer tools), but this method is no longer recommended by Apple.

[mac-hires]: http://developer.apple.com/library/mac/#documentation/GraphicsAnimation/Conceptual/HighResolutionOSX/Optimizing/Optimizing.html#//apple_ref/doc/uid/TP40012302-CH7-SW3


## Windows

Windows launcher is produced by replacing icons in Eclipse executable with provided ones.
So if the replacement fails, the executable will contain Eclipse icons.

To use with [Tycho][tycho], you need to create a multi-icon _ICO_ file. The _.product_
file also provides fields to specify 7 separate _BMP_ images to use as icons. Unfortunately,
I could not manage to get it to work with Tycho 0.15.0 - it seemed to only pick the first
one.

The _ICO_ file supports all resolutions outlined in the _.product_ file, in
32-bit (RGB / Alpha Channel) and 8-bit (256 colors, indexed) _BMP_ icons:

1. 32-bit: `16×16`, `32×32`, `48×48`, `256×256`.
2. 8-bit: `16×16`, `24×24` (for older Eclipse versions?), `32×32`, `48×48`

To convert from transparent _PNG_ to _BMP_ files, I used [GIMP][gimp]. For 32-bit files,
just export to _BMP_ file. For 8-bit files, first convert to 255 colours using
Image > Mode > Indexed... and select _Generate optimum palette_ with 255 colours.
Then export to _BMP_ file with a warning about losing the transparency.

After producing all BMP files, [combine them in GIMP using different layers][gimp-ico]
and export as _ICO_ file. Alternatively, use icon software such as [IcoFX][icofx]
(15 days fully-functional trial on Windows) to produce the multi-icon _ICO_.

For Eclipse, you need to make sure that the `256×256` icon is uncompressed, otherwise
Tycho will not use it. When you have the _ICO_ file (or if you are producing it with GIMP),
re-save it with GIMP and deselect _Compressed (PNG)_ for all layers during export.

[gimp-ico]: http://egressive.com/tutorial/creating-a-multi-resolution-favicon-including-transparency-with-the-gimp
[icofx]: http://icofx.ro/


### Tycho 0.16.0 required for 256×256

Tycho 0.15.0 does not support _ICO_ files containing `256×256` resolution images. There is
[a bug in IconExe][bug-iconexe] that prevents such file to be used altogether. It is fixed
in Tycho 0.16.0, however, so use it if you need large icons. Otherwise, make sure your _ICO_
file does not contain large `256×256` icons.

[bug-iconexe]: https://bugs.eclipse.org/bugs/show_bug.cgi?id=384509


## Further information

Similar information is [available for branding Eclipse itself][eclipse-branding] with some
additional links.

[eclipse-branding]: http://wiki.eclipse.org/Platform-releng/Updating_Branding
