# AEM Dictionary Translator

This tool is for managing i18n labels in AEMaaCS. The tool in http://localhost:4502/libs/cq/i18n/translator.html is not
available
in cloud service versions of AEM.

### How to use

After you've installed the project in your AEM instance, you can go
to http://localhost:4502/tools/translation/dictionaries.html. There you will
see all i18n dictionaries in /content, /var and /libs that use translation nodes (json files are not supported).
You can select a dictionary to edit its translations.

## Modules

The main parts of the template are:

* core: Java bundle containing all core functionality: component-related Java code and some utility classes.
* ui.apps: contains the /apps part of the project, ie the components
* all: a single content package that embeds all the compiled modules (bundles and content packages) including any vendor
  dependencies

## How to build

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

To build all the modules and deploy the `all` package to a local instance of AEM, run in the project root directory the
following command:

    mvn clean install -PautoInstallSinglePackage

Or alternatively

    mvn clean install -PautoInstallSinglePackage -Daem.port=4503

Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle

Or to deploy only a single content package, run in the sub-module directory (i.e `ui.apps`)

    mvn clean install -PautoInstallPackage
