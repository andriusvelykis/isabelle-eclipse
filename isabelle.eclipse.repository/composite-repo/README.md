# Create composite p2 repository

[Composite p2 repositories][p2-comp] allow aggregating content from multiple p2
repositories. This way allows including different versions of Isabelle/Eclipse releases
in a single update site

[p2-comp]: http://wiki.eclipse.org/Equinox/p2/Composite_Repositories_(new)


## Running the script

To run the script, use Maven build in repository project directory with the following command:

    mvn generate-sources -P generate-composite-repo -N -Declipse.dir=ECLIPSE_DIR -Dtycho.mode=maven


## Running the script standalone

The `comp-repo.sh` shell script builds (or modifies) a composite p2 repository.
Below is a sample execution that produces a composite repository
that contains 2 child repositories of different releases (using relative paths).


    ./comp-repo.sh <REPO_DIR> --eclipse <ECLIPSE_DIR> \
    --name "Isabelle/Eclipse Releases" \
    add 1.1.0 \
    add 1.1.1

See [the blog post][comp-repo-blog] for more details on the script and its arguments.

[comp-repo-blog]: http://eclipsesource.com/blogs/2012/06/11/creating-p2-composite-repositories-on-the-command-line/

