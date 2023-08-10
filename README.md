[![Maven Central](https://img.shields.io/maven-central/v/be.orbinson.aem/aem-dictionary-translator)](https://search.maven.org/artifact/be.orbinson.aem/aem-dictionary-translator.all)
[![GitHub](https://img.shields.io/github/v/release/orbinson/aem-dictionary-translator)](https://github.com/orbinson/aem-dictionary-translator/releases)
[![Build and test for AEM 6.5](https://github.com/orbinson/aem-dictionary-translator/actions/workflows/build.yml/badge.svg)](https://github.com/orbinson/aem-dictionary-translator/actions/workflows/build.yml)
[![Build with AEM IDE](https://img.shields.io/badge/Built%20with-AEM%20IDE-orange)](https://plugins.jetbrains.com/plugin/9269-aem-ide)

# AEM Dictionary Translator

AEM TouchUI tool to translate labels for i18n internationalisation in AEM on premise or AEM as a Cloud Service. The AEM
Dictionary Translator is a replacement for the
ClassicUI [translator](http://localhost:4502/libs/cq/i18n/translator.html) which is not available on AEMaaCS.

![Dictionaries](docs/assets/dictionaries.png)

![Labels](docs/assets/labels.png)

## Installation

To deploy the AEM Dictionary Translator as an embedded package you need to update your `pom.xml`

1. Add the `aem-dictionary-translator.all` to the `<dependencies>` section

   ```xml
   <dependency>
     <groupId>be.orbinson.aem</groupId>
     <artifactId>aem-dictionary-translator.all</artifactId>
     <version>1.0.1</version>
     <type>zip</type>
   </dependency>
   ```
2. Embed the package in with
   the [filevault-package-maven-plugin](https://jackrabbit.apache.org/filevault-package-maven-plugin/) in
   the `<embeddeds>` section

   ```xml
   <embedded>
      <groupId>be.orbinson.aem</groupId>
      <artifactId>aem-dictionary-translator.all</artifactId>
      <target>/apps/vendor-packages/content/install</target>
   </embedded>
   ```

## Development

To build all the modules run in the project root directory the following command

```shell
mvn clean install
```

To build all the modules and deploy the `all` package to a local instance of AEM, run in the project root directory the
following command

```shell
mvn clean install -PautoInstallSinglePackage
```

This project follows the [AEM Archetype](https://github.com/adobe/aem-project-archetype) conventions so for further
guidelines consult the available documentation.
