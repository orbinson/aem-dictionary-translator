# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- Add sling:key to labels for a newly created language: [#28](https://github.com/orbinson/aem-dictionary-translator/issues/28)

## [1.1.2] - 2024-02-28

## [1.1.1] - 2024-02-08

### Added

-   CSV files with other languages than the dictionary currently has will not be imported: [#17](https://github.com/orbinson/aem-dictionary-translator/pull/17)

### Fixed

-   Resolved errors occurring when publishing labels: [#13](https://github.com/orbinson/aem-dictionary-translator/issues/13)
-   Reactivated publish button for dictionaries: [#13](https://github.com/orbinson/aem-dictionary-translator/issues/13)
-   Exporting a csv sometimes gives completely empty file: [#15](https://github.com/orbinson/aem-dictionary-translator/issues/15)
-   Importing a csv with the correct languages sometimes still gives the error of wrong languages: [#19](https://github.com/orbinson/aem-dictionary-translator/issues/19)


## [1.1.0] - 2024-02-01

### Changed

-   Dictionaries with .json files are no longer showed in dictionary
    list: [#8](https://github.com/orbinson/aem-dictionary-translator/pull/5)
-   Labels with no translation for a specific language no longer have a
    sling:message so the value won't be empty but will have a fallback
    from another language or the key itself: [#12](https://github.com/orbinson/aem-dictionary-translator/pull/12)

### Added

-   Publish specific labels in a dictionary: [#8](https://github.com/orbinson/aem-dictionary-translator/pull/5)
-   Export/Import CSV files of/to dictionaries: [#10](https://github.com/orbinson/aem-dictionary-translator/issues/10)

## [1.0.6] - 2023-09-12

### Changed

-   Add breadcrumbs to the dictionary labels page: [#3](https://github.com/orbinson/aem-dictionary-translator/issues/3)

## [1.0.5] - 2023-08-30

### Fixed

-   Fix labels not working anymore when a new language is added

## [1.0.4] - 2023-08-16

### Fixed

-   Use the distribution API to be able to deep replicate a
    dictionary: [#1](https://github.com/orbinson/aem-dictionary-translator/pull/1)

## [1.0.3] - 2023-08-14

### Changed

-   Set minimum required SDK version to 2023.6.12255.20230608T053118Z-230400

## [1.0.2] - 2023-08-11

### Fixed

-   When adding new languages existing labels could not be updated

### Changed

-   Update README.md documentation

## [1.0.1] - 2023-08-10

### Fixed

-   Use correct user mapping for service user

## [1.0.0] - 2023-08-09

### Added

-   Create, publish and edit dictionaries
-   Create, delete and update labels in a dictionary

[Unreleased]: https://github.com/orbinson/aem-dictionary-translator/compare/1.1.2...HEAD

[1.1.2]: https://github.com/orbinson/aem-dictionary-translator/compare/1.1.1...1.1.2

[1.1.1]: https://github.com/orbinson/aem-dictionary-translator/compare/1.1.0...1.1.1

[1.1.0]: https://github.com/orbinson/aem-dictionary-translator/compare/1.0.6...1.1.0

[1.0.6]: https://github.com/orbinson/aem-dictionary-translator/compare/1.0.5...1.0.6

[1.0.5]: https://github.com/orbinson/aem-dictionary-translator/compare/1.0.4...1.0.5

[1.0.4]: https://github.com/orbinson/aem-dictionary-translator/compare/1.0.3...1.0.4

[1.0.3]: https://github.com/orbinson/aem-dictionary-translator/compare/1.0.2...1.0.3

[1.0.2]: https://github.com/orbinson/aem-dictionary-translator/compare/1.0.1...1.0.2

[1.0.1]: https://github.com/orbinson/aem-dictionary-translator/compare/1.0.0...1.0.1

[1.0.0]: https://github.com/orbinson/aem-dictionary-translator/compare/aef9658ce0967039de44f69228c16744d45e2764...1.0.0
