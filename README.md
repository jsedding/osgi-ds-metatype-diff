# OSGi Declarative Services & MetaType Diff

This maven module creates a runnable jar file, which can be used to show the differences
of both Declarative Services metadata and MetyType metadata contained by two OSGi bundles.

Diffing OSGi bundle metadata can be useful, e.g. when refactoring a code-base from
`org.apache.felix.scr.annotations` to the official OSGi annotations
(`org.osgi.service.component.annotations` and `org.osgi.service.metatype.annotations`).
Diffing the a bundle created before refactoring and after refactoring of the annotations
can help catch subtle differences that would otherwise go unnoticed and may lead to bugs.

Note that the goal of such a refactoring is not necessarily to have no differences. E.g.
it is quite common that older DS components have their references injected via bind/unbind
methods, while later spec versions allow field injection, which manifests in the metadata,
but usually does not have any impact on functionality.

## Usage

    $ java -jar osgi-ds-metatype-diff.jar <oldBundle> <newBundle>
    org.example.BarService
    Declarative Services
        References
            foo
                - bind = bindFoo
                - unbind = unbindFoo
                + field = foo
                + field-option = replace
    ...
